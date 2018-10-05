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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;

/**
 * Extends from OidcLogin so we can have a different metatype element with all the defaults set for Google.
 *
 * This class provide two service:
 * . One is for the googleConfig which extends from the generic OAuth2LoginConfig
 * . The other is for JwtConsumerConfig. This make googleLogin does not need to define an additional jJwtConsumerConfig
 * .. So, we can reuse the jwksUri and sslRef defined in the googleLogin.
 */
@Component(name = "com.ibm.ws.security.social.google", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = { SocialLoginConfig.class, JwtConsumerConfig.class }, property = { "service.vendor=IBM", "type=googleLogin" })
public class GoogleLoginConfigImpl extends OidcLoginConfigImpl {
    public static final TraceComponent tc = Tr.register(GoogleLoginConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    @Override
    public String getSignatureAlgorithm() {
        if (jwksUri == null) {
            return null;
        } else {
            return signatureAlgorithm;
        }
    }

}
