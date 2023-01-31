/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.test.featurestart.features;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FeatureLevels {
    /**
     * Read the table of feature required java levels.
     *
     * Any feature not lists is expected to be startable on all supported
     * java levels.
     *
     * @return A table of required java levels.
     *
     * @throws IOException Thrown if the table could not be read.
     */
    public static Map<String, Integer> getRequiredLevels() throws IOException {
        Properties props = new Properties();
        readRequiredLevels(props);

        Map<String, Integer> requiredLevels = new HashMap<>(props.size());
        props.forEach((sName, reqLevel) -> {
            Integer requiredLevel = Integer.valueOf((String) reqLevel);
            requiredLevels.put((String) sName, requiredLevel);
        });
        return requiredLevels;
    }

    public static final String REQUIRED_LEVELS_NAME = "/com/ibm/ws/test/featurestart/features/feature-levels.properties";

    protected static void readRequiredLevels(Properties props) throws IOException {
        URL url = FeatureLevels.class.getResource(REQUIRED_LEVELS_NAME);
        try (InputStream input = url.openStream()) {
            props.load(input);
        }
    }
}
