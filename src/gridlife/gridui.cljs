(ns gridlife.gridui
  (:require [om.core :as om :include-macros true]
            [sablono.core :as h :refer-macros [html]]
            [gridlife.gridmodel :as model :refer [populate-grid]]
            [gridlife.langton :as langdon :refer [tick]]))

(enable-console-print!)

(def app-state (atom {:grid-model  {},
                      :cell-px-size   10,
                      :cells-wide  60,
                      :cells-high 60,
                      :line-width  1,
                      :run false
                      }))

(defn draw-grid-lines []
  (let [line-width (:line-width @app-state)
        cell-size (:cell-px-size @app-state)
        cells-wide (:cells-wide @app-state)
        cells-high (:cells-high @app-state)
        grid-width-px (* cell-size cells-wide)
        grid-height-px (* cell-size cells-high)
        bg-canvas (.getElementById js/document "bgcanvas")
        bg-context (.getContext bg-canvas "2d")]
    (doseq [x (range 0 (+ grid-width-px cell-size) cell-size)]
      (let [fromx (+ x line-width)
            fromy line-width
            tox (+ x line-width)
            toy (+ grid-height-px line-width)]
        (.moveTo bg-context fromx fromy)
        (.lineTo bg-context tox toy)
        ))
    (doseq [y (range 0 (+ grid-height-px cell-size) cell-size)]
      (let [fromx line-width
            fromy (+ y line-width)
            tox (+ grid-width-px line-width)
            toy (+ y line-width)]
        (.moveTo bg-context fromx fromy)
        (.lineTo bg-context tox toy)
        ))
    (set! (.-lineWidth bg-context) line-width)
    (set! (.-strokeStyle bg-context) "black")
    (.stroke bg-context)
    ))

(defn paint-cell [x y color]
  (let [line-width (:line-width @app-state)
        half-line-width (/ (:line-width @app-state) 2)
        cell-size (:cell-px-size @app-state)
        x-px-pos (+ line-width half-line-width (* x cell-size))
        y-px-pos (+ line-width half-line-width (* y cell-size))
        line-px-length (- cell-size line-width)
        fg-canvas (.getElementById js/document "fgcanvas")
        fg-context (.getContext fg-canvas "2d")]
    (set! (.-fillStyle fg-context) (name color))
    (.fillRect fg-context x-px-pos y-px-pos line-px-length line-px-length)
    (.stroke fg-context)
    ))

(defn paint-cells []
  (doseq [[k v] (:grid-model @app-state)]
    (paint-cell (:x k) (:y k) v))
  )

(defn set-request-anim-frame-function []
  (let [std-function  (.-requestAnimationFrame js/window)
        webkit-function (.-webkitRequestAnimationFrame js/window)
        moz-function (.-mozRequestAnimationFrame js/window)
        fallback-function (fn [callback] (.setTimeout js/window callback (/ 1000 60)))
        use (or std-function webkit-function moz-function fallback-function)
        ]
    (set! (.-requestAnimFrame js/window) use)
    ))

(defn render-grid []
  (.requestAnimFrame js/window (fn [] (render-grid)))
  (paint-cells)
  )

(defn init-grid-rendering []
  (draw-grid-lines)
  (set-request-anim-frame-function)
  (.requestAnimFrame js/window (fn [] (render-grid)))
  )

(defn reset-grid [app]
  (let [initial-grid (model/populate-grid (:cells-wide app) (:cells-high app))]
    (om/update! app :grid-model initial-grid)
    ))

(defn controls-component [app _]
  (reify
    om/IRender
    (render [_]
      (h/html
           [:div {:class "btn-group btn-group-lg" :role "group"}
            [:button {:class "btn btn-default" :type "button" :on-click #(om/transact! app :run not)} (if (:run app) "Stop" "Start")]
            [:button {:class "btn btn-default" :type "button" :on-click #(reset-grid app)} "Reset"]]))))

(defn grid-component [app _]
  (reify
    om/IDidMount
    (did-mount [_]
      (reset-grid app)
      (init-grid-rendering)
      (js/setInterval
        (fn [] (do
                 (if (:run @app)
                  (om/update! app :grid-model (langdon/tick (:grid-model @app)))
                  nil)))
        30)
      )
    om/IRender
    (render [_]
      (let [cell-size (:cell-px-size app)
            grid-width (:cells-wide app)
            grid-height (:cells-high app)
            double-line-width (* 2 (:line-width app))
            pxwidth (+ double-line-width (* cell-size grid-width))
            pxheight (+ double-line-width (* cell-size grid-height))]
        (h/html
          [:div
            [:canvas {:id "bgcanvas" :class "canvas" :width pxwidth :height pxheight}]
            [:canvas {:id "fgcanvas" :class "canvas" :width pxwidth :height pxheight}]]
          )))))

(om/root grid-component app-state
         {:target (.getElementById js/document "content")})

(om/root controls-component app-state
         {:target (.getElementById js/document "controls")})