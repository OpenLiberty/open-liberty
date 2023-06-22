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

    // <featureManager>
    //   <feature>springBoot-3.0</feature>
    // </featureManager>
    //
    // [6/20/23, 23:47:52:701 EDT] 00000041 SystemOut
    //   O Using [ com.ibm.ws.classloading.internal.AppClassLoader@5ad3f40f ]
    // [6/20/23, 23:47:52:701 EDT] 00000041 SystemOut
    //   O Failed to locate required class [ com.ibm.ws.springboot.support.web.server.version30.container.LibertyConfiguration ]
    //     for spring boot version [ 3.0.4 ]
    //
    // at com.ibm.ws.springboot.support.shutdown.FeatureAuditor$SpringFeatureRequirement.verify(FeatureAuditor.java:153)
    //
    // com.ibm.ws.app.manager.springboot.container.ApplicationError: CWWKC0267E: The application failed to start because the springBoot-1.5 or springBoot-2.0 feature is configured in the server.xml file. The application requires the springBoot-3.0 feature to be configured.

    @Test
    public void testMissingServletFor30() throws Exception {
        assertNotNull("No error message was found for missing servlet feature ", server.waitForStringInLog("CWWKC0254E"));
        stopServer(true, "CWWKC0254E", "CWWKZ0002E");
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
