/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.batch.runtime.StepExecution;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class BatchFatUtils {

    static JavaArchive createTestUtilJar() throws Exception {
        JavaArchive jar = ShrinkHelper.buildJavaArchiveNoResources("testutil.jar",
                                                 "batch.fat.util");
        return jar;
    }

    static JavaArchive createCommonUtilJar() throws Exception {
        JavaArchive jar = ShrinkHelper.buildJavaArchiveNoResources("commonUtil.jar",
                                                 "batch.fat.common.util", 
                                                 "com.ibm.ws.jbatch.test",
                                                 "com.ibm.ws.jbatch.test.dbservlet");
        return jar;
    }

    static JavaArchive createClientJar() throws Exception {
        JavaArchive jar = ShrinkHelper.buildJavaArchiveNoResources("com.ibm.ws.jbatch.test.dbservlet.client.jar", 
                                                 "com.ibm.ws.jbatch.test",
                                                 "com.ibm.ws.jbatch.test.dbservlet");
        return jar;
    }

    static WebArchive createDbServletAppWar() {
        WebArchive webApp = createBatchWar("DbServletApp.war", 
                                    "(.*)(DbServlet|ServerKillerServlet|StringUtils)(.*)",  // include regex
                                    "batch.fat.web");
        return webApp;
    }

    static WebArchive createBatchSecurityWar() {
        WebArchive webApp = createBatchWar("batchSecurity.war", 
                                    null, 
                                    "batch.fat.artifacts", "batch.security");
        return webApp;
    }

    static WebArchive createBatchFatWar() throws Exception {
        WebArchive webApp = createBatchWar("batchFAT.war",
                                    null, 
                                    "batch.fat.artifacts", "batch.fat.cdi", "batch.fat.common", "batch.fat.web", "batch.fat.web.customlogic",
                                    "chunktests.artifacts",
                                    "processitem.artifacts");
        JavaArchive jar = createTestUtilJar();
        webApp.addAsLibrary(jar);

        jar = createCommonUtilJar();
        webApp.addAsLibrary(jar);

        return webApp;
    }

    static WebArchive createBonusPayoutWar() throws Exception {
        WebArchive webApp = createBatchWar("BonusPayout.war", 
                                    null,
                                    "com.ibm.websphere.samples.batch.artifacts",
                                    "com.ibm.websphere.samples.batch.beans",
                                    "com.ibm.websphere.samples.batch.fat",
                                    "com.ibm.websphere.samples.batch.util");

        JavaArchive jar = createCommonUtilJar();
        webApp.addAsLibrary(jar);

        return webApp;
    }

    static EnterpriseArchive createBonusPayoutEar() throws Exception {
        final String appName = "BonusPayoutEAR.ear";
        EnterpriseArchive earApp = ShrinkWrap.create(EnterpriseArchive.class, appName);
        File appXml = new File("test-applications/" + appName + "/resources/META-INF/application.xml");
        earApp.setApplicationXML(appXml);
        WebArchive webApp = createBonusPayoutWar();
        earApp.addAsModule(webApp);

        return earApp;
    }

    static WebArchive createBatchWar(String appName,
                                     String regex,
                                     String... packageNames) {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName); // appName ends with ".war"
        if (regex == null) {
            webApp.addPackages(false, packageNames);
        } else {
            webApp.addPackages(false, Filters.include(regex), packageNames);  // Exclude subpackages, include pkg paths matching regex 
        }
        // Web-inf resources
        File webInf = new File("test-applications/" + appName + "/resources/WEB-INF");
        if (webInf.exists()) {
            for (File webInfElement : webInf.listFiles()) {
                if (!!!webInfElement.isDirectory()) { // Ignore classes subdir
                    webApp.addAsWebInfResource(webInfElement);
                }
            }
        }
        // Batch job definition files
        File webInfBatchJobs = new File("test-applications/" + appName + "/resources/WEB-INF/classes/META-INF/batch-jobs");
        if (webInfBatchJobs.exists()) {
            for (File batchJob : webInfBatchJobs.listFiles()) {
                String target = "classes/META-INF/batch-jobs/" + batchJob.getName();
                webApp.addAsWebInfResource(batchJob, target);
            }
        }
        // Package properties
        File pkgProps = new File("test-applications/" + appName + "/package.properties");
        if (pkgProps.exists()) {
            webApp.addAsWebResource(pkgProps);
        }
        // Readme
        File readme = new File("test-applications/" + appName + "/README.txt");
        if (readme.exists()) {
            webApp.addAsWebResource(readme);
        }

        return webApp;
    }
    
    
    
    
    

    public static WebArchive addDropinsDbServletAppWar(LibertyServer targetServer) throws Exception {
        WebArchive webApp = createDbServletAppWar();
        ShrinkHelper.exportToServer(targetServer, "dropins", webApp);
        return webApp;
        //return addDropinsWebApp(targetServer, "DbServletApp.war", 
        //                 "batch.fat.web", "batch.fat.common.util");
    }
    public static WebArchive addDropinsBatchSecurityWar(LibertyServer targetServer) throws Exception {
        WebArchive webApp = createBatchSecurityWar();
        ShrinkHelper.exportToServer(targetServer, "dropins", webApp);
        return webApp;
        //return addDropinsWebApp(targetServer, "batchSecurity.war",
        //                 "batch.fat.artifacts", "batch.security", "batch.fat.util");
    }
    public static WebArchive addDropinsBatchFatWar(LibertyServer targetServer) throws Exception {
        WebArchive webApp = createBatchFatWar();
        ShrinkHelper.exportToServer(targetServer, "dropins", webApp);
        return webApp;
        //return addDropinsWebApp(targetServer, "batchFAT.war",
        //                 "batch.fat.artifacts", "batch.fat.cdi", "batch.fat.common", "batch.fat.web", "batch.fat.web.customlogic", "batch.fat.util",
        //                 "chunktests.artifacts",
        //                 "processitem.artifacts");
    }
    public static WebArchive addDropinsBonusPayoutWar(LibertyServer targetServer) throws Exception {
        WebArchive webApp = createBonusPayoutWar();
        ShrinkHelper.exportToServer(targetServer, "dropins", webApp);
        return webApp;
        //return addDropinsWebApp(targetServer, "BonusPayout.war", 
        //                 "com.ibm.websphere.samples.batch.artifacts",
        //                 "com.ibm.websphere.samples.batch.beans",
        //                 "com.ibm.websphere.samples.batch.fat",
        //                 "com.ibm.websphere.samples.batch.util",
        //                 "batch.fat.common.util");
    }
    public static EnterpriseArchive addDropinsBonusPayoutEar(LibertyServer targetServer) throws Exception {
        EnterpriseArchive earApp = createBonusPayoutEar();
        ShrinkHelper.exportToServer(targetServer, "dropins", earApp);
        return earApp;
    }

    public static WebArchive addDropinsWebApp(LibertyServer targetServer,
                                              String appName,
                                              String... packageNames) throws Exception {
        return addWebApp(targetServer, IS_DROPIN, appName, packageNames);
    }

    public static final boolean IS_DROPIN = true;

    public static WebArchive addWebApp(LibertyServer targetServer,
                                       boolean isDropin,
                                       String appName,
                                       String... packageNames) throws Exception {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName); // Assumes appName ends in ".war"
        webApp.addPackages(true, packageNames);
        // Web-inf resources
        File webInf = new File("test-applications/" + appName + "/resources/WEB-INF");
        if (webInf.exists()) {
            for (File webInfElement : webInf.listFiles()) {
                if (!!!webInfElement.isDirectory()) { // Ignore classes subdir
                    webApp.addAsWebInfResource(webInfElement);
                }
            }
        }
        // Batch job definition files
        File webInfBatchJobs = new File("test-applications/" + appName + "/resources/WEB-INF/classes/META-INF/batch-jobs");
        if (webInfBatchJobs.exists()) {
            for (File batchJob : webInfBatchJobs.listFiles()) {
                String target = "classes/META-INF/batch-jobs/" + batchJob.getName();
                webApp.addAsWebInfResource(batchJob, target);
            }
        }
        // Package properties
        File pkgProps = new File("test-applications/" + appName + "/package.properties");
        if (pkgProps.exists()) {
            webApp.addAsWebResource(pkgProps);
        }
        // Readme
        File readme = new File("test-applications/" + appName + "/README.txt");
        if (readme.exists()) {
            webApp.addAsWebResource(readme);
        }
        String appFolder = (isDropin ? "dropins" : "apps");
        ShrinkHelper.exportToServer(targetServer, appFolder, webApp);

        return webApp;
    }




    public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";

    /**
     * @return http://{server.host}:{server.port}/{contextRoot}{uri}
     */
    public static URL buildUrl(LibertyServer server, String contextRoot, String uri) throws IOException {
        return new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + uri);
    }

    /**
     * @param jobExecution
     *
     * @return true if jobExecution.batchStatus is any of STOPPED, FAILED, COMPLETED, ABANDONED.
     */
    public static boolean isDone(JsonObject jobExecution) {
        String batchStatus = jobExecution.getString("batchStatus");
        return ("STOPPED".equals(batchStatus) ||
                "FAILED".equals(batchStatus) ||
                "COMPLETED".equals(batchStatus) || "ABANDONED".equals(batchStatus));
    }

    /**
     * Parse job parameters from the request's query parms.
     *
     * @param queryParmNames The query parms to include in the job parameters Properties object
     *
     * @return the given query parms as a Properties object.
     */
    public static Properties getJobParameters(HttpServletRequest request, String... queryParmNames) throws IOException {
        Properties retMe = new Properties();

        for (String queryParmName : queryParmNames) {
            String queryParmValue = request.getParameter(queryParmName);
            if (queryParmValue != null) {
                retMe.setProperty(queryParmName, queryParmValue);
            }
        }

        return retMe;
    }

    /**
     * Converts List<StepExecution> to map, with key equal to "step name (id)"
     *
     * @param stepExecutions
     * @return
     */
    public static Map<String, StepExecution> getStepExecutionMap(List<StepExecution> stepExecutions) {
        Map<String, StepExecution> map = new HashMap<String, StepExecution>();
        for (StepExecution step : stepExecutions) {
            map.put(step.getStepName(), step);
        }

        return Collections.unmodifiableMap(map);
    }

}
