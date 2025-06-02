(ns demo.petclinic.web.controllers.pets
   (:require
    [clojure.edn :as edn]
    [demo.petclinic.utils :refer [group-properties keywordize-keys]]
    [demo.petclinic.web.pages.layout :as layout]
    [demo.petclinic.web.translations :refer [translate-key with-translation]]
    [demo.petclinic.web.pagination :refer [PAGESIZE parse-page with-pagination]]
    [ring.util.http-response :refer [found]]
    [clojure.tools.logging :as log]))

(defn edit-pet-form [{:keys [query-fn]} {{:keys [ownerid petid]} :path-params :as request}]
  (let [ownerid (int (edn/read-string ownerid))
        owner   (query-fn :get-owner {:id ownerid})
        petid (int (edn/read-string petid))
        pet   (query-fn :get-pet {:id petid :ownerid ownerid})
        types (query-fn :get-types {})]
    (if (some nil? [owner pet])
      (throw (Exception.))
      (layout/render request "pets/createOrUpdatePetForm.html"
                     (with-translation {:owner owner :pet pet :types types} request)))))

