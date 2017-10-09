/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.authentication.mechanism.http;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

/**
 *
 */
public class LoginToContinuePropertiesImplTest {

    @Test
    public void testGetProperties() throws Exception {
        Properties props = new Properties();
        LoginToContinueProperties ltcp = new LoginToContinuePropertiesImpl(props);
        assertEquals("It should return the same properties object", props, ltcp.getProperties());
    }
}
