(ns gridlife.gridui
  (:require [om.core :as om :include-macros true]
            [sablono.core :as h :refer-macros [html]]
            [gridlife.gridmodel :as model :refer [populate-grid]]
            [gridlife.langton :as langdon :refer [tick]]
            [gridlife.random :as random :refer [tick]]))

(enable-console-print!)

(def app-state (atom {:grid-model   {},
                      :cell-px-size 10,
                      :cells-wide   60,
                      :cells-high   60,
                      :run          false
                      }))

(defn paint-cell [x y color]
  (let [cell-size (:cell-px-size @app-state)
        x-px-pos (* x cell-size)
        y-px-pos (* y cell-size)
        canvas (.getElementById js/document "canvas")
        context (.getContext canvas "2d")]
    (set! (.-fillStyle context) (name color))
    (.fillRect context x-px-pos y-px-pos cell-size cell-size)
    (.stroke context)
    ))

(defn paint-cells []
  (let [model (:grid-model @app-state)]
    (doseq [[k v] model]
      (paint-cell (:x k) (:y k) v))
    )
  )

(defn set-request-anim-frame-function []
  (let [std-function (.-requestAnimationFrame js/window)
        webkit-function (.-webkitRequestAnimationFrame js/window)
        moz-function (.-mozRequestAnimationFrame js/window)
        fallback-function (fn [callback] (.setTimeout js/window callback (/ 1000 30)))
        use (or std-function webkit-function moz-function fallback-function)
        ]
    (set! (.-requestAnimFrame js/window) use)
    ))

(defn render-grid [app]
  (if (:run @app)
    (let [model (:grid-model @app)
          xsize (:cells-wide @app)
          ysize (:cells-high @app)]
      (om/update! app :grid-model (langdon/tick model xsize ysize))
      (paint-cells))
    nil)
  (.requestAnimFrame js/window (fn [] (render-grid app))))

(defn init-grid-rendering [app]
  (set-request-anim-frame-function)
  (.requestAnimFrame js/window (fn [] (render-grid app)))
  )

(defn reset-grid [app]
  (let [initial-grid (model/populate-grid (:cells-wide app) (:cells-high app))]
    (om/update! app :grid-model initial-grid)
    (paint-cells)
    ))

(defn controls-component [app _]
  (reify
    om/IRender
    (render [_]
      (let [cell-size (:cell-px-size app)
            grid-width (:cells-wide app)
            grid-height (:cells-high app)
            pxwidth (* cell-size grid-width)
            pxheight (* cell-size grid-height)]
        (h/html
          [:div {:width pxwidth :height pxheight}
           [:div {:id "buttons" :class "btn-group btn-group-lg" :role "group"}
            [:button {:class "btn btn-default" :type "button" :on-click #(om/transact! app :run not)} (if (:run app) "Stop" "Start")]
            [:button {:class "btn btn-default" :type "button" :on-click #(reset-grid app)} "Reset"]]
           [:div {:id "options"}
            [:div {:class "radio"}
             [:label
              [:input {:type "radio" :name "game" :id "random-game" :value "random-game" :checked "checked"}]
              "animate with random black and white cells"
              ]
             ]
            [:div {:class "radio disabled"}
             [:label
              [:input {:type "radio" :name "game" :id "langdon-game" :value "langdon-game"}]
              "release the Langdon Rat!"
              ]]
            ]])))))

(defn grid-component [app _]
  (reify
    om/IDidMount
    (did-mount [_]
      (reset-grid app)
      (init-grid-rendering app))
    om/IRender
    (render [_]
      (let [cell-size (:cell-px-size app)
            grid-width (:cells-wide app)
            grid-height (:cells-high app)
            pxwidth (* cell-size grid-width)
            pxheight (* cell-size grid-height)]
        (h/html
          [:div
           [:canvas {:id "canvas" :class "canvas" :width pxwidth :height pxheight}]]
          )))))

(om/root grid-component app-state
         {:target (.getElementById js/document "content")})

(om/root controls-component app-state
         {:target (.getElementById js/document "controls")})