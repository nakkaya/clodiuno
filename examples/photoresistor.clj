(ns photoresistor
  (:use :reload-all clodiuno))

;; refer
;; http://nakkaya.com/2009/10/29/connecting-a-photoresistor-to-an-arduino/
;; for circuit diagram.

(def photo-pin 0)
(def led-pin 13)
(def threshold 250)
(def board (arduino "/dev/tty.usbserial-A6008nhh"))

;;allow arduino to boot
(Thread/sleep 5000)

(pin-mode board 13 OUTPUT)

;;start receiving data for photo-pin
(enable-pin board :analog photo-pin)

(doseq [_ (range 1000000)] 
  (let [val (analog-read board photo-pin)] 
    (if (> val threshold)
      (digital-write board led-pin HIGH)
      (digital-write board led-pin LOW))))

(close board)
