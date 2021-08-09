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
package com.ibm.ws.testtooling.msgcli;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.testtooling.testinfo.TestSessionSignature;

/**
 * Messaging Payload carrier for use by tests.
 *
 */
public class DataPacket implements Serializable {
    private static final long serialVersionUID = 3560329086534240176L;

    private TestSessionSignature testSessionSig;
    private Map<String, Serializable> properties;
    private Serializable payload;

    public DataPacket() {
        properties = new HashMap<String, Serializable>();
    }

    public DataPacket(TestSessionSignature testSessionSig,
                      Map<String, Serializable> properties, Serializable payload) {
        super();

        if (properties != null) {
            this.properties = new HashMap<String, Serializable>(properties);
        } else {
            this.properties = new HashMap<String, Serializable>();
        }

        this.testSessionSig = testSessionSig;
        this.payload = payload;
    }

    public TestSessionSignature getTestSessionSig() {
        return testSessionSig;
    }

    public void setTestSessionSig(TestSessionSignature testSessionSig) {
        this.testSessionSig = testSessionSig;
    }

    public Map<String, Serializable> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

    public Serializable getPayload() {
        return payload;
    }

    public void setPayload(Serializable payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "TestDataPacket [testSessionSig=" + testSessionSig
               + ", properties=" + properties + ", payload=" + payload + "]";
    }
}