# AIP Integration Tools

The AIP Integration tools are a couple of examples to showcase the use of the AIP Console REST API to automate the deployment and analysis of your applications on AIP Console.

It offers 2 main examples:
* A Jenkins Plugin
* A CLI tool

Please check each folder for more details on each elements.

### Modules

This project contains 3 modules :

* AIP Jenkins Plugin : Module containing the code to our Jenkins plugin to create an application or add a version
* AIP Integration CLI : Module containing the CLI to create an application or add a version
* AIP Integration Core : library containing shared code between the Jenkins Plugin and the AIP Integration CLI

### Java Version

AIP Integration Core and AIP Integration CLI will compile fine on JDK versions 8 through 12. The source will be compiled to be used with Java 8. 

AIP Jenkins Plugin will only compile for Java 8. THis is due to the baseline version we target (2.60.3) which, while not particularly recent, covers a wider range of users.

If you need to compile the plugin with a different Java version, you can pass the `-Djenkins.version=2.XXX` to the maven command.
