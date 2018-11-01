/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jwt.internal;

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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.Consumer;
import com.ibm.websphere.security.jwt.InvalidConsumerException;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/*
 * @author IBM Corporation
 *
 * @version 1.0
 *
 * @since 1.0
 *
 * @ibm-api
 */
@Component(service = Consumer.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM", name = "consumer")
public class ConsumerImpl implements Consumer {

    private static final TraceComponent tc = Tr.register(ConsumerImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private String configId = DEFAULT_ID;

    private static boolean active = false;

    /*********************************** Begin OSGi-related fields and methods ***********************************/

    private final static String KEY_JWT_CONSUMER_SERVICE = "jwtConsumerConfig";
    private static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
    private static final String CFG_KEY_ID = "id";

    private static ConcurrentServiceReferenceMap<String, JwtConsumerConfig> jwtServiceMapRef = new ConcurrentServiceReferenceMap<String, JwtConsumerConfig>(KEY_JWT_CONSUMER_SERVICE);
    private static AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE);

    @Reference(service = JwtConsumerConfig.class, name = KEY_JWT_CONSUMER_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.RELUCTANT)
    protected void setJwtConsumerConfig(ServiceReference<JwtConsumerConfig> ref) {
        synchronized (jwtServiceMapRef) {
            jwtServiceMapRef.putReference((String) ref.getProperty(CFG_KEY_ID), ref);
        }
    }

    protected void unsetJwtConsumerConfig(ServiceReference<JwtConsumerConfig> ref) {
        synchronized (jwtServiceMapRef) {
            jwtServiceMapRef.removeReference((String) ref.getProperty(CFG_KEY_ID), ref);
        }
    }

    @Reference(service = KeyStoreService.class, name = KEY_KEYSTORE_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.setReference(ref);
    }

    protected void unsetKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        jwtServiceMapRef.activate(cc);
        keyStoreServiceRef.activate(cc);
        active = true;
        Tr.info(tc, "JWT_CONSUMER_SERVICE_ACTIVATED");
    }

    @Modified
    protected void modify(Map<String, Object> properties) {

    }

    @Deactivate
    protected void deactivate(int reason, ComponentContext cc) {
        jwtServiceMapRef.deactivate(cc);
        keyStoreServiceRef.deactivate(cc);
        active = false;
    }

    /*********************************** End OSGi-related fields and methods ***********************************/

    /**
     * Instantiates a new {@code JwtConsumer} object using the default configuration ID {@value #DEFAULT_ID}. The behavior of the
     * instantiated object reflects the configuration of the corresponding {@code jwtConsumer} element in {@code server.xml}.
     */
    public ConsumerImpl() {
        this(DEFAULT_ID);
    }

    /**
     * Instantiates a new {@code JwtConsumer} object using the configuration ID provided. The ID is expected to match the
     * {@code id} attribute of a {@code jwtConsumer} element in the server configuration. The behavior of the instantiated object
     * reflects the configuration of the corresponding {@code jwtConsumer} element in {@code server.xml}.
     *
     * @param consumerConfigId
     *            ID of a {@code jwtConsumer} element in the server configuration to be reflected by this object. If {@code null}
     *            is specified, the default configuration ID {@value #DEFAULT_ID} will be used.
     */
    public ConsumerImpl(String consumerConfigId) {
        if (consumerConfigId == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Null config ID provided; using " + DEFAULT_ID + " instead");
            }
            consumerConfigId = DEFAULT_ID;
        }
        if (consumerConfigId.equals("")) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Blank config ID provided; using " + DEFAULT_ID + " instead");
            }
            consumerConfigId = DEFAULT_ID;
        }
        configId = consumerConfigId;
    }

    @Override
    public synchronized Consumer create() throws InvalidConsumerException {
        if (!active) {
            String err = Tr.formatMessage(tc, "JWT_CONSUMER_SERVICE_NOT_ACTIVATED");
            throw new InvalidConsumerException(err);
        }
        return create(DEFAULT_ID);
    }

    @Override
    public synchronized Consumer create(String consumerConfigId) throws InvalidConsumerException {
        if (!active) {
            String err = Tr.formatMessage(tc, "JWT_CONSUMER_SERVICE_NOT_ACTIVATED");
            throw new InvalidConsumerException(err);
        }
        if (consumerConfigId == null) {
            String err = Tr.formatMessage(tc, "JWT_CONSUMER_NULL_ID");
            throw new InvalidConsumerException(err);
        }
        return new ConsumerImpl(consumerConfigId);
    }

    @Override
    public JwtToken createJwt(String encodedTokenString) throws InvalidTokenException, InvalidConsumerException {
        JwtConsumerConfig config = jwtServiceMapRef.getService(configId);
        if (config == null) {
            String msg = Tr.formatMessage(tc, "JWT_CONSUMER_CONFIG_NOT_FOUND", new Object[] { configId });
            throw new InvalidConsumerException(msg);
        }

        if (encodedTokenString == null || encodedTokenString.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "JWT_CONSUMER_NULL_OR_EMPTY_STRING", new Object[] { configId, encodedTokenString });
            throw new InvalidTokenException(errorMsg);
        }

        ConsumerUtil util = config.getConsumerUtils();
        if (util == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ConsumerUtil object was not found for config [" + configId + "]");
            }
            return null;
        }

        JwtToken token = null;
        try {
            token = util.parseJwt(encodedTokenString, config);
        } catch (Exception e) { // CWWKS6031E
            String msg = Tr.formatMessage(tc, "JWT_ERROR_PROCESSING_JWT", new Object[] { configId, e.getLocalizedMessage() });
            throw new InvalidTokenException(msg, e);
        }
        return token;
    }

}
