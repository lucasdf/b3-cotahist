(defproject b3-cotahist "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :main b3-cotahist.core
  :aot [b3-cotahist.core]
  :uberjar-name "b3-cotahist.jar"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/data.json "1.0.0"]]
  :profiles {:dev {:dependencies [[criterium "0.4.6"]
                                  [com.taoensso/tufte "2.2.0"]]}}
  :jvm-opts ["-Xverify:none"]
  :repl-options {:init-ns b3-cotahist.core})
