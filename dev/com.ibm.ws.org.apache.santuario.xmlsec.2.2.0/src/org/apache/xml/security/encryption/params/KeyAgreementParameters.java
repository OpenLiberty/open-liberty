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
package org.apache.xml.security.encryption.params;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;

/**
 * This class is used to pass parameters to the KeyAgreement algorithm.
 */
public class KeyAgreementParameters implements AlgorithmParameterSpec {
    /**
     * This enum defines the actor type of the KeyAgreement algorithm. The actor type defines which public and which
     * private key is expected to be present for the KeyAgreement algorithm to define derived key.
     * <ul>
     *     <li>ORIGINATOR: The key agreement is used by originator of the message. Therefore, the originator private key and
     *     the recipient public key are expected to be present.</li>
     *     <li>RECIPIENT: The key agreement is used by recipient of the message. Therefore, the recipient private key and
     *     the originator public key are expected to be present.</li>
     * </ul>
     */
    public enum ActorType {
        ORIGINATOR,
        RECIPIENT
    }

    private final KeyDerivationParameters KeyDerivationParameter;
    private final  ActorType actorType;
    private final String keyAgreementAlgorithm;

    private PublicKey originatorPublicKey;
    private PrivateKey originatorPrivateKey;
    private PublicKey recipientPublicKey;
    private PrivateKey recipientPrivateKey;


    public KeyAgreementParameters(ActorType actorType, String keyAgreementAlgorithm, KeyDerivationParameters keyDerivationParameter) {
        this.actorType = actorType;
        this.KeyDerivationParameter = keyDerivationParameter;
        this.keyAgreementAlgorithm = keyAgreementAlgorithm;
    }

    public KeyDerivationParameters getKeyDerivationParameter() {
        return KeyDerivationParameter;
    }

    public String getKeyAgreementAlgorithm() {
        return keyAgreementAlgorithm;
    }

    public void setOriginatorKeyPair(KeyPair originatorKeyPair) {
        this.originatorPublicKey = originatorKeyPair.getPublic();
        this.originatorPrivateKey = originatorKeyPair.getPrivate();
    }

    public PublicKey getOriginatorPublicKey() {
        return originatorPublicKey;
    }

    public void setOriginatorPublicKey(PublicKey originatorPublicKey) {
        this.originatorPublicKey = originatorPublicKey;
    }

    public PrivateKey getOriginatorPrivateKey() {
        return originatorPrivateKey;
    }

    public void setOriginatorPrivateKey(PrivateKey originatorPrivateKey) {
        if (actorType != ActorType.ORIGINATOR) {
            throw new IllegalStateException("Cannot set originator private key when actor type is not ORIGINATOR");
        }
        this.originatorPrivateKey = originatorPrivateKey;
    }

    public PublicKey getRecipientPublicKey() {
        return recipientPublicKey;
    }

    public void setRecipientPublicKey(PublicKey recipientPublicKey) {
        if (actorType != ActorType.ORIGINATOR) {
            throw new IllegalStateException("Cannot set recipient public key when actor type is not ORIGINATOR");
        }
        this.recipientPublicKey = recipientPublicKey;
    }

    public PrivateKey getRecipientPrivateKey() {
        return recipientPrivateKey;
    }

    public void setRecipientPrivateKey(PrivateKey recipientPrivateKey) {
        if (actorType != ActorType.RECIPIENT) {
            throw new IllegalStateException("Cannot set recipient private key when actor type is not RECIPIENT");
        }
        this.recipientPrivateKey = recipientPrivateKey;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public PublicKey getAgreementPublicKey() {
        return actorType == ActorType.ORIGINATOR ? recipientPublicKey : originatorPublicKey;
    }

    public PrivateKey getAgreementPrivateKey() {
        return actorType == ActorType.ORIGINATOR ? originatorPrivateKey : recipientPrivateKey;
    }
}
