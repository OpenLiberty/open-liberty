/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaspi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import javax.security.auth.message.config.AuthConfigProvider;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.wsspi.security.jaspi.ProviderService;

public class ProviderRegistryTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ProviderService mockProviderService = mock.mock(ProviderService.class);
    private final AuthConfigProvider mockAuthConfigProvider = mock.mock(AuthConfigProvider.class);

    /**
     * This class was ported from twas and only 2 methods were changed for liberty,
     * the constructor and setProvider, so that is all that will be unit tested on Liberty.
     */

    @Test
    public void testSetProvider() throws Exception {
        final ProviderRegistry reg = new ProviderRegistry();
        mock.checking(new Expectations() {
            {
                allowing(mockProviderService).getAuthConfigProvider(reg);
                will(returnValue(mockAuthConfigProvider));
            }
        });
        AuthConfigProvider provider = reg.setProvider(mockProviderService);
        assertNotNull(provider);
        assertEquals(provider, mockAuthConfigProvider);
    }

    @Test
    public void testPersistence() throws Exception {
        System.setProperty(PersistenceManager.JASPI_CONFIG, File.listRoots()[0].getAbsolutePath() + "jaspiConfig.xml");
        final ProviderRegistry reg = new ProviderRegistry();
        PersistenceManager pm = reg.getPersistenceManager();
        assertNotNull(pm);
        assertEquals(pm.getAuthConfigFactory(), reg);
        assertNotNull(pm.getFile());
        assertEquals(pm.getFile().getName(), "jaspiConfig.xml");
    }
}
