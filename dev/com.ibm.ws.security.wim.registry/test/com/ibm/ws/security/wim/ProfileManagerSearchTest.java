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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.wim.adapter.file.FileAdapter;
import com.ibm.wsspi.security.wim.exception.MaxResultsExceededException;
import com.ibm.wsspi.security.wim.exception.MissingSearchControlException;
import com.ibm.wsspi.security.wim.exception.SearchControlException;
import com.ibm.wsspi.security.wim.exception.SortControlException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;
import com.ibm.wsspi.security.wim.model.SortControl;
import com.ibm.wsspi.security.wim.model.SortKeyType;

import test.common.SharedOutputManager;

/**
 * This class tests the ProfileManager search call. It does not test the paging functionality.
 * TODO:: Add paging related test cases.
 */
public class ProfileManagerSearchTest {
    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private VMMService vmmService;
    private final ConfigManager configManager = new ConfigManager();

    private final Mockery mock = new JUnit4Mockery();
    private final ConfiguredRepository repository = mock.mock(ConfiguredRepository.class);
    private final ComponentContext cc = mock.mock(ComponentContext.class);

    private final Configuration defaultRealmConfig = mock.mock(Configuration.class, "defaultRealmConfig");

    private final Configuration baseEntryConfig = mock.mock(Configuration.class, "baseEntryConfig");

    private static class FA extends FileAdapter {

        @Override
        protected void activate(Map<String, Object> properties, ComponentContext cc) {
            super.activate(properties, cc);
        }
    }

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setup() throws IOException {
        mock.checking(new Expectations() {
            {
                String[] baseEntries = { "o=defaultWIMFileBasedRealm" };
                Hashtable<String, Object> realmConfig = new Hashtable<String, Object>();
                realmConfig.put(RealmConfig.PARTICIPATING_BASEENTRIES, baseEntries);
                realmConfig.put(RealmConfig.NAME, "defaultWIMFileBasedRealm");
                realmConfig.put(RealmConfig.ALLOW_IF_REPODOWN, false);

                allowing(defaultRealmConfig).getProperties();
                will(returnValue(realmConfig));

                Hashtable<String, Object> baseEntryProps = new Hashtable<String, Object>();
                baseEntryProps.put(RealmConfig.NAME, "o=defaultWIMFileBasedRealm");

                allowing(baseEntryConfig).getProperties();
                will(returnValue(baseEntryProps));

            }
        });

        Map<String, Object> fileConfigProps = new HashMap<String, Object>();
        String[] baseEntries = { "o=defaultWIMFileBasedRealm" };
        fileConfigProps.put(BaseRepository.BASE_ENTRY, baseEntries);
        fileConfigProps.put(BaseRepository.KEY_ID, "InternalFileRepository");
//        fileConfigProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        FA fa = new FA();
        fa.activate(fileConfigProps, cc);

        HashMap<String, Object> configProps = new HashMap<String, Object>();
        configProps.put(BaseRepository.KEY_ID, "InternalFileRepository");
//        configProps.put(BaseRepository.REPOSITORY_TYPE, "file");
        configProps.put(ConfigManager.MAX_SEARCH_RESULTS, 1000);
        configProps.put(ConfigManager.SEARCH_TIME_OUT, 1000L);
        configProps.put(ConfigManager.PRIMARY_REALM, "defaultRealm");
        configManager.activate(cc, configProps);

        vmmService = new VMMService();
        vmmService.configMgr = configManager;
        vmmService.setConfiguredRepository(fa, fileConfigProps);
        vmmService.activate(cc);
    }

    @After
    public void tearDown() {}

    @Test
    public void testMissingSearchControl() {
        Root root = new Root();
        try {
            vmmService.search(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", MissingSearchControlException.class, e.getClass());
            // assertEquals("The error code for MissingSearchControlException", "CWIML1017E", errorMessage.substring(0, 10));
        }
    }

    @Test
    @Ignore
    public void testIncorrectCountLimit() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and (uid='admin' or cn='user1')");
        srchCtrl.setCountLimit(-1);
        srchCtrl.setSearchLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            vmmService.search(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", SearchControlException.class, e.getClass());
            // assertEquals("The error code for SearchControlException", "CWIML1022E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testNoCountAndSearchLimit() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and (uid='admin' or cn='user1')");
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            root = vmmService.search(root);
            List<Entity> entities = root.getEntities();
            int i = entities.size();
            int index = 0;

            assertEquals("Number of members mismatched", 2, i);

            String[] cns = new String[i];
            String[] expectedcns = { "admin", "user1" };

            for (Entity entity : entities) {
                cns[index++] = ((PersonAccount) entity).getCn();
            }

            assertEquals("CN Mismatched", expectedcns, cns);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", SearchControlException.class, e.getClass());
            // assertEquals("The error code for SearchControlException", "CWIML1031E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testNoCountLimit() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and (uid='admin' or cn='user1')");
        srchCtrl.setSearchLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            root = vmmService.search(root);
            List<Entity> entities = root.getEntities();
            int i = entities.size();
            int index = 0;

            assertEquals("Number of members mismatched", 2, i);

            String[] cns = new String[i];
            String[] expectedcns = { "admin", "user1" };

            for (Entity entity : entities) {
                cns[index++] = ((PersonAccount) entity).getCn();
            }

            assertEquals("CN Mismatched", expectedcns, cns);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", SearchControlException.class, e.getClass());
            // assertEquals("The error code for SearchControlException", "CWIML1031E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testNoSearchLimit() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and (uid='admin' or cn='user1')");
        srchCtrl.setCountLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            root = vmmService.search(root);
            List<Entity> entities = root.getEntities();
            int i = entities.size();
            int index = 0;

            assertEquals("Number of members mismatched", 2, i);

            String[] cns = new String[i];
            String[] expectedcns = { "admin", "user1" };

            for (Entity entity : entities) {
                cns[index++] = ((PersonAccount) entity).getCn();
            }

            assertEquals("CN Mismatched", expectedcns, cns);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", SearchControlException.class, e.getClass());
            // assertEquals("The error code for SearchControlException", "CWIML1031E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testIncorrectSearchLimit() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and (uid='admin' or cn='user1')");
        srchCtrl.setCountLimit(100);
        srchCtrl.setSearchLimit(-1);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            vmmService.search(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", SearchControlException.class, e.getClass());
            // assertEquals("The error code for SearchControlException", "CWIML1031E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testMissingSearchExpression() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setCountLimit(100);
        srchCtrl.setSearchLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            vmmService.search(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", SearchControlException.class, e.getClass());
            // assertEquals("The error code for SearchControlException", "CWIML1003E", errorMessage.substring(0, 10));
        }
    }

    @Test
    @Ignore
    public void testSearchExpressionError() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and (uid='admin' or cn='user1'))");
        srchCtrl.setCountLimit(100);
        srchCtrl.setSearchLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            vmmService.search(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", SearchControlException.class, e.getClass());
            // assertEquals("The error code for SearchControlException", "CWIML1004E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testInvalidSearchExpressionError() {
        Root root = new Root();
        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("wrong expression");
        srchCtrl.setCountLimit(100);
        srchCtrl.setSearchLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);
        try {
            vmmService.search(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", SearchControlException.class, e.getClass());
            // assertEquals("The error code for SearchControlException", "CWIML1029E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testMaxLimitExceeded() {
        Root root = new Root();

        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and (uid='admin' or cn='user1')");
        srchCtrl.setCountLimit(100);
        srchCtrl.setSearchLimit(1);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);

        SortControl sortCtrl = new SortControl();
        root.getControls().add(sortCtrl);

        try {
            vmmService.search(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", MaxResultsExceededException.class, e.getClass());
            // assertEquals("The error code for MaxResultsExceededException", "CWIML1018E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testMissingSortKey() {
        Root root = new Root();

        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and (uid='admin' or cn='user1')");
        srchCtrl.setCountLimit(100);
        srchCtrl.setSearchLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);

        SortControl sortCtrl = new SortControl();
        root.getControls().add(sortCtrl);

        try {
            vmmService.search(root);
            assertEquals("Call completed successfully", false, true);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Incorrect exception thrown", SortControlException.class, e.getClass());
            // assertEquals("The error code for SortControlException", "CWIML1001E", errorMessage.substring(0, 10));
        }
    }

    @Test
    public void testSearchUnSorted() {
        Root root = new Root();

        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and (uid='admin' or cn='user1')");
        srchCtrl.setCountLimit(100);
        srchCtrl.setSearchLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);

        try {
            root = vmmService.search(root);

            List<Entity> entities = root.getEntities();
            int i = entities.size();
            int index = 0;

            assertEquals("Number of members mismatched", 2, i);

            String[] cns = new String[i];
            String[] expectedcns = { "admin", "user1" };

            for (Entity entity : entities) {
                cns[index++] = ((PersonAccount) entity).getCn();
            }

            assertEquals("CN Mismatched", expectedcns, cns);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testSearchSorted() {
        Root root = new Root();

        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='PersonAccount' and (uid='admin' or cn='user1')");
        srchCtrl.setCountLimit(100);
        srchCtrl.setSearchLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);

        SortKeyType sortKey = new SortKeyType();
        sortKey.setPropertyName("cn");
        sortKey.setAscendingOrder(false);

        SortControl sortCtrl = new SortControl();
        sortCtrl.getSortKeys().add(sortKey);
        root.getControls().add(sortCtrl);

        try {
            root = vmmService.search(root);

            List<Entity> entities = root.getEntities();
            int i = entities.size();
            int index = 0;

            assertEquals("Number of members mismatched", 2, i);

            String[] cns = new String[i];
            String[] expectedcns = { "user1", "admin" };

            for (Entity entity : entities) {
                cns[index++] = ((PersonAccount) entity).getCn();
            }

            assertEquals("CN Mismatched", expectedcns, cns);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testSearchGroup() {
        Root root = new Root();

        SearchControl srchCtrl = new SearchControl();
        srchCtrl.getProperties().add("cn");
        srchCtrl.getProperties().add("sn");
        srchCtrl.setExpression("@xsi:type='Group' and cn='group1'");
        srchCtrl.setCountLimit(100);
        srchCtrl.setSearchLimit(100);
        srchCtrl.setTimeLimit(-1);
        root.getControls().add(srchCtrl);

        try {
            root = vmmService.search(root);

            List<Entity> entities = root.getEntities();
            int i = entities.size();
            int index = 0;

            assertEquals("Number of members mismatched", 1, i);

            String cn = ((Group) entities.get(0)).getCn();

            assertEquals("CN Mismatched", "group1", cn);
        } catch (WIMException e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }
}
