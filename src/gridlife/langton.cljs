(ns gridlife.langton
  (:require [gridlife.gridmodel :as model :refer [populate-grid]]))

(defn- move [gridmodel]
      (let [ant (:langton-ant gridmodel)
            location (:location ant)
            old-count (get (:visited-counts gridmodel) location)
            new-count (if (nil? old-count) 1 (+ old-count 1))
            new-counts (assoc (:visited-counts gridmodel) location new-count)
            heading (:heading ant)
            model (:model gridmodel)
            current-cell-contents (get model location)
            new-heading-f (if (model/white? current-cell-contents) model/turn-left model/turn-right)
            new-heading (new-heading-f heading)
            new-model (assoc model location (model/toggle-color current-cell-contents))
            new-ant (assoc ant :heading new-heading :location (model/new-location gridmodel location new-heading 1))]
        (assoc gridmodel :model new-model :langton-ant new-ant :visited-counts new-counts)
        )
      )

(defrecord LangtonAnt [location heading])

(defn tick [gridmodel]
  (move gridmodel))
