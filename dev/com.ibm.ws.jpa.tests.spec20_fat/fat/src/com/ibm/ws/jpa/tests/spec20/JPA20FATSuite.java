/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.spec20;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test cases for functionality introduced with JPA 2.0.
 */
@RunWith(Suite.class)
@SuiteClasses({
//                JPA20Example_EJB.class,
//                JPA20Example_WEB.class,

                JPA20Cache_WEB.class,
                JPA20CriteriaQuery_EJB.class,
                JPA20CriteriaQuery_WEB.class,
                JPA20Query_EJB.class,
                JPA20Query_WEB.class,
                JPA20Util_EJB.class,
                JPA20Util_WEB.class,

                JPA20DerivedIdentity_EJB.class,
                JPA20DerivedIdentity_WEB.class
})
public class JPA20FATSuite {

}
