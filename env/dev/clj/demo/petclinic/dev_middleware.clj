(ns demo.petclinic.dev-middleware)

(defn wrap-dev [handler _opts]
  (-> handler
      ))
