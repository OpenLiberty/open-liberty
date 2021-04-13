/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.io.File;
import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPUtils;

public class ShibbolethHelpers {
    private final static Class<?> thisClass = ShibbolethHelpers.class;
    protected ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();
    protected static TestHelpers helpers = new TestHelpers();
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    public static CommonIOTools cioTools = new CommonIOTools();
    public ShibbolethServerVars[] shibServerVarsToUpdate = null;

    public class ShibbolethServerVars {

        String dirName = null;
        String fileName = null;
        HashMap<String, String> varMap = null;

        public ShibbolethServerVars(String inDirName, String inFileName, HashMap<String, String> inVarMap) {
            dirName = inDirName;
            fileName = inFileName;
            varMap = new HashMap<String, String>();
            varMap.putAll(inVarMap);
        }

    }

    public boolean updateToUseExternalLDaPIfInMemoryIsBad(TestServer server) throws Exception {
        return updateToUseExternalLDaPIfInMemoryIsBad(server, null, null);
    }

    public boolean updateToUseExternalLDaPIfInMemoryIsBad(TestServer server, String realPort, String realSSLPort) throws Exception {
        return updateToUseExternalLDaPIfInMemoryIsBad(server, realPort, realSSLPort, null, null);
    }

    public boolean updateToUseExternalLDaPIfInMemoryIsBad(TestServer server, String realPort1, String realSSLPort1, String realPort2, String realSSLPort2) throws Exception {

        String thisMethod = "updateToUseExternalLDaPIfInMemoryIsBad";
        msgUtils.printMethodName(thisMethod);

        boolean useExternalLDAP = false;

        String def_port = System.getProperty("ldap.1.port");
        if (realPort1 != null) {
            def_port = realPort1;
        }
        String def_ssl_port = System.getProperty("ldap.1.ssl.port");
        //        if (realSSLPort != null) {
        //            def_ssl_port = realSSLPort;
        //        }
        String def_host = "localhost";
        String def_url = "ldap://" + def_host + ":" + def_port;
        String def_princ = "uid=admin,ou=system";
        String def_creds = "secret";

        String selected_url = "";
        String selected_princ = "";
        String selected_creds = "";
        HashMap<String, String> propMap = new HashMap<String, String>();

        if (isLDAPHealthy(def_url, def_princ, def_creds)) {
            Log.info(thisClass, thisMethod, "The In Memory LDAP server is up and running");
            // use 2 in memory ldap servers
            propMap.put("shibboleth.ldap.server.name", def_host);
            propMap.put("shibboleth.ldap.server2.name", def_host);
            propMap.put("shibboleth.ldap.server.bindDN", def_princ);
            propMap.put("shibboleth.ldap.server.bindPassword", def_creds);
            propMap.put("shibboleth.ldap.server.port", def_port);
            if (realPort2 != null) {
                propMap.put("shibboleth.ldap.server2.port", realPort2);
            } else {
                propMap.put("shibboleth.ldap.server2.port", def_port);
            }
            propMap.put("shibboleth.ldap.server.ssl.port", def_ssl_port);
            selected_url = def_url;
            selected_princ = def_princ;
            selected_creds = def_creds;
        } else {
            Log.info(thisClass, thisMethod, "The In Memory LDAP server is NOT functioning - try to use the backup");
            // setup to use the backup LDAP - which is a real LDAP server
            propMap.put("shibboleth.ldap.server.name", LDAPUtils.LDAP_SERVER_4_NAME);
            propMap.put("shibboleth.ldap.server2.name", LDAPUtils.LDAP_SERVER_8_NAME);
            propMap.put("shibboleth.ldap.server.bindDN", LDAPUtils.LDAP_SERVER_4_BINDDN);
            propMap.put("shibboleth.ldap.server.bindPassword", LDAPUtils.LDAP_SERVER_4_BINDPWD);
            propMap.put("shibboleth.ldap.server.port", LDAPUtils.LDAP_SERVER_4_PORT);
            propMap.put("shibboleth.ldap.server2.port", LDAPUtils.LDAP_SERVER_8_PORT);
            propMap.put("shibboleth.ldap.server.ssl.port", LDAPUtils.LDAP_SERVER_4_SSL_PORT);
            selected_url = "ldap://" + LDAPUtils.LDAP_SERVER_4_NAME + ":" + LDAPUtils.LDAP_SERVER_4_PORT;
            selected_princ = LDAPUtils.LDAP_SERVER_4_BINDDN;
            selected_creds = LDAPUtils.LDAP_SERVER_4_BINDPWD;
            useExternalLDAP = true;
        }
        bootstrapUtils.writeBootstrapProperties(server, propMap);
        if (isLDAPHealthy(selected_url, selected_princ, selected_creds)) {
            Log.info(thisClass, thisMethod, "The chosen LDAP server appears to be healthy");
        } else {
            Log.info(thisClass, thisMethod, "The selected LDAP server still does NOT appear to be healthy - terminating tests - review logs");
            throw new Exception("Could not find a working LDAP server - terminating test exection");
        }
        return useExternalLDAP;
    }

    public boolean isLDAPHealthy(String url, String principal, String creds) throws Exception {
        String thisMethod = "isLocalLDAPHealthy";

        Hashtable<String, String> environment = new Hashtable<String, String>();

        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, url);
        environment.put(Context.SECURITY_AUTHENTICATION, "simple");
        environment.put(Context.SECURITY_PRINCIPAL, principal);
        environment.put(Context.SECURITY_CREDENTIALS, creds);

        try {
            DirContext context = new InitialDirContext(environment);
            Log.info(thisClass, thisMethod, "Connected..");
            Log.info(thisClass, thisMethod, context.getEnvironment().toString());
            context.close();
            return true;
        } catch (AuthenticationNotSupportedException exception) {
            Log.info(thisClass, thisMethod, "The authentication is not supported by the server");
            return false;
        }

        catch (AuthenticationException exception) {

            Log.info(thisClass, thisMethod, "Incorrect password or username");
            return false;
        }

        catch (NamingException exception) {
            Log.info(thisClass, thisMethod, "Error when trying to create the context");
            return false;
        }

        catch (Exception exception) {
            Log.error(thisClass, thisMethod, exception, "Exception occurred in " + thisMethod);
            Log.info(thisClass, thisMethod, "Error trying to connect to the LDAP server");
            return false;
        }

    }

    /***
     * Set Shibboleth home
     * Tell Shibboleth where to log
     * Fix/workaround issues with the JDK by calling fixShibbolethJDKSettings
     *
     * @throws Exception
     */
    public void setShibbolethPropertiesForTestMachine(TestServer server) throws Exception {

        String thisMethod = "setShibbolethPropertiesForTestMachine";
        String curDir = getShibbolethHome();
        Log.info(thisClass, thisMethod, "Current Dir: " + curDir);
        bootstrapUtils.writeBootstrapProperty(server, "idp.home", curDir);
        String logLoc = server.getServer().getLogsRoot().replace("\\", "/");
        bootstrapUtils.writeBootstrapProperty(server, "was.idp.logs", logLoc);
        fixShibbolethJDKSettings(server, curDir);
        //fixShibbolethJvmOptions(server);
        chooseIdpWarVersion(server);

    }

    /**
     * Fix/workaround issued due to differences between IBM's and Oracle's JDKs
     * 1) Shibboleth uses a secret key to protect cookies - IBM secret keys can not be processed by the Oracle JDK (and
     * vice/versa)
     * generate the secret key on the test machine before the server starts
     * 2) IBM and Oracle JDKs reference different apache classes for parsing - fix references before starting the server
     * 3) Set unrestricted policy
     *
     * @param curDir
     * @throws Exception
     */
    public void fixShibbolethJDKSettings(TestServer server, String curDir) throws Exception {

        String thisMethod = "fixShibbolethJDKSettings";

        final String IBMjdkBuilderAttribute = "org.apache.xerces.util.SecurityManager";
        final String ORACLEjdkBuilderAttribute = "com.sun.org.apache.xerces.internal.util.SecurityManager";

        // fix/generate secret key in sealer keystore
        String keyToolCmd = server.getServer().getMachineJavaJDK() + "/bin/keytool";
        Log.info(thisClass, thisMethod, "Keytool is: " + keyToolCmd);
        try {
            String sealerFileName = curDir + "/credentials/sealer.jks";
            File sealerFile = new File(sealerFileName);
            // The sealer file should not exist on a clean test machine, but will exist after 1 test class is executed
            // subsequent test classes will see an error that it can't create secret key "secret1" because it already exists
            // we can live with that, but, if someone accidently checks in a copy of the file, we could end up
            // running into the problem that we're trying to fix (non-creator jdk trying to open the file)
            if (sealerFile.exists()) {
                Log.info(thisClass, thisMethod, "Deleting the sealer.jks file: " + sealerFileName);
                sealerFile.delete();
            }
            String cmd = keyToolCmd + " -genseckey -alias secret1 -keyalg AES -keysize 128 -storetype jceks -keypass password -storepass password -keystore " + sealerFileName;
            helpers.execCmd(cmd, thisMethod);
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }

        // Fix apache instance reference
        Provider[] providers = Security.getProviders();
        boolean found = false;
        for (Provider provider : providers) {
            String pName = provider.getName();
            Log.info(thisClass, thisMethod, "Provider: " + pName);
            if (pName.startsWith("IBMJSSE")) {
                found = true;
            }
        }
        // the apache parser is different depending on the JDK, set a variable that Shibboleth can load to get the correct apache
        if (found) {
            // copy IBM value in global
            Log.info(thisClass, thisMethod, "Setting Apache instance for IBM: " + IBMjdkBuilderAttribute);
            bootstrapUtils.writeBootstrapProperty(server, "jdkBuilderAttribute", IBMjdkBuilderAttribute);
        } else {
            Log.info(thisClass, thisMethod, "Setting Apache instance for Oracle: " + ORACLEjdkBuilderAttribute);
            bootstrapUtils.writeBootstrapProperty(server, "jdkBuilderAttribute", ORACLEjdkBuilderAttribute);
        }

        // enable unrestricted policy
        Security.setProperty("crypto.policy", "unlimited");
    }

    /**
     * Update the files in the SP tree with the real hostname/port of the Shibboleth server.
     * We'll start the Shibboleth server, but NOT start the Shibboleth app (we'll just start an empty Liberty server) - we'll
     * be able to get it's port. Then, we can start the SP.
     *
     * @param spServer
     * @param idpServer
     * @throws Exception
     */
    public void fixShibbolethInfoinSPServer(TestServer spServer, TestServer idpServer) throws Exception {

        String thisMethod = "fixShibbolethInfoinSPServer";
        msgUtils.printMethodName(thisMethod);

        String spServerHome = spServer.getServerFileLoc();
        Log.info(thisClass, thisMethod, "SP ServerConfigPath: " + spServerHome);
        String idpServerHome = idpServer.getServerFileLoc();
        Log.info(thisClass, thisMethod, "IDP ServerConfigPath: " + idpServerHome);
        updateConfigFiles(TestHelpers.getFileList_endsWith(spServerHome + "/imports/localhost", ".orig"), spServer, idpServer);
        updateConfigFiles(TestHelpers.getFileList_endsWith(spServerHome + "/localhost", ".orig"), spServer, idpServer);

        // if default idpMetadata.xml file exists, copy the updated version to serversettings/SAMLServerFiles/localhost/idpMetadata.xml
        if (LibertyFileManager.libertyFileExists(spServer.getServer().getMachine(), spServerHome + "/localhost/idpMetadata.xml")) {
            String toDir = new File(".").getAbsoluteFile().getCanonicalPath().replace("\\", "/") + "/lib/LibertyFATTestFiles/serversettings/SAMLServerFiles/localhost";
            LibertyFileManager.copyFileIntoLiberty(spServer.getServer().getMachine(), toDir, spServerHome + "/localhost/idpMetadata.xml");
        }

    }

    public void updateConfigFiles(File[] spFilesNeedingUpdates, TestServer spServer, TestServer idpServer) throws Exception {

        String thisMethod = "updateConfigFiles";
        msgUtils.printMethodName(thisMethod);

        if (spFilesNeedingUpdates == null) {
            return;
        }
        for (File file : spFilesNeedingUpdates) {
            String filename = file.getCanonicalPath();
            String newFilename = filename.replace(".orig", "");
            Log.info(thisClass, thisMethod, "Copying file: " + filename + " to: " + newFilename + " and update hostname and port info");
            Map<String, String> replaceVals = new HashMap<String, String>();
            replaceVals.put("xxx_IdpHostname_xxx", idpServer.getServerCanonicalHostname());
            replaceVals.put("xxx_IdpPort_xxx", getServerHttpPort(idpServer));
            replaceVals.put("xxx_IdpSecurePort_xxx", getServerHttpsPort(idpServer));
            replaceVals.put("xxx_SpHostname_xxx", spServer.getServerCanonicalHostname());
            replaceVals.put("xxx_SpPort_xxx", getServerHttpPort(spServer));
            replaceVals.put("xxx_SpSecurePort_xxx", getServerHttpsPort(spServer));
            cioTools.replaceStringsInFile(filename, replaceVals, newFilename);

        }
    }

    public void updateConfigFilesSecondTime(File[] spFilesNeedingUpdates, TestServer spServer, TestServer idpServer) throws Exception {

        String thisMethod = "updateConfigFilesSecondTime";
        msgUtils.printMethodName(thisMethod);

        if (spFilesNeedingUpdates == null) {
            return;
        }
        for (File file : spFilesNeedingUpdates) {
            String filename = file.getCanonicalPath();
            String newFilename = filename.replace(".orig", "");
            Log.info(thisClass, thisMethod, "Updating file: " + newFilename + " with second SP hostname and port info");
            Map<String, String> replaceVals = new HashMap<String, String>();
            replaceVals.put("xxx_IdpHostname2_xxx", idpServer.getServerCanonicalHostname());
            replaceVals.put("xxx_IdpPort2_xxx", getServerHttpPort(idpServer));
            replaceVals.put("xxx_IdpSecurePort2_xxx", getServerHttpsPort(idpServer));
            replaceVals.put("xxx_SpHostname2_xxx", spServer.getServerCanonicalHostname());
            replaceVals.put("xxx_SpPort2_xxx", getServerHttpPort(spServer));
            replaceVals.put("xxx_SpSecurePort2_xxx", getServerHttpsPort(spServer));
            cioTools.replaceStringsInFile(newFilename, replaceVals, null);

        }

    }

    /**
     * We can get the most accurate port info by using the getServerHttpPort/getServerHttpsPort methods, but, we may need
     * the port before those are accurately set, so, use the default port methods in that case
     *
     * @param server
     * @return
     * @throws Exception
     */
    public String getServerHttpPort(TestServer server) throws Exception {

        String port = null;
        if (server.getServerHttpPort() == null) {
            port = server.getHttpDefaultPort().toString();
        } else {
            port = server.getServerHttpPort().toString();

        }
        return port;
    }

    /**
     * We can get the most accurate port info by using the getServerHttpPort/getServerHttpsPort methods, but, we may need
     * the port before those are accurately set, so, use the default port methods in that case
     *
     * @param server
     * @return
     * @throws Exception
     */
    public String getServerHttpsPort(TestServer server) throws Exception {

        String port = null;
        if (server.getServerHttpsPort() == null) {
            port = server.getHttpDefaultSecurePort().toString();
        } else {
            port = server.getServerHttpsPort().toString();

        }
        return port;
    }

    /**
     * Update the config files within Shibboleth with the real hostname/port of the Liberty SP.
     * We've already started the Shibboleth server, but did NOT start the Shibboleth app. We'll update these files, then
     * we can start the app
     *
     * @param spServer
     * @param idpServer
     * @throws Exception
     */
    public void fixSPInfoInShibbolethServer(TestServer spServer, TestServer idpServer) throws Exception {

        String thisMethod = "fixSPInfoInShibbolethServer";
        msgUtils.printMethodName(thisMethod);

        String shibbolethHome = getShibbolethHome();
        String confHome = shibbolethHome + "/conf";
        String metadataHome = shibbolethHome + "/metadata";

        updateConfigFiles(TestHelpers.getFileList_endsWith(confHome, ".orig"), spServer, idpServer);
        updateConfigFiles(TestHelpers.getFileList_endsWith(metadataHome, ".orig"), spServer, idpServer);

    }

    public void fixSecondSPInfoInShibbolethServer(TestServer spServer, TestServer idpServer) throws Exception {

        String thisMethod = "fixSecondSPInfoInShibbolethServer";
        msgUtils.printMethodName(thisMethod);

        String shibbolethHome = getShibbolethHome();
        String confHome = shibbolethHome + "/conf";
        String metadataHome = shibbolethHome + "/metadata";

        updateConfigFilesSecondTime(TestHelpers.getFileList_endsWith(confHome, ".orig"), spServer, idpServer);
        updateConfigFilesSecondTime(TestHelpers.getFileList_endsWith(metadataHome, ".orig"), spServer, idpServer);

    }

    public void fixMiscVarsInShibbolethServer(TestServer idpServer, ShibbolethServerVars[] shibVars) throws Exception {

        String thisMethod = "fixMiscVarsInShibbolethServer";
        msgUtils.printMethodName(thisMethod);

        String shibbolethHome = getShibbolethHome();
        for (ShibbolethServerVars s : shibVars) {
            String filename = shibbolethHome + "/" + s.dirName + "/" + s.fileName;
            // third parm is null and will result in the specified file to be updated instead of updated on the file copy
            cioTools.replaceStringsInFile(filename, s.varMap, null);
        }
    }

    /**
     * As we add new variables, we MUST update this method for a default value for those variables
     *
     * @param idpServer
     * @throws Exception
     */
    public void fixVarsInShibbolethServerWithDefaultValues(TestServer idpServer) throws Exception {

        String thisMethod = "fixVarsInShibbolethServerWithDefaultValues";
        msgUtils.printMethodName(thisMethod);

        // Update properties in idp.properties
        HashMap<String, String> replaceVarMap = new HashMap<String, String>();
        replaceVarMap.put("xxx_IdpSessionTimeout_xxx", "PT60M");
        replaceVarMap.put("xxx_IdpCookieMaxAge_xxx", "31536000");

        String shibbolethHome = getShibbolethHome();
        String confHome = shibbolethHome + "/conf";
        // third parm is null and will result in the specified file to be updated instead of updated on the file copy
        cioTools.replaceStringsInFile(confHome + "/idp.properties", replaceVarMap, null);

        //      String metadataHome = shibbolethHome + "/metadata";

    }

    public void startShibbolethApp(TestServer idpServer) throws Exception {

        LibertyServer theServer = idpServer.getServer();
        theServer.copyFileToLibertyServerRoot(theServer.getServerRoot() + "/test-apps", "dropins", "idp.war");
        theServer.validateAppLoaded("idp");

    }

    public String getShibbolethHome() throws Exception {
        if (System.getProperty("java.specification.version").matches("1\\.[789]")) {
            return new File(".").getAbsoluteFile().getCanonicalPath().replace("\\", "/") + "/shibboleth-idp/3.3.1";
        } else {
            return new File(".").getAbsoluteFile().getCanonicalPath().replace("\\", "/") + "/shibboleth-idp/4.1.0";
        }

    }

    public void fixShibbolethJvmOptions(TestServer server) throws Exception {

        if (!System.getProperty("java.specification.version").matches("1\\.[78]")) {
            bootstrapUtils.writeJvmOptionProperty(server, "--add-opens", "java.xml/com.sun.org.apache.xerces.internal.util=ALL-UNNAMED");
        }

    }

    public void chooseIdpWarVersion(TestServer idpServer) throws Exception {

        String thisMethod = "chooseIdpWarVersion";
        LibertyServer theServer = idpServer.getServer();

        // copy the appropriate version of the idp.war file
        if (System.getProperty("java.specification.version").matches("1\\.[789]")) {
            Log.info(thisClass, thisMethod, "################## Copying the 3.1.1 version of Shibbolet ##################h");
            LibertyFileManager.copyFileIntoLiberty(theServer.getMachine(), theServer.getServerRoot() + "/test-apps", "idp.war", theServer.getServerRoot() + "/test-apps/idp-war-3.3.1.war");
        } else {
            Log.info(thisClass, thisMethod, "################## Copying the 4.1.0 version of Shibboleth ##################");
            LibertyFileManager.copyFileIntoLiberty(theServer.getMachine(), theServer.getServerRoot() + "/test-apps", "idp.war", theServer.getServerRoot() + "/test-apps/idp-war-4.1.0.war");
        }

    }

}