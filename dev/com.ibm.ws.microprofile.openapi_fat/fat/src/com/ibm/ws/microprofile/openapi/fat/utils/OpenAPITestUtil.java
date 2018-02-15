/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat.utils;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;

import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.OpenAPIElement;
import com.ibm.websphere.simplicity.config.OpenAPIElement.WebModuleDocElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;

/**
 * Utility methods used by OpenAPI Tests
 */
public class OpenAPITestUtil {

    private final static Class<?> c = OpenAPITestUtil.class;
    private final static int TIMEOUT = 30000;

    /**
     * Change Liberty features (Mark is set first on log. Then wait for feature updated message using mark)
     *
     * @param server - Liberty server
     * @param features - Liberty features to enable
     * @throws Exception
     */
    public static void changeFeatures(LibertyServer server, String... features) throws Exception {
        List<String> featuresList = Arrays.asList(features);
        server.setMarkToEndOfLog();
        server.changeFeatures(featuresList);
        assertNotNull("Features weren't updated successfully", server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*"));
    }

    /**
     * @param server - Liberty server
     * @param name - The name of a feature to remove e.g. openapi-3.0
     */
    public static void removeFeature(LibertyServer server, String name) {
        try {
            server.setMarkToEndOfLog();
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            if (!features.contains(name))
                return;
            features.remove(name);
            server.updateServerConfiguration(config);
            assertNotNull("Config wasn't updated successfully",
                          server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*"));
        } catch (Exception e) {
            Assert.fail("Unable to remove feature:" + name);
        }
    }

    /**
     * @param server - Liberty server
     * @param name - The name of a feature to add e.g. openapi-3.0
     */
    public static void addFeature(LibertyServer server, String name) {
        try {
            server.setMarkToEndOfLog();
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            if (features.contains(name))
                return;
            features.add(name);
            server.updateServerConfiguration(config);
            assertNotNull("Config wasn't updated successfully",
                          server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*"));
        } catch (Exception e) {
            Assert.fail("Unable to add feature:" + name);
        }
    }

    public static Application removeApplication(LibertyServer server, String appName) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Application webApp = config.getApplications().removeById(appName);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
        assertNotNull("FAIL: App didn't report is has been removed.",
                      server.waitForStringInLogUsingMark("CWWKT0017I.*" + appName));
        assertNotNull("FAIL: App didn't report is has been stopped.",
                      server.waitForStringInLogUsingMark("CWWKZ0009I.*" + appName));

        return webApp;
    }

    /**
     * Adds an application to the current config, or updates an application with
     * a specific name if it already exists
     *
     * @param name the name of the application
     * @param path the fully qualified path to the application archive on the liberty machine
     * @param type the type of the application (ear/war/etc)
     * @return the deployed application
     */
    public static Application addApplication(LibertyServer server, String name, String path, String type) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Application app = config.addApplication(name, path, type);
        ConfigElementList<ClassloaderElement> cel = app.getClassloaders();
        if (cel.isEmpty()) {
            ClassloaderElement ce = new ClassloaderElement();
            ce.setApiTypeVisibility("spec,ibm-api,api,third-party");
            cel.add(ce);
        }
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
        assertNotNull("FAIL: App didn't report is has started.",
                      server.waitForStringInLogUsingMark("CWWKZ0001I.*" + name));
        return app;
    }

    /**
     * Adds an WAR application inside the '${server.config.dir}/apps/'
     * to the current config, or updates an application with a specific name
     * if it already exists
     *
     * @param name the name of the application
     * @return the deployed application
     */
    public static Application addApplication(LibertyServer server, String name) throws Exception {
        return addApplication(server, name, "${server.config.dir}/apps/" + name + ".war", "war");
    }
}
