(ns love-letter-cljs.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [love-letter-cljs.handlers]
              [love-letter-cljs.subs]
              [love-letter-cljs.views :as views]
              [love-letter-cljs.config :as config]))

(when config/debug?
  (println "dev mode"))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
