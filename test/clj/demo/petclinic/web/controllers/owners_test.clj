(ns demo.petclinic.web.controllers.owners-test
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure.string :refer [includes?]]
   [demo.petclinic.web.pagination :as pagination]
   [demo.petclinic.test-utils :refer [system-state system-fixture GET]]))

(use-fixtures :once (system-fixture))

(defonce owner-names
  ["George Franklin" "Betty Davis" "Eduardo Rodriquez" "Harold Davis" "Peter McTavish"
   "Jean Coleman" "Jeff Black" "Maria Escobito" "David Schroeder" "Carlos Estaban"])

(deftest owners-controller-tests []
  (testing "Owners list page 1"
    (let [handler (:handler/ring (system-state))
          params {:lastName ""}
          headers {}
          response (GET handler "/owners" params headers)]
      (is (= 200 (:status response)))
      (is (includes? (:body response) "<a href=\"?page=2\" title=\"Next\" class=\"fa fa-step-forward\"></a>"))
      (dorun
       (for [name (take pagination/PAGESIZE owner-names)]
         (is (includes? (:body response) (format ">%s</a></td>" name)))))))

  (testing "Owners list page 2"
    (let [handler (:handler/ring (system-state))
          params {}
          headers {}
          response (GET handler "/owners?page=2" params headers)]
      (is (= 200 (:status response)))
      (is (includes? (:body response) "<a href=\"?page=1\" title=\"Previous\" class=\"fa fa-step-backward\"></a>"))
      (dorun
       (for [name (drop pagination/PAGESIZE owner-names)]
         (is (includes? (:body response) (format ">%s</a></td>" name))))))))

