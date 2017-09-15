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

import java.util.Map;
import java.util.Random;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import org.apache.commons.codec.binary.Hex;
import org.apache.myfaces.application.StateCache;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

/**
 * @since 2.2
 * @author Leonardo Uribe
 */
class RandomCsrfSessionTokenFactory extends CsrfSessionTokenFactory
{
    private final Random random;
    private final int length;

    public RandomCsrfSessionTokenFactory(FacesContext facesContext)
    {
        length = WebConfigParamUtils.getIntegerInitParameter(
            facesContext.getExternalContext(), 
            StateCache.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH_PARAM, 
            StateCache.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH_PARAM_DEFAULT);
        random = new Random(((int) System.nanoTime()) + this.hashCode());
    }

    public Integer generateCounterKey(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        Object sessionObj = externalContext.getSession(true);
        Integer sequence;
        synchronized (sessionObj) // are handled at the same time for the session
        {
            Map<String, Object> map = externalContext.getSessionMap();
            sequence = (Integer) map.get(RendererUtils.SEQUENCE_PARAM);
            if (sequence == null || sequence.intValue() == Integer.MAX_VALUE)
            {
                sequence = Integer.valueOf(1);
            }
            else
            {
                sequence = Integer.valueOf(sequence.intValue() + 1);
            }
            map.put(RendererUtils.SEQUENCE_PARAM, sequence);
        }
        return sequence;
    }

    public byte[] generateKey(FacesContext facesContext)
    {
        byte[] array = new byte[length];
        random.nextBytes(array);
        return array;
    }

    @Override
    public String createCryptographicallyStrongTokenFromSession(FacesContext context)
    {
        byte[] key = generateKey(context);
        return new String(Hex.encodeHex(key));
    }
}
