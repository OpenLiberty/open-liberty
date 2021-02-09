/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml20.fat.commonTest;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.TestServer;

import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
public class SAMLTestServer extends TestServer {

    private final Class<?> thisClass = SAMLTestServer.class;
    protected static SAMLCommonTestHelpers helpers = new SAMLCommonTestHelpers();;

    //    protected LibertyServer server = null;
    //protected String origServerXml = null;
    //    protected String serverName = null;
    // protected CommonTestTools cct = new CommonTestTools();
    //    protected SAMLMessageTools msgUtils = new SAMLMessageTools();
    //    protected String lastServer = null;
    //    protected Boolean serverWasRestarted = false;
    //    protected List<String> lastCheckApps = new ArrayList<String>();
    //    protected static String hostName = "localhost";
    //protected String thisServerType = "SP" ;
    //    protected Boolean checkForSecurityReady = true ;
    //    protected Boolean UsingDerby = false ;

    public SAMLTestServer(String requestedServer, String serverXML, String testServerType) {
        super(requestedServer, serverXML);
        //        server = LibertyServerFactory.getLibertyServer(requestedServer);
        //        serverName = requestedServer;
        //        /* handle using a server xml with a name other than server.xml */
        //        if (serverXML != null) {
        //            origServerXml = buildFullServerConfigPath(server, serverXML);
        //        } else {
        //            origServerXml = server.getServerRoot() + File.separator + "server.xml";
        //        }
        //        Log.info(thisClass, "SAMLTestServer-init1", "origServerXml set to: " + origServerXml);
        //
        setServerType(testServerType);

    }

    public SAMLTestServer(String requestedServer, String serverXML, String testServerType, String callbackHandler, String feature) {
        super(requestedServer, serverXML, callbackHandler, feature);
        setServerType(testServerType);

    }

    public SAMLTestServer() {
        super();
        //    	File file = new File("./publish/servers");
        //    	String[] directories = file.list(new FilenameFilter(){
        //    		@Override
        //    		public boolean accept(File current, String name){
        //    			return new File(current, name).isDirectory();
        //    		}
        //    	});
        //    	Log.info(thisClass, "TestServer-init", "server dirs:"+ Arrays.toString(directories));
        //    	for (String aServer: directories){
        //    		server = LibertyServerFactory.getLibertyServer (aServer);
        //    	}
    }

    public List<String> getDefaultStartMessages(String testType) {

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKO0219I.*ssl");
        return startMsgs;
    }

    public List<String> getDefaultTestApps(List<String> testApps, String serverInstance) {

        if (testApps == null) {
            testApps = new ArrayList<String>();
        }
        //        Log.info(thisClass, "getDefaultTestApps", "serverInstance: " + serverInstance);
        if (thisServerType.equals(SAMLConstants.SAML_SERVER_TYPE) && (!serverInstance.contains("wssecurity"))) {
            testApps = getDefaultSAMLTestApps(testApps);
        }

        return testApps;
    }

    public List<String> getDefaultSAMLTestApps(List<String> testApps) {

        Log.info(thisClass, "addDefaultSAMLTestApps", "Adding default SAML test Apps");
        //        Log.info(thisClass, "addDefaultSAMLTestApps", "None at this time");
        testApps.add(SAMLConstants.SAML_DEMO_APP);

        return testApps;
    }

    public void addIDPServerProp(String tfimServerName) throws Exception {
        String thisMethod = "pointToApp";
        // create proprty to point to the correct TFIM IDP server
        String newPropString = "tfimIdpServer=" + tfimServerName;
        // add property to bootstrap.properties
        String bootProps = getServerFileLoc() + "/bootstrap.properties";
        Log.info(thisClass, thisMethod, "tfimIdpProp File: " + bootProps);
        // append to bootstrap.properties
        FileWriter writer = new FileWriter(bootProps, true);
        writer.append(newPropString);
        writer.append(System.getProperty("line.separator"));
        writer.close();

    }

    public void addShibbolethProp(String name, String value) throws Exception {
        String thisMethod = "addShibbolethProp";
        // create proprty to point to the Shibboleth variable
        String newPropString = name + "=" + value;
        //        Properties props = new Properties() ;
        //        props.setProperty("samlCurDir", currentDir);
        // add property to bootstrap.properties
        String bootProps = getServerFileLoc() + "/bootstrap.properties";
        Log.info(thisClass, thisMethod, "bootstrap File: " + bootProps + " Content: " + newPropString);
        // append to bootstrap.properties
        FileWriter writer = new FileWriter(bootProps, true);
        writer.append(newPropString);
        writer.append(System.getProperty("line.separator"));
        writer.close();

    }

    public void copyDefaultSPConfigFiles(String chosenSAMLServer) throws Exception {

        Log.info(thisClass, "copyDefaultSPConfigFiles", "Copying serversettings/SAMLServerFiles/" + chosenSAMLServer + "/idpMetadata.xml to resources/security/");
        Log.info(thisClass, "copyDefaultSPConfigFiles", "Server loc: " + server.getServerConfigurationPath());
        //server.copyFileToLibertyInstallRoot("resources/security/", "serversettings/SAMLServerFiles/" + chosenSAMLServer + "/idpMetadata.xml" ) ;
        server.copyFileToLibertyServerRoot("resources/security/", "serversettings/SAMLServerFiles/" + chosenSAMLServer + "/idpMetadata.xml");

    }

    /**
     * Returns a new file path based on this server's runtime configs/ directory.
     *
     * @param newConfigFileName
     * @return
     */
    public String createRuntimeConfigPath(String newConfigFileName) {
        String method = "createRuntimeConfigPath";
        String serverXmlPath = buildFullServerConfigPath(this.getServer(), newConfigFileName);
        Log.info(thisClass, method, "Config path: " + serverXmlPath);
        return serverXmlPath;
    }

    @Override
    public Integer getHttpDefaultPort() {
        if (thisServerType.equals(SAMLConstants.IDP_SERVER_TYPE)) {
            return Integer.getInteger(SAMLConstants.SYS_PROP_PORT_IDP_HTTP_DEFAULT);
        } else {
            return server.getHttpDefaultPort();
        }
    }

    @Override
    public Integer getHttpDefaultSecurePort() {
        if (thisServerType.equals(SAMLConstants.IDP_SERVER_TYPE)) {
            return Integer.getInteger(SAMLConstants.SYS_PROP_PORT_IDP_HTTPS_DEFAULT);
        } else {
            return server.getHttpDefaultSecurePort();
        }
    }

}
