(defproject clodiuno "0.0.1-SNAPSHOT"
  :description "Clojure API for the firmata protocol."
  :dependencies [[org.clojure/clojure "1.1.0"]
		 [org.clojure/clojure-contrib "1.1.0"]]
  :native-dependencies 
  [[org.clojars.nakkaya/rxtx-macosx-native-deps "2.1.7"]]
  :dev-dependencies [[native-deps "1.0.0"]
                     [lein-clojars "0.5.0-SNAPSHOT"]])
