/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.web;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class WebFragmentTestBase extends DDTestBase {

    private WebFragmentAdapter createFragmentAdapter(int maxVersion) {
        @SuppressWarnings("unchecked")
        ServiceReference<ServletVersion> versionRef = mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(versionRef).getProperty("version");
                will(returnValue(maxVersion));
            }
        });

        WebFragmentAdapter adapter = new WebFragmentAdapter();
        adapter.setVersion(versionRef);

        return adapter;
    }
        
    WebFragment parse(String ddText) throws Exception {
        return parse(ddText, WebApp.VERSION_3_0, null);
    }

    WebFragment parse(String ddText, int maxSchemaVersion) throws Exception {
        return parse(ddText, maxSchemaVersion, null);
    }

    WebFragment parse(String ddText, String altMessage, String... messages) throws Exception {
        return parse(ddText, WebApp.VERSION_3_0, altMessage, messages);
    }
    
    WebFragment parse(String ddText, int maxSchemaVersion,
            String altMessage,
            String... messages) throws Exception {

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/MyWar.war";
        String fragmentPath = "WEB-INF/lib/fragment1.jar";
        String ddPath = WebFragment.DD_NAME;

        try {
            return parse(
                    appPath, modulePath, fragmentPath,
                    ddText, createFragmentAdapter(maxSchemaVersion), ddPath,
                    altMessage, messages);
        } catch ( UnableToAdaptException e ) {
            Throwable cause = e.getCause();
            throw cause instanceof Exception ? (Exception) cause : e;
        }
    }

    protected static String webFragmentTail() {
        return "</web-fragment>";
    }
    
    protected static final String webFragment30() {
        return webFragment30(WebAppTestBase.webAppBody());
    }
    
    protected static final String webFragment30(String body) {
        return "<web-fragment" +
                   " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd\"" +
                   " version=\"3.0\"" +
                   " id=\"WebFragment_ID\"" +
               ">" +
                   body +
               webFragmentTail();
    }

    protected static final String webFragment31() {
        return webFragment31(WebAppTestBase.webAppBody());
    }
    
    protected static final String webFragment31(String body) {
        return "<web-fragment" +
                   " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-fragment_3_1.xsd\"" +
                   " version=\"3.1\"" +
                   " id=\"WebFragment_ID\"" +
               ">" +
                   body +
               webFragmentTail();
    }

    protected static final String webFragment40() {
        return webFragment40(WebAppTestBase.webAppBody());
    }

    protected static final String webFragment40(String body) {
        return "<web-fragment" +
                   " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-fragment_4_0.xsd\"" +
                   " version=\"4.0\"" +
                   " id=\"WebFragment_ID\"" +
               ">" +
                   body +
               webFragmentTail();
    }

    protected static String webFragment50() {
        return webFragment50(WebAppTestBase.webAppBody());
    }

    protected static String webFragment50(String body) {
        return "<web-fragment xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-fragment_5_0.xsd\"" +
                   " version=\"5.0\"" +
               ">" +
                   body +
               webFragmentTail();
    }    
    
    protected static String webFragment60(String body) {
        return "<web-fragment xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-fragment_6_0.xsd\"" +
                   " version=\"6.0\"" +
               ">" +
                   body +
               webFragmentTail();
    }
    
    protected static String webFragment61(String body) {
        return "<web-fragment xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-fragment_6_1.xsd\"" +
                   " version=\"6.1\"" +
               ">" +
                   body +
               webFragmentTail();
    } 

    protected static String webFragment(int schemaVersion) {
        return webFragment(schemaVersion, WebAppTestBase.webAppBody());
    }

    protected static String webFragment(int version, String body) {
        if ( version == WebApp.VERSION_3_0 ) {
            return webFragment30(body);
        } else if ( version == WebApp.VERSION_3_1 ) {
            return webFragment31(body);
        } else if ( version == WebApp.VERSION_4_0 ) {
            return webFragment40(body);
        } else if ( version == WebApp.VERSION_5_0 ) {
            return webFragment50(body);
        } else if ( version == WebApp.VERSION_6_0 ) {
            return webFragment60(body);
        } else if ( version == WebApp.VERSION_6_1 ) {
            return webFragment61(body);
        } else {
            throw new IllegalArgumentException("Unexpected WebFragment version [ " + version + " ]");
        }
    }
}
