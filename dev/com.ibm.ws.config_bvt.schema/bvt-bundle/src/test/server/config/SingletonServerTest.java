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

public class SingletonServerTest extends ManagedTest {

    public SingletonServerTest(String name) {
        super(name);
    }

    @Override
    public void configurationUpdated(Dictionary properties) throws ConfigurationException {
        assertEquals("simple attr", "abc", properties.get("simpleAttr"));
        assertArrayEquals("collection attr", new String[] { "a", "b", "c" }, (String[]) properties.get("collAttr"));
    }

}
