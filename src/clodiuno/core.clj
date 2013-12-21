(ns clodiuno.core)

(def INPUT 0) ;;pin to input mode
(def OUTPUT 1) ;;pin to output mode
(def ANALOG 2) ;;pin to analog mode
(def PWM 3) ;; pin to PWM mode
(def SERVO 4) ;; attach servo to pin (pins 2 - 13)
(def HIGH 1) ;;high value (+5 volts) to a pin in a call to digital-write
(def LOW 0) ;;low value (0 volts) to a pin in a call to digital-write

(defmulti arduino
  "Connect to board."
  (fn [type & _] type))

(defmulti enable-pin 
  "Tell firmware to start sending pin readings."
  (fn [type _ _] (type :interface)))

(defmulti disable-pin 
  "Tell firmware to stop sending pin readings."
  (fn [type _ _] (type :interface)))

(defmulti pin-mode 
  "Configures the specified pin to behave either as an input or an output."
  (fn [type _ _] (type :interface)))

(defmulti digital-write 
  "Write a HIGH or a LOW value to a digital pin."
  (fn [type _ _] (type :interface)))

(defmulti digital-read
  "Read a HIGH or a LOW value from a digital pin."
  (fn [type _] (type :interface)))

(defmulti analog-read 
  "Reads the value from the specified analog pin."
  (fn [type _] (type :interface)))

(defmulti analog-write 
  "Write an analog value (PWM-wave) to a digital pin."
  (fn [type _ _] (type :interface)))

(defmulti set-sampling-interval
  "Set the Sampling Interval, that is, how often analog data and I2C data is reported to the client"
  (fn [type _] (type :interface)))

(defmulti i2c-init
  "You need to call this before doing ANY I2C work.
   delay: microseconds to wait between receiving a firmata read
   request and sending it to the slave. Needed by some devices like
   WiiNunchuck. Default is 19ms"
  (fn [type & _] (type :interface)))

(defmulti i2c-blocking-read
  "Make a single read request to a I2C device in a blocking way"
  (fn [type & _] (type :interface)))

(defmulti i2c-write
  "Write to I2C device"
  (fn [type & _] (type :interface)))

(defmulti i2c-start-reading
  "Read continously from I2C device. This will register an slave-addr+register
   entry in the Board. The board then issues I2C read requests at sampling-interval
   freq, and reports back to the host. You can access these lectures using i2c-read.
   Will read indefinitely until i2c-stop-reading is called for that particular device. "
  (fn [type & _] (type :interface)))

(defmulti i2c-stop-reading
  "Stop reading continuously from I2C device"
  (fn [type & _] (type :interface)))

(defmulti i2c-read
  "Read from I2C device. Before using this function you need to start reading using i2c-start-reading"
  (fn [type _ _] (type :interface)))

(defmulti close
  "Close serial interface."
  (fn [type] (type :interface)))
