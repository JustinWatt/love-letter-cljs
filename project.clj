(defproject love-letter-cljs "0.1.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [reagent "0.6.0-alpha"]
                 [re-frame "0.7.0"]
                 [prismatic/schema "1.1.0"]
                 [org.clojure/core.match "1.0.0"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-cljfmt "0.6.4"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :profiles {:dev  {:dependencies [[nrepl "0.8.3"]
                                   [cider/piggieback "0.5.2"]]
                    :source-paths ["cljs_src" "dev"]}}

  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}

  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler     {:main                 love-letter-cljs.core
                                       :output-to            "resources/public/js/compiled/app.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :asset-path           "js/compiled/out"
                                       :source-map-timestamp true}}

                       {:id           "min"
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler     {:main            love-letter-cljs.core
                                       :output-to       "resources/public/js/compiled/app.js"
                                       :optimizations   :advanced
                                       :closure-defines {goog.DEBUG false}
                                       :pretty-print    false}}]})
