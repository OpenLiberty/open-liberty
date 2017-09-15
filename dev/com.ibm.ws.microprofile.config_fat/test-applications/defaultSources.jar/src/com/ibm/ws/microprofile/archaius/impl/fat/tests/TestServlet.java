/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.archaius.impl.fat.tests;

import javax.servlet.annotation.WebServlet;

import com.ibm.ws.microprofile.appConfig.test.utils.AbstractTestServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class TestServlet extends AbstractTestServlet {
    private static final String[] TESTS = new String[] {
                                                         DefaultsGetBuilderWithDefaults.class.getSimpleName(),
                                                         DefaultsGetConfig.class.getSimpleName(),
                                                         DefaultsGetConfigPathEar.class.getSimpleName(),
                                                         DefaultsGetConfigPathJar.class.getSimpleName(),
                                                         DefaultsGetConfigPathProcEnv.class.getSimpleName(),
                                                         DefaultsGetConfigPathSysProps.class.getSimpleName(),
                                                         DefaultsGetConfigPathWar.class.getSimpleName(),
                                                         DefaultsGetConfigProperties.class.getSimpleName(),
                                                         DefaultsGetConfigWasSpecific.class.getSimpleName(),
                                                         DefaultsGetEmptyBuilderNoDefaults.class.getSimpleName()
    };

    public TestServlet() {
        super(TestServlet.class.getPackage().getName(), TESTS);
    }
}