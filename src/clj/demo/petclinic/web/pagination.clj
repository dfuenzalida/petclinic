(ns demo.petclinic.web.pagination
  (:require [clojure.edn :as edn]))

(def PAGESIZE 5)

(defn with-pagination
  "Given a context map `m`, a current page and the total of items, add pagination-related keys to `m`"
  [m current-page total-items]
  (let [total-pages (-> total-items (/ PAGESIZE) Math/ceil int)]
    (merge m
           {:pagination
            {:current current-page :total total-pages :pages (next (range (inc total-pages)))}})))

(defn parse-page
  "Attempts to parse `s` into an integer >= 1. Returns the greatest of 1 and `default` when parsing fails."
  [s default]
  (max 1 (try
           (int (edn/read-string s))
           (catch Exception _ default))))