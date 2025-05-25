(ns demo.petclinic.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[petclinic starting]=-"))
   :start      (fn []
                 (log/info "\n-=[petclinic started successfully]=-"))
   :stop       (fn []
                 (log/info "\n-=[petclinic has shut down successfully]=-"))
   :middleware (fn [handler _] handler)
   :opts       {:profile :prod}})
