/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient12.headerPropagation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class InAppConfigSource implements ConfigSource {
    private static final Logger LOG = Logger.getLogger(InAppConfigSource.class.getName());
    private static final String NAME = InAppConfigSource.class.getSimpleName();
    private static final Map<String,String> PROPERTIES = new HashMap<>();

    static {
        String baseUriProperty = Client.class.getName() + "/mp-rest/uri";
        String baseUriValue = "http://localhost:" + System.getProperty("bvt.prop.HTTP_default", "8010") + "/headerPropagation12App";
        LOG.info("Setting " + baseUriProperty + " to " + baseUriValue);
        PROPERTIES.put(baseUriProperty, baseUriValue);

        baseUriProperty = SecureClient.class.getName() + "/mp-rest/uri";
        baseUriValue = "https://localhost:" + System.getProperty("bvt.prop.HTTP_default.secure", "8020") + "/headerPropagation12App";
        LOG.info("Setting " + baseUriProperty + " to " + baseUriValue);
        PROPERTIES.put(baseUriProperty, baseUriValue);

        baseUriProperty = ClientHeaderParamClient.class.getName() + "/mp-rest/uri";
        baseUriValue = "http://localhost:" + System.getProperty("bvt.prop.HTTP_default", "8010") + "/headerPropagation12App";
        LOG.info("Setting " + baseUriProperty + " to " + baseUriValue);
        PROPERTIES.put(baseUriProperty, baseUriValue);
        
        PROPERTIES.put("org.eclipse.microprofile.rest.client.propagateHeaders", "Authorization,MyCustomHeader");
    }

    public static String getUriForClient(Class<?> clientIntfClass) {
        return PROPERTIES.get(clientIntfClass.getName() + "/mp-rest/uri");
    }

    @Override
    public int getOrdinal() {
        LOG.info("getOrdinal -> 900");
        return 900;
    }

    @Override
    public Map<String, String> getProperties() {
        LOG.info("getProperties -> " + PROPERTIES);
        return PROPERTIES;
    }

    @Override
    public Set<String> getPropertyNames() {
        Set<String> propNames = PROPERTIES.keySet();
        LOG.info("getPropertyNames -> " + propNames);
        return propNames;
    }
    
    @Override
    public String getValue(String key) {
        String value = PROPERTIES.get(key);
        LOG.info("getValue(" + key + ") -> " + value);
        return value;
    }

    @Override
    public String getName() {
        LOG.info("getName -> " + NAME);
        return NAME;
    }
}
