# AIP Console Tools Core

This project contains code shared between the CLI and the AIP Console Jenkins Plugin.

### Content

In the project, you'll find the following packages:

* `dto`: Java representation of JSON object sent and retrieved from AIP Console API
* `services`: The interfaces and implementation for calls to the API.
    * Impl classes : These are the actual implementation for the upload and jobs services that are shared between the Jenkins Plugin and the CLI.
* `utils`: Some utility classes

### How to build

Provided you have maven installed, and Java 8 or above, you can simply run the following :

```bash
mvn install
```

This will build and install the library inside your local maven repository.