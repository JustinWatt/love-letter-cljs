(ns love-letter-cljs.utils)

(defn find-card [game target]
  (-> game
      (get-in [:players target :hand])
      first))

(defn- valid-target? [current-player {:keys [id protected? alive?]}]
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

;; For cycling turns
(defn next-in-list [item-list current]
  (as-> item-list i
    (filter #(> % current) i)
    (or (first i)
        (first item-list))))

(defn player-list [game]
  (->> game
       :players
       vals
       (filter :alive?)
       (mapv :id)))

(defn set-phase [game phase]
  (assoc-in game [:phase] phase))
