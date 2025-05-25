(ns demo.petclinic.env
  (:require
    [clojure.tools.logging :as log]
    [demo.petclinic.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[petclinic starting using the development or test profile]=-"))
   :start      (fn []
                 (log/info "\n-=[petclinic started successfully using the development or test profile]=-"))
   :stop       (fn []
                 (log/info "\n-=[petclinic has shut down successfully]=-"))
   :middleware wrap-dev
   :opts       {:profile       :dev}})
