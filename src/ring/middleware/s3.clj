(ns ring.middleware.s3
  "Serving files from an S3 bucket"
  (:require [ring.util.codec :as codec]
	    [ring.util.response :as response])
  (:import [com.amazonaws.auth BasicAWSCredentials]
	   [com.amazonaws.services.s3 AmazonS3Client]))

(defn- get-buckets [^AmazonS3Client client]
  "Retrieve the set of all buckets accessible by this client"
  (reduce #(conj %1 (.getName %2)) #{} (seq (.listBuckets client))))

(defn- ensure-bucket
  [^AmazonS3Client client ^String bucket-name]
  "Hard-stop if no bucket is specified or the specified bucket doesn't exist."
  (if (or (empty? bucket-name) (nil? bucket-name))
    (throw (Exception. "Please specify a bucket name as the system property \"PARAM1\"")))
  (if-not (contains? (get-buckets client) bucket-name)
    (throw (Exception. (format "Bucket does not exist: %s" bucket-name))))
  bucket-name)

(defn- ensure-object
  [^AmazonS3Client client ^String bucket-name ^String object-key]
  "Return the object if it exists, false otherwise"
  (try
    (.getObject client bucket-name object-key)
    (catch RuntimeException e false)))

(defn- remove-empty-headers
  [resp]
  "Return resp but with any headers with empty? values removed."
  (let [headers (reduce #(if-not (empty? (last %2))
			   (apply assoc (cons %1 %2))
			   %1)
			{}
			(:headers resp))]
    (assoc resp :headers headers)))

(defn wrap-s3
  "Wrap an app such that the specified bucket is checked for an object whose
   key matches the request path with which to respond to the request. If the
   requested object does not exist, the request is passed along to the app.

   Accepts an option map with the following keys:
   - :bucket-name -- name of the bucket to check for objects. Defaults to
     the system property specified by \"PARAM1\"
   - :access-key-id -- the AWS access key id to use for S3 interaction.
     Defaults to the system property specified by \"AWS_ACCESS_KEY_ID\"
   - :secret-key -- the AWS secret key to use for S3 interaction. Defaults
     to the system property specified by \"AWS_SECRET_KEY\""
  [app & [opts]]
  (let [opts (merge {:bucket-name (System/getProperty "PARAM1")
		     :access-key-id (System/getProperty "AWS_ACCESS_KEY_ID")
		     :secret-key (System/getProperty "AWS_SECRET_KEY")}
		    opts)
	creds (BasicAWSCredentials. (:access-key-id opts)
				    (:secret-key opts))
	client (AmazonS3Client. creds)]
    ;; make sure we're dealing with a bucket we own
    (ensure-bucket client (:bucket-name opts))
    (fn [req]
      ;; only respond to GET requests
      (if-not (= :get (:request-method req))
	(app req)
	(let [object-key (.substring (codec/url-decode (:uri req)) 1)]
	  ;; move along if the request is for an un-key-like object
	  (if (or (= \/ (last object-key))
		  (= 0 (count object-key)))
	    (app req)
	    ;; move along if the request is for an object which doesn't exist
	    (if-let [object (ensure-object client (:bucket-name opts) object-key)]
	      (let [stream (.getObjectContent object)
		    meta (.getObjectMetadata object)]
		(remove-empty-headers (-> (response/response stream)
					  (response/content-type (.getContentType meta))
					  (response/header "Cache-Control" (.getCacheControl meta))
					  (response/header "Content-Disposition" (.getContentDisposition meta))
					  (response/header "Content-Encoding" (.getContentEncoding meta))
					  (response/header "Content-Length" (.getContentLength meta))
					  (response/header "Last-Modified" (.getLastModified meta)))))
	      (app req))))))))
