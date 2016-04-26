(defproject love-letter-cljs "0.1.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [reagent "0.6.0-alpha"]
                 [re-frame "0.7.0"]
                 [prismatic/schema "1.1.0"]
                 [devcards "0.2.1-6"]
                 [org.clojure/core.match "0.3.0-alpha4"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.2"]
            [lein-doo "0.1.6"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.0-2"]]
                   :source-paths ["cljs_src" "dev"]}
             :repl {:plugins [[cider/cider-nrepl "0.12.0"]]}}

  :figwheel-options {:port 7888}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:css-dirs ["resources/public/css"]
                                   :on-jsload "love-letter-cljs.core/mount-root"}
                        :compiler {:main love-letter-cljs.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true}}

                       {:id "devcards"
                        :source-paths ["src"]
                        :figwheel {:css-dirs ["resources/public/css"]
                                   :devcards true}
                        :compiler {:main love-letter-cljs.cards
                                   :output-to "resources/public/js/compiled/love_letter_cljs_devcards.js"
                                   :output-dir "resources/public/js/compiled/devcards_out"
                                   :asset-path "js/compiled/devcards_out"
                                   :source-map-timestamp true}}

                       {:id "test"
                        :source-paths ["src/cljs" "test/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/test.js"
                                   :main love-letter-cljs.runner
                                   :optimizations :none}}

                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:main love-letter-cljs.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}]})
