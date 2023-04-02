/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.injection.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.injection.fat.tests.InjectionMIXTest;
import com.ibm.ws.ejbcontainer.injection.fat.tests.InjectionMiscTest;
import com.ibm.ws.ejbcontainer.injection.fat.tests.InjectionXMLTest;
import com.ibm.ws.ejbcontainer.injection.fat.tests.LookupOverrideTest;
import com.ibm.ws.ejbcontainer.injection.fat.tests.RemoteInjectionTest;

@RunWith(Suite.class)
@SuiteClasses({
                InjectionMiscTest.class,
                InjectionMIXTest.class,
                InjectionXMLTest.class,
                LookupOverrideTest.class,
                RemoteInjectionTest.class
})
public class FATSuite {}
