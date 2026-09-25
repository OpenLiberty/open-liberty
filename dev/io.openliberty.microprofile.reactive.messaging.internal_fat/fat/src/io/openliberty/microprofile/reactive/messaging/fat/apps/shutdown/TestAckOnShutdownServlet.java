/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.apps.shutdown;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/TestAckOnShutdown")
public class TestAckOnShutdownServlet extends FATServlet {

    @Inject
    private AckOnShutdownKafkaConsumer consumer;

    @Test
    public void testAckOnShutdown() throws Exception {
        // TODO: implement test
    }

}
