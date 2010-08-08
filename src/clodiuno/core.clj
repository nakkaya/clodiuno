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

(defmulti close
  "Close serial interface."
  (fn [type] (type :interface)))
