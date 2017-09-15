/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;

import java.util.Dictionary;

/**
 *
 */
public class FinalTypeTest extends ManagedTest {

    /**
     * @param name
     */
    public FinalTypeTest(String name) {
        super(name);
    }

    /** {@inheritDoc} */
    @Override
    public void configurationUpdated(Dictionary properties) throws Exception {
        assertEquals("name", "someName", properties.get("name"));
        assertNull("finalField1 should be null but is " + properties.get("finalField1"), properties.get("finalField1"));
        String configDir = System.getProperty("user.variable");
        assertEquals("finalField2 should be an expanded variable", configDir, properties.get("finalField2"));
        assertEquals("finalField3 should be a default value", "someDefault", properties.get("finalField3"));
    }

}
