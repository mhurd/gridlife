(ns gridlife.langton
  (:require [gridlife.gridmodel :as model]))

(defrecord LangtonAnt [location heading])

(defn- move [gridmodel]
      (let [ant (:langton-ant gridmodel)
            location (:location ant)
            heading (:heading ant)
            model (:model gridmodel)
            current-cell-contents (get model location)
            new-cell-contents (model/toggle-color current-cell-contents)
            new-heading-f (if (model/white? current-cell-contents) model/turn-left model/turn-right)
            new-heading (new-heading-f heading)
            new-model (assoc model location new-cell-contents)
            new-ant (assoc ant :heading new-heading :location (model/new-location gridmodel location new-heading 1))]
        [(assoc gridmodel :model new-model :langton-ant new-ant) [[(:location new-ant) "red"] [location (name new-cell-contents)]]]
        )
      )

(defn tick [gridmodel]
  (move gridmodel))
