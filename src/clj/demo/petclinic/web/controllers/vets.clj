(ns demo.petclinic.web.controllers.vets
  (:require
   [demo.petclinic.utils :refer [group-properties]]
   [demo.petclinic.web.pages.layout :as layout]
   [demo.petclinic.web.pagination :refer [PAGESIZE parse-page with-pagination]]
   [demo.petclinic.web.translations :refer [with-translation]]))

(defn show-vets [{:keys [query-fn]} {{:strs [page]} :query-params :as request}]
  (let [current-page (parse-page page 1)
        total-items  (:total (query-fn :get-vets-count {}))
        vets         (query-fn :get-vets {:pagesize PAGESIZE :page current-page})
        vets-specs   (query-fn :specialties-by-vet-ids {:vetids (map :id vets)})
        vets         (group-properties vets vets-specs :id :id :specialties)]
    (layout/render request "vets/vetsList.html"
                   (-> {:vets vets}
                       (with-pagination current-page total-items)
                       (with-translation request)))))

