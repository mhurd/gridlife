(ns gridlife.random
  (:require [gridlife.gridmodel :as model]
            [gridlife.gamemodel :as gamemodel]))

;; Declare the record type that represents Random Noise, this has no game state but implements
;; the gamemodel/game Protocol to implement a behaviour that on each tick toggles the color of a
;; random cell.
(defrecord RandomNoise []
  gamemodel/game
  (game-name [_] "Random Noise")
  (to-str [_] "Random Noise")
  (tick [this gridmodel]
    (let [model (:model gridmodel)
          cells-wide (:cells-wide gridmodel)
          cells-high (:cells-high gridmodel)
          random-coord (model/random-grid-coord cells-wide cells-high)
          new-color (model/toggle-if-color (get model random-coord))
          new-model (assoc model random-coord new-color)]
      [(assoc gridmodel :model new-model) this [[random-coord (name new-color)]]])))

(defn new-random-noise
  "Create a fresh random noise game"
  []
  (RandomNoise.))