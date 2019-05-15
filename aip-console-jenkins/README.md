# AIP Console Jenkins Plugin

### Configuration

You can define global settings for the AIP Console Jenkins Plugins. 

Under Manage Jenkins > Configure System > AIP Console global configuration

![Global Configuration](./doc/images/global-conf.png)

Here you can define:
* AIP Console URL: The main URL to access AIP Console. If you access the AIP Console UI with, for example, http://my-aip-console:8081/ui/index.html, then the AIP Console URL will be http://my-aip-console:8081. If left blank this will default to http://localhost:8081
* API Key: An API Key, generated inside AIP Console (top right menu). If this option is missing from your AIP Console instance, you can set this field to a user's aip console password and set the `user name` field inside "Legacy Settings"
* User Name: The user name to use for basic authentication. This should only be used if your AIP Console is 1.9.0 or below.

### Create Application Step

In the "Build" of your job, add a new Build Step, then click "Create Application on AIP Console".

![Create Application Capture](./doc/images/create-app.png)

In the step definition, you can either set a fixed value, an environment variable, or a Build Parameter (here, `$APP_NAME` is a build parameter). 

Once you run the job, you'll get the following output :

```
Starting 'Create Application Job'
Job Started, polling status :
Current step of job is Create delivery folder
Current step of job is Create application schemas
Current step of job is Declare application in Management database
Application 'abr_jenkins_test' was created successfully, application guid is : a612338b-79a4-4fc4-9c57-12a98c694d7e
Build step 'Create Application on AIP Console' changed build result to SUCCESS
```

The plugin will create a new application on your target AIP Console instance, and regularly poll the job status until it is either completed, successfully or not.

It'll then show the Application GUID if the newly created application.

You can use this step both with a Freestyle Job and a Pipeline Job.

### Add Version Step

In the "Build" of your job, add a new Build Step, then click "Add Application version on AIP Console".

![Add Application Version capture](./doc/images/add-version.png)

You must give an Application GUID and a path to a `zip` file as the first 2 parameters. The Zip file will be uploaded to AIP Console, extracted and the run through the whole analysis workflow, as if you had done so through the AIP Console UI.

If you check the "Copy configuration" checkbox, this new version will copy the previous version's configuration (packages, extensions, etc.). 

You can provide a version name. If left empty, the following version will be based o nthe current date and time and formatted as such: "vyyMMdd.HHmmss"

Here is a sample output of the Add Version step : 

```
Uploading given file to AIP Console
Starting new Add version job
Polling status of Add Version job
Current step of job is Content discovery
Current step of job is Creating version
Current step of job is Creating packages
Current step of job is Attaching packages to version
Current step of job is Delivering version
Current step of job is Accepting Delivery
Current step of job is Importing configuration
Current step of job is Analyzing application
Current step of job is Create snapshot
Current step of job is Consolidate snapshot and publish to Health Dashboard
Analysis of application is completed.
```

As with the Create Application step, you can also use this step in a Pipeline Project:

```
aipAddVersion applicationGuid: 'a2b41b7e-538a-4ec8-8ddf-8ec7baa5a980', filePath: 'source.zip', versionName: '$BUILD_ID'
```

## Development 

### Generating the HPI file

Provided you have Maven installed, you can run the following command 

```bash
mvn package
```

This will generate a `.hpi` file under the `target` directory. You can then add this plugin to your Jenkins instance by going Manage Jenkins > Manage Plugins > Advanced tab and then Upload the `aip-console-jenkins.hpi` file.

### Testing changes to the plugin

Run the following command :

```bash
mvn package hpi:run
```

This will compile the plugin and the start and instance of Jenkins (at http://localhost:8080) where your plugin will be automatically installed. Please refer to the [Jenkins documentation regarding plugin development](https://jenkins.io/doc/developer/plugin-development/) for further details. 