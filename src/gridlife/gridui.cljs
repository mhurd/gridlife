(ns gridlife.gridui
  (:require [om.core :as om :include-macros true]
            [sablono.core :as h :refer-macros [html]]
            [gridlife.gridmodel :as model :refer [populate-grid GridModel]]
            [gridlife.langton :as langton :refer [tick]]
            [gridlife.random :as random :refer [tick]]))

(enable-console-print!)

(defn default-ant [] (langton/LangtonAnt. {:x 30 :y 30} :north))

(def app-state (atom {:gridmodel    (GridModel. {} 60 60 (default-ant)),
                      :cell-px-size 10,
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
  (let [gridmodel (:gridmodel @app-state)
        model (:model gridmodel)
        ant (:langton-ant gridmodel)]
    (doseq [[k v] model]
      (paint-cell (:x k) (:y k) v))
    (if (nil? ant)
      nil
      (let [location (:location ant)]
        (paint-cell (:x location) (:y location) "red")
        )))
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
    (do
      (om/update! app :gridmodel (langton/tick (:gridmodel @app)))
      (paint-cells))
    nil)
  (.requestAnimFrame js/window (fn [] (render-grid app))))

(defn init-grid-rendering [app]
  (set-request-anim-frame-function)
  (.requestAnimFrame js/window (fn [] (render-grid app)))
  )

(defn reset-grid [app]
  (let [initial-grid (model/populate-grid (:gridmodel @app))]
    (om/update! app :gridmodel (assoc initial-grid :langton-ant (default-ant)))
    (paint-cells)
    ))

(defn controls-component [app _]
  (reify
    om/IRender
    (render [_]
      (let [cell-size (:cell-px-size app)
            cells-wide (:cells-wide (:gridmodel @app))
            cells-high (:cells-high (:gridmodel @app))
            pxwidth (* cell-size cells-wide)
            pxheight (* cell-size cells-high)]
        (h/html
          [:div {:width pxwidth :height pxheight}
           [:div {:id "buttons" :class "btn-group btn-group-lg" :role "group"}
            [:button {:class "btn btn-default" :type "button" :on-click #(om/transact! app :run not)} (if (:run app) "Stop" "Start")]
            [:button {:class "btn btn-default" :type "button" :on-click #(reset-grid app)} "Reset"]]
           ])))))

(defn grid-component [app _]
  (reify
    om/IDidMount
    (did-mount [_]
      (reset-grid app)
      (init-grid-rendering app))
    om/IRender
    (render [_]
      (let [cell-size (:cell-px-size app)
            cells-wide (:cells-wide (:gridmodel @app))
            cells-high (:cells-high (:gridmodel @app))
            pxwidth (* cell-size cells-wide)
            pxheight (* cell-size cells-high)]
        (h/html
          [:div
           [:canvas {:id "canvas" :class "canvas" :width pxwidth :height pxheight}]]
          )))))

(om/root grid-component app-state
         {:target (.getElementById js/document "content")})

(om/root controls-component app-state
         {:target (.getElementById js/document "controls")})