/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.social.SocialLoginService;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

public class CommonConfigTestClass extends CommonTestClass {

    protected final static String DEFAULT_JWT_BUILDER = "defaultJWT";
    protected final static int DEFAULT_CACHE_LIMIT = 50000;

    protected final String UTF_8 = "UTF-8";
    protected final String ALPHA_NUM_REGEX = "[a-zA-Z0-9]";
    protected final String host = "https://some-domain.com";
    protected final String hostAndPort = host + ":80";
    protected final String hostAndPortWithPath = hostAndPort + "/path";

    protected final String configId = "myConfigId";
    protected final String clientId = "myClientId";
    protected final String clientSecret = "myClientSecret";
    protected final SerializableProtectedString clientSecretPS = new SerializableProtectedString(clientSecret.toCharArray());
    protected final String authzEndpoint = hostAndPortWithPath + "/authorize";
    protected final String tokenEndpoint = hostAndPortWithPath + "/token";
    protected final String scope = "profile email some_scope";
    protected final String authFilterRef = "myAuthFilterRef";
    protected final String userApi = "myUserApi";

    protected final static String SOCIAL_LOGIN_SERVICE_ID = "mySocialLoginServiceId";
    protected final static String KEY_SOCIAL_LOGIN_SERVICE = "socialLoginService";

    @SuppressWarnings("unchecked")
    protected final ServiceReference<AuthenticationFilter> authFilterServiceRef = mockery.mock(ServiceReference.class, "authFilterServiceRef");
    @SuppressWarnings("unchecked")
    protected final ServiceReference<SocialLoginService> socialLoginServiceRef = mockery.mock(ServiceReference.class, "socialLoginServiceRef");
    @SuppressWarnings("unchecked")
    protected final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = mockery.mock(AtomicServiceReference.class, "keyStoreServiceRef");

    protected final SocialLoginService socialLoginService = mockery.mock(SocialLoginService.class);
    protected final SSLSupport sslSupport = mockery.mock(SSLSupport.class);
    protected final KeyStoreService keyStoreService = mockery.mock(KeyStoreService.class);
    protected final JSSEHelper jsseHelper = mockery.mock(JSSEHelper.class);

    /************************************** Helper methods **************************************/

    protected Map<String, Object> getStandardConfigProps() {
        Map<String, Object> props = getRequiredConfigProps();
        props.put(Oauth2LoginConfigImpl.KEY_UNIQUE_ID, configId);
        return props;
    }

    protected Map<String, Object> getRequiredConfigProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        return props;
    }

    protected String getRandomRequiredConfigAttribute() {
        Map<String, Object> requiredAttrs = getRequiredConfigProps();
        Set<String> keys = requiredAttrs.keySet();
        return keys.iterator().next();
    }

    protected void verifyAllMissingRequiredAttributes(SharedOutputManager outputMgr) {
        verifyMissingRequiredAttributes(outputMgr, getRequiredConfigProps());
    }

    protected void verifyMissingRequiredAttributes(SharedOutputManager outputMgr, Map<String, Object> attributeMap) {
        verifyMissingRequiredAttributes(outputMgr, attributeMap.keySet().toArray(new String[attributeMap.keySet().size()]));
    }

    protected void verifyMissingRequiredAttributes(SharedOutputManager outputMgr, String... attributes) {
        for (String attr : attributes) {
            verifyLogMessageWithInserts(outputMgr, CWWKS5479E_CONFIG_REQUIRED_ATTRIBUTE_NULL, attr);
        }
    }

}
