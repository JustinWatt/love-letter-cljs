(ns love-letter-cljs.handlers
    (:require [re-frame.core :refer [dispatch register-handler]]
              [love-letter-cljs.db :as db]
              [love-letter-cljs.game :as l]))

(register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(register-handler
 :deal-cards
 re-frame.core/debug
 (fn [db _]
   (assoc (:game db) :game  (l/deal-cards (:game db)))))

(register-handler
 :new-game
 re-frame.core/debug
 (fn [db _]
   (assoc db :game (l/create-game))))

