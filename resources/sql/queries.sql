-- :name get-vets :? :*
-- :doc selects the up to :pagesize veterinarians in the :page page
SELECT * FROM vets LIMIT :pagesize OFFSET ((:page - 1) * :pagesize)

-- Returns a single map with the total number in :total
-- :name get-vets-count :! :1
-- :doc returns the total number of veterinarians
SELECT COUNT(*) AS total FROM vets
