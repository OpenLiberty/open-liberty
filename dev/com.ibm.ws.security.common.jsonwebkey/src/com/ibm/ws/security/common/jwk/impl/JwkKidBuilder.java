package com.ibm.ws.security.common.jwk.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class JwkKidBuilder {

    public JwkKidBuilder() {

    }

    public String buildKeyId(PublicKey cert) {
        if (cert != null && cert.getEncoded() != null) {
            byte[] certhash = null;
            try {
                certhash = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
            } catch (NoSuchAlgorithmException e) {
            }
            if (certhash != null) {
                return org.jose4j.base64url.Base64Url.encode(certhash);
            }
        }
        return null;
    }
}
