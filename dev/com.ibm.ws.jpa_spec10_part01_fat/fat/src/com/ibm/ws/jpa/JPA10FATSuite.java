/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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

import com.ibm.ws.jpa.spec10.Callback_EJB;
import com.ibm.ws.jpa.spec10.Callback_Web;
import com.ibm.ws.jpa.spec10.Inheritance_EJB;
import com.ibm.ws.jpa.spec10.Inheritance_Web;
import com.ibm.ws.jpa.spec10.TestOLGH10310_EJB;
import com.ibm.ws.jpa.spec10.TestOLGH10310_Web;

/**
 * Test cases for functionality introduced with JPA 1.0.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                Callback_EJB.class,
                Callback_Web.class,
                Inheritance_EJB.class,
                Inheritance_Web.class,
                TestOLGH10310_Web.class,
                TestOLGH10310_EJB.class
})
public class JPA10FATSuite {

}
