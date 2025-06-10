(ns demo.petclinic.web.translations
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(defn request-language [request]
  (let [accept-language (some->> (get-in request [:headers "accept-language"])
                                 (re-seq #"[a-z]+")
                                 first)
        lang-param (get-in request [:params :lang])]
    (or lang-param accept-language "en")))

(defn props-as-map [filename]
  (let [props (java.util.Properties.)]
    (with-open [rdr (io/reader filename)]
      (.load props rdr))
    (->> (map (juxt keyword #(.getProperty props %))
              (.stringPropertyNames props))
         (into {}))))

(def default-translations
  "resources/translations/messages.properties")

(def translations
  (memoize
   (fn [language]
     (log/info "Loading translations for language" language)
     (let [default (props-as-map default-translations)]
       (try
         (let [lang-path (format "resources/translations/messages_%s.properties" language)
               transmap (props-as-map lang-path)]
           (merge default transmap))
         (catch Exception _ default))))))

(defn with-translation [m request]
  (let [language (request-language request)]
    (merge m {:t (translations language)})))

(defn translate-key
  "Find the translation for a single key. If not found, returns (str key)"
  [request key]
  (let [m (with-translation {} request)]
    (get-in m [:t key] (str key))))
