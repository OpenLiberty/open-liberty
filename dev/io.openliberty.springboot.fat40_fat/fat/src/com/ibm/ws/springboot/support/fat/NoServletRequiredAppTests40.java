/*******************************************************************************
 * Copyright (c) 2018,2025 IBM Corporation and others.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
@MinimumJavaLevel(javaLevel = 17)
public class NoServletRequiredAppTests40 extends AbstractSpringTests {
    @Override
    public boolean expectWebApplication() {
        return false;
    }

    @Test
    public void testNoServletRequiredFor40() throws Exception {
        assertNotNull("Output not found", server.waitForStringInLog("Hello World!!"));
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-4.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_JAVA;
    }

}
