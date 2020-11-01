/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClientFT.retry;

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
        String baseUriProperty = RetryClient.class.getName() + "/mp-rest/uri";
        String baseUriValue = "http://localhost:" + System.getProperty("bvt.prop.HTTP_default", "8010") + "/retryApp";
        LOG.info("Setting " + baseUriProperty + " to " + baseUriValue);
        PROPERTIES.put(baseUriProperty, baseUriValue);
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
        LOG.info("getValue (" + key + ") -> " + value);
        return value;
    }

    @Override
    public String getName() {
        LOG.info("getName -> " + NAME);
        return NAME;
    }
}
