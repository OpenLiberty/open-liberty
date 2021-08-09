package io.openliberty.microprofile.openapi20.fat.deployments;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import componenttest.topology.utils.LibertyServerUtils;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestResource;
import io.openliberty.microprofile.openapi20.fat.deployments.test2.DeploymentTestResource2;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;

/**
 * Test that OpenAPI works correctly when the application is deployed in different ways (ear, war, etc.)
 */
@RunWith(FATRunner.class)
public class DeploymentTest {

    @Server("OpenAPITestServer")
    public static LibertyServer server;
    
    /**
     * Add configuration to only scan the test resource class
     * <p>
     * If file paths are not handled correctly, this will cause the test resource class not to be scanned and fail the test.
     */
    private final static PropertiesAsset SCAN_CONFIG = new PropertiesAsset().addProperty("mp.openapi.scan.classes", DeploymentTestResource.class.getName());

    private ServerConfiguration initialConfig;

    @BeforeClass
    public static void setupServer() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void shutdownServer() throws Exception {
        server.stopServer();
    }

    @Before
    public void setup() throws Exception {
        initialConfig = server.getServerConfiguration();
        RemoteFile testLibsDir = LibertyFileManager.createRemoteFile(server.getMachine(), server.getServerRoot() + "/testLibs");
        testLibsDir.mkdirs();
    }

    @After
    public void cleanup() throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(initialConfig);
        server.waitForConfigUpdateInLogUsingMark(Collections.emptySet());

        RemoteFile appsDir = server.getFileFromLibertyServerRoot("apps");
        for (RemoteFile app : appsDir.list(false)) {
            app.delete();
        }

        RemoteFile testLibsDir = LibertyFileManager.createRemoteFile(server.getMachine(), server.getServerRoot() + "/testLibs");
        for (RemoteFile lib : testLibsDir.list(false)) {
            lib.delete();
        }
    }

    @Test
    public void testEar() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testWar.war")
                                   .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                   .addAsResource(SCAN_CONFIG, "META-INF/microprofile-config.properties");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                                          .addAsModule(war);

        deployApp(ear);

        assertRest();
        assertOpenApiDoc();
        // Slightly odd behaviour - the app manager expands the .ear so the .war file is available on disk
        // However, when the .ear is redeployed, the .ear is re-expanded and the location is different
        assertCache(ear, CacheUsed.CACHE_NOT_USED, CacheWritten.CACHE_WRITTEN);
    }

    @Test
    public void testExpandedEar() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testWar.war")
                                   .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                   .addAsResource(SCAN_CONFIG, "META-INF/microprofile-config.properties");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEar.ear")
                                          .addAsModule(war);

        deployApp(ear, c -> c.getApplicationManager().setAutoExpand(true));

        assertRest();
        assertOpenApiDoc();
        assertCache(ear, CacheUsed.CACHE_NOT_USED, CacheWritten.CACHE_NOT_WRITTEN);
    }

    @Test
    public void testWar() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testWar.war")
                                   .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                   .addAsResource(SCAN_CONFIG, "META-INF/microprofile-config.properties");

        deployApp(war);

        assertRest();
        assertOpenApiDoc();
        assertCache(war, CacheUsed.CACHE_USED, CacheWritten.CACHE_NOT_WRITTEN);
    }

    @Test
    public void testExpandedWar() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testWar.war")
                                   .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                   .addAsResource(SCAN_CONFIG, "META-INF/microprofile-config.properties");

        deployApp(war, c -> c.getApplicationManager().setAutoExpand(true));

        assertRest();
        assertOpenApiDoc();
        assertCache(war, CacheUsed.CACHE_NOT_USED, CacheWritten.CACHE_NOT_WRITTEN);
    }

    @Test
    public void testWarLib() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "testJar.jar")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                    .addAsResource(SCAN_CONFIG, "META-INF/microprofile-config.properties");
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testWar.war")
                                   .addAsLibrary(jar);

        deployApp(war);

        assertRest();
        assertOpenApiDoc();
        assertCache(war, CacheUsed.CACHE_USED, CacheWritten.CACHE_NOT_WRITTEN);
    }

    @Test
    public void testExpandedWarLib() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "testJar.jar")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class)
                                    .addAsResource(SCAN_CONFIG, "META-INF/microprofile-config.properties");
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testWar.war")
                                   .addAsLibrary(jar);

        deployApp(war, c -> c.getApplicationManager().setAutoExpand(true));

        assertRest();
        assertOpenApiDoc();
        assertCache(war, CacheUsed.CACHE_NOT_USED, CacheWritten.CACHE_NOT_WRITTEN);
    }
    
    @Test
    public void testLooseWarWithLib() throws Exception {
        copyLoosePackage(server, "looseFiles/warClasses", DeploymentTestResource.class.getPackage().getName());
        copyLoosePackage(server, "looseFiles/libClasses", DeploymentTestResource2.class.getPackage().getName());
        deployLooseApp("looseWar.war.xml", "war");
        
        String response = new HttpRequest(server, "/looseWar.war/test").run(String.class);
        assertEquals("Failed to call test resource", "OK", response);
        String response2 = new HttpRequest(server, "/looseWar.war/test2").run(String.class);
        assertEquals("Failed to call test resource", "OK", response2);

        
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 2, "/test", "/test2");
    }
    
    private void copyLoosePackage(LibertyServer server, String looseDir, String packageName) throws Exception {
        String packageDir = packageName.replace('.', '/');
        RemoteFile remoteDirClasses = LibertyFileManager.createRemoteFile(server.getMachine(), server.getServerRoot() + "/" + looseDir + "/" + packageDir);
//        RemoteFile remoteDirClasses = server.getFileFromLibertyServerRoot(looseDir + "/" + packageDir);
        LocalFile localDirClasses = new LocalFile(LibertyServerUtils.makeJavaCompatible("build/classes/" + packageDir));
        localDirClasses.copyToDest(remoteDirClasses, true, true);
    }

    private Application getConfig(Archive<?> archive) {
        Application appConfig = new Application();
        appConfig.setId("testApp");
        appConfig.setLocation(archive.getName());
        if (archive instanceof WebArchive) {
            appConfig.setType("war");
        } else if (archive instanceof EnterpriseArchive) {
            appConfig.setType("ear");
        }

        return appConfig;
    }

    private void assertRest() throws Exception {
        String response = new HttpRequest(server, "/testWar/test").run(String.class);
        assertEquals("Failed to call test resource", "OK", response);
    }

    private void assertOpenApiDoc() throws Exception {
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(openapiNode, 1, "/test");
    }

    /**
     * Remove and reload the application and assert whether the cache is used or not
     * 
     * @param archive the application archive
     * @param cacheUsed whether the model is loaded from the cache
     * @param cacheWritten whether the generated model is written to the cache
     * @throws Exception
     */
    private void assertCache(Archive<?> archive, CacheUsed cacheUsed, CacheWritten cacheWritten) throws Exception {

        // Remove app from server.xml
        ServerConfiguration deployedConfig = server.getServerConfiguration();
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(initialConfig);
        server.waitForConfigUpdateInLogUsingMark(Collections.emptySet());
        server.waitForStringInLogUsingMark("CWWKZ0009I.*" + getName(archive)); // App stopped

        // Add app to server.xml
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(deployedConfig);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(getName(archive)));

        // Check for appropriate cache messages in trace
        if (cacheUsed == CacheUsed.CACHE_USED) {
            assertThat(server.findStringsInLogsAndTraceUsingMark("Using OpenAPI model loaded from cache"), not(empty()));
            assertThat(server.findStringsInLogsAndTraceUsingMark("Generating OpenAPI model"), is(empty()));
        } else {
            assertThat(server.findStringsInLogsAndTraceUsingMark("Using OpenAPI model loaded from cache"), is(empty()));
            assertThat(server.findStringsInLogsAndTraceUsingMark("Generating OpenAPI model"), not(empty()));
        }

        if (cacheWritten == CacheWritten.CACHE_WRITTEN) {
            assertThat(server.findStringsInLogsAndTraceUsingMark("Cache entry written"), not(empty()));
        } else {
            assertThat(server.findStringsInLogsAndTraceUsingMark("Cache entry written"), is(empty()));
        }
    }

    private enum CacheUsed {
        CACHE_USED,
        CACHE_NOT_USED
    }

    private enum CacheWritten {
        CACHE_WRITTEN,
        CACHE_NOT_WRITTEN
    }

    private void deployApp(Archive<?> archive) throws Exception {
        deployApp(archive, c -> {
        });
    }

    private void deployApp(Archive<?> archive, Consumer<ServerConfiguration> configModifier) throws Exception {
        ShrinkHelper.exportAppToServer(server, archive, SERVER_ONLY, DISABLE_VALIDATION);

        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        config.getApplications().add(getConfig(archive));
        configModifier.accept(config);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(getName(archive)));
    }
    
    private void deployLooseApp(String fileName, String type) throws Exception {
        server.copyFileToLibertyServerRoot("apps", fileName);
        
        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        Application appConfig = new Application();
        appConfig.setId("testApp");
        appConfig.setLocation(fileName);
        appConfig.setType(type);
        config.getApplications().add(appConfig);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(getName(fileName)));
    }

    private String getName(Archive<?> archive) {
        return getName(archive.getName());
    }
    
    private String getName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot != -1) {
            fileName = fileName.substring(0, lastDot);
        }
        return fileName;
    }
}
