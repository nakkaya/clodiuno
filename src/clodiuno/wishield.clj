(ns clodiuno.wishield
  #^{:author "Nurullah Akkaya",
     :doc "WiShield Library for Clojure."}
  (:use clodiuno.core :reload-all)
  (:import (java.text DecimalFormat)
	   (java.net Socket)
	   (java.io PrintWriter InputStreamReader BufferedReader)))

(def pin-format (DecimalFormat. "00"))
(def pwm-format (DecimalFormat. "000"))

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
  (let [pin (.format pin-format pin)
	mode (cond (= mode INPUT) "i"
		   (= mode OUTPUT) "o"
		   (= mode ANALOG) "a"
		   (= mode PWM) "p"
		   (= mode SERVO) "s"
		   :default (throw (Exception. "Invalid Mode.")))]
    (send-command board (str "pm" pin mode))))

(defmethod digital-write :wishield [board pin value]
  (let [pin (.format pin-format pin)
	value (cond (= value HIGH) "h"
		   (= value LOW) "l"
		   :default (throw (Exception. "Invalid Value.")))]
    (send-command board (str "dw" pin value))))

(defmethod analog-write :wishield [board pin value]
  (send-command 
   board (str "aw" (.format pin-format pin) (.format pwm-format value))))

(defmethod analog-read :wishield [board pin]
  (read-string
   (send-command board (str "ar" (.format pin-format pin)))))

(defmethod digital-read :wishield [board pin]
  (read-string
   (send-command board (str "dr" (.format pin-format pin)))))

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

  (pin-mode board 5 SERVO)
  (analog-write board 5 0)

  (do 
    (pin-mode board 3 OUTPUT)
    (pin-mode board 6 INPUT)
    (while true 
	   (let [i (digital-read board 6)] 
	     (println i)
	     (digital-write board 3 i))))

  (let [map (fn [x in-min in-max out-min out-max]
		(+ (/ (* (- x in-min) (- out-max out-min)) 
		      (- in-max in-min)) out-min))]
      (pin-mode board 3 PWM)
      (pin-mode board 5 ANALOG)
      
      (while true (analog-write board 3 
				(map (analog-read board 5) 0 1023 0 255))))

  (close board)
  )
