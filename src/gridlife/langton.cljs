(ns gridlife.langton
  (:require [gridlife.gridmodel :as model :refer [populate-grid]]))

(defn tick [model]
  (let [random-coord (model/random-grid-coord model)
        new-color (model/toggle (get model random-coord))
        new-model (assoc model random-coord new-color)]
    new-model
    )
  )
