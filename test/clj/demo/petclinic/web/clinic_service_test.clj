(ns demo.petclinic.web.clinic-service-test
  (:import
   [java.time LocalDate]
   [java.time.format DateTimeFormatter])
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [demo.petclinic.test-utils :refer [system-state system-fixture]]
   [demo.petclinic.web.controllers.vets :refer [vets-with-specialties]]
   [demo.petclinic.web.controllers.owners :refer [pets-with-visits]]))

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
    (let [updated-owner (update owner :last_name str "x")]
      (query-fn :update-owner! updated-owner)
      (let [updated-owner-from-db (query-fn :get-owner {:id 1})]
        (is (= (str (:last_name owner) "x") (:last_name updated-owner-from-db)))))

    ;; Restore the original owner so can run the tests multiple times in dev
    (query-fn :update-owner! owner)))

(deftest should-find-all-pet-types
  (let [query-fn (:db.sql/query-fn (system-state))
        pet-types (query-fn :get-types {})
        pet-types-by-id (group-by :id pet-types)]
    (is (= (first (get pet-types-by-id 1)) {:id 1 :name "cat"}))
    (is (= (first (get pet-types-by-id 4)) {:id 4 :name "snake"}))))

(deftest should-insert-pet-into-database-and-generate-id
  (let [query-fn (:db.sql/query-fn (system-state))
        owner    (query-fn :get-owner {:id 6})
        pets     (query-fn :get-pets-by-owner-ids {:ownerids [(:id owner)]})
        pet      {:name "bowser" :type_id 2  :birth_date (LocalDate/now) :ownerid (:id owner)}]
    (query-fn :create-pet! pet)
    (let [pets-after (query-fn :get-pets-by-owner-ids {:ownerids [(:id owner)]})
          bowser     (first (filter #(= "bowser" (:name %)) pets-after))]
      (is (= (count pets-after) (inc (count pets))))
      (is (some? (:id bowser))))))

(deftest should-update-pet-name
  (let [query-fn (:db.sql/query-fn (system-state))
        pets     (query-fn :get-pets-by-owner-ids {:ownerids [6]})
        pet      (first (filter #(= (:id %) 7) pets))
        _        (->> (update pet :name str "X")
                      (merge {:ownerid 6})
                      (query-fn :update-pet!))
        pets     (query-fn :get-pets-by-owner-ids {:ownerids [6]})
        updated-pet (first (filter #(= (:id %) 7) pets))]

    (is (= (str (:name pet) "X") (:name updated-pet)))))

(deftest should-find-vets
  (let [query-fn (:db.sql/query-fn (system-state))
        total (:total (query-fn :get-vets-count {}))
        vets  (vets-with-specialties query-fn total 1)
        vet3  (first (filter #(= 3 (:id %)) vets))]
    (is (= (:last_name vet3) "Douglas"))
    (is (= 2 (count (:specialties vet3))))
    (is (= ["dentistry" "surgery"] (map :name (:specialties vet3))))))

(deftest should-add-new-visit-for-pet
  (let [query-fn (:db.sql/query-fn (system-state))
        ownerid 6
        pets    (pets-with-visits query-fn ownerid)
        pet7    (first (filter #(= (:id %) 7) pets))
        old-count (count (:visits pet7))
        new-visit {:description "test"
                   :visit_date (.format (LocalDate/now) DateTimeFormatter/ISO_LOCAL_DATE)}
        _       (query-fn :create-visit! (assoc new-visit :pet_id (:id pet7)))
        pet7    (->> (pets-with-visits query-fn ownerid)
                     (filter #(= (:id %) 7))
                     first)
        new-count (->> pet7 :visits count)]

    (is (= (inc old-count) new-count))
    ;; Every visit should have an :id
    (is (every? #(some? (:id %)) (:visits pet7)))))

(deftest should-find-visits-by-pet-id
  (let [query-fn (:db.sql/query-fn (system-state))
        visits   (query-fn :get-visits-by-pet-ids {:petids [7]})]
    (is (= 2 (count visits)))
    (is (some? (-> visits first :visit_date)))))
