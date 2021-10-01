/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.interceptor.v32.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.interceptor.v32.fat.tests.AroundConstructTest;
import com.ibm.ws.ejbcontainer.interceptor.v32.fat.tests.AroundConstructXmlTest;
import com.ibm.ws.ejbcontainer.interceptor.v32.fat.tests.UnspecifiedContextTest;

@RunWith(Suite.class)
@SuiteClasses({
                AroundConstructTest.class,
                AroundConstructXmlTest.class,
                UnspecifiedContextTest.class
})
public class FATSuite {
}
