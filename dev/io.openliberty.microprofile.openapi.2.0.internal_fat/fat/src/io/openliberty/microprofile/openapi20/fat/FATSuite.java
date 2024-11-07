/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.microprofile.openapi20.fat.cache.CacheTest;
import io.openliberty.microprofile.openapi20.fat.deployments.DeploymentTest;
import io.openliberty.microprofile.openapi20.fat.deployments.MergeConfigBothTest;
import io.openliberty.microprofile.openapi20.fat.deployments.MergeConfigServerXMLTest;
import io.openliberty.microprofile.openapi20.fat.deployments.MergeConfigTest;
import io.openliberty.microprofile.openapi20.fat.deployments.MergeTest;
import io.openliberty.microprofile.openapi20.fat.deployments.MergeWithServletTest;
import io.openliberty.microprofile.openapi20.fat.deployments.StartupWarningMessagesTest;
import io.openliberty.microprofile.openapi20.fat.shutdown.ShutdownTest;
import io.openliberty.microprofile.openapi20.fat.version.OpenAPIVersionTest;

@RunWith(Suite.class)
@SuiteClasses({
    ApplicationProcessorTest.class,
    CacheTest.class,
    DeploymentTest.class,
    MergeConfigTest.class,
    MergeConfigServerXMLTest.class,
    MergeConfigBothTest.class,
    MergeTest.class,
    MergeWithServletTest.class,
    OpenAPIVersionTest.class,
    ShutdownTest.class,
    StartupWarningMessagesTest.class
})
public class FATSuite {
    public static RepeatTests repeatDefault(String serverName) {
        return MicroProfileActions.repeat(serverName,
                                          MicroProfileActions.MP70_EE10, // mpOpenAPI-4.0, LITE
                                          MicroProfileActions.MP70_EE11, // mpOpenAPI-4.0, FULL
                                          MicroProfileActions.MP61, // mpOpenAPI-3.1, FULL
                                          MicroProfileActions.MP50, // mpOpenAPI-3.0, FULL
                                          MicroProfileActions.MP41);// mpOpenAPI-2.0, FULL
    }

    /**
     * A reduced set of repeats for slow tests where the implementation is common across all feature versions
     * <p>
     * Care should be taken when applying this repeat list to avoid having code paths go untested. If the code in common is not common across all versions of the feature, a more
     * specific list of repeats may be more appropriate.
     * 
     * @param serverName the server which should have its features updated
     * @return the repeat action
     */
    public static RepeatTests repeatReduced(String serverName) {
        return MicroProfileActions.repeat(serverName,
                                          MicroProfileActions.MP70_EE10, // mpOpenAPI-4.0, LITE
                                          MicroProfileActions.MP61);// mpOpenAPI-3.1, FULL
    }
}
