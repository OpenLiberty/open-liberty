/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.legacy.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.legacy.fat.tests.CacheTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.EJB1XStatefulTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.EJBinWAR2xTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.PassivationRegressionTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.SFLocalTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.SFPassivationTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.SFRemoteTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.SLLocalTest;
import com.ibm.ws.ejbcontainer.legacy.fat.tests.SLRemoteTest;

@RunWith(Suite.class)
@SuiteClasses({
                CacheTest.class,
                EJB1XStatefulTest.class,
                EJBinWAR2xTest.class,
                PassivationRegressionTest.class,
                SFLocalTest.class,
                SFPassivationTest.class,
                SFRemoteTest.class,
                SLLocalTest.class,
                SLRemoteTest.class
})
public class FATSuite {}
