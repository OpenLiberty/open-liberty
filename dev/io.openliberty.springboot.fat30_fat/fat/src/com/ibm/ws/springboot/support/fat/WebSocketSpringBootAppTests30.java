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
package com.ibm.ws.springboot.support.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class WebSocketSpringBootAppTests30 extends WebSocketAbstractTests {

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-3.0", "servlet-6.0", "websocket-2.1"));
    }

    @Test
    public void testEchoWebSocketSpringBootApp30() throws Exception {
        testEchoWebSocket30();
    }

    @Test
    public void testEchoWithCustomWebsocketHandlerSpringBootApp30() throws Exception {
        testEchoWithCustomWebsocketHandler();
    }
}
