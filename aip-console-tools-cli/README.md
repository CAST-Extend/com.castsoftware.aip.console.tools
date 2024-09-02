## CAST Imaging Tools CLI

### Objectives

The purpose of the CAST Imaging Tools CLI is to provide methods to start Application analysis without needing to interact
directly with the CAST Imaging UI, by leveraging its REST API.

The CLI can onboard new applications, scan the application and run application deep-analysis. 

### Requirements

Before using the CLI, you will need a JRE or JDK, version 8 or above. You can use either a JVM provided by Oracle,
AdoptOpenJDK or other validated JVMs (like Amazon Corretto, Azul Zulu, Eclipse OpenJ9, etc.)

You will also need the following :

* An installation of CAST Imaging that is accessible and configured.
* An API Token for the user that will run the CLI (
  check [here for details on obtaining a token](https://doc.castsoftware.com/display/AIPCONSOLE/AIP+Console+-+User+Profile+options))

[//]: # (* For the Console Standalone server you must provide following two parameters)

[//]: # (  <br/> `--user`="login name here" `--apikey`="password here")
* Prepare your source code in one of the two following ways :
  * As a zip or tar.gz archive that will be uploaded to CAST Imaging
  * As a relative path, pointing to content in the Source Folder location **(SFL)** defined in CAST Imaging like
    below :

![source folder location](doc/images/source_folder_location_config.png)

### Quick Start
For any of the command line listed below, you can use *--verbose* argument to decide whether to display API log information or not.
They can be executed un Linux and Windows environment as well.

#### Onboard Application

To **Onboard Application** using a **file full path**, the process will upload sources on the CAST Imaging server. The file should be accessible from the machine where the batch is executed.

The *Onboard Application* feature is using following strategies in a separated commands:

- *Fast-Scan*: for the sources' delivery contents without running the analysis. This strategy can be used as much as
  required by providing the sources file.
- *Deep-Analyze: to trigger the analysis after the Fast-Scan has been performed. This also manage to upload data to CAST
  imaging depending on the available configuration.
- *Publish-Imaging* to publish existing application data to CAST-Imaging

To perform that action you can inspire from the following command (see advanced usage for more details):

**Fast-Scan command example**
```bash
java -jar .\aip-console-tools-cli.jar Fast-Scan -s="Console URL" --apikey="valid.key" -n "my app" --domain-name="Your Domain" -f "C:\folder\some-location\sources-file.zip" --verbose=false --exclude-patterns="tmp/, temp/, *test, tests, target/, .svn/, .git/, _Macosx/, test/"
```

**Deep-Analyze command example**
```bash
java -jar .\aip-console-tools-cli.jar Deep-Analyze -s="Console URL" --apikey="valid.key" -n "my app" --module-option="ONE_PER_AU" --snapshot-name="desired name" --process-imaging=false --verbose=false
```

To perform all-in-one operation **Onboard-Application**

- all parameters required for *Fast-Scan* command should be provided
- all parameters required for *Deep-Analyze* command should be provided
- all parameters required for *Publish-Imaging* command should be provided
  To trigger this All-in-one command, you just have to customize the CLI given below

**Onboard-Application CLI example**
```bash
java -jar .\aip-console-tools-cli.jar Onboard-Application --apikey="valid key" -s "Console URL" -n "my app" -f="zip file full path" --domain-name="Some Domain name" --snapshot-name="SNAP name" --sleep-duration 5 --module-option="one_per_techno""
```

If you wish to redirect the above command output to a file that you can read back afterword, proceed as follows

```bash
java -jar .\aip-console-tools-cli.jar Onboard-Application --apikey="valid key" -s "Console URL" -n "my app" -f="zip file full path" --domain-name="Domain name" --snapshot-name="Snapshot name" --sleep-duration 5 --module-option="one_per_techno" > my_outpout_file_full_path.txt"
```

### Advanced Usage

When running the CLI, you must specify a command to be run. The list of commands is :

* `Fast-Scan` to perform a *fast-scan* on the sources contents and optionally do a Deep-Analysis (run the analysis).
* `Deep-Analyze` to perform a *Deep-Analysis* on an existing application. It does run the analysis and publish to the dashboard and Imaging depending on the operating settings
* `Onboard-Application` to perform an *Onboard-Application* on an existing application. It does run the fast-scan, deep-analysis and publish to the dashboard and Imaging depending on the operating settings.

Each command have a `--help` parameter, providing a list of all parameters available.

Below, is a detail of all available parameters for each command, and how it affects the CLI.

#### Fast-Scan

Creates an application or uses an existing application to manage source code in CAST Imaging.

This command is used to perform the *first scan* or to *refresh* the sources contents before you optionally perform a *Deep Analysis* (run the analysis).

The available options are :

* `--server-url` or `-s` (optional): Specify the URL to your CAST Imaging server. *default* : localhost:8081
* `--apikey` or `--apikey:env` (**either is required**) : the API Key to log in to CAST Imaging**OR** the environment
* `--app-name` or `-n` (**required**): The application name.
* `--file` or `-f`: (**required**) Represents either the local zip or tar.gz file full path to the sources or a relative path using the Source Folder Location configured.
* `--node-name`  (**optional**): The name of the node on which the application will be created
* `--domain-name`  (**optional**): A domain is a group of applications. You may use domain to sort/filter applications. Will be created if it doesn't exist. No domain will be assigned if left empty
* `--exclude-patterns` or `-exclude` (**optional**): File patterns(glob pattern) to exclude in the delivery, separated
  with comma
* `--exclusion-rules`  (**optional**): Project's exclusion rules, separated with comma.
* `--sleep-duration`  (**optional**):Amount of seconds used to fetch the ongoing job status (defaulted to **1s**).

```bash
java -jar .\aip-console-tools-cli.jar Fast-Scan --apikey="valid.key" -n "my app" --domain-name="Your Domain" -f "C:\folder\some-location\sources-file.zip" --verbose=false --sleep-duration=2 --exclude-patterns="tmp/, temp/, *test, tests, target/, .svn/, .git/, _Macosx/, test/"
```

### Deep Analyze

* `--server-url` or `-s` (optional): Specify the URL to your CAST Imaging server. *default* : localhost:8081
* `--apikey` or `--apikey:env` (**either is required**) : the API Key to log in to CAST Imaging**OR** the environment
* `--app-name` or `-n` (**required**): The application name.
* `--snapshot-name` or `-S` (optional): Used to specify the snapshot name other than the default one provided internally.
* `--module-option` (optional) Generates a user defined module option for either technology module or analysis unit module.Possible value is one of: full_content, one_per_au, one_per_techno.
* `--process-imaging` (optional) Default: true, if true it will trigger Generate Views step and upload the application to CAST Imaging Viewer. If used without value associated then assumes true.
* `--publish-engineering` (optional) Default: true, if true it will upload the application results and publish them to the Dashboards. If used without value associated then assumes true.
* `--sleep-duration`  (**optional**):Amount of seconds used to fetch the ongoing job status (defaulted to **15s**).

```bash
java -jar .\aip-console-tools-cli.jar Deep-Analyze --apikey="valid.key" -n "my app"  --verbose=false
```

### Onboard-Application (All-in-one)

The available options are :

* `--server-url` or `-s` (optional): Specify the URL to your CAST Imaging server. *default* : localhost:8081
* `--apikey` or `--apikey:env` (**either is required**) : the API Key to log in to CAST Imaging**OR** the environment
* `--app-name` or `-n` (**required**): The application name.
* `--file-path` or `-f`: **required** only when performing the FIRST_SCAN. Represents either the local zip or tar.gz file full path to the sources or a relative path using the Source Folder Location configured.
* `--node-name`  (**optional**): The name of the node on which the application will be created
* `--domain-name`  (**optional**): A domain is a group of applications. You may use domain to sort/filter applications. Will be created if it doesn't exist. No domain will be assigned if left empty
* `--exclude-patterns` or `-exclude` (**optional**): File patterns(glob pattern) to exclude in the delivery, separated with comma.
* `--exclusion-rules`  (**optional**): Project's exclusion rules, separated with comma.
* `--sleep-duration`  (**optional**):Amount of seconds used to fetch the ongoing job status (defaulted to **1s**).
* `--snapshot-name` or `-S` (optional): Used to specify the snapshot name.
* `--module-option` (optional) Generates a user defined module option for either technology module or analysis unit module. Possible value is one of: full_content, one_per_au, one_per_techno.
* `--process-imaging` (optional) Default: true, if true it will trigger Generate Views step and upload the application to CAST Imaging Viewer. If used without value associated then assumes true.
* `--publish-engineering` (optional) Default: true, if true it will upload the application results and publish them to the Dashboards. If used without value associated then assumes true.

```bash
java -jar .\aip-console-tools-cli.jar Onboard-Application --apikey="valid key" -s "http://lfolap1.corp.castsoftware.com:8081" -n "APP name" -f="zip file full path" --domain-name="Some Domain name" --snapshot-name="SNAP name" --sleep-duration 5 --module-option="one_per_techno"
```
## Exclusion Rules
The value of the '--exclusion-rules' parameter it's an array of mnemonics separated with comma (see details bellow)

**Exclusion rules details**

| Mnemonic                                         | Label or Description                                                                       |
|--------------------------------------------------|--------------------------------------------------------------------------------------------|
| EXCLUDE_EMPTY_PROJECTS                           | "Exclude all empty projects"                                                               |
| PREFER_FULL_DOT_NET_TO_BASIC_DOT_NET_WEB         | "Exclude ASP .NET web projects when a Visual C#/basic .NET project also exists"            |
| PREFER_DOT_NET_WEB_TO_ASP                        | "Exclude ASP projects when a .NET web project also exists"                                 |
| PREFER_FULL_JAVA_PROJECTS_TO_BASIC_JSP           | "Exclude basic JSP projects when a full JEE project also exists for the same web.xml file" |
| PREFER_MAVEN_TO_ECLIPSE                          | "Exclude Eclipse Java projects when a Maven project also exists"                           |
| PREFER_ECLIPSE_TO_MAVEN                          | "Exclude Maven Java projects when an Eclipse project also exists"                          |
| EXCLUDE_EMBEDDED_ECLIPSE_PROJECTS                | "Exclude Eclipse project located inside the output folder of another Eclipse project"      |
| EXCLUDE_ECLIPSE_PROJECT_WITH_DUPLICATED_NAME     | "Exclude Eclipse project sharing the name of another Eclipse project"                      |
| EXCLUDE_DUPLICATE_DOT_NET_PROJECT_IN_SAME_FOLDER | "Exclude Duplicate Dot Net project located inside the exactly same source folder."         |
| EXCLUDE_TEST_CODE                                | "Exclude Test Code"                                                                        |

**Exclusion rules example**
Passing the --exclusion-rules parameter as an array of mnemonics like in this example
(no blank space allowed between)

--exclusion-rules="EXCLUDE_EMPTY_PROJECTS,PREFER_FULL_DOT_NET_TO_BASIC_DOT_NET_WEB,PREFER_DOT_NET_WEB_TO_ASP,PREFER_FULL_JAVA_PROJECTS_TO_BASIC_JSP,PREFER_MAVEN_TO_ECLIPSE,EXCLUDE_EMBEDDED_ECLIPSE_PROJECTS,EXCLUDE_ECLIPSE_PROJECT_WITH_DUPLICATED_NAME,EXCLUDE_DUPLICATE_DOT_NET_PROJECT_IN_SAME_FOLDER,EXCLUDE_TEST_CODE"

## Execution results

When CAST Imaging finishes execution, it will return a specific return code, based on the execution.

Here is a detailed list of all error codes that can be returned by the CLI :

* 0 : No errors, processing was completed correctly. This is also the return code for`--help` and `--version`
  parameters.
* 1 : API key missing. No API key was provided either in the prompt or in the environment variable.
* 2 : Login Error. Unable to login to CAST Imaging with the given API key. Please check that you provide the proper
  value.
* 3 : Upload Error. An error occurred during upload to CAST Imaging. Check the standard output to see more details.
* 4 : Add Version Job Error. Creation of the Add Version job failed, or CAST Imaging CLI is unable to get the status of the running job. Please see the standard output for more details regarding this error.
* 5 : Job terminated. The Add Version job did not finish in an expected state. Check the standard output or CAST Imaging for more details about the state of the job.
* 6 : Application name or GUID missing. The AddVersion job cannot run due to a missing application name or missing application guid.
* 7 : Application Not Found. The given Application Name or GUID could not be found.
* 8 : Source Folder Not Found. THe given source folder could not be found on the AIP Node where the application version is delivered
* 9 : No Version. Application has no version and the provided command cannot be run.
* 10 : Version Not Found. The given version could not be found OR no version matches the requested command (i.e. No delivered version exists to be used for analysis)
* 1000 : Unexpected error. This can occur for various reasons, and the standard output should be checked for more information.

### Authentication

As detailed in the CAST Imaging documentation, you can obtain the API Key from the profile in the CAST Imaging UI.

[//]: # (If you cannot use an API Key, you can authenticate using username and password, by passing the `--user` flag to a command, and set the user's password in the `--apikey` or set it in an environment variable, which name you'll pass to `--apikey:env`)