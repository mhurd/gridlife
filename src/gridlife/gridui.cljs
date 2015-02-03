;; ## The OM-based GUI
;; Uses OM which is a ClojureScript wrapper to Facebooks's
;; [React](http://facebook.github.io/react/ "react") project.

(ns gridlife.gridui
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [om.core :as om :include-macros true]
    [sablono.core :as h :refer-macros [html]]
    [gridlife.gridmodel :as model :refer [GridModel]]
    [gridlife.langton :as langton :refer [tick]]
    [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

(defn empty-model
  "Creates an empty map of ```{:x 1 :y 2}``` location to contents."
  [cells-wide cells-high]
  (let [keys (for [x (range 0 cells-wide)
                   y (range 0 cells-high)]
               {:x x, :y y})]
    (zipmap keys (repeat :white)))
  )

;; Constant defining the number of cells wide the grid should be.
(def cells-wide 70)
;; Constant defining the number of cells high the grid should be.
(def cells-high 70)
;; Constant defining the sizein pixels of each cell.
(def cell-size 8)

(defn default-ant
  "Create a new default Langton Ant record (starts in the middle of the grid)"
  [] (langton/new-ant {:x (/ cells-wide 2) :y (/ cells-high 2)}))

(defn empty-gridmodel []
  "Creates an empty grid model (map of location to contents)"
  (model/GridModel. (empty-model cells-wide cells-high) cells-wide cells-high)
  )

;; The key application state used by OM/react, holds the following information:
;;
;; 1. ```:gridmodel``` - the model of the grid mapping locations to cell contents
;; 2. ```:langton-ant``` - the model of the current Langton Ant
;; 3. ```:run``` - boolean indicating whether the simultaion is currently running
(def app-state (atom {:gridmodel   (empty-gridmodel),
                      :langton-ant (default-ant)
                      :run         false
                      }))

;; The [core-async](https://github.com/clojure/core.async "core.async") channel used
;; to pass the messages of what cells need re-painting.
(def render-chan (chan))

(defn paint-cell
  "Paints the cell at the specified x/y location the specified color"
  [x y color]
  (let [x-px-pos (* x cell-size)
        y-px-pos (* y cell-size)
        canvas (.getElementById js/document "canvas")
        context (.getContext canvas "2d")]
    (set! (.-fillStyle context) color)
    (.fillRect context x-px-pos y-px-pos cell-size cell-size)
    (.stroke context))
  )

(defn reset-grid
  "Resets the grid (empty gridmodel and default ant)"
  [app]
  (om/update! app :gridmodel (empty-gridmodel))
  (om/update! app :langton-ant (default-ant))
  (doseq [location (keys (:model (:gridmodel app)))] (paint-cell (:x location) (:y location) "white"))
  )

(defn handle-render-cell
  "The core.async loop that accepts the repaint messages"
  []
  (go (loop []
        (let [[cell color] (<! render-chan)]
          (paint-cell (:x cell) (:y cell) color)
          (recur))))
  )

(defn set-request-anim-frame-function
  "Either sets up the standard requestAnimationFrame functions specific to the current
  browser or falls back onto using timeouts for the animation."
  []
  (let [std-function (.-requestAnimationFrame js/window)
        webkit-function (.-webkitRequestAnimationFrame js/window)
        moz-function (.-mozRequestAnimationFrame js/window)
        fallback-function (fn [callback] (.setTimeout js/window callback (/ 1000 30)))
        use (or std-function webkit-function moz-function fallback-function)
        ]
    (set! (.-requestAnimFrame js/window) use)
    ))

(defn run-frame
  "Normalises the calls from the animation function to the specified maximum moves
  per-second. Only then does it move the Langton Ant, call for a repaint and request the
  next animation frame"
  [app last-time]
  (let [current-time (.getTime (js/Date.))
        difference (- current-time last-time)
        max-moves-per-second 30
        frame-rate-millis (/ 1000 max-moves-per-second)]
    (if (and (> difference frame-rate-millis) (:run @app))
      (let [[new-gridmodel new-ant repaint] (langton/tick @app)]
        (om/update! app :gridmodel new-gridmodel)
        (om/update! app :langton-ant new-ant)
        (doseq [location-color repaint] (put! render-chan location-color))
        (.requestAnimFrame js/window (fn [] (run-frame app current-time)))
        )
      (.requestAnimFrame js/window (fn [] (run-frame app last-time)))
      ))
  )

(defn init-grid-rendering
  "Initialises the rendering setup"
  [app]
  (set-request-anim-frame-function)
  (.requestAnimFrame js/window (fn [] (run-frame app (.getTime (js/Date.)))))
  )

(defn controls-component
  "The OM definition of the controls (Start/Stop and Reset)"
  [app _]
  (reify
    om/IRender
    (render [_]
      (h/html
        [:div
         [:div {:id "buttons" :class "btn-group btn-group-sm" :role "group"}
          [:button {:class "btn btn-default" :type "button" :on-click #(om/transact! app :run not)} (if (:run app) "Stop" "Start")]
          [:button {:class "btn btn-default" :type "button" :on-click #(reset-grid app)} "Reset"]]
         ]))))

(defn grid-component [app _]
  "The OM definition of the grid canvas"
  (reify
    om/IDidMount
    (did-mount [_]
      (reset-grid app)
      (handle-render-cell)
      (init-grid-rendering app))
    om/IRender
    (render [_]
      (let [cells-wide (:cells-wide (:gridmodel @app))
            cells-high (:cells-high (:gridmodel @app))
            pxwidth (* cell-size cells-wide)
            pxheight (* cell-size cells-high)]
        (h/html
          [:div
           [:canvas {:id "canvas" :class "canvas" :width pxwidth :height pxheight}]]
          )))))

;; The hook into the DOM for the controls.
(om/root controls-component app-state
         {:target (.getElementById js/document "controls")})

;; The hook into the DOM for the grid canvas.
(om/root grid-component app-state
         {:target (.getElementById js/document "content")})