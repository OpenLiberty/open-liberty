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
package com.ibm.ws.beanvalidation.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.beanvalidation.fat.basic.BasicValidation11Test;
import com.ibm.ws.beanvalidation.fat.basic.BasicValidation20Test;
import com.ibm.ws.beanvalidation.fat.cdi.BeanValidation11CDITest;
import com.ibm.ws.beanvalidation.fat.cdi.BeanValidation20CDITest;
import com.ibm.ws.beanvalidation.fat.ejb.EJBModule11Test;
import com.ibm.ws.beanvalidation.fat.ejb.EJBModule20Test;

@RunWith(Suite.class)
@SuiteClasses({
                BasicValidation11Test.class,
                EJBModule11Test.class,
                BeanValidation11CDITest.class,
                BasicValidation20Test.class,
                EJBModule20Test.class,
                BeanValidation20CDITest.class
})

public class FATSuite {}
