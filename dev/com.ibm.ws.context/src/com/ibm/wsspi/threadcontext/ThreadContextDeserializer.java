/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.threadcontext;

import java.io.IOException;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.context.service.serializable.ThreadContextDescriptorImpl;

/**
 * Deserializes thread context descriptors from bytes.
 */
@Trivial
public class ThreadContextDeserializer {
    /**
     * Deserializes a thread context descriptor.
     * 
     * @param bytes bytes obtained from the ThreadContextDescriptor.serialize method.
     * @param execProps execution properties.
     * @return a thread context descriptor.
     * @throws ClassNotFoundException if unable to find a class during deserialization.
     * @throws IOException if an error occurs during deserialization.
     */
    public static ThreadContextDescriptor deserialize(byte[] bytes, Map<String, String> execProps) throws ClassNotFoundException, IOException {
        return new ThreadContextDescriptorImpl(execProps, bytes);
    }
}
