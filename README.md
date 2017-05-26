Introduction
==============
This project shows how to extract the RuleApp of a specific Decision Service from Decision Center.

The IlrDeploymentFacility interface exposes a deployDSRuleAppArchive API, which does in fact two things:
- it generates the RuleApp of a Decision Service and returns it
- and it deploys it to the list of specified RES servers.

If this API is called with an empty server list, then it can be used to retrieve the RuleApp locally.
This is very useful when implementing a DevOps pipeline in which you want an offline deployment strategy.
With the [odm-tools-dc-xom-extractor](https://git.ng.bluemix.net/guilhem.molines/odm-tools-dc-xom-extractor) you can get the XOM supporting the RuleApp, and with this project, you can get the RuleApp.
These two elements can then be published to a binary artifact repository such as Nexus, Artifactory or Code Station, where they can be picked up by an automated deployment process of your choice.


Software Prerequisites
========================
IBM Operational Decision Manager, including Decision Center

Version(s) Supported
======================
IBM ODM 8.8.1 (and later)

Usage Instructions
===================
Clone the git repository locally.

In the `build.xml` file at the root, set the value of the `teamserver.home` property to the location where you have installed Decision Center.

Then run:

`ant usage` to discover the command line argument

`ant compile` to compile the java class

`ant [required params] download` to execute the extraction

Note the value of the "redeploy" flag within the code, which specifies if a new deployment baseline should be created or if we should redeploy an existing one.


License Information
====================
This project is licensed as specified in this [file](https://git.ng.bluemix.net/guilhem.molines/odm-tools-dc-ruleapp-extractor/blob/master/IBMLicense.txt)
