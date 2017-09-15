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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;

public class FactorySimpleTest extends ManagedFactoryTest {

    public FactorySimpleTest(String name, int count) {
        super(name, count);
    }

    @Override
    public void configurationUpdated(String pid, Dictionary properties) throws ConfigurationException {
        String id = (String) properties.get("id");
        if ("serverInstance".equals(id)) {
            assertEquals("simple attr", "foo", properties.get("simpleAttr"));
            assertArrayEquals("collection attr", new String[] { "Lisa", "Simpson" }, (String[]) properties.get("collAttr"));
        } else {
            throw new RuntimeException("Invalid instance id: " + id);
        }
    }

}
