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

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.jsf.FacesConfig;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.FacesVersion;

public class JSFAppTestBase extends DDTestBase {
    private FacesConfigAdapter createFacesAdapter(int maxSchemaVersion) {
        @SuppressWarnings("unchecked")        
        ServiceReference<FacesVersion> versionRef =
            mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(versionRef).getProperty(FacesVersion.FACES_VERSION);
                will(returnValue(maxSchemaVersion));
            }
        });

        FacesConfigAdapter ddAdapter = new FacesConfigAdapter();
        ddAdapter.setVersion(versionRef);
    
        return ddAdapter;
    }
    
    protected FacesConfig parse(String ddText, int maxSchemaVersion) throws Exception {
        return parse(ddText, maxSchemaVersion, null);
    }

    protected FacesConfig parse(
            String ddText,
            int maxSchemaVersion,
            String altMessage, String... messages) throws Exception {

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/myWar.war";
        String fragmentPath = null;

        String ddPath = FacesConfig.DD_NAME;

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, createFacesAdapter(maxSchemaVersion), ddPath,
                altMessage, messages);                    
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
