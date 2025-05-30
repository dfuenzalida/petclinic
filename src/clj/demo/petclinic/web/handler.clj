(ns demo.petclinic.web.handler
  (:require
    [clojure.tools.logging :as log]
    [demo.petclinic.web.middleware.core :as middleware]
    [integrant.core :as ig]
    [ring.util.http-response :as http-response]
    [reitit.ring :as ring]
    [reitit.swagger-ui :as swagger-ui]
    [demo.petclinic.web.pages.layout :as layout]
    [demo.petclinic.web.translations :as tr]))

(defmethod ig/init-key :handler/ring
  [_ {:keys [router api-path] :as opts}]
  (ring/ring-handler
   (router)
   (ring/routes
    ;; Handle trailing slash in routes - add it + redirect to it
    ;; https://github.com/metosin/reitit/blob/master/doc/ring/slash_handler.md
    (ring/redirect-trailing-slash-handler)
    (ring/create-resource-handler {:path "/"})
    (when (some? api-path)
      (swagger-ui/create-swagger-ui-handler {:path api-path
                                             :url  (str api-path "/swagger.json")}))
    (ring/create-default-handler
     {:not-found
      (fn [req]
        (layout/error-page
         (tr/with-translation {:status 404 :title "Not found" :message "Page not found."} req)))

      :method-not-allowed
      (fn [req]
        (layout/error-page (tr/with-translation {:status 405 :title "Not allowed"} req)))

      :not-acceptable
      (fn [req] (layout/error-page (tr/with-translation {:status 406 :title "Not acceptable"} req)))}))
   {:middleware [(middleware/wrap-base opts)]}))

(defmethod ig/init-key :router/routes
  [_ {:keys [routes]}]
  (mapv (fn [route]
          (if (fn? route)
            (route)
            route))
        routes))

(defmethod ig/init-key :router/core
  [_ {:keys [routes env] :as opts}]
  (if (= env :dev)
    #(ring/router ["" opts routes])
    (constantly (ring/router ["" opts routes]))))
