(ns gridlife.gridui
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [om.core :as om :include-macros true]
    [sablono.core :as h :refer-macros [html]]
    [gridlife.gridmodel :as model :refer [GridModel]]
    [gridlife.langton :as langton :refer [tick]]
    [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

(defn empty-model [cells-wide cells-high]
  (let [keys (for [x (range 0 cells-wide)
                   y (range 0 cells-high)]
               {:x x, :y y})]
    (zipmap keys (repeat :white)))
  )

(def cells-wide 80)
(def cells-high 80)
(def cell-size 8)

(defn default-ant [] (langton/LangtonAnt. {:x (/ cells-wide 2) :y (/ cells-high 2)} :north))

(defn empty-gridmodel []
  (model/GridModel. (empty-model cells-wide cells-high) cells-wide cells-high (default-ant))
  )

(def app-state (atom {:gridmodel    (empty-gridmodel),
                      :cell-px-size cell-size,
                      :run          false
                      }))

(def render-chan (chan))

(defn paint-cell [x y color]
  (let [cell-size (:cell-px-size @app-state)
        x-px-pos (* x cell-size)
        y-px-pos (* y cell-size)
        canvas (.getElementById js/document "canvas")
        context (.getContext canvas "2d")]
    (set! (.-fillStyle context) color)
    (.fillRect context x-px-pos y-px-pos cell-size cell-size)
    (.stroke context))
  )

(defn reset-grid [app]
  (om/update! app :gridmodel (empty-gridmodel))
  (doseq [location (keys (:model (:gridmodel app)))] (paint-cell (:x location) (:y location) "white"))
  )

(defn handle-render-cell []
  (go (loop []
        (let [[cell color] (<! render-chan)]
          (paint-cell (:x cell) (:y cell) color)
          (recur))))
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

(defn run-frame [app]
  (if (:run @app)
    (let [[new-gridmodel repaint] (langton/tick (:gridmodel @app))]
      (om/update! app :gridmodel new-gridmodel)
      (doseq [location-color repaint] (put! render-chan location-color))
      )
    nil)
  (.requestAnimFrame js/window (fn [] (run-frame app)))
  )

(defn init-grid-rendering [app]
  (set-request-anim-frame-function)
  (.requestAnimFrame js/window (fn [] (run-frame app)))
  )

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
      (handle-render-cell)
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