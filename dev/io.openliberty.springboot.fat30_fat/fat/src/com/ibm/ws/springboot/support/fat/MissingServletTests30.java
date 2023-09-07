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

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
@MinimumJavaLevel(javaLevel = 17)
public class MissingServletTests30 extends AbstractSpringTests {
    @Override
    public boolean expectApplicationSuccess() {
        return false;
    }

    @Test
    public void testMissingServletFor30() throws Exception {
        assertNotNull("No error message CWWKC0274E was found for missing servlet feature",
                      server.waitForStringInLog("CWWKC0274E"));
        stopServer(true, "CWWKC0274E", "CWWKZ0002E");
    }

    // appsecurity-5.0 cannot be added for a seperate test as it contains the servlet-6.0 feature
    @Override
    public Set<String> getFeatures() {
        return Collections.singleton("springBoot-3.0");
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

}
