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


(defn aggregate-by-key
  "Given collections of maps `xs` and `ys` with keys `kx` and `ky` so that `(= (kx x) (ky y))`,
   'updates' the xs with their the corresponding values of `ys` under the key `aggr-key`"
  [xs kx ys ky aggr-key]
  (let [xs-by-id (into {} (map (juxt kx identity) xs))
        ys-by-id (group-by ky ys)]
    (->> (reduce (fn [state [xid ys]] (assoc-in state [xid aggr-key] ys)) xs-by-id ys-by-id)
         vals)))


(comment
  (let [visits [{:pet_id 7, :visit_date #inst "2013-01-01", :description "rabies shot"}
                {:pet_id 7, :visit_date #inst "2013-01-04", :description "spayed"}
                {:pet_id 8, :visit_date #inst "2013-01-02", :description "rabies shot"}
                {:pet_id 8, :visit_date #inst "2013-01-03", :description "neutered"}]

        pets             [{:id 7, :name "Samantha", :birth_date #inst "2012-09-04", :pet_type "cat", :owner_id 6}
                          {:id 8, :name "Max", :birth_date #inst "2012-09-04", :pet_type "cat", :owner_id 6}]

        ;; pets-by-id      (->> (map (juxt :id identity) pets) (into {}))
        ]

    ;; Add the visits to each pet under :visits
    #_(->> (reduce (fn [state [petid vs]] (assoc-in state [petid :visits] vs)) pets-by-id visits-by-petid)
           vals)
    #_(->> (map (juxt :id identity) pets) (into {}))

    (aggregate-by-key pets :id visits :pet_id :visits))

  )
