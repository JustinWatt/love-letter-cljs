(ns love-letter-cljs.prod
  (:require [love-letter-cljs.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
