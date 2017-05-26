Introduction
==============
Since 8.8.1, you can  benefit from the new "XOM deployment in Decision Center" feature, as explained here:
https://www.ibm.com/support/knowledgecenter/SSQP76_8.8.1/com.ibm.odm.dcenter.deploy/topics/con_deploy_xom.html

If you choose to do so, Decision Center includes both the XOM and the RuleApp upon deployment.

If you choose to implement an offline deployment strategy, driven by the API calls, you need a convenient way to access the XOM as well as the RuleApp.

Generating a RuleApp on disk, for example, to be able to publish it to a binary artifact repository such as Nexus, Artifactory or Code Station, can easily be achieved through the IlrDeploymentFacility of Decision Center, by using the deployDSRuleAppArchive as documented here:
https://www.ibm.com/support/knowledgecenter/SSQP76_8.8.1/com.ibm.odm.dcenter.ref.dc/html/api/html/ilog/rules/teamserver/model/IlrDeploymentFacility.html

However, downloading a copy of the XOM that was used to generate said RuleApp requires using several APIs in sequence. This asset shows how to achieve this.

Moreover, this asset demonstrates how to upload a XOM, so that you can replace the existing one by a version you would have compiled with your toolchain, so that you are sure that the rule artifacts get generated with the exact same xom that was produced by your compilation chain.


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

`ant [required params] upload` to execute the upload


License Information
====================
This project is licensed as specified in this [file](https://hub.jazz.net/project/gmolines/dc-xom-extractor/overview#https://hub.jazz.net/git/gmolines%252Fdc-xom-extractor/contents/master/IBMLicense.txt)
