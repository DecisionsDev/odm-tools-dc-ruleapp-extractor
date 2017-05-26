package com.ibm.rules.sample;

import ilog.rules.teamserver.brm.IlrBaseline;
import ilog.rules.teamserver.brm.IlrBrmPackage;
import ilog.rules.teamserver.brm.IlrPackageKind;
import ilog.rules.teamserver.brm.IlrResource;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.brm.IlrServer;
import ilog.rules.teamserver.client.IlrRemoteSessionFactory;
import ilog.rules.teamserver.dsm.IlrDeployment;
import ilog.rules.teamserver.model.IlrApplicationException;
import ilog.rules.teamserver.model.IlrArchiveOutput;
import ilog.rules.teamserver.model.IlrConnectException;
import ilog.rules.teamserver.model.IlrDefaultSearchCriteria;
import ilog.rules.teamserver.model.IlrElementDetails;
import ilog.rules.teamserver.model.IlrElementError;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionFactory;
import ilog.rules.teamserver.model.IlrSessionHelper;
import ilog.rules.teamserver.model.permissions.IlrRoleRestrictedPermissionException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class RuleAppExtractor {


	private IlrBaseline findBaseline(IlrSession session, String url, String datasource, String user, String password, String project, String baselineName) throws IlrObjectNotFoundException {
		IlrBaseline baseline = null;
		
        // get the requested project
        IlrRuleProject ruleProject = IlrSessionHelper.getProjectNamed(session, project);
        if (ruleProject == null) {
            System.err.format("Project not found: '%s' %n", project);
        } else {
	        // get the requested baseline, or main if none was specified
	        IlrBaseline mainBaseline = ruleProject.getCurrentBaseline(); // initializes to the main branch
	        baseline = mainBaseline;
	        if (baselineName != null) {
	            baseline = IlrSessionHelper.getBaselineNamed(session, ruleProject, baselineName);
	            if (baseline == null) {
	                System.err.format("Baseline not found: '%s' - using main %n", baselineName);
	                baseline = mainBaseline;
	            }
	        }
        }
        
		return baseline;
	}
	
	private IlrResource findXOM(IlrSession session, String xomName) throws IlrRoleRestrictedPermissionException, IlrObjectNotFoundException {
        // builds a query to find the XOM resource
        //
        IlrBrmPackage brm = session.getBrmPackage(); // BR Model package, to get meta data from
        IlrDefaultSearchCriteria criteria = new IlrDefaultSearchCriteria(brm.getResource()); // select the EClass for "resources"
        criteria.setFeatures(Arrays.asList(brm.getModelElement_Name())); // add a selection criteria on the name
        criteria.setValues(Arrays.asList(xomName)); // whose value should be xomName
        List<IlrElementDetails> elements = session.findElementDetails(criteria); // run the query

        // Check if we found anything
        IlrResource xom = elements.isEmpty() ? null : (IlrResource)elements.get(0);
        return xom;
	}
	
	public void downloadRuleApp(String url, String datasource, String user, String password,
			String project, String baselineName, String deploymentConfigName, String filePath) {

        IlrSessionFactory factory = new IlrRemoteSessionFactory();
        IlrSession session = null;
        boolean reDeploy = false;

        try {
			// connect to Decision Center
            factory.connect(user, password, url, datasource);
            session = factory.getSession();
            session.beginUsage();

            IlrBaseline baseline = findBaseline(session, url, datasource, user, password, project, baselineName);
            session.setWorkingBaseline(baseline);

			// Get the deployment configuration
            IlrDeployment deployment = (IlrDeployment) IlrSessionHelper
					.getElementFromPath(session, deploymentConfigName, IlrPackageKind.DEPLOYMENT_LITERAL);
			HashMap<String, String> rulesetsVersionsMap =  new HashMap<String, String>();
			
			// Generate the Decision Service archive. The 2nd param as an empty list prevents deployment on any RES
			List<IlrServer> servers = new ArrayList<IlrServer>();
			IlrArchiveOutput output = session.deployDSRuleAppArchive(deployment, servers, baselineName,
					reDeploy, rulesetsVersionsMap);
			Iterator<IlrElementError> it = output.getCheckingErrors().iterator();
			// if no error, write the file to disk
			if (!it.hasNext()) {
				System.out.println("Extraction succeeded.");
				String filename = (String) output.getAttribute(IlrArchiveOutput.DEPLOYMENT_NAME_ATTRIBUTE)
							+ ".jar";
				File archive = new File(filePath, filename).getCanonicalFile();
				FileOutputStream stream = new FileOutputStream(archive);
				try {
					stream.write(output.getBytes());
				} finally {
					stream.close();
				}
			}
			// otherwise, write the errors to the console
			while (it.hasNext()) {
				IlrElementError error = it.next();
				System.out.println(IlrElementError.errorAsString(error, session));
			}

        } catch (IlrConnectException | IlrApplicationException | IOException e) {
			System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.endUsage();
            }
        }

    }

	public void uploadXOM(String url, String datasource, String user, String password, String project, String baselineName, String xomName, String filePath) {

        IlrSessionFactory factory = new IlrRemoteSessionFactory();
        IlrSession session = null;

        try {

			// connect to Decision Center
            factory.connect(user, password, url, datasource);
            session = factory.getSession();
            session.beginUsage();

            IlrBaseline baseline = findBaseline(session, url, datasource, user, password, project, baselineName);
            session.setWorkingBaseline(baseline);

            IlrResource xom = findXOM(session, xomName);
            IlrBrmPackage brm = session.getBrmPackage(); // BR Model package, to get meta data from

            // reads the new XOM from disk
            if (xom != null) {
                Path path = Paths.get(filePath);
                byte[] xomBytes = Files.readAllBytes(path);
                // store this new files in the resource
                xom.setRawValue(brm.getResource_Body(), xomBytes);
                // and commits this to Decision Center
                session.commit(xom);
                System.err.format("Read file: '%s' %n", filePath);
            } else {
                System.err.format("XOM not found: '%s' %n", xomName);
            }

        } catch (IlrConnectException | IlrApplicationException | IOException e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.endUsage();
            }
        }

    }
	
    public static void main(String args[]) {

        Options options = new Options();

        Option command = Option.builder("command").hasArg().argName("command").required().desc("Command (download or upload)").build();
        Option url = Option.builder("url").hasArg().argName("url").required().desc("Decision Center URL").build();
        Option datasource = Option.builder("datasource").hasArg().argName("datasource").required().desc("JDBC datasource").build();
        Option user = Option.builder("user").hasArg().argName("user").required().desc("User name").build();
        Option password = Option.builder("password").hasArg().argName("password").required().desc("User password").build();
        Option project = Option.builder("project").hasArg().argName("project").required().desc("Project name").build();
        Option baseline = Option.builder("baseline").hasArg().argName("baseline").desc("Baseline (current if not provided)").build();
        Option deploymentConfigName = Option.builder("deploymentConfigName").hasArg().argName("deploymentConfigName").required().desc("Deployment Config name").build();
        Option filepath = Option.builder("filepath").hasArg().argName("filepath").required().desc("Output file path").build();

        options.addOption(command);
        options.addOption(url);
        options.addOption(datasource);
        options.addOption(user);
        options.addOption(password);
        options.addOption(project);
        options.addOption(baseline);
        options.addOption(deploymentConfigName);
        options.addOption(filepath);

        CommandLineParser parser = new DefaultParser();
        RuleAppExtractor raExtractor = new RuleAppExtractor();
        try {
            CommandLine cmd = parser.parse(options, args);
            String runCommand = cmd.getOptionValue("command");
            if ("download".equalsIgnoreCase(runCommand)) {
                raExtractor.downloadRuleApp(
	                    cmd.getOptionValue("url"),
	                    cmd.getOptionValue("datasource"),
	                    cmd.getOptionValue("user"),
	                    cmd.getOptionValue("password"),
	                    cmd.getOptionValue("project"),
	                    cmd.getOptionValue("baseline"),
	                    cmd.getOptionValue("deploymentConfigName"),
	                    cmd.getOptionValue("filepath")
	            );
            }

        } catch (ParseException e) {
            System.err.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java", options);
        }

    }

}