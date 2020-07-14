/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class ApplicationStartedEventTests extends CommonWebServerTests {
    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-4.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_22_APP_BASE;
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        Map<String, String> result = super.getBootStrapProperties();
        result.put("com.ibm.ws.logging.trace.specification", "com.ibm.ws.container.service.state.internal.StateChangeServiceImpl=FINEST");
        return result;
    }

    @Test
    public void testApplicationStartedAfterModuleStarted() throws Exception {
        assertNotNull("No ApplicationStarted event.", server.waitForStringInTraceUsingLastOffset("fireApplicationStarted Entry"));
        assertNull("ModuleStarted event fired after ApplicationStarted event", server.waitForStringInTraceUsingLastOffset("fireModuleStarted Entry", 10000));
    }

}
