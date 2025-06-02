(ns demo.petclinic.web.controllers.owners
  (:require
   [clojure.edn :as edn]
   [demo.petclinic.utils :refer [group-properties]]
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
    (condp = (count owners)
      ;; No results: return to search page with error
      0 (let [m        (with-translation {} request)
              notFound (translate-key request :notFound)]
          (layout/render request "owners/ownersFind.html"
                         (merge {:lastName lastName :errors [notFound]} m)))

      ;; 1 result: redirect to owner details
      1 (found (->> owners first :id (str "/owners/")))

      ;; More than 1 result: show list of owners
      (layout/render request "owners/ownersList.html"
                     (-> {:owners owners}
                         (with-pagination current-page total-items)
                         (with-translation request))))))

(defn owner-details [{:keys [query-fn]} {{:keys [ownerid]} :path-params {:keys [error message]} :flash :as request}]
  (condp = ownerid
    "find"
    (layout/render request "owners/ownersFind.html" (with-translation {} request))

    ;; Attempt to read the param as an int. If fails or owner not found, show error
    (try
      (let [ownerid (int (edn/read-string ownerid))
            owner   (query-fn :get-owner {:id ownerid})
            pets    (query-fn :get-pets-by-owner-ids {:ownerids [ownerid]})]
        (if (nil? owner)
          (throw (Exception.))
          (layout/render request "owners/ownerDetails.html"
                         (with-translation {:owner owner :pets pets :error error :message message} request))))
      (catch Exception _
        (let [not-found-message (translate-key request :notFound)]
          (layout/error-page (with-translation {:status 404 :message not-found-message} request)))))))

(defn edit-owner-form [{:keys [query-fn]} {{:keys [ownerid]} :path-params :as request}]
  (let [ownerid (int (edn/read-string ownerid))
        owner   (query-fn :get-owner {:id ownerid})]
    (if (nil? owner)
      (throw (Exception.))
      (layout/render request "owners/createOrUpdateOwnerForm.html" (with-translation {:owner owner} request)))))

(defn save-owner
  [{:keys [query-fn]}
   {{:keys [ownerid]} :path-params {:strs [first_name last_name address city telephone]} :form-params :as request}]
  
  ;; Form validation. If there are errors, redirect to the edit page with the errors map
  (let [owner {:id ownerid :first_name first_name :last_name last_name :address address :city city :telephone telephone}
        errors (cond-> {}
                 (empty? first_name) (assoc-in [:errors :first_name] "must not be blank")
                 (empty? last_name) (assoc-in [:errors :last_name] "must not be blank")
                 (empty? address) (assoc-in [:errors :address] "must not be blank")
                 (empty? city) (assoc-in [:errors :city] "must not be blank")
                 (not (re-matches #"\d{10}" telephone)) (assoc-in [:errors :telephone] (translate-key request :telephone.invalid)))]

    (if (= errors {})
      ;; No errors, update the owner in DB
      (let [result (try (query-fn :update-owner! owner) (catch Exception _ 0))]
        (log/debug "Update result:" result)
        ;; When all validations succeed, save and redirect to /owners/:ownerid with a flash message
        (let [flash (if (= 1 result) {:message "Owner values updated"} {:error "Error when updating owner"})]
          (-> (found (str "/owners/" ownerid))
              (assoc :flash flash))))

      ;; errors found, render the form with the errors
      (layout/render request "owners/createOrUpdateOwnerForm.html" (-> {:owner owner} (merge errors) (with-translation request))))))

(comment
  (let [owner {"id" "123" "first_name" "John" "last_name" "Jones"}]
    (->> (mapv (fn [[k v]] [(keyword k) v]) owner)
         (into {})))
  )