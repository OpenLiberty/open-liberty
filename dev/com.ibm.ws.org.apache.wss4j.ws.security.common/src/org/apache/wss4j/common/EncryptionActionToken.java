/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.wss4j.common;


/**
 * This class encapsulates configuration for Encryption Actions.
 */
public class EncryptionActionToken extends SignatureEncryptionActionToken {

    private boolean encSymmetricEncryptionKey = true;
    private String mgfAlgorithm;
    private String symmetricAlgorithm;
    private String keyTransportAlgorithm;
    private boolean getSymmetricKeyFromCallbackHandler;

    public boolean isEncSymmetricEncryptionKey() {
        return encSymmetricEncryptionKey;
    }
    public void setEncSymmetricEncryptionKey(boolean encSymmetricEncryptionKey) {
        this.encSymmetricEncryptionKey = encSymmetricEncryptionKey;
    }
    public String getMgfAlgorithm() {
        return mgfAlgorithm;
    }
    public void setMgfAlgorithm(String mgfAlgorithm) {
        this.mgfAlgorithm = mgfAlgorithm;
    }
    public String getSymmetricAlgorithm() {
        return symmetricAlgorithm;
    }
    public void setSymmetricAlgorithm(String symmetricAlgorithm) {
        this.symmetricAlgorithm = symmetricAlgorithm;
    }
    public String getKeyTransportAlgorithm() {
        return keyTransportAlgorithm;
    }
    public void setKeyTransportAlgorithm(String keyTransportAlgorithm) {
        this.keyTransportAlgorithm = keyTransportAlgorithm;
    }
    public boolean isGetSymmetricKeyFromCallbackHandler() {
        return getSymmetricKeyFromCallbackHandler;
    }
    public void setGetSymmetricKeyFromCallbackHandler(boolean getSymmetricKeyFromCallbackHandler) {
        this.getSymmetricKeyFromCallbackHandler = getSymmetricKeyFromCallbackHandler;
    }

}

