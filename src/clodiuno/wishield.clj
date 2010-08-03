(ns clodiuno.wishield
  #^{:author "Nurullah Akkaya",
     :doc "WiShield Library for Clojure."}
  (:use clodiuno.core :reload-all)
  (:import (java.net Socket)
	   (java.io PrintWriter InputStreamReader BufferedReader)))

(derive ::wishield ::interface)

(def INPUT 0) ;;pin to input mode
(def OUTPUT 1) ;;pin to output mode
(def ANALOG 2) ;;pin to analog mode
(def PWM 3) ;; pin to PWM mode
(def SERVO 4) ;; attach servo to pin (pins 2 - 13)
(def HIGH 1) ;;high value (+5 volts) to a pin in a call to digital-write
(def LOW 0) ;;low value (0 volts) to a pin in a call to digital-write

(defn- send-command [conn cmd]
  (doto (:out conn)
    (.println cmd)
    (.flush))
  (.readLine (:in conn)))

(defmethod enable-pin :wishield [board type pin])

(defmethod disable-pin :wishield [board type pin])

(defmethod pin-mode :wishield [board pin mode]
  (let [pin (if (< pin 10) (str "0" pin) (str pin))
	mode (cond (= mode INPUT) "i"
		   (= mode OUTPUT) "o"
		   (= mode ANALOG) "a"
		   (= mode PWM) "p"
		   (= mode SERVO) "s"
		   :default (throw (Exception. "Invalid Mode.")))]
    (send-command board (str "pm" pin mode))))

(defmethod digital-write :wishield [board pin value]
  (let [pin (if (< pin 10) (str "0" pin) (str pin))
	value (cond (= value HIGH) "h"
		   (= value LOW) "l"
		   :default (throw (Exception. "Invalid Value.")))]
    (send-command board (str "dw" pin value))))

(defmethod close :wishield [board]
  (let [{:keys [in out socket]} board]
    (send-command board "bye")
    (.close in)
    (.close out)
    (.close socket)))

(defn arduino [ip port]
  (let [socket (Socket. ip port)
	in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
	out (PrintWriter. (.getOutputStream socket))]
    (.readLine in)
    {:interface :wishield :in in :out out :socket socket}))


(comment 
  (def board (arduino "10.0.2.100" 1000))

  (pin-mode board 6 OUTPUT)
  (digital-write board 6 HIGH)
  (digital-write board 6 LOW)

  (close board)
  )
