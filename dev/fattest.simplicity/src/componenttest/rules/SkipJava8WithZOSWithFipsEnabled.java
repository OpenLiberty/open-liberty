/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.PrivHelper;

/**
 * Use @SkipForSecurity instead
 */
@Deprecated
public class SkipJava8WithZOSWithFipsEnabled implements TestRule {

    // All tests that must be skipped with this rule must be annotated with the following tag
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE })
    public @interface SkipJava8WithZOSWithFipsEnabledRule {}

    static private final Class<?> thisClass = SkipJava8WithZOSWithFipsEnabled.class;

    // Variables to evaluate the rule
    private final boolean IS_FIPS_140_3_ENABLED = Boolean.parseBoolean(PrivHelper.getProperty("global.client.fips_140-3", "false"));
    private final boolean IS_JAVA_8;
    private final boolean IS_ZOS;

    private final LibertyServer server;
    private final int majorVersion;


    // Constructor to pass through a LibertyServer instance and obtain Java information
    public SkipJava8WithZOSWithFipsEnabled(String server) {
        this.server = LibertyServerFactory.getLibertyServer(server);

        JavaInfo javaInfo = null;
        boolean isZOS = false;
        try {
            javaInfo = obtainJavaInfo(this.server);
            isZOS = (this.server.getMachine().getOperatingSystem() == OperatingSystem.ZOS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.majorVersion = javaInfo.majorVersion();
        this.IS_JAVA_8 = (this.majorVersion == 8);
        this.IS_ZOS = isZOS;

    }

    // constructor that inspects the local jvm where LibertyServer is not available
    public SkipJava8WithZOSWithFipsEnabled(){
        String thisMethod = "SkipJava8WithZOSWithFipsEnabled";
        this.server = null;
        String version = System.getProperty("java.version");
        String[] versionElements = version.split("\\D"); // split on non-digits

        // Pre-JDK 9 the java.version is 1.MAJOR.MINOR
        // Post-JDK 9 the java.version is MAJOR.MINOR
        int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
        this.majorVersion = Integer.valueOf(versionElements[i++]);
        //this.IS_JAVA_8 = Integer.valueOf(versionElements[i++]) == 8 ? true : false;
        this.IS_JAVA_8 = this.majorVersion == 8 ? true : false;
        
        // Check if running on z/OS
        String osName = System.getProperty("os.name");
        this.IS_ZOS = (osName != null && (osName.toLowerCase().contains("z/os") ||
                                          osName.toLowerCase().contains("os/390") ||
                                          osName.toLowerCase().contains("zos")));
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        if (description.getAnnotation(SkipJava8WithZOSWithFipsEnabledRule.class) != null) {

            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    if (!isJava8WithZOSWithFips()) {
                        statement.evaluate();
                    } else {
                        Log.info(description.getTestClass(), description.getMethodName(),
                                 "Test class or method is skipped because environment is Java 8 with ZOS and with FIPS 140-3 Enabled.");
                        Assume.assumeTrue(false);
                    }
                }
            };
        } else {
            return statement;
        }
    }

    // Evaluate if environment is Java 8 with z/OS and FIPS enabled
    public boolean isJava8WithZOSWithFips() {
        return this.IS_JAVA_8 && this.IS_ZOS && IS_FIPS_140_3_ENABLED;
    }


    // Retrieve Java information from server
    public static JavaInfo obtainJavaInfo(LibertyServer server) throws IOException {
        return JavaInfo.forServer(server);
    }

}