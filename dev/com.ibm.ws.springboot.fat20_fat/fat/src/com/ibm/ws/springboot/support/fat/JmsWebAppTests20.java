/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.SkipJavaSemeruWithFipsEnabled.SkipJavaSemeruWithFipsEnabledRule;

@RunWith(FATRunner.class)
public class JmsWebAppTests20 extends JmsAbstractTests {
    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("servlet-4.0", "jms-2.0", "jndi-1.0", "componenttest-1.0", "jdbc-4.2"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.WEB_APP_TAG;
    }

    @Override
    public String getContextRoot() {
        return "/testName/";
    }

    @ExpectedFFDC("org.springframework.web.util.NestedServletException")
    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void testJmsWebApplicationWithTransaction() throws Exception {
        testJmsWithTransaction();
    }
}
