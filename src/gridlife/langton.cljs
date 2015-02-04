;; ## Langton Ant Behaviour
;; This namespace models the behaviour of the Langton Ant as defined on the
;; [wikipedia](https://en.wikipedia.org/wiki/Langton's_ant "Langton Ant") page.
(ns gridlife.langton
  (:require [gridlife.gridmodel :as model]
            [gridlife.gamemodel :as gamemodel]))

;; Declare the record type that represents the Langton Ant, a location and a heading (compass).
(defrecord LangtonAnt [location heading]
  gamemodel/game
  (game-name [_] "Langton's Ant")
  (game-state [_] (str "Langton's Ant: ") location ", " heading)
  (tick [_ gridmodel]
    (let [model (:model gridmodel)
          cells-wide (:cells-wide gridmodel) 
          cells-high (:cells-high gridmodel)
          current-cell-contents (get model location)
          new-cell-contents (model/toggle-color current-cell-contents)
          new-heading-f (if (model/white? current-cell-contents) model/turn-left model/turn-right)
          new-heading (new-heading-f heading)
          new-model (assoc model location new-cell-contents)
          new-location (model/new-location cells-wide cells-high location new-heading 1)
          new-ant (LangtonAnt. new-location new-heading)]
      (println (str "new-location: " new-location))
      (println (str "   old-location: " location))
      (println (str "   new-color: " new-cell-contents))
      [(assoc gridmodel :model new-model) new-ant [[new-location "red"] [location (name new-cell-contents)]]]
      )
    ))

(defn new-ant
  "Create a fresh ant at the specified location"
  [start-location]
  (LangtonAnt. start-location :north)
  )
