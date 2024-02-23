/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.remote.fat.tests.BadApplicationTests;
import com.ibm.ws.ejbcontainer.remote.fat.tests.RemoteSessionTests;
import com.ibm.ws.ejbcontainer.remote.fat.tests.RemoteTests;
import com.ibm.ws.ejbcontainer.remote.fat.tests.Server2ServerTests;
//import com.ibm.ws.ejbcontainer.remote.fat.tests.Server2TraditionalTests;

@RunWith(Suite.class)
@SuiteClasses({
                BadApplicationTests.class,
                RemoteSessionTests.class,
                RemoteTests.class,
//                Server2TraditionalTests.class,
                Server2ServerTests.class
})
public class FATSuite {
}
