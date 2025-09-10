/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.xmlsec.agreement.impl;

import java.util.List;
import java.util.ServiceLoader;

import javax.annotation.Nonnull;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.xmlsec.agreement.KeyAgreementException;
import org.opensaml.xmlsec.agreement.KeyAgreementParameters;
import org.opensaml.xmlsec.agreement.KeyAgreementSupport;
import org.opensaml.xmlsec.encryption.AgreementMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A component which parses an instance of {@link AgreementMethod} and produces a new instance
 * of {@link KeyAgreementParameters}.
 */
public class KeyAgreementParametersParser {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(KeyAgreementParametersParser.class);

    /**
     * Parse the specified {@link AgreementMethod} into an instance of {@link KeyAgreementParameters}.
     *
     * @param agreementMethod the AgreementMethod to process
     *
     * @return the new instance of key agreement parameters
     *
     * @throws KeyAgreementException if parameters parsing does not complete successfully
     */
    @Nonnull
    public KeyAgreementParameters parse(@Nonnull final AgreementMethod agreementMethod) throws KeyAgreementException {
        Constraint.isNotNull(agreementMethod, "AgreementMethod was null");

        final KeyAgreementParameters parameters = new KeyAgreementParameters();

        final List<KeyAgreementParameterParser> parsers = getParsers();

        final List<XMLObject> xmlChildren = Lists.newArrayList(agreementMethod.getUnknownXMLObjects());

        // KANonce is the only parameter with a "named" slot on AgreementMethod, so handle it specifically
        if (agreementMethod.getKANonce() != null) {
            xmlChildren.add(agreementMethod.getKANonce());
        }

        for (final XMLObject xmlChild : xmlChildren) {

            boolean handled = false;
            for (final KeyAgreementParameterParser parser : parsers) {
                if (parser.handles(xmlChild)) {
                    log.debug("AgreementMethod child '{}' was indicated to be handled by: {}",
                              xmlChild.getElementQName(), parser.getClass().getName());
                    parameters.add(parser.parse(xmlChild));
                    handled = true;
                    continue;
                }
            }
            if (!handled) {
                throw new KeyAgreementException("AgreementMethod child is not a supported parameter type: "
                                                + xmlChild.getElementQName());
            }
        }

        // The grandparent's EncryptionMethod KeySize element is an implicit parameter to the agreement operation
        final Integer keySize = KeyAgreementSupport.getExplicitKeySize(agreementMethod);
        if (keySize != null) {
            parameters.add(new KeySize(keySize));
        }

        parameters.initializeAll();

        return parameters;
    }

    /**
     * Obtain the list of {@link KeyAgreementParameterParser} instances to use.
     *
     * <p>
     * This implementation uses the Java Service API to load the instances. Subclasses may override.
     * </p>
     *
     * @return the list of parser instances
     */
    @Nonnull
    protected List<KeyAgreementParameterParser> getParsers() {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try { // Liberty Start
            Thread.currentThread().setContextClassLoader(KeyAgreementParameterParser.class.getClassLoader());
            final ServiceLoader<KeyAgreementParameterParser> loader = ServiceLoader.load(KeyAgreementParameterParser.class);
            return Lists.newArrayList(loader);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        } // Liberty End
    }
}
