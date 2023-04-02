/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel;

import java.util.EnumMap;

import org.junit.Test;

import com.ibm.ws.javaee.dd.PlatformVersion;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.jsf.FacesConfig;
import com.ibm.ws.javaee.dd.permissions.PermissionsConfig;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.ddmodel.DDParser.VersionData;
import com.ibm.ws.javaee.ddmodel.app.ApplicationDDParser;
import com.ibm.ws.javaee.ddmodel.bval.ValidationConfigDDParser;
import com.ibm.ws.javaee.ddmodel.client.ApplicationClientDDParser;
import com.ibm.ws.javaee.ddmodel.ejb.EJBJarDDParser;
import com.ibm.ws.javaee.ddmodel.jsf.FacesConfigDDParser;
import com.ibm.ws.javaee.ddmodel.web.WebAppDDParser;
import com.ibm.ws.javaee.ddmodel.web.WebFragmentDDParser;
import com.ibm.ws.javaee.ddmodel.permissions.PermissionsConfigDDParser;

public class DDParserSpecList implements PlatformVersion {

    public static enum DDType {
        DD_APPLICATION("Application", Application.DD_SHORT_NAME),
        DD_APPLICATION_CLIENT("Application client", ApplicationClient.DD_SHORT_NAME),
        DD_CONNECTOR("Connector", "ra.xml"), // Not visible
        DD_EJB("EJB jar", EJBJar.DD_SHORT_NAME),   
        DD_FACES("Faces Config", FacesConfig.DD_SHORT_NAME),
        DD_PERMISSIONS("Permissions Config", PermissionsConfig.DD_SHORT_NAME),
        DD_VALIDATION("Validation Config", ValidationConfig.DD_SHORT_NAME),
        DD_WEB("Web Module", WebApp.DD_SHORT_NAME),
        DD_WEB_FRAGMENT("Web Fragment", WebFragment.DD_SHORT_NAME),
        DD_WEB_SERVICES("Web Services", "webservices.xml"); // Not visible

        private DDType(String shortName, String resource) {
            this.shortName = shortName;
            this.resource = resource;
        }

        public final String shortName;
        
        public String getShortName() {
            return shortName;
        }

        public final String resource;
        
        public String getResource() {
            return resource;
        }
    }

    public static class DDInfo {
        public final DDType type;
        public final VersionData[] versions;
        public final int maxTolerated;
        public final int maxImplemented;

        public DDInfo(DDType type, DDParser.VersionData[] versions, int maxTolerated, int maxImplemented) {
            this.type = type;
            this.versions = versions;
            this.maxTolerated = maxTolerated;
            this.maxImplemented = maxImplemented;
        }
    }

    // JCA has an entirely different parser implementation which
    // is not visible from here.

    public static final String jca10DTD = "http://java.sun.com/dtd/connector_1_0.dtd";
    public static final String jca15NamespaceURI = "http://java.sun.com/xml/ns/j2ee";
    public static final String jca16NamespaceURI = "http://java.sun.com/xml/ns/javaee";
    public static final String jca17NamespaceURI = "http://xmlns.jcp.org/xml/ns/javaee";
    public static final String connectors20NamespaceURI = "https://jakarta.ee/xml/ns/jakartaee";     
        
    public static final VersionData[] CONNECTOR_DATA = new VersionData[] {
            new VersionData("1.0", jca10DTD, null, 10, VERSION_1_3_INT),
            new VersionData("1.5", null, jca15NamespaceURI, 15, VERSION_1_4_INT),
            new VersionData("1.6", null, jca15NamespaceURI, 16, VERSION_6_0_INT),
            new VersionData("1.7", null, jca17NamespaceURI, 17, VERSION_7_0_INT),
            new VersionData("2.0", null, connectors20NamespaceURI, 20, VERSION_9_0_INT),
            new VersionData("2.1", null, connectors20NamespaceURI, 21, VERSION_10_0_INT),
    };
    
    // JCA 1.0 - J2EE Version 1.3
    // JCA 1.5 - J2EE Version 1.4
    // JCA 1.6 - Java EE Version 6
    // JCA 1.7 - Java EE Version 7?
    // JCA 2.0 - Jakarta 9
    // JCA 2.1 - Jakarta 10    
    
    // Webservices is also not visible from here.

    public static VersionData[] WEBSERVICES_DATA = {
            new VersionData("1.1", null, DDParser.NAMESPACE_SUN_J2EE, 11, 14),
            new VersionData("1.2", null, DDParser.NAMESPACE_SUN_JAVAEE, 12, 50),
            new VersionData("1.3", null, DDParser.NAMESPACE_SUN_JAVAEE, 13, 60),
            new VersionData("1.4", null, DDParser.NAMESPACE_JCP_JAVAEE, 14, 70),
            new VersionData("2.0", null, DDParser.NAMESPACE_JAKARTA, 20, 90),
            // No new data for Jakarta 10
        };

    public static final EnumMap<DDType, DDInfo> DD_INFO;
    
    static {
        EnumMap<DDType, DDInfo> ddInfo = new EnumMap<DDType, DDInfo>(DDType.class);

        ddInfo.put(DDType.DD_APPLICATION, new DDInfo(DDType.DD_APPLICATION,
                ApplicationDDParser.VERSION_DATA,
                ApplicationDDParser.getMaxTolerated(),
                ApplicationDDParser.getMaxImplemented()));

        ddInfo.put(DDType.DD_APPLICATION, new DDInfo(DDType.DD_APPLICATION,
                ApplicationDDParser.VERSION_DATA,
                ApplicationDDParser.getMaxTolerated(),
                ApplicationDDParser.getMaxImplemented()));

        ddInfo.put(DDType.DD_APPLICATION_CLIENT, new DDInfo(DDType.DD_APPLICATION_CLIENT,
                ApplicationClientDDParser.VERSION_DATA,
                ApplicationClientDDParser.getMaxTolerated(),
                ApplicationClientDDParser.getMaxImplemented()));

        ddInfo.put(DDType.DD_CONNECTOR, new DDInfo(DDType.DD_CONNECTOR, CONNECTOR_DATA, 21, 20));

        ddInfo.put(DDType.DD_EJB, new DDInfo(DDType.DD_EJB,
                EJBJarDDParser.VERSION_DATA,
                EJBJarDDParser.getMaxTolerated(),
                EJBJarDDParser.getMaxImplemented() ) );

        ddInfo.put(DDType.DD_FACES, new DDInfo(DDType.DD_FACES,
                FacesConfigDDParser.VERSION_DATA,
                FacesConfigDDParser.getMaxTolerated(),
                FacesConfigDDParser.getMaxImplemented() ) );

        ddInfo.put(DDType.DD_PERMISSIONS, new DDInfo(DDType.DD_PERMISSIONS,
                PermissionsConfigDDParser.VERSION_DATA,
                PermissionsConfigDDParser.getMaxTolerated(),
                PermissionsConfigDDParser.getMaxImplemented() ) );

        ddInfo.put(DDType.DD_VALIDATION, new DDInfo(DDType.DD_VALIDATION,
                ValidationConfigDDParser.VERSION_DATA,
                ValidationConfigDDParser.getMaxTolerated(),
                ValidationConfigDDParser.getMaxImplemented() ) );

        ddInfo.put(DDType.DD_WEB, new DDInfo(DDType.DD_WEB,
                WebAppDDParser.VERSION_DATA,
                WebAppDDParser.getMaxTolerated(),
                WebAppDDParser.getMaxImplemented() ) );

        ddInfo.put(DDType.DD_WEB_FRAGMENT, new DDInfo(DDType.DD_WEB_FRAGMENT,
                WebFragmentDDParser.VERSION_DATA,
                WebFragmentDDParser.getMaxTolerated(),
                WebFragmentDDParser.getMaxImplemented() ) );

        ddInfo.put(DDType.DD_WEB_SERVICES, new DDInfo(DDType.DD_WEB_SERVICES,
                WEBSERVICES_DATA, 20, 20));

        
        DD_INFO = ddInfo;
    }
    
    public static EnumMap<DDType, DDInfo> getDDInfo() {
        return DD_INFO;
    }

    public static void println(String text) {
        System.out.println(text);
    }

    // 44 to match the usual length of lines

    public static final String BIG_DASHES =
            "================================================================";
    public static final String SMALL_DASHES =
            "----------------------------------------------------------------";        

    public static final String SPACES =
            "                                        ";
    public static final int MAX_SPACES = 40;

    public static final boolean PAD_RIGHT = true;
    
    public static String fill(String text, int width) {
        return fill(text, width, PAD_RIGHT);
    }

    public static String fill(String text, int width, boolean padRight) {
        if ( width > MAX_SPACES ) {
            width = MAX_SPACES;
        }

        int textLen; 
        if ( (textLen = text.length()) >= width ) {
            return text;
        } else {
            String spaces = SPACES.substring(0, width - textLen);
            if ( padRight ) {
                return text + spaces;
            } else {
                return spaces + text;
            }
        }
    }

    public static String fillDotted(int version, int width) {
        return fill( PlatformVersion.getDottedVersionText(version), width, !PAD_RIGHT);
    }

    public static void main(String[] args) {
        printSummary();                
        printDetail();
    }
    
    public static void printSummary() {
        println("Descriptor Parsing Summary");
        println("Implemented / Tolerated | Resource");
        println(BIG_DASHES);
        
        for ( DDInfo ddInfo : getDDInfo().values() ) {
            DDType ddType = ddInfo.type;

            println(" " + fillDotted(ddInfo.maxImplemented, 4) +
                    " / " + fillDotted(ddInfo.maxTolerated, 4) + 
                    " | " + fill(ddType.resource, 25));
            
            // println("Descriptor [ " + fill(ddType.shortName, 19) + " ] [ " + fill(ddType.resource, 25) + " ]");
            // println("  Implemented / Tolerated" +
            //   " [ " +
            //   fillDotted(ddInfo.maxImplemented, 4) +
            //   " / " +
            //   fillDotted(ddInfo.maxTolerated, 4) +
            //   " ]");
            
        }
        
        println(BIG_DASHES);        
    }
    
    public static void printDetail() {
        println("Descriptor Parsing Detail");
        println(BIG_DASHES);
        
        boolean isFirst = true;

        for ( DDInfo ddInfo : getDDInfo().values() ) {
            DDType ddType = ddInfo.type;

            if ( isFirst ) {
                isFirst = false;
            } else {
                println(SMALL_DASHES);
            }
            
            // Match the usual (schema) width of version data lines.
            println("Descriptor [ " + fill(ddType.shortName, 19) + " ] [ " + fill(ddType.resource, 25) + " ]");
            println("  Implemented [ " + fillDotted(ddInfo.maxImplemented, 4) + " ]");
            println("  Tolerated   [ " + fillDotted(ddInfo.maxTolerated, 4) + " ]");

            for ( VersionData vData : ddInfo.versions ) {
                String line = "[ " + fillDotted(vData.version, 4) + " ] [ " + fillDotted(vData.platformVersion, 4) + " ]";
                if ( vData.publicId != null ) {
                    line += " DTD    [ " + fill(vData.publicId, 35) + " ]";
                } else {
                    line += " Schema [ " + fill(vData.namespace, 35) + " ]";
                }
                println(line);
            }
        }

        println(BIG_DASHES);        
    }
    
    @Test
    public void testSummary() {
        DDParserSpecList.printSummary();
    }
    
    @Test
    public void testDetail() {
        DDParserSpecList.printDetail();
    }
}
