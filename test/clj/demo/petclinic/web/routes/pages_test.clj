(ns demo.petclinic.web.routes.pages-test
  (:require
   [clojure.tools.logging :as log]
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure.string :refer [includes?]]
   [demo.petclinic.web.routes.pages :refer []]
   [demo.petclinic.test-utils :refer [system-state system-fixture GET]]))

(use-fixtures :once (system-fixture))

(deftest routes-pages-test []
  (testing "Welcome page"
    (let [handler (:handler/ring (system-state))
          params {}
          headers {}
          response (GET handler "/" params headers)]
      (is (= 200 (:status response)))
      (is (includes? (:body response) "<h2>Welcome</h2>"))))

  (testing "Welcome page translation with header"
    (let [handler (:handler/ring (system-state))
          params {}
          headers {:accept-language "es"}
          response (GET handler "/" params headers)]
      (is (= 200 (:status response)))
      (is (includes? (:body response) "<h2>Bienvenido</h2>"))))

  (testing "Welcome page translation with param in URL"
    (let [handler (:handler/ring (system-state))
          params {:lang "es"}             ;; pass "lang=es" in URL params
          headers {:accept-language "fr"} ;; header should be overriden by param
          response (GET handler "/" params headers)]
      (is (= 200 (:status response)))
      (is (includes? (:body response) "<h2>Bienvenido</h2>"))))

  (testing "Error page"
    (with-redefs [log/log* (constantly nil)] ;; suppress logging for this test
      (let [handler (:handler/ring (system-state))
            params {}
            headers {}
            response (GET handler "/oups" params headers)]
        (is (= 500 (:status response)))
        (is (includes? (:body response) "<h2>Something happened...</h2>"))
        (is (includes? (:body response) "Expected: controller used to showcase what happens when an exception is thrown")))))
  ;;
  )

