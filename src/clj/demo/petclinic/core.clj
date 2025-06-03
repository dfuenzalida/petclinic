(ns demo.petclinic.core
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [demo.petclinic.config :as config]
   [demo.petclinic.env :refer [defaults]]

    ;; Edges
   [kit.edge.server.undertow]
   [demo.petclinic.web.handler]

    ;; Routes
   [demo.petclinic.web.routes.api] 
    [demo.petclinic.web.routes.pages] 
    [kit.edge.db.sql.conman] 
    [kit.edge.db.sql.migratus])
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (fn [thread ex]
   (log/error {:what :uncaught-exception
               :exception ex
               :where (str "Uncaught exception on" (.getName thread))})))

(defonce system (atom nil))

(defn stop-app []
  ((or (:stop defaults) (fn [])))
  (some-> (deref system) (ig/halt!)))

(defn start-app [& [params]]
  ((or (:start params) (:start defaults) (fn [])))
  (->> (config/system-config (or (:opts params) (:opts defaults) {}))
       (ig/expand)
       (ig/init)
       (reset! system)))

(defn -main [& _]
  (start-app)
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (stop-app) (shutdown-agents)))))

(comment
  ;; Eval these to debug SQL queries
  (defn log-sqlvec [sqlvec]
    (log/info (->> sqlvec
                   (map #(clojure.string/replace (or % "") #"\n" ""))
                   (clojure.string/join " ; "))))

  (defn log-command-fn [this db sqlvec options]
    (log-sqlvec sqlvec)
    (condp contains? (:command options)
      #{:!} (hugsql.adapter/execute this db sqlvec options)
      #{:? :<!} (hugsql.adapter/query this db sqlvec options)))

  (defmethod hugsql.core/hugsql-command-fn :! [_sym] `log-command-fn)
  (defmethod hugsql.core/hugsql-command-fn :<! [_sym] `log-command-fn)
  (defmethod hugsql.core/hugsql-command-fn :? [_sym] `log-command-fn)

  )
