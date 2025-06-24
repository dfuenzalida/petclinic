(ns demo.petclinic.web.controllers.pets
   (:import
    [java.time LocalDate]
    [java.time.format DateTimeFormatter])
   (:require
    [clojure.edn :as edn]
    [demo.petclinic.utils :refer [keywordize-keys]]
    [demo.petclinic.web.pages.layout :as layout]
    [demo.petclinic.web.translations :refer [with-translation]]
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

(defn upsert-pet! [create? query-fn {{:keys [ownerid petid]} :path-params :as request}]
  (let [owner   (query-fn :get-owner {:id ownerid})
        pet (-> request :form-params keywordize-keys (assoc :ownerid ownerid))
        pet (if create? pet (assoc pet :id petid))
        {:keys [name birth_date type]} pet
        types (query-fn :get-types {})
        type_id (->> types (filter #(= type (:name %))) first :id)
        pet    (assoc pet :type_id type_id)

        ;; Validate Pet properties
        errors (cond-> {}
                 (empty? name) (assoc :name "must not be blank")
                 (empty? birth_date) (assoc :birth_date "must not be blank")
                 (empty? type) (assoc :type "must not be blank"))]

    (if (empty? errors)
      ;; No validation errors, persist create/update to the DB
      (let [db-fn  (if create? :create-pet! :update-pet!)
            result (try (query-fn db-fn pet) (catch Exception _ 0))
            flash (if (= 1 result) ;; Redirect to `/owners/:ownerid` with OK `message` or `error` if no rows were updated
                    {:message (if create? "New Pet has been Added" "Pet details have been updated")}
                    {:error "Error when updating pet"})]
        (-> (found (str "/owners/" ownerid))
            (assoc :flash flash)))

      ;; Errors were found, render the form again with the data and errors
      (layout/render request "pets/createOrUpdatePetForm.html"
                     (with-translation {:pet pet :owner owner :types types :errors errors :new create?} request)))))

(defn update-pet! [{:keys [query-fn]} request]
  (upsert-pet! nil query-fn request))

(defn create-pet! [{:keys [query-fn]} request]
  (upsert-pet! true query-fn request))

(defn new-visit-form [{:keys [query-fn]} {{:keys [ownerid petid]} :path-params :as request}]
  (let [ownerid (int (edn/read-string ownerid))
        owner   (query-fn :get-owner {:id ownerid})
        petid (int (edn/read-string petid))
        pet   (query-fn :get-pet {:id petid :ownerid ownerid})
        types (query-fn :get-types {})
        pet   (assoc pet :type (->> (filter #(= (:type_id pet) (:id %)) types) first :name))
        visits (query-fn :get-visits-by-pet-ids {:petids [petid]})
        visit  {:description "" :visit_date (.format (LocalDate/now) DateTimeFormatter/ISO_LOCAL_DATE)}]
    (if (some nil? [owner pet])
      (throw (Exception.))
      (layout/render request "pets/createOrUpdateVisitForm.html"
                     (with-translation {:owner owner :pet pet :visit visit :visits visits} request)))))

(defn create-visit! [{:keys [query-fn]} {{:keys [ownerid petid]} :path-params :as request}]
  (let [owner   (query-fn :get-owner {:id ownerid})
        petid (int (edn/read-string petid))
        pet   (query-fn :get-pet {:id petid :ownerid ownerid})
        types (query-fn :get-types {})
        pet   (assoc pet :type (->> (filter #(= (:type_id pet) (:id %)) types) first :name))
        visits (query-fn :get-visits-by-pet-ids {:petids [petid]})
        visit (-> request :form-params keywordize-keys (assoc :pet_id petid))
        {:keys [visit_date description]} visit

        ;; Validate visit properties
        errors (cond-> {}
                 (empty? visit_date) (assoc :visit_date "must not be blank")
                 (empty? description) (assoc :description "must not be blank"))]

    (if (empty? errors)
      ;; No validation errors, persist create/update to the DB
      (let [;;db-fn  (if create? :create-pet! :update-pet!)
            result (try (query-fn :create-visit! visit) (catch Exception e (log/error e) 0))
            flash (if (zero? result) ;; Redirect to `/owners/:ownerid` with OK `message` or `error` if no rows were updated
                    {:error "Error saving visit"}
                    {:message "Your visit has been booked"})]
        (-> (found (str "/owners/" ownerid))
            (assoc :flash flash)))

      ;; Errors were found, render the form again with the data and errors
      (layout/render request "pets/createOrUpdateVisitForm.html"
                     (with-translation {:pet pet :owner owner :types types :errors errors :visit visit :visits visits} request)))))
