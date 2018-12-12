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
package org.apache.cxf.jaxrs.provider;

import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ProviderFactoryTest {
    @Before
    public void setUp() {
        System.setProperty("com.ibm.ws.jaxrs.testing", "true");
        ServerProviderFactory.getInstance().clearProviders();
    }
    
    @Test
    public void testProviderFactorySortJsonWriters() {
        // create a ProviderFactory instance
        //ProviderFactory factory = new TestProviderFactory();
        // add our user defined MessageBodyWriter
        ProviderFactory factory = ServerProviderFactory.getInstance();
        factory.setProviders(true, false, new TestEntityMessageBodyWriter());
        
        // print out the sorted list of providers and record the index of JsonBProvider and TestEntityMessageBodyWriter
        System.out.println("List of current providers:");
        int i = 0;
        int jsonbIndex = -1;
        int testEntityIndex = -1;
        for(ProviderInfo<MessageBodyWriter<?>> writer : factory.getMessageWriters()) {
            String name = writer.getProvider().getClass().getSimpleName();
            System.out.println("[" + i++ + "] " + name + " custom=" + writer.isCustom());
            if (name.equals("JsonBProvider")) {
                jsonbIndex = i;
            }
            if (name.equals("TestEntityMessageBodyWriter")) {
                testEntityIndex = i;
            }
        }
        
        // compare the indexes to make sure the user defined provider is sorted higher
        assertTrue("Default JsonB provider sorted higher than user defined MessageBodyWriter for application/json", testEntityIndex < jsonbIndex);
    }
}
