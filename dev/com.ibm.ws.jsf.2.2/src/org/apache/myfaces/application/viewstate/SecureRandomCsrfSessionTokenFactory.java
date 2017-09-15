/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.application.viewstate;

import javax.faces.context.FacesContext;
import org.apache.commons.codec.binary.Hex;
import org.apache.myfaces.application.StateCache;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

/**
 * This factory generate a key composed by a counter and a random number. The
 * counter ensures uniqueness, and the random number prevents guess the next
 * session token.
 * 
 * @since 2.2
 * @author Leonardo Uribe
 */
class SecureRandomCsrfSessionTokenFactory extends CsrfSessionTokenFactory
{
    private final SessionIdGenerator sessionIdGenerator;
    private final int length;

    public SecureRandomCsrfSessionTokenFactory(FacesContext facesContext)
    {
        length = WebConfigParamUtils.getIntegerInitParameter(
            facesContext.getExternalContext(), 
            StateCache.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH_PARAM, 
            StateCache.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH_PARAM_DEFAULT);
        sessionIdGenerator = new SessionIdGenerator();
        sessionIdGenerator.setSessionIdLength(length);
        String secureRandomClass = WebConfigParamUtils.getStringInitParameter(
            facesContext.getExternalContext(), 
            StateCache.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_CLASS_PARAM);
        if (secureRandomClass != null)
        {
            sessionIdGenerator.setSecureRandomClass(secureRandomClass);
        }
        String secureRandomProvider = WebConfigParamUtils.getStringInitParameter(
            facesContext.getExternalContext(), 
            StateCache.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_PROVIDER_PARAM);
        if (secureRandomProvider != null)
        {
            sessionIdGenerator.setSecureRandomProvider(secureRandomProvider);
        }
        String secureRandomAlgorithm = WebConfigParamUtils.getStringInitParameter(
            facesContext.getExternalContext(), 
            StateCache.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_ALGORITM_PARAM);
        if (secureRandomAlgorithm != null)
        {
            sessionIdGenerator.setSecureRandomAlgorithm(secureRandomAlgorithm);
        }
    }

    public byte[] generateKey(FacesContext facesContext)
    {
        byte[] array = new byte[length];
        sessionIdGenerator.getRandomBytes(array);
        return array;
    }

    @Override
    public String createCryptographicallyStrongTokenFromSession(FacesContext context)
    {
        byte[] key = generateKey(context);
        return new String(Hex.encodeHex(key));
    }
}
