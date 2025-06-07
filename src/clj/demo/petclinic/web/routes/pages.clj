(ns demo.petclinic.web.routes.pages
  (:require
   [demo.petclinic.web.controllers.owners :as owners]
   [demo.petclinic.web.controllers.pets :as pets]
   [demo.petclinic.web.controllers.vets :as vets]
   [demo.petclinic.web.middleware.exception :as exception]
   [demo.petclinic.web.pages.layout :as layout]
   [demo.petclinic.web.translations :refer [with-translation]]
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
  (layout/render request "home.html" (with-translation {} request)))

;; Routes
(defn page-routes [opts]
  [["/" {:get (partial home opts)}]
   ["/vets.html" {:get (partial vets/show-vets opts)}]
   ["/owners" {}
    ["" (partial owners/search-owners opts)]
    ["/:ownerid" {}
     ["" {:get  (partial owners/owner-details opts)
          :post (partial owners/create-owner! opts)}]
     ["/pets" {}
      ["/new" {:get (partial pets/create-pet-form opts)
               :post (partial pets/create-pet! opts)}]
      ["/:petid/edit" {:get (partial pets/edit-pet-form opts)
                       :post (partial pets/update-pet! opts)}]]
     ["/edit" {:get (partial owners/edit-owner-form opts)
               :post (partial owners/update-owner! opts)}]]]
   ["/oups" {:get (fn [& _] (throw (RuntimeException. "Expected: controller used to showcase what happens when an exception is thrown")))}]])

(comment
  ;; Troubleshoot routes:
  (require '[reitit.core :as r])
  (clojure.pprint/pprint
   (r/routes (r/router (page-routes {}))))
  
  )

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

