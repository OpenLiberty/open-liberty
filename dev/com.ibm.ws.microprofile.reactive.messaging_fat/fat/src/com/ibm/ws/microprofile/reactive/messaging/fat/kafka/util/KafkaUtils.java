/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.utils;

import java.util.Arrays;
import java.util.List;
import componenttest.topology.impl.LibertyServer;

public class KafkaUtils {

    private final static String KAFKA_REGEX = "E Error.*kafka";

    public static void kafkaStopServer(LibertyServer server, String... ignoredFailuresRegex) throws Exception {
        List<String> failuresRegExps = Arrays.asList(LibertyServer.LIBERTY_ERROR_REGEX, KAFKA_REGEX);
        //booleans are default values you get when calling LibertyServer.stopServer() with no args
        server.stopServer(true, false, true, failuresRegExps, ignoredFailuresRegex);
    }

}
