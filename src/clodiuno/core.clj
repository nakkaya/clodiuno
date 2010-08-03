(ns clodiuno.core)

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

;; (defmulti digital-read
;;   "Read a HIGH or a LOW value from a digital pin."
;;   (fn [type] ((meta type) :interface)))

;; (defmulti analog-read 
;;   "Reads the value from the specified analog pin."
;;   (fn [type] ((meta type) :interface)))

;; (defmulti analog-write 
;;   "Write an analog value (PWM-wave) to a digital pin."
;;   (fn [type] ((meta type) :interface)))

(defmulti close
  "Close serial interface."
  (fn [type] (type :interface)))
