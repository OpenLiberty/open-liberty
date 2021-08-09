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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tck;

import org.apache.kafka.common.serialization.Deserializer;

/**
 * A deserializer which always throws an exception
 * <p>
 * Used to test behavior in failing cases.
 */
public class FailingDeserializer implements Deserializer<String> {

    @Override
    public String deserialize(String topic, byte[] data) {
        throw new RuntimeException("Test deserialization exception");
    }

}
