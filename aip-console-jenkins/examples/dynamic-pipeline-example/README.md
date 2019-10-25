## Jenkins Pipeline example with AIP Console plugin

This example reads content from a JSON file and builds a list of parallel steps to be run in a Jenkins Pipeline job.

We run this script internally to run the analysis on multiple projects and their associated versions.
To accomplish this, we created a new Pipeline Job, with "Pipeline from SCM", pointing it to a repository containing the `Jenkinsfile` and the ``projects.json``.
Once we launch the build, the build will read the JSON file and build a list of steps based on the number of applications and projects to be then executed in parallel by Jenkins. 

Check below for more details about how the build is defined and run.

### List of projects

Here is an example of the content of the `projects.json` file :

```json
[
  {
    "enabled": true,
    "name": "AIP Console tools",
    "url": "https://github.com/CAST-Extend/com.castsoftware.uc.aip.console.tools.git",
    "versions": [
      {
        "versionName": "tools-0.0.1-${BUILD_ID}",
        "branch": "refs/tags/0.0.1",
        "rescan": false
      },
      {
        "versionName": "tools-master-${BUILD_ID}",
        "branch": "refs/heads/master",
        "rescan": true
      }
    ]
  },{
    "enabled": true,
    "name": "Tomcat",
    "url": "https://github.com/apache/tomcat.git",
    "versions": [
      {
        "versionName": "tomcat-9.0.22-${BUILD_ID}",
        "branch": "refs/tags/9.0.22",
        "rescan": false
      },
      {
        "versionName": "tomcat-master-${BUILD_ID}",
        "branch": "refs/heads/master",
        "rescan": true
      }
    ]
  }
]
```
The JSON file contains a list of projects. 
Each project has an `enabled` attribute to define whether it should be analyzed on AIP Console or not.

URLs for each project should only point to a git repository that we'll use to retrieve the source code. 
You can specify a credential ID if you need authentication to the Git repository you're targeting.
Otherwise we'll just call the Git URL without any authentication

`versions` contains all versions that should be analyzed. For each version (order matters !), we'll create the version on AIP Console, with the given version name, the given branch name and we'll either enable rescan based on that parameter value (rescan meaning, in essence, cloning the previous version's configuration and running a new analysis)

For branches, you can either give a direct `ref` like `refs/heads/develop` for the develop branch, `refs/tags/v1.0.1` for the v1.0.1 tag, etc. or just a branch name.

### How the build works

In the `Jenkinsfile`, we define a 'Prepare projects' stage, where we retrieve the json file and parse it to list all the steps that will be done.

By default, we run everything in parallel until the Run analysis step of each projects, which have a lock surrounding them.
This is to avoid launching all jobs at once and have them wait indefinitely for other jobs to complete.
Once a lock is released, the analysis of blocked jobs will start up.

To accomplish this, we filter out the projects that are not enabled in the json file, then for each project found we define the following steps in a map :

* Assign a node
* Use a subdirectory with the application name
* For each versions :
  * Lock a resource; we define a label based on current index, with a modulo 2 so that it'll alternate between `lock-0` and `lock-1`
  * Retrieve the source code for this version's branch (or tag)
  * Generate a Zip file to be uploaded
  * Run the Add Version step

Once this map is constructed, we pass it to a `parallel` step, and let Jenkins handle parallel execution of each of those steps.

### Dry Run

There is a "dry run" parameter which doesn't execute anything but rather writes all the configuration that would be passed to either the `scm` or `addVersion` steps.
It might be useful to understand a bit more how the pipeline script works.
