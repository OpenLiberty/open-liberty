/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.regr.cfg.prop;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import suite.r80.base.jca16.ann.ConfigPropertyMergeActionTest;
import suite.r80.base.jca16.ann.ConfigPropertyValidatorTest;

@RunWith(Suite.class)
@SuiteClasses({
                ConfigPropertyMergeActionTest.class,
                ConfigPropertyValidatorTest.class,
})
public class FATSuite {
}
