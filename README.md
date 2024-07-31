# AIP Automation Tools

This documentation is only up to date for AIP Console tools 1.X. For AIP Consoel Tools 2.X please check here :
[📄Link to AIP Console Tools 2.X version and documentation](https://github.com/CAST-Extend/com.castsoftware.aip.console.tools/tree/v2)

The AIP Automation tools are a couple of components to ease the automation of application analysis on AIP Console.

It offers 2 main methods of automation:
* [AIP Console Jenkins Plugin](./aip-console-jenkins/README.md) with an example [Jenkinsfile](./aip-console-jenkins/examples/dynamic-pipeline-example/README.md) to automate retrieving and analysing multiple applications.
* [AIP Console Tool CLI](./aip-console-tools-cli/README.md)

The latest releases are available on the [Release page](https://github.com/CAST-Extend/com.castsoftware.aip.console.tools/releases).

The latest changes are described in the [Release Notes](./RELEASE-NOTES.md)

### Modules

**NB**: If you have downloaded this project from CAST Extend, you can use a ZIP extraction tool (like 7zip) to extract the content of the `.nupkg` file.

This project contains 3 modules :

* *AIP Console Jenkins Plugin* : Module containing the code to our Jenkins plugin to create an application or add a version
* *AIP Console Tools CLI* : Module containing the CLI to create an application or add a version
* *AIP Console Tools Core* : library containing shared code between the Jenkins Plugin and the AIP Automation CLI

### Java Version

AIP Console Tools Core and AIP Console Tools CLI will compile fine on Java versions 8 through 12. The source will be compiled to be used with Java 8.

AIP Console Jenkins Plugin will only compile for Java 8. This is due to the baseline version we target (2.60.3) which, while not particularly recent, covers a wider range of users.

If you need to compile the plugin with a different Java version, you can pass the `-Djenkins.version=2.XXX` to the maven command.
