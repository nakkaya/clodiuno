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

(def arduino-port-count 7)

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
  [identifier baudrate]
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

(defn- write-bytes [conn & bs]
  (let [out (.getOutputStream (:port @conn))]
    (doseq [b bs]
      (.write out b))
    (.flush out)))

(defn- bits [n]
  (map #(bit-and (bit-shift-right n %) 1) (range 8)))

(defn- numb [bits]
   (int (BigInteger. (apply str bits) 2)))

(defn- assoc-in! [r ks v]
  (dosync (alter r assoc-in ks v)))

;;
;; Firmata Calls
;;

(defmethod enable-pin :firmata [conn type pin]
           (cond (= type :analog) (write-bytes conn (bit-or REPORT-ANALOG pin) 1)
                 (= type :digital) (write-bytes conn (bit-or REPORT-DIGITAL (int (/ pin 8))) 1)
                 :default (throw (Exception. "Unknown pin type."))))

(defmethod disable-pin :firmata [conn type pin]
	   (cond (= type :analog) (write-bytes conn (bit-or REPORT-ANALOG pin) 0)
                 (= type :digital) (write-bytes conn (bit-or REPORT-DIGITAL (int (/ pin 8))) 0)
                 :default (throw (Exception. "Unknown pin type."))))

(defmethod pin-mode :firmata [conn pin mode]
           (write-bytes conn SET-PIN-MODE pin mode))

(defmethod digital-write :firmata [conn pin value]
           (let [port (int (/ pin 8))
                 vals ((@conn :digital-out) port)
                 beg (take (mod pin 8) vals)
                 end (drop (inc (mod pin 8)) vals)
                 state (concat beg [value] end)]
             (assoc-in! conn [:digital-out port] state)
             (write-bytes conn (bit-or DIGITAL-MESSAGE port) (numb (reverse state)) 0)))

(defmethod digital-read :firmata [conn pin]
           (let [port (int (/ pin 8))
                 vals ((@conn :digital-in) port)]
             (first (drop (mod pin 8) vals))))

(defmethod analog-read :firmata [conn pin]
           ((@conn :analog) pin))

(defmethod analog-write :firmata [conn pin val]
           (write-bytes conn (bit-or ANALOG-MESSAGE (bit-and pin 0x0F)) (bit-and val 0x7F) (bit-shift-right val 7)))

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

(defmethod arduino :firmata [type port & {:keys [baudrate] :or {baudrate 57600}}]
	   (let [port (open (port-identifier port) baudrate)
		 conn (ref {:port port :interface :firmata})]

             (doto port
               (.addEventListener (listener #(process-input conn (.getInputStream (:port @conn)))))
               (.notifyOnDataAvailable true))

             (write-bytes conn REPORT-VERSION)

	     (while (nil? (:version @conn))
               (Thread/sleep 100))

             (dotimes [i arduino-port-count]
               (assoc-in! conn [:digital-out i] (repeat 8 0)))

             (dotimes [i arduino-port-count]
               (assoc-in! conn [:digital-in i] (repeat 8 0)))

	     conn))
