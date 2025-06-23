(ns demo.petclinic.web.clinic-service-test
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [demo.petclinic.test-utils :refer [system-state system-fixture]]))

(use-fixtures :once (system-fixture))

(deftest should-find-owner-by-last-name
  (let [query-fn (:db.sql/query-fn (system-state))]
    (testing "Find owners with last name 'Davis'"
      (let [owners (query-fn :get-owners {:lastNameLike "Davis" :pagesize 100 :page 1})]
        (is (= 2 (count owners)))))
    (testing "Find owners with invalid last name"
      (let [owners (query-fn :get-owners {:lastNameLike "Daviss" :pagesize 100 :page 1})]
        (is (empty? owners))))))

(deftest should-find-single-owner-with-pet
  (let [query-fn (:db.sql/query-fn (system-state))
        owner   (query-fn :get-owner {:id 1})
        pets    (query-fn :get-pets-by-owner-ids {:ownerids [1]})]
    (is (some? owner))
    (is (= "Franklin" (:last_name owner)))
    (is (= 1 (count pets)))
    (is (= "cat" (:pet_type (first pets))))))

(deftest should-insert-owner
  (let [query-fn (:db.sql/query-fn (system-state))
        found    (:total (query-fn :get-owners-count {:lastNameLike "%"}))
        owner    {:first_name "Sam"
                  :last_name "Schultz"
                  :address "4, Evans Street"
                  :city "Wollongong"
                  :telephone "4444444444"}
        new-id   (-> (query-fn :create-owner! owner) :id)
        found-after (:total (query-fn :get-owners-count {:lastNameLike "%"}))]
    (is (pos? new-id))
    (is (= (inc found) found-after))))

(deftest should-update-owner
  (let [query-fn (:db.sql/query-fn (system-state))
        owner    (query-fn :get-owner {:id 1})]
    (is (some? owner))
    (is (= "Franklin" (:last_name owner)))

    ;; Update the owner's last name
    (let [updated-owner (update owner :last_name #(str % "x"))]
      (query-fn :update-owner! updated-owner)
      (let [updated-owner-from-db (query-fn :get-owner {:id 1})]
        (is (= (str (:last_name owner) "x") (:last_name updated-owner-from-db)))))))

(deftest should-find-all-pet-types
  (let [query-fn (:db.sql/query-fn (system-state))
        pet-types (query-fn :get-types {})
        pet-types-by-id (group-by :id pet-types)]
    (is (= (first (get pet-types-by-id 1)) {:id 1 :name "cat"}))
    (is (= (first (get pet-types-by-id 4)) {:id 4 :name "snake"}))))
