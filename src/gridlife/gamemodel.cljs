;; ## Generic functions for the games

 (ns gridlife.gamemodel)

;; A protocol for the game implementations requiring the following functions:
;;
;; 1. _game-name_ - returns the name of the game
;; 2. _to-str_ - returns a string representation of the game state
;; 3. _tick_ - Move the game forward one tick using the supplied gridmodel, returns
;; the new gridmodel the new game (state) and the list of required repaint
;; instructions
 (defprotocol game
   (game-name [this])
   (to-str [this])
   (tick [this gridmodel])
   )