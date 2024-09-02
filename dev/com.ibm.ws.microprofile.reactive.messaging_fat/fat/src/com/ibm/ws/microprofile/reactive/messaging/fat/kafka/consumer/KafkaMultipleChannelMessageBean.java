/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.consumer;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class KafkaMultipleChannelMessageBean {

    public static final String CHANNEL_IN_1 = "channel_in_1";
    public static final String CHANNEL_OUT_1 = "channel_out_1";

    public static final String CHANNEL_IN_2 = "channel_in_2";
    public static final String CHANNEL_OUT_2 = "channel_out_2";

    public static final String CHANNEL_IN_3 = "channel_in_3";
    public static final String CHANNEL_OUT_3 = "channel_out_3";

    public static final String CHANNEL_IN_4 = "channel_in_4";
    public static final String CHANNEL_OUT_4 = "channel_out_4";

    public static final String CHANNEL_IN_5 = "channel_in_5";
    public static final String CHANNEL_OUT_5 = "channel_out_5";

    public static final String CHANNEL_IN_6 = "channel_in_6";
    public static final String CHANNEL_OUT_6 = "channel_out_6";

    public static final String CHANNEL_IN_7 = "channel_in_7";
    public static final String CHANNEL_OUT_7 = "channel_out_7";


    @Incoming(CHANNEL_IN_1)
    @Outgoing(CHANNEL_OUT_1)
    public String processor1(String value){
        return processInput(value, CHANNEL_IN_1);
    }

    @Incoming(CHANNEL_IN_2)
    @Outgoing(CHANNEL_OUT_2)
    public String processor2(String value){
        return processInput(value, CHANNEL_IN_2);
    }

    @Incoming(CHANNEL_IN_3)
    @Outgoing(CHANNEL_OUT_3)
    public String processor3(String value){
        return processInput(value, CHANNEL_IN_3);
    }

    @Incoming(CHANNEL_IN_4)
    @Outgoing(CHANNEL_OUT_4)
    public String processor4(String value){ return processInput(value, CHANNEL_IN_4); }

    @Incoming(CHANNEL_IN_5)
    @Outgoing(CHANNEL_OUT_5)
    public String processor5(String value){ return processInput(value, CHANNEL_IN_5); }

    @Incoming(CHANNEL_IN_6)
    @Outgoing(CHANNEL_OUT_6)
    public String processor6(String value){
        return processInput(value, CHANNEL_IN_6);
    }

    @Incoming(CHANNEL_IN_7)
    @Outgoing(CHANNEL_OUT_7)
    public String processor7(String value){
        return processInput(value, CHANNEL_IN_7);
    }

    public String processInput(String value, String channelName){
        return channelName + "-"+value;
    }

}
