(ns led
  (:use :reload-all clodiuno.core))

(def board (arduino "/dev/tty.usbserial-A600aeCj"))

;;allow arduino to boot
(Thread/sleep 5000)

(pin-mode board 13 OUTPUT)

(doseq [_ (range 5)] 
  (digital-write board 13 HIGH)
  (Thread/sleep 1000)
  (digital-write board 13 LOW)
  (Thread/sleep 1000))

(close board)
