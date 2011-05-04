(defproject s3-edge "1.0.0"
  :description "Serve files from s3 as if they were your own."
  :dependencies [[org.clojure/clojure "1.2.1"]
		 [ring/ring-core "0.3.7"] ; match ring version with lein-ring
		 [com.amazonaws/aws-java-sdk "1.1.9"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
		     [lein-ring "0.4.0"]]
  :ring {:handler s3-edge.core/handler})
