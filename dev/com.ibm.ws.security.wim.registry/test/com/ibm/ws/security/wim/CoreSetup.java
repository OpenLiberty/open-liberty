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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.wim.adapter.file.FileAdapter;

import test.common.SharedOutputManager;

public class CoreSetup {

    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final Mockery mock = new JUnit4Mockery();

    private VMMService vmmService;

    private final ConfigManager configManager = new ConfigManager();

    private final ComponentContext cc = mock.mock(ComponentContext.class);

    private final ServiceReference<ConfigurationAdmin> configAdminRef = mock.mock(ServiceReference.class, "configAdminRef");

    private final ConfigurationAdmin configAdmin = mock.mock(ConfigurationAdmin.class, "configAdmin");

    private final Configuration defaultRealmConfig = mock.mock(Configuration.class, "defaultRealmConfig");

    private final Configuration baseEntryConfig = mock.mock(Configuration.class, "baseEntryConfig");

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private static class FA extends FileAdapter {

        @Override
        protected void activate(Map<String, Object> properties, ComponentContext cc) {
            super.activate(properties, cc);
        }
    }

    public void setup(Map<String, Object> configProps) throws IOException {
        mock.checking(new Expectations() {
            {

            }
        });

        HashMap<String, Object> fileConfigProps = new HashMap<String, Object>();
        String[] baseEntries = { "o=defaultWIMFileBasedRealm" };
        fileConfigProps.put(MessageFormat.format("{0}.0.{1}", BaseRepository.BASE_ENTRY, RealmConfig.NAME), "o=defaultWIMFileBasedRealm");
        fileConfigProps.put(BaseRepository.KEY_ID, "InternalFileRepository");
//        fileConfigProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        FA fa = new FA();
        fa.activate(fileConfigProps, cc);

        configProps.put(ConfigManager.MAX_SEARCH_RESULTS, 1000);
        configProps.put(ConfigManager.SEARCH_TIME_OUT, 1000L);
        configProps.put(MessageFormat.format("{0}.0.{1}", ConfigManager.PRIMARY_REALM, RealmConfig.NAME), "defaultWIMFileBasedRealm");
        configProps.put(MessageFormat.format("{0}.0.{1}", ConfigManager.PRIMARY_REALM, RealmConfig.ALLOW_IF_REPODOWN), false);
        configProps.put(MessageFormat.format("{0}.0.{1}.0.{2}", ConfigManager.PRIMARY_REALM, RealmConfig.PARTICIPATING_BASEENTRIES, RealmConfig.NAME),
                        "o=defaultWIMFileBasedRealm");
        configManager.activate(cc, configProps);

        vmmService = new VMMService();
        vmmService.configMgr = configManager;
        vmmService.setConfiguredRepository(fa, fileConfigProps);
        vmmService.activate(cc);
    }

    public VMMService getVMMService() {
        return vmmService;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
