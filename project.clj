(defproject clodiuno "0.0.2-SNAPSHOT"
  :description "Clojure API for Arduino."
  :dependencies [[org.clojure/clojure "1.2.0"]]
  :native-dependencies 
  [[org.clojars.nakkaya/rxtx-macosx-native-deps "2.1.7"]]
  :dev-dependencies [[native-deps "1.0.5"]
                     [lein-clojars "0.5.0-SNAPSHOT"]]
  :jvm-opts ["-Djava.library.path=./native/macosx/x86/"
             "-d32"])
