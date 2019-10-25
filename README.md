# AIP Integration Tools

The AIP Integration tools are a couple of components to ease the automation of application analysis on AIP Console.

It offers 2 main methods of automation:
* [AIP Console Jenkins Plugin](./aip-console-jenkins/README.md)
* [AIP Console Tool CLI](./aip-console-tools-cli/README.md) 

You can find the latest releases on the [Release page](https://github.com/CAST-Extend/com.castsoftware.uc.aip.console.tools/releases). You can also find the latest changes in the [Release Notes](./RELEASE-NOTES.md)  

### Modules

**NB**: If you have downloaded this project from CAST Extend, you can use a ZIP extraction tool (like 7zip) to extract the content of the `.nupkg` file.

This project contains 3 modules :

* *AIP Console Jenkins Plugin* : Module containing the code to our Jenkins plugin to create an application or add a version
* *AIP Console Tools CLI* : Module containing the CLI to create an application or add a version
* *AIP Console Tools Core* : library containing shared code between the Jenkins Plugin and the AIP Integration CLI

### Java Version

AIP Console Tools Core and AIP Console Tools CLI will compile fine on Java versions 8 through 12. The source will be compiled to be used with Java 8. 

AIP Console Jenkins Plugin will only compile for Java 8. This is due to the baseline version we target (2.60.3) which, while not particularly recent, covers a wider range of users.

If you need to compile the plugin with a different Java version, you can pass the `-Djenkins.version=2.XXX` to the maven command.