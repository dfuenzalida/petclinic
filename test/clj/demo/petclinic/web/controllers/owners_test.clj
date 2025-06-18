(ns demo.petclinic.web.controllers.owners-test
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure.string :refer [includes?]]
   [clojure.tools.logging :as log]
   [demo.petclinic.web.pagination :as pagination]
   [demo.petclinic.test-utils :refer [system-state system-fixture GET POST get-cookie get-csrf-token]]))

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
    (let [handler (:handler/ring (system-state))
          params {}
          headers {}
          response (GET handler "/owners/new" params headers)
          body (:body response)]
      (is (= 200 (:status response)))
      (is (includes? body "<span>Owner</span>"))
      (is (includes? body ">First Name</label>"))
      (is (includes? body ">Last Name</label>"))
      (is (includes? body ">Address</label>"))
      (is (includes? body ">City</label>"))
      (is (includes? body ">Telephone</label>"))
      ;;
      ))

  (testing "Owners creation form with missing required fields"
    (let [handler (:handler/ring (system-state))
          get-response (GET handler "/owners/new" {} {})
          token (get-csrf-token get-response)
          cookie (get-cookie get-response)
          headers {:x-csrf-token token :Cookie cookie}
          params {:first_name "" :last_name "" :address "" :city "" :telephone ""}
          response (POST handler "/owners/new" params headers)
          body (:body response)]
      (is (= 200 (:status response)))
      (is (includes? body "must not be blank"))
      (is (includes? body "Telephone must be a 10-digit number"))))

  ;;
  )
