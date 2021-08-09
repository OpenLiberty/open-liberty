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

import org.apache.kafka.common.serialization.Deserializer;

/**
 *
 */
public class MyDataDeserializer implements Deserializer<MyData> {

    /** {@inheritDoc} */
    @Override
    public MyData deserialize(String topic, byte[] data) {
        String dataStr = new String(data, Charset.forName("UTF-8"));
        String[] dataArr = dataStr.split(":");
        String dataA = dataArr[0];
        String dataB = dataArr[1];
        MyData myData = new MyData(dataA, dataB);

        if (myData.equals(MyData.NULL)) {
            myData = MyData.NULL;
        }
        return myData;
    }

}
