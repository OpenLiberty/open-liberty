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
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import javax.naming.OperationNotSupportedException;

import org.junit.Test;

public class JavaURLContextFactoryTest extends MockServicesHelper {
    @Test
    public void testGetObjectInstanceNullObject() throws Exception {
        Object ctx = performTestGetObjectInstance(null);
        assertNotNull("The java url context was null when it shouldn't have been", ctx);
        assertTrue("The java url context was not an instance of " + JavaURLContext.class.getName(),
                   (ctx instanceof JavaURLContext));
    }

    @Test
    public void testGetObjectInstanceString() throws Exception {
        // now look up via the factory (as JNDI would) and the mock check should
        // pass
        Object lookedUpObject = performTestGetObjectInstance("java:comp/env/test");
        // check that the object we got back was not null
        assertNotNull(lookedUpObject);
        assertEquals("The looked up object was not the one we expected", testObject, lookedUpObject);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testGetObjectInstanceStringArray() throws Exception {
        performTestGetObjectInstance(new String[] { "one", "two", "three" });
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testGetObjectInstanceOther() throws Exception {
        performTestGetObjectInstance(new Object());
    }

    /**
     * A utility method to perform the getObjectInstance method on the factory
     * 
     * @param o
     *            the Object to pass to the factory
     * @return the Object instance
     */
    private Object performTestGetObjectInstance(Object o) throws Exception {
        return factory.getObjectInstance(o, null, null, new Hashtable<Object, Object>(0));
    }
}
