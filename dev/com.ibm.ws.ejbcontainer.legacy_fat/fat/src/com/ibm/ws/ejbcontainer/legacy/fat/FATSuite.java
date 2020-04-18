/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.legacy.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.legacy.fat.tests.SFLocalTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.SFRemoteTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.SLLocalTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.SLRemoteTest;

@RunWith(Suite.class)
@SuiteClasses({
                SFLocalTest.class,
                SLLocalTest.class,
                SFRemoteTest.class,
                SLRemoteTest.class
})
public class FATSuite {}
