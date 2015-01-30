(ns gridlife.random
  (:require [gridlife.gridmodel :as model]))

(defn tick [gridmodel]
  (let [cells-wide (:cells-wide gridmodel)
        cells-high (:cells-high gridmodel)
        random-coord (model/random-grid-coord cells-wide cells-high)
        model (:model gridmodel)
        new-color (model/toggle-if-color (get model random-coord))
        new-model (assoc model random-coord new-color)]
    (assoc gridmodel :model new-model)
    ))
