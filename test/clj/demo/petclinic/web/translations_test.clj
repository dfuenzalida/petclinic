(ns demo.petclinic.web.translations-test
  (:require
   [demo.petclinic.web.translations :refer [default-translations props-as-map request-language translations translate-key with-translation]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest are is testing]]))

(deftest request-language-tests
  (are [expected request] (= expected (request-language request))
    "en" {}
    "es" {:params {:lang "es"}}
    "fr" {:headers {"accept-language" "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5"}}
    "es" {:headers {"accept-language" "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5"} :params {:lang "es"}}
    ;;
    ))

(deftest props-as-map-tests
  (let [en-props (props-as-map default-translations)]
    (is (= "Welcome" (:welcome en-props)))))

(deftest translations-tests
  (let [es-translations (translations "es")]
    (is (= "Bienvenido" (:welcome es-translations)))))

(deftest with-translation-tests
  (let [with-ts (with-translation {:hello "World!"} {:params {:lang "es"}})]
    (is (= "World!" (:hello with-ts)))
    (is (= "Bienvenido" (get-in with-ts [:t :welcome])))))

(deftest translate-key-tests
  (are [expected request key] (= expected (translate-key request key))
    "Welcome"    {} :welcome
    "Bienvenido" {:params {:lang "es"}} :welcome
    "Bienvenido" {:headers {"accept-language" "fr-CH"} :params {:lang "es"}} :welcome
    ;;
    ))

(deftest translations-file-tests
  (testing "Check that all translations files have the same keys"
    (let [folder (io/file "resources/translations")
          files  (->> (file-seq folder)
                      (filter #(and (.isFile %) (.endsWith (.getName %) ".properties")))
                      (map #(.getName %))
                      (into (sorted-set)))
          files (disj files "messages_en.properties") ;; this file is empty on purpose, so we exclude it
          keys  (->> files
                     (map #(props-as-map (str "translations/" %)))
                     (map #(into (sorted-set) (keys %))))]
      (is (true? (apply = keys))))))
