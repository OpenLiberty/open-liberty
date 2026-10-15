/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class AopSpringBootAppTests30 extends AopAbstractTests {
    @Override
    public Set<String> getFeatures() {
        // springBoot-3.0 requires Jakarta EE 10 (servlet-6.0); servlet-6.1 (Jakarta EE 11) is incompatible.
        return new HashSet<>(Arrays.asList("servlet-6.0", "springBoot-3.0"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    // Stop the server after each test method so that each test uses its own servlet feature.
    // springBoot-3.0 requires Jakarta EE 10 (servlet-6.0) and cannot be combined with servlet-6.1,
    // so both tests run with servlet-6.0 only. SRVE8046E does not apply here.
    @After
    public void stopServerAfterTest() throws Exception {
        stopServer(DO_CLEANUP_APPS);
    }

    @Test
    public void testAopSpringBootApplicationServlet60Sync() throws Exception {
        testAopSync();
    }

    @Test
    public void testAopSpringBootApplicationServlet60Aysnc() throws Exception {
        testAopAsync();
    }
}
