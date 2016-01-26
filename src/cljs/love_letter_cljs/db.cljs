(ns love-letter-cljs.db
  [:require [love-letter-cljs.game :refer [create-game]]])

(def default-db {:game (create-game)})
