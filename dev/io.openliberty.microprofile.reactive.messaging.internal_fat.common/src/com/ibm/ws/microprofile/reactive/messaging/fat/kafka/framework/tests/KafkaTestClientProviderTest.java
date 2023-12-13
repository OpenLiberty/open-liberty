/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.tests;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClientProvider;

import componenttest.custom.junit.runner.FATRunner;

/**
 * Basic test for KafkaTestClientProvider text encoding
 */
@RunWith(FATRunner.class)
public class KafkaTestClientProviderTest {

    @Test
    public void testEncode() {
        Map<String, String> testProps = new TreeMap<>();
        testProps.put("comma-sep", "a,b,c,d");
        testProps.put("file-path", "C:\\foo\\bar");
        String result = KafkaTestClientProvider.encodeProperties(testProps);

        assertEquals("comma-sep,a\\,b\\,c\\,d,file-path,C:\\\\foo\\\\bar", result);
    }

}
