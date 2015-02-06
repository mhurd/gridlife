;; ## The OM-based GUI
;; Uses OM which is a ClojureScript wrapper to Facebooks's
;; [React](http://facebook.github.io/react/ "react") project.

(ns gridlife.gridui
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [om.core :as om :include-macros true]
    [sablono.core :as h :refer-macros [html]]
    [gridlife.gridmodel :as model :refer [GridModel]]
    [gridlife.langton :as langton]
    [gridlife.random :as random]
    [cljs.core.async :refer [put! chan <!]]
    [gridlife.gamemodel :as gamemodel :refer [game]]))

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
(def cells-wide 50)
;; Constant defining the number of cells high the grid should be.
(def cells-high 50)
;; Constant defining the sizein pixels of each cell.
(def cell-size 12)

(defn default-ant
  "Create a new default Langton Ant record (starts in the middle of the grid)"
  []
  (langton/new-ant {:x (.floor js/Math (/ cells-wide 2)) :y (.floor js/Math (/ cells-high 2))}))

(defn default-games
  "Get the default game strategies available for this grid"
  []
  [(random/new-random-noise) (default-ant)]
  )

(defn empty-gridmodel
  "Creates an empty grid model (map of location to contents)"
  []
  (model/GridModel. (empty-model cells-wide cells-high) cells-wide cells-high)
  )

;; The key application state used by OM/react, holds the following information:
;;
;; 1. ```:gridmodel``` - the model of the grid mapping locations to cell contents
;; 2. ```:games``` - the list of games (only Langton Ant at preset)
;; 3. ```:enabled-games``` - the list of enabled game names
;; 4. ```:run``` - boolean indicating whether the simultaion is currently running
(def app-state (atom {:gridmodel     (empty-gridmodel),
                      :games         (default-games),
                      :enabled-games [],
                      :run           false
                      }))

(defn- enabled-games-ref []
  (om/ref-cursor (:enabled-games (om/root-cursor app-state))))

(defn- enabled? [game]
  (let [enabled-games (enabled-games-ref)]
    (some #(= (gamemodel/game-name game) %) enabled-games)
    ))

;; The [core-async](https://github.com/clojure/core.async "core.async") channel used
;; to pass the messages of what cells need re-painting.
(def render-chan (chan))

;; The [core-async](https://github.com/clojure/core.async "core.async") channel used
;; to pass the messages that enable / disable games.
(def game-control-chan (chan))

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
  (om/update! app :games (default-games))
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

(defn handle-game-control
  "The core.async loop that accepts the game control messages"
  []
  (go (loop []
        (let [[name enable] (<! render-chan)]
          (println (str "Handling: " name " > " enable))
          (let [enabled-games (enabled-games-ref)]
            (if enable
              (if (contains? enabled-games name)
                nil
                (om/update! enabled-games (cons name enabled-games))
                )
              (if (contains? enabled-games name)
                (om/update! enabled-games (remove #(= name %) enabled-games))
                nil
                )))))
      ))

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

(defn- reduce-games
  "Reduces a list of games by moving them forward one tick, taking the new gridmodel to
  pass onto the next game and collecting the new game and required repaint instructions. Finally returns
  a vector containing the final gridmodel, the new list of games (with their new states) and the list of
  repaint instructions."
  [result game]
  (let [[gridmodel games repaint-instructions] result
        [next-gridmodel new-game new-repaint-instructions] (if (enabled? game)
                                                             (gamemodel/tick game gridmodel)
                                                             [gridmodel game repaint-instructions])]
    [next-gridmodel (cons new-game games) (into repaint-instructions new-repaint-instructions)]
    )
  )

(defn- repaint
  "Iterates through the repaint instructions and places a call onto the render channel for
  each location / color pair"
  [repaint-instructions]
  (doseq [location-color repaint-instructions] (put! render-chan location-color))
  )

(defn run-frame
  "Normalises the calls from the animation function to the specified maximum moves
  per-second. Only then does it iterate through the games to generate the new states and
  repaints to render."
  [app last-time]
  (let [current-time (.getTime (js/Date.))
        difference (- current-time last-time)
        max-moves-per-second 15
        frame-rate-millis (/ 1000 max-moves-per-second)
        gridmodel (:gridmodel @app)]
    (if (and (> difference frame-rate-millis) (:run @app))
      (let [[new-gridmodel new-games repaint-instructions] (reduce reduce-games [gridmodel [] []] (:games @app))]
        (om/update! app :gridmodel new-gridmodel)
        (om/update! app :games new-games)
        (repaint repaint-instructions)
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

(defn- game-checkbox [game owner]
  (println "game = " game)
  (println "owner = " owner)
  (reify
    om/IRender
    (render [_]
      (let [name (gamemodel/game-name game)]
        (h/html
          [:div
           [:input {:type     "checkbox"
                    :checked  (enabled? game)
                    :on-click #(put! game-control-chan [name (not (enabled? game))])}]
           [:label name]
           ]
          ))))
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
         [:div
          (om/build-all game-checkbox (:games app))
          ]
         ]))))

(defn grid-component [app _]
  "The OM definition of the grid canvas"
  (reify
    om/IDidMount
    (did-mount [_]
      (reset-grid app)
      (handle-render-cell)
      (handle-game-control)
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