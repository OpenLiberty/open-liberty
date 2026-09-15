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

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class AopSpringBootAppTests30 extends AopAbstractTests {
    @Override
    public Set<String> getFeatures() {
        String servletVersion = testName.getMethodName().contains("Servlet61") ? "servlet-6.1" : "servlet-6.0";
        return new HashSet<>(Arrays.asList(servletVersion, "springBoot-3.0"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @Test
    public void testAopSpringBootApplicationServlet60() throws Exception {
        testAop();
    }

    // The SRVE8046E NullPointerException on AsyncContext dispatch is expected when running with servlet-6.1.
    // See https://github.com/OpenLiberty/open-liberty/issues/35666
    @Test
    public void testAopSpringBootApplicationServlet61() throws Exception {
        testAop();
    }
}
