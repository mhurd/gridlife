(defproject gridlife "0.1.0-SNAPSHOT"
  :description "Grid based artificial life, Langton's Ant and Conway's Life"
  :url "http://gridworld.mhurd.com/"

  :min-lein-version "2.3.4"

  :source-paths ["src" "src/gridworld"]

  :dependencies [[org.clojure/clojurescript "0.0-2511"]
                 [org.clojure/clojure "1.6.0"]
                 [om "0.8.0-beta5"]
                 [sablono "0.2.22"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-marginalia "0.8.0"]]

  :hooks [leiningen.cljsbuild]

  :cljsbuild {
    :builds [{:source-paths ["src/gridlife/"]
              :compiler {:output-to     "src/js/gridlife.js"
                         :optimizations :whitespace
                         :pretty-print  true
                         :externs       ["externs/externs.js",
                                         "resources/public/lib/jquery-2.1.3.min.js"]
                         }}]}

  :profiles
    {})