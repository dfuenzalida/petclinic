(ns demo.petclinic.test-utils
  (:require
   [byte-streams :as bs]
   [demo.petclinic.core :as core]
   [demo.petclinic.web.pages.layout :as layout]
   [integrant.repl.state :as state]
   [peridot.core :as p]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [ring.util.http-response :refer [content-type ok]]
   [selmer.parser :as parser]))

(defn system-state
  []
  (or @core/system state/system))

(defn system-fixture
  []
  (fn [f]
    (core/start-app {:opts {:profile :test}})
    (f)
    (core/stop-app)))

(defn get-response [ctx]
  (-> ctx
      :response
      (update :body (fnil bs/to-string ""))))

(defn send-request [verb app path params headers]
  (-> (p/session app)
      (p/request path
                 :request-method verb
                 :content-type "application/edn"
                 :headers headers
                 :params params)
      (get-response)))

(defn GET [app path params headers]
  (send-request :get app path params headers))

(defn POST [app path params headers]
  (send-request :post app path params headers))

(defn get-cookie [response]
  (-> response :headers (get "Set-Cookie") first))

(defn get-csrf-token [response]
  (let [field-regex #"name=\"__anti-forgery-token\" type=\"hidden\" value=\"([A-Za-z0-9+/=]+)\""]
    (second (re-find field-regex (:body response)))))

(def captured-args (atom nil))

(defn render-capturing-args
  [request template & [params]]
  (reset! captured-args [request template params])
  (-> (parser/render-file template
                          (assoc params :page template :csrf-token *anti-forgery-token*)
                          layout/selmer-opts)
      (ok)
      (content-type "text/html; charset=utf-8")))