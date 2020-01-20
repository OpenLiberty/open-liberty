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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.message;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

@WebServlet("/useConfiguredTopicTest")
public class UseConfiguredTopicServlet extends AbstractTopicServlet {

    @Test
    public void testConfiguredTopic() {
        testTopic(ConfiguredTopicBean.CHANNEL_IN, ConfiguredTopicBean.CONFIGURED_TOPIC, ConfiguredTopicBean.PRODUCER_RECORD_TOPIC, ConfiguredTopicBean.PRODUCER_RECORD_KEY,
                  ConfiguredTopicBean.PRODUCER_RECORD_VALUE);
    }

}
