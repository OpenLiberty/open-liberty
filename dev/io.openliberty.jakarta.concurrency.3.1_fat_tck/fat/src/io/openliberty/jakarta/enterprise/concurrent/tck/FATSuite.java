/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
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
package io.openliberty.jakarta.enterprise.concurrent.tck;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, //Need to have a passing test for Java 8, 11, and 17
                ConcurrentTckLauncherFull.class, //FULL MODE
                ConcurrentTckLauncherWeb.class //LITE MODE
})
public class FATSuite {

    public static boolean shouldRunSignatureTests(Class<?> testClass) {
        /*
         * The Jakarta Concurrency 3.1 TCK uses org.jboss.shrinkwrap.resolver.api.maven.Maven
         * resolver to append the signature plugin to the signature test artifact
         * before it is deployed to the server.
         * The Shrinkwrap Maven resolver does not honor custom repository settings.xml
         * and pulls directly from Maven Central leading to failures due to rate limits.
         *
         * Signature testings is covered in the 3.2 TCK where this resolver was removed.
         */
        return false;
    }
}
