(ns demo.petclinic.web.pagination-test
  (:require
   [demo.petclinic.web.pagination :refer [parse-page with-pagination]]
   [clojure.test :refer [deftest are is]]))

(deftest parse-page-tests
  (are [expected s default] (= expected (parse-page s default))
    42 ""  42
    41 "a" 41
    1  "1"  0
    8  "8"  0
   99 "99"  0
    ;;
    ))

(deftest with-pagination-tests
  ;; Page 1 with 3 total items
  (is (= {:hello "World!" :pagination {:current 1 :total 1 :pages [1]}}
         (with-pagination {:hello "World!"} 1 3)))

  ;; Page 2 with 12 total items
  (is (= {:hello "World!" :pagination {:current 2 :total 3 :pages [1 2 3]}}
         (with-pagination {:hello "World!"} 2 12)))

  (with-redefs [demo.petclinic.web.pagination/PAGESIZE 10]
    ;; Page 2 with 12 total items with PAGESIZE redefined to 10 should be 2 pages only
    (is (= {:hello "World!" :pagination {:current 2 :total 2 :pages [1 2]}}
           (with-pagination {:hello "World!"} 2 12))))
  ;;
  )

