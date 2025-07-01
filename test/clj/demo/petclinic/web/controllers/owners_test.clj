(ns demo.petclinic.web.controllers.owners-test
  (:require
   [clojure.string :refer [includes?]]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [demo.petclinic.test-utils :refer [GET get-cookie get-csrf-token POST system-fixture
                                      system-state render-capturing-args captured-args]]
   [demo.petclinic.web.pages.layout :as layout]
   [demo.petclinic.web.pagination :as pagination]
   [ring.mock.request :as mock])
  (:import
   [java.util Base64]))

(use-fixtures :once (system-fixture))

(defonce owner-names
  ["George Franklin" "Betty Davis" "Eduardo Rodriquez" "Harold Davis" "Peter McTavish"
   "Jean Coleman" "Jeff Black" "Maria Escobito" "David Schroeder" "Carlos Estaban"])

(deftest owners-list-tests []
  (testing "Owners list page 1"
    (let [handler (:handler/ring (system-state))
          params {:lastName ""}
          headers {}
          response (GET handler "/owners" params headers)
          body (:body response)]
      (is (= 200 (:status response)))
      (is (includes? body "<a href=\"?page=2\" title=\"Next\" class=\"fa fa-step-forward\"></a>"))
      (dorun
       (for [name (take pagination/PAGESIZE owner-names)]
         (is (includes? body (format ">%s</a></td>" name)))))))

  (testing "Owners list page 2"
    (let [handler (:handler/ring (system-state))
          params {}
          headers {}
          response (GET handler "/owners?page=2" params headers)
          body (:body response)]
      (is (= 200 (:status response)))
      (is (includes? body "<a href=\"?page=1\" title=\"Previous\" class=\"fa fa-step-backward\"></a>"))
      (dorun
       (for [name (drop pagination/PAGESIZE owner-names)]
         (is (includes? body (format ">%s</a></td>" name)))))))

  (testing "Owners list with known lastName filter"
    (let [handler (:handler/ring (system-state))
          params {:lastName "Davis"}
          headers {}
          response (GET handler "/owners" params headers)
          body (:body response)]
      (is (= 200 (:status response)))
      (is (includes? body ">Betty Davis</a></td>"))
      (is (includes? body ">Harold Davis</a></td>"))))

  (testing "Owners list with when lastName filter finds no matches"
    (let [handler (:handler/ring (system-state))
          params {:lastName "xyz"}
          headers {}
          response (GET handler "/owners" params headers)
          body (:body response)]
      (is (= 200 (:status response)))
      (is (includes? body "has not been found")))))

(deftest owners-creation-tests
  (testing "Owners creation form"
    (with-redefs [layout/render render-capturing-args]
      (let [handler (:handler/ring (system-state))
            params {}
            headers {}
            response (GET handler "/owners/new" params headers)
            [_req template _params] @captured-args
            body (:body response)]
        (is (= 200 (:status response)))
        (is (= "owners/createOrUpdateOwnerForm.html" template))
        (is (includes? body "<span>Owner</span>"))
        (is (includes? body ">First Name</label>"))
        (is (includes? body ">Last Name</label>"))
        (is (includes? body ">Address</label>"))
        (is (includes? body ">City</label>"))
        (is (includes? body ">Telephone</label>"))
        ;;
        )))

  (testing "Owners creation form with missing required fields"
    (with-redefs [layout/render render-capturing-args]
      (let [handler (:handler/ring (system-state))
            get-response (GET handler "/owners/new" {} {})
            token (get-csrf-token get-response)
            cookie (get-cookie get-response)
            headers {:x-csrf-token token :Cookie cookie}
            params {:first_name "" :last_name "" :address "" :city "" :telephone ""}
            response (POST handler "/owners/new" params headers)
            [_req template _params] @captured-args
            body (:body response)]
        (is (= 200 (:status response)))
        (is (= "owners/createOrUpdateOwnerForm.html" template))
        (is (= #{:first_name :last_name :address :city :telephone}
               (set (keys (:errors _params)))))
        (is (includes? body "must not be blank"))
        (is (includes? body "Telephone must be a 10-digit number")))))

  (testing "Owners creation form with valid data"
    (let [handler (:handler/ring (system-state))

          ;; Get the csrf token and cookie from the GET response
          get-response (GET handler "/owners/new" {} {})
          token (get-csrf-token get-response)
          cookie (get-cookie get-response)
          base64 (.withoutPadding (Base64/getEncoder))

          ;; Generate a random last name for the owner
          last-name (->> (random-uuid) str .getBytes (.encodeToString base64) (take 10) (apply str))
          params {:__anti-forgery-token token
                  :first_name "John"
                  :last_name last-name
                  :address "123 Elm St"
                  :city "Springfield"
                  :telephone "1234567890"}

          ;; Send the POST request to create the owner
          response (-> (mock/request :post "/owners/new" params)
                       (mock/header "x-csrf-token" token)
                       (mock/header "Cookie" cookie)
                       handler)]
      (is (= 302 (:status response)))
      (is (->> response :headers keys (some #{"Location"})))
      (is (includes? (get-in response [:headers "Location"]) "/owners"))

      ;; Finally, check if the owner was created in the DB
      (let [query-fn (:db.sql/query-fn (system-state))
            params {:lastNameLike last-name :pagesize 1 :page 1}
            owner  (-> (query-fn :get-owners params) first)]
        (is (= "John" (:first_name owner)))
        (is (= last-name (:last_name owner)))
        (is (= "123 Elm St" (:address owner)))
        (is (= "Springfield" (:city owner)))
        (is (= "1234567890" (:telephone owner))))))
  ;;
  )
