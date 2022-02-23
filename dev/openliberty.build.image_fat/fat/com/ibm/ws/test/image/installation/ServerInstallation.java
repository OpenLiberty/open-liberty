/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.installation;

import static com.ibm.ws.test.image.util.FileUtils.IS_WINDOWS;
import static com.ibm.ws.test.image.util.FileUtils.load;
import static com.ibm.ws.test.image.util.FileUtils.selectMissing;
import static com.ibm.ws.test.image.util.ProcessRunner.StreamCopier;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.junit.Assert;

import com.ibm.ws.test.image.Timeouts;
import com.ibm.ws.test.image.util.FileUtils;
import com.ibm.ws.test.image.util.ProcessRunner;
import com.ibm.ws.test.image.util.ScriptFilter;
import com.ibm.ws.test.image.util.XMLUtils;

/**
 * Pointer to a server installation.
 * 
 * The home directory is the parent of the "wlp" directory.
 */
public class ServerInstallation {
    public static final String CLASS_NAME = ServerInstallation.class.getSimpleName();

    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }

    //

    public ServerInstallation(String name, String homePath) {
        this.name = name;

        this.homePath = homePath;
        
        this.path = this.homePath + "/wlp";
        this.binPath = this.path + "/bin";
        this.libPath = this.path + "/lib";
        
        this.propPath = this.libPath + "/versions/WebSphereApplicationServer.properties";
        this.featuresPath = this.libPath + "/features";
    }

    private final String name;
    
    public String getName() {
        return name;
    }

    private final String homePath;
    
    private final String path;
    private final String binPath;
    private final String libPath;

    private final String featuresPath;
    private final String propPath;

    public String getHomePath() {
        return homePath;
    }

    public String getPath() {
        return path;
    }
    
    public String getBinPath() {
        return binPath;
    }

    public String getLibPath() {
        return libPath;
    }

    public String getFeaturesPath() {
        return featuresPath;
    }
    
    public String getPropertiesPath() {
        return propPath; 
    }

    public void updateProperties(Properties newProperties) throws IOException {
        FileUtils.updateProperties( getPropertiesPath(), newProperties);
    }

    public String[] getScriptNames() {
        return ScriptFilter.list( getBinPath() );
    }

    public String getServerScriptPath() {
        return getBinPath() + '/' + (IS_WINDOWS ? "server.bat" : "server");
    }
    
    public String getSchemaGenScriptPath() {
        return getBinPath() + '/' + (IS_WINDOWS ? "serverSchemaGen.bat" : "serverSchemaGen");
    }

    public String getFeatureManagerPath() {
        return getBinPath() + '/' + (IS_WINDOWS ? "featureManager.bat" : "featureManager");    
    }

    public String getInstallUtilityPath() {
        return getBinPath() + '/' + (IS_WINDOWS ? "installUtility.bat" : "installUtility");
    }

    public String getProductInfoPath() {
        return getBinPath() + '/' + (IS_WINDOWS ? "productInfo.bat" : "productInfo");
    }

    public String getServerPath(String serverName) {
        return getPath() + "/usr/servers/" + serverName;
    }

    public String getLogsPath(String serverName) {
        return getServerPath(serverName) + "/logs";
    }
    
    public String getConsolePath(String serverName) {
        return getLogsPath(serverName) + "/console.log";
    }

    public String getMessagesPath(String serverName) {
        return getLogsPath(serverName) + "/messages.log";
    }
    
    public String getSchemaPath(String serverName) {
        return getServerPath(serverName) + "/schemaGen/server.xsd";
    }

    public String getConfigPath(String serverName) {
        return getServerPath(serverName) + "/server.xml";
    }

    //
    
    public static final String DEFAULT_SERVER_NAME = "defaultServer";

    public String getServerPath() {
        return getServerPath(DEFAULT_SERVER_NAME);
    }
    
    public String getConsolePath() {
        return getConsolePath(DEFAULT_SERVER_NAME);
    }
    
    public String getMessagesPath() {
        return getMessagesPath(DEFAULT_SERVER_NAME);
    }

    public String getConfigPath() {
        return getConfigPath(DEFAULT_SERVER_NAME);
    }

    public String getSchemaPath() {
        return getSchemaPath(DEFAULT_SERVER_NAME);
    }

    //

    private static final String[] LOCAL_CONNECTOR_LINES = new String[] {
            "<feature>localConnector-1.0</feature>",
            "</featureManager>"
    };
    
    public static String[] addLocalConnector(String line, String[] singleton) {
        if ( !line.contains("</featureManager>") ) {
            singleton[0] = line;
            return singleton;
        } else {
            return LOCAL_CONNECTOR_LINES;
        }
    }

    public int addLocalConnectorToConfig(String serverName) throws IOException {
        return FileUtils.update( getConfigPath(serverName), ServerInstallation::addLocalConnector );
    }

    public int addLocalConnectorToDefault() throws IOException {
        return addLocalConnectorToConfig(DEFAULT_SERVER_NAME);
    }    

    //
    
    public static final boolean DO_BACKUP = true;
    public static final boolean DO_RESTORE = false;

    public void backupConfig(String serverName, String backupExt) throws IOException {
        copyConfig(serverName, backupExt, DO_BACKUP);
    }
    
    public void restoreConfig(String serverName, String backupExt) throws IOException {
        copyConfig(serverName, backupExt, DO_RESTORE);
    }
    
    public void copyConfig(String serverName, String copyExt, boolean doBackup) throws IOException {
        String sourcePath = getConfigPath(serverName);
        String targetPath = sourcePath + copyExt;

        if ( !doBackup ) {
            String useSourcePath = sourcePath;
            sourcePath = targetPath;
            targetPath = useSourcePath;
        }

        Files.copy( Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING );
    }
    
    //
    
    public static final long SIMPLE_SCRIPT_TIMEOUT = 10 * Timeouts.NS_IN_SEC;

    public ProcessRunner.Result run(String scriptPath, String... args) throws Exception {
        return run(SIMPLE_SCRIPT_TIMEOUT, scriptPath, args);
    }
    
    public ProcessRunner.Result run(long timeout, String scriptPath, String... args) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(scriptPath);

        for ( String arg : args ) {
            command.add(arg);
        }

        return ProcessRunner.validate( command, timeout, ServerInstallation::adjustEnvironment );            
    }

    public ProcessRunner.Result run(
            long timeout,
            Function<String, Boolean> stdoutSuccess, Function<String, Boolean> stdoutFailure,
            Function<String, Boolean> stderrSuccess, Function<String, Boolean> stderrFailure,
            int expectedRc,
            StreamCopier.TerminationCondition expectedStdoutCondition,
            StreamCopier.TerminationCondition expectedStderrCondition,
            String scriptPath, String... args) throws Exception {

        List<String> command = new ArrayList<String>();
        command.add(scriptPath);

        for ( String arg : args ) {
            command.add(arg);
        }

        return ProcessRunner.validate(
                command,
                timeout,
                stdoutSuccess, stdoutFailure,
                stderrSuccess, stderrFailure,
                expectedRc, expectedStdoutCondition, expectedStderrCondition,
                ServerInstallation::adjustEnvironment );            
    }
    
    public List<String> getVersionInfo() throws Exception {
        return run( getProductInfoPath(), "version" ).getStdout();
    }    

    public static Boolean detectFailure(String line) {
        return Boolean.valueOf( (line != null) && !line.isEmpty() );
    }

    public static Boolean detectDefaultServerCreated(String line) {
        return Boolean.valueOf( line.equals("Server defaultServer created.") );
    }

    public static Boolean detectDefaultServerStarted(String line) {
        return Boolean.valueOf( line.equals("Server defaultServer started.") );
    }

    public static Boolean detectDefaultServerStopped(String line) {
        return Boolean.valueOf( line.equals("Server defaultServer stopped.") );
    }

    public ProcessRunner.Result createDefaultServer() throws Exception {
        return run( Timeouts.CREATE_SERVER_TIMEOUT_NS, getServerScriptPath(), "create" );
    }

    public ProcessRunner.Result start() throws Exception {
        return run(
                Timeouts.START_SERVER_TIMEOUT_NS,
                ServerInstallation::detectDefaultServerStarted, null,
                null, ServerInstallation::detectFailure,
                0,
                StreamCopier.TerminationCondition.SUCCEEDED,
                StreamCopier.TerminationCondition.HALTED,
                getServerScriptPath(), "start" );
    }

    public ProcessRunner.Result stop() throws Exception {
        return run( Timeouts.STOP_SERVER_TIMEOUT_NS, getServerScriptPath(), "stop" );
    }

    public ProcessRunner.Result schemaGen() throws Exception {
        return schemaGen(DEFAULT_SERVER_NAME);
    }

    public ProcessRunner.Result schemaGen(String serverName) throws Exception {
        // TODO: Temporary:
        //
        // SchemaGen requires that the server logs folder
        // already exist.
        //
        // See open liberty issue #20207:
        // https://github.com/OpenLiberty/open-liberty/issues/20207

        (new File(getLogsPath(serverName))).mkdir();
        
        return run( Timeouts.SCHEMAGEN_TIMEOUT_NS, getSchemaGenScriptPath(), serverName );
    }

    public static void adjustEnvironment(ProcessBuilder pb) {
        pb.environment().remove("WLP_USER_DIR");
        pb.environment().remove("WLP_INSTALL_DIR");
        pb.environment().remove("WLP_OUTPUT_DIR");
    }

    //
    
    public void installBundles(
            List<String> bundles, String repoPath,
            String javaPath, String tmpPath, String workPath)
        throws Exception {

        List<String> command = new ArrayList<String>();
        command.add( getFeatureManagerPath() );
        command.add("install");
        command.addAll(bundles);
        command.add("--location=" + repoPath);
        command.add("--when-file-exists=ignore");
        command.add("--offlineOnly");
        command.add("--verbose");
        command.add("--acceptLicense");

        ProcessRunner.validate(
                command,
                Timeouts.INSTALL_BUNDLES_TIMEOUT_NS, 
                (ProcessBuilder pb) -> {
                    Map<String, String> env = pb.environment();
                    env.put("JAVA_HOME", javaPath);
                    env.put("JVM_ARGS", "-Djava.io.tmpdir=" + tmpPath);
                    pb.directory( new File(workPath) );
                } );
    }

    public void installFeatures(
            List<String> features,
            String repoPath,
            String javaPath, String tmpPath, String workPath)
        throws Exception {

        List<String> command = new ArrayList<String>();
        command.add( getInstallUtilityPath() );
        command.add("install");
        command.addAll(features);
        command.add("--from=" + repoPath);
        command.add("--verbose");
        command.add("--acceptLicense");

        ProcessRunner.validate(
                command,
                Timeouts.INSTALL_FEATURES_TIMEOUT_NS,
                (ProcessBuilder pb) -> {
                    Map<String, String> env = pb.environment();
                    env.put("JAVA_HOME", javaPath);
                    env.put("JVM_ARGS", "-Djava.io.tmpdir=" + tmpPath);
                    pb.directory( new File(workPath) );
                } );
    }

    public List<String> getInstalledFeatures() {
        String featuresPath =  getFeaturesPath();
        if ( featuresPath == null ) {
            fail("Features directory [ " + featuresPath + " ] does not exist");
            return null;
        }

        String[] candidates = (new File(featuresPath)).list();
        if ( candidates == null ) {
            fail("Features directory [ " + featuresPath + " ] could not be listed");
            return null;
        }

        // All but one or two of the candidates are features.
        List<String> installedFeatures = new ArrayList<>(candidates.length);

        for ( String name : candidates ) {
            if ( !name.endsWith(".mf") ) {
                continue;
            }
            String feature = name.substring( 0, name.length() - ".mf".length() );
            installedFeatures.add(feature);
        }

        Collections.sort(installedFeatures);

        return installedFeatures;
    }
    
    //
    
    public void validateVersion(String[][] expectations) throws Exception {
        log("Validating product information [ " + getName() + " ]:");
        for ( String[] expectation : expectations ) {
            log("Expect [ " + expectation[0] + " : " + expectation[1] + " ]");
        }

        int numMet = 0;
        boolean[] metExpectation = new boolean[ expectations.length ];
        
        List<String> versionOutput = getVersionInfo();
        for ( String versionLine : versionOutput ) {
            for ( int expectationNo = 0; expectationNo < expectations.length; expectationNo++ ) {
                String[] expectation = expectations[expectationNo];
                if ( versionLine.startsWith( expectation[0] ) &&
                     versionLine.endsWith( expectation[1] ) ) {
                    if ( !metExpectation[expectationNo] ) {
                        numMet++;
                        metExpectation[expectationNo] = true;
                    }
                }
            }
        }

        if ( numMet < expectations.length ) {
            StringBuilder builder = new StringBuilder();
            for ( int expectationNo = 0; expectationNo < expectations.length; expectationNo++ ) {
                String[] expectation = expectations[expectationNo];
                if ( !metExpectation[expectationNo] ) {
                    builder.append("Unmet expectation [ " + expectation[0] + " : " + expectation[1] + " ]\n");
                }
            }
            fail( builder.toString() );
        }
    }

    // Scripts are mostly but not entirely uniform:
//    
//    auditUtility
//    Usage: auditUtility {auditReader|help} [options]
//
//    batchManager
//    Usage: batchManager {help|submit|stop|restart|status|getJobLog|listJobs|purge} [options]
//
//    binaryLog
//    Usage: binaryLog action {serverName | repositoryPath} [options]
//
//    client
//    -- none --
//    
//    ddlGen
//    Usage: ddlGen {generate|help} serverName
//
//    featureUtility
//    Usage: featureUtility {installFeature|installServerFeatures|viewSettings|find|help} [options]
//
//    pluginUtility
//    Usage: pluginUtility action [options]
//    Actions:
//        merge [options]
//        Merges the multiple web server plugin configuration files into a single file.
//        generate [options]
//        This command generates a plugin configuration file for an application
//        server or a cluster of servers. 
//        help [actionName]
//        Print help information for the specified action.
//
//    productInfo
//    Usage: productInfo {help|compare|featureInfo|viewLicenseInfo|viewLicenseAgreement|version|validate} [options]
//
//    securityUtility
//    Usage: securityUtility {encode|createSSLCertificate|createLTPAKeys|tlsProfiler|help} [options]
//
//    server
//    -- none --
//
//    serverSchemaGen
//    Usage: serverSchemaGen server
//
//    springBootUtility
//    Usage: springBootUtility {thin|help} [options]
    
    public void validateScripts() throws Exception {
        String useBinPath = getBinPath();

        log("Validating scripts [ " + getName() + " ]");
        log("Scripts path [ " + useBinPath + " ]");
        for ( String scriptName : getScriptNames() ) {
            log("Validating script [ " + scriptName + " ]");
            run(useBinPath + '/' + scriptName);
        }
    }

    //

    public void validateDefaultServer(
        boolean supportsStartup,
        boolean supportsSchemaGen,
        String... addedAllowed) throws Exception {

        log("Validating default instance [ " + getName() + " ]");
        log("Supports startup [ " + supportsStartup + " ]");
        log("Supports schemaGen [ " + supportsSchemaGen + " ]");
        if ( addedAllowed.length != 0 ) {
            log("Added allowed console messages");
            for ( String added : addedAllowed ) {
                log("Added [ " + added + " ]");
            }
        }

        createDefaultServer();

        if ( supportsStartup ) {
            if ( supportsSchemaGen ) {
                int numUpdates = FileUtils.update(
                    getConfigPath(DEFAULT_SERVER_NAME),
                    ServerInstallation::addLocalConnector );
                Assert.assertEquals("Count of connector updates", numUpdates, 2);
            }

            exerciseDefaultServer(supportsSchemaGen, addedAllowed);
        }
    }

    public static final String SERVER_STARTED_REGEX = ".*CWWKF0011I.*";
    public static final String SERVER_STOPPED_REGEX = ".*CWWKE0036I.*";

    public static final String FAILURE_REGEX = ".*(FFDC|ERROR|WARNING|[A-z]{5}[0-9]{4}[E|W]).*";

    // CWWKE0953W - Beta release warning. Message appears in early release builds.    
    public static final String BETA_MSG = "CWWKE0953W";

    // TRAS4352W - Health Center agent was not found
    // Health Center feature is only supported with the IBM JDK.    
    public static final String NON_IBM_ADDED = "TRAS4352W";
    
    // CWWKF0014W or CWWKF0015I - The server has the following test or ifixes installed
    public static final String ALLOWED_PREFIX = ".*(CWWKF0014W|CWWKF0015I|";

    public static final String ALLOWED_SUFFIX = ").*";

    // public static final String IBM_ALLOWED_REGEX =
    //     ".*(CWWKF0014W|CWWKF0015I|" + BETA_MSG + ").*";

    // public static final String NON_IBM_ALLOWED_REGEX =    
    //     ".*(CWWKF0014W|CWWKF0015I|TRAS4352W|" + BETA_MSG + ").*";

    public static final String getAllowed(String...added) {
        StringBuilder builder = new StringBuilder(ALLOWED_PREFIX);
        if ( !ProcessRunner.IS_IBM_JVM ) {
            builder.append('|');
            builder.append(NON_IBM_ADDED);
        }
        for ( String nextAdded : added ) {
            builder.append('|');
            builder.append(nextAdded);
        }
        builder.append(ALLOWED_SUFFIX);
        return builder.toString();
    }
    
    // Assert that no issues were present in either log after stop.
    // Right now, these are failing because of messages:
    //
    // [WARNING] The TransactionSynchronizationRegistry used to manage
    // persistence contexts is no longer available. Managed persistence
    // contexts will no longer be able to integrate with JTA transactions,
    // and will behave as if  no there is no transaction context at all
    // times until a new TransactionSynchronizationRegistry is available.
    // Applications using managed persistence contexts may not work correctly
    // until a new JTA Transaction services implementation is available.
    //
    // [WARNING] Managed persistence context support is no longer available
    // for use with the Aries Blueprint container.

    public static final boolean DO_VALIDATE = true;
    
    public void exerciseDefaultServer(boolean doValidate, String... addedAllowed) throws Exception {
        log("Exercising default instance [ " + getName() + " ]");
        log("Validate [ " + doValidate + " ]");
        
        String consolePath = getConsolePath();
        String messagesPath = getMessagesPath();

        File console = new File(consolePath);
        File messages = new File(messagesPath);

        FileUtils.ensureNonexistence(console);
        FileUtils.ensureNonexistence(messages);

        // 'stop' is invoked even if 'start' fails with an exception.
        // That is an attempt to prevent orphan server processes.

        try {
            start();

            // "kernel" does not support schema generation.
            if ( doValidate ) {
                validateDefaultConfig();
            }

        } finally {
            stop();
        }

        if ( !console.exists() ) {
            fail("Console log [ " + consolePath + " ] does not exist");
        }
        if ( !messages.exists() ) {
            fail("Messages log [ " + messagesPath + " ] does not exist");
        }

        List<String> startLines = FileUtils.select(console, SERVER_STARTED_REGEX);
        List<String> stopLines = FileUtils.select(console, SERVER_STOPPED_REGEX);        

        String allowedRegex = getAllowed(addedAllowed);

        List<String> consoleDisallowed = FileUtils.selectLines(console, FAILURE_REGEX, allowedRegex);
        List<String> messagesDisallowed = FileUtils.selectLines(messages, FAILURE_REGEX, allowedRegex);

        String startFailure;
        if ( startLines.isEmpty() ) {
            startFailure = "Did not find [ " + SERVER_STARTED_REGEX + " ] in [ " + consolePath + " ]";
        } else if ( startLines.size() > 1 ) {
            startFailure = "Too many [ " + SERVER_STARTED_REGEX + " ] in [ " + consolePath + " ]";            
        } else {
            startFailure = null;
        }
        if ( startFailure != null ) {
            log(startFailure);
        }

        String stopFailure;
        if ( stopLines.isEmpty() ) {
            stopFailure = "Did not find [ " + SERVER_STARTED_REGEX + " ] in [ " + consolePath + " ]";
        } else if ( stopLines.size() > 1 ) {
            stopFailure = "Too many [ " + SERVER_STARTED_REGEX + " ] in [ " + consolePath + " ]";            
        } else {
            stopFailure = null;
        }
        if ( stopFailure != null ) {
            log(stopFailure);
        }

        String consoleFailure;
        if ( !consoleDisallowed.isEmpty() ) {
            consoleFailure =
                    "Disallowed console messages [ " + consolePath + " ]:" +
                    " [ " + consoleDisallowed.size() + " ]: " + 
                    " [ " + consoleDisallowed.get(0) + " ]";
            log(consoleFailure);
            for ( String line : consoleDisallowed ) {
                log("  [ " + line + " ]");
            }
        } else {
            consoleFailure = null;
        }

        String messagesFailure;
        if ( !messagesDisallowed.isEmpty() ) {
            messagesFailure =
                    "Disallowed console messages [ " + messagesPath + " ]:" +
                    " [ " + messagesDisallowed.size() + " ]: " +
                    " [ " + messagesDisallowed.get(0) + " ]";
            log(messagesFailure);
            for ( String line : messagesDisallowed ) {
                log("  [ " + line + " ]");
            }
        } else {
            messagesFailure = null;
        }

        if ( startFailure != null ) {
            fail(startFailure);
        }
        if ( stopFailure != null ) {
            fail(stopFailure);
        }
        if ( consoleFailure != null ) {
            fail(consoleFailure);
        }
        if ( messagesFailure != null ) {
            fail(messagesFailure);
        }
    }

    //
    
    public void validateDefaultConfig() throws Exception {
        log("Validating default configuration [ " + getName() + " ]");

        String configPath = getConfigPath();
        String schemaPath = getSchemaPath();

        log("Configuration [ " + configPath + " ]");
        log("Schema [ " + schemaPath + " ]");

        // The default server configuration is created with
        // an 'ssl' element and with no security features.
        //
        // That schema which is generated for the 
        String compliantConfigPath = getCompliantConfigPath();
        String strictSchemaPath = getStrictSchemaPath();

//        try {
            XMLUtils.validate(compliantConfigPath, strictSchemaPath);
//        } catch ( Exception e ) {
//            try {
//                FileUtils.display(configPath);
//                FileUtils.display(strictSchemaPath);
//            } catch ( Exception nestedException ) {
//                // Ignore
//            }
//            throw e;
//        }
    }

    public static String[] makeStrict(String line, String[] singleton) {
        if ( line.contains("<xsd:anyAttribute processContents=\"skip\">\\W*<\\/xsd:anyAttribute>") ) {
            return null;
        } else {
            singleton[0] = line;
            return singleton;
        }
    }
    
    public String getStrictSchemaPath() throws Exception {
        schemaGen();

        String schemaPath = getSchemaPath();

        int extensionLoc = schemaPath.lastIndexOf('.');
        String strictSchemaPath = schemaPath.substring(0, extensionLoc) + "-strict.xsd";

        FileUtils.ensureNonexistence(strictSchemaPath);
        FileUtils.update(schemaPath, strictSchemaPath, ServerInstallation::makeStrict);

        return strictSchemaPath;
    }

    public static String[] makeCompliant(String line, String[] singleton) {
        // log("Make compliant [ " + line + " ]");
        if ( line.contains("<ssl") && line.endsWith("/>") ) {
            singleton[0] = "<!-- " + line + "-->";
            // log("Compliant [ " + singleton[0] + " ]");
        } else {
            singleton[0] = line;
        }
        return singleton;        
    }

    public String getCompliantConfigPath() throws Exception {
        String configPath = getConfigPath();

        int extensionLoc = configPath.lastIndexOf('.');
        String compliantConfigPath = configPath.substring(0, extensionLoc) + "-compliant.xml";

        FileUtils.ensureNonexistence(compliantConfigPath);
        FileUtils.update(configPath, compliantConfigPath, ServerInstallation::makeCompliant);

        return compliantConfigPath;
    }
    
    //

    public void validateDefaultTemplate(String[] requiredElements) throws Exception {
        log("Validating default template [ " + getName() + " ]");
        
        String wlpPath = getPath();
        File wlpRoot = new File(wlpPath);
        if ( !wlpRoot.exists() ) {
            fail("Installation [ " + wlpPath + " ] does not exist");
        }
        log("Installation [ " + wlpPath + " ] exists");

        String defaultTemplatePath = wlpPath + "/templates/servers/defaultServer/server.xml"; 
        File defaultTemplate = new File(defaultTemplatePath);
        if ( !defaultTemplate.exists() ) {
            fail("Default template [ " + defaultTemplatePath + " ] does not exist");
        }
        log("Default template [ " + defaultTemplatePath + " ] exists");

        log("Required template elements:");
        for ( String element : requiredElements ) {
            log("[ " + element + " ]");
        }

        List<String> actualElements = load(defaultTemplate); // throws IOException
        List<String> missingElements = selectMissing(actualElements, requiredElements);

        if ( (missingElements != null) && !missingElements.isEmpty() ) {
            log("Missing default template elements:");
            for ( String missing : missingElements ) {
                log("  [ " + missing + " ]");
            }

            log("Actual default template elements (features only):");            
            for ( String actual : actualElements ) {
                if ( actual.contains("feature") ) {
                    log("  [ " + actual + " ]");
                }
            }

            fail("Missing default template elements");

        } else {
            log("Default template [ " + defaultTemplatePath + " ] has all required elements");
        }
    }
    
    //
    
    public static final String INSTALL_MAP_PREFIX = "com.ibm.ws.install.map";
    public static final String INSTALL_MAP_SUFFIX = ".jar";

    public static boolean isInstallMap(File parent, String name) {
        return ( name.startsWith(INSTALL_MAP_PREFIX) && name.endsWith(INSTALL_MAP_SUFFIX) );        
    }
    
    public static String getMapVersion(String name) {
        String version = FileUtils.removeEnds(name, INSTALL_MAP_PREFIX, INSTALL_MAP_SUFFIX);
        return ( version.isEmpty() ? null : version );
    }

    private boolean failedInstallMapPath; 
    private String installMapPath;

    public String getInstallMapPath() {
        if ( failedInstallMapPath ) {
            fail("Previous failure to locate install map");
            return null;
        }
        
        if ( installMapPath == null ) {
            try {
                installMapPath = locateInstallMap();
                failedInstallMapPath = false;
            } catch ( Throwable th ) {
                installMapPath = null;
                failedInstallMapPath = true;
                throw th;
            }
        }

        return installMapPath;
    }

    private String locateInstallMap() {
        String useLibPath = getLibPath();
        String[] installMapNames = (new File(useLibPath)).list( ServerInstallation::isInstallMap );
        if ( installMapNames == null ) {
            fail("Failed to list library folder [ " + useLibPath + " ]");
            return null;
        } else if ( installMapNames.length == 0 ) {
            fail("Library folder has no install map jars [ " + useLibPath + " ]");
            return null;            
        }

        String latestMapName = null;
        String latestVersion = null;

        for ( String nextMap : installMapNames ) {
            String nextVersion = getMapVersion(nextMap);

            if ( (nextVersion == null) ||
                 ((latestMapName == null) || nextVersion.compareTo(latestVersion) > 0) ) {
                latestMapName = nextMap;
                latestVersion = nextVersion;
            }

            if ( nextVersion == null ) {
                break; // The null version is highest.
            }
        }

        String latestMapPath = useLibPath + "/" + latestMapName; 
        log("Install map [ " + latestMapPath + " ] [ " + latestVersion + " ]");
        return latestMapPath;
    }

    public static final String INSTALL_MAP_CLASS_NAME = "com.ibm.ws.install.map.InstallMap";
    
    private Map<String, Object> loadInstallMap() throws Exception {
        String installMapPath = getInstallMapPath();
        File installMapJar = new File(installMapPath);
        
        Map<String, Object> installMap = AccessController.doPrivileged(
            new PrivilegedExceptionAction<Map<String, Object>>() {
                @SuppressWarnings({ "unchecked", "resource" })
                @Override
                public Map<String, Object> run() throws Exception {
                    URL installMapURL = installMapJar.toURI().toURL();
                    URL[] installMapURLs = new URL[] { installMapURL };
                    ClassLoader installMapLoader = new URLClassLoader(installMapURLs, null);

                    Class<Map<String, Object>> installMapClass = (Class<Map<String, Object>>)
                        installMapLoader.loadClass(INSTALL_MAP_CLASS_NAME);

                    return installMapClass.newInstance();
                }
            }
        );

        log("install.kernel.init.code [ " + installMap.get("install.kernel.init.code") + " ]");
        log("install.kernel.init.error.message [ " + installMap.get("install.kernel.init.error.message") + " ]");

        return installMap;
    }

    public List<String> resolveFeatures(
            List<File> availableFeatures, List<String> features) throws Exception {

        log("Resolving features");
        for ( String feature : features ) {
            log("  [ " + feature + " ]");
        }
        log("Available features [ " + availableFeatures.size() + " ]");

        Map<String, Object> installMap = loadInstallMap();
        
        installMap.put("install.local.esa", true);
        installMap.put("single.json.file", availableFeatures);
        installMap.put("license.accept", true);

        if ( installMap.put("features.to.resolve", features) != null ) {
            String errorMessage = (String) installMap.get("action.error.message");
            log("Feature resolution failure [ action.error.message ] [ " + errorMessage + " ]");
            fail(errorMessage);
            return null;
        }

        @SuppressWarnings("unchecked")
        Collection<String> resolvedFeatures = (Collection<String>)
            installMap.get("action.result");

        List<String> useResolvedFeatures = new ArrayList<String>();;
        log("Resolved feature: ");
        for ( String featureResolvant : resolvedFeatures ) {
            int firstColon = featureResolvant.indexOf(":");
            int secondColon = featureResolvant.indexOf(":", firstColon + 1);
            String feature = featureResolvant.substring(firstColon + 1, secondColon);
            if ( !feature.startsWith("com.ibm.") ) {
                feature = "com.ibm.websphere.appserver." + feature;
            }
            useResolvedFeatures.add(feature);
            log("  [ " + feature + " ]");
        }

        Collections.sort(useResolvedFeatures);

        return useResolvedFeatures;
    }
}
