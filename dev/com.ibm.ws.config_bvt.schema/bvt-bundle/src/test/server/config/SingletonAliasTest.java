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

public class SingletonAliasTest extends ManagedTest {

    public SingletonAliasTest(String name) {
        super(name);
    }

    @Override
    public void configurationUpdated(Dictionary properties) throws ConfigurationException {
        assertEquals("kids", new Integer(2), properties.get("kids"));
        assertEquals("lastName", "Smith", properties.get("lastName"));
        assertEquals("firstName", "Stan", properties.get("firstName"));
        assertEquals("fullName", "Stan Smith", properties.get("fullName"));
        assertEquals("others", "Roger Smith, Klaus Smith", properties.get("others"));
    }

}
