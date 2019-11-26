/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jdbc.fat.tests.ConfigTest;
import com.ibm.ws.jdbc.fat.tests.DataSourceJaasTest;
import com.ibm.ws.jdbc.fat.tests.DataSourceTest;

@RunWith(Suite.class)
@SuiteClasses({
               ConfigTest.class,
               DataSourceTest.class,
               DataSourceJaasTest.class
})
public class FATSuite {}
