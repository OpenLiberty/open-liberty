package io.openliberty.microprofile.openapi20.fat.deployments;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.MpOpenAPIElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi20.fat.FATSuite;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestResource;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class StartupWarningMessagesTest {
    private static final String SERVER_NAME = "OpenAPIWarningMessageTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        server.saveServerConfiguration();
    }

    @After
    public void cleanup() throws Exception {
        try {
            if (server.isStarted()) {
                server.stopServer("CWWKO1680W");
            }
        } finally {
            server.deleteAllDropinApplications();
            server.removeAllInstalledAppsForValidation();
            server.clearAdditionalSystemProperties();
            server.restoreServerConfiguration();
        }
    }

    @Test
    public void testAppNameMatchesOtherAppDeploymentName() throws Exception {

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "nameClash.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        ServerConfiguration serverConfig = server.getServerConfiguration().clone();
        serverConfig.addApplication("nameClash", "test1.war", "war");

        MpOpenAPIElement.MpOpenAPIElementBuilder.cloneBuilderFromServerResetAppsAndModules(server)
                                                //these match the application's deployment descriptor but not its name as set in server.xml.
                                                //a warning should be emitted informing the user of their likely mistake and how o fix it.
                                                .addIncludedApplicaiton("nameClash")
                                                .buildAndOverwrite(serverConfig.getMpOpenAPIElement());

        server.updateServerConfiguration(serverConfig);

        ShrinkHelper.exportAppToServer(server, war1, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, war2, SERVER_ONLY, DISABLE_VALIDATION);
        server.startServer();

    }

    @Test
    public void testWarningWhenMatchesDeploymentName() throws Exception {

        ServerConfiguration serverConfig = server.getServerConfiguration().clone();
        serverConfig.addApplication("serverXMLNameIncluded", "testEarIncluded.ear", "ear");
        serverConfig.addApplication("serverXMLNameExcluded", "testEarExcluded.ear", "ear");

        MpOpenAPIElement.MpOpenAPIElementBuilder.cloneBuilderFromServerResetAppsAndModules(server)
                                                //these match the application's deployment descriptor but not its name as set in server.xml.
                                                //a warning should be emitted informing the user of their likely mistake and how o fix it.
                                                .addIncludedApplicaiton("testEarIncluded")
                                                .addExcludedApplicaiton("testEarExcluded")
                                                .addIncludedModule("testEarIncluded/test1")
                                                .addExcludedModule("testEarExcluded/test2")
                                                .buildAndOverwrite(serverConfig.getMpOpenAPIElement());

        server.updateServerConfiguration(serverConfig);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEarIncluded.ear")
                                          .addAsModules(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        EnterpriseArchive ear2 = ShrinkWrap.create(EnterpriseArchive.class, "testEarExcluded.ear")
                                           .addAsModules(war2);

        ShrinkHelper.exportAppToServer(server, ear, SERVER_ONLY, DISABLE_VALIDATION);//set up validation manually because the app name doesn't match the archive name
        server.addInstalledAppForValidation("serverXMLNameIncluded");
        ShrinkHelper.exportAppToServer(server, ear2, SERVER_ONLY, DISABLE_VALIDATION);
        server.addInstalledAppForValidation("serverXMLNameExcluded");

        server.startServer();

        //Example message: CWWKO1680W: The testEar application name in the includeApplication or includeModule configuration element does not match the name of any deployed application but it does match the name from the deployment descriptor of the serverXMLName application. The application name used here must be the application name specified in server.xml, or the archive file name with the extension removed if no name is specified in server.xml.
        List<String> messages = new ArrayList<>();
        messages.add("CWWKO1680W: The testEarIncluded application name in the includeModule configuration.*serverXMLNameIncluded application. The application name");
        messages.add("CWWKO1680W: The testEarIncluded application name in the includeApplication configuration.*serverXMLNameIncluded application. The application name");
        messages.add("CWWKO1680W: The testEarExcluded application name in the excludeModule configuration.*serverXMLNameExcluded application. The application name");
        messages.add("CWWKO1680W: The testEarExcluded application name in the excludeApplication configuration.*serverXMLNameExcluded application. The application name");
        server.waitForStringsInLogUsingMark(messages); //This method asserts the messages exist so no need to check its output.  

    }

}
