(ns demo.petclinic.web.controllers.pets
   (:require
    [clojure.edn :as edn]
    [demo.petclinic.utils :refer [group-properties keywordize-keys]]
    [demo.petclinic.web.pages.layout :as layout]
    [demo.petclinic.web.translations :refer [translate-key with-translation]]
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

(defn create-pet-form [{:keys [query-fn]} {{:keys [ownerid]} :path-params :as request}]
  (let [ownerid (int (edn/read-string ownerid))
        owner   (query-fn :get-owner {:id ownerid})
        types (query-fn :get-types {})]
    (if (nil? owner)
      (throw (Exception.))
      (layout/render request "pets/createOrUpdatePetForm.html"
                     (with-translation {:owner owner :types types :new true} request)))))

(defn update-pet! [{:keys [query-fn]} {{:keys [ownerid petid]} :path-params :as request}]
  (let [owner   (query-fn :get-owner {:id ownerid})
        pet (-> request :form-params keywordize-keys (merge {:id petid :ownerid ownerid}))
        {:keys [name birthDate type]} pet
        typeId (->> (query-fn :get-types {}) (filter #(= type (:name %))) first :id int)

        ;; Validate Pet properties
        errors (cond-> {}
                 (empty? name) (assoc :name "must not be blank")
                 (empty? birthDate) (assoc :birth_date "must not be blank")
                 (empty? type) (assoc :type "must not be blank"))
        pet    (assoc pet :typeId typeId)]

    (if (empty? errors)
      ;; No errors, update the owner in DB. `update-owner!` returns the number of rows updated.
      (let [result (try (query-fn :update-pet! pet) (catch Exception _ 0))]
        ;; Redirect to `/owners/:ownerid` with message or error if no rows were updated
        (let [flash (if (= 1 result) {:message "Pet details have been updated"} {:error "Error when updating pet"})]
          (-> (found (str "/owners/" ownerid))
              (assoc :flash flash))))

      ;; Errors were found, render the form again with the data and errors
      (layout/render request "pets/createOrUpdatePetForm.html"
                     (with-translation {:pet pet :owner owner :errors errors} request)))))

(defn create-pet! [{:keys [query-fn]} {{:keys [ownerid]} :path-params :as request}]
  )