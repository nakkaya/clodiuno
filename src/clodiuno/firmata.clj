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

;; SYSEX extended commands
(def RESERVED-COMMAND        0x00) ;;2nd SysEx data byte is a chip-specific command (AVR, PIC, TI, etc).
(def ANALOG-MAPPING-QUERY    0x69) ;;ask for mapping of analog to pin numbers
(def ANALOG-MAPPING-RESPONSE 0x6A) ;;reply with mapping info
(def CAPABILITY-QUERY        0x6B) ;;ask for supported modes and resolution of all pins
(def CAPABILITY-RESPONSE     0x6C) ;;reply with supported modes and resolution
(def PIN-STATE-QUERY         0x6D) ;;ask for a pin's current mode and value
(def PIN-STATE-RESPONSE      0x6E) ;;reply with a pin's current mode and value
(def EXTENDED-ANALOG         0x6F) ;;analog write (PWM, Servo, etc) to any pin
(def SERVO-CONFIG            0x70) ;;set max angle, minPulse, maxPulse, freq
(def STRING-DATA             0x71) ;;a string message with 14-bits per char
(def SHIFT-DATA              0x75) ;;shiftOut config/data message (34 bits)
(def I2C-REQUEST             0x76) ;;I2C request messages from a host to an I/O board
(def I2C-REPLY               0x77) ;;I2C reply messages from an I/O board to a host, only for read/read-continously
(def I2C-CONFIG              0x78) ;;Configure special I2C settings such as power pins and delay times
(def REPORT-FIRMWARE         0x79) ;;report name and version of the firmware
(def SAMPLING-INTERVAL       0x7A) ;;sampling interval
(def SYSEX-NON-REALTIME      0x7E) ;;MIDI Reserved for non-realtime messages
(def SYSEX-REALTIME          0x7F) ;;MIDI Reserved for realtime messages

;; Taken from StandardFirmata.ino
(def I2C-WRITE                   2r00000000)
(def I2C-READ                    2r00001000)
;; read-continously indicates that the firmware should continuously
;; read the device at the rate specified by the sampling interval.
;; firmware implementation should support read continuous mode for
;; several I2C devices simultaneously. Sending the stop reading
;; command will end read continuous mode for that particular device.
(def I2C-READ-CONTINUOUSLY       2r00010000)
(def I2C-STOP-READING            2r00011000)
(def I2C-READ-WRITE-MODE-MASK    2r00011000)
(def I2C-10BIT-ADDRESS-MODE-MASK 2r00100000)


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

(defn- lsb [b]
  (bit-and b 0x7F))

(defn- msb [b]
  (bit-and (bit-shift-right b 7) 0x7F))

(defn- bytes-to-int [lsb msb]
  (bit-or (bit-shift-left (bit-and msb 0x7F) 7) 
          (bit-and lsb 0x7F)))

(defn- write-data [conn data]
  (when (not (empty? data))
    (apply write-bytes conn 
           (mapcat (fn [b] [(lsb b) (msb b)])
                   data))))

(defn- bits [n]
  (map #(bit-and (bit-shift-right n %) 1) (range 8)))

(defn- numb [bits]
  (int (BigInteger. (apply str bits) 2)))

(defn- assoc-in! [r ks v]
  (dosync (alter r assoc-in ks v)))

(defn- i2c-request [conn slave-addr mode data]
   {:pre  [(<= slave-addr 127)]} ;; Current arduino firmata doesn't support 10-bit addressing. 
   (doto conn
       (write-bytes START-SYSEX 
                    I2C-REQUEST 
                    (lsb slave-addr)
                    (bit-or (bit-and (msb slave-addr) 
                                     (bit-not I2C-READ-WRITE-MODE-MASK)) 
                            mode)
                    )
       (write-data  data)
       (write-bytes END-SYSEX)
       )
   )

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

;; default 19 (ms)
(defmethod set-sampling-interval :firmata [conn delay]
  (write-bytes conn START-SYSEX SAMPLING-INTERVAL (lsb delay) (msb delay) END-SYSEX)
)

;; default delay = 19 (ms)
(defmethod i2c-init :firmata [conn & {:keys [delay] :or {delay 19}}]
  (write-bytes conn START-SYSEX I2C-CONFIG (lsb delay) (msb delay) END-SYSEX))

(defmethod i2c-blocking-read :firmata [conn slave-addr register bytes-to-read  & {:keys [timeout]}]
  ;; Note, Firmata protocol doesn't allows you to distingish between
  ;; read replies and read-continously reports, both are marked as I2C_REPLY
  ;; If a report comes for that slave-addr/register it will be taken
  ;; as the response of the i2c-blocking-read command.
  ;;
  ;; Register can be nil
  (let [reply (promise)]
    (assoc-in! conn [:i2c :last-blocking-read] {:slave-addr slave-addr
                                               :register   (or register -1)
                                               :response   reply})
    
    (i2c-request conn slave-addr I2C-READ (if register [register bytes-to-read] [bytes-to-read]))
    (if timeout
      (deref reply timeout nil)
      @reply)))
  
(defmethod i2c-write :firmata 
  ([conn slave-addr data]          (i2c-request conn slave-addr I2C-WRITE data))
  ([conn slave-addr register data] (i2c-request conn slave-addr I2C-WRITE (concat [register] data))))

(defmethod i2c-start-reading :firmata [conn slave-addr register bytes-to-read]
  (i2c-request conn slave-addr I2C-READ-CONTINUOUSLY [register bytes-to-read]))

(defmethod i2c-stop-reading :firmata [conn slave-addr]
  (i2c-request conn slave-addr I2C-STOP-READING []))

(defmethod i2c-read :firmata [conn slave-addr register]
  (get-in @conn [:i2c slave-addr register]))


(defn- read-multibyte [in]
  (let [lsb (.read in)
        msb (.read in)
        val (bit-or (bit-shift-left msb 7) lsb)]
    [lsb msb val]))

(defn- read-to-sysex-end [in]
  (loop [buffer    []]
    (let [b (.read in)]
      (if (= b END-SYSEX) 
        buffer
        (recur (conj buffer b))))))

(defn- multibytes-to-ints [list]
  (for [[lsb msb] (partition 2 list)] (bytes-to-int lsb msb)))

(defn- handle-sysex [conn in]
  (let [cmd (.read in)]
    (cond
     (= cmd REPORT-FIRMWARE) (let [major-version (.read in)
                                   minor-version (.read in)
                                   firmware-name (apply str (map char (read-to-sysex-end in)))]
                               (assoc-in! conn [:firmware] {:version [major-version minor-version] 
                                                            :name firmware-name}))
     (= cmd STRING-DATA) (let [packet       (read-to-sysex-end in)
                               msg          (apply str (map char packet))
                               msg-callback (get-in @conn [:callbacks :msg])]
                           (when msg-callback
                             (msg-callback msg)))

     (= cmd I2C-REPLY) (let [packet    (read-to-sysex-end in)
                             [slave-addr
                              register
                              & data] (multibytes-to-ints packet)
                             r        (get-in @conn [:i2c :last-blocking-read])]
                         (if (and r (= (:slave-addr r) slave-addr)
                                    (= (:register   r) register)
                                    (not (realized? (:response r))))
                           (deliver (:response r) data)
                           (assoc-in! conn [:i2c slave-addr register] data))
                         )
     :unknown-cmd  (read-to-sysex-end in) ;; discard
     )))

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

       (= data REPORT-VERSION) (assoc-in! conn [:version] [(.read in) (.read in)])

       (= data START-SYSEX) (handle-sysex conn in)))))


(defmethod arduino :firmata [type port & {:keys [baudrate msg-callback] :or {baudrate 57600}}]
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

    (assoc-in! conn [:i2c] {:last-blocking-read nil})
    (assoc-in! conn [:callbacks] {:msg msg-callback})

    conn))
