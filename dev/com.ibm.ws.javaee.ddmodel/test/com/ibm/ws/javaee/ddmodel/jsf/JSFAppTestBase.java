/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.jsf.FacesConfig;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.FacesVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class JSFAppTestBase extends DDTestBase {
    protected boolean isWarModule = false;

    private static final String JSF22_FacesConfig_Version = "2.2";
    private static final String JSF23_FacesConfig_Version = "2.3";

    protected FacesConfig parse(String xml) throws Exception {
        if (xml.contains(JSF22_FacesConfig_Version)) {
            return parseJSFApp(xml, FacesConfig.VERSION_2_2);
        } else if(xml.contains(JSF23_FacesConfig_Version)){
            return parseJSFApp(xml, FacesConfig.VERSION_2_3);
        } else { // Faces 3.0
            return parseJSFApp(xml, FacesConfig.VERSION_3_0);
        }
    }

    FacesConfig parseJSFApp(String xml, int maxVersion) throws Exception {
        OverlayContainer rootOverlay = mockery.mock(OverlayContainer.class, "rootOverlay" + mockId++);
        ArtifactContainer artifactContainer = mockery.mock(ArtifactContainer.class, "artifactContainer" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(rootOverlay).getFromNonPersistentCache(with(any(String.class)), with(any(Class.class)));
                will(returnValue(null));
                allowing(rootOverlay).addToNonPersistentCache(with(any(String.class)), with(any(Class.class)), with(any(Object.class)));

                allowing(artifactContainer).getPath();
                will(returnValue(FacesConfig.DD_NAME));
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
                will(returnValue(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
            }
        });
        
        @SuppressWarnings("unchecked")
        ServiceReference<FacesVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(versionRef).getProperty(FacesVersion.FACES_VERSION);
                will(returnValue(maxVersion));
            }
        });

        FacesConfigAdapter adapter = new FacesConfigAdapter();
        adapter.setVersion(versionRef);

        try {
            return adapter.adapt(moduleRoot, rootOverlay, artifactContainer, moduleRoot);
        } catch (UnableToAdaptException e) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : e;
        }
    }

    protected static final String jsf22() {
        return "<faces-config" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_2.xsd\"" +
               " version=\"2.2\"" +
               ">";
    }

    protected static final String jsf23() {
        return "<faces-config" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_3.xsd\"" +
               " version=\"2.3\"" +
               ">";
    }

    protected static final String faces30() {
        return "<faces-config" +
               " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_3_0.xsd\"" +
               " version=\"3.0\"" +
               ">";
    }

}
