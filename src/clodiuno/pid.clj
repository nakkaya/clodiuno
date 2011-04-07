(ns clodiuno.pid)

(defn scale [x in-min in-max out-min out-max]
  (+ (/ (* (- x in-min) (- out-max out-min)) (- in-max in-min)) out-min))

(defn clamp [x min max]
  (cond
   (> x max) max
   (< x min) min
   :default x))

(defn pid
  ([s]
     (ref (assoc s :integrator 0 :derivator 0)))
  ([s v]
     (let [{:keys [set-point kp kd ki integrator derivator bounds]} @s
	   [in-min in-max out-min out-max] bounds
	   v (scale (clamp v in-min in-max) in-min in-max -1.0 1.0)
	   sp (scale (clamp set-point in-min in-max) in-min in-max -1.0 1.0)
	   error (- sp v)
	   p-val (* kp error)
	   d-val (* kd (- error derivator))
	   integrator (clamp (+ integrator error) -1.0 1.0)
	   i-val (* integrator ki)
	   pid (scale (clamp (+ p-val i-val d-val) -1.0 1.0)
		      -1.0 1.0 out-min out-max)]
       (dosync (alter s assoc :integrator integrator :derivator error))
       pid)))
