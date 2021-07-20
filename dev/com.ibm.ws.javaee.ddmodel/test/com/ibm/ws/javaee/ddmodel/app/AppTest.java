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

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Version;

import com.ibm.ws.javaee.version.JavaEEVersion;

public class AppTest extends AppTestBase {
    @Test
    public void testCompareVersions() throws Exception {
        for ( int v1No = 0; v1No < JavaEEVersion.VERSIONS.length; v1No++ ) {
            Version v1 = JavaEEVersion.VERSIONS[v1No];
            for ( int v2No = 0; v2No < JavaEEVersion.VERSIONS.length; v2No++ ) {
                Version v2 = JavaEEVersion.VERSIONS[v2No];

                int expectedCmp = v2No - v1No;
                int actualCmp = v2.compareTo(v1);

                boolean matchCmp = 
                    ( ((expectedCmp  < 0) && (actualCmp  < 0)) ||
                      ((expectedCmp == 0) && (actualCmp == 0)) ||
                      ((expectedCmp  > 0) && (actualCmp  > 0)) );

                if ( !matchCmp ) {
                    Assert.assertEquals( "Version [ " + v1 + " ] compared with [ " + v2 + " ]." +
                                        " Expected [ " + expectedCmp + " ]; " +
                                        " received [ " + actualCmp + " ]",
                                        expectedCmp, actualCmp);
                }
            }
        }
    }

    @Test
    public void testApp() throws Exception {
        for ( int schemaVersion : JavaEEVersion.VERSION_INTS ) {
            for ( int maxSchemaVersion : JavaEEVersion.VERSION_INTS ) {
                // Open liberty will always parse JavaEE6 and earlier
                // schema versions.
                int effectiveMax;
                if ( maxSchemaVersion < JavaEEVersion.VERSION_6_0_INT ) {
                    effectiveMax = JavaEEVersion.VERSION_6_0_INT;
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

                parseApp( app(schemaVersion, appBody), maxSchemaVersion, altMessage, messages );
            }
        }
    }    
}
