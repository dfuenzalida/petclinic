-- :name get-vets :? :*
-- :doc selects up to :limit veterinarians by a given :offset
SELECT * FROM vets LIMIT :limit OFFSET :offset