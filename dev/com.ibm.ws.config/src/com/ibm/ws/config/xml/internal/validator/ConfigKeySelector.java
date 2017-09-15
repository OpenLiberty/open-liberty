/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal.validator;

import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.XMLConfigConstants;

/**
 * Selects the key that can be used to verify the signature of a configuration
 * document.
 * 
 * @since V8.5 feature XXXXXX.
 */
public class ConfigKeySelector extends KeySelector {

    /**
     * Trace component to use for this class.
     */
    private static final TraceComponent tc =
                    Tr.register(ConfigKeySelector.class,
                                XMLConfigConstants.TR_GROUP,
                                XMLConfigConstants.NLS_PROPS);

    /** {@inheritDoc} */
    @Override
    public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose purpose,
                                    AlgorithmMethod method, XMLCryptoContext context)
                    throws KeySelectorException {
        KeySelectorResult result = null;

        // The purpose for the key is not to verify a signature.
        if (!purpose.equals(KeySelector.Purpose.VERIFY))
            throw new KeySelectorException("Key purpose not set to \"verify\"");

        X509Data x509Data = null;
        X509Certificate cert = null;

        // Scans through elements contained in KeyInfo until X509Data 
        // found
        @SuppressWarnings("unchecked")
        List<XMLStructure> content = keyInfo.getContent();
        Iterator<XMLStructure> keyInfoIterator = content.iterator();
        while (keyInfoIterator.hasNext() && x509Data == null) {

            // Gets next element within KeyInfo
            XMLStructure xmlStruct = keyInfoIterator.next();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "select():  xmlStruct = " + xmlStruct);

            // X509Data element found
            if (xmlStruct instanceof X509Data) {

                // Scans through elements contained in X509Data until 
                // X509Certificate found
                x509Data = (X509Data) xmlStruct;
                List<?> x509content = x509Data.getContent();
                Iterator<?> x509DataIterator = x509content.iterator();
                while (x509DataIterator.hasNext() && result == null) {

                    // Gets next element within X509Data
                    Object obj = x509DataIterator.next();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "select():  obj = " + obj);

                    // X509Certificate found
                    if (obj instanceof X509Certificate) {

                        // Gets public key from certificate
                        cert = (X509Certificate) obj;
                        final PublicKey publicKey = cert.getPublicKey();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "select():  publicKey = " + publicKey);

                        // Gets the requested algorithm method
                        String algURI = method.getAlgorithm();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "select():  algURI = " + algURI);

                        // Gets the public key algorithm
                        String algName = publicKey.getAlgorithm();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "select():  algName = " + algName);

                        // Public key algorithm matches requested algorithm method
                        if ((algName.equalsIgnoreCase("DSA") &&
                            algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) ||
                            (algName.equalsIgnoreCase("RSA") &&
                            algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1)))
                            result = new ConfigKeySelectorResult(publicKey);
                    }
                }
            }
        }

        // The key to verify the signature was not found
        if (result == null)
            result = new ConfigKeySelectorResult(null);

        return result;
    }

    /**
     * Returns the result of a <em>verify</em> key selection request.
     */
    public class ConfigKeySelectorResult implements KeySelectorResult {
        /**
         * The key to be used to verify a signature
         * (may be set to <code>null</code>).
         */
        private Key key = null;

        /**
         * Creates a class instance.
         * 
         * @param key The key to be used to verify a signature (may be set to
         *            <code>null</code>).
         */
        public ConfigKeySelectorResult(Key key) {
            this.key = key;
        }

        /** {@inheritDoc} */
        @Override
        public Key getKey() {
            return key;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return getClass().getName() + ": key = " + key;
        }
    }
}
