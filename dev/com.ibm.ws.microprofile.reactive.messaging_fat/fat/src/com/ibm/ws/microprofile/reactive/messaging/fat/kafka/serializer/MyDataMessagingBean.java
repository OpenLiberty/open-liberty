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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.serializer;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class MyDataMessagingBean {

    public static final String IN_CHANNEL = "data-in";
    public static final String OUT_CHANNEL = "data-out";
    public static final String GROUP_ID = "data-consumer";

    @Incoming(IN_CHANNEL)
    @Outgoing(OUT_CHANNEL)
    public MyData reverseString(MyData in) {
        return in.reverse();
    }

}
