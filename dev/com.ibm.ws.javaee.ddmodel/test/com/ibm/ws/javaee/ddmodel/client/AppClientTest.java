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

import org.junit.Test;

import com.ibm.ws.javaee.dd.client.ApplicationClient;

/**
 * Application deployment descriptor parse tests.
 */
public class AppClientTest extends AppClientTestBase {
    @Test
    public void testAppClient() throws Exception {
        for ( int schemaVersion : ApplicationClient.VERSIONS ) {
            for ( int maxSchemaVersion : ApplicationClient.VERSIONS ) {
                // Open liberty will always parse JavaEE6 and earlier
                // schema versions.
                int effectiveMax;
                if ( maxSchemaVersion < ApplicationClient.VERSION_6 ) {
                    effectiveMax = ApplicationClient.VERSION_6;
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
                
                parse( appClientXML(schemaVersion, ""), maxSchemaVersion, altMessage, messages );
            }
        }
    }
}
