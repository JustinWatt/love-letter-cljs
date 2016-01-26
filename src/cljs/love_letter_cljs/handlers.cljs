(ns love-letter-cljs.handlers
    (:require [re-frame.core :as re-frame]
              [love-letter-cljs.db :as db]))

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))
