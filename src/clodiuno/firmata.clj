(ns clodiuno.firmata
  #^{:author "Nurullah Akkaya",
     :doc "Firmata Library for Clojure."}
  (:use clodiuno.core)
  (:use clodiuno.constants)
  (:import (java.io InputStream)
	   (gnu.io SerialPort CommPortIdentifier 
		   SerialPortEventListener SerialPortEvent 
		   NoSuchPortException)))

(def DIGITAL-MESSAGE  0x90) ;;send data for a digital port
(def ANALOG-MESSAGE   0xE0) ;;send data for an analog pin (or PWM)
(def REPORT-ANALOG    0xC0) ;;enable analog input by pin #
(def REPORT-DIGITAL   0xD0) ;;enable digital input by port
(def SET-PIN-MODE     0xF4) ;;set a pin to INPUT/OUTPUT/PWM/etc
(def REPORT-VERSION   0xF9) ;;report firmware version
(def SYSTEM-RESET     0xFF) ;;reset from MIDI
(def START-SYSEX      0xF0) ;;start a MIDI SysEx message
(def END-SYSEX        0xF7) ;;end a MIDI SysEx message
(def baudrate 57600)

(defn- on-thread [f]
  (doto (Thread. #^Runnable f) 
    (.start)))

;;
;; Serial Setup
;;

(defn- port-identifier 
  "Given a port name return its identifier."
  [port-name]
  (try
   (let [ports (CommPortIdentifier/getPortIdentifiers)]
     (loop [port (.nextElement ports)
	    name (.getName port)]
       (if (= name port-name)
	 port (recur (.nextElement ports) (.getName port)))))
   (catch Exception e (throw (NoSuchPortException.)))))

(defn- open 
  "Open serial interface."
  [identifier]
  (doto (.open identifier "clojure" 1)
    (.setSerialPortParams baudrate
			  SerialPort/DATABITS_8
			  SerialPort/STOPBITS_1
			  SerialPort/PARITY_NONE)))

(defmethod close :firmata [conn]
  (dosync (alter conn merge {:thread false}))
  (.close (:port @conn)))

(defn- listener 
  "f will be called whenever there is data availible on the stream."
  [f]
  (proxy [SerialPortEventListener] [] 
    (serialEvent 
     [event]
     (if (= (.getEventType event) SerialPortEvent/DATA_AVAILABLE)
       (f)))))

;;
;; Firmata Related Calls
;;

(defmethod enable-pin :firmata [conn type pin]
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

(defmethod disable-pin :firmata [conn type pin]
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

(defmethod pin-mode :firmata [conn pin mode]
  (doto (.getOutputStream (:port @conn))
    (.write (byte-array (vector (byte SET-PIN-MODE) (byte pin) (byte mode))))
    (.flush)))

(defn- set-bit 
  "Given a vector of bits set the bit at the index to value"
  [bit-vec index value]
  (let [head (subvec bit-vec 0 index)
	tail (subvec bit-vec (inc index))]
    (apply conj head value tail)))

(defn- to-byte 
  "Produce digital write command, by combining cmd and bit vector."
  [cmd bit-vec]
  (let [bytes  (split-at 8 (reverse bit-vec))
	first  (BigInteger. (apply str (first bytes)) 2)
	second (BigInteger. (apply str (second bytes)) 2)]
    (vector (byte cmd) (byte first) (byte second))))

(defmethod digital-write :firmata [conn pin value]
  (let [out   (.getOutputStream (:port @conn))
	cmd   (bit-or (bit-and (bit-shift-right pin 3) 0x0F) DIGITAL-MESSAGE)
	state (set-bit (:digital-out-state @conn) pin value)]
    (dosync (alter conn merge {:digital-out-state state}))
    (doto out
      (.write (byte-array (to-byte cmd state)))
      (.flush))))

(defmethod analog-read :firmata [conn pin]
  ((:analog-in-state @conn) pin))

(defmethod analog-write :firmata [conn pin val]
  (doto (.getOutputStream (:port @conn))
    (.write (bit-or 0xE0 (bit-and pin 0x0F)))
    (.write (bit-and val 0x7F))
    (.write (bit-shift-right val 7))
    (.flush)))

(defn- process-input 
  "Parse input from firmata."
  [conn in]
  (while
   (:thread @conn)
   (if-not (= 0 (.available in))
     (let [data  (.read in)]
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
	 (alter conn merge 
		{:version {:major (.read in) :minor (.read in)}})))))))

(defmethod arduino :firmata [type port]
  (let [port (open (port-identifier port))
	conn (ref {:port port
		   :digital-out-state [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]
		   :digital-in-state  [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]
		   :analog-in-state   [0 0 0 0 0 0]
		   :thread true
		   :interface :firmata})]
    (on-thread #(process-input conn (.getInputStream (:port @conn))))
    conn))
