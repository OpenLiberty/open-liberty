/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class WebAppTestBase extends DDTestBase {

	protected WebApp parse(String xml, String... messages) throws Exception {
        return parse(xml, WebApp.VERSION_3_0, messages);
    }

    @SuppressWarnings("deprecation")
    WebApp parse(String xml, int maxVersion, String... messages) throws Exception {
        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactEntry artifactEntry = mockery.mock(ArtifactEntry.class, "artifactEntry" + mockId++);

        Container moduleRoot = mockery.mock(Container.class, "root" + mockId++);
        Entry moduleEntry = mockery.mock(Entry.class, "entry" + mockId++);        

        Container root = mockery.mock(Container.class, "root" + mockId++);
        Entry entry = mockery.mock(Entry.class, "entry" + mockId++);

        @SuppressWarnings("unchecked")
        ServiceReference<ServletVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));                
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));

                allowing(moduleRoot).getPhysicalPath();
                will(returnValue("c:\\someDir\\apps\\expanded\\myEar.ear"));
                allowing(moduleRoot).adapt(Entry.class);
                will(returnValue(null));

                allowing(moduleEntry).getRoot();
                will(returnValue(moduleRoot));
                allowing(moduleEntry).getPath();
                will(returnValue("webModule.war"));

                allowing(artifactEntry).getRoot();
                will(returnValue(root));
                allowing(artifactEntry).getPath();
                will(returnValue('/' + WebApp.DD_NAME));

                allowing(root).adapt(Entry.class);
                will(returnValue(moduleEntry));

                allowing(entry).getRoot();
                will(returnValue(root));                
                allowing(entry).getPath();
                will(returnValue('/' + WebApp.DD_NAME));

                allowing(entry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));

                allowing(versionRef).getProperty(ServletVersion.VERSION);
                will(returnValue(maxVersion));
            }
        });

        WebAppEntryAdapter adapter = new WebAppEntryAdapter();
        adapter.setVersion(versionRef);

        Exception boundException;

        try {
            WebApp webApp = adapter.adapt(root, rootOverlay, artifactEntry, entry);
            if ( (messages != null) && (messages.length != 0) ) {
                throw new Exception("Expected exception text [ " + Arrays.toString(messages) + " ]");
            }
            return webApp;

        } catch ( UnableToAdaptException e ) {
            Throwable cause = e.getCause();
            if ( cause instanceof Exception ) {
                boundException = (Exception) cause;
            } else {
                boundException = e;
            }
        }

        if ( (messages != null) && (messages.length != 0) ) {
            String message = boundException.getMessage();
            if ( message != null ) {
                for ( String expected : messages ) {
                    if ( message.contains(expected) ) {
                        return null;
                    }
                }
            }
        }
        throw boundException;        
    }

    protected static String webApp22Head() {    
        return "<!DOCTYPE web-app PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN\"" +
               " \"http://java.sun.com/j2ee/dtds/web-app_2.2.dtd\"" +
               ">" + "\n" +
               "<web-app>";
    }

    protected static String webApp23Head() {
        return "<!DOCTYPE web-app PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"" +
               " \"http://java.sun.com/dtd/web-app_2_3.dtd\"" +
               ">" + "\n" +
               "<web-app>";
    }

    protected static String webApp24Head() {
        return "<web-app" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"" +
               " version=\"2.4\"" +
               " id=\"WebApp_ID\"" +
               ">";
    }

    protected static String webApp25Head() {
        return "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"" +
               " version=\"2.5\"" +
               " id=\"WebApp_ID\"" +
               ">";
    }
    
    protected static String webApp30Head() {
        return "<web-app" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
               " version=\"3.0\"" +
               " id=\"WebApp_ID\"" +
               ">";
    }
    
    protected static String webApp31Head() {
        return "<web-app" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\"" +
               " version=\"3.1\"" +
               " id=\"WebApp_ID\"" +
               ">";
    }

    protected static String webApp40Head() {
        return "<web-app" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\"" +
               " version=\"4.0\"" +
               " id=\"WebApp_ID\"" +
               ">";
    }

    protected static String webApp50Head() {
        return "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd\"" +
               " version=\"5.0\"" +
               ">";
    }
    
    
    protected static String webAppTail() {
        return "</web-app>";
    }
    
    protected static String webApp(int version) {
        return webApp(version, "");
    }

    protected static String webApp(int version, String nestedText) {
        String head;
        if ( version == WebApp.VERSION_2_2 ) {
            head = webApp22Head();
        } else if ( version == WebApp.VERSION_2_3 ) {
            head = webApp23Head();
        } else if ( version == WebApp.VERSION_2_4 ) {
            head = webApp24Head();
        } else if ( version == WebApp.VERSION_2_5 ) {
            head = webApp25Head();
        } else if ( version == WebApp.VERSION_3_0 ) {
            head = webApp30Head();
        } else if ( version == WebApp.VERSION_3_1 ) {
            head = webApp31Head();
        } else if ( version == WebApp.VERSION_4_0 ) {
            head = webApp40Head();
        } else if ( version == WebApp.VERSION_5_0 ) {
            head = webApp50Head();  
        } else {
            throw new IllegalArgumentException("Unexpected WebVersion [ " + version + " ]");
        }
        return head + "\n" + nestedText + "\n" + webAppTail();
    }
}
