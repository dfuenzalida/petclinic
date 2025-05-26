(ns demo.petclinic.web.controllers.vets
    (:require
    [clojure.tools.logging :as log]
    [ring.util.http-response :as http-response]))

(defn get-all-vets
  [{:keys [query-fn]} {{:strs [name]} :form-params :as request}]
  (log/debug "Getting all vets")
  (query-fn :get-vets {}))
