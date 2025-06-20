(ns demo.petclinic.web.controllers.owners
  (:require
   [clojure.edn :as edn]
   [clojure.string :as s]
   [demo.petclinic.utils :refer [aggregate-by-key keywordize-keys]]
   [demo.petclinic.web.pages.layout :as layout]
   [demo.petclinic.web.translations :refer [translate-key with-translation]]
   [demo.petclinic.web.pagination :refer [PAGESIZE parse-page with-pagination]]
   [ring.util.http-response :refer [found]]
   [clojure.tools.logging :as log]))

(defn search-owners
  [{:keys [query-fn]} {{:strs [lastName page]} :query-params :as request}]
  (let [current-page (parse-page page 1)
        lastNameLike (str lastName "%")
        total-items  (:total (query-fn :get-owners-count {:lastNameLike lastNameLike}))
        owners       (query-fn :get-owners {:lastNameLike lastNameLike :pagesize PAGESIZE :page current-page})]

    (cond
      ;; No results: return to search page with error
      (zero? (count owners))
      (let [m        (with-translation {} request)
            notFound (translate-key request :notFound)]
        (layout/render request "owners/ownersFind.html"
                       (merge {:lastName lastName :errors [notFound]} m)))

      ;; If the request was a search by lastName AND only 1 owner was found: redirect to that owner's details
      (and (not (s/blank? lastName)) (= 1 (count owners)))
      (found (->> owners first :id (str "/owners/")))

      ;; else: show list of owners with their pets
      :else
      (let [pets   (query-fn :get-pets-by-owner-ids {:ownerids (map :id owners)})
            owners (aggregate-by-key owners :id pets :owner_id :pets)]
        (layout/render request "owners/ownersList.html"
                       (-> {:owners owners}
                           (with-pagination current-page total-items)
                           (with-translation request)))))))

(defn owners-find-form [_opts request]
  (layout/render request "owners/ownersFind.html" (with-translation {} request)))

(defn owners-new-form [_opts request]
  (layout/render request "owners/createOrUpdateOwnerForm.html"
                 (with-translation {:owner {} :new true} request)))

(defn owner-details [{:keys [query-fn]} {{:keys [ownerid]} :path-params {:keys [error message]} :flash :as request}]
  (try
    (let [ownerid (int (edn/read-string ownerid))
          owner   (query-fn :get-owner {:id ownerid})
          pets    (query-fn :get-pets-by-owner-ids {:ownerids [ownerid]})
          visits  (query-fn :get-visits-by-pet-ids {:petids (mapv :id pets)})
          pets    (aggregate-by-key pets :id visits :pet_id :visits)]

      (if (nil? owner)
        (throw (Exception.))
        (layout/render request "owners/ownerDetails.html"
                       (with-translation {:owner owner :pets pets :error error :message message} request))))
    (catch Exception _
      (let [not-found-message (translate-key request :notFound)]
        (layout/error-page (with-translation {:status 404 :message not-found-message} request))))))

(defn edit-owner-form [{:keys [query-fn]} {{:keys [ownerid]} :path-params :as request}]
  (let [ownerid (int (edn/read-string ownerid))
        owner   (query-fn :get-owner {:id ownerid})]
    (if (nil? owner)
      (throw (Exception.))
      (layout/render request "owners/createOrUpdateOwnerForm.html" (with-translation {:owner owner} request)))))

(defn upsert-owner!
  [create? {:keys [query-fn]} {{:keys [ownerid]} :path-params :as request}]

  ;; Form validation
  (let [owner (-> request :form-params keywordize-keys (merge {:id ownerid}))
        {:keys [first_name last_name address city telephone]} owner
        errors (cond-> {}
                 (empty? first_name) (assoc :first_name "must not be blank")
                 (empty? last_name) (assoc :last_name "must not be blank")
                 (empty? address) (assoc :address "must not be blank")
                 (empty? city) (assoc :city "must not be blank")
                 (not (re-matches #"\d{10}" (str telephone))) (assoc :telephone (translate-key request :telephone.invalid)))]

    (if (empty? errors)
      ;; No errors, update the owner in DB. `update-owner!` returns the number of rows updated.
      (let [result (try (if create?
                          (query-fn :create-owner! owner) ;; create returns a map of {:id new-row-id}
                          {:numrows (query-fn :update-owner! owner)})
                        (catch Exception _ {}))
            ownerid (if create? (:id result) ownerid)]
        (log/debug "Update result:" result)
        ;; Redirect to `/owners/:ownerid` with message or error if no rows were updated
        (let [flash (if (empty? result)
                      {:error "Error when updating owner"}
                      {:message (if create? "New Owner Created" "Owner values updated")})]
          (-> (found (str "/owners/" ownerid))
              (assoc :flash flash))))

      ;; Errors were found, render the form again with the data and errors
      (layout/render request "owners/createOrUpdateOwnerForm.html"
                     (with-translation {:owner owner :errors errors :new create?} request)))))

(defn update-owner! [opts request]
  (upsert-owner! false opts request))

(defn create-owner! [opts request]
  (upsert-owner! true opts request))
