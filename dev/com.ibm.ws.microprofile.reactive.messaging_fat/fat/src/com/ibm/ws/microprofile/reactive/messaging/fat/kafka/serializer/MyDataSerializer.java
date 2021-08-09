/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.serializer;

import java.nio.charset.Charset;

import org.apache.kafka.common.serialization.Serializer;

/**
 *
 */
public class MyDataSerializer implements Serializer<MyData> {

    /** {@inheritDoc} */
    @Override
    public byte[] serialize(String topic, MyData data) {
        if (data == null) {
            data = MyData.NULL;
        }
        String dataStr = data.getDataA() + ":" + data.getDataB();
        return dataStr.getBytes(Charset.forName("UTF-8"));
    }

}
