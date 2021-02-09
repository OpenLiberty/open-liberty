/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.test.servlets;

import static org.junit.Assert.assertNotNull;

import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Applies and asserts changes to the test server configuration.
 */
public class TestConfiguration {

    private static final int WAIT_TIME = 10000;

    private final Class<?> logClass;
    private final LibertyServer server;
    private final TestName name;
    private final String application;

    private String currentConfigFile;

    public TestConfiguration(LibertyServer server, Class<?> logClass, TestName name, String application) {
        this.server = server;
        this.name = name;
        this.logClass = logClass;
        this.application = application;
    }

    /**
     * Starts the server with the configuration file that has security enabled,
     * performs common security feature assertions, that the application started,
     * and asserts that the specified features are started.
     *
     * @throws Exception
     */
    public void startServerWithSecurityAndAppStarted(String configFile, String... featuresRegExps) throws Exception {
        startServerWithSecurityAndAppStarted(configFile);
        assertOtherFeaturesStarted(featuresRegExps);
    }

    /**
     * Starts the server with the configuration file that has security enabled,
     * performs common security feature assertions, and that application started.
     *
     * @throws Exception
     */
    public void startServerWithSecurityAndAppStarted(String configFile) throws Exception {
        startServer(configFile);
        assertSecurityFeatureExpectations();
        assertApplicationStarted();
    }

    /**
     * Starts the server with the given configuration.
     *
     * @throws Exception
     */
    private void startServer(String configFile) throws Exception {
        server.setServerConfigurationFile(configFile);
        server.startServer(name.getMethodName() + ".log");
        currentConfigFile = configFile;
    }

    /**
     * Asserts that other features are started.
     *
     * @param featuresRegExps
     * @throws Exception
     */
    private void assertOtherFeaturesStarted(String[] featuresRegExps) throws Exception {
        for (String featureRegularExpression : featuresRegExps) {
            server.waitForStringInLogUsingMark(featureRegularExpression, WAIT_TIME);
        }
    }

    /**
     * Starts the server with the configuration file that has no security enabled
     * and asserts that the application started.
     *
     * @throws Exception
     */
    public void startServerWithNoSecurityAndAppStarted(String configFile) throws Exception {
        startServer(configFile);
        assertApplicationStarted();
    }

    /**
     * Starts the server with the given configuration using the --clean parameter.
     *
     * @param configFile
     * @throws Exception
     */
    public void startServerClean(String configFile) throws Exception {
        server.setServerConfigurationFile(configFile);
        currentConfigFile = configFile;
        server.startServer(true);
        assertSecurityFeatureExpectations();
        assertApplicationStarted();
    }

    /**
     * Performs assertions common to enabling the security feature.
     */
    private void assertSecurityFeatureExpectations() {
        assertFeatureUpdateCompleted();
        assertSecurityServiceReady();
    }

    /**
     * Asserts that the FeatureManager updates are complete.
     */
    private void assertFeatureUpdateCompleted() {
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
    }

    /**
     * Asserts that the SecurityService is ready.
     */
    private void assertSecurityServiceReady() {
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLogUsingMark("CWWKS0008I"));
    }

    /**
     * Asserts that the application specified was updated.
     */
    public void assertApplicationUpdated() {
        assertNotNull("The application " + application + " should have been updated",
                      waitForAppUpdate(application));
    }

    /**
     * Asserts that the application is started. This message is issued only once
     * in the server life cycle unless the application is reinstalled.
     */
    public void assertApplicationStarted() {
        assertNotNull("The application " + application + " should have started",
                      server.waitForStringInLogUsingMark("CWWKZ0001I.* " + application));
    }

    /**
     * Asserts that the application is stopped.
     */
    public void assertApplicationStopped() {
        assertNotNull("The application " + application + " should have stopped",
                      server.waitForStringInLogUsingMark("CWWKZ0009I.* " + application));
    }

    /**
     * Enables the security feature dynamically.
     * Assert common security expectations, that the application was updated,
     * and asserts that the specified features are started.
     *
     * @throws Exception
     */
    public void enableSecurity(String configEnablingSecurity, String... featuresRegExps) throws Exception {
        enableSecurity(configEnablingSecurity);
        assertOtherFeaturesStarted(featuresRegExps);
    }

    /**
     * Enables the security feature dynamically.
     * Assert common security expectations and that the application was updated.
     *
     * @throws Exception
     */
    public void enableSecurity(String configEnablingSecurity) throws Exception {
        updateServerConfig(configEnablingSecurity);
        assertSecurityFeatureExpectations();
        assertApplicationUpdated();
    }

    /**
     * @param newConfig
     * @throws Exception
     */
    private void updateServerConfig(String newConfig) throws Exception {
        Log.info(TestConfiguration.class, "updateServerConfig", "setServerConfigurationFile to : " + newConfig);
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(newConfig);
        currentConfigFile = newConfig;
    }

    /**
     * Disables the security feature dynamically.
     * Performs assertions common to disabling the security feature.
     *
     * @throws Exception
     */
    public void disableSecurity(String configDisablingSecurity) throws Exception {
        updateServerConfig(configDisablingSecurity);
        assertFeatureUpdateCompleted();
        assertApplicationUpdated();
    }

    public void logTestEntry() {
        Log.info(logClass, name.getMethodName(), "Entering test " + name.getMethodName());
    }

    public void logTestExit() {
        Log.info(logClass, name.getMethodName(), "Exiting test " + name.getMethodName());
    }

    /**
     * Sets the server configuration only if it is different than the current config.
     * note that this method does not wait for the completion of the config update (CWWKG0017I message),
     * so unless there is any specific reason, use setServerConfiguration method instead.
     *
     * @param newConfig
     * @throws Exception
     */
    public void setDifferentConfig(String newConfig) throws Exception {
        if (isDifferentConfig(newConfig)) {
            updateServerConfig(newConfig);
            waitForServerConfigurationUpdate();
        }
    }

    /**
     * Sets a different server configuration that modifies the webAppSecurity attributes.
     *
     * @param newConfig
     * @throws Exception
     */
    public void modifyWebAppSecurity(String newConfig) throws Exception {
        if (isDifferentConfig(newConfig)) {
            updateServerConfig(newConfig);
            waitForPropertiesUpdate();
            assertServerConfigurationCompleted();
            assertAppUpdatedIfStopped();
        }
    }

    /**
     * Asserts that the server configuration was completed.
     */
    public void assertServerConfigurationCompleted() {
        assertNotNull("The server config change was not completed",
                      waitForServerConfigurationUpdate());
    }

    /**
     * Asserts that the application updated if it was stopped.
     */
    private void assertAppUpdatedIfStopped() {
        if (wasAppStopped()) {
            assertApplicationUpdated();
        }
    }

    /**
     * Determines if the application was stopped.
     * Only search for 2 seconds since this should be quickly found from the last mark
     * if the application was stopped. Increase this time if for some reason it takes longer
     * than the current timeout to search for and there was an instance missed.
     */
    private boolean wasAppStopped() {
        return server.waitForStringInLogUsingMark("CWWKZ0009I.* " + application, 2000) != null;
    }

    /**
     * Sets a different server configuration waiting for the configuration to update.
     *
     * @param newConfig
     * @throws Exception
     */
    public void setServerConfiguration(String newConfig) throws Exception {
        if (isDifferentConfig(newConfig)) {
            updateServerConfig(newConfig);
            waitForServerConfigurationUpdate();
        }
    }

    /**
     * Sets a different server configuration waiting for the configuration and application to update.
     *
     * @param newConfig
     * @throws Exception
     */
    public void setServerConfiguration(String newConfig, String application) throws Exception {
        if (isDifferentConfig(newConfig)) {
            updateServerConfig(newConfig);
            assertNotNull("Expected to see server configuration was successfully updated",
                          waitForServerConfigurationUpdate());
            assertNotNull("Expected to see application " +application+ " updated",
                          waitForAppUpdate(application));
        }
    }

    private boolean isDifferentConfig(String newConfig) {
        return currentConfigFile == null || !currentConfigFile.equals(newConfig);
    }

    /**
     * Waits for the web security configuration properties to be modified.
     */
    public void waitForPropertiesUpdate() {
        server.waitForStringInLogUsingMark("CWWKS9112A");
    }

    /**
     * Waits for the application to be updated.
     */
    private String waitForAppUpdate(String application) {
        return server.waitForStringInLogUsingMark("CWWKZ0003I.* " + application);
    }

    /**
     * Waits for the server configuration to be updated.
     */
    private String waitForServerConfigurationUpdate() {
        return server.waitForStringInLogUsingMark("CWWKG0017I");
    }

}
