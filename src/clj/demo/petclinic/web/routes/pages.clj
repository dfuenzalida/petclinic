(ns demo.petclinic.web.routes.pages
  (:require
   [clojure.edn :as edn]
   [demo.petclinic.web.middleware.exception :as exception]
   [demo.petclinic.web.pages.layout :as layout]
   [demo.petclinic.web.translations :as tr]
   [integrant.core :as ig]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.util.http-response :refer [found]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [clojure.tools.logging :as log]))

(defn wrap-page-defaults []
  (let [error-page (layout/error-page
                    {:status 403
                     :title "Invalid anti-forgery token"})]
    #(wrap-anti-forgery % {:error-response error-page})))

(defn home [_ request]
  (layout/render request "home.html" (tr/with-translation {} request)))

(def PAGESIZE 5)

(defn with-pagination
  "Given a context map `m`, a current page and the total of items, add pagination-related keys to `m`"
  [m current-page total-items]
  (let [total-pages (-> total-items (/ PAGESIZE) Math/ceil int)]
    (merge m
           {:pagination
            {:current current-page :total total-pages :pages (next (range (inc total-pages)))}})))

(defn parse-page
  "Attempts to parse `s` into an integer >= 1. Returns the greatest of 1 and `default` when parsing fails."
  [s default]
  (max 1 (try
           (int (edn/read-string s))
           (catch Exception _ default))))

(defn group-properties
  "Given maps representing `items` with some `id1` and another list of `items with an `id2` and some other `key`, 'updates' the list of maps with the collection of `:prop`s where `id1` equals `id2`"
  [items extra-props id1 id2 key]
  (let [items-by-id (into {} (map (juxt id1 identity) items))]
    (->> (reduce
          (fn [m props]
            (let [k (id2 props)
                  xs (get-in m [k key] [])]
              (assoc-in m [k key] (into xs [(key props)]))))
          items-by-id extra-props)
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
    (group-properties vets specs :id :vet_id :specialty))
  )

;; TODO
;; No results => stay if search page with error
;; If searching yields one result, we open the owners detail page
;; otherwise we show the list of results [implemented]
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
      0 (let [m (tr/with-translation {} request)]
          (layout/render request "ownersFind.html"
                         (merge {:lastName lastName :errors [(get-in m [:t :notFound])]} m)))

      ;; 1 result: redirect to owner details
      1 (found (->> owners first :id (str "/owners/")))

      ;; More than 1 result: show list of owners
      (layout/render request "owners.html"
                     (-> {:owners owners}
                         (with-pagination current-page total-items)
                         (tr/with-translation request))))))

(defn owner-details [{:keys [query-fn]} {{:keys [ownerid]} :path-params :as request}]
  (condp = ownerid
    "find"
    (layout/render request "ownersFind.html" (tr/with-translation {} request))

    ;; otherwise expect the ownerid to be a proper int
    (let [owner (query-fn :get-owner {:id ownerid})
          pets (query-fn :get-pets-by-owner-ids {:ownerids [ownerid]})]
      (layout/render request "ownerDetails.html" (tr/with-translation {:owner owner :pets pets} request)))))

(defn show-vets [{:keys [query-fn]} {{:strs [page]} :query-params :as request}]
  (let [current-page (parse-page page 1)
        total-items  (:total (query-fn :get-vets-count {}))
        vets         (query-fn :get-vets {:pagesize PAGESIZE :page current-page})
        vets-specs   (query-fn :specialties-by-vet-ids {:vetids (map :id vets)})
        vets         (group-properties vets vets-specs :id :id :specialties)]
    (layout/render request "vets.html"
                   (-> {:vets vets}
                       (with-pagination current-page total-items)
                       (tr/with-translation request)))))

;; Routes
(defn page-routes [opts]
  [["/" {:get (partial home opts)}]
   ["/vets.html" {:get (partial show-vets opts)}]
   ["/owners" {:get (partial search-owners opts)}]
   ["/owners/:ownerid" {:get (partial owner-details opts)}]
   ["/oups" {:get (fn [& _] (throw (RuntimeException. "Expected: controller used to showcase what happens when an exception is thrown")))}]])

(def route-data
  {:middleware
   [;; Default middleware for pages
    (wrap-page-defaults)
    ;; query-params & form-params
    parameters/parameters-middleware
    ;; encoding response body
    muuntaja/format-response-middleware
    ;; exception handling
    exception/wrap-exception]})

(derive :reitit.routes/pages :reitit/routes)

(defmethod ig/init-key :reitit.routes/pages
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (layout/init-selmer! opts)
  (fn [] [base-path route-data (page-routes opts)]))

