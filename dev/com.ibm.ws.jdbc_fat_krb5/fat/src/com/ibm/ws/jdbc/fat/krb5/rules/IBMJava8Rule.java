/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.krb5.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.JavaInfo.Vendor;

/**
 * Skips tests if we are using IBM Java 8.
 * Otherwise, tests will run
 */
public class IBMJava8Rule implements TestRule {

    public static IBMJava8Rule instance() {
        return new IBMJava8Rule();
    }

    @Override
    public Statement apply(Statement stmt, Description desc) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (shouldRun(desc)) {
                    stmt.evaluate();
                } else {
                    Log.info(IBMJava8Rule.class, "evaluate", "Rule chain broken, skipping next statement: " + stmt.toString());
                }
            }
        };
    }

    public static boolean shouldRun(Description desc) {
        Class<?> c = desc == null ? IBMJava8Rule.class : desc.getTestClass();
        String m = (desc == null || desc.getMethodName() == null) ? "shouldRun" : desc.getMethodName();

        JavaInfo java = JavaInfo.forCurrentVM();
        if (java.majorVersion() == 8 && java.vendor() == Vendor.IBM) {
            Log.info(c, m, "Skipping tests because JDBC driver does not work with IBM JDK 8");
            return false;
        }

        return true;
    }

}
