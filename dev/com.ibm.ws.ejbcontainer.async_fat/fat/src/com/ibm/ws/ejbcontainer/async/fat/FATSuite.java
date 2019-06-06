/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.async.fat.tests.AsyncConfigTests;
import com.ibm.ws.ejbcontainer.async.fat.tests.AsyncCoreTests;
import com.ibm.ws.ejbcontainer.async.fat.tests.AsyncRemoteTests;
import com.ibm.ws.ejbcontainer.async.fat.tests.AsyncSecureTests;

@RunWith(Suite.class)
@SuiteClasses({
                AsyncConfigTests.class,
                AsyncCoreTests.class,
                AsyncRemoteTests.class,
                AsyncSecureTests.class
})
public class FATSuite {
}
