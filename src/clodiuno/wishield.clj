(ns clodiuno.wishield
  #^{:author "Nurullah Akkaya",
     :doc "WiShield Library for Clojure."}
  (:use clodiuno.core)
  (:import (java.text DecimalFormat)
	   (java.net Socket)
	   (java.io PrintWriter InputStreamReader BufferedReader)))

;;Pins 10,11,12,13 are "mandatory" for SPI communications 
;;as is pin 2 OR 8 depending on your jumper setting.
;;Pin 9 (LED) and Pin 7 (Dataflash) can be freed by removing the jumper.

(def pin-format (DecimalFormat. "00"))
(def pwm-format (DecimalFormat. "000"))

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

(defmethod arduino :wishield [interface ip port]
  (let [socket (Socket. ip port)
	in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
	out (PrintWriter. (.getOutputStream socket))]
    (.readLine in)
    {:interface :wishield :in in :out out :socket socket}))
