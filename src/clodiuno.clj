(ns clodiuno
  #^{:author "Nurullah Akkaya",
     :doc "Firmata Library for Clojure."}
  (:import (java.io InputStream)
	   (gnu.io SerialPort CommPortIdentifier 
		   SerialPortEventListener SerialPortEvent)))

(def DIGITAL-MESSAGE  0x90) ;;send data for a digital port
(def DIGITAL-MESSAGE-PORT-0  0x90) ;;send data for a digital port
(def DIGITAL-MESSAGE-PORT-1  0x91) ;;send data for a digital port
(def ANALOG-MESSAGE   0xE0) ;;send data for an analog pin (or PWM)
(def REPORT-ANALOG    0xC0) ;;enable analog input by pin #
(def REPORT-DIGITAL   0xD0) ;;enable digital input by port
(def SET-PIN-MODE     0xF4) ;;set a pin to INPUT/OUTPUT/PWM/etc
(def REPORT-VERSION   0xF9) ;;report firmware version
(def SYSTEM-RESET     0xFF) ;;reset from MIDI
(def START-SYSEX      0xF0) ;;start a MIDI SysEx message
(def END-SYSEX        0xF7) ;;end a MIDI SysEx message

(def INPUT 0) ;;pin to input mode
(def OUTPUT 1) ;;pin to output mode
(def HIGH 1) ;;high value (+5 volts) to a pin in a call to digital-write
(def LOW 0) ;;low value (0 volts) to a pin in a call to digital-write
(def baudrate 57600)

;;
;; Serial Setup
;;

(defn- port-identifier [port-name]
  (let [ports (CommPortIdentifier/getPortIdentifiers)]
    (loop [port (.nextElement ports)
	   name (.getName port)]
      (if (= name port-name)
	port (recur (.nextElement ports) (.getName port))))))

(defn- open 
  "Open serial interface."
  [identifier]
  (doto (.open identifier "clojure" 1)
    (.setSerialPortParams baudrate
			  SerialPort/DATABITS_8
			  SerialPort/STOPBITS_1
			  SerialPort/PARITY_NONE)))

(defn close
  "Close serial interface."
  [conn]
  (.close (:port @conn)))

(defn- listener [f]
  (proxy [SerialPortEventListener] [] 
    (serialEvent 
     [event]
     (if (= (.getEventType event) SerialPortEvent/DATA_AVAILABLE)
       (f)))))

;;
;; Firmata Related Calls
;;

(defn enable-pin [conn type pin]
  (let [out (.getOutputStream (:port @conn))] 
    (if (= type :analog)
      (do
	(.write out (bit-or REPORT-ANALOG pin))
	(.write out 1)))
    (if (= type :digital)
      (do
	(.write out (bit-or REPORT-DIGITAL pin))
	(.write out 1)))
    (.flush out)))

(defn disable-pin [conn type pin]
  (let [out (.getOutputStream (:port @conn))] 
    (if (= type :analog)
      (do
	(.write out (bit-or REPORT-ANALOG pin))
	(.write out 0)))
    (if (= type :digital)
      (do
	(.write out (bit-or REPORT-DIGITAL pin))
	(.write out 0)))
    (.flush out)))

(defn pin-mode [conn pin mode]
  (doto (.getOutputStream (:port @conn))
    (.write (byte-array (vector (byte SET-PIN-MODE) (byte pin) (byte mode))))
    (.flush)))

(defn- set-bit [bit-vec index value]
  (let [head (subvec bit-vec 0 index)
	tail (subvec bit-vec (inc index))]
    (apply conj head value tail)))

(defn- to-byte [cmd bit-vec]
  (let [bytes  (split-at 8 (reverse bit-vec))
	first  (BigInteger. (apply str (first bytes)) 2)
	second (BigInteger. (apply str (second bytes)) 2)]
    (vector (byte cmd) (byte first) (byte second))))

(defn digital-write [conn pin value]
  (let [out (.getOutputStream (:port @conn))
	cmd (if (and (> pin 1) (< pin 8)) 
	      DIGITAL-MESSAGE-PORT-0 
	      DIGITAL-MESSAGE-PORT-1)
	state (set-bit (:digital-out-state @conn) pin value)]
    (dosync (alter conn merge {:digital-out-state state}))
    (doto out
      (.write (byte-array (to-byte cmd state)))
      (.flush))))

(defn analog-read [conn pin]
  ((:analog-in-state @conn) pin))

(defn- process-input [conn]
  (let [in (.getInputStream (:port @conn))
	data  (.read in)]
    (cond 
     ;;Multibyte
     (< data 0xF0)
     (let [msg (bit-and data 0xF0)] 
       (cond 
	;;Analog Message
	(= msg ANALOG-MESSAGE)
	(let [pin (bit-and data 0x0F)
	      lsb (.read in)
	      msb (.read in)
	      val (bit-or (bit-shift-left msb 7) lsb)
	      state (set-bit (:analog-in-state @conn) pin val)]
	  (dosync (alter conn merge {:analog-in-state state})))
	;;Digital Message
	(= msg DIGITAL-MESSAGE)
	(let [pin (bit-and data 0x0F)
	      lsb (.read in)
	      msb (.read in)
	      val (bit-or (bit-shift-left msb 7) lsb)
	      state (set-bit (:digital-in-state @conn) pin val)]
	  (dosync (alter conn merge {:digital-in-state state})))))
     (= data REPORT-VERSION)
     (dosync
      (alter conn merge {:version {:major (.read in) :minor (.read in)}})))))

(defn arduino [p]
  (let [port (open (port-identifier p))
	conn (ref {:port port
		   :digital-out-state [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]
		   :digital-in-state  [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]
		   :analog-in-state   [0 0 0 0 0 0]})]
    (.addEventListener port (listener #(process-input conn)))
    (.notifyOnDataAvailable port true)
    conn))
