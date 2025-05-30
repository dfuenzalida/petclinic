(ns demo.petclinic.web.translations
  (:require [clojure.java.io :as io]))

(defn request-language [request]
  (let [accept-language (get-in request [:headers "accept-language"] "en")]
    (first (re-seq #"[a-z]+" accept-language))))

;; TODO move and use delay/memoize to compute only once
(defn props-as-map [filename]
  (let [props (java.util.Properties.)]
    (with-open [rdr (io/reader filename)]
      (.load props rdr))
    (->> (map (juxt keyword #(.getProperty props %))
              (.stringPropertyNames props))
         (into {}))))

(def default-translations
  "resources/translations/messages.properties")

;; TODO memoize this
(defn translations [language]
  (let [default (props-as-map default-translations)]
    (try
      (let [lang-path (format "resources/translations/messages_%s.properties" language)
            transmap (props-as-map lang-path)]
        (merge default transmap))
      (catch Exception _ default))))

(defn with-translation [m request]
  (let [language (request-language request)]
    (merge m {:t (translations language)})))

(comment
  (:pet (props-as-map "resources/translations/messages_es.properties"))
  (translations "es")

  (with-translation {:hello "world"} {:headers {"accept-language" "es"}}))

(comment
  ;; language preference
  ;; TODO move into test
  (= "fr" (request-language {:headers {"accept-language" "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5"}})))

(comment
  ;; Message translation support
  (require '[clojure.java.io :as io])
  ;; Loading a properties file
  (let [props (java.util.Properties.)]
    (with-open [rdr (io/reader "resources/translations/messages_es.properties")]
      (.load props rdr))
    (.stringPropertyNames props)
    #_(.getProperty props "findOwner"))

  ;; Idea: create a cache of translations with a delay so they
  ;; are loaded only once when needed. The properties need to be
  ;; turned into nested maps:
  ;; {:es {:name "Nombre" ,,,} :en {,,,}}
  ;;
  ;; A `translate` or `with-translation` would take a map
  ;; and add the translations for the given language in the
  ;; context map to be passed to the view (under the :t keyword)
  ;; so we can have {{t.findOwner}}. 
  ;; Probably the map needs to be built from the english keys
  ;; overridden with the translations so that missing translations
  ;; are rendered in english.
  ;; Another option is {{t.findOwner|default:"Find owner"}}
  ;;
  ;; Need something to list the resources, but inside the JAR,
  ;; using resources
  (->> (java.io.File. "resources/translations") .list (into []))
  ;; (into [] (-> java.io.File .getClassLoader (.getResourceAsStream "resources")))
  )
