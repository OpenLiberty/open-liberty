/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jca.fat.regr.rar.beanVal;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import suite.r80.base.jca16.jbv.RarBeanValidationTest11;
import suite.r80.base.jca16.jbv.RarBeanValidationTest20;

@RunWith(Suite.class)
@SuiteClasses({
                RarBeanValidationTest11.class,
                RarBeanValidationTest20.class,
})
public class FATSuite {
}
