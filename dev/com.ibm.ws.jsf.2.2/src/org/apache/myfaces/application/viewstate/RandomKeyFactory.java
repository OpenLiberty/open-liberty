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
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

/**
 *
 */
class RandomKeyFactory extends KeyFactory<byte[]>
{
    private final Random random;
    private final int length;

    public RandomKeyFactory(FacesContext facesContext)
    {
        length = WebConfigParamUtils.getIntegerInitParameter(
            facesContext.getExternalContext(), 
            ServerSideStateCacheImpl.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH_PARAM, 
            ServerSideStateCacheImpl.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH_PARAM_DEFAULT);
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

    @Override
    public byte[] generateKey(FacesContext facesContext)
    {
        byte[] array = new byte[length];
        byte[] key = new byte[length + 4];
        //sessionIdGenerator.getRandomBytes(array);
        random.nextBytes(array);
        for (int i = 0; i < array.length; i++)
        {
            key[i] = array[i];
        }
        int value = generateCounterKey(facesContext);
        key[array.length] = (byte) (value >>> 24);
        key[array.length + 1] = (byte) (value >>> 16);
        key[array.length + 2] = (byte) (value >>> 8);
        key[array.length + 3] = (byte) (value);
        return key;
    }

    @Override
    public String encode(byte[] key)
    {
        return new String(Hex.encodeHex(key));
    }

    @Override
    public byte[] decode(String value)
    {
        try
        {
            return Hex.decodeHex(value.toCharArray());
        }
        catch (DecoderException ex)
        {
            // Cannot decode, ignore silently, later it will be handled as
            // ViewExpiredException
        }
        return null;
    }
    
}
