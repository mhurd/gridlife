;; ## Models the grid
;;
;; Defines a record that holds the model (a map of location to contents), the size in cells (width
;; and height) and finally the Langton Ant model (this should be factored out).
(ns gridlife.gridmodel)

;; This record type represents the grid, it holds a model of location to contents mappings and
;; the width and height of the grid.
(defrecord GridModel [model cells-wide cells-high])

;; Maps a numeric index to the 4 compass points.
(def index->headings {0 :north
                      1 :east
                      2 :south
                      3 :west})

;; Maps a compass point to a numeric index.
(def headings->index {:north 0
                      :east  1
                      :south 2
                      :west  3})

(defn- turn
  "Turns 90-degrees from a compass heading using the specified function
  ```-``` for left and ```+``` for right"
  [heading f]
  (let [current-index (get headings->index heading)
        index (f current-index 1)
        new-index (mod index 4)
        new-heading (get index->headings new-index)]
    new-heading
    ))

(defn- turn-right
  "Turn 90-degrees right from the compass heading"
  [heading]
  (turn heading +))

(defn- turn-left
  "Turn 90-degrees left from the compass heading"
  [heading]
  (turn heading -))

(defn random-int
  "Return a random Integer between min and max (inclusive)"
  [min max]
  (+ min (.floor js/Math (* (.random js/Math) (+ 1 (- max min)))))
  )

(defn random
  "Get a random number from 0 to size (exclusive)."
  [size]
  (random-int 0, (- size 1))
  )

(defn random-grid-coord
  "Return a random grid coordinate given the specified grid dimensions
  in the form ```{:x 1 :y 1}```"
  [xsize ysize]
  {:x (random xsize) :y (random ysize)}
  )

(defn white?
  "Are the contents ```:white```?"
  [contents]
  (= contents :white)
  )

(defn black?
  "Are the contents ```:black```?"
  [contents]
  (= contents :black)
  )

(defn toggle-color
  "Flip ```:black``` to ```:white``` and vice-versa"
  [color]
  (if (white? color) :black :white)
  )

(defn color?
  "Determines if the specified contents are a color (```:black``` or ```:white```)"
  [contents]
  (or (white? contents) (black? contents))
  )

(defn toggle-if-color
  "Flip ```:black``` to ```:white``` and vice-versa but only if the contents are valid"
  [contents]
  (if (color? contents) (toggle-color contents) contents))

(defn compass
  "Return a map listing the grid locations of the compass directions at the specified distance from the
  current location. In the form ```{:north {:x 1 :y 1} ... }```. Note that this assumes that the grid
  wraps."
  [gridmodel location distance]
  (let [cells-wide (:cells-wide gridmodel)
        cells-high (:cells-high gridmodel)
        old-x (:x location)
        old-y (:y location)]
    {:north {:x old-x :y (mod (- old-y distance) cells-high)}
     :south {:x old-x :y (mod (+ old-y distance) cells-high)}
     :east  {:x (mod (+ old-x distance) cells-wide) :y old-y}
     :west  {:x (mod (- old-x distance) cells-wide) :y old-y}
     }
    ))

(defn new-location
  "Get the new location given the grid, the current location, the compass heading and the distance"
  [gridmodel location heading distance]
  (get (compass gridmodel location distance) heading)
  )