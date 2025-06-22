(ns demo.petclinic.web.controllers.vets
  (:require
   [clojure.data.json :as json]
   [clojure.data.xml :as xml]
   [clojure.set :refer [rename-keys]]
   [demo.petclinic.utils :refer [aggregate-by-key]]
   [demo.petclinic.web.pages.layout :as layout]
   [demo.petclinic.web.pagination :refer [PAGESIZE parse-page with-pagination]]
   [demo.petclinic.web.translations :refer [with-translation]]
   [ring.util.http-response :as http-response]))

(defn vets-with-specialties [query-fn pagesize page]
  (let [vets         (query-fn :get-vets {:pagesize pagesize :page page})
        vets-specs   (->> (query-fn :specialties-by-vet-ids {:vetids (map :id vets)})
                          (sort-by :name))]
    (->> (aggregate-by-key vets :id vets-specs :vet_id :specialties)
         ;; Remove the :vet_id property from the specialties
         (mapv (fn [vet] (update-in vet [:specialties] (fn [ss] (mapv #(dissoc % :vet_id) ss))))))))

(defn show-vets [{:keys [query-fn]} {{:strs [page]} :query-params :as request}]
  (let [current-page (parse-page page 1)
        total-items  (:total (query-fn :get-vets-count {}))
        vets         (vets-with-specialties query-fn PAGESIZE current-page)]
    (layout/render request "vets/vetsList.html"
                   (-> {:vets vets}
                       (with-pagination current-page total-items)
                       (with-translation request)))))

(declare to-xml)

(defn show-vets-data [{:keys [query-fn]} {{:strs [accept]} :headers}]
  (let [total-items (:total (query-fn :get-vets-count {}))
        vets        (vets-with-specialties query-fn total-items 1)
        ;; Not all keys need renaming but adding them here will keep the order in the output
        new-names   {:id :id, :first_name :firstName, :last_name :lastName, :specialties :specialties}
        vets        (mapv #(rename-keys % new-names) vets)]

    (cond
      (= accept "application/json")
      (-> (json/write-str {:vetList vets})
          (http-response/ok)
          (http-response/content-type "application/json"))

      (= accept "application/edn")
      (-> (pr-str {:vetList vets})
          (http-response/ok)
          (http-response/content-type "application/edn"))

      :else
      (-> {:vetsList (map #(hash-map :vet %) vets)}
          to-xml
          xml/emit-str
          http-response/ok
          (http-response/content-type "application/xhtml+xml")))))

(defn to-xml [x]
  (cond
    (map? x) (map (fn [[k v]] (xml/element k {} (to-xml v))) x)
    (coll? x) (map to-xml x)
    :else x))
