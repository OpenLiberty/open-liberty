/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.feature.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses({
        Servlet3toMetrics.class,
        Servlet4toMetrics.class,
        Servlet5toMetrics.class,
        Servlet6toMetrics.class,
        Servlet3toHealth.class,
        Servlet4toHealth.class,
        Servlet5toHealth.class,
        Servlet6toHealth.class,
        ee7toMP.class,
        ee8toMP.class,
        ee9toMP.class,
        ee10toMP.class,
        testErrorMessages.class
})
public class FATSuite {}
