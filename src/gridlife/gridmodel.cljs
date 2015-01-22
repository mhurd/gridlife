(ns gridlife.gridmodel)

(defn random-int [min max]
  (+ min (.floor js/Math (* (.random js/Math) (+ 1 (- max min)))))
  )

(defn random [size]
  (random-int 0, (- size 1))
  )

(defn random-grid-coord [xsize ysize]
  {:x (random xsize) :y (random ysize)}
  )

(defn white? [contents]
  (= contents :white)
  )

(defn black? [contents]
  (= contents :black)
  )

(defn toggle-color [color]
  (if (white? color) :black :white)
  )

(defn color? [contents]
  (or (white? contents) (black? contents))
  )

(defn toggle-if-color [contents]
  (if (color? contents) (toggle-color contents) contents))

(defn populate-grid [cells-wide cells-high]
  (let [keys (for [x (range 0 cells-wide)
                   y (range 0 cells-high)]
               {:x x, :y y})]
    (zipmap keys (repeat :white)))
  )