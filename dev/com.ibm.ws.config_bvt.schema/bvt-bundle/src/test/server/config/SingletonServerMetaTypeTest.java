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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Vector;

import org.osgi.service.cm.ConfigurationException;

public class SingletonServerMetaTypeTest extends ManagedTest {

    public SingletonServerMetaTypeTest(String name) {
        super(name);
    }

    @Override
    public void configurationUpdated(Dictionary properties) throws ConfigurationException {
        // picked up directly from metatype
        assertEquals("present", Boolean.TRUE, properties.get("present"));
        assertArrayEquals("int array", new int[] { 1, 2, 3 }, (int[]) properties.get("intColl"));
        assertEquals("long vector", new Vector<Long>(Arrays.asList(4l, 5l, 6l, 7l)), properties.get("longColl"));

        // from metatype
        assertEquals("lastname", "Doe", properties.get("lastname"));
        assertEquals("spouse", "Marge Doe", properties.get("spouse"));

        // from server config
        assertArrayEquals("children", new String[] { "Jon Doe", "Jane Doe" }, (String[]) properties.get("children"));
    }

}
