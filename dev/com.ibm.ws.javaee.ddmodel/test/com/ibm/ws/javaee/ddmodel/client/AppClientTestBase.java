/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class AppClientTestBase extends DDTestBase {

    @SuppressWarnings("deprecation")
    protected ApplicationClient parse(
        String xml,
        int maxVersion,
        String... expectedMessages) throws Exception {

        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));

                allowing(artifactEntry).getPath();
                will(returnValue('/' + ApplicationClient.DD_NAME));
            }
        });
        
        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);
        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(moduleRoot).adapt(Entry.class);
                will(returnValue(null));
                allowing(moduleRoot).getPhysicalPath();
                will(returnValue("/root/wlp/usr/servers/server1/apps/myClient.jar"));   
                allowing(moduleRoot).getPath();
                will(returnValue("/"));
                allowing(moduleRoot).getEntry(Application.DD_NAME);
                will(returnValue(ddEntry));

                allowing(ddEntry).getRoot();
                will(returnValue(moduleRoot));
                allowing(ddEntry).getPath();
                will(returnValue('/' + ApplicationClient.DD_NAME));
                allowing(ddEntry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
            }
        });
        
        @SuppressWarnings("unchecked")
        ServiceReference<ApplicationClientDDParserVersion> versionRef =
            mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {        
                allowing(versionRef).getProperty(ApplicationClientDDParserVersion.VERSION);
                will(returnValue(maxVersion));
            }
        });

        ApplicationClientEntryAdapter adapter = new ApplicationClientEntryAdapter();
        adapter.setVersion(versionRef);

        Exception boundException;

        try {
            ApplicationClient appClient = adapter.adapt(moduleRoot, rootOverlay, artifactEntry, ddEntry);
            if ( (expectedMessages != null) && (expectedMessages.length != 0) ) {
                throw new Exception("Expected exception text [ " + Arrays.toString(expectedMessages) + " ]");
            }
            return appClient;

        } catch ( UnableToAdaptException e ) {
            Throwable cause = e.getCause();
            if ( cause instanceof Exception ) {
                boundException = (Exception) cause;
            } else {
                boundException = e;
            }
        }

        if ( (expectedMessages != null) && (expectedMessages.length != 0) ) {
            String message = boundException.getMessage();
            if ( message != null ) {
                for ( String expected : expectedMessages ) {
                    if ( message.contains(expected) ) {
                        return null;
                    }
                }
            }
        }
        throw boundException;
    }

    // 1.2
    // 1.3
    //
    // 1.4, http://java.sun.com/xml/ns/j2ee
    // 5,   http://java.sun.com/xml/ns/javaee
    // 6,   http://java.sun.com/xml/ns/javaee
    // 7,   http://xmlns.jcp.org/xml/ns/javaee
    // 8,   http://xmlns.jcp.org/xml/ns/javaee
    //
    // 9,   https://jakarta.ee/xml/ns/jakartaee    
    
    protected static String appClient12Head =
        "<!DOCTYPE application-client PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.2//EN\"" + 
               " \"http://java.sun.com/j2ee/dtds/application_client_1_2.dtd\">" +
               "<application-client>";

    protected static String appClient13Head =
        "<!DOCTYPE application-client PUBLIC" +
                " \"-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.3//EN\"" + 
                " \"http://java.sun.com/j2ee/dtds/application_client_1_3.dtd\">" +
                "<application-client>";

    protected static String appClient14Head =
        "<application-client" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
               " http://java.sun.com/xml/ns/javaee/application-client_1_4.xsd\"" +
               " version=\"1.4\"" +
               " id=\"ApplicationClient_ID\"" +
               ">";

    protected static String appClient50Head = 
        "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                  " http://java.sun.com/xml/ns/javaee/application-client_5.xsd\"" +
                " version=\"5\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";

    protected static String appClient60Head =
        "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                  " http://java.sun.com/xml/ns/javaee/application-client_6.xsd\"" +
                " version=\"6\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";

    protected static String appClient70Head =
        "<application-client" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
                  " http://xmlns.jcp.org/xml/ns/javaee/application-client_7.xsd\"" +
                " version=\"7\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";

    protected static String appClient80Head = 
        "<application-client" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
                  " http://xmlns.jcp.org/xml/ns/javaee/application-client_8.xsd\"" +
                " version=\"8\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";

    protected static String appClient90Head =
        "<application-client" +
                " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee " +
                  " https://jakarta.ee/xml/ns/jakartaee/application-client_9.xsd\"" +
                " version=\"9\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";

    protected static String appClientTail =
        "</application-client>";
    
    protected static String appClientXML(int schemaVersion, String body) {
        String appClientHead;
        if ( schemaVersion == ApplicationClient.VERSION_1_2 ) {
            appClientHead = appClient12Head;
        } else if ( schemaVersion == ApplicationClient.VERSION_1_3) {
            appClientHead = appClient13Head;
        } else if ( schemaVersion == ApplicationClient.VERSION_1_4 ) {
            appClientHead = appClient14Head;
        } else if ( schemaVersion == ApplicationClient.VERSION_5) {
            appClientHead = appClient50Head;
        } else if ( schemaVersion == ApplicationClient.VERSION_6 ) {
            appClientHead = appClient60Head;            
        } else if ( schemaVersion == ApplicationClient.VERSION_7 ) {
            appClientHead = appClient70Head;            
        } else if ( schemaVersion == ApplicationClient.VERSION_8 ) {
            appClientHead = appClient80Head;
        } else if ( schemaVersion == ApplicationClient.VERSION_9 ) {
            appClientHead = appClient90Head;
        } else {
            throw new IllegalArgumentException("Unsupported application client version [ " + schemaVersion + " ]");
        }

        return appClientHead + body + appClientTail;
    }
}
