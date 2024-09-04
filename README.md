# CAST Imaging Automation Tools

The CAST Imaging Automation tools are a couple of components to ease the automation of application analysis on CAST Imaging.

It offers 2 main methods of automation:
* [CAST Imaging Jenkins Plugin](./aip-console-jenkins/README.md) with an example [Jenkins files](./aip-console-jenkins/examples/dynamic-pipeline-example/README.md) to automate retrieving and analysing multiple applications.
* [CAST Imaging Tools CLI](./aip-console-tools-cli/README.md)

The latest releases are available on the [Release page](https://github.com/CAST-Extend/com.castsoftware.aip.console.tools/releases).

The latest changes are described in the [Release Notes](./RELEASE-NOTES.md)

### Modules

**NB**: If you have downloaded this project from CAST Extend, you can use a ZIP extraction tool (like 7zip) to extract the content of the `.nupkg` file.

This project contains 3 modules :

* *CAST Imaging Jenkins Plugin* : Module containing the code to our Jenkins plugin to do a scan of the application or to do a deep-analysis
* *CAST Imaging Tools CLI* : Module containing the CLI to do a scan of the application or to do a deep-analysis
* *CAST Imaging Tools Core* : library containing shared code between the Jenkins Plugin and the CLI

### Java Version

CAST Imaging Tools Core and CAST Imaging Tools CLI will compile fine on Java versions 8 through 17. The source will be compiled to be used with Java 8.

If you need to compile the plugin with a different Java version, you can pass the `-Djenkins.version=2.XXX` to the maven command.
