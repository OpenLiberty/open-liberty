/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.map.ObjectMapper;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.ws.ui.internal.v1.ICatalog;
import com.ibm.ws.ui.internal.v1.IFeatureToolService;
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.IToolbox;
import com.ibm.ws.ui.internal.v1.IToolboxService;
import com.ibm.ws.ui.internal.v1.utils.LogRule;
import com.ibm.ws.ui.internal.validation.InvalidCatalogException;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.ws.ui.persistence.IPersistenceProvider;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

import test.common.JSONablePOJOTestCase;
import test.common.SharedOutputManager;

/**
 *
 */
public class CatalogTest extends JSONablePOJOTestCase {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = new LogRule(outputMgr);

    private final Mockery mock = new JUnit4Mockery();
    private final IFeatureToolService mockFeatureToolService = mock.mock(IFeatureToolService.class);
    private final IPersistenceProvider mockPersistence = mock.mock(IPersistenceProvider.class);
    private final IToolboxService mockToolboxService = mock.mock(IToolboxService.class);
    private Catalog catalog;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(mockFeatureToolService).getTools();
                will(returnValue(Collections.EMPTY_SET));

                allowing(mockFeatureToolService).getToolsForRequestLocale();
                will(returnValue(Collections.EMPTY_LIST));

                allowing(mockFeatureToolService).getToolForRequestLocale(with(any(String.class)));
                will(returnValue(null));
            }
        });
        jsonablePojo = new Catalog(mockPersistence, mockToolboxService, mockFeatureToolService);
        sourceJson = "{\"_metadata\":{\"lastModified\":1,\"isDefault\":true},\"featureTools\":[],\"bookmarks\":[{\"name\":\"openliberty.io\",\"url\":\"https://openliberty.io\",\"description\":\"Open Liberty website.\",\"icon\":\"images/tools/Open_Liberty_square_142x142.png\",\"id\":\"openliberty.io\",\"type\":\"bookmark\"}]}";

        catalog = new Catalog(mockPersistence, mockToolboxService, mockFeatureToolService);
    }

    @After
    public void tearDown() {
        catalog = null;

        mock.assertIsSatisfied();
    }

    @Test
    public void constructorDefautInitialization() {
        assertNotNull("FAIL: Catalog tools should not be null",
                      catalog.getFeatureTools());
        assertEquals("FAIL: Catalog tools should be empty",
                     0, catalog.getFeatureTools().size());

        assertNotNull("FAIL: Catalog bookmarks should not be null",
                      catalog.getBookmarks());
        assertEquals("FAIL: Catalog bookmarks should be empty",
                     1, catalog.getBookmarks().size());

        assertNotNull("FAIL: Catalog metadata should not be null",
                      catalog.get_metadata());
        assertEquals("FAIL: Catalog metadata should not be empty",
                     3, catalog.get_metadata().size());
        assertTrue("FAIL: Catalog metadata should reflect isDefault=true",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));
    }

    /**
     * initialProcessFeatures should not cause a isDefault change or cause a store.
     */
    @Test
    public void initialProcessFeatures_noFeatures() throws Exception {
        mock.checking(new Expectations() {
            {
                never(mockPersistence).store(Catalog.PERSIST_NAME, catalog);
            }
        });

        assertTrue("FAIL: Catalog metadata should reflect isDefault=true before initialProcessFeatures",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        catalog.initialProcessFeatures();

        assertTrue("FAIL: Catalog metadata should reflect isDefault=true after initialProcessFeatures",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));
    }

    /**
     * initialProcessFeatures should not cause a isDefault change or cause a store.
     */
    @Test
    public void initialProcessFeatures_hasFeatures() throws Exception {
        final IFeatureToolService newMockFeatureToolService = mock.mock(IFeatureToolService.class, "newMockFeatureToolService");
        final Set<FeatureTool> featureTools = new HashSet<FeatureTool>();
        FeatureTool featureTool = new FeatureTool("com.ibm.websphere.feature", "1.0", "tool-1.0", "FT", "ft.html", "ft.png", "A feature tool");
        featureTools.add(featureTool);

        mock.checking(new Expectations() {
            {
                allowing(newMockFeatureToolService).getTools();
                will(returnValue(featureTools));

                allowing(newMockFeatureToolService).getToolsForRequestLocale();
                will(returnValue(new ArrayList<FeatureTool>(featureTools)));

                never(mockPersistence).store(Catalog.PERSIST_NAME, catalog);
            }
        });

        assertTrue("FAIL: Catalog metadata should reflect isDefault=true before initialProcessFeatures",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        catalog.setIFeatureToolService(newMockFeatureToolService);
        catalog.initialProcessFeatures();

        assertTrue("FAIL: Catalog metadata should reflect isDefault=true after initialProcessFeatures",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#get_metadata()}.
     */
    @Test
    public void get_metadata_uninitialized() {
        assertNull("FAIL: get_metadata() should return null when the object is not initialized",
                   new Catalog().get_metadata());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#getFeatureTools()}.
     */
    @Test
    public void getFeatureTools_uninitialized() {
        assertNull("FAIL: getFeatureTools() should return null when the object is not initialized",
                   new Catalog().getFeatureTools());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#getTools()}.
     */
    @Test
    public void getFeatureTools() {
        assertNotNull("FAIL: getTools() should never return null",
                      catalog.getFeatureTools());
        assertEquals("FAIL: should have no pre-defined feature tools",
                     0, catalog.getFeatureTools().size());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#getTools()}.
     */
    @Test
    public void getFeatureTools_availableFeatures() throws Exception {
        final Set<FeatureTool> featureSet = new HashSet<FeatureTool>();
        final FeatureTool featureTool = new FeatureTool("com.ibm.websphere.feature", "1.0", "tool-1.0", "FT", "ft.html", "ft.png", "A feature tool");
        featureSet.add(featureTool);

        final IFeatureToolService newMockFeatureToolService = mock.mock(IFeatureToolService.class, "newMockFeatureToolService");
        mock.checking(new Expectations() {
            {
                allowing(newMockFeatureToolService).getTools();
                will(returnValue(featureSet));

                allowing(newMockFeatureToolService).getToolsForRequestLocale();
                will(returnValue(new ArrayList<FeatureTool>(featureSet)));

                allowing(newMockFeatureToolService).getToolForRequestLocale("com.ibm.websphere.feature-1.0");
                will(returnValue(featureTool));
            }
        });

        // Change the featureToolService to have a feature tool
        catalog.setIFeatureToolService(newMockFeatureToolService);
        setMockPersistStore();

        assertNotNull("FAIL: getTools() should never return null",
                      catalog.getFeatureTools());
        assertEquals("FAIL: should have the now available feature tools",
                     1, catalog.getFeatureTools().size());
        assertTrue("FAIL: featureTools should contain the featureTool",
                   catalog.getFeatureTools().contains(featureTool));
        assertEquals("FAIL: featureTools should contain an exact match for the featureTool",
                     featureTool, catalog.getTool(featureTool.getId()));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#getTools()}.
     */
    @Test
    public void getFeatureTools_removesFeatures() throws Exception {
        // Add in a tool - "com.ibm.websphere.feature-1.0"
        getFeatureTools_availableFeatures();

        // Change the featureToolService to have no tools
        catalog.setIFeatureToolService(mockFeatureToolService);
        setMockPersistStore();

        assertNotNull("FAIL: getTools() should never return null",
                      catalog.getFeatureTools());
        assertEquals("FAIL: should have no pre-defined feature tools",
                     0, catalog.getFeatureTools().size());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#getTools()}.
     */
    @Test
    public void getFeatureTools_updatesChangedFeature() throws Exception {
        // Add in a tool - "com.ibm.websphere.feature-1.0"
        getFeatureTools_availableFeatures();

        // CHANGE "com.ibm.websphere.feature-1.0" slightly. Update its URL.
        final Set<FeatureTool> featureSet = new HashSet<FeatureTool>();
        final FeatureTool featureTool = new FeatureTool("com.ibm.websphere.feature", "1.0", "FT", "tool-1.0", "ft-new.html", "ft.png", "A feature tool");
        featureSet.add(featureTool);

        final IFeatureToolService updatedMockFeatureToolService = mock.mock(IFeatureToolService.class, "updatedMockFeatureToolService");
        mock.checking(new Expectations() {
            {
                allowing(updatedMockFeatureToolService).getTools();
                will(returnValue(featureSet));

                allowing(updatedMockFeatureToolService).getToolsForRequestLocale();
                will(returnValue(new ArrayList<FeatureTool>(featureSet)));

                allowing(updatedMockFeatureToolService).getToolForRequestLocale("com.ibm.websphere.feature-1.0");
                will(returnValue(featureTool));
            }
        });

        catalog.setIFeatureToolService(updatedMockFeatureToolService);
        setMockPersistStore();

        assertNotNull("FAIL: getTools() should never return null",
                      catalog.getFeatureTools());
        assertEquals("FAIL: should have the changed feature tools",
                     1, catalog.getFeatureTools().size());
        assertTrue("FAIL: should have the changed feature tools",
                   catalog.getFeatureTools().contains(featureTool));
        assertEquals("FAIL: the stored tool did not match the expected tool",
                     featureTool, catalog.getTool(featureTool.getId()));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#getBookmarks()}.
     */
    @Test
    public void getBookmarks_uninitialized() {
        assertNull("FAIL: getBookmarks() should return null when the object is not initialized",
                   new Catalog().getBookmarks());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#getTools()}.
     */
    @Test
    public void getBookmarks() {
        assertNotNull("FAIL: getBookmarks() should never return null",
                      catalog.getBookmarks());
        assertEquals("FAIL: should have 1 pre-defined URLs",
                     1, catalog.getBookmarks().size());

        Bookmark openlibertyio = catalog.getBookmarks().get(0);
        assertEquals("The default openliberty.io bookmark did not have the right 'id'",
                     "openliberty.io", openlibertyio.getId());
        assertEquals("The default openliberty.io bookmark did not have the right 'id'",
                     "bookmark", openlibertyio.getType());
        assertEquals("The default openliberty.io bookmark did not have the right 'id'",
                     "openliberty.io", openlibertyio.getName());
        assertEquals("The default openliberty.io bookmark did not have the right 'id'",
                     "https://openliberty.io", openlibertyio.getURL());
        assertEquals("The default openliberty.io bookmark did not have the right 'id'",
                     "images/tools/Open_Liberty_square_142x142.png", openlibertyio.getIcon());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#getTool(java.lang.String)}.
     */
    @Test
    public void getTool_doesntExist() {
        assertNull("FAIL: tool 'doesntExist' should not be defined",
                   catalog.getTool("doesntExist"));
    }

    /**
     * Establish the mock to expect a call to storage.
     *
     * @throws JSONMarshallException
     */
    private void setMockPersistStore() throws IOException, JSONMarshallException {
        mock.checking(new Expectations() {
            {
                one(mockPersistence).store(Catalog.PERSIST_NAME, catalog);
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#addBookmark(com.ibm.ws.ui.internal.v1.pojo.FeatureTool)}.
     */
    @Test
    public void addBookmark() throws Exception {
        setMockPersistStore();

        assertTrue("FAIL: The metadata isDefault should be true before any changes are made",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        int lCountBefore = catalog.getBookmarks().size();
        long modBefore = (Long) catalog.get_metadata().get(ICatalog.METADATA_LAST_MODIFIED);

        Bookmark featureTool = new Bookmark("Google Gmail", "https://mail.google.com/mail/u/0/?shva=1#inbox", "http://google.com/mail.ico", "Google Gmail desktop version");
        catalog.addBookmark(featureTool);

        int lCountAfter = catalog.getBookmarks().size();
        long modAfter = (Long) catalog.get_metadata().get(ICatalog.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The tool count should be increased by 1 when a tool is added",
                     lCountBefore + 1, lCountAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the add",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an add",
                    (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#addBookmark(com.ibm.ws.ui.internal.v1.pojo.FeatureTool)}.
     */
    @Test
    public void addBookmark_implied() throws Exception {
        setMockPersistStore();

        assertTrue("FAIL: The metadata isDefault should be true before any changes are made",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        int lCountBefore = catalog.getBookmarks().size();
        long modBefore = (Long) catalog.get_metadata().get(ICatalog.METADATA_LAST_MODIFIED);

        ObjectMapper mapper = new ObjectMapper();
        Bookmark featureTool = mapper.readValue("{\"name\":\"b\",\"url\":\"ibm.com\",\"icon\":\"img.png\"}", Bookmark.class);
        catalog.addBookmark(featureTool);

        int lCountAfter = catalog.getBookmarks().size();
        long modAfter = (Long) catalog.get_metadata().get(ICatalog.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The tool count should be increased by 1 when a tool is added",
                     lCountBefore + 1, lCountAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the add",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an add",
                    (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#addBookmark(com.ibm.ws.ui.internal.v1.pojo.FeatureTool)}.
     */
    @Test(expected = DuplicateException.class)
    public void addBookmarkDuplicate() throws Exception {
        setMockPersistStore();

        Bookmark featureTool = new Bookmark("Google Gmail", "https://mail.google.com/mail/u/0/?shva=1#inbox", "https://google.com/mail.ico", "Google Gmail desktop version");
        catalog.addBookmark(featureTool);

        int lCountBefore = catalog.getBookmarks().size();

        catalog.addBookmark(featureTool);

        int lCountAfter = catalog.getBookmarks().size();
        assertEquals("FAIL: The tool count should not change if the tool is not added",
                     lCountBefore, lCountAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addBookmark(com.ibm.ws.ui.internal.v1.pojo.FeatureTool)}.
     */
    @Test
    public void addBookmarkInvalidTool() throws Exception {
        Bookmark badTool = new Bookmark("Simple Action", "htt://www.abc.com/SimpleAction.html", "action-0.1/Action-icon.png", "A simple action");

        int lCountBefore = catalog.getBookmarks().size();

        try {
            catalog.addBookmark(badTool);
            fail("Expected a InvalidToolException to be thrown when adding a bad tool");
        } catch (InvalidToolException e) {
            // Pass
        } ;

        int lCountAfter = catalog.getBookmarks().size();
        assertEquals("FAIL: The tool count should not change if the tool is not added",
                     lCountBefore, lCountAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#addBookmark(com.ibm.ws.ui.internal.v1.pojo.FeatureTool)}.
     */
    @Test
    public void addBookmarkPersistenceError() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockPersistence).store(Catalog.PERSIST_NAME, catalog);
                will(throwException(new IOException("TestException")));
            }
        });

        assertTrue("FAIL: The metadata isDefault should be true before any changes are made",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        long modBefore = (Long) catalog.get_metadata().get(ICatalog.METADATA_LAST_MODIFIED);

        Bookmark featureTool = new Bookmark("Google Gmail", "https://mail.google.com/mail/u/0/?shva=1#inbox", "http://google.com/mail.ico", "Google Gmail desktop version");
        catalog.addBookmark(featureTool);

        long modAfter = (Long) catalog.get_metadata().get(ICatalog.METADATA_LAST_MODIFIED);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the add",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an add",
                    (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Persistence failed on add - did not produce expected CWWKX1012E",
                   outputMgr.checkForMessages("CWWKX1012E:.*TestException"));
    }

    /**
     * Check all futures for any exceptions.
     *
     * @param futureList
     * @throws InterruptedException
     */
    private void checkFuturesForExceptions(List<Future<String>> futureList) throws InterruptedException {
        try {
            for (int i = 0; i < futureList.size(); i++) {
                futureList.get(i).get();
            }
        } catch (ExecutionException ex) {
            ex.printStackTrace();
            fail("Thread ExecutionException: " + ex.getCause());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#addBookmark(com.ibm.ws.ui.internal.v1.pojo.FeatureTool)}.
     */
    @Test
    public void addAndDeleteToolMultiThreads() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(mockPersistence).store(Catalog.PERSIST_NAME, catalog);

                allowing(mockToolboxService).removeToolEntryFromAllToolboxes(with(any(String.class)));
            }
        });

        // Spawn concurrent tool additions
        int threadSize = 10;
        int loops = 50;
        int totalNewTools = loops * 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadSize);
        List<Future<String>> futureList = new ArrayList<Future<String>>();

        // Spawn writers
        for (int t = 0; t < loops; t++) {
            Callable<String> worker = new AddBookmarkWorker(catalog, "t" + t);
            Future<String> future = executor.submit(worker);
            futureList.add(future);
        }

        // Spawn readers
        Future<String> reader = executor.submit(new GetCataloglWorker(catalog));
        Future<String> metadataReader = executor.submit(new GetMetadataWorker(catalog));

        executor.shutdown();
        // Wait until all threads are finish (timeout 10 seconds)
        executor.awaitTermination(10L, TimeUnit.SECONDS);

        // Validation
        List<Future<String>> validationList = new ArrayList<Future<String>>();
        validationList.addAll(futureList);
        validationList.add(reader);
        validationList.add(metadataReader);
        checkFuturesForExceptions(validationList);
        assertEquals("FAIL: The catalog does not have the expected number of bookmarks",
                     1 + totalNewTools, catalog.getBookmarks().size());

        // Spawn concurrent tool removals
        executor = Executors.newFixedThreadPool(threadSize);
        List<Future<String>> futureDeleteList = new ArrayList<Future<String>>();

        // Spawn writers
        for (int t = 0; t < loops; t++) {
            Callable<String> worker = new DeleteBookmarkWorker(catalog, futureList.get(t).get());
            futureDeleteList.add(executor.submit(worker));
        }
        // Spawn readers
        reader = executor.submit(new GetCataloglWorker(catalog));
        metadataReader = executor.submit(new GetMetadataWorker(catalog));

        executor.shutdown();
        // Wait until all threads are finish (timeout 10 seconds)
        executor.awaitTermination(10L, TimeUnit.SECONDS);

        // Validation
        validationList.clear();
        validationList.addAll(futureList);
        validationList.add(reader);
        validationList.add(metadataReader);
        checkFuturesForExceptions(validationList);
        assertEquals("FAIL: The catalog bookmarks did not return to 1 bookmark",
                     1, catalog.getBookmarks().size());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#deleteBookmark(java.lang.String)}.
     */
    @Test
    public void deleteTool() throws Exception {
        final String toolId = "Test+Bookmark";

        setMockPersistStore();
        catalog.addBookmark(new Bookmark("Test Bookmark", "ibm.com", "img.png"));

        long modBefore = (Long) catalog.get_metadata().get(ICatalog.METADATA_LAST_MODIFIED);
        int countBefore = catalog.getBookmarks().size();

        mock.checking(new Expectations() {
            {
                one(mockToolboxService).removeToolEntryFromAllToolboxes(toolId);
            }
        });
        setMockPersistStore();

        ITool deletedTool = catalog.deleteBookmark(toolId);
        assertEquals("FAIL: The delete operation failed or it did not return the correct deleted Tool JSON",
                     toolId, deletedTool.getId());

        int countAfter = catalog.getBookmarks().size();
        long modAfter = (Long) catalog.get_metadata().get(ICatalog.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The metadata count did not get decremented as part of the delete",
                     countBefore - 1, countAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the delete",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an delete",
                    (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#deleteBookmark(java.lang.String)}.
     */
    @Test
    public void deleteToolTwice() throws Exception {
        final String toolId = "Test+Bookmark";

        setMockPersistStore();
        catalog.addBookmark(new Bookmark("Test Bookmark", "ibm.com", "img.png"));

        setMockPersistStore();
        mock.checking(new Expectations() {
            {
                one(mockToolboxService).removeToolEntryFromAllToolboxes(toolId);
            }
        });

        ITool t1 = catalog.deleteBookmark(toolId);
        assertEquals("FAIL: The delete operation failed or it did not return the correct deleted Tool JSON",
                     t1.getId(), toolId);

        ITool t2 = catalog.deleteBookmark(toolId);
        assertNull("FAIL: The delete operation did not return a null object when the tool did not exist", t2);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#deleteBookmark(java.lang.String)}.
     *
     * @throws JSONMarshallException
     */
    @Test
    public void deleteToolWithInvalidIds() throws IOException, JSONMarshallException {
        mock.checking(new Expectations() {
            {
                allowing(mockPersistence).store(Catalog.PERSIST_NAME, catalog);
            }
        });

        ITool deletedTool = catalog.deleteBookmark(null);
        assertNull("FAIL: The delete operation did not return a null object when the toolId was null",
                   deletedTool);
        deletedTool = catalog.deleteBookmark("");
        assertNull("FAIL: The delete operation did not return a null object when the toolId was empty",
                   deletedTool);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#deleteBookmark(java.lang.String)}.
     */
    @Test
    public void deleteToolPersistenceError() throws Exception {
        final String toolId = "Test+Bookmark";

        setMockPersistStore();
        catalog.addBookmark(new Bookmark("Test Bookmark", "ibm.com", "img.png"));

        int countBefore = catalog.getBookmarks().size();
        long modBefore = (Long) catalog.get_metadata().get(ICatalog.METADATA_LAST_MODIFIED);

        mock.checking(new Expectations() {
            {
                one(mockToolboxService).removeToolEntryFromAllToolboxes(toolId);

                one(mockPersistence).store(Catalog.PERSIST_NAME, catalog);
                will(throwException(new IOException("TestException")));
            }
        });

        catalog.deleteBookmark(toolId);

        int countAfter = catalog.getBookmarks().size();
        long modAfter = (Long) catalog.get_metadata().get(ICatalog.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The metadata count did not get decremented as part of the delete",
                     countBefore - 1, countAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the delete",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after a delete",
                    (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Persistence failed on add - did not produce expected CWWKX1012E",
                   outputMgr.checkForMessages("CWWKX1012E:.*TestException"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Catalog#reset()}.
     */
    @Test
    public void reset() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockToolboxService).removeToolEntryFromAllToolboxes("openliberty.io");
            }
        });

        setMockPersistStore();
        catalog.reset();
        assertTrue("FAIL: The metadata isDefault should be true after a reset",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));
        assertNull("Added Tool should no longer be present in the catalog",
                   catalog.getTool("T"));
    }

    @Test
    public void validateSelf() throws Exception {
        catalog.validateSelf();
    }

    @Test
    public void validateSelf_badFeatureTool() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Catalog incompleteToolDataCatalog = mapper.readValue(
                                                             "{\"_metadata\":{\"lastModified\":1,\"isDefault\":true},\"featureTools\":[{\"id\":\"Feature+Tool\"}],\"bookmarks\":[]}",
                                                             Catalog.class);
        incompleteToolDataCatalog.validateSelf();
        assertEquals("FAIL: Catalog tools should contain 0 tools after the tool was removed",
                     0, incompleteToolDataCatalog.getBookmarks().size());

        assertTrue("FAIL: Did not get expected catalog validation message CWWKX1013W",
                   outputMgr.checkForMessages("CWWKX1013W:.*Feature\\+Tool.*"));
    }

    @Test
    public void validateSelf_badBookmark() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Catalog incompleteToolDataCatalog = mapper.readValue(
                                                             "{\"_metadata\":{\"lastModified\":1,\"isDefault\":true},\"featureTools\":[],\"bookmarks\":[{\"id\":\"Simple+Clock\"}]}",
                                                             Catalog.class);
        incompleteToolDataCatalog.validateSelf();
        assertEquals("FAIL: Catalog tools should contain 0 tools after the tool was removed",
                     0, incompleteToolDataCatalog.getBookmarks().size());

        assertTrue("FAIL: Did not get expected catalog validation message CWWKX1013W",
                   outputMgr.checkForMessages("CWWKX1013W:.*Simple\\+Clock.*"));
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameInstance() throws Exception {
        final ICatalog c1 = new Catalog(mockPersistence, mockToolboxService, mockFeatureToolService);
        assertEquals("FAIL: The same Catalog instance did not compare as equals=true",
                     c1, c1);
        assertEquals("FAIL: The same Catalog instance did not compare hashCode as equals=true",
                     c1.hashCode(), c1.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsUninitializedTrue() {
        final ICatalog c1 = new Catalog();
        final ICatalog c2 = new Catalog();
        assertEquals("FAIL: Two Catalogs with uninitialized required fields did not compare as equals",
                     c1, c2);
        assertEquals("FAIL: Two Catalogs with uninitialized required fields did not compare hashCode as equals",
                     c1.hashCode(), c2.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsInitializedTrue() {
        final ICatalog c1 = new Catalog(mockPersistence, mockToolboxService, mockFeatureToolService);
        final ICatalog c2 = new Catalog(mockPersistence, mockToolboxService, mockFeatureToolService);
        assertEquals("FAIL: Two Catalogs with initialized required fields did not compare as equals",
                     c1, c2);
        assertEquals("FAIL: Two Catalogs with initialized required fields did not compare hashCode as equals",
                     c1.hashCode(), c2.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsMismatchTools() {
        final ICatalog c1 = new Catalog();

        final IFeatureToolService newMockFeatureToolService = mock.mock(IFeatureToolService.class, "newMockFeatureToolService");
        final Set<FeatureTool> featureTools = new HashSet<FeatureTool>();
        featureTools.add(new FeatureTool("", "", "", "", "", "", ""));
        mock.checking(new Expectations() {
            {
                allowing(newMockFeatureToolService).getTools();
                will(returnValue(featureTools));

                allowing(newMockFeatureToolService).getToolsForRequestLocale();
                will(returnValue(new ArrayList<FeatureTool>(featureTools)));
            }
        });
        final Catalog c2 = new Catalog(mockPersistence, mockToolboxService, newMockFeatureToolService);
        c2.initialProcessFeatures();
        assertFalse("FAIL: Two unequal Catalogs compared as equal",
                    c1.equals(c2));
    }

    @Test
    public void equalsNotACatalog() {
        assertFalse("FAIL: A non-Catalog object was considered to equal a Catalog",
                    jsonablePojo.equals(new Object()));
    }

    @Test
    public void equalsNull() {
        assertFalse("FAIL: Null was conisdered to equal a Catalog",
                    jsonablePojo.equals(null));
    }

    /** {@inheritDoc} */
    @Override
    @Test
    public void incompleteJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Catalog incompleteObj = mapper.readValue("{}", Catalog.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidCatalogException e) {
            assertNotNull("FAIL: Should have caught an InvalidCatalogException", e);
        }
    }

    @Test
    public void incompleteJsonNoMetadata() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Catalog incompleteObj = mapper.readValue("{\"_metadata\":null,\"featureTools\":[],\"bookmarks\":[]}", Catalog.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidCatalogException e) {
            assertNotNull("FAIL: Should have caught an InvalidCatalogException", e);
            assertTrue("FAIL: Exception message did not contain the word '_metadata'. Message: " + e.getMessage(),
                       e.getMessage().contains("_metadata"));
        }
    }

    @Test
    public void incompleteJsonNoMetaDataLastModified() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Catalog incompleteObj = mapper.readValue("{\"_metadata\":{\"isDefault\":true},\"featureTools\":[],\"bookmarks\":[]}", Catalog.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidCatalogException e) {
            assertNotNull("FAIL: Should have caught an InvalidCatalogException", e);
            assertTrue("FAIL: Exception message did not contain the word '" + ICatalog.METADATA_LAST_MODIFIED + "'",
                       e.getMessage().contains(ICatalog.METADATA_LAST_MODIFIED));
        }
    }

    @Test
    public void incompleteJsonNoMetaDataIsDefault() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Catalog incompleteObj = mapper.readValue("{\"_metadata\":{\"lastModified\":0},\"featureTools\":[],\"bookmarks\":[]}", Catalog.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidCatalogException e) {
            assertNotNull("FAIL: Should have caught an InvalidCatalogException", e);
            assertTrue("FAIL: Exception message did not contain the word '" + ICatalog.METADATA_IS_DEFAULT + "'",
                       e.getMessage().contains(ICatalog.METADATA_IS_DEFAULT));
        }
    }

    @Test
    public void incompleteJsonNoFeatureTools() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Catalog incompleteObj = mapper.readValue("{\"_metadata\":{\"lastModified\":1,\"isDefault\":true},\"featureTools\":null,\"bookmarks\":[]}",
                                                 Catalog.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidCatalogException e) {
            assertNotNull("FAIL: Should have caught an InvalidCatalogException", e);
            assertTrue("FAIL: Exception message did not contain the word 'featureTools'",
                       e.getMessage().contains("featureTools"));
        }
    }

    @Test
    public void incompleteJsonNoBookmarks() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Catalog incompleteObj = mapper.readValue("{\"_metadata\":{\"lastModified\":1,\"isDefault\":true},\"featureTools\":[],\"bookmarks\":null}",
                                                 Catalog.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidCatalogException e) {
            assertNotNull("FAIL: Should have caught an InvalidCatalogException", e);
            assertTrue("FAIL: Exception message did not contain the word 'bookmarks'",
                       e.getMessage().contains("bookmarks"));
        }
        assertFalse("FAIL: An incomplete Catalog was considered to equal a valid Catalog",
                    incompleteObj.equals(jsonablePojo));
    }

    /** Valdiate Self! */
    @Override
    protected void extraPojoMatchesSourceJSONChecks(SelfValidatingPOJO unmarshalledPojo) throws Exception {
        unmarshalledPojo.validateSelf();
    }

    /**
     * Worker class to get the Catalog 100 times in the addAndDeleteToolMultiThreads test case
     */
    public static class GetCataloglWorker implements Callable<String> {

        private final ICatalog catalog;

        public GetCataloglWorker(ICatalog catalog) {
            this.catalog = catalog;
        }

        /** {@inheritDoc} */
        @Override
        public String call() throws Exception {
            int i = 0;
            try {
                // Each thread will get the catalog 100 times
                for (; i < 100; i++) {
                    for (Bookmark catalogTool : catalog.getBookmarks()) {
                        // Iterate through to try and break things
                        catalogTool.getId();
                    }
                }
                return null;
            } catch (Exception e) {
                throw new Exception("Error while in getting catalog iteration " + i + ". Error:" + e.getMessage(), e);
            }
        }
    }

    /**
     * Worker class to get the Catalog metadata 100 times in the addAndDeleteToolMultiThreads test case
     */
    public static class GetMetadataWorker implements Callable<String> {

        private final ICatalog catalog;

        public GetMetadataWorker(ICatalog catalog) {
            this.catalog = catalog;
        }

        /** {@inheritDoc} */
        @Override
        public String call() throws Exception {
            int i = 0;
            try {
                // Each thread will get the catalog 100 times
                for (; i < 100; i++) {
                    Map<String, Object> metadata = catalog.get_metadata();
                    for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                        // Iterate through to try and break things
                        entry.getKey();
                        entry.getValue();
                    }
                }
                return null;
            } catch (Exception e) {
                throw new Exception("Error while in getting catalog metadata iteration " + i + ". Error:" + e.getMessage(), e);
            }
        }
    }

    /**
     * Worker class to add 100 bookmarks in the addAndDeleteToolMultiThreads test case
     */
    public static class AddBookmarkWorker implements Callable<String> {

        private final ICatalog catalog;
        private final String namePrefix;

        public AddBookmarkWorker(ICatalog catalog, String name) {
            this.catalog = catalog;
            this.namePrefix = name;
        }

        /** {@inheritDoc} */
        @Override
        public String call() throws Exception {
            int i = 0;
            try {
                // Each thread will create 100 tools
                for (; i < 100; i++) {
                    Bookmark featureTool = new Bookmark(namePrefix + "-" + i, "http://www.ebay.com", "http://p.ebaystatic.com/aw/pics/globalheader/spr11.png", "eBay US store");
                    catalog.addBookmark(featureTool);
                }
                return namePrefix;
            } catch (Exception e) {
                throw new Exception("Error while in adding tool " + namePrefix + ", iteration " + i + ". Error:" + e.getMessage(), e);
            }
        }
    }

    /**
     * Worker class to delete 100 bookmarks in the addAndDeleteToolMultiThreads test case
     */
    public static class DeleteBookmarkWorker implements Callable<String> {

        private final ICatalog catalog;
        private final String namePrefix;

        public DeleteBookmarkWorker(ICatalog catalog, String name) {
            this.catalog = catalog;
            this.namePrefix = name;
        }

        /** {@inheritDoc} */
        @Override
        public String call() throws Exception {
            int i = 0;
            try {
                // Each thread will create 100 tools
                for (; i < 100; i++) {
                    catalog.deleteBookmark(namePrefix + "-" + i);
                }
                return namePrefix;
            } catch (Exception e) {
                throw new Exception("Error while in delete loop for " + namePrefix + ", iteration " + i + ". Error:" + e.getMessage(), e);
            }
        }
    }

    @Test
    public void getMetadataAndgetTools_returnsSnapshot() throws Exception {
        final Map<String, Object> firstMetadata = catalog.get_metadata();
        final List<FeatureTool> firstTools = catalog.getFeatureTools();
        final List<Bookmark> firstURLs = catalog.getBookmarks();

        setMockPersistStore();
        catalog.addBookmark(new Bookmark("T", "http://www.ebay.com", "http://p.ebaystatic.com/aw/pics/globalheader/spr11.png", "eBay US store"));

        final Map<String, Object> secondMetadata = catalog.get_metadata();
        final List<FeatureTool> secondTools = catalog.getFeatureTools();
        final List<Bookmark> secondURLs = catalog.getBookmarks();

        assertTrue("FAIL: the initial metadata should have a lower last modified compared to the second metadata",
                   (Long) secondMetadata.get(IToolbox.METADATA_LAST_MODIFIED) >= (Long) firstMetadata.get(IToolbox.METADATA_LAST_MODIFIED));
        assertEquals("FAIL: the initial feature tools list should be the same size as the second tools list",
                     firstTools.size(), secondTools.size());
        assertEquals("FAIL: the initial URL list should have one less count compared to the second tools list",
                     firstURLs.size() + 1, secondURLs.size());
    }
}
