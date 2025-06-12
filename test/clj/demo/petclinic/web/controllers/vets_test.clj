(ns demo.petclinic.web.controllers.vets-test
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure.string :refer [includes?]]
   [demo.petclinic.web.pagination :as pagination]
   [demo.petclinic.test-utils :refer [system-state system-fixture GET]]))

(def vet-names
  ["James Carter" "Helen Leary" "Linda Douglas" "Rafael Ortega" "Henry Stevens" "Sharon Jenkins"])

(use-fixtures :once (system-fixture))

(deftest vets-controller-test []
  (testing "Vets list page 1"
    (let [handler (:handler/ring (system-state))
          params {}
          headers {}
          response (GET handler "/vets.html" params headers)]
      (is (= 200 (:status response)))
      (is (includes? (:body response) "<a href=\"?page=2\" title=\"Next\" class=\"fa fa-step-forward\"></a>"))
      (dorun
       (for [name (take pagination/PAGESIZE vet-names)]
         (is (includes? (:body response) (format "<td>%s</td>" name)))))))

  (testing "Vets list page 2"
    (let [handler (:handler/ring (system-state))
          params {}
          headers {}
          response (GET handler "/vets.html?page=2" params headers)]
      (is (= 200 (:status response)))
      (is (includes? (:body response) "<a href=\"?page=1\" title=\"Previous\" class=\"fa fa-step-backward\"></a>"))
      (dorun
       (for [name (drop pagination/PAGESIZE vet-names)]
         (is (includes? (:body response) (format "<td>%s</td>" name))))))))
