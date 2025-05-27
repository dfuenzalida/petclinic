-- :name get-vets :? :*
-- :doc selects up to :limit veterinarians by a given :offset
SELECT * FROM vets LIMIT :limit OFFSET :offset

-- Returns a single map with the total number in :total
-- :name get-vets-count :! :1
-- :doc returns the total number of veterinarians
SELECT COUNT(*) AS total FROM vets
