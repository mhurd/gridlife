;; ## Langton Ant Behaviour
;; This namespace models the behaviour of the Langton Ant as defined on the
;; [wikipedia](https://en.wikipedia.org/wiki/Langton's_ant "Langton Ant") page.
(ns gridlife.langton
  (:require [gridlife.gridmodel :as model]))

;; Declare the record type that represents the Langton Ant, a location and a heading (compass).
(defrecord LangtonAnt [location heading])

(defn- move
  "Move the ant as per the standard rules for Langton's Ants:

  1. At a white square, turn 90 degrees right, flip the color of the square then move forward one unit
  2. At a black square, turn 90 degrees left, flip the color of the square, then move forward one unit
  "
  [gridmodel ant]
      (let [location (:location ant)
            heading (:heading ant)
            model (:model gridmodel)
            current-cell-contents (get model location)
            new-cell-contents (model/toggle-color current-cell-contents)
            new-heading-f (if (model/white? current-cell-contents) model/turn-left model/turn-right)
            new-heading (new-heading-f heading)
            new-model (assoc model location new-cell-contents)
            new-ant (assoc ant :heading new-heading :location (model/new-location gridmodel location new-heading 1))]
        [(assoc gridmodel :model new-model) new-ant [[(:location new-ant) "red"] [location (name new-cell-contents)]]]
        )
      )

(defn new-ant
  "Create a fresh ant at the specified location"
  [start-location]
  (LangtonAnt. start-location :north)
  )

(defn tick
  "Move the ant from the specified app state one tick on the gridmodel"
  [app]
  (let [gridmodel (:gridmodel app)
        ant (:langton-ant app)]
    (move gridmodel ant)
    ))
