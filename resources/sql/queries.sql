-- :name get-vets :? :*
-- :doc selects up to :limit veterinarians by a given :offset
SELECT * FROM vets LIMIT :limit OFFSET :offset

-- :name get-vets-count :! :raw
-- :doc returns the total number of veterinarians
SELECT COUNT(*) AS total FROM vets
