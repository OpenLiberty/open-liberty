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

package com.ibm.ws.jpa;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.injection.dfi.JPA10Injection_DFI;
import com.ibm.ws.jpa.injection.dmi.JPA10Injection_DMI;
import com.ibm.ws.jpa.injection.jndi.JPA10Injection_JNDI;

/**
 * Test cases for JPA Injection.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                JPA10Injection_JNDI.class,
                JPA10Injection_DFI.class,
                JPA10Injection_DMI.class
})
public class JPAInjectionFATSuite {

}
