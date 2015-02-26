;; ## Generic functions for the games

(ns gridlife.gamemodel)

;; A protocol for the game implementations
(defprotocol game
  (game-name [this] "The name of the game")
  (to-str [this] "Return a string representation of the game state (if any)")
  (tick [this gridmodel] "Make a move in the game given the current gridmodel, returns
   the new gridmodel the new game (state) and the list of required repaint"))