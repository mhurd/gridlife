(defproject gridlife "0.1.0-SNAPSHOT"
            :description "Grid based artificial life, Langton's Ant and Conway's Life"
            :url "http://gridworld.mhurd.com/"

            :min-lein-version "2.3.4"

            :source-paths ["src" "src/gridworld"]

            :dependencies [[org.clojure/clojurescript "0.0-2760"]
                           [org.clojure/clojure "1.6.0"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                           [om "0.8.0-rc1"]
                           [sablono "0.2.22"]]

            :plugins [[lein-cljsbuild "1.0.3"]
                      ;; lein marg --dir resources/public/docs src/gridlife/gamemodel.cljs
                      [lein-marginalia "0.8.0"]
                      ;; https://github.com/weavejester/cljfmt/
                      ;; lein cljfmt check, lein cljfmt fix
                      [lein-cljfmt "0.1.10"]]

            :hooks [leiningen.cljsbuild]

            :cljsbuild {
                        :builds [{:source-paths ["src/gridlife/"]
                                  :compiler     {
                                                 :main          main.core
                                                 :output-to     "src/js/gridlife.js"
                                                 :optimizations :advanced
                                                 :pretty-print  false
                                                 :externs       ["externs/externs.js",
                                                                 "resources/public/lib/jquery-2.1.3.min.js"]
                                                 }}]}

            :profiles
            {})