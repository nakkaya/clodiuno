(ns pwm
  (:use :reload-all clodiuno.core)
  (:use :reload-all clodiuno.wishield))

;; Potentiometer connected to pin 5
;; LED connected to pin 3

(defn map-int [x in-min in-max out-min out-max]
  (+ (/ (* (- x in-min) (- out-max out-min)) (- in-max in-min)) out-min))

(def board (arduino :wishield "10.0.2.100" 1000))

(pin-mode board 3 PWM)
(pin-mode board 5 ANALOG)

(while true 
       (analog-write board 3 (map-int (analog-read board 5) 0 1023 0 255)))

;;(close board)
