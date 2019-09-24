# AIP Integration Tools

The AIP Integration tools are a couple of examples to showcase the use of the AIP Console REST API to automate the deployment and analysis of your applications on AIP Console.

It offers 2 main examples:
* A Jenkins Plugin
* A CLI tool

Please check each folder for more details on each elements.

### Modules

This project contains 3 modules, Please use unzip tool to extract the .nupkg file if downloaded from CAST-EXTEND to get the components :

* AIP Jenkins Plugin : Module containing the code to our Jenkins plugin to create an application or add a version
* AIP Integration CLI : Module containing the CLI to create an application or add a version
* AIP Integration Core : library containing shared code between the Jenkins Plugin and the AIP Integration CLI

### Java Version

AIP Integration Core and AIP Integration CLI will compile fine on JDK versions 8 through 12. The source will be compiled to be used with Java 8. 

AIP Jenkins Plugin will only compile for Java 8. This is due to the baseline version we target (2.60.3) which, while not particularly recent, covers a wider range of users.

If you need to compile the plugin with a different Java version, you can pass the `-Djenkins.version=2.XXX` to the maven command.

## Release Notes :

#### 1.0.4 : 

⚠ Default request timeout is now 90s. This is to avoid errors when uploading very large files (over 1GB). 

Fix :
* An error with the message `UploadException: No more content to read, but file not complete (the file might be modified by another program?).` occured for evry large file. This was due to an integer overflow that has been fixed.
* Updated library `com.fasterxml.jackson.core` libraries to version 2.9.10, to fix a vulnerability (see https://nvd.nist.gov/vuln/detail/CVE-2019-14540 and https://nvd.nist.gov/vuln/detail/CVE-2019-16335)

#### 1.0.3:

Aligning version with CAST Extend version.
 
Additions : Connection Timeout, Rescan, Application Name in Add Version, Automatically create a new application if the give name doesn't exist, Ignore Failures

CLI:
* With the `AddVersion` sub command, you can specify the application name (with `-n`) instead of having to provide an application guid
* With the `AddVersion` sub command, you can specify `--auto-create` to create a new application on the target AIP Console instance if it doesn't exists
* You can specify a timeout (in seconds) for each sub commands, in case your AIP Console instance takes some time to answer.

Jenkins Plugin:
* In the Add Version build step, you have to specify an application name, instead of the application GUID.
* In the Add Version build step, you can check the "auto create" checkbox to have AIP Console plugin automatically create the application in AIP Console instance.
* In the Add Version build step, you can check "Rescan" to automate the delivery and analysis of your application. This will create a new version, based on the previously delivered one, and keep the same configuration.
* In the Add Version build step, you can check "Ignore Failures" to avoid having the plugin fail the job. Instead, the plugin will mark its execution has being unstable if any issue occurs.

*Note : Application GUID will still appear in the build step, but it is a read only value, meant for verification (and backwards compatibility)*

#### 0.0.1 :

Initial release 🎉 

CLI
* Create an application using the `CreateApplication` subcommand
* Deliver a new version using the `AddVersion` subcommand

Jenkins Plugin :
* Configure access to your AIP Console instance in the Global Configuration of Jenkins
* Create a new application within a Jenkins Job, either Freestyle or Pipeline
* Add a new version to an existing application on AIP Console in a Jenkins Job 