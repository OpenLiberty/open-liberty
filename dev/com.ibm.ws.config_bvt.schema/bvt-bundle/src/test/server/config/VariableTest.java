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

public class VariableTest extends ManagedTest {

    public VariableTest(String name) {
        super(name);
    }

    @Override
    public void configurationUpdated(Dictionary properties) throws ConfigurationException {
        assertEquals("lastname", "Potter", properties.get("lastname"));
        assertEquals("name", "Harry Potter", properties.get("name"));
        assertArrayEquals("names", new String[] { "Lisa Griffin", "Lois Griffin" }, (String[]) properties.get("names"));
        assertEquals("testVariable should be the variable defined in bootstrap", "variableFromBootstrap", properties.get("testVariable"));
        assertEquals("testVarNotExist should use the default from the metatype", "defaultFromMetaType", properties.get("testVarNotExist"));
        assertEquals("testToken should use the default from the metatype", "default value", properties.get("testToken"));
    }

}
