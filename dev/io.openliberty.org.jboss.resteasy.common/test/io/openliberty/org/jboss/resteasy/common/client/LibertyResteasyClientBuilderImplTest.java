/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.client;

import static org.junit.Assert.assertNotNull;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.Test;

/**
 * Unit test for LibertyResteasyClientBuilderImpl
 */
public class LibertyResteasyClientBuilderImplTest {

    @Test
    public void testCanBuildLibertyResteasyClientBuilderImpl() {
        LibertyResteasyClientBuilderImpl builder = new LibertyResteasyClientBuilderImpl();
        ResteasyClient client = builder.build();
        assertNotNull(client);
    }
}
