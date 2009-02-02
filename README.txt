 *   GitDoc
 *
 *   Copyright (c) Lau B. Jensen. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file LICENSE.txt at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 *   the terms of this license.
 *   You must not remove this notice, or any other, from this software.

GitDoc was booted on 1. February 2009.

---------------------------------------------

GitDoc aims to accomplish two things.

1)  To provide a flexible frontend, to a solid backend which parses logs from git repositories.
   	The flexibility is needed, to enable the user to make use of his own naming conventions, in order
	to produce quality documentation solely from Git logs.

2)	To be the first project using a consolidated set of bindings for Jambi to Clojure.	
	Jambi is the JVM port of the standard Qt libs.

	Much of the hard work in porting Jambi was intially made by Jamie Brandon, a lone gunman who appeared
	on the clojure scene briefly, never to be seen again. Before he packed up, I got a snapshot of his initial	
	experiments with Jambi.
	
	Chouser has also provided, as he always does, quality sparring and source code snippets that may of may not appear
	in this distribution.

TO USE:

First, download clojure from Clojure.org, then modify the path to the UI in
(main-qt) in engine.clj.


Linux)
Then from the CLI:

$ java -cp clojure.jar:/path/to/gitdoc/src/ clojure.lang.Repl

user> (use 'dk.bestinclass.gitdoc)
nil
user> (in-ns 'dk.bestinclass.gitdoc)
nil
dk.bestinclass.gitdoc> (init)
nil
dk.bestinclass.gitdoc> (main-qt)

Windows)
1) Install Linux
2) Follow recommendations above




/Lau B. Jensen/

