-- :name get-vets :? :*
-- :doc selects the up to :pagesize veterinarians in the :page page
SELECT * FROM vets LIMIT :pagesize OFFSET ((:page - 1) * :pagesize)

-- Returns a single map with the total number in :total
-- :name get-vets-count :! :1
-- :doc returns the total number of veterinarians
SELECT COUNT(*) AS total FROM vets

-- :name specialties-by-vet-ids :? :*
-- :doc given a list of vet ids, get their specialties
SELECT vs.vet_id AS id, s.name AS specialties FROM specialties s, vet_specialties vs WHERE vet_id IN (:v*:vetids) AND vs.specialty_id = s.id

-- :name get-owner :? :1
-- :doc returns a single owner by their id
SELECT * FROM owners WHERE id = :id

-- :name get-owners :? :*
-- :doc selects the up to :pagesize owners in the :page page
SELECT * FROM owners WHERE last_name LIKE :lastNameLike LIMIT :pagesize OFFSET ((:page - 1) * :pagesize)

-- Returns a single map with the total number in :total
-- :name get-owners-count :! :1
-- :doc returns the total number of veterinarians
SELECT COUNT(*) AS total FROM owners WHERE last_name LIKE :lastNameLike

-- :name get-pet :? :1
-- :doc returns a single pet by their id and ownerid
SELECT * FROM pets WHERE id = :id and owner_id = :ownerid

-- :name get-types :? :*
-- :doc returns all pet types
SELECT * FROM types ORDER BY name ASC

-- :name get-pets-by-owner-ids :? :*
-- :doc Return all pets for a list of owners
SELECT p.id AS id, p.name AS name, p.birth_date AS birth_date, t.name AS pet_type, p.owner_id AS owner_id FROM pets p, types t WHERE owner_id IN (:v*:ownerids) AND p.type_id = t.id ORDER BY p.name ASC

-- :name update-owner! :! :n
-- :doc update an owner
UPDATE owners SET first_name = :first_name, last_name = :last_name, address = :address, city = :city, telephone = :telephone WHERE id = :id
