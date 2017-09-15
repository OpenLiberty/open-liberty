/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Dictionary;
import java.util.Map;

import org.osgi.service.cm.ConfigurationException;

public class NestedResultTest extends ManagedFactoryTest {

    private Map<String, Map<String, Object>> expectedProperties;

    public NestedResultTest(String name, int count) {
        super(name, count);
    }

    public void setExpectedProperties(Map<String, Map<String, Object>> expectedProperties) {
        this.expectedProperties = expectedProperties;
    }

    @Override
    public void configurationUpdated(String pid, Dictionary properties) throws ConfigurationException {
        String name = (String) properties.get("name");

        Map<String, Object> expectedProps = expectedProperties.get(name);
        assertNotNull("Unexpected name: " + name, expectedProps);
        for (Map.Entry<String, Object> entry : expectedProps.entrySet()) {
            assertEquals("property " + entry.getKey() + " mismatch", entry.getValue(), properties.get(entry.getKey()));
        }
    }

}
