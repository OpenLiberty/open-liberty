/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
public class ExceptionOccuredAfterAppIsAvailableTest20 extends AbstractSpringTests {

    @Override
    public boolean expectApplicationSuccess() {
        return false;
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        List<String> appArgs = appConfig.getApplicationArguments();
        appArgs.add("--throw.application.exception=true");
    }

    @Test
    public void testSpringAppWithExceptionInMainMethod() throws Exception {
        //When an exception occurs after the application is available on an endpoint, it should be removed before closing the context or stopping the server.
        assertNotNull("The endpoint should be removed", server
                        .waitForStringInLog("CWWKT0017I:.*"));
        assertNotNull("Error message is not displayed", server
                        .waitForStringInLog("CWWKZ0002E:.*"));

        //Remove the application argument "--throw.application.exception=true" from server configuration
        ServerConfiguration config = server.getServerConfiguration();
        SpringBootApplication app = config.getSpringBootApplications().iterator().next();
        app.getApplicationArguments().remove("--throw.application.exception=true");

        //Set mark to end so we can test the messages after we add the dropins app back
        server.setMarkToEndOfLog();

        //Application restarts on updating the server configuration
        server.updateServerConfiguration(config);
        assertNotNull("The endpoint is not available", server
                        .waitForStringInLog("CWWKT0016I:.*"));
        assertNotNull("The application was not updated", server
                        .waitForStringInLog("CWWKZ0003I:.*"));
        stopServer(true, "CWWKZ0002E");
    }
}
