(ns demo.petclinic.web.routes.pages
  (:require
   [clojure.edn :as edn]
   [demo.petclinic.web.middleware.exception :as exception]
   [demo.petclinic.web.pages.layout :as layout]
   [integrant.core :as ig]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [clojure.tools.logging :as log]))

(defn wrap-page-defaults []
  (let [error-page (layout/error-page
                    {:status 403
                     :title "Invalid anti-forgery token"})]
    #(wrap-anti-forgery % {:error-response error-page})))

(defn home [_ request]
  (layout/render request "home.html" {}))

(def PAGESIZE 5)

(defn paginate
  "Given a context map `m`, a current page and the total of items, add pagination-related keys to `m`"
  [m current-page total-items]
  (let [total-pages (-> total-items (/ PAGESIZE) int inc)]
    (merge m
           {:pagination
            {:current current-page :total total-pages :pages (next (range (inc total-pages)))}})))

(defn parse-page
  "Attempts to parse `s` into an integer >= 1. Returns the greatest of 1 and `default` when parsing fails."
  [s default]
  (max 1 (try
           (int (edn/read-string s))
           (catch Exception _ default))))

(defn group-specialties
  "Given a list of vets and specialties by vet_id, groups the specialties by vet"
  [vets specs-by-vet]
  (let [vets-by-id (into {} (map (juxt :id identity) vets))]
    (->> (reduce
          (fn [m {:keys [vet_id specialty]}]
            (let [v (get-in m [vet_id :specialties] [])]
              (assoc-in m [vet_id :specialties] (into v [specialty]))))
          vets-by-id specs-by-vet)
         vals
         (sort-by :id))))

(comment
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
  )

(defn show-vets [{:keys [query-fn]} {{:strs [page]} :query-params :as request}]
  (let [current-page (parse-page page 1)
        total-items  (:total (query-fn :get-vets-count {}))
        vets         (query-fn :get-vets {:pagesize PAGESIZE :page current-page})
        vets-specs   (query-fn :specialties-by-vet-ids {:vetids (map :id vets)})
        vets         (group-specialties vets vets-specs)]
    ;; (log/info "vets:" vets)
    (layout/render request "vets.html"
                   (paginate
                    {:vets vets}
                    current-page total-items))))

;; Routes
(defn page-routes [opts]
  [["/" {:get (partial home opts)}]
   ["/vets" {:get (partial show-vets opts)}]
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

