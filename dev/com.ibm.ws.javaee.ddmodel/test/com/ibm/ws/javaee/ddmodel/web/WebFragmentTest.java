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

import org.junit.Test;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;

/**
 * Web fragment descriptor parsing unit tests.
 */
public class WebFragmentTest extends WebFragmentTestBase {

    @Test
    public void testWebFragment() throws Exception {
        for ( int schemaVersion : WebFragment.VERSIONS ) {
            for ( int maxSchemaVersion : WebFragment.VERSIONS ) {
                // The WebApp parser uses a maximum schema
                // version of "max(version, WebApp.VERSION_3_0)".
                // Adjust the message expectations accordingly.
                //
                // See: com.ibm.ws.javaee.ddmodel.web.WebAppDDParser

                int effectiveMax;
                if ( maxSchemaVersion < WebApp.VERSION_3_0 ) {
                    effectiveMax = WebApp.VERSION_3_0;
                } else {
                    effectiveMax = maxSchemaVersion;
                }

                String altMessage;
                String[] messages;
                if ( schemaVersion > effectiveMax ) {
                    altMessage = UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE;
                    messages = UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES;
                } else {
                    altMessage = null;
                    messages = null;
                }

                parse( webFragment( schemaVersion, WebAppTestBase.webAppBody() ),
                       maxSchemaVersion,
                       altMessage, messages );
            }
        }
    }
    
    @Test
    public void testEE6WebFragment30OrderingElement() throws Exception {
        parse(webFragment30("<ordering/>"));
    }

    // The prohibition against having more than one ordering element
    // was added in JavaEE7.
    @Test
    public void testEE6WebFragment30OrderingDuplicates() throws Exception {
        parse(webFragment30("<ordering/>" + "<ordering/>"));
    }

    @Test
    public void testEE7WebFragment31OrderingDuplicates() throws Exception {
        parse(webFragment31("<ordering/>" + "<ordering/>"),
                WebApp.VERSION_3_1,
                "at.most.one.occurrence",
                "CWWKC2266E", "ordering",
                    "MyWar.war : WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml" );        
    }
}
