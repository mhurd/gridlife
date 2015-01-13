(ns gridlife
  (:require [om.core :as om :include-macros true]
            [sablono.core :as h :refer-macros [html]]))

(enable-console-print!)

(def app-state (atom {:grid-model {}}))

(defn black? [node]
  (= (:color node) :black))

(defn white? [node]
  (= (:color node) :white))

(defn langton-tick [model]
  (map #(:color %) (keys model))
  )

(defn populate-grid [cells]
  (let [keys (for [x (range 0 cells)
                   y (range 0 cells)]
                    {:x x, :y y})]
    (zipmap keys (repeat :white)))
  )

(defn drawBoard [cell-size grid-size-in-cells]
  (let [line-width 2
        px-dimension (* cell-size grid-size-in-cells)
        bg-canvas (. js/document (getElementById "bgcanvas"))
        bg-context (. bg-canvas (getContext "2d"))]
  (for [x (range 0 px-dimension cell-size)]
    (do
      (. bg-context (moveTo (+ x line-width) line-width))
      (. bg-context (lineTo (+ x line-width) (+ px-dimension line-width)))
      (. bg-context (moveTo line-width (+ x line-width)))
      (. bg-context (lineTo (+ px-dimension line-width) (+ x line-width))))
    )
  (set! (.-lineWidth bg-context) line-width)
  (set! (.-strokeStyle bg-context) line-width)
  (.stroke bg-context)
  ))

(defn langton-grid [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [cell-size 20
            grid-size-in-cells 20
            initial-grid (populate-grid grid-size-in-cells)]
        (om/transact! app assoc :grid-model initial-grid)
        (drawBoard cell-size grid-size-in-cells)
        (js/setInterval
          (fn [] (om/transact! app assoc :grid-model (langton-tick (:grid-model app)))
          500)
        )
        ))
    om/IRender
    (render [this]
      (h/html
          [:div]))))

(om/root langton-grid app-state
         {:target (. js/document (getElementById "controls"))})