(ns user
  (:require [prone.demo :as demo]
            [ring.server.standalone :as standalone]))

(comment

  (standalone/serve demo/app {:port 3001})

  )
