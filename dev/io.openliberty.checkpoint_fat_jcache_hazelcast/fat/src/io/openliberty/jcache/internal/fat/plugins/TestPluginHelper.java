/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.jcache.internal.fat.plugins;

/**
 * Utility class to set and get the {@link TestPlugin} implementation to use during testing.
 */
public class TestPluginHelper {
    private static TestPlugin testPlugin;

    /**
     * Set the configured {@link TestPlugin} implementation.
     *
     * @param testPlugin The {@link TestPlugin} to use.
     */
    public static void setTestPlugin(TestPlugin testPlugin) {
        TestPluginHelper.testPlugin = testPlugin;
    }

    /**
     * Get the configured {@link TestPlugin} implementation.
     *
     * @return The {@link TestPlugin} to use.
     */
    public static TestPlugin getTestPlugin() {
        return testPlugin;
    }

    /**
     * Is the currently configured test plugin configured for Hazelcast?
     *
     * @return True if the plugin is configured for Hazelcast.
     */
    public static boolean isHazelcast() {
        return testPlugin != null && testPlugin.getClass().getName().contains("Hazelcast");
    }

    /**
     * Is the currently configured test plugin configured for Infinispan?
     *
     * @return True if the plugin is configured for Infinispan.
     */
    public static boolean isInfinispan() {
        return testPlugin != null && testPlugin.getClass().getName().contains("Infinispan");
    }
}
