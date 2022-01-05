/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.image.test.topo;

import static com.ibm.ws.image.test.util.FileUtils.IS_WINDOWS;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.ws.image.test.util.FileUtils;
import com.ibm.ws.image.test.util.ProcessRunner;
import com.ibm.ws.image.test.util.ScriptFilter;
import com.ibm.ws.image.test.util.XMLUtils;

/**
 * Pointer to a server installation.
 * 
 * The home directory is the parent of the "wlp" directory.
 */
public class ServerInstallation {
    public static final String CLASS_NAME = ServerInstallation.class.getSimpleName();

    public static void log(String message) {
        System.out.println(message);
    }

    //

    public ServerInstallation(String homePath) {
        this.homePath = homePath;
    }

    private final String homePath;

    public String getHomePath() {
        return homePath;
    }

    public String getLibPath() {
        return getHomePath() + "/wlp/lib";
    }

    public String getPropertiesPath() {
        return getLibPath() + "/versions/WebSphereApplicationServer.properties";
    }

    public void updateProperties(Properties newProperties) throws IOException {
        FileUtils.updateProperties( getPropertiesPath(), newProperties);
    }

    public String getFeaturesPath() {
        return getLibPath() + "/features";
    }
    
    public String getBinPath() {
        return getHomePath() + "/wlp/bin";
    }

    public String[] getScriptPaths() {
        return ScriptFilter.list( getBinPath() );
    }

    public String getServerScriptPath() {
        return getBinPath() + (IS_WINDOWS ? "server.bat" : "server");
    }
    
    public String getSchemaGenScriptPath() {
        return getBinPath() + (IS_WINDOWS ? "serverSchemaGen.bat" : "serverSchemaGen");
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
        return getBinPath() + '/' + serverName;
    }

    public String getConsolePath(String serverName) {
        return getServerPath() + "/logs/console.log";
    }
    
    public String getMessagesPath(String serverName) {
        return getServerPath() + "/logs/messages.log";
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

    public static String addLocalConnector(String line) {
        if ( !line.contains("</server>") ) {
            return null;
        }

        return "  <featureManager>\n" +
               "    <feature>localConnector-1.0</feature>\n" +
               "  </featureManager>\n" +
               "</server>";
    }

    public int addLocalConnectorToConfig(String serverName) throws IOException {
        return FileUtils.update( getConfigPath(serverName), ServerInstallation::addLocalConnector );
    }

    public int addLocalConnectorToConfig() throws IOException {
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

    public ProcessRunner.Result run(String scriptPath, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add(scriptPath);

        for ( String arg : args ) {
            command.add(arg);
        }

        return ProcessRunner.validate( command, ServerInstallation::adjustEnvironment );            
    }

    public List<String> getVersionInfo() throws IOException, InterruptedException {
        return run( getProductInfoPath(), "version" ).getStdout();
    }    

    public ProcessRunner.Result create() throws InterruptedException, IOException {
        return run( getServerScriptPath(), "create" );
    }

    public ProcessRunner.Result start() throws InterruptedException, IOException {
        return run( getServerScriptPath(), "start" );
    }

    public ProcessRunner.Result stop() throws InterruptedException, IOException {
        return run( getServerScriptPath(), "stop" );
    }
    
    public ProcessRunner.Result schemaGen() throws InterruptedException, IOException {
        return schemaGen(DEFAULT_SERVER_NAME);
    }

    public ProcessRunner.Result schemaGen(String serverName) throws InterruptedException, IOException {
        return run( getSchemaGenScriptPath(), serverName );
    }

    // Remove standard directory settings.  That forces
    // the standard directories to the java temporary directory.
    public static void adjustEnvironment(ProcessBuilder pb) {
        pb.environment().remove("WLP_USER_DIR");
        pb.environment().remove("WLP_INSTALL_DIR");
        pb.environment().remove("WLP_OUTPUT_DIR");
    }

    //
    
    public void installBundles(
            List<String> bundles, String repoPath,
            String javaPath, String tmpPath, String workPath)
        throws IOException, InterruptedException {

        List<String> command = new ArrayList<String>();
        command.add( getFeatureManagerPath() );
        command.add("install");
        command.addAll(bundles);
        command.add("--location=" + repoPath);
        command.add("--when-file-exists=ignore");
        command.add("--offlineOnly");
        command.add("--verbose");
        command.add("--acceptLicense");

        ProcessRunner.validate( command, (ProcessBuilder pb) -> {
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
        throws IOException, InterruptedException {

        List<String> command = new ArrayList<String>();
        command.add( getInstallUtilityPath() );
        command.add("install");
        command.addAll(features);
        command.add("--from=" + repoPath);
        command.add("--verbose");
        command.add("--acceptLicense");

        ProcessRunner.validate( command, (ProcessBuilder pb) -> {
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
    
    public void validate(String expectedVersion, boolean doValidateConfig) throws Exception {
        log("Validating server installation [ " + getHomePath() + " ]");

        validateVersion(expectedVersion);
        validateScripts();

        create();

        addLocalConnectorToConfig();
        if ( doValidateConfig ) {
            validateConfig();
        }

        validateServer();
    }

    //

    public void validateVersion(String version) throws IOException, InterruptedException {
        log("Validating version [ " + version + " ]");
        
        List<String> versionOutput = getVersionInfo();
        for ( String versionLine : versionOutput ) {
            if ( versionLine.contains(version) ) {
                return;
            }
        }
        fail("Version [ " + version + " ] not present for [ " + getHomePath() + " ]");
    }

    //
    
    public void validateScripts() throws Exception {
        log("Validating scripts [ " + getBinPath() + " ]");

        for ( String scriptPath : getScriptPaths() ) {
            run(scriptPath);
        }
    }

    //
    
    public static final String SERVER_STARTED_REGEX = ".*CWWKF0011I.*";
    public static final String SERVER_STOPPED_REGEX = ".*CWWKE0036I.*";

    public static final String FAILURE_REGEX = ".*(FFDC|ERROR|WARNING|[A-z]{5}[0-9]{4}[E|W]).*";

    // CWWKE0953W - Beta release warning. Message appears in early release builds.    
    public static final String BETA_MSG = "CWWKE0953W";

    // CWWKF0014W or CWWKF0015I - The server has the following test or ifixes installed    
    public static final String IBM_ALLOWED_REGEX =
        ".*(CWWKF0014W|CWWKF0015I|" + BETA_MSG + ").*";

    // TRAS4352W - Health Center agent was not found
    // Health Center feature is only supported with the IBM JDK.
    public static final String NON_IBM_ALLOWED_REGEX =    
        ".*(CWWKF0014W|CWWKF0015I|TRAS4352W|" + BETA_MSG + ").*";

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

    public void validateServer() throws Exception {
        String consolePath = getConsolePath();
        String messagesPath = getMessagesPath();

        File console = new File(consolePath);
        File messages = new File(messagesPath);

        FileUtils.ensureNonexistence(console);
        FileUtils.ensureNonexistence(messages);

        start();
        stop();

        if ( !console.exists() ) {
            fail("Console log [ " + consolePath + " ] does not exist");
        }
        if ( !messages.exists() ) {
            fail("Messages log [ " + messagesPath + " ] does not exist");
        }

        List<String> startLines = FileUtils.select(console, SERVER_STARTED_REGEX);
        List<String> stopLines = FileUtils.select(console, SERVER_STOPPED_REGEX);        

        String allowedRegex =
            ProcessRunner.IS_IBM_JVM ? IBM_ALLOWED_REGEX : NON_IBM_ALLOWED_REGEX;
        
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
            consoleFailure = "Disallowed console messages [ " + consolePath + " ]: [ " + consoleDisallowed.size() + " ]";
            log(consoleFailure);
            for ( String line : consoleDisallowed ) {
                log("  [ " + line + " ]");
            }
        } else {
            consoleFailure = null;
        }

        String messagesFailure;
        if ( !messagesDisallowed.isEmpty() ) {
            messagesFailure = "Disallowed console messages [ " + messagesPath + " ]: [ " + messagesDisallowed.size() + " ]";
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
    
    public void validateConfig() throws Exception {
        String configPath = getConfigPath();
        String schemaPath = getSchemaPath();

        log("Validating [ " + configPath + " ] using [ " + schemaPath + " ]");

        String strictSchemaPath = getStrictSchemaPath();

        XMLUtils.validate(configPath, strictSchemaPath);
    }
    
    public static String makeStrict(String line) {
        if ( line.contains("<xsd:anyAttribute processContents=\"skip\">\\W*<\\/xsd:anyAttribute>") ) {
            return null;
        } else {
            return line;
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

    //

    public Map<String, Object> getInstallMap() throws Exception {
        return ServerImages.getInstallMap( getHomePath() );
    }

    public List<String> resolveFeatures(
        List<File> availableFeatures, List<String> features) throws Exception {
        return ServerImages.resolveFeatures( getHomePath(), availableFeatures, features );
    }
}
