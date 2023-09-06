/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Bean which exposes a KafkaTestClient as a RequestScoped bean and ensures any clients are closed at the end of each request.
 * <p>
 * This can be used to safely inject a KafkaTestClient into a FATServlet.
 */
@ApplicationScoped
public class KafkaTestClientProvider {

    public static final String CONNECTION_PROPERTIES_KEY = "kafka-connection-properties";

    @Inject
    @ConfigProperty(name = CONNECTION_PROPERTIES_KEY)
    private Optional<List<String>> connectionProps;

    @Produces
    @RequestScoped
    public KafkaTestClient getTestClient() {
        return new KafkaTestClient(getConnectionPropertiesMap());
    }

    public void disposeTestClient(@Disposes KafkaTestClient client) {
        client.cleanUp();
    }

    private Map<String, String> getConnectionPropertiesMap() {
        HashMap<String, String> result = new HashMap<>();
        Iterator<String> propsIterator = connectionProps.orElse(Collections.emptyList()).iterator();
        while (propsIterator.hasNext()) {
            String key = propsIterator.next();
            String value = propsIterator.next();
            result.put(key, value);
        }
        return result;
    }

    /**
     * Encode a map of connection properties into a list as understood by MP Config so it can be decoded later
     *
     * @param props the properties to encode
     * @return the resulting encoded string
     */
    public static String encodeProperties(Map<String, ?> props) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Entry<String, ?> entry : props.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append(escapeString(entry.getKey()));
            sb.append(",");
            sb.append(escapeString((String) entry.getValue()));
            first = false;
        }

        return sb.toString();
    }

    private static String escapeString(String string) {
        string = string.replaceAll("\\\\", "\\\\\\\\"); // Replace '\' with '\\'. Yes, really.
        string = string.replaceAll(",", "\\\\,"); // Replace ',' with '\,'
        return string;
    }

}
