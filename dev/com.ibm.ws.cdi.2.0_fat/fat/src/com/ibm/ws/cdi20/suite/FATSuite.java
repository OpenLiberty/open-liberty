/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi20.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi20.fat.tests.AsyncEventsTest;
import com.ibm.ws.cdi20.fat.tests.BasicCdi20Tests;
import com.ibm.ws.cdi20.fat.tests.BuiltinAnnoLiteralsTest;
import com.ibm.ws.cdi20.fat.tests.CDIContainerConfigTest;
import com.ibm.ws.cdi20.fat.tests.SecureAsyncEventsTest;

@RunWith(Suite.class)
@SuiteClasses({
                AsyncEventsTest.class,
                BasicCdi20Tests.class,
                BuiltinAnnoLiteralsTest.class,
                CDIContainerConfigTest.class,
                SecureAsyncEventsTest.class
})
public class FATSuite {

}
