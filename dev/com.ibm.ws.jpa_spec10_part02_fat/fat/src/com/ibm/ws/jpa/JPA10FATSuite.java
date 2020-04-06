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

import com.ibm.ws.jpa.spec10.Relationships_ManyXMany_EJB;
import com.ibm.ws.jpa.spec10.Relationships_ManyXMany_Web;
import com.ibm.ws.jpa.spec10.Relationships_ManyXOne_EJB;
import com.ibm.ws.jpa.spec10.Relationships_ManyXOne_Web;
import com.ibm.ws.jpa.spec10.Relationships_OneXMany_EJB;
import com.ibm.ws.jpa.spec10.Relationships_OneXMany_Web;
import com.ibm.ws.jpa.spec10.Relationships_OneXOne_EJB;
import com.ibm.ws.jpa.spec10.Relationships_OneXOne_Web;

/**
 * Test cases for functionality introduced with JPA 1.0.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                Relationships_ManyXMany_Web.class,
                Relationships_ManyXMany_EJB.class,
                Relationships_ManyXOne_Web.class,
                Relationships_ManyXOne_EJB.class,
                Relationships_OneXMany_Web.class,
                Relationships_OneXMany_EJB.class,
                Relationships_OneXOne_EJB.class,
                Relationships_OneXOne_Web.class,
})
public class JPA10FATSuite {

}
