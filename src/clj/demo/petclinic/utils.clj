(ns demo.petclinic.utils)

(defn group-properties
  "Given collections of maps `xs` and `ys` with id functions `id1` and `id2`,
   'updates' the `xs` maps with their corresponding aggregation of `key` applied to `ys`"
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

(comment
  ;; TODO create a test with this
  (let [vets [{:id 1 :first_name "James" :last_name "Carter"}
              {:id 2 :first_name "Helen" :last_name "Leary"}
              {:id 3 :first_name "Linda" :last_name "Douglas"}]
        specs [{:vet_id 2 :specialty "radiology"}
               {:vet_id 3 :specialty "dentistry"}
               {:vet_id 3 :specialty "surgery"}]
        vets-by-id (into {} (map (juxt :id identity) vets))]
    (reduce (fn [m {:keys [vet_id specialty]}]
              (let [v (get-in m [vet_id :specialties] '())]
                (update-in m [vet_id :specialties] conj specialty)))
            vets-by-id specs)
    #_(group-by :id vets))

  (let [vets [{:id 1 :first_name "James" :last_name "Carter"}
              {:id 2 :first_name "Helen" :last_name "Leary"}
              {:id 3 :first_name "Linda" :last_name "Douglas"}]
        specs [{:vet_id 2 :specialty "radiology"}
               {:vet_id 3 :specialty "dentistry"}
               {:vet_id 3 :specialty "surgery"}]]
    (group-properties vets specs :id :vet_id :specialty)))