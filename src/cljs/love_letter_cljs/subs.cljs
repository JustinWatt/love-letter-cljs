(ns love-letter-cljs.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :refer [register-sub subscribe]]))

(register-sub
 :deck
 (fn [db]
   (reaction (get-in @db [:game :deck]))))

(register-sub
 :players
 (fn [db]
   (reaction
    (->> (get-in @db [:game :players])
         vals
         vec))))
