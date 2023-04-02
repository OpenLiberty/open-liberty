/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi.jee.ejbWithJsp.JEEInjectionTargetTest;
import com.ibm.ws.cdi.jee.jaxrs.inject.InjectIntoPathTest;
import com.ibm.ws.cdi.jee.jsf.SimpleJSFTest;
import com.ibm.ws.cdi.jee.jsf.SimpleJSFWithSharedLibTest;
import com.ibm.ws.cdi.jee.jsp.SimpleJSPTest;
import com.ibm.ws.cdi.jee.webservices.CDI12WebServicesTest;

/**
 * Tests specific to JEE integration
 */
@RunWith(Suite.class)
@SuiteClasses({
                CDI12WebServicesTest.class,
                InjectIntoPathTest.class,
                JEEInjectionTargetTest.class,
                SimpleJSFTest.class,
                SimpleJSFWithSharedLibTest.class,
                SimpleJSPTest.class,
})
public class FATSuite {

}
