/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
public class MissingServletTests30 extends AbstractSpringTests {
    @Override
    public boolean expectApplicationSuccess() {
        return false;
    }

    // Disabled: The jakarta servlet class is being provisioned even without the servlet feature!
    //
    // FeatureAuditor: Found [ true ] class [ org.springframework.web.WebApplicationInitializer ] as [ org/springframework/web/WebApplicationInitializer.class ]
    // FeatureAuditor: Found [ false ] class [ org.springframework.web.socket.WebSocketHandler ] as [ org/springframework/web/socket/WebSocketHandler.class ]
    // FeatureAuditor: Found [ false ] class [ com.ibm.ws.springboot.support.web.server.version15.container.LibertyConfiguration ] as [ com/ibm/ws/springboot/support/web/server/version15/container/LibertyConfiguration.class ]
    // FeatureAuditor: Found [ false ] class [ com.ibm.ws.springboot.support.web.server.version20.container.LibertyConfiguration ] as [ com/ibm/ws/springboot/support/web/server/version20/container/LibertyConfiguration.class ]
    // FeatureAuditor: Found [ false ] class [ io.openliberty.springboot.support.web.server.version30.container.LibertyConfiguration ] as [ io/openliberty/springboot/support/web/server/version30/container/LibertyConfiguration.class ]
    // FeatureAuditor: Found [ false ] class [ javax.servlet.Servlet ] as [ javax/servlet/Servlet.class ]
    // FeatureAuditor: Found [ true ] class [ jakarta.servlet.Servlet ] as [ jakarta/servlet/Servlet.class ]
    // FeatureAuditor: Found [ false ] class [ javax.websocket.WebSocketContainer ] as [ javax/websocket/WebSocketContainer.class ]
    // FeatureAuditor: Found [ true ] class [ jakarta.websocket.WebSocketContainer ] as [ jakarta/websocket/WebSocketContainer.class ]

    @Test
    public void testMissingServletFor30() throws Exception {
        assertNotNull("No error message CWWKC0274E was found for missing servlet feature",
                      server.waitForStringInLog("CWWKC0274E"));
        stopServer(true, "CWWKC0274E", "CWWKZ0002E");
    }

    @Override
    public Set<String> getFeatures() {
        return Collections.singleton("springBoot-3.0");
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

}
