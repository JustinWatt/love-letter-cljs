(ns love-letter-cljs.utils)

(defn find-card [game target]
  (-> game
      (get-in [:players target :hand])
      peek))

(defn- valid-target? [current-player {:keys [id protected? alive?] :as player}]
  (and (not= current-player id)
       (and alive? (not protected?))))

(defn valid-targets [{:keys [current-player] :as game}]
  (-> game
     :players
     vals
     (->> (filter (partial valid-target? current-player))
      (map :id))))

(defn remove-first [face coll]
  (let [[pre post] (split-with #(not= face (:face %)) coll)]
    (vec (concat pre (rest post)))))

