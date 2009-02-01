;; Copyright (c) 2008,2009 Lau B. Jensen <lau.jensen {at} bestinclass.dk
;;                         
;; All rights reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE.txt at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by the
;; terms of this license. You must not remove this notice, or any other, from
;; this software.

(clojure.core/in-ns 'dk.bestinclass.gitdoc)

(import '(com.trolltech.qt.core QFile QIODevice QIODevice$OpenMode
                               QIODevice$OpenModeFlag QUrl )
        '(com.trolltech.qt.gui QApplication QWidget QStyleFactory QListView
                              QStandardItemModel QStandardItem)
        '(com.trolltech.qt.designer QUiLoader)
        '(java.io StringReader BufferedReader))


;;=========== GLOBALS ===========

(defstruct struct-filemod :name :insertions :deletions)
(defstruct struct-commit  :title :hash :author :date :description :files)

(def *global-variables* (ref (hash-map)))
(def *commits*          (ref (hash-map)))

;;=========== HELPERS ===========

(defn $get
  " Retrieves the value in keyword k - Global vars "
  [k]
  (@*global-variables* k))

(defn $set
  " Set the keyword k to a value - Global vars "
  [k value]
  (dosync
   (alter *global-variables* assoc k value)))

(defn plines
  [lines]
  (doseq [lines lines]
    (doseq [line (.split lines "\n")]
      (println line))))

(defn pl
  [coll]
  (if (string? coll)
    (doseq [r (.split coll "\n")]
      (println r))
    (doseq [r coll]
      (println r))))

(defn leadingSpaces
  " Git-log differentiates the description and file section by the
    number of spaces in the indentation, this function counts the indent "
  [in]
  (count (take-while #(= % \space) in)))

(defn >nstring
  " Convert a collection of strings to 1 string, seperated by \newlines "
  [in]
  (apply str (interpose "\n" in)))

(defn >filestruct
  " Given the file-section in a single string, this will build file-structs "
  [file-section]
  (for [file (.split file-section "\n")]
    (let [ [filename div count graph] (remove empty? (.split file " | ")) ]
      (struct-map struct-filemod :name    filename
                                 :changes count))))


(defn ->filestruct
  [file-section]
  (if (string? file-section)
    (let [result   (drop 1 (re-find #"(\d+)\t(\d+)\s+(.*)" file-section)) ]
      (struct-map struct-filemod   :name       (last result)
                                   :insertions (first result)
                                   :deletions  (second result)))                  
    (for [file file-section]
      (let [result   (re-seq #"(\d+)\t(\d+)\s+(.*)" file)
            items    (drop 1 (first result)) ]
        (struct-map struct-filemod :name       (last items)
                                   :insertions (first items)
                                   :deletions  (second items))))))
  
(defn getObject
  " Returns a reference to an object in the UI "
  [ name ]
  (.findChild ($get :ui) Object name))

(defn partition-by 
  "Applies f to each value in coll, splitting it each time f returns
   a new value.  Returns a lazy seq of lazy seqs. "
  [f coll]
  (when-let [s (seq coll)]
    (let [fv (f (first s))
          run (take-while #(= fv (f %)) s)]
      (lazy-cons run (partition-by f (drop (count run) s))))))


;;=========== GIT  ===========


(defn stripHashes
  " Obsolete "
  [log]
  (remove nil?
          (for [row (.split log "commit ")]
            (let [istart  (.indexOf row "\n")]
              (when (pos? istart)
                (.substring row 0 istart))))))

(defn build-struct
  " Given a commit, this will coarse the elements into a struct-map "
  [commit]
  (let [
         hash           (.substring (nth commit 0) (inc (.indexOf (nth commit 0) " ")))
         author         (second      (re-matches #"Author:\s+(.*)$" (nth commit 1)))
         date           (re-find #"(?<=\b[Date: ]\b)+.*" (nth commit 2))        
         crest          (interpose "\n" (remove empty?
                                                (map #(str %) (drop 3 commit))))
         description    (>nstring (filter #(= 4 (leadingSpaces %)) commit))
         files          (->filestruct (remove #(= % "\n") (filter #(zero? (leadingSpaces %)) crest)))
         title          (if (pos? (.indexOf description "\n"))
                          (.trim (.substring description 0 (.indexOf description "\n")))
                          (.trim description)) ]
    (struct-map struct-commit :title title  :hash hash               :author author
                              :date  date   :description description :files files)))
    
         

(defn get-log
  " Will retrieve a log from a given directory and return a sequence of the commits "
  []
  (let [ gitdir ($get :path)
         gitlog (with-sh-dir gitdir (sh "git-log" "--numstat")) ]
    (map #(apply concat %)
         (partition 2
                    (partition-by #(re-seq #"^commit .*" %)
                                  (line-seq (BufferedReader.
                                             (StringReader. gitlog))))))))

;;=========== SLOTS/EVENTS ===========

(declare initCommitView initDocumentation)

(def run-clicked
     (slot/slot 0 #(do
                     ($set :path (. (getObject "pathEdit") text))
                     (let [ textBuffer (getObject "statView")
                           git-tree   (map build-struct (get-log))
                           titles     (map (fn [_] (:title _)) git-tree) ]
                       (initCommitView titles)
                       (initDocumentation git-tree)))))
                     

;;=========== UI ===========

(defn init
  " All Qt applications must have 1 (and only 1) QApplication "
  []
  (def app (QApplication. (into-array String *command-line-args*))))


(defn initCommitView
  " Logic to init the CommitView component "
  [commit-names]
  (let [ cv         ($get :commits)
         model      (QStandardItemModel. 2 1)
         countNames (count commit-names) ]
    (doall
     (map #(doto  model
             (.setItem %1 0 (QStandardItem. (str (inc %1) ": " %2))))
         (range countNames)
         commit-names))
    (. cv (setModel model))))


(defn initDocumentation
  [git-tree]
  (. (getObject "statView") (setText
                             (apply str
                                        (map #(str (:title %) \newline ) git-tree)))))

(defn main-qt
  []
  (let 
      [  ui (QUiLoader/load (QFile. "/home/lau/coding/lisp/projects/gitdoc/git.ui")) ]
    ($set :ui ui)
    ($set :commits (getObject "commitView"))
    ($set :path (. (getObject "pathEdit") text))
    (.show ui)
    (QApplication/setStyle (QStyleFactory/create "Plastique"))
    (slot/connect (. (getObject "runButton") clicked) run-clicked)  
    (QApplication/exec)))



