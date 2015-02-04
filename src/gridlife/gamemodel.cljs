 (ns gridlife.gamemodel)

 (defprotocol game
   (game-name [this])
   (game-state [this])
   (tick [this gridmodel])
   )