/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jca.fat.app.ConnectionManagerMBeanTest;
import com.ibm.ws.jca.fat.app.DependantApplicationTest;
import com.ibm.ws.jca.fat.app.JCATest;
import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
               AlwaysPassesTest.class,
               //DependantApplicationTest.class, // TODO needs updates, then enable
               JCATest.class,
               ConnectionManagerMBeanTest.class
})
public class FATSuite {}