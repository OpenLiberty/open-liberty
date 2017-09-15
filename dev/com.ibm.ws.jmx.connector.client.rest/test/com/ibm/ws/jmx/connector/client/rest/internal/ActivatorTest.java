/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.client.rest.internal;

import static com.ibm.ws.jmx.connector.client.rest.internal.Activator.add;
import static com.ibm.ws.jmx.connector.client.rest.internal.Activator.remove;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class ActivatorTest {

    @Test
    public void testPattern() throws Exception {
        assertEquals("a", remove(add("a")));
        assertEquals("a.com.ibm.ws.jmx.connector.client", remove(add("a.com.ibm.ws.jmx.connector.client")));
        assertEquals("a.com.ibm.ws.jmx.connector.client.b", remove(add("a.com.ibm.ws.jmx.connector.client.b")));
        assertEquals("com.ibm.ws.jmx.connector.client.b", remove(add("com.ibm.ws.jmx.connector.client.b")));
    }

}
