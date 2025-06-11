(ns demo.petclinic.web.controllers.owners
  (:require
   [clojure.edn :as edn]
   [clojure.string :as s]
   [demo.petclinic.utils :refer [group-properties keywordize-keys]]
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
        owners       (query-fn :get-owners {:lastNameLike lastNameLike :pagesize PAGESIZE :page current-page})
        pets-by-owner (query-fn :get-pets-by-owner-ids {:ownerids (map :id owners)})
        owners       (group-properties owners pets-by-owner :id :owner_id :name)]
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

      ;; else: show list of owners
      :else
      (layout/render request "owners/ownersList.html"
                     (-> {:owners owners}
                         (with-pagination current-page total-items)
                         (with-translation request))))))

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
          visits  (->> (query-fn :get-visits-by-pet-ids {:petids (mapv :id pets)})
                       (group-by :pet_id))
          pets-by-id (->> (map (juxt :id identity) pets) (into {}))
          pets    (->> (reduce (fn [state [petid vs]]
                                 (assoc-in state [petid :visits] vs)) pets-by-id visits)
                       vals)]
      (if (nil? owner)
        (throw (Exception.))
        (layout/render request "owners/ownerDetails.html"
                       (with-translation {:owner owner :pets pets :error error :message message} request))))
    (catch Exception _
      (let [not-found-message (translate-key request :notFound)]
        (layout/error-page (with-translation {:status 404 :message not-found-message} request))))))

(comment
  (let [visits-by-petid {7 [{:pet_id 7, :visit_date #inst "2013-01-01", :description "rabies shot"}
                            {:pet_id 7, :visit_date #inst "2013-01-04", :description "spayed"}],
                         8 [{:pet_id 8, :visit_date #inst "2013-01-02", :description "rabies shot"}
                            {:pet_id 8, :visit_date #inst "2013-01-03", :description "neutered"}]}

        pets             [{:id 7, :name "Samantha", :birth_date #inst "2012-09-04", :pet_type "cat", :owner_id 6}
                          {:id 8, :name "Max", :birth_date #inst "2012-09-04", :pet_type "cat", :owner_id 6}]

        pets-by-id      (->> (map (juxt :id identity) pets) (into {}))
        ]

    ;; Add the visits to each pet under :visits
    (->> (reduce (fn [state [petid vs]] (assoc-in state [petid :visits] vs)) pets-by-id visits-by-petid)
         vals)
    #_(->> (map (juxt :id identity) pets) (into {}))
    )

  )

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
                 (not (re-matches #"\d{10}" telephone)) (assoc :telephone (translate-key request :telephone.invalid)))]

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
      (layout/render request "owners/createOrUpdateOwnerForm.html" (with-translation {:owner owner :errors errors :new create?} request)))))

(defn update-owner! [opts request]
  (upsert-owner! false opts request))

(defn create-owner! [opts request]
  (upsert-owner! true opts request))
