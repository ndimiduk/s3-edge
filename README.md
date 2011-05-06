# s3-edge

Serving files to the interwebs via AWS S3 and/or CloudFront is super
convenient for most cases. You can even hide them behind a CNAME so it
looks like you're your own capable web host. However, one nagging
feature lack is the ability to upload a root certificate and serve
said files via HTTPS. Without this, your users' browsers will display
an annoying SSL Certificate error when using that super friendly
CNAMEed URL.

Insert s3-edge.

This is a simple app which sits between S3 and your users. Push this
war to a service like Elastic Beanstalk, a service which does allow
you to specify your own certificate, and voila! You now have CNAME
friendly HTTPS with the simplicity of S3 file management. Isn't that
easy?

## Usage

Use s3-edge in one of two ways: stand-alone or as part of your larger
clojure/ring web application.

### Stand-alone

Pull the source and build a war. s3-edge uses
[leiningen](https://github.com/technomancy/leiningen) so be sure you
have that installed first.

    $ git clone https://github.com/ndimiduk/s3-edge.git
    $ cd s3-edge
    $ lein deps, ring uberwar

> Note: building the war appears to compile and execute the code! This
> causes an exception to be thrown because you likely don't have the
> environment properly configured in your shell. This is a known
> issue; please don't be alarmed.

You can now upload the newly created
<code>s3-edge-1.0.0-standalone.war</code> to EBS or wherever else
you'd find it useful.

s3-edge expects to be deployed to Elastic Beanstalk and so looks for
configuration in a specific place. It looks for AWS access credentials
in the System properties <code>AWS_SECRET_KEY_ID</code> and
<code>AWS_SECRET_KEY</code>. It looks for the name of the bucket from
which to serve content in the most intuitively named property
<code>PARAM1</code>. These are easily set in your EBS environment
configuration. For everyone else, you can likely launch your servlet
container with the <code>JAVA_OPTS</code> environment variable
defining these values,
ie. <code>JAVA_OPTS="-DPARAM1=my-bucket-name"</code>.

### Ring Middleware

s3-edge defines the <code>ring.middleware.s3</code> namespace which
exports the <code>wrap-s3</code> function. Use it just like you would
<code>wrap-file</code>. To use the middleware, include it as a
dependency in your leiningen project:

    :dependencies [[s3-edge "1.0.0"]]

It accepts an option map which respects the following keys:

- *:bucket-name* name of the bucket to check for objects. Defaults to
   the system property specified by <code>PARAM1</code>
- *:access-key-id* the AWS access key id to use for S3
   interaction. Defaults to the system property specified by
   <code>AWS_ACCESS_KEY_ID</code>
- *:secret-key* the AWS secret key to use for S3 interaction. Defaults
   to the system property specified by <code>AWS_SECRET_KEY</code>

## Further Notes

- s3-edge does its best to pass along appropriate headers. Basically,
  if it's available in the s3 client library API it's passed through.
- File streams opened from s3 are passed directly into the
  response. This may do things like consume lots of memory if you're
  not kicking those responses back right away.

## License

Copyright (C) 2011 Nick Dimiduk

Distributed under the Eclipse Public License, the same as Clojure.
