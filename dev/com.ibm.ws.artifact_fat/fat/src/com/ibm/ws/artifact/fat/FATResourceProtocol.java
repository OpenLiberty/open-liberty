/*******************************************************************************
 * Copyright (c) 2013,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.Assert;
import org.junit.After;
import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

import com.ibm.websphere.simplicity.config.ClassloadingElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.ShrinkHelper;

/**
 * FAT used to test the ability to configure and to dynamically update
 * the resource protocol configuration.
 */
@Mode(TestMode.LITE)
public class FATResourceProtocol {
    private static final Class<? extends FATResourceProtocol> CLASS = FATResourceProtocol.class;

    public static void info(String methodName, String text, Object value) {
        System.out.println(CLASS.getSimpleName() + "." + methodName + ": " + text + " [ " + value + " ]");
        FATLogging.info(CLASS, methodName, text, value);
    }

    public static void info(String methodName, String text) {
        System.out.println(CLASS.getSimpleName() + "." + methodName + ": " + text);
        FATLogging.info(CLASS, methodName, text);
    }

    //

    public static enum ResourceProtocol {
        JAR(true, "jar"),
        WSJAR(false, "wsjar");

        private ResourceProtocol(boolean isJar, String name) {
            this.isJar = isJar;
            this.name= name;
        }

        private final boolean isJar;

        public boolean getIsJar() {
            return isJar;
        }

        public boolean getIsWsJar() {
            return !isJar;
        }

        private final String name;

        public String getName() {
            return name;
        }
    }
    
    //

    public static class WebArchiveParams {
        public final String warName;
        public final String[] warPackageNames;
        
        public final String rootResourcesPath;
        public final String[] metaInfResourcePaths;
        public final String[] webInfResourcePaths;

        public WebArchiveParams(
                String warName,
                
                String[] warPackageNames,
                
                String rootResourcesPath,
                String[] metaInfResourcePaths,
                String[] webInfResourcePaths) {

            this.warName = warName;

            this.warPackageNames = warPackageNames;

            this.rootResourcesPath = rootResourcesPath;
            this.metaInfResourcePaths = metaInfResourcePaths;
            this.webInfResourcePaths = webInfResourcePaths;
        }

        public WebArchive asWebArchive() {
            String methodName = "asWebArchive";
            info(methodName, "WAR [ " + warName + " ]");
            info(methodName, "  Root Resources [ " + rootResourcesPath + " ]");

            File rootResourcesFile = new File(rootResourcesPath);

            WebArchive webArchive = ShrinkWrap.create(WebArchive.class, warName);

            if ( warPackageNames != null ) {
                for ( String warPackageName : warPackageNames ) {
                    info(methodName, "  Package [ " + warPackageName + " ]");
                    webArchive.addPackages(true, warPackageName);
                }
            } else {
                info(methodName, "  *** NO PACKAGES ***");
            }

            if ( metaInfResourcePaths != null ) {
                File metaInfResourcesFile = new File(rootResourcesPath, "META-INF");
                for ( String metaInfResourcePath : metaInfResourcePaths ) {
                    info(methodName, "  META-INF Resource [ " + metaInfResourcePath + " ]");
                    File metaInfResourceFile = new File(metaInfResourcesFile, metaInfResourcePath);
                    webArchive.addAsManifestResource(metaInfResourceFile, metaInfResourcePath);
                }
            } else {
                info(methodName, "  *** NO META-INF RESOURCES ***");
            }

            if ( webInfResourcePaths != null ) {
                File webInfResourcesFile = new File(rootResourcesPath, "WEB-INF");
                for ( String webInfResourcePath : webInfResourcePaths ) {
                    info(methodName, "  WEB-INF Resource [ " + webInfResourcePath + " ]");
                    File webInfResourceFile = new File(webInfResourcesFile, webInfResourcePath);
                    webArchive.addAsWebInfResource(webInfResourceFile, webInfResourcePath);
                }
            } else {
                info(methodName, "  *** NO WEB-INF RESOURCES ***");
            }

            return webArchive;
        }
    }

    //

    public static final String JARJAR_SERVER_NAME = "com.ibm.ws.artifact.jarjar.binks";
    public static final String DYNAMISM_SERVER_NAME = "com.ibm.ws.artifact.dynamism";

    public static final String JAR_NEEDER_CONTEXT_ROOT = "jarneeder";
    public static final String JAR_NEEDER_SERVLET_NAME = "JarNeederServlet";

    public static final String JAR_NEEDER_WAR_NAME = "jarneeder.war";
    public static final String JAR_NEEDER_PACKAGE_NAME = "com.ibm.ws.artifact.fat.servlet";

    public static final String JAR_NEEDER_ROOT_RESOURCES_PATH =
        "test-applications/" + JAR_NEEDER_WAR_NAME + "/resources";
    public static final String PERMISSIONS_RESOURCE_PATH = "permissions.xml";
        
    public static final String TARGET_RESOURCE_PATH = "classes/myresource.something";

    public static WebArchiveParams JAR_NEEDER_WAR_PARAMS = new WebArchiveParams(
        JAR_NEEDER_WAR_NAME,
        new String[] { JAR_NEEDER_PACKAGE_NAME },
        JAR_NEEDER_ROOT_RESOURCES_PATH,
        new String[] { PERMISSIONS_RESOURCE_PATH },
        new String[] { TARGET_RESOURCE_PATH }
    );

    //

    public LibertyServer getServer(String serverName) throws Exception {
        return LibertyServerFactory.getLibertyServer(serverName);
    }

    public URL getRequestUrl(LibertyServer server) throws MalformedURLException {
        return new URL(
            "http://" + server.getHostname() +
            ":" + server.getHttpDefaultPort() +
            "/" + JAR_NEEDER_CONTEXT_ROOT +
            "/" + JAR_NEEDER_SERVLET_NAME );
    }

    public List<String> getResponse(LibertyServer server, URL requestUrl) throws Exception {
        String methodName = "getResponse";
        
        info(methodName, "ENTER [ " + requestUrl + " ]");
        
        HttpURLConnection urlConnection =
            HttpUtils.getHttpConnection(requestUrl, HttpURLConnection.HTTP_OK, CONN_TIMEOUT );

        List<String> responseLines = new ArrayList<String>();

        try {
            BufferedReader responseReader = HttpUtils.getConnectionStream(urlConnection);

            String line;
            while ( (line = responseReader.readLine()) != null ) {
                responseLines.add(line);
            }

        } finally {
            urlConnection.disconnect();
        }
        
        info(methodName, "Response:");        
        int numLines = responseLines.size();
        for ( int lineNo = 0; lineNo < numLines; lineNo++ ) {
            info(methodName, "  [ " + Integer.toString(lineNo) + " ] [ " + responseLines.get(lineNo) + " ]");
        }
        info(methodName, "RETURN");
        
        return responseLines;
    }
    
    
    public void installWarToServer(LibertyServer server, WebArchive webArchive) throws Exception {
        String methodName = "installWarToServer";
        info(methodName, "Server [ " + server.getServerName() + " ] WAR [ " + webArchive.getName() + " ] ... ");

        ShrinkHelper.exportAppToServer(server, webArchive);

        info(methodName, "Server [ " + server.getServerName() + " ] WAR [ " + webArchive.getName() + " ] ... done");
    }

    public LibertyServer startServer(LibertyServer server) throws Exception {
        String methodName = "startServer";
        info(methodName, "Server [ " + server.getServerName() + " ] ...");

        server.startServer();
        info(methodName, "  HostName [ " + server.getHostname() + " ]");
        info(methodName, "  HttpPort [ " + server.getHttpDefaultPort() + " ]");

        info(methodName, "Server  ... [ " + server.getServerName() + " ] ... done");
        return server;
    }

    public void stopServer(LibertyServer server) throws Exception {
        String methodName = "stopServer";
        info(methodName, "Server ... [ " + server.getServerName() + " ] ...");

        server.stopServer();

        info(methodName, "Server ... [ " + server.getServerName() + " ] ... done");
    }

    //

    public LibertyServer prepareServerAndWar(String serverName, WebArchiveParams webArchiveParams)
        throws Exception {

        String methodName = "prepareServerAndWar";
        info(methodName, "Server [ " + serverName + " ] ...");

        WebArchive webArchive = webArchiveParams.asWebArchive();

        LibertyServer server = getServer(serverName);
        installWarToServer(server, webArchive);
        startServer(server);

        info(methodName, "Server [ " + serverName + " ] ... done");
        return server;
    }

    //

    @Test
    public void testResourceProtocolConfiguration() throws Exception {
        String methodName = "testResourceProtocolConfiguration";
        info(methodName, "ENTER");

        LibertyServer jarJarServer = prepareServerAndWar(JARJAR_SERVER_NAME, JAR_NEEDER_WAR_PARAMS);

        try {
            jarJarServer.addInstalledAppForValidation(JAR_NEEDER_CONTEXT_ROOT);

            validateResourceProtocol(
                jarJarServer, ResourceProtocol.JAR,
                "Configuration override"); 

        } finally {
            stopServer(jarJarServer);
        }

        info(methodName, "RETURN");
    }

    @Test
    public void testResourceProtocolUpdate() throws Exception {
        String methodName = "testResourceProtocolUpdate";
        info(methodName, "ENTER");

        LibertyServer dynamismServer = prepareServerAndWar(DYNAMISM_SERVER_NAME, JAR_NEEDER_WAR_PARAMS);

        try {
            dynamismServer.addInstalledAppForValidation(JAR_NEEDER_CONTEXT_ROOT);

            validateResourceProtocol(
                dynamismServer, ResourceProtocol.WSJAR,
                "Configuration default");

            validateUpdateResourceProtocol(
                dynamismServer, ResourceProtocol.JAR,
                "Change to jar (first time)");
            validateUpdateResourceProtocol(
                dynamismServer, ResourceProtocol.WSJAR,
                "Change to wsjar (first time)");
            validateUpdateResourceProtocol(
                dynamismServer, ResourceProtocol.JAR,
                "Change to jar (second time)");
            validateUpdateResourceProtocol(
                dynamismServer, ResourceProtocol.WSJAR,
                "Change to wsjar (second time)");

        } finally {
            stopServer(dynamismServer);
        }

        info(methodName, "RETURN");
    }

    //

    public void validateUpdateResourceProtocol(
        LibertyServer server, ResourceProtocol resourceProtocol, String message)
        throws Exception {

        updateResourceProtocol(server, resourceProtocol);
        validateResourceProtocol(server, resourceProtocol, message);
    }
    
    public void updateResourceProtocol(
        LibertyServer server, ResourceProtocol resourceProtocol)
        throws Exception {
        
        String methodName = "updateResourceProtocol";

        info(methodName, "New resource protocol [ " + resourceProtocol.getName() + " ]");

        server.setMarkToEndOfLog();

        ServerConfiguration serverConfig = server.getServerConfiguration();
        ClassloadingElement classloadingElement = serverConfig.getClassLoadingElement();
        classloadingElement.setUseJarUrls( resourceProtocol.getIsJar() );

        server.updateServerConfiguration(serverConfig);

        Assert.assertNotNull(
            "Detection of completion mark",
            server.waitForStringInLogUsingMark("CWWKG0017I"));
    }

    //

    public static final int CONN_TIMEOUT = 10;

    private String selectLine(String responseLine, String prefix, String suffix) {
        if ( responseLine.startsWith(prefix) && responseLine.endsWith(suffix) ) {
            return responseLine.substring(prefix.length(), responseLine.length() - suffix.length());
        } else {
            return null;
        }
    }

    public void validateResourceProtocol(
        LibertyServer server, ResourceProtocol expectedProtocol, String message)
        throws Exception {

        String methodName = "validateResourceProtocol";
        info(methodName, "{ " + message + " ] [ " + expectedProtocol.getName() + " ]");

        List<String> responseLines = getResponse( server, getRequestUrl(server) );

        boolean matchAll = false;
        String urlLine = null;
        String protocolLine = null;
        String pathLine = null;

        for ( String responseLine : responseLines ) {
            String selectLine = null;
            
            if ( urlLine == null ) {
                selectLine = selectLine(responseLine, "URL [ ", " ]");
                if ( selectLine != null ) {
                    urlLine = selectLine;
                    info(methodName, "URL [ " + urlLine + " ]");
                }
            }
            
            if ( (selectLine == null) && (protocolLine == null) ) {
                selectLine = selectLine(responseLine, "Protocol [ ", " ]");
                if ( selectLine != null ) {
                    protocolLine = selectLine;
                    info(methodName, "Protocol [ " + protocolLine + " ]");
                }
            }
            
            if ( (selectLine == null) && (pathLine == null) ) {
                selectLine = selectLine(responseLine, "Path [ ", " ]");
                if ( selectLine != null ) {
                    pathLine = selectLine;
                    info(methodName, "Path [ " + pathLine + " ]");
                }
            }

            if ( (urlLine != null) && (protocolLine != null) && (pathLine != null) ) {
                matchAll = true;
                break;
            }
        }

        // Expecting these lines from 'JarNeederServlet.doGet':
        //
        // [ 0 ] [ JarNeederServlet.doGet: ENTER ]
        // [ 1 ] [ URL [ jar:file:/C:/openLiberty/repos-pub/microprofile/open-liberty/dev/build.image/wlp/usr/servers/com.ibm.ws.artifact.jarjar.binks/apps/jarneeder.war!/WEB-INF/classes/myresource.something ] ]
        // [ 2 ] [ Protocol [ jar ] ]
        // [ 3 ] [ Path [ file:/C:/openLiberty/repos-pub/microprofile/open-liberty/dev/build.image/wlp/usr/servers/com.ibm.ws.artifact.jarjar.binks/apps/jarneeder.war!/WEB-INF/classes/myresource.something ] ]
        // [ 4 ] [ Content [ sun.net.www.protocol.jar.JarURLConnection$JarURLInputStream@eb629db5 ] ]
        // [ 5 ] [ JarNeederServlet.doGet: RETURN ]
        //
        // With variations allowed only on the path prefix (the directories above 'wlp',
        // and with the server name according to actual server being used for the test.

        Assert.assertTrue("URL and Protocol and Path were all obtained", matchAll);

        String protocolName = expectedProtocol.getName();

        if ( !protocolLine.equals(protocolName) ) {
            Assert.assertTrue(
                "Protocol [ " + protocolLine + " ] equals [ " + protocolName + " ]",
                protocolLine.equals(protocolName));
        }

        String pathPrefix = "file:";

        if ( !pathLine.startsWith(pathPrefix) ) {
            Assert.assertTrue(
                    "Path [ " + pathLine + " ] starts with [ " + pathPrefix + " ]",
                    pathLine.startsWith(pathPrefix));
        }

        String pathTail =  
            "wlp/usr/servers/" + server.getServerName() +
            "/apps/" + JAR_NEEDER_WAR_NAME +
            "!/WEB-INF/" + TARGET_RESOURCE_PATH;

        if ( !pathLine.endsWith(pathTail) ) {
            Assert.assertTrue(
                "Path [ " + pathLine + " ] ends with [ " + pathTail + " ]",
                pathLine.endsWith(pathTail));
        }
    }
}
