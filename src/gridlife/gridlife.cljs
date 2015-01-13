(ns gridlife
  (:require [om.core :as om :include-macros true]
            [sablono.core :as h :refer-macros [html]]))

(enable-console-print!)

(def app-state (atom {:grid-model {},
                      :cell-size 20,
                      :grid-width 20,
                      :grid-height 20,
                      :line-width 2
                      }))

(defn random-int [min max]
  (+ min (.floor js/Math (* (.random js/Math) (+ 1 (- max min)))))
  )

(defn random-x []
  (random-int 0, (- (:grid-width @app-state) 1))
  )

(defn random-y []
  (random-int 0, (- (:grid-height @app-state) 1))
  )

(defn toggle [color]
  (if (= color :white) :black :white))

(defn langton-tick [model]
  (let [random-coord {:x (random-x) :y (random-y)}
        new-color (toggle (get model random-coord))
        new-model (assoc model random-coord new-color)]
    new-model
    )
  )

(defn populate-grid [xsize ysize]
  (let [keys (for [x (range 0 xsize)
                   y (range 0 ysize)]
                    {:x x, :y y})]
    (zipmap keys (repeat :white)))
  )

(defn draw-grid [app]
  (let [line-width (:line-width @app)
        cell-size (:cell-size @app)
        grid-width (:grid-width @app)
        grid-height (:grid-height @app)
        px-width (* cell-size grid-width)
        px-height (* cell-size grid-height)
        bg-canvas (.getElementById js/document "bgcanvas")
        bg-context (.getContext bg-canvas "2d")]
  (doseq [x (range 0 (+ px-width cell-size) cell-size)]
    (let [fromx (+ x line-width)
          fromy line-width
          tox (+ x line-width)
          toy (+ px-height line-width)]
      (.moveTo bg-context fromx fromy)
      (.lineTo bg-context tox toy)
      ))
  (doseq [y (range 0 (+ px-height cell-size) cell-size)]
    (let [fromx line-width
          fromy (+ y line-width)
          tox (+ px-width line-width)
          toy (+ y line-width)]
      (.moveTo bg-context fromx fromy)
      (.lineTo bg-context tox toy)
      ))
  (set! (.-lineWidth bg-context) line-width)
  (set! (.-strokeStyle bg-context) "black")
  (.stroke bg-context)
  ))

(defn paint-cell [app x y color]
  (let [line-width (:line-width @app)
        half-line-width (/ (:line-width @app) 2)
        cell-size (:cell-size @app)
        xpos (+ line-width half-line-width (* x cell-size))
        ypos (+ line-width half-line-width (* y cell-size))
        extent (- cell-size line-width)
        fg-canvas (.getElementById js/document "fgcanvas")
        fg-context (.getContext fg-canvas "2d")]
    (set! (.-fillStyle fg-context) (name color))
    (.fillRect fg-context xpos ypos extent extent)
    (.stroke fg-context)
    ))

(defn paint-cells [app]
  (doseq [[k v] (:grid-model @app)]
    (paint-cell app (:x k) (:y k) v))
  )

(defn langton-grid [app _]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [initial-grid (populate-grid (:grid-width @app) (:grid-height @app))]
        (om/update! app :grid-model initial-grid)
        (draw-grid app)
        (js/setInterval
          (fn [] (do
                   (om/update! app :grid-model (langton-tick (:grid-model @app)))
                   (paint-cells app)))
          30)
        ))
    om/IRender
    (render [_]
      (h/html
          [:div]))))

(om/root langton-grid app-state
         {:target (.getElementById js/document "controls")})