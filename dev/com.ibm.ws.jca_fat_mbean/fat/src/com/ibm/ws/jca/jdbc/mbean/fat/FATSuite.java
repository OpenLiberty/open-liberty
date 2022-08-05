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
package com.ibm.ws.jca.jdbc.mbean.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jca.mbean.fat.app.ConnectionPoolStatsTest;
import com.ibm.ws.jca.mbean.fat.app.JCA_JDBC_JSR77_MBeanTest;
import com.ibm.ws.jca.mbean.fat.app.JCA_JDBC_JSR77_MBean_ExtendedTest;
import com.ibm.ws.jca.mbean.fat.app.JCA_JDBC_JSR77_MBean_MultipleTest;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;

@RunWith(Suite.class)
@SuiteClasses({
                JCA_JDBC_JSR77_MBeanTest.class,
                JCA_JDBC_JSR77_MBean_ExtendedTest.class,
                JCA_JDBC_JSR77_MBean_MultipleTest.class,
                ConnectionPoolStatsTest.class

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