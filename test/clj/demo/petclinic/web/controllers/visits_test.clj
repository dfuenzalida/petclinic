(ns demo.petclinic.web.controllers.visits-test
  (:import
   [java.time LocalDate]
   [java.time.format DateTimeFormatter])
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure.string :refer [includes?]]
   [clojure.tools.logging :as log]
   [demo.petclinic.test-utils :refer [system-state system-fixture GET POST get-csrf-token get-cookie]]
   [ring.mock.request :as mock]))

(use-fixtures :once (system-fixture))

(deftest init-new-visits-form
  (testing "New Visit Form"
    (let [handler (:handler/ring (system-state))
          params {:lastName ""}
          headers {}
          response (GET handler "/owners/1/pets/1/visits/new" params headers)
          body (:body response)]
      (is (= 200 (:status response)))
      (is (includes? body "<h2>New Visit</h2>")))))

(deftest process-new-visit-form-success
  (testing "Successfully creating a new visit"
    (let [handler (:handler/ring (system-state))

          ;; Get the csrf token and cookie from the GET response
          get-response (GET handler "/owners/new" {} {})
          token (get-csrf-token get-response)
          cookie (get-cookie get-response)
          params {:__anti-forgery-token token
                  :visit_date (.format (LocalDate/now) DateTimeFormatter/ISO_LOCAL_DATE)
                  :description "Visit Description"}

          ;; Send the POST request to create the visit
          response (-> (mock/request :post "/owners/1/pets/1/visits/new" params)
                       (mock/header "x-csrf-token" token)
                       (mock/header "Cookie" cookie)
                       handler)]
      (is (= 302 (:status response)))
      (is (->> response :headers keys (some #{"Location"})))
      (is (includes? (get-in response [:headers "Location"]) "/owners/1")))))

(deftest process-new-visit-form-has-errors
  (testing "Errors when creating a new visit but missing required fields"
    (let [handler (:handler/ring (system-state))

          ;; Get the csrf token and cookie from the GET response
          get-response (GET handler "/owners/new" {} {})
          token (get-csrf-token get-response)
          cookie (get-cookie get-response)
          params {:__anti-forgery-token token
                  :name "George"}

          ;; Send the POST request to create the visit
          response (-> (mock/request :post "/owners/1/pets/1/visits/new" params)
                       (mock/header "x-csrf-token" token)
                       (mock/header "Cookie" cookie)
                       handler)]
      (is (= 200 (:status response)))
      (is (includes? (:body response) "must not be blank")))))
  