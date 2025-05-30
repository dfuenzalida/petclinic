-- :name get-vets :? :*
-- :doc selects the up to :pagesize veterinarians in the :page page
SELECT * FROM vets LIMIT :pagesize OFFSET ((:page - 1) * :pagesize)

-- Returns a single map with the total number in :total
-- :name get-vets-count :! :1
-- :doc returns the total number of veterinarians
SELECT COUNT(*) AS total FROM vets

-- :name specialties-by-vet-ids :? :*
-- :doc given a list of vet ids, get their specialties
SELECT vs.vet_id AS vet_id, s.name AS specialty FROM specialties s, vet_specialties vs WHERE vet_id IN (:v*:vetids) AND vs.specialty_id = s.id

-- :name get-owners :? :*
-- :doc selects the up to :pagesize owners in the :page page
SELECT * FROM owners LIMIT :pagesize OFFSET ((:page - 1) * :pagesize)

-- Returns a single map with the total number in :total
-- :name get-owners-count :! :1
-- :doc returns the total number of veterinarians
SELECT COUNT(*) AS total FROM owners
