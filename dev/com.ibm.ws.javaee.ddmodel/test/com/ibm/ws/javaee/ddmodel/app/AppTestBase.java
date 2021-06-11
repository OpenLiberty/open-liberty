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
package com.ibm.ws.javaee.ddmodel.app;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class AppTestBase extends DDTestBase {

    @SuppressWarnings("deprecation")
    protected Application parse(String xmlText, Version platformVersion) throws Exception {
        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactContainer artifactContainer = mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));

                allowing(artifactContainer).getPath();
                will(returnValue("/"));
            }
        });
        
        Container appRoot = mockery.mock(Container.class, "appRoot" + mockId++);
        Entry ddEntry = mockery.mock(Entry.class, "ddEntry" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(appRoot).adapt(Entry.class);
                will(returnValue(null));
                allowing(appRoot).getPhysicalPath();
                will(returnValue("/root/wlp/usr/servers/server1/apps/myEAR.ear"));   
                allowing(appRoot).getPath();
                will(returnValue("/"));
                allowing(appRoot).getEntry(Application.DD_NAME);
                will(returnValue(ddEntry));

                allowing(ddEntry).getRoot();
                will(returnValue(appRoot));
                allowing(ddEntry).getPath();
                will(returnValue('/' + Application.DD_NAME));
                allowing(ddEntry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xmlText.getBytes("UTF-8"))));
            }
        });

        @SuppressWarnings("unchecked")
        ServiceReference<JavaEEVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);
        String versionText = platformVersion.toString();

        mockery.checking(new Expectations() {
            {                
                allowing(versionRef).getProperty(JavaEEVersion.VERSION);
                will(returnValue(versionText));
            }
        });

        ApplicationAdapter adapter = new ApplicationAdapter();
        adapter.setVersion(versionRef);

        try {
            return adapter.adapt(appRoot, rootOverlay, artifactContainer, appRoot);
        } catch (UnableToAdaptException e) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : e;
        }
    }

    // 1.2
    // 1.3
    // 1.4, http://java.sun.com/xml/ns/j2ee
    // 5,   http://java.sun.com/xml/ns/javaee
    // 6,   http://java.sun.com/xml/ns/javaee
    // 7,   http://xmlns.jcp.org/xml/ns/javaee
    // 8,   http://xmlns.jcp.org/xml/ns/javaee
    // 9,   https://jakarta.ee/xml/ns/jakartaee

    // 1.2 and 1.3 are DD based:

    protected static String app12Head() {
        return "<!DOCTYPE application PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN\"" +
               " \"http://java.sun.com/j2ee/dtds/application_1_2.dtd\">" +
               "<application>";
    }

    protected static String app13Head() {
        return "<!DOCTYPE application PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN\"" +
               " \"http://java.sun.com/j2ee/dtds/application_1_3.dtd\">" +
               "<application>";
    }

    // 1.4, 5.0. 6.0, 7.0, and 8.0 are schema based:

    protected static String app14Head() {
        return "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee" +
               " http://java.sun.com/xml/ns/j2ee/application_1_4.xsd\"" +
               " version=\"1.4\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String app50Head() {
        return "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
               " http://java.sun.com/xml/ns/javaee/application_5.xsd\"" +
               " version=\"5\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String app60Head() {
        return "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
               " http://java.sun.com/xml/ns/javaee/application_6.xsd\"" +
               " version=\"6\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String app70Head() {
        return "<application" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
               " http://xmlns.jcp.org/xml/ns/javaee/application_7.xsd\"" +
               " version=\"7\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String app80Head() {
        return "<application" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
               " http://xmlns.jcp.org/xml/ns/javaee/application_8.xsd\"" +
               " version=\"8\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String app90Head() {
        return "<application" +
               " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee" +
               " https://jakarta.ee/xml/ns/jakartaee/application_9.xsd\"" +
               " version=\"9\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    protected static String appTail() {
        return "</application>";
    }

    //

    protected static String app12(String text) {
        return app12Head() + text + appTail();
    }
    
    protected static String app13(String text) {
        return app13Head() + text + appTail();
    }
    
    protected static String app14(String text) {
        return app14Head() + text + appTail();
    }
    
    protected static String app50(String text) {
        return app50Head() + text + appTail();
    }
    
    protected static String app60(String text) {
        return app60Head() + text + appTail();
    }
    
    protected static String app70(String text) {
        return app70Head() + text + appTail();
    }
    
    protected static String app80(String text) {
        return app80Head() + text + appTail();
    }
    
    protected static String app90(String text) {
        return app90Head() + text + appTail();
    }

    //

    protected static String app12() {
        return app12("");
    }
    
    protected static String app13() {
        return app13("");
    }
    
    protected static String app14() {
        return app14("");
    }
    
    protected static String app50() {
        return app50("");
    }
    
    protected static String app60() {
        return app60("");
    }
    
    protected static String app70() {
        return app70("");
    }
    
    protected static String app80() {
        return app80("");
    }
    
    protected static String app90() {
        return app90("");
    }    
}
