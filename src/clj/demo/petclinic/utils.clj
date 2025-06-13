(ns demo.petclinic.utils)

(defn group-properties
  "Given collections of maps `xs` and `ys` with functions `id1` and `id2`,
   'updates' the `xs` maps with the aggregation of `key` applied to their corresponding `ys`
   where `(= (id1 x) (id2 y))`"
  [xs ys id1 id2 key]
  (let [items-by-id (into {} (map (juxt id1 identity) xs))]
    (->> (reduce
          (fn [m props]
            (let [k (id2 props)
                  xs (get-in m [k key] [])]
              (assoc-in m [k key] (into xs [(key props)]))))
          items-by-id ys)
         vals
         (sort-by :id))))

(defn keywordize-keys
  "Applies `keyword` to the keys of a map `m`"
  [m]
  (into {} (map (fn [[k v]] [(keyword k) v]) m)))
