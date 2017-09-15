/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.jwt;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This is a class that facilitates validating and parsing JSON Web Tokens.
 * 
 * @author IBM Corporation
 * 
 * @version 1.0
 * 
 * @since 1.0
 * 
 * @ibm-api
 */
@Component(service = JwtConsumer.class, name = "com.ibm.websphere.security.jwt.JwtConsumer", immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class JwtConsumer {

    /**
     * The ID used for the default {@code jwtConsumer} server configuration element that it implicitly included with the
     * {@code jwt-1.0} feature.
     */
    public final static String DEFAULT_ID = "defaultJwtConsumer";

    private Consumer consumer;

    /*********************************** Begin OSGi-related fields and methods ***********************************/

    private static final String KEY_JWT_CONSUMER_SERVICE = "consumer";
    private static AtomicServiceReference<Consumer> consumerServiceRef = new AtomicServiceReference<Consumer>(KEY_JWT_CONSUMER_SERVICE);

    @Reference(service = Consumer.class, name = KEY_JWT_CONSUMER_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.RELUCTANT)
    protected void setConsumer(ServiceReference<Consumer> ref) {
        consumerServiceRef.setReference(ref);
    }

    protected void unsetConsumer(ServiceReference<Consumer> ref) {
        consumerServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        consumerServiceRef.activate(cc);
    }

    @Modified
    protected void modify(Map<String, Object> properties) {

    }

    @Deactivate
    protected void deactivate(int reason, ComponentContext cc) {
        consumerServiceRef.deactivate(cc);
    }

    /*********************************** End OSGi-related fields and methods ***********************************/

    public JwtConsumer() {
    }

    public JwtConsumer(String consumerConfigId) throws InvalidConsumerException {
        if (consumerConfigId == null) {
            consumerConfigId = DEFAULT_ID;
        }
        consumer = getTheService().create(consumerConfigId);
    }

    private Consumer getTheService() {
        return consumerServiceRef.getService();
    }

    /**
     * Creates a new {@code JwtConsumer} object using the default configuration ID {@value #DEFAULT_ID}.
     * 
     * @return A new {@code JwtConsumer} object tied to the {@code jwtConsumer} server configuration element with the default ID
     *         {@value #DEFAULT_ID}.
     * @throws InvalidConsumerException
     *             Thrown if the JWT consumer service is not available.
     */
    public synchronized static JwtConsumer create() throws InvalidConsumerException {
        return create(DEFAULT_ID);
    }

    /**
     * Creates a new {@code JwtConsumer} object using the configuration ID provided.
     * 
     * @param consumerConfigId
     *            ID of a corresponding {@code jwtConsumer} element in {@code server.xml}. If {@code null}, the default
     *            configuration ID {@value #DEFAULT_ID} will be used.
     * @return A new {@code JwtConsumer} object tied to the {@code jwtConsumer} server configuration element whose {@code id}
     *         attribute matches the ID provided.
     * @throws InvalidConsumerException
     *             Thrown if the JWT consumer service is not available.
     */
    public synchronized static JwtConsumer create(String consumerConfigId) throws InvalidConsumerException {
        return new JwtConsumer(consumerConfigId);
    }

    /**
     * Creates a new {@code JwtToken} object based on the provided encoded token string. The token string is processed based on
     * the configuration for the {@code jwtConsumer} element that is specified in {@code server.xml} that matches the ID used to
     * instantiate this {@code JwtConsumer} object.
     * 
     * @param encodedTokenString
     *            JWT string to be used to create and validate a new {@code JwtToken} object. The string should
     *            adhere to the format described in {@link https://tools.ietf.org/html/rfc7519#section-3}, where the string is a
     *            sequence of base64url-encoded URL-safe parts separated by period ('.') characters.
     * @return A new {@code JwtToken} object based on the data contained in the provided token string.
     * @throws InvalidConsumerException
     *             Thrown if a {@code jwtConsumer} element with the ID used to instantiate this {@code JwtConsumer} object cannot
     *             be found in the server configuration.
     * @throws InvalidTokenException
     *             Thrown if the provided token string is {@code null} or empty, or if there is an error while processing the
     *             token string.
     */
    public JwtToken createJwt(String encodedTokenString) throws InvalidTokenException, InvalidConsumerException {
        return consumer.createJwt(encodedTokenString);
    }

}