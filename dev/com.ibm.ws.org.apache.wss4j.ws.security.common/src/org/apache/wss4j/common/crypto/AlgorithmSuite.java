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

package org.apache.wss4j.common.crypto;

import java.util.HashSet;
import java.util.Collections;
import java.util.Set;

/**
 * This class holds the permitted values for encryption/signature/etc. algorithms on the
 * inbound side. If the corresponding value is not null then the received algorithm must
 * match the appropriate algorithm stored in this class.
 */
public class AlgorithmSuite {

    private Set<String> signatureMethods = Collections.emptySet();
    private Set<String> c14nAlgorithms = Collections.emptySet();
    private Set<String> digestAlgorithms = Collections.emptySet();
    private Set<String> transformAlgorithms = Collections.emptySet();

    private Set<String> encryptionMethods = Collections.emptySet();
    private Set<String> keyWrapAlgorithms = Collections.emptySet();

    private Set<String> derivedKeyAlgorithms = Collections.emptySet();

    private int maximumSymmetricKeyLength = 256;
    private int minimumSymmetricKeyLength = 128;
    private int maximumAsymmetricKeyLength = 4096;
    private int minimumAsymmetricKeyLength = 1024;
    private int maximumEllipticCurveKeyLength = 512;
    private int minimumEllipticCurveKeyLength = 160;

    private int signatureDerivedKeyLength;
    private int encryptionDerivedKeyLength;

    public void addSignatureMethod(String signatureMethod) {
        if (signatureMethods.isEmpty()) {
            signatureMethods = new HashSet<>();
        }
        signatureMethods.add(signatureMethod);
    }

    public Set<String> getSignatureMethods() {
        return signatureMethods;
    }

    public void addC14nAlgorithm(String c14nAlgorithm) {
        if (c14nAlgorithms.isEmpty()) {
            c14nAlgorithms = new HashSet<>();
        }
        c14nAlgorithms.add(c14nAlgorithm);
    }

    public Set<String> getC14nAlgorithms() {
        return c14nAlgorithms;
    }

    public void addDigestAlgorithm(String digestAlgorithm) {
        if (digestAlgorithms.isEmpty()) {
            digestAlgorithms = new HashSet<>();
        }
        digestAlgorithms.add(digestAlgorithm);
    }

    public Set<String> getDigestAlgorithms() {
        return digestAlgorithms;
    }

    public void addTransformAlgorithm(String transformAlgorithm) {
        if (transformAlgorithms.isEmpty()) {
            transformAlgorithms = new HashSet<>();
        }
        transformAlgorithms.add(transformAlgorithm);
    }

    public Set<String> getTransformAlgorithms() {
        return transformAlgorithms;
    }

    public void addEncryptionMethod(String encryptionMethod) {
        if (encryptionMethods.isEmpty()) {
            encryptionMethods = new HashSet<>();
        }
        encryptionMethods.add(encryptionMethod);
    }

    public Set<String> getEncryptionMethods() {
        return encryptionMethods;
    }

    public void addKeyWrapAlgorithm(String keyWrapAlgorithm) {
        if (keyWrapAlgorithms.isEmpty()) {
            keyWrapAlgorithms = new HashSet<>();
        }
        keyWrapAlgorithms.add(keyWrapAlgorithm);
    }

    public Set<String> getKeyWrapAlgorithms() {
        return keyWrapAlgorithms;
    }

    public void addDerivedKeyAlgorithm(String derivedKeyAlgorithm) {
        if (derivedKeyAlgorithms.isEmpty()) {
            derivedKeyAlgorithms = new HashSet<>();
        }
        derivedKeyAlgorithms.add(derivedKeyAlgorithm);
    }

    public Set<String> getDerivedKeyAlgorithms() {
        return derivedKeyAlgorithms;
    }

    public int getMaximumSymmetricKeyLength() {
        return maximumSymmetricKeyLength;
    }

    public void setMaximumSymmetricKeyLength(int maximumSymmetricKeyLength) {
        this.maximumSymmetricKeyLength = maximumSymmetricKeyLength;
    }

    public int getMinimumAsymmetricKeyLength() {
        return minimumAsymmetricKeyLength;
    }

    public void setMinimumAsymmetricKeyLength(int minimumAsymmetricKeyLength) {
        this.minimumAsymmetricKeyLength = minimumAsymmetricKeyLength;
    }

    public int getMaximumAsymmetricKeyLength() {
        return maximumAsymmetricKeyLength;
    }

    public void setMaximumAsymmetricKeyLength(int maximumAsymmetricKeyLength) {
        this.maximumAsymmetricKeyLength = maximumAsymmetricKeyLength;
    }

    public int getEncryptionDerivedKeyLength() {
        return encryptionDerivedKeyLength;
    }

    public void setEncryptionDerivedKeyLength(int encryptionDerivedKeyLength) {
        this.encryptionDerivedKeyLength = encryptionDerivedKeyLength;
    }

    public int getSignatureDerivedKeyLength() {
        return signatureDerivedKeyLength;
    }

    public void setSignatureDerivedKeyLength(int signatureDerivedKeyLength) {
        this.signatureDerivedKeyLength = signatureDerivedKeyLength;
    }

    public int getMinimumSymmetricKeyLength() {
        return minimumSymmetricKeyLength;
    }

    public void setMinimumSymmetricKeyLength(int minimumSymmetricKeyLength) {
        this.minimumSymmetricKeyLength = minimumSymmetricKeyLength;
    }

    public int getMaximumEllipticCurveKeyLength() {
        return maximumEllipticCurveKeyLength;
    }

    public void setMaximumEllipticCurveKeyLength(int maximumEllipticCurveKeyLength) {
        this.maximumEllipticCurveKeyLength = maximumEllipticCurveKeyLength;
    }

    public int getMinimumEllipticCurveKeyLength() {
        return minimumEllipticCurveKeyLength;
    }

    public void setMinimumEllipticCurveKeyLength(int minimumEllipticCurveKeyLength) {
        this.minimumEllipticCurveKeyLength = minimumEllipticCurveKeyLength;
    }

}