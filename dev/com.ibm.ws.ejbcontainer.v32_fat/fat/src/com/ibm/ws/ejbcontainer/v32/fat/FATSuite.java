/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.v32.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.v32.fat.tests.JavaGlobalInjectIntoServletTest;
import com.ibm.ws.ejbcontainer.v32.fat.tests.PassivationTest;
import com.ibm.ws.ejbcontainer.v32.fat.tests.SingletonLifecycleTxTest;
import com.ibm.ws.ejbcontainer.v32.fat.tests.StatefulLifecycleTxTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                JavaGlobalInjectIntoServletTest.class,
                PassivationTest.class,
                SingletonLifecycleTxTest.class,
                StatefulLifecycleTxTest.class
})
public class FATSuite {
}
