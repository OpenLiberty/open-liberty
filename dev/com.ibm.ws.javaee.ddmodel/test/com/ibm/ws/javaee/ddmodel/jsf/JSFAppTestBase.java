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
package com.ibm.ws.javaee.ddmodel.jsf;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.jsf.FacesConfig;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.FacesVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class JSFAppTestBase extends DDTestBase {
    @SuppressWarnings("deprecation")
    protected FacesConfig parse(String xmlText, int maxSchemaVersion, String... messages) throws Exception {
        OverlayContainer rootOverlay =
            mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactContainer artifactContainer =
            mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));

                allowing(artifactContainer).getPath();
                will(returnValue("/"));
            }
        });

        Container moduleRoot = mockery.mock(Container.class, "moduleRoot" + mockId++);
        Entry ddEntry = mockery.mock(Entry.class, "entry" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(moduleRoot).adapt(Entry.class);
                will(returnValue(null));
                allowing(moduleRoot).getPhysicalPath();
                will(returnValue("/root/wlp/usr/servers/server1/apps/myWar.war"));   
                allowing(moduleRoot).getPath();
                will(returnValue("/"));
                allowing(moduleRoot).getEntry(FacesConfig.DD_NAME);
                will(returnValue(ddEntry));

                allowing(ddEntry).getRoot();
                will(returnValue(moduleRoot));
                allowing(ddEntry).getPath();
                will(returnValue('/' + FacesConfig.DD_NAME));
                allowing(ddEntry).adapt(InputStream.class);
                will(returnValue(new ByteArrayInputStream(xmlText.getBytes("UTF-8"))));
            }
        });
        
        @SuppressWarnings("unchecked")
        ServiceReference<FacesVersion> versionRef =
            mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(versionRef).getProperty(FacesVersion.FACES_VERSION);
                will(returnValue(maxSchemaVersion));
            }
        });

        FacesConfigAdapter adapter = new FacesConfigAdapter();
        adapter.setVersion(versionRef);

        Exception boundException;

        try {
            FacesConfig facesConfig = adapter.adapt(moduleRoot, rootOverlay, artifactContainer, moduleRoot);
            if ( (messages != null) && (messages.length != 0) ) {
                throw new Exception("Expected exception text [ " + Arrays.toString(messages) + " ]");
            }
            return facesConfig;

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

    protected static final String jsf10Head =
        "<!DOCTYPE faces-config PUBLIC" +
            " \"-//Sun Microsystems, Inc.//DTD JavaServer Faces Config 1.0//EN\"" +
            " \"http://java.sun.com/dtd/web-facesconfig_1_0.dtd\">" + "\n" +
        "<faces-config>";

    protected static final String jsf11Head =
        "<!DOCTYPE faces-config PUBLIC" +
            " \"-//Sun Microsystems, Inc.//DTD JavaServer Faces Config 1.1//EN\"" +
            " \"http://java.sun.com/dtd/web-facesconfig_1_1.dtd\">" + "\n" +
        "<faces-config>";

    protected static final String jsf12Head =
        "<faces-config" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + 
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-facesconfig_1_2.xsd\"" +
            " version=\"1.2\">";            

    protected static final String jsf20Head =
        "<faces-config" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-facesconfig_2_0.xsd\"" +
            " version=\"2.0\">";
    
    protected static final String jsf21Head =
        "<faces-config" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-facesconfig_2_1.xsd\"" +
            " version=\"2.1\">";    
    
    protected static final String jsf22Head =
        "<faces-config" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_2.xsd\"" +
            " version=\"2.2\"" +
            ">";

    protected static final String jsf23Head =
        "<faces-config" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_3.xsd\"" +
            " version=\"2.3\"" +
            ">";

    protected static final String jsf30Head =
        "<faces-config" +
            " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_3_0.xsd\"" +
            " version=\"3.0\"" +
            ">";
    
    protected static final String jsfTail =
        "</faces-config>";
    
    public String jsf(int version, String jsfBody) {
        String jsfHead;
        if ( version == FacesConfig.VERSION_1_0 ) {
            jsfHead = jsf10Head;
        } else if ( version == FacesConfig.VERSION_1_1 ) {
            jsfHead = jsf11Head;

        } else if ( version == FacesConfig.VERSION_1_2 ) {
            jsfHead = jsf12Head;            
        } else if ( version == FacesConfig.VERSION_2_0 ) {
            jsfHead = jsf20Head;
        } else if ( version == FacesConfig.VERSION_2_1 ) {
            jsfHead = jsf21Head;

        } else if ( version == FacesConfig.VERSION_2_2 ) {
            jsfHead = jsf22Head;
        } else if ( version == FacesConfig.VERSION_2_3 ) {
            jsfHead = jsf23Head;
            
        } else if ( version == FacesConfig.VERSION_3_0 ) {
            jsfHead = jsf30Head;
        } else {
            throw new IllegalArgumentException("Unknown faces config version [ " + version + " ]");
        }

        return jsfHead + "\n" +
               jsfBody + "\n" +
               jsfTail;
    }
}
