(ns clodiuno.firmata
  (:refer-clojure :exclude [byte])
  #^{:author "Nurullah Akkaya",
     :doc "Firmata Library for Clojure."}
  (:use clodiuno.core)
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

(defn- byte [v]
  (.byteValue (- v 256)))

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
	   (.close (:port @conn)))

(defn- listener 
  "f will be called whenever there is data availible on the stream."
  [f]
  (proxy [SerialPortEventListener] [] 
    (serialEvent 
     [event]
     (if (= (.getEventType event) SerialPortEvent/DATA_AVAILABLE)
       (f)))))

(defn bits [n]
  (map #(bit-and (bit-shift-right n %) 1) (range 8)))

(defn- assoc-in! [r ks v]
  (dosync (alter r assoc-in ks v)))

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
                 (.write out (bit-or REPORT-DIGITAL (if (< pin 8) 0 1)))
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
                 (.write out (bit-or REPORT-DIGITAL (if (< pin 8) 0 1)))
                 (.write out 0)))
	     (.flush out)))

(defmethod pin-mode :firmata [conn pin mode]
	   (doto (.getOutputStream (:port @conn))
	     (.write (byte-array (vector (byte SET-PIN-MODE) (byte pin) (byte mode))))
	     (.flush)))

(defmethod digital-write :firmata [conn pin value]
           (throw (Exception. "Digital Write Temporarily Disabled.")))

(defmethod digital-read :firmata [conn pin]
           (let [port (int (/ pin 8))
                 vals ((@conn :digital-in) port)]
             (first (drop (mod pin 8) vals))))

(defmethod analog-read :firmata [conn pin]
           ((@conn :analog) pin))

(defmethod analog-write :firmata [conn pin val]
	   (doto (.getOutputStream (:port @conn))
	     (.write (bit-or 0xE0 (bit-and pin 0x0F)))
	     (.write (bit-and val 0x7F))
	     (.write (bit-shift-right val 7))
	     (.flush)))

(defn- read-multibyte [in]
  (let [lsb (.read in)
        msb (.read in)
        val (bit-or (bit-shift-left msb 7) lsb)]
    [lsb msb val]))

(defn- process-input 
  "Parse input from firmata."
  [conn in]
  (while (> (.available in) 2)
    (let [data (.read in)]
      (cond
       (= (bit-and data 0xF0) ANALOG-MESSAGE) (let [pin (bit-and data 0x0F)
                                                    [_ _ val] (read-multibyte in)]
                                                (assoc-in! conn [:analog pin] val))
       
       (= (bit-and data 0xF0) DIGITAL-MESSAGE) (let [port (bit-and data 0x0F)
                                                     [lsb msb val] (read-multibyte in)]
                                                 (assoc-in! conn [:digital-in port] (bits val)))

       (= data REPORT-VERSION) (assoc-in! conn [:version] [(.read in) (.read in)])))))

(defmethod arduino :firmata [type port]
	   (let [port (open (port-identifier port))
		 conn (ref {:port port :interface :firmata})]
             
             (doto port
               (.addEventListener (listener #(process-input conn (.getInputStream (:port @conn)))))
               (.notifyOnDataAvailable true))
             
	     (while (nil? (:version @conn))
               (Thread/sleep 100))
	     conn))
