(ns love-letter-cljs.utils)

(defn find-card [game target]
  (-> game
      (get-in [:players target :hand])
      first))

(defn- valid-target? [current-player {:keys [id protected? alive?] :as player}]
  (and (not= current-player id)
       (and alive? (not protected?))))

(defn valid-targets [{:keys [current-player] :as game}]
  (-> game
      :players
      vals
      (->> (filter (partial valid-target? current-player))
           (map :id))))

(defn remove-first [coll face]
  (let [[pre post] (split-with #(not= face (:face %)) coll)]
    (vec (concat pre (rest post)))))


