(ns gridlife.gridmodel)

(defn random-int [min max]
  (+ min (.floor js/Math (* (.random js/Math) (+ 1 (- max min)))))
  )

(defn random-grid-coord [model]
  (let [coords (keys model)
        size (count coords)
        rand-coord (nth coords (random-int 0 (- size 1)))
        ]
    rand-coord
    )
  )

(defn toggle [color]
  (if (= color :white) :black :white))

(defn populate-grid [cells-wide cells-high]
  (let [keys (for [x (range 0 cells-wide)
                   y (range 0 cells-high)]
               {:x x, :y y})]
    (zipmap keys (repeat :white)))
  )