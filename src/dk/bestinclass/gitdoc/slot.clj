; A collection of utilities for using Qt's signal/slot mechanism within clojure

(ns slot
 ; (:uses qutils)
  (:import (com.trolltech.qt QSignalEmitter
                            QSignalEmitter$Signal0
                            QSignalEmitter$Signal1
                            QSignalEmitter$Signal2
                            QSignalEmitter$Signal3
                            QSignalEmitter$Signal4
                            QSignalEmitter$Signal5
                            QSignalEmitter$Signal6
                            QSignalEmitter$Signal7
                            QSignalEmitter$Signal8
                            QSignalEmitter$Signal9)
            (com.trolltech.qt.clojure QHolder
                                    QHolder$Arity
                                    QHolder$QSignalHolder0
                                    QHolder$QSignalHolder1
                                    QHolder$QSignalHolder2
                                    QHolder$QSignalHolder3
                                    QHolder$QSignalHolder4
                                    QHolder$QSignalHolder5
                                    QHolder$QSignalHolder6
                                    QHolder$QSignalHolder7
                                    QHolder$QSignalHolder8
                                    QHolder$QSignalHolder9
                                    QHolder$QMethod0
                                    QHolder$QMethod1
                                    QHolder$QMethod2
                                    QHolder$QMethod3
                                    QHolder$QMethod4
                                    QHolder$QMethod5
                                    QHolder$QMethod6
                                    QHolder$QMethod7
                                    QHolder$QMethod8
                                    QHolder$QMethod9)
            (com.trolltech.qt.core Qt$ConnectionType)
            (clojure.lang IFn)))

;(refer 'qutils)

(defmacro do_ [& exprs] (list 'let (apply vector (mapcat #(list '_ %) exprs)) '_ ))
(defmacro doto-this [obj & exprs] `(let [~'this ~obj] (doto ~obj ~@exprs))) 

(defmacro def-
  "private version of def"
  [name & decls]
    (list* `def (with-meta name (assoc (meta name) :private true)) decls))

(defn dnew [name & args] 
  (clojure.lang.Reflector/invokeConstructor (resolve name) (to-array args))) 



(def- slots 
  (vec 
    (for [arity (range 10)] 
      (let [args (for [i (range arity)] (gensym))]
        (eval `(fn [f#] 
                  (proxy
                      [~(symbol (str "QHolder$QMethod" arity)) ~'QHolder$Arity ~'IFn]
                      []
                      (~'arity [] ~arity)
                      (~'invoke [~@args] (f# ~@args))
                      (~'applyTo ~(if (== 0 (count args)) [] `[[~@args]]) (f# ~@args))
                      (~'method [~@args] (f# ~@args)))))))))


(defn slot [arity f]
  "Wraps f in a qt slot with a fixed arity. Slots are instances of IFn, calling the wrapped clojure fn."
  (if (> arity 9)
    (throw (Exception. "Clojure slots cannot have more than 9 args"))
    ((nth slots arity) f) ))

(def- signals 
  (vec 
    (for [arity (range 10)] 
      (eval `(fn [] 
                (let [holder# (new ~(symbol (str "QHolder$QSignalHolder" arity)))] 
                  (set! (. holder# ~'signal) (new ~(symbol (str "QSignalEmitter$Signal" arity)) holder#))))))))

(defn signal [arity]
  "Makes a signal of fixed arity. The signals arguments are all Objects so 
   this signal cannot be directly connected to a c++ slot."
  (if (> arity 9)
    (throw (Exception. "Clojure signals cannot have more than 9 args"))
    ((nth signals arity)) ))

; need exception handling in here - by default errors in signals are discarded
; could have an optional logging agent as an arg?
; want to be able to handle normal slots as well?
(defn connect [signal slot]
  "Connect a qt signal to a wrapped clojure function (made with make-slot).
   The signal and slot must have the same arity."
  (.connect signal slot   ; note this is the connect method, not the connect function
    (str 
      "method("  
      (apply str (interpose ", " (replicate (. slot (arity)) "Object")))
      ")")
    (. Qt$ConnectionType QueuedConnection)))

(defn disconnect 
  ([signal] 
  "Disconnect all slots from signal" 
    (.disconnect signal))
  ([signal slot]
  "Disconnect a signal from a slot"
    (.disconnect signal slot   ; note this is the disconnect method, not the disconnect function
      (str 
        "method("  
        (apply str (interpose ", " (replicate (.arity slot) "Object")))
        ")"))))

(defmacro emit [signal & args]
    "Trigger signal with args"
  `(. ~signal (~'emit ~@args))) 