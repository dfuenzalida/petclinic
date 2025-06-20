(ns demo.petclinic.utils-test
  (:require
   [demo.petclinic.utils :refer [aggregate-by-key group-properties keywordize-keys]]
   [clojure.test :refer [deftest are is]]))

(deftest group-properties-tests
  (let [vets [{:id 1 :first_name "James" :last_name "Carter"}
              {:id 2 :first_name "Helen" :last_name "Leary"}
              {:id 3 :first_name "Linda" :last_name "Douglas"}]
        specs [{:vet_id 2 :specialty "radiology"}
               {:vet_id 3 :specialty "dentistry"}
               {:vet_id 3 :specialty "surgery"}]
        grouped (group-properties vets specs :id :vet_id :specialty)]
    (is (= 3 (count grouped)))
    (is (= nil (:specialty (first grouped))))
    (is (= ["radiology"] (:specialty (second grouped))))
    (is (= #{"dentistry" "surgery"} (set (:specialty (nth grouped 2)))))
    ;;
    ))

(deftest keywordize-keys-tests
  (is (= (keywordize-keys {:id 123 "first_name" "John" "last_name" "Jones"})
         {:id 123 :first_name "John" :last_name "Jones"})))

(deftest aggregate-by-key-tests
  (let [visits [{:pet_id 7, :visit_date #inst "2013-01-01", :description "rabies shot"}
                {:pet_id 7, :visit_date #inst "2013-01-04", :description "spayed"}
                {:pet_id 8, :visit_date #inst "2013-01-02", :description "rabies shot"}
                {:pet_id 8, :visit_date #inst "2013-01-03", :description "neutered"}]

        pets   [{:id 7, :name "Samantha", :birth_date #inst "2012-09-04", :pet_type "cat", :owner_id 6}
                {:id 8, :name "Max", :birth_date #inst "2012-09-04", :pet_type "cat", :owner_id 6}]

        expected [{:id 7, :name "Samantha", :birth_date #inst "2012-09-04", :pet_type "cat",
                   :owner_id 6, :visits [{:pet_id 7, :visit_date #inst "2013-01-01", :description "rabies shot"}
                                         {:pet_id 7, :visit_date #inst "2013-01-04", :description "spayed"}]}
                  {:id 8, :name "Max", :birth_date #inst "2012-09-04", :pet_type "cat",
                   :owner_id 6, :visits [{:pet_id 8, :visit_date #inst "2013-01-02", :description "rabies shot"}
                                         {:pet_id 8, :visit_date #inst "2013-01-03", :description "neutered"}]}]]

    (is (= expected (aggregate-by-key pets :id visits :pet_id :visits)))))