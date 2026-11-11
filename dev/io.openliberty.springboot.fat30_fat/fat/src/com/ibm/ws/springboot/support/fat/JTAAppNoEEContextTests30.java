/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.custom.junit.runner.FATRunner;

/**
 * Negative counterpart to {@link JTAAppDropinsTests30} and {@link JTAAppTests30}.
 *
 * The application is deployed with a springBootApplication element in server.xml and
 * setEEContextOnStartup is explicitly set to false. An explicit false must still be
 * honored: the Java EE context is not available while Spring starts, so the JTA
 * transaction manager bean cannot be created and the application fails to start.
 *
 * This shows that treating an unset setEEContextOnStartup as true (applications in
 * dropins/spring) does not change the behavior of an explicit false.
 *
 * An explicit value cannot be given to an application in dropins/spring, so the
 * apps (springBootApplication element) deployment is required here.
 */
@RunWith(FATRunner.class)
public class JTAAppNoEEContextTests30 extends JTAAppAbstractTests {
    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        appConfig.setEEContextOnStartup(false);
        super.modifyAppConfiguration(appConfig);
    }

    @Test
    public void testNoEEContextOnStartupFailsToStart() throws Exception {
        assertNotNull("Application start did not fail with the expected missing JTA UserTransaction error",
                      server.waitForStringInLog("CWWKZ0002E:.*No JTA UserTransaction available"));
        assertTrue("Application should not have started when setEEContextOnStartup is false",
                   server.findStringsInLogs("CWWKZ0001I:.*testName").isEmpty());
        stopServer(true, "CWWKZ0002E");
    }

    @Override
    public boolean expectApplicationSuccess() {
        return false;
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-3.0", "servlet-6.0", "connectors-2.1", "jdbc-4.2", "jndi-1.0", "componenttest-2.0", "cdi-4.0", "enterpriseBeansLite-4.0"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }
}
