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
package com.ibm.ws.jca.fat.regr;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import suite.r80.base.jca16.ann.ActivationMergeActionTest;
import suite.r80.base.jca16.ann.AdministeredObjectMergeActionTest;
import suite.r80.base.jca16.ann.AdministeredObjectValidatorTest;
import suite.r80.base.jca16.ann.ConnectionDefinitionMergeActionTest;
import suite.r80.base.jca16.ann.ConnectionDefinitionValidatorTest;
import suite.r80.base.jca16.ann.ConnectionDefinitionsMergeActionTest;
import suite.r80.base.jca16.ann.ConnectionDefinitionsValidatorTest;
import suite.r80.base.jca16.ann.ConnectorMergeActionTest;
import suite.r80.base.jca16.ann.ConnectorValidatorTest;
import suite.r80.base.jca16.gwc.GenericWorkContextTest;
import suite.r80.base.jca16.tranlvl.TranLvlTest;

@RunWith(Suite.class)
@SuiteClasses({
                GenericWorkContextTest.class,
                ConnectorMergeActionTest.class,
                TranLvlTest.class,
                ActivationMergeActionTest.class,
                AdministeredObjectMergeActionTest.class,
                AdministeredObjectValidatorTest.class,
                ConnectionDefinitionValidatorTest.class,
                ConnectionDefinitionsValidatorTest.class,
                ConnectorValidatorTest.class,
                ConnectionDefinitionMergeActionTest.class,
                ConnectionDefinitionsMergeActionTest.class,
})
public class FATSuite {
}