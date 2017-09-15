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

public class SharedAliasTest extends ManagedTest {

    private final String firstName;
    private final String lastName;

    public SharedAliasTest(String name, String firstName, String lastName) {
        super(name);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public void configurationUpdated(Dictionary properties) throws ConfigurationException {
        assertEquals("kids", "5", properties.get("kids"));
        assertEquals("lastName", lastName, properties.get("lastName"));
        assertEquals("firstName", firstName, properties.get("firstName"));
        assertEquals("fullName", firstName + " " + lastName, properties.get("fullName"));
    }

}
