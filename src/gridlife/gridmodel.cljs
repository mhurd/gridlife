(ns gridlife.gridmodel)

(defrecord GridModel [model visited-counts cells-wide cells-high langton-ant])

(def index->headings {0 :north
                      1 :east
                      2 :south
                      3 :west})

(def headings->index {:north 0
                      :east 1
                      :south 2
                      :west 3})

(defn- turn [heading f]
  (let [current-index (get headings->index heading)
        index (f current-index 1)
        new-index (mod index 4)
        new-heading (get index->headings new-index)]
    new-heading
    ))

(defn- turn-right [heading]
  (turn heading +))

(defn- turn-left [heading]
  (turn heading -))

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

(defn populate-grid [gridmodel]
  (let [keys (for [x (range 0 (:cells-wide gridmodel))
                   y (range 0 (:cells-high gridmodel))]
               {:x x, :y y})]
    (assoc gridmodel :model (zipmap keys (repeat :white))))
  )

(defn compass [gridmodel location cells]
  (let [cells-wide (:cells-wide gridmodel)
        cells-high (:cells-high gridmodel)
        old-x (:x location)
        old-y (:y location)]
    {:north {:x old-x :y (mod (- old-y cells) cells-high)}
     :south {:x old-x :y (mod (+ old-y cells) cells-high)}
     :east {:x (mod (+ old-x cells) cells-wide) :y old-y}
     :west {:x (mod (- old-x cells) cells-wide) :y old-y}
     }
    ))

(defn new-location [gridmodel location heading cells]
  (get (compass gridmodel location cells) heading)
  )