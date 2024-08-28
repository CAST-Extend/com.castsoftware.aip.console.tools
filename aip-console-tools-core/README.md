# CAST Imaging Tools Core

This project contains code shared between the CLI and the CAST Imaging Jenkins Plugin.

### Content

In the project, you'll find the following packages:

* `dto`: Java representation of JSON object sent and retrieved from CAST Imaging API
* `services`: The interfaces and implementation for calls to the API.
  * Impl classes : These are the actual implementation for the upload and jobs services that are shared between the
    Jenkins Plugin and the CLI.
* `utils`: Some utility classes

You will need to update the target CAST Console Server version before building the project.
To do so set the required version in this method:
`SemVerUtils.getMinCompatibleVersion()`.

Any running command or plugin will first check the version compatibility be fore processed.

* Jenkins plugin issues a failure when condition doesn't meet the requirement.
* CLI returns error code 28 indicating bad server version.

### How to build

Provided you have maven installed, and Java 8 or above, you can simply run the following :

```bash
mvn install
```

This will build and install the library inside your local maven repository.