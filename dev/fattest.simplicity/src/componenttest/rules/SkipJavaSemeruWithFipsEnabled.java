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

package componenttest.rules;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.PrivHelper;

/**
 * Use @SkipForSecurity instead
 */
@Deprecated
public class SkipJavaSemeruWithFipsEnabled implements TestRule {

    // All tests that must be skipped with this rule must be annotated with the following tag
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE })
    public @interface SkipJavaSemeruWithFipsEnabledRule {}

    // Variables to evaluate the rule
    private final boolean IS_FIPS_140_3_ENABLED = Boolean.parseBoolean(PrivHelper.getProperty("global.client.fips_140-3", "false"));
    private final boolean IS_SEMERU_JAVA;

    private final LibertyServer server;
    private final int majorVersion;

    // Constructor to pass through a LibertyServer instance and obtain Java information
    public SkipJavaSemeruWithFipsEnabled(String server) {
        this.server = LibertyServerFactory.getLibertyServer(server);

        JavaInfo javaInfo = null;
        try {
            javaInfo = obtainJavaInfo(this.server);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.IS_SEMERU_JAVA = javaInfo.runtimeName().contains("Semeru");
        this.majorVersion = javaInfo.majorVersion();

    }

    @Override
    public Statement apply(Statement statement, Description description) {
        if (description.getAnnotation(SkipJavaSemeruWithFipsEnabledRule.class) != null) {

            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    if (!isSemeruWithFips()) {
                        statement.evaluate();
                    } else {
                        Log.info(description.getTestClass(), description.getMethodName(),
                                 "Test class or method is skipped because environment is Java Semeru with FIPS 140-3 Enabled. \n Machine Java Version: " + JavaInfo.JAVA_VERSION
                                                                                          + "\n Server Java Version " + majorVersion);
                        Assume.assumeTrue(false);
                    }
                }
            };
        } else {
            return statement;
        }
    }

    // Evaluate if environment Semeru with FIPS enabled
    public boolean isSemeruWithFips() {
        return this.IS_SEMERU_JAVA && IS_FIPS_140_3_ENABLED;
    }

    // Retrieve Java information from server
    public static JavaInfo obtainJavaInfo(LibertyServer server) throws IOException {
        return JavaInfo.forServer(server);
    }
}