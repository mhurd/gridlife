(ns gridlife.random
  (:require [gridlife.gridmodel :as model :refer [populate-grid]]))

(defn tick [model xsize ysize]
  (let [random-coord (model/random-grid-coord xsize ysize)
        new-color (model/toggle-if-color (get model random-coord))
        new-model (assoc model random-coord new-color)]
    new-model
    )
  )
