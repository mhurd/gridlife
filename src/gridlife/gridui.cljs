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

;; enable printing to the Javascript console
(enable-console-print!)

;; Constant defining the number of cells wide the grid should be.
(def cells-wide 50)
;; Constant defining the number of cells high the grid should be.
(def cells-high 50)
;; Constant defining the sizein pixels of each cell.
(def cell-size 12)

(defn empty-model
  "Creates an empty map of ```{:x 1 :y 2}``` location to contents."
  [cells-wide cells-high]
  (let [keys (for [x (range 0 cells-wide)
                   y (range 0 cells-high)]
               {:x x, :y y})]
    (zipmap keys (repeat :white)))
  )

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
;; 2. ```:games``` - the list of games (only Random noise & Langton Ant at preset)
;; 3. ```:enabled-games``` - the map of enabled game names to whether they are enabled
;; 4. ```:run``` - boolean indicating whether the simultaion is currently running
(def app-state (atom {:gridmodel     (empty-gridmodel),
                      :games         (default-games),
                      :enabled-games {},
                      :run           false
                      }))

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

(defn set-request-anim-frame-function
  "Either sets up the standard requestAnimationFrame functions specific to the current
  browser or falls back onto using timeouts for the animation."
  []
  (let [std-function (.-requestAnimationFrame js/window)
        webkit-function (.-webkitRequestAnimationFrame js/window)
        moz-function (.-mozRequestAnimationFrame js/window)
        fallback-function (fn [callback] (.setTimeout js/window callback (/ 1000 30)))
        use (or std-function webkit-function moz-function fallback-function)]
    (set! (.-requestAnimFrame js/window) use)
    ))

(defn- reduce-games
  "Reduces a list of games by moving them forward one tick, taking the new gridmodel to
  pass onto the next game and collecting the new game and required repaint instructions. Finally returns
  a vector containing the final gridmodel, the new list of games (with their new states) and the list of
  repaint instructions so that the global app state can be updated."
  [result game]
  (let [[gridmodel games repaint-instructions] result
        name (gamemodel/game-name game)
        enabled-games (:enabled-games @app-state)
        enabled? (get enabled-games name)
        [next-gridmodel new-game new-repaint-instructions] (if enabled?
                                                             (gamemodel/tick game gridmodel)
                                                             [gridmodel game repaint-instructions])]
    [next-gridmodel (cons new-game games) (into repaint-instructions new-repaint-instructions)]
    )
  )

(defn run-frame
  "Normalises the calls from the animation function to the specified maximum moves
  per-second. Only then does it iterate through the games to generate the new states and
  repaints to render. The cells are individually painted by passing messages onto the
  [core-async](https://github.com/clojure/core.async) channel for painting onto
  the canvas"
  [app render-chan last-time]
  (let [current-time (.getTime (js/Date.))
        difference (- current-time last-time)
        max-moves-per-second 15
        frame-rate-millis (/ 1000 max-moves-per-second)
        gridmodel (:gridmodel @app)]
    (if (and (> difference frame-rate-millis) (:run @app))
      (let [[new-gridmodel new-games repaint-instructions] (reduce reduce-games [gridmodel [] []] (:games @app))]
        (om/update! app :gridmodel new-gridmodel)
        (om/update! app :games new-games)
        (doseq [location-color repaint-instructions] (put! render-chan location-color))
        (.requestAnimFrame js/window (fn [] (run-frame app render-chan current-time)))
        )
      (.requestAnimFrame js/window (fn [] (run-frame app render-chan last-time)))
      ))
  )

(defn- game-checkbox
  "Renders an individual game checkbox used to enable/disable that game, it sets up an on-click handler
  for the checkbox that will pass an instruction onto the game control
  [core-async](https://github.com/clojure/core.async) channel that is passed as initial state to this
  component by the controls conponent"
  [enabled-game _]
  (reify
    om/IRenderState
    (render-state [this {:keys [game-control-chan]}]
      (let [[name enabled?] enabled-game]
        (h/html
          [:div {:class "checkbox-div"}
           [:input {:type     "checkbox"
                    :checked  enabled?
                    :on-click #(put! game-control-chan [name (not enabled?)])}]
           [:label {:class "checkbox-label"} name]
           ]
          ))))
  )

(defn controls-component
  "The OM controls component (Start/Stop, Reset and enabling/disabling games). It is configured with the following
  lifecycle stages:

  1. _om/IInitState_ sets up the initial state containing a
  [core-async](https://github.com/clojure/core.async) channel to be used to pass control messages
  2. _om/IWillMount_ will, before the DOM is setup, initialise the enabled games global state and start the go loop
  for the control channel (this listens for the control messages indicating that a game has been
  enabled/disabled and updates the global state)
  3. _om/IRenderState_ sets up the DOM elements and passes the initial state containing the game control channel to
  the sub-component responsible for building the individual game enable/disable checkboxes"
  [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:game-control-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [games (:games app)
            enabled-games (:enabled-games app)
            game-control-chan (om/get-state owner :game-control-chan)
            initial-state (reduce (fn [acc game] (assoc acc (gamemodel/game-name @game) false)) {} games)]
        (om/update! enabled-games initial-state)
        (go (loop []
              (let [[name enable] (<! game-control-chan)
                    new-enabled-games (assoc @enabled-games name enable)]
                (om/update! enabled-games new-enabled-games)
                (recur))))))
    om/IRenderState
    (render-state [owner {:keys [game-control-chan]}]
      (h/html
        [:div
         [:div {:id "buttons" :class "btn-group btn-group-sm" :role "group"}
          [:button {:class "btn btn-default"
                    :type "button"
                    :on-click #(om/transact! app :run not)} (if (:run app) "Stop" "Start")]
          [:button {:class "btn btn-default"
                    :type "button"
                    :on-click #(reset-grid app)} "Reset"]]
         [:div {:class "games-div"}
          (om/build-all game-checkbox (:enabled-games app) {:init-state {:game-control-chan game-control-chan}})
          ]
         ]))))

(defn grid-component
  "The OM canvas component. It is configured with the following
  lifecycle stages:

  1. _om/IInitState_ sets up the initial state containing a
  [core-async](https://github.com/clojure/core.async) channel to be used to pass paint cell messages
  2. _om/IWillMount_ will, before the DOM is setup, start the go loop for the render channel
  (this listens for the paint messages and paints the corresponding cell on the canvas)
  3. _om/IDidMount_ will, after the DOM is setup, reset the grid and setup the required animation functions
  and the render channel.
  4. _om/IRenderState_ sets up the DOM elements."
  [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:render-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [render-chan (om/get-state owner :render-chan)]
        (go (loop []
              (let [[cell color] (<! render-chan)]
                (paint-cell (:x cell) (:y cell) color)
                (recur))))))
    om/IDidMount
    (did-mount [_]
      (let [render-chan (om/get-state owner :render-chan)]
        (reset-grid app)
        (set-request-anim-frame-function)
        (.requestAnimFrame js/window (fn [] (run-frame app render-chan (.getTime (js/Date.)))))))
    om/IRenderState
    (render-state [_ _]
      (let [cells-wide (:cells-wide (:gridmodel @app))
            cells-high (:cells-high (:gridmodel @app))
            pxwidth (* cell-size cells-wide)
            pxheight (* cell-size cells-high)]
        (h/html
          [:div
           [:canvas {:id "canvas"
                     :class "canvas"
                     :width pxwidth
                     :height pxheight}]]
          )))))

;; The hook into the DOM for the controls.
(om/root controls-component app-state
         {:target (.getElementById js/document "controls")})

;; The hook into the DOM for the grid canvas.
(om/root grid-component app-state
         {:target (.getElementById js/document "content")})