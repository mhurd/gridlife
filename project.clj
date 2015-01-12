(defproject gridlife "0.1.0-SNAPSHOT"
  :description "Grid based artificial life, Langton's Ant and Conway's Life"
  :url "http://gridworld.mhurd.com/"

  :min-lein-version "2.3.4"

  :source-paths ["src" "src/gridworld"]

  :dependencies [[org.clojure/clojurescript "0.0-2511"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.8.0-beta5"]
                 [secretary "1.2.1"]                        ;; see http://spootnik.org/entries/2014/10/26_from-angularjs-to-om-a-walk-through.html
                 [sablono "0.2.22"]                         ;; see https://github.com/r0man/sablono
                 [cljs-ajax "0.3.3"]
                 [prismatic/om-tools "0.3.6"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-marginalia "0.8.0"]]

  :hooks [leiningen.cljsbuild]

  :cljsbuild {
    :builds [{:source-paths ["src/gridworld/cljs/"]
              :compiler {:output-to     "resources/public/js/gridworld.js"
                         :optimizations :advanced
                         :pretty-print  false
                         :externs       ["externs/externs.js",
                                         "resources/public/lib/jquery-2.1.3.min.js",
                                         "resources/public/lib/bootstrap.min.js"]
                         }}]}

  :profiles
    {})