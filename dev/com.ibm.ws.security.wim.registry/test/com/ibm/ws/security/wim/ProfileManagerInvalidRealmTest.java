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
package com.ibm.ws.security.wim;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.wim.adapter.file.FileAdapter;
import com.ibm.wsspi.security.wim.exception.EntityNotInRealmScopeException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.Root;

import test.common.SharedOutputManager;

public class ProfileManagerInvalidRealmTest {
    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private VMMService vmmService;
    private final ConfigManager configManager = new ConfigManager();

    private final Mockery mock = new JUnit4Mockery();
    private final ConfiguredRepository repository = mock.mock(ConfiguredRepository.class);
    private final ComponentContext cc = mock.mock(ComponentContext.class);

    private final Configuration defaultRealmConfig = mock.mock(Configuration.class, "defaultRealmConfig");

    private final Configuration baseEntryConfig = mock.mock(Configuration.class, "baseEntryConfig");

    private final Configuration dummyBaseEntryConfig = mock.mock(Configuration.class, "dummyBaseEntryConfig");

    private static class FA extends FileAdapter {

        @Override
        protected void activate(Map<String, Object> properties, ComponentContext cc) {
            super.activate(properties, cc);
        }
    }

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value
     * we allocated statically. -- the normal-variable-ness is for before/after
     * processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setup() throws IOException {
        mock.checking(new Expectations() {
            {
            }
        });

        Map<String, Object> fileConfigProps = new HashMap<String, Object>();
        fileConfigProps.put(MessageFormat.format("{0}.0.{1}", BaseRepository.BASE_ENTRY, RealmConfig.NAME), "o=defaultWIMFileBasedRealm");
        fileConfigProps.put(BaseRepository.KEY_ID, "InternalFileRepository");
//        fileConfigProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        FA fa = new FA();
        fa.activate(fileConfigProps, cc);

        HashMap<String, Object> configProps = new HashMap<String, Object>();
        configProps.put(BaseRepository.KEY_ID, "InternalFileRepository");
//        configProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        configProps.put(ConfigManager.MAX_SEARCH_RESULTS, 1000);
        configProps.put(ConfigManager.SEARCH_TIME_OUT, 1000L);
        configProps.put(MessageFormat.format("{0}.0.{1}", ConfigManager.PRIMARY_REALM, RealmConfig.NAME), "DummyRealm");
        configProps.put(MessageFormat.format("{0}.0.{1}.0.{2}", ConfigManager.PRIMARY_REALM, RealmConfig.PARTICIPATING_BASEENTRIES, RealmConfig.NAME), "o=dummyRealm");
        configManager.activate(cc, configProps);

        vmmService = new VMMService();
        vmmService.configMgr = configManager;
        vmmService.setConfiguredRepository(fa, fileConfigProps);
        vmmService.activate(cc);
    }

    @After
    public void tearDown() {}

    @Test
    public void testEntityNotInRealmScope() throws IOException {
        Root root = new Root();
        PersonAccount person = new PersonAccount();
        root.getEntities().add(person);
        IdentifierType id = new IdentifierType();
        id.setUniqueName("uid=user1,o=defaultWIMFileBasedRealm");
        person.setIdentifier(id);
        try {
            vmmService.get(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", EntityNotInRealmScopeException.class, e.getClass());
            // assertEquals("The error code for EntityNotInRealmScopeException", "CWIML0515E", errorMessage.substring(0, 10));
        }
    }
}
