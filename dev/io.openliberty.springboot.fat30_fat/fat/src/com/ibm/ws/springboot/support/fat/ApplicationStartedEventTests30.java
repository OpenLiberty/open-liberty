/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ApplicationStartedEventTests30 extends CommonWebServerTests {

    /**
     * Override: Add state change service trace enablement to
     * the bootstrap properties.
     *
     * @return The bootstrap properties used by this test class.
     */
    @Override
    public Map<String, String> getBootStrapProperties() {
        Map<String, String> result = super.getBootStrapProperties();
        result.put("com.ibm.ws.logging.trace.specification",
                   "com.ibm.ws.container.service.state.internal.StateChangeServiceImpl=FINEST");
        return result;
    }

    //

    /**
     * Test method: Verify that the application and then the module started.
     */
    @Test
    public void testApplicationStartedAfterModuleStarted() throws Exception {
        requireServerTrace("No ApplicationStarted event.",
                           "fireApplicationStarted Entry");

        forbidServerTrace("ModuleStarted event fired after ApplicationStarted event",
                          "fireModuleStarted Entry", 10000);
    }

    // Full expected event pattern:
    // StateChangeServiceImpl > fireApplicationStarting Entry
    //   ApplicationInfoImpl@1247d83e[io.openliberty.springboot.fat30.app-0.0.1-SNAPSHOT]
    // StateChangeServiceImpl < fireApplicationStarting Exit
    //
    // StateChangeServiceImpl > fireModuleStarting Entry
    //   SpringBootModuleInfo@defffa41
    // StateChangeServiceImpl < fireModuleStarting Exit
    //
    // StateChangeServiceImpl > fireModuleStarted Entry
    //   SpringBootModuleInfo@defffa41
    // StateChangeServiceImpl < fireModuleStarted Exit
    //
    // StateChangeServiceImpl > fireModuleStarting Entry
    //   com.ibm.ws.app.manager.module.internal.WebModuleInfoImpl@9ba3d5d7
    // StateChangeServiceImpl < fireModuleStarting Exit
    //
    // StateChangeServiceImpl > fireModuleStarted Entry
    //   com.ibm.ws.app.manager.module.internal.WebModuleInfoImpl@9ba3d5d7
    // StateChangeServiceImpl < fireModuleStarted Exit
    //
    // StateChangeServiceImpl > fireApplicationStarted Entry
    //   ApplicationInfoImpl@1247d83e[io.openliberty.springboot.fat30.app-0.0.1-SNAPSHOT]
    // StateChangeServiceImpl < fireApplicationStarted Exit
    //
    // --
    //
    // StateChangeServiceImpl > fireApplicationStopping Entry
    //   ApplicationInfoImpl@1247d83e[io.openliberty.springboot.fat30.app-0.0.1-SNAPSHOT]
    // StateChangeServiceImpl < fireApplicationStopping Exit
    //
    // StateChangeServiceImpl > fireModuleStopping Entry
    //   SpringBootModuleInfo@defffa41
    // StateChangeServiceImpl < fireModuleStopping Exit
    //
    // StateChangeServiceImpl > fireModuleStopping Entry
    //   WebModuleInfoImpl@9ba3d5d7
    // StateChangeServiceImpl < fireModuleStopping Exit
    //
    // StateChangeServiceImpl > fireModuleStopped Entry
    //   WebModuleInfoImpl@9ba3d5d7
    // StateChangeServiceImpl < fireModuleStopped Exit
    //
    // StateChangeServiceImpl > fireModuleStopped Entry
    //   SpringBootModuleInfo@defffa41
    // StateChangeServiceImpl < fireModuleStopped Exit
    //
    // StateChangeServiceImpl > fireApplicationStopped Entry
    //   ApplicationInfoImpl@1247d83e[io.openliberty.springboot.fat30.app-0.0.1-SNAPSHOT]
    // StateChangeServiceImpl < fireApplicationStopped Exit
}
