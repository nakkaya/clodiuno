(defproject clodiuno "0.0.3-SNAPSHOT"
  :description "Clojure API for Arduino."
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :native-dependencies [[org.clojars.nakkaya/rxtx-macosx-native-deps "2.1.7"]
                        [org.clojars.mschristiansen/rxtx-linux-native-deps "2.1.7"]]
  :dev-dependencies [[native-deps "1.0.5"]]
  :jvm-opts ["-Djava.library.path=./native/linux/x86/:./native/macosx/x86/"
             "-d32"])
