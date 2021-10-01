/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.jpa22;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test cases for functionality introduced with JPA 2.2.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                JPACDIIntegrationTest.class,
                JPA22QueryTest.class,
                JPA22Injection.class,
                JPA22TimeAPITest.class
})
public class JPA22FATSuite {

}
