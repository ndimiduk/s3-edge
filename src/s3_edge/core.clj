(ns s3-edge.core
  "Simple webapp for serving files from S3"
  (:use [ring.util.response]
	[ring.middleware.s3]))

(defn- empty-response
  "Return an empty 200 unless otherwise requested."
  [req]
  (response ""))

(def handler
     (-> empty-response
	 wrap-s3))
