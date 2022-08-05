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
package com.ibm.ws.jca.fat.regr;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jca.fat.regr.app.InboundSecurityTest;
import com.ibm.ws.jca.fat.regr.app.InboundSecurityTestRapid;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
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
                InboundSecurityTestRapid.class,
                InboundSecurityTest.class,
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
    @ClassRule
    public static RepeatTests repeat;

    static {
        // EE10 requires Java 11.  If we only specify EE10 for lite mode it will cause no tests to run which causes an error.
        // If we are running on Java 8 have EE9 be the lite mode test to run.
        if (JavaInfo.JAVA_VERSION >= 11) {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            // need widen option to handle jar file within a jar file.
                            .andWith(new JakartaEE9Action().fullFATOnly().withWiden())
                            // need widen option to handle jar file within a jar file.
                            .andWith(new JakartaEE10Action().withWiden());
        } else {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            // need widen option to handle jar file within a jar file.
                            .andWith(new JakartaEE9Action().withWiden());
        }
    }
}