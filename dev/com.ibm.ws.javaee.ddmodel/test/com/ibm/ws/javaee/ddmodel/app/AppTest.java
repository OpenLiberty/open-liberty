/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.app;

import org.junit.Test;
import com.ibm.ws.javaee.dd.app.Application;

public class AppTest extends AppTestBase {
    @Test
    public void testApp() throws Exception {
        for ( int schemaVersion : Application.VERSIONS ) {
            for ( int maxSchemaVersion : Application.VERSIONS ) {
                // Open liberty will always parse JavaEE6 and earlier
                // schema versions.
                int effectiveMax;
                if ( maxSchemaVersion < VERSION_6_0_INT ) {
                    effectiveMax = VERSION_6_0_INT;
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
