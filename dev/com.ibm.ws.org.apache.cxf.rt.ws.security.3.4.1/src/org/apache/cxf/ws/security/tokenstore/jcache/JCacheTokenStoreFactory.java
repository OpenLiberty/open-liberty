/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.ws.security.tokenstore.jcache;

import java.net.URL;

import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.wss4j.common.util.Loader;


/**
 * A factory to return an JCacheTokenStore instance.
 */
public class JCacheTokenStoreFactory extends TokenStoreFactory {

    private static final String DEFAULT_CONFIG_FILE = "cxf-jcache.xml";

    @Override
    public TokenStore newTokenStore(String key, Message message) throws TokenStoreException {
        URL configFileURL = SecurityUtils.getConfigFileURL(message, SecurityConstants.CACHE_CONFIG_FILE,
                DEFAULT_CONFIG_FILE);
        if (configFileURL == null) {
            configFileURL = Loader.getResource(this.getClass().getClassLoader(),
                    DEFAULT_CONFIG_FILE);
        }
        return new JCacheTokenStore(key, message.getExchange().getBus(), configFileURL);
    }

}
