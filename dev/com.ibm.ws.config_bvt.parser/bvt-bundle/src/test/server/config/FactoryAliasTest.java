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

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;

public class FactoryAliasTest extends ManagedFactoryTest {

    public FactoryAliasTest(String name, int count) {
        super(name, count);
    }

    @Override
    public void configurationUpdated(String pid, Dictionary properties) throws ConfigurationException {
        String id = (String) properties.get("id");

        assertEquals("kids", new Integer(3), properties.get("kids"));

        if ("simpsons".equals(id)) {
            assertEquals("lastName", "Simpson", properties.get("lastName"));
            assertEquals("firstName", "Homer", properties.get("firstName"));
            assertEquals("fullName", "Homer Simpson", properties.get("fullName"));
        } else if ("griffins".equals(id)) {
            assertEquals("lastName", "Griffin", properties.get("lastName"));
            assertEquals("firstName", "Peter", properties.get("firstName"));
            assertEquals("fullName", "Peter Griffin", properties.get("fullName"));
        } else {
            throw new RuntimeException("Invalid instance id: " + id);
        }
    }

}
