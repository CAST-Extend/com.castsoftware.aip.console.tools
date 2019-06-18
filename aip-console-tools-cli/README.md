## AIP Console CLI Tool

This project is aimed at facilitating automation of AIP Console with CI tools.

It can do the following :
* Create a new application on a target AIP Console
* Automate the delivery of your source code to an instance of AIP Console, by uploading a zip containing your source and starting the full workflow of application analysis (i.e. creating a version, analysis, snapshot creation and snapshot publishing).

### Pre-requisite

Java version from 8 to 12 are compatible with this CLI tool. 
HotSpot (AdoptOpenJDK, Oracle) JVMs have been tested and work well; other JVMs (i.e. Eclipse OpenJ9, Azul Zulu, Amazon Corretto, etc.) have not been tested, but they should have no issues.  

Before using this tool, you will have to :

* Create an application on AIP Console and keep its GUID, as it is our only way to uniquely identify which application is targeted.
* NOT YET IMPLEMENTED <s>Generate an API Token for your user. See below in the "Token" chapter to see different methods for passing the token to AIP Console CLI Tool.</s>
* Prepare your source code as a ZIP file that will be uploaded to AIP Console.

**NB:** The provided ZIP file structure will be better processed if you separate each technology into a specific root folder.
  

### Basic Usage

This tool accept 2 different actions :
* `CreateApplication` (aliased `new`) : Create a new application on AIP Console
* `AddVersion` (aliased `add`) : Creates a new version for a given application on AIP Console

The following options are common to both actions  
* `-s` or `--server-url` : URL to access AIP Console. Defaults to http://localhost:8081
* `--apikey` : Option to prompt the user for an API Key to access AIP Console
* `--apikey:env` :  Option to read the API key value from an environment variable.

**NB**: If either `--apikey` or `--apikey:env` is missing, the tool will fail to access AIP Console.

You can check below to see all parameters for each actions.   

#### Create application

Here is an example to create a new application with AIP Console CLI Tool :

```bash
$ java -jar aip-console-tools-cli.jar CreateApplication -n "my_new_application_name" 
```

This will output the following :

```bash
$ java -jar aip-console-tools-cli.jar CreateApplication --apikey:env=AIP_CONSOLE_KEY -n "my_new_application"
2019-04-25 17:09:26.201 - INFO --- Started job to create new application.
2019-04-25 17:09:26.210 - INFO --- Checking status of Job with GUID 17bcd793-a6eb-40b2-9250-5dd86dfbd6a6
2019-04-25 17:09:26.453 - INFO --- Current job step is 'create_delivery_folder'
2019-04-25 17:09:36.631 - INFO --- Current job step is 'restore_triplet'
2019-04-25 17:10:07.221 - INFO --- Current job step is 'import_preferences'
2019-04-25 17:10:27.705 - INFO --- Current job step is 'manage_application'
2019-04-25 17:10:37.875 - INFO --- Creation of version successful for application 'my_new_application'. Application GUID is '500f089e-263d-4d09-8b6e-c5df5902cf12'
```

The parameters for `CreateApplication` are the following :

```bash
Usage: aip-console-tools-cli CreateApplication [-hV] [--apikey=<apiKey>]
                                              [--apikey:env=ENV_VAR_NAME]
                                              -n=APPLICATION_NAME
                                              [-s=AIP_CONSOLE_URL]
Creates a new application on AIP Console
      --apikey=<apiKey>   Enable prompt to enter password after start of CLI
      --apikey:env=ENV_VAR_NAME
                          The name of the environment variable containing the user's access token to AIP Console
  -h, --help              Show this help message and exit.
  -n, --app-name=APPLICATION_NAME
                          The name of the application to create
  -s, --server-url=AIP_CONSOLE_URL
                          The base URL for AIP Console (defaults to http://localhost:8081)
  -V, --version           Print version information and exit.
```

#### Add Version

Here is an example command to create a new version :
```bash
$ $JAVA_HOME/bin/java -jar aip-console-tools-cli.jar AddVersion --apikey:env=AIP_CONSOLE_KEY -a de7655a3-ecaa-4cd7-b860-5079a138db96 -f /tmp/jenkins-2.171.zip
```

Here is the output for this command :

```bash
$ $JAVA_HOME/bin/java -jar aip-console-tools-cli.jar AddVersion --apikey:env=AIP_CONSOLE_KEY -n "my cli application" -f /tmp/jenkins-2.171.zip
2019-06-18 15:56:50.248 - INFO --- Search for application 'my cli application' or AIP Console
2019-06-18 15:58:02.236 - INFO --- Creating a new upload for application
2019-04-12 16:26:07.625 - INFO --- Uploading chunk 1 of 2
2019-04-12 16:26:07.850 - INFO --- Uploading chunk 2 of 2
2019-04-12 16:26:08.115 - INFO --- Upload completed.
2019-04-12 16:26:08.117 - INFO --- Starting "Add Version" job for application with GUID de7655a3-ecaa-4cd7-b860-5079a138db96
2019-04-12 16:26:08.413 - INFO --- Successfully started Job
2019-04-12 16:26:08.415 - INFO --- Checking status of Job with GUID e9ca3e3e-ca5e-4c9e-9c4b-c49f56c1e682
2019-04-12 16:26:08.455 - INFO --- Current job step is 'unzip_source'
2019-04-12 16:26:58.533 - INFO --- Current job step is 'code_scanner'
2019-04-12 16:30:59.114 - INFO --- Current job step is 'add_version'
2019-04-12 16:31:09.144 - INFO --- Current job step is 'create_package'
2019-04-12 16:31:29.179 - INFO --- Current job step is 'attach_package_to_version'
2019-04-12 16:46:40.900 - INFO --- Current job step is 'deliver_version'
2019-04-12 16:47:00.938 - INFO --- Current job step is 'accept'
2019-04-12 16:47:31.005 - INFO --- Current job step is 'setcurrent'
2019-04-12 16:50:01.271 - INFO --- Current job step is 'update_extensions'
2019-04-12 16:50:11.299 - INFO --- Current job step is 'analyze'
2019-04-12 17:34:51.004 - INFO --- Current job step is 'snapshot'
2019-04-12 17:36:12.263 - INFO --- Current job step is 'consolidate_snapshot'
2019-04-12 17:36:13.745 - INFO --- Job completed successfully.
```

This command will first search for the application `my cli application` on AIP Console server located at `http://localhost:8081` and then upload the file `jenkins-2.171.zip`. Once the upload is complete, the CLI tool will ask AIP Console to start an "Add Version" job, with snapshot creation.

It'll then wait until the job is complete on AIP Console before closing, continuously monitoring the status of the job on AIP Console.
The AIP Console CLI Tool will output information in the standard output, including error messages.

Here is a detailed look at the options available for `AddVersion` : 

```bash
$ java -jar target/aip-console-tools-cli.jar AddVersion -h
Usage: aip-integration-tool AddVersion [-chV] [--auto-create] [--apikey[=<apiKey>]] [--apikey:env=ENV_VAR_NAME]
                                       [--user=<username>] [-a=APPLICATION_GUID] -f=FILE [-n=APPLICATION_NAME]
                                       [-s=AIP_CONSOLE_URL] [-v=VERSION-NAME]
Creates a new version for an application on AIP Console
      --apikey[=<apiKey>]   The API Key to access AIP Console. Will prompt entry if no value is passed.
      --apikey:env=ENV_VAR_NAME
                            The name of the environment variable containing the AIP Key to access AIP Console
      --auto-create         If the given application name doesn't exist on the target server, it'll be automatically created
                              before creating a new version
      --user=<username>     User name. Use this if no API Key generation is available on AIP Console. Provide the user's
                              password in the apikey parameter.
  -a, --app-guid=APPLICATION_GUID
                            The GUID of the application to rescan
  -c, --clone, --rescan     Clones the latest version configuration instead of creating a new application
  -f, --file=FILE           The ZIP file containing the source to rescan
  -h, --help                Show this help message and exit.
  -n, --app-name=APPLICATION_NAME
                            The Name of the application to rescan
  -s, --server-url=AIP_CONSOLE_URL
                            The base URL for AIP Console (defaults to http://localhost:8081)
  -v, --version-name=VERSION-NAME
                            The name of the version to create
  -V, --version             Print version information and exit.
```

When starting the CLI, you can either provide an application name or an application GUID. If an application name is provided, it'll be looked up on AIP Console before continuing. If the CLI cannot find the application, it'll exit.

Note that by default, the CLI will not create the application if it cannot be found. You have to provide the `--auto-create` flat. In which case, the application will be created before the version is added.

By default, `AddVersion` will create a new version. If you want to clone an existing version, you will have to provide the `-c` flag, which will copy the previous version configuration.

To automate error handling, specific codes will be returned once the tool closes. 
See below for details on which return code corresponds to which potential issue.

### API Key

To access AIP Console, you have to provide an API key by generating one on AIP Console.

You can provide the `--apikey` parameter to be prompted to enter your password without passing it as an argument.

You can also provide the `--apikey:env` parameter and specify an environment variable name where the API key will be obtained.

To create an API key for your account, log in to AIP Console, click on your user name (top right) and select "API Key".

You will be able to generate an API key that will be shown to you. Copy it and save it to a secure location.

**NB**: For backwards compatibility purposes, you can provide the `--user` parameter to specify a user name. In the `--apikey` or `--apikey:env`, you should specify the password (or the environment variable with the password) and we'll let you authenticate this way. This will not be supported in later versions of the CLI and should be used only if you are using AIP Console where API Key generation isn't available.

### Return Codes

Here is a list of returned error codes :

* 0 : No errors, processing was completed correctly. This is also the return code for`--help` and `--version` parameters.
* 1 : API key missing. No API key was provided either in the prompt or in the environment variable.
* 2 : Login Error. Unable to login to AIP Console with the given API key. Please check that you provide the proper value. 
* 3 : Upload Error. An error occurred during upload to AIP Console. Check the standard output to see more details.
* 4 : Add Version Job Error. Creation of the Add Version job failed, or AIP CLI is unable to get the status of the running job. Please see the standard output for more details regarding this error.
* 5 : Job terminated. The Add Version job did not finish in an expected state. Check the standard output or AIP Console for more details about the state of the job.
* 6 : Application name or GUID missing. The AddVersion job cannot run due to a missing application name or missing application guid.
* 7 : Application Not Found. The given Application Name or GUID could not be found.
* 1000 : Unexpected error. This can occur for various reasons, and the standard output should be checked for more information.