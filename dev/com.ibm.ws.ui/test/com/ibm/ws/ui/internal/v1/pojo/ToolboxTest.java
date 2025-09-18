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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.IToolbox;
import com.ibm.ws.ui.internal.v1.utils.LogRule;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.ws.ui.internal.validation.InvalidToolboxException;
import com.ibm.ws.ui.persistence.IPersistenceProvider;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;
import org.osgi.service.component.ComponentContext;

import test.common.JSONablePOJOTestCase;
import test.common.SharedOutputManager;

/**
 *
 */
public class ToolboxTest extends JSONablePOJOTestCase {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = new LogRule(outputMgr);

    private static final String USER_ID = "bob_johnson";
    private static final String ENCRYPTED_USER_ID = "Ym9iX2pvaG5zb24=";
    private static final String USER_DISPLAY_NAME = "Bob Johnson";
    private static final String USER_ID_2 = "luke_skywalker";
    private static final String ENCRYPTED_USER_ID_2 = "bHVrZV9za3l3YWxrZXI=";
    private static final String USER_DISPLAY_NAME_2 = "Luke Skywalker";

    private final Mockery mock = new JUnit4Mockery();
    private final ICatalog mockCatalog = mock.mock(ICatalog.class);

    // Mocks for RegistryHelper
    final ComponentContext cc = mock.mock(ComponentContext.class);
    private final IPersistenceProvider mockPersistence = mock.mock(IPersistenceProvider.class);

    private final List<FeatureTool> featureTools = new ArrayList<FeatureTool>();
    private final FeatureTool featureTool = new FeatureTool("com.ibm.websphere.featureTool", "1.0", "tool-1.0", "myFeatureTool", "feature/tool.html", "feature/icon.png", "Feature Tool");

    private final List<Bookmark> bookmarks = new ArrayList<Bookmark>();
    private final Bookmark bookmark = new Bookmark("myBookmark", "catalog/url.html", "catalog/icon.png", "Catalog URL");
    private Toolbox toolbox;

    @Before
    public void setUp() throws Exception {
        featureTools.add(featureTool);
        bookmarks.add(bookmark);

        final Map<String, Object> catalogMetadata = new HashMap<String, Object>();
        catalogMetadata.put(ICatalog.METADATA_IS_DEFAULT, false);
        catalogMetadata.put(ICatalog.METADATA_LAST_MODIFIED, 123L);
        mock.checking(new Expectations() {
            {
                allowing(mockCatalog).getFeatureTools();
                will(returnValue(featureTools));

                allowing(mockCatalog).getBookmarks();
                will(returnValue(bookmarks));

                allowing(mockCatalog).get_metadata();
                will(returnValue(catalogMetadata));
            }
        });

        jsonablePojo = new Toolbox(mockCatalog, mockPersistence, USER_ID, USER_DISPLAY_NAME);
        sourceJson = "{\"_metadata\":{\"lastModified\":123,\"isDefault\":true},\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\"" + USER_DISPLAY_NAME
                     + "\",\"preferences\":{},\"toolEntries\":[{\"id\":\""
                     + featureTool.getId() + "\",\"type\":\"" + featureTool.getType() + "\"},{\"id\":\"" + bookmark.getId()
                     + "\",\"type\":\"" + bookmark.getType() + "\"}],\"bookmarks\":[]}";

        toolbox = new Toolbox(mockCatalog, mockPersistence, USER_ID, USER_DISPLAY_NAME);
    }

    @After
    public void tearDown() {
        toolbox = null;
        mock.assertIsSatisfied();
    }

    @Test
    public void constructorDefautInitialization() {
        Toolbox tb = new Toolbox(mockCatalog, mockPersistence, USER_ID_2, USER_DISPLAY_NAME_2);
        assertNotNull("FAIL: Toolbox instance was null",
                      tb);
        assertEquals("FAIL: Toolbox did not have the correct user ID",
                     USER_ID_2, tb.getOwnerId());
        assertEquals("FAIL: Toolbox did not have the correct user display name",
                     USER_DISPLAY_NAME_2, tb.getOwnerDisplayName());

        assertFalse("FAIL: Catalog metadata should reflect isDefault=false when cataog is not default",
                    (Boolean) mockCatalog.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));
        assertTrue("FAIL: Toolbox metadata should reflect isDefault=true when toolbox is default",
                   (Boolean) tb.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: The feature tool was not in the list of tool entries",
                   tb.getToolEntries().contains(new ToolEntry(featureTool.getId(), featureTool.getType())));
        assertTrue("FAIL: The catalog URL was not in the list of tool entries",
                   tb.getToolEntries().contains(new ToolEntry(bookmark.getId(), bookmark.getType())));
    }

    @Test
    public void constructorDefautInitialization2() {
        final FeatureTool featureTool2 = new FeatureTool("com.ibm.websphere.featureTool", "tool-2.0", "2.0", "FeatureTool", "feature/tool.html", "feature/icon.png", "Feature Tool");
        featureTools.add(featureTool2);

        final Bookmark Bookmark2 = new Bookmark("Bookmark2", "catalog/url.html", "catalog/icon.png", "Catalog URL");
        bookmarks.add(Bookmark2);

        Toolbox tb = new Toolbox(mockCatalog, mockPersistence, USER_ID_2, USER_DISPLAY_NAME_2);
        assertNotNull("FAIL: Toolbox instance was null",
                      tb);
        assertEquals("FAIL: Toolbox did not have the correct user ID",
                     USER_ID_2, tb.getOwnerId());
        assertEquals("FAIL: Toolbox did not have the correct user display name",
                     USER_DISPLAY_NAME_2, tb.getOwnerDisplayName());

        assertFalse("FAIL: Catalog metadata should reflect isDefault=false when cataog is not default",
                    (Boolean) mockCatalog.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));
        assertTrue("FAIL: Toolbox metadata should reflect isDefault=true when toolbox is default",
                   (Boolean) tb.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: The feature tool was not in the list of tool entries",
                   tb.getToolEntries().contains(new ToolEntry(featureTool.getId(), featureTool.getType())));
        assertTrue("FAIL: The feature tool 2 was not in the list of tool entries",
                   tb.getToolEntries().contains(new ToolEntry(featureTool2.getId(), featureTool2.getType())));
        assertTrue("FAIL: The catalog URL was not in the list of tool entries",
                   tb.getToolEntries().contains(new ToolEntry(bookmark.getId(), bookmark.getType())));
        assertTrue("FAIL: The catalog URL 2 was not in the list of tool entries",
                   tb.getToolEntries().contains(new ToolEntry(Bookmark2.getId(), Bookmark2.getType())));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#getPreferences()}.
     */
    @Test
    public void getPreferences() {
        assertNotNull("FAIL: getPreferences() should never return null",
                      toolbox.getPreferences());
        assertEquals("FAIL: Toolbox preferences should contain 0 tool entries by default",
                     0, toolbox.getPreferences().size());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#updatePreferences(Map)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void updatePreferences_null() {
        toolbox.updatePreferences(null);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#updatePreferences(Map)}.
     */
    @Test
    public void updatePreferences_valid() throws Exception {
        Map<String, Object> newPreferences = new HashMap<String, Object>();
        newPreferences.put("pref1", true);

        setMockPersistStore();
        Map<String, Object> oldPrefs = toolbox.updatePreferences(newPreferences);

        assertNotNull("FAIL: updatePreferences() should never return null",
                      oldPrefs);
        assertEquals("FAIL: Toolbox default preferences should contain 0 tool entries by default",
                     0, oldPrefs.size());

        assertEquals("FAIL: should have 1 preference defined",
                     1, toolbox.getPreferences().size());
        assertEquals("FAIL: pref1 did not have the correct value",
                     true, toolbox.getPreferences().get("pref1"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#updatePreferences(Map)}.
     */
    @Test
    public void updatePreferences_malicious() throws Exception {
        Map<String, Object> newPreferences = new HashMap<String, Object>();
        newPreferences.put("malicious", "<script>alert('hi');</script>");

        setMockPersistStore();
        toolbox.updatePreferences(newPreferences);
        assertEquals("FAIL: should have 0 preference defined since the input was malicious",
                     0, toolbox.getPreferences().size());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#getToolEntries()}.
     */
    @Test
    public void getToolEntries() {
        List<ToolEntry> featureTools = toolbox.getToolEntries();
        assertNotNull("FAIL: getTools() should never return null",
                      featureTools);
        assertEquals("FAIL: should have 2 pre-defined tools because Catalog does",
                     2, featureTools.size());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#updateToolEntries(List<ToolEntry> toolEntries)}.
     */
    @Test
    public void updateToolEntries_emptyList() throws Exception {
        final List<ToolEntry> toolEntries = toolbox.getToolEntries();
        assertEquals("FAIL: The default tool entries should be 2 length",
                     2, toolEntries.size());

        final List<ToolEntry> toolEntriestoUpdate = new ArrayList<ToolEntry>();

        try {
            toolbox.updateToolEntries(toolEntriestoUpdate);
            fail("Expected IllegalArgumentException to be thrown when the tool entries list to be update is empty");
        } catch (IllegalArgumentException e) {
            assertTrue("FAIL: The IllegalArgumentException message did not contain CWWKX1044E. Message: " + e.getMessage(),
                       e.getMessage().contains("CWWKX1043E"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#updateToolEntries(List<ToolEntry> toolEntries)}.
     */
    @Test
    public void updateToolEntries_duplicateToolEntries() throws Exception {
        final List<ToolEntry> toolEntries = toolbox.getToolEntries();
        assertEquals("FAIL: The default tool entries should be 2 length",
                     2, toolEntries.size());

        final List<ToolEntry> toolEntriestoUpdate = new ArrayList<ToolEntry>();
        toolEntriestoUpdate.add(toolEntries.get(0));
        toolEntriestoUpdate.add(toolEntries.get(0));

        try {
            toolbox.updateToolEntries(toolEntriestoUpdate);
            fail("Expected IllegalArgumentException to be thrown when there is duplicate key in the tool entries list");
        } catch (IllegalArgumentException e) {
            assertTrue("FAIL: The IllegalArgumentException message did not contain CWWKX1044E. Message: " + e.getMessage(),
                       e.getMessage().contains("CWWKX1044E"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#updateToolEntries(List<ToolEntry> toolEntries)}.
     */
    @Test
    public void updateToolEntries_invalidToolEntry() throws Exception {
        final List<ToolEntry> toolEntries = toolbox.getToolEntries();
        assertEquals("FAIL: The default tool entries should be 2 length",
                     2, toolEntries.size());

        final List<ToolEntry> toolEntriestoUpdate = new ArrayList<ToolEntry>();
        toolEntriestoUpdate.add(toolEntries.get(0));
        toolEntriestoUpdate.add(new ToolEntry("invalidTool", "bookmark"));

        try {
            toolbox.updateToolEntries(toolEntriestoUpdate);
            fail("Expected NoSuchToolException to be thrown when there is invalid key in the tool entries list");
        } catch (NoSuchToolException e) {
            assertTrue("FAIL: The NoSuchToolException message did not contain CWWKX1045E. Message: " + e.getMessage(),
                       e.getMessage().contains("CWWKX1045E"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#updateToolEntries(List<ToolEntry> toolEntries)}.
     */
    @Test
    public void updateToolEntries_invalidListTooManyEntries() throws Exception {
        final List<ToolEntry> toolEntries = toolbox.getToolEntries();
        assertEquals("FAIL: The default tool entries should be 2 length",
                     2, toolEntries.size());

        final List<ToolEntry> toolEntriestoUpdate = new ArrayList<ToolEntry>();
        toolEntriestoUpdate.addAll(toolEntries);
        toolEntriestoUpdate.add(new ToolEntry("myTool", "bookmark"));

        try {
            toolbox.updateToolEntries(toolEntriestoUpdate);
            fail("Expected IllegalArgumentException to be thrown when the number of tools to be update is not the same as the number of tools in the toolbox");
        } catch (IllegalArgumentException e) {
            assertTrue("FAIL: The IllegalArgumentException message did not contain CWWKX1043E. Message: " + e.getMessage(),
                       e.getMessage().contains("CWWKX1043E"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.ToolboxAPI#updateToolEntries(List<ToolEntry> toolEntries)}.
     */
    @Test
    public void updateToolEntries_validToolEntries() throws Exception {
        final List<ToolEntry> toolEntries = toolbox.getToolEntries();
        assertEquals("FAIL: The default tool entries should be 2 length",
                     2, toolEntries.size());

        final List<ToolEntry> toolEntriestoUpdate = new ArrayList<ToolEntry>(2);
        toolEntriestoUpdate.add(toolEntries.get(1));
        toolEntriestoUpdate.add(toolEntries.get(0));

        setMockPersistStore();
        toolbox.updateToolEntries(toolEntriestoUpdate);

        List<ToolEntry> getToolEntriesAfterUpdate = toolbox.getToolEntries();
        assertEquals("Original 1th should now be in the 0th element",
                     toolEntries.get(1), getToolEntriesAfterUpdate.get(0));
        assertEquals("Original 0th should now be in the 1th element",
                     toolEntries.get(0), getToolEntriesAfterUpdate.get(1));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#getTools()}.
     */
    @Test
    public void getBookmarks() {
        List<Bookmark> bookmarks = toolbox.getBookmarks();
        assertNotNull("FAIL: getBookmarks() should never return null",
                      bookmarks);
        assertEquals("FAIL: should have 0 pre-defined bookmarks",
                     0, bookmarks.size());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#getToolEntry(java.lang.String)}.
     */
    @Test
    public void getTool() {
        final String doesntExist = "doesntExist";
        mock.checking(new Expectations() {
            {
                one(mockCatalog).getTool(featureTool.getId());
                will(returnValue(bookmark));
            }
        });
        assertNull("FAIL: tool 'doesntExist' should not be defined",
                   toolbox.getToolEntry(doesntExist));

        assertNotNull("FAIL: tool '" + featureTool.getId() + "' should be pre-defined",
                      toolbox.getToolEntry(featureTool.getId()));
    }

    /**
     * Establish the mock to expect a call to storage.
     *
     * @throws JSONMarshallException
     */
    private void setMockPersistStore() throws IOException, JSONMarshallException {
        mock.checking(new Expectations() {
            {
                one(mockPersistence).store(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, toolbox);
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addToolEntry(ToolEntry)}.
     */
    @Test
    public void addTool_ToolEntry() throws Exception {
        final Bookmark Bookmark = new Bookmark("T1", "t1/url.html", "t1/icon.png", "T1 URL");
        mock.checking(new Expectations() {
            {
                allowing(mockCatalog).getTool("T1");
                will(returnValue(Bookmark));
            }
        });

        setMockPersistStore();

        assertTrue("FAIL: The metadata isDefault should be true before any changes are made",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        int lCountBefore = toolbox.getToolEntries().size();
        long modBefore = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);

        assertNull("The Tool has not been added yet, we should get back null",
                   toolbox.getToolEntry("T1"));

        ToolEntry entry = new ToolEntry("T1", "bookmark");
        ITool added = toolbox.addToolEntry(entry);
        assertEquals("FAIL: The added tool was not added",
                     added, entry);

        assertEquals("The Tool has been added and the backing Bookmark should be returned",
                     Bookmark, toolbox.getToolEntry("T1"));

        int lCountAfter = toolbox.getToolEntries().size();
        long modAfter = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The tool entries count should be increased by 1 when a tool is added",
                     lCountBefore + 1, lCountAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the add",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an add",
                    (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));
    }

    /**
     * /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addToolEntry(ToolEntry)}.
     */
    @Test
    public void addTool_ToolEntry_duplicate() throws Exception {
        final Bookmark Bookmark = new Bookmark("T1", "t1/url.html", "t1/icon.png", "T1 URL");
        mock.checking(new Expectations() {
            {
                allowing(mockCatalog).getTool("T1");
                will(returnValue(Bookmark));
            }
        });

        setMockPersistStore();

        ToolEntry entry = new ToolEntry("T1", "bookmark");
        toolbox.addToolEntry(entry);

        int lCountBefore = toolbox.getToolEntries().size();

        try {
            toolbox.addToolEntry(entry);
            fail("Re-adding the tool entry should result in a DuplicateException");
        } catch (DuplicateException e) {
            assertTrue("FAIL: The DuplicateException message did not contain CWWKX1025E and the tool id. Message: " + e.getMessage(),
                       e.getMessage().matches("CWWKX1025E:.*" + entry.getId() + ".*"));
        }

        int lCountAfter = toolbox.getToolEntries().size();
        assertEquals("FAIL: The tool entries count should not change if the tool is not added",
                     lCountBefore, lCountAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addToolEntry(ToolEntry)}.
     */
    @Test
    public void addTool_ToolEntry_noSuchFeatureToolInCatalog() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(mockCatalog).getTool("noSuchFeatureTool");
                will(returnValue(null));
            }
        });

        ToolEntry entry = new ToolEntry("noSuchFeatureTool", "featureTool");

        int lCountBefore = toolbox.getToolEntries().size();

        try {
            toolbox.addToolEntry(entry);
            fail("Expected a NoSuchToolException to be thrown when adding a tool which is not in the catalog");
        } catch (NoSuchToolException e) {
            assertTrue("FAIL: The DuplicateException message did not contain CWWKX1001E and the tool id. Message: " + e.getMessage(),
                       e.getMessage().matches("CWWKX1001E:.*" + entry.getId() + ".*"));
        } ;

        int lCountAfter = toolbox.getToolEntries().size();
        assertEquals("FAIL: The tool entries count should not change if the tool is not added",
                     lCountBefore, lCountAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addToolEntry(ToolEntry)}.
     */
    @Test
    public void addTool_ToolEntry_noSuchBookmarkInCatalog() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(mockCatalog).getTool("noSuchBookmark");
                will(returnValue(null));
            }
        });

        ToolEntry entry = new ToolEntry("noSuchBookmark", "bookmark");

        int lCountBefore = toolbox.getToolEntries().size();

        try {
            toolbox.addToolEntry(entry);
            fail("Expected a NoSuchToolException to be thrown when adding a tool which is not in the catalog");
        } catch (NoSuchToolException e) {
            assertTrue("FAIL: The DuplicateException message did not contain CWWKX1001E and the tool id. Message: " + e.getMessage(),
                       e.getMessage().matches("CWWKX1001E:.*" + entry.getId() + ".*"));
        } ;

        int lCountAfter = toolbox.getToolEntries().size();
        assertEquals("FAIL: The tool entries count should not change if the tool is not added",
                     lCountBefore, lCountAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addToolEntry(ToolEntry)}.
     */
    @Test
    public void addTool_ToolEntry_idAndTypeMismatch() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(mockPersistence).store(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, toolbox);
            }
        });
        toolbox.deleteToolEntry(featureTool.getId());

        final Bookmark Bookmark = new Bookmark("T1", "t1/url.html", "t1/icon.png", "T1 URL");
        mock.checking(new Expectations() {
            {
                allowing(mockCatalog).getTool("T1");
                will(returnValue(Bookmark));
            }
        });

        ToolEntry entry = new ToolEntry("T1", "featureTool");

        int lCountBefore = toolbox.getToolEntries().size();

        try {
            toolbox.addToolEntry(entry);
            fail("Expected a NoSuchToolException to be thrown when adding a tool which is not in the catalog");
        } catch (NoSuchToolException e) {
            assertTrue("FAIL: The DuplicateException message did not contain CWWKX1041E and the tool id and type. Message: " + e.getMessage(),
                       e.getMessage().matches("CWWKX1041E:.*" + entry.getId() + ".*" + entry.getType() + ".*"));
        } ;

        int lCountAfter = toolbox.getToolEntries().size();
        assertEquals("FAIL: The tool entries count should not change if the tool is not added",
                     lCountBefore, lCountAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addToolEntry(ToolEntry)}.
     */
    @Test
    public void addTool_ToolEntry_invalidTool() throws Exception {
        ToolEntry entry = new ToolEntry("id", "type");

        int lCountBefore = toolbox.getToolEntries().size();

        try {
            toolbox.addToolEntry(entry);
            fail("Expected a InvalidToolException to be thrown when adding a bad tool");
        } catch (InvalidToolException e) {
            assertTrue("FAIL: The DuplicateException message did not contain assCWWKX1026E. Message: " + e.getMessage(),
                       e.getMessage().matches("CWWKX1026E:.*"));
        } ;

        int lCountAfter = toolbox.getToolEntries().size();
        assertEquals("FAIL: The tool entries count should not change if the tool is not added",
                     lCountBefore, lCountAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addToolEntry(ToolEntry)}.
     */
    @Test
    public void addTool_ToolEntry_persistenceError() throws Exception {
        final Bookmark Bookmark = new Bookmark("T1", "t1/url.html", "t1/icon.png", "T1 URL");
        mock.checking(new Expectations() {
            {
                allowing(mockCatalog).getTool("T1");
                will(returnValue(Bookmark));
            }
        });

        mock.checking(new Expectations() {
            {
                one(mockPersistence).store(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, toolbox);
                will(throwException(new IOException("TestException")));
            }
        });

        assertTrue("FAIL: The metadata isDefault should be true before any changes are made",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        long modBefore = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);

        ToolEntry entry = new ToolEntry("T1", "bookmark");
        toolbox.addToolEntry(entry);

        long modAfter = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the add",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an add",
                    (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Persistence failed on add - did not produce expected CWWKX1036E",
                   outputMgr.checkForMessages("CWWKX1036E:.*" + USER_ID + ".*TestException"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addBookmark(Bookmark)}.
     */
    @Test
    public void addTool_Bookmark() throws Exception {
        setMockPersistStore();

        assertTrue("FAIL: The metadata isDefault should be true before any changes are made",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        int lCountBefore = toolbox.getToolEntries().size();
        int uCountBefore = toolbox.getBookmarks().size();
        long modBefore = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);

        assertNull("The Tool has not been added yet, we should get back null",
                   toolbox.getToolEntry("T1"));

        Bookmark bookmark = new Bookmark("T1", "t1.com", "t1.com/icon.png");
        ITool added = toolbox.addBookmark(bookmark);
        assertEquals("FAIL: The added tool was not added",
                     added, bookmark);
        assertEquals("The Tool has been added and the Bookmark should be returned",
                     bookmark, toolbox.getToolEntry("T1"));
        assertTrue("FAIL: Did NOT find the tool in tool entries",
                   toolbox.getToolEntries().contains(new ToolEntry(bookmark.getId(), bookmark.getType())));
        assertFalse("FAIL: Should NOT find the whole tool in tool entries",
                    toolbox.getToolEntries().contains(bookmark));

        int lCountAfter = toolbox.getToolEntries().size();
        int uCountAfter = toolbox.getBookmarks().size();
        long modAfter = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The tool entries count should be increased by 1 when a tool is added",
                     lCountBefore + 1, lCountAfter);
        assertEquals("FAIL: The bookmark count should be increased by 1 when a tool is added",
                     uCountBefore + 1, uCountAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the add",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an add",
                    (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addBookmark(Bookmark)}.
     */
    @Test
    public void addTool_Bookmark_implied() throws Exception {
        setMockPersistStore();

        assertTrue("FAIL: The metadata isDefault should be true before any changes are made",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        int lCountBefore = toolbox.getToolEntries().size();
        int uCountBefore = toolbox.getBookmarks().size();
        long modBefore = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);

        assertNull("The Tool has not been added yet, we should get back null",
                   toolbox.getToolEntry("T1"));

        Bookmark completeTool = new Bookmark("T1", "t1.com", "t1.com/icon.png");
        Bookmark incomplete = new Bookmark(null, null, "T1", "t1.com", "t1.com/icon.png", null);

        ITool added = toolbox.addBookmark(incomplete);
        assertEquals("FAIL: The added tool was not added as a complete Bookmark",
                     completeTool, added);
        assertEquals("The Tool has been added and the complete Bookmark should be returned",
                     completeTool, toolbox.getToolEntry("T1"));
        assertTrue("FAIL: Did NOT find the tool in tool entries",
                   toolbox.getToolEntries().contains(new ToolEntry(completeTool.getId(), completeTool.getType())));
        assertFalse("FAIL: Should NOT find the whole tool in tool entries",
                    toolbox.getToolEntries().contains(completeTool));

        int lCountAfter = toolbox.getToolEntries().size();
        int uCountAfter = toolbox.getBookmarks().size();
        long modAfter = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The tool entries count should be increased by 1 when a tool is added",
                     lCountBefore + 1, lCountAfter);
        assertEquals("FAIL: The bookmark count should be increased by 1 when a tool is added",
                     uCountBefore + 1, uCountAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the add",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an add",
                    (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addBookmark(Bookmark)}.
     */
    @Test
    public void addTool_Bookmark_duplicate() throws Exception {
        setMockPersistStore();

        Bookmark bookmark = new Bookmark("T1", "t1.com", "t1.com/icon.png");
        toolbox.addBookmark(bookmark);

        int lCountBefore = toolbox.getToolEntries().size();
        int uCountBefore = toolbox.getBookmarks().size();

        try {
            toolbox.addBookmark(bookmark);
            fail("Expected a DuplicateException to be thrown when adding a duplicate tool");
        } catch (DuplicateException e) {
            assertTrue("FAIL: The DuplicateException message did not contain CWWKX1025E and the tool id. Message: " + e.getMessage(),
                       e.getMessage().matches("CWWKX1025E:.*" + bookmark.getId() + ".*"));
        } ;

        int lCountAfter = toolbox.getToolEntries().size();
        int uCountAfter = toolbox.getBookmarks().size();
        assertEquals("FAIL: The tool entries count should not change if the tool is not added",
                     lCountBefore, lCountAfter);
        assertEquals("FAIL: The bookmark count should not change if the tool is not added",
                     uCountBefore, uCountAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addBookmark(Bookmark)}.
     */
    @Test
    public void addTool_Bookmark_invalidTool() throws Exception {
        Bookmark bookmark = new Bookmark(null, "https://mail.google.com/mail/u/0/?shva=1#inbox", "http://google.com/mail.ico");

        int lCountBefore = toolbox.getToolEntries().size();
        int uCountBefore = toolbox.getBookmarks().size();

        try {
            toolbox.addBookmark(bookmark);
            fail("Expected a InvalidToolException to be thrown when adding a bad tool");
        } catch (InvalidToolException e) {
            assertTrue("FAIL: The DuplicateException message did not contain CWWKX1026E. Message: " + e.getMessage(),
                       e.getMessage().matches("CWWKX1026E:.*"));
        } ;

        int lCountAfter = toolbox.getToolEntries().size();
        int uCountAfter = toolbox.getBookmarks().size();
        assertEquals("FAIL: The tool entries count should not change if the tool is not added",
                     lCountBefore, lCountAfter);
        assertEquals("FAIL: The bookmark count should not change if the tool is not added",
                     uCountBefore, uCountAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addBookmark(Bookmark)}.
     */
    @Test
    public void addTool_Bookmark_persistenceError() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockPersistence).store(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, toolbox);
                will(throwException(new IOException("TestException")));
            }
        });

        assertTrue("FAIL: The metadata isDefault should be true before any changes are made",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        int lCountBefore = toolbox.getToolEntries().size();
        int uCountBefore = toolbox.getBookmarks().size();
        long modBefore = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);

        Bookmark bookmark = new Bookmark("T1", "t1.com", "t1.com/icon.png");
        toolbox.addBookmark(bookmark);

        int lCountAfter = toolbox.getToolEntries().size();
        int uCountAfter = toolbox.getBookmarks().size();
        long modAfter = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The tool entries count should be increased by 1 when a tool is added",
                     lCountBefore + 1, lCountAfter);
        assertEquals("FAIL: The bookmark count should be increased by 1 when a tool is added",
                     uCountBefore + 1, uCountAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the add",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an add",
                    (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Persistence failed on add - did not produce expected CWWKX1036E",
                   outputMgr.checkForMessages("CWWKX1036E:.*" + USER_ID + ".*TestException"));
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
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#addBookmark(Bookmark)}.
     */
    @Test
    public void addAndDeleteBookmarkMultiThreads() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(mockPersistence).store(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, toolbox);
            }
        });

        int countBefore = toolbox.getToolEntries().size();

        // Spawn concurrent tool additions
        int threadSize = 10;
        int loops = 50;
        int totalNewTools = loops * 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadSize);
        List<Future<String>> futureList = new ArrayList<Future<String>>();

        // Spawn writers
        for (int t = 0; t < loops; t++) {
            Callable<String> worker = new AddBookmarkWorker(toolbox, "t" + t);
            Future<String> future = executor.submit(worker);
            futureList.add(future);
        }
        // Spawn readers
        Future<String> reader = executor.submit(new GetToolboxWorker(toolbox));
        Future<String> metadataReader = executor.submit(new GetMetadataWorker(toolbox));

        executor.shutdown();
        // Wait until all threads are finish (timeout 10 seconds)
        executor.awaitTermination(10L, TimeUnit.SECONDS);

        // Validation
        List<Future<String>> validationList = new ArrayList<Future<String>>();
        validationList.addAll(futureList);
        validationList.add(reader);
        validationList.add(metadataReader);
        checkFuturesForExceptions(validationList);
        assertEquals("FAIL: The toolbox does not have the expected number of tool entries",
                     countBefore + totalNewTools, toolbox.getToolEntries().size());
        assertEquals("FAIL: The toolbox does not have the expected number of bookmarks",
                     totalNewTools, toolbox.getBookmarks().size());

        // Spawn concurrent tool removals
        executor = Executors.newFixedThreadPool(threadSize);
        List<Future<String>> futureDeleteList = new ArrayList<Future<String>>();

        // Spawn writers
        for (int t = 0; t < loops; t++) {
            Callable<String> worker = new DeleteBookmarkWorker(toolbox, futureList.get(t).get());
            Future<String> future = executor.submit(worker);
            futureDeleteList.add(future);
        }
        // Spawn readers
        reader = executor.submit(new GetToolboxWorker(toolbox));
        metadataReader = executor.submit(new GetMetadataWorker(toolbox));

        executor.shutdown();
        // Wait until all threads are finish (timeout 10 seconds)
        executor.awaitTermination(10L, TimeUnit.SECONDS);

        // Validation
        validationList.clear();
        validationList.addAll(futureList);
        validationList.add(reader);
        validationList.add(metadataReader);
        checkFuturesForExceptions(validationList);
        assertEquals("FAIL: The toolbox did not return to the beginning tool entries",
                     countBefore, toolbox.getToolEntries().size());
        assertEquals("FAIL: The toolbox did not return to zero bookmarks",
                     0, toolbox.getBookmarks().size());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#deleteToolEntry(java.lang.String)}.
     */
    @Test
    public void deleteToolEntry_withInvalidIds() throws IOException {
        ITool deletedTool = toolbox.deleteToolEntry(null);
        assertNull("FAIL: The delete operation did not return a null object when the toolId was null",
                   deletedTool);

        deletedTool = toolbox.deleteToolEntry("");
        assertNull("FAIL: The delete operation did not return a null object when the toolId was empty",
                   deletedTool);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#deleteToolEntry(java.lang.String)}.
     *
     * @throws IOException
     * @throws JSONMarshallException
     *
     */
    @Test
    public void deleteToolEntry() throws IOException, JSONMarshallException {
        setMockPersistStore();

        assertTrue("FAIL: The metadata isDefault should be true before any changes are made",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        int lCountBefore = toolbox.getToolEntries().size();
        int uCountBefore = toolbox.getBookmarks().size();
        long modBefore = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);

        ITool deletedTool = toolbox.deleteToolEntry(featureTool.getId());
        assertEquals("FAIL: The delete operation failed or it did not return the correct deleted Tool JSON",
                     deletedTool.getId(), featureTool.getId());

        int lCountAfter = toolbox.getToolEntries().size();
        int uCountAfter = toolbox.getBookmarks().size();
        long modAfter = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The tool entries count did not get decremented as part of the ToolEntry delete",
                     lCountBefore - 1, lCountAfter);
        assertEquals("FAIL: The bookmarks count should not be decremented as part of a ToolEntry delete",
                     uCountBefore, uCountAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the delete",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an delete",
                    (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#deleteToolEntry(java.lang.String)}.
     *
     * @throws IOException
     * @throws JSONMarshallException
     *
     */
    @Test
    public void deleteToolEntry_twice() throws IOException, JSONMarshallException {
        setMockPersistStore();

        ITool deletedTool = toolbox.deleteToolEntry(featureTool.getId());
        assertEquals("FAIL: The delete operation failed or it did not return the correct deleted Tool JSON",
                     deletedTool.getId(), featureTool.getId());

        deletedTool = toolbox.deleteToolEntry(featureTool.getId());
        assertNull("FAIL: The delete operation did not return a null object when the tool did not exist",
                   deletedTool);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#deleteToolEntry(java.lang.String)}.
     */
    @Test
    public void deleteToolEntry_Bookmark() throws Exception {
        Bookmark bookmark = new Bookmark("T1", "t1.com", "t1.com/icon.png");
        setMockPersistStore();
        toolbox.addBookmark(bookmark);

        int lCountBefore = toolbox.getToolEntries().size();
        int uCountBefore = toolbox.getBookmarks().size();
        long modBefore = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);

        setMockPersistStore();
        ITool deletedTool = toolbox.deleteToolEntry(bookmark.getId());
        assertEquals("FAIL: The delete operation failed or it did not return the correct deleted Tool JSON",
                     bookmark.getId(), deletedTool.getId());

        int lCountAfter = toolbox.getToolEntries().size();
        int uCountAfter = toolbox.getBookmarks().size();
        long modAfter = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The tool entries count did not get decremented as part of the Bookmark delete",
                     lCountBefore - 1, lCountAfter);
        assertEquals("FAIL: The bookmarks count did not get decremented as part of the Bookmark delete",
                     uCountBefore - 1, uCountAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the delete",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an delete",
                    (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#deleteToolEntry(java.lang.String)}.
     */
    @Test
    public void deleteToolEntry_Bookmark_twice() throws Exception {
        Bookmark bookmark = new Bookmark("T1", "t1.com", "t1.com/icon.png");
        setMockPersistStore();
        toolbox.addBookmark(bookmark);

        setMockPersistStore();
        ITool deletedTool = toolbox.deleteToolEntry(bookmark.getId());
        assertEquals("FAIL: The delete operation failed or it did not return the correct deleted Tool JSON",
                     deletedTool.getId(), bookmark.getId());

        deletedTool = toolbox.deleteToolEntry(bookmark.getId());
        assertNull("FAIL: The delete operation did not return a null object when the tool did not exist",
                   deletedTool);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#deleteToolEntry(java.lang.String)}.
     */
    @Test
    public void deleteToolEntry_persistenceError() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockPersistence).store(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, toolbox);
                will(throwException(new IOException("TestException")));
            }
        });

        assertTrue("FAIL: The metadata isDefault should be true before any changes are made",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        int countBefore = toolbox.getToolEntries().size();
        long modBefore = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);

        toolbox.deleteToolEntry(featureTool.getId());

        int countAfter = toolbox.getToolEntries().size();
        long modAfter = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The metadata count did not get decremented as part of the delete",
                     countBefore - 1, countAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the delete",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after a delete",
                    (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Persistence failed on add - did not produce expected CWWKX1036E",
                   outputMgr.checkForMessages("CWWKX1036E:.*" + USER_ID + ".*TestException"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#deleteBookmark(java.lang.String)}.
     */
    @Test
    public void deleteBookmark_withInvalidIds() throws IOException {
        ITool deletedTool = toolbox.deleteBookmark(null);
        assertNull("FAIL: The delete operation did not return a null object when the toolId was null",
                   deletedTool);

        deletedTool = toolbox.deleteBookmark("");
        assertNull("FAIL: The delete operation did not return a null object when the toolId was empty",
                   deletedTool);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#deleteBookmark(java.lang.String)}.
     */
    @Test
    public void deleteBookmark() throws Exception {
        Bookmark bookmark = new Bookmark("T1", "t1.com", "t1.com/icon.png");
        setMockPersistStore();
        toolbox.addBookmark(bookmark);

        int lCountBefore = toolbox.getToolEntries().size();
        int uCountBefore = toolbox.getBookmarks().size();
        long modBefore = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);

        setMockPersistStore();
        ITool deletedTool = toolbox.deleteBookmark(bookmark.getId());
        assertEquals("FAIL: The delete operation failed or it did not return the correct deleted Tool JSON",
                     bookmark.getId(), deletedTool.getId());

        int lCountAfter = toolbox.getToolEntries().size();
        int uCountAfter = toolbox.getBookmarks().size();
        long modAfter = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The tool entries count did not get decremented as part of the Bookmark delete",
                     lCountBefore - 1, lCountAfter);
        assertEquals("FAIL: The bookmarks count did not get decremented as part of the Bookmark delete",
                     uCountBefore - 1, uCountAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the delete",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after an delete",
                    (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#deleteBookmark(java.lang.String)}.
     */
    @Test
    public void deleteBookmark_twice() throws Exception {
        Bookmark bookmark = new Bookmark("T1", "t1.com", "t1.com/icon.png");
        setMockPersistStore();
        toolbox.addBookmark(bookmark);

        setMockPersistStore();
        ITool deletedTool = toolbox.deleteBookmark(bookmark.getId());
        assertEquals("FAIL: The delete operation failed or it did not return the correct deleted Tool JSON",
                     deletedTool.getId(), bookmark.getId());

        deletedTool = toolbox.deleteBookmark(bookmark.getId());
        assertNull("FAIL: The delete operation did not return a null object when the tool did not exist",
                   deletedTool);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#deleteBookmark(java.lang.String)}.
     */
    @Test
    public void deleteBookmark_persistenceError() throws Exception {
        Bookmark bookmark = new Bookmark("T1", "t1.com", "t1.com/icon.png");
        setMockPersistStore();
        toolbox.addBookmark(bookmark);

        mock.checking(new Expectations() {
            {
                one(mockPersistence).store(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, toolbox);
                will(throwException(new IOException("TestException")));
            }
        });

        int countBefore = toolbox.getToolEntries().size();
        long modBefore = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);

        toolbox.deleteBookmark(bookmark.getId());

        int countAfter = toolbox.getToolEntries().size();
        long modAfter = (Long) toolbox.get_metadata().get(IToolbox.METADATA_LAST_MODIFIED);
        assertEquals("FAIL: The metadata count did not get decremented as part of the delete",
                     countBefore - 1, countAfter);
        assertTrue("FAIL: The metadata lastModified did not get set as part of the delete",
                   modAfter >= modBefore);
        assertFalse("FAIL: The metadata isDefault should be false after a delete",
                    (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Persistence failed on add - did not produce expected CWWKX1036E",
                   outputMgr.checkForMessages("CWWKX1036E:.*" + USER_ID + ".*TestException"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.Toolbox#reset()}.
     */
    @Test
    public void reset() throws Exception {
        Bookmark bookmark = new Bookmark("T", "http://ibm.com/", "default.png");

        setMockPersistStore();
        toolbox.addBookmark(bookmark);
        // No need to check if addTool worked, we already know from previous tests

        setMockPersistStore();
        toolbox.reset();
        assertTrue("FAIL: The metadata isDefault should be true after a reset",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));
        assertNull("Added Tool should no longer be present in the toolbox",
                   toolbox.getToolEntry("T-1"));
    }

    @Test
    public void validateSelf() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockCatalog).getTool(featureTool.getId());
                will(returnValue(featureTool));

                one(mockCatalog).getTool(bookmark.getId());
                will(returnValue(bookmark));
            }
        });

        toolbox.validateSelf();

        assertTrue("FAIL: Validation should not have produced any messages",
                   outputMgr.isEmptyMessageLog());
    }

    @Test
    public void validateSelf_featureToolRemovedFromCatalog() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockCatalog).getTool(featureTool.getId());
                will(returnValue(null));

                one(mockCatalog).getTool(bookmark.getId());
                will(returnValue(bookmark));
            }
        });

        toolbox.validateSelf();

        assertTrue("FAIL: Did not get expected tool no longer available message CWWKX1037W",
                   outputMgr.checkForMessages("CWWKX1037W:.*" + featureTool.getId() + ".*" + USER_ID));
    }

    @Test
    public void validateSelf_BookmarkRemovedFromCatalog() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockCatalog).getTool(featureTool.getId());
                will(returnValue(featureTool));

                one(mockCatalog).getTool(bookmark.getId());
                will(returnValue(null));
            }
        });

        toolbox.validateSelf();

        assertTrue("FAIL: Did not get expected tool no longer available message CWWKX1037W",
                   outputMgr.checkForMessages("CWWKX1037W:.*" + bookmark.getId() + ".*" + USER_ID));
    }

    @Test
    public void validateSelf_BookmarkNotRemoved() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockCatalog).getTool(featureTool.getId());
                will(returnValue(featureTool));

                one(mockCatalog).getTool(bookmark.getId());
                will(returnValue(bookmark));
            }
        });

        setMockPersistStore();
        toolbox.addBookmark(new Bookmark("T1", "t1.com/", "default.png"));
        toolbox.validateSelf();

        assertTrue("FAIL: Validation should not have produced any messages",
                   outputMgr.isEmptyMessageLog());
    }

    /**
     * @return
     * @throws Exception
     */
    private Toolbox deserializeToolbox(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Toolbox tb = mapper.readValue(json, Toolbox.class);
        tb.setCatalog(mockCatalog);
        return tb;
    }

    @Test
    public void validateSelf_badPeferencesEntry() throws Exception {
        Toolbox toolbox = deserializeToolbox("{\"_metadata\":{\"lastModified\":123,\"isDefault\":true},\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\""
                                             + USER_DISPLAY_NAME
                                             + "\",\"preferences\":{\"malicious\":\"<script>alert('hi');</script>\"},\"toolEntries\":[{\"id\":\"badEntry\"}],\"bookmarks\":[]}");
        toolbox.validateSelf();

        assertEquals("FAIL: Toolbox preferences should contain 0 tool entries because the one entry was malicious",
                     0, toolbox.getPreferences().size());
    }

    @Test
    public void validateSelf_badToolEntry() throws Exception {
        Toolbox toolbox = deserializeToolbox("{\"_metadata\":{\"lastModified\":123,\"isDefault\":true},\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\""
                                             + USER_DISPLAY_NAME + "\",\"preferences\":{},\"toolEntries\":[{\"id\":\"badEntry\"}],\"bookmarks\":[]}");
        toolbox.validateSelf();
        assertEquals("FAIL: Toolbox tools should contain 0 tool entries",
                     0, toolbox.getToolEntries().size());
        assertEquals("FAIL: Toolbox tools should contain 0 bookmarks",
                     0, toolbox.getBookmarks().size());
        assertNull("FAIL: Bad tool was not removed from the catalog after validation",
                   toolbox.getToolEntry("badEntry"));

        assertTrue("FAIL: Did not get expected toolbox validation message CWWKX1035W",
                   outputMgr.checkForMessages("CWWKX1035W:.*badEntry.*" + USER_ID + ".*"));
    }

    @Test
    public void validateSelf_badBookmark() throws Exception {
        Toolbox toolbox = deserializeToolbox("{\"_metadata\":{\"lastModified\":123,\"isDefault\":true},\"ownerId\":\""
                                             + USER_ID
                                             + "\",\"ownerDisplayName\":\""
                                             + USER_DISPLAY_NAME
                                             + "\",\"preferences\":{},\"toolEntries\":[{\"id\":\"badBookmark\",\"type\":\"bookmark\"}],\"bookmarks\":[{\"id\":\"badBookmark\",\"name\":\"badBookmark\"}]}");
        assertNotNull("FAIL: Bad tool is not bad of the toolbox before validation",
                      toolbox.getToolEntry("badBookmark"));

        toolbox.validateSelf();
        assertEquals("FAIL: Toolbox tools should contain 0 tool entries",
                     0, toolbox.getToolEntries().size());
        assertEquals("FAIL: Toolbox tools should contain 0 bookmarks",
                     0, toolbox.getBookmarks().size());
        assertNull("FAIL: Bad tool was not removed from the catalog after validation",
                   toolbox.getToolEntry("badBookmark"));
    }

    @Test
    public void validateSelf_bookmarkEntryNotInURLs() throws Exception {
        Toolbox toolbox = deserializeToolbox("{\"_metadata\":{\"lastModified\":123,\"isDefault\":true},\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\""
                                             + USER_DISPLAY_NAME + "\",\"preferences\":{},\"toolEntries\":[{\"id\":\"badBookmark\",\"type\":\"bookmark\"}],\"bookmarks\":[]}");
        mock.checking(new Expectations() {
            {
                one(mockCatalog).getTool("badBookmark");
                will(returnValue(null));
            }
        });
        toolbox.validateSelf();
        assertEquals("FAIL: Toolbox tools should contain 0 tool entries",
                     0, toolbox.getToolEntries().size());
        assertEquals("FAIL: Toolbox tools should contain 0 bookmarks",
                     0, toolbox.getBookmarks().size());
        assertNull("FAIL: Bad tool was not removed from the catalog after validation",
                   toolbox.getToolEntry("badBookmark"));
    }

    @Test
    public void validateSelf_bookmarkNotInEntries() throws Exception {
        Toolbox toolbox = deserializeToolbox("{\"_metadata\":{\"lastModified\":123,\"isDefault\":true},\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\""
                                             + USER_DISPLAY_NAME + "\",\"preferences\":{},\"toolEntries\":[],\"bookmarks\":[{\"id\":\"badBookmark\",\"name\":\"badBookmark\"}]}");
        assertNotNull("FAIL: Bad tool is not bad of the toolbox before validation",
                      toolbox.getToolEntry("badBookmark"));

        toolbox.validateSelf();
        assertEquals("FAIL: Toolbox tools should contain 0 tool entries",
                     0, toolbox.getToolEntries().size());
        assertEquals("FAIL: Toolbox tools should contain 0 bookmarks",
                     0, toolbox.getBookmarks().size());
        assertNull("FAIL: Bad tool was not removed from the catalog after validation",
                   toolbox.getToolEntry("badBookmark"));
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameInstance() throws Exception {
        final IToolbox c1 = new Toolbox(mockCatalog, mockPersistence, USER_ID, USER_DISPLAY_NAME);
        assertEquals("FAIL: The same Toolbox instance did not compare as equals=true",
                     c1, c1);
        assertEquals("FAIL: The same Toolbox instance did not compare hashCode as equals=true",
                     c1.hashCode(), c1.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsMismatchUsers() {
        final IToolbox c1 = new Toolbox(mockCatalog, mockPersistence, USER_ID, USER_DISPLAY_NAME);
        final IToolbox c2 = new Toolbox(mockCatalog, mockPersistence, USER_ID_2, USER_DISPLAY_NAME_2);
        assertFalse("FAIL: Two unequal Toolboxes compared as equal",
                    c1.equals(c2));
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsMismatchPreferences() throws Exception {
        final IToolbox c1 = new Toolbox(mockCatalog, mockPersistence, USER_ID, USER_DISPLAY_NAME);
        final IToolbox c2 = new Toolbox(mockCatalog, mockPersistence, USER_ID_2, USER_DISPLAY_NAME_2);
        Map<String, Object> newPreferences = new HashMap<String, Object>();
        newPreferences.put("pref1", true);

        mock.checking(new Expectations() {
            {
                one(mockPersistence).store(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID_2, c2);
            }
        });
        c2.updatePreferences(newPreferences);
        assertFalse("FAIL: Two unequal Toolboxes compared as equal",
                    c1.equals(c2));
    }

    @Test
    public void equalsNotAToolbox() {
        assertFalse("FAIL: A non-Toolbox object was considered to equal a Toolbox",
                    jsonablePojo.equals(new Object()));
    }

    @Test
    public void equalsNull() {
        assertFalse("FAIL: Null was conisdered to equal a Toolbox",
                    jsonablePojo.equals(null));
    }

    /** {@inheritDoc} */
    @Override
    @Test
    public void incompleteJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Toolbox incompleteObj = mapper.readValue("{}", Toolbox.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidToolboxException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Toolbox should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    @Test
    public void incompleteJsonEmptyMetadata() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Toolbox incompleteObj = mapper.readValue("{\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\"" + USER_DISPLAY_NAME
                                                 + "\",\"preferences\":{},\"toolEntries\":[],\"bookmarks\":[]}",
                                                 Toolbox.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidToolboxException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: Exception message did not contain the word '_metadata'. Message: " + e.getMessage(),
                       e.getMessage().contains("_metadata"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Toolbox with toolEntries should have a non-zero hashcode",
                   0 != incompleteObj.hashCode());
    }

    @Test
    public void incompleteJsonNoMetaDataLastModified() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Toolbox incompleteObj = mapper.readValue("{\"_metadata\":{\"isDefault\":true},\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\"" + USER_DISPLAY_NAME
                                                 + "\",\"preferences\":{},\"toolEntries\":[],\"bookmarks\":[]}",
                                                 Toolbox.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidToolboxException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: Exception message did not contain the word '" + IToolbox.METADATA_LAST_MODIFIED + "'. Message: " + e.getMessage(),
                       e.getMessage().contains(IToolbox.METADATA_LAST_MODIFIED));
        }
        assertFalse("FAIL: An incomplete Toolbox should not be considered equal to a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Toolbox with toolEntries should have a non-zero hashcode",
                   0 != incompleteObj.hashCode());
    }

    @Test
    public void incompleteJsonNoMetaDataIsDefault() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Toolbox incompleteObj = mapper.readValue("{\"_metadata\":{\"lastModified\":0},\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\""
                                                 + USER_DISPLAY_NAME + "\",\"preferences\":{},\"toolEntries\":[],\"bookmarks\":[]}", Toolbox.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidToolboxException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: Exception message did not contain the word '" + IToolbox.METADATA_IS_DEFAULT + "'. Message: " + e.getMessage(),
                       e.getMessage().contains(IToolbox.METADATA_IS_DEFAULT));
        }
        assertFalse("FAIL: An incomplete Toolbox should not be considered equal to a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Toolbox with toolEntries should have a non-zero hashcode",
                   0 != incompleteObj.hashCode());
    }

    @Test
    public void incompleteJsonNoPreferences() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Toolbox incompleteObj = mapper.readValue("{\"_metadata\":{\"lastModified\":0,\"isDefault\":true},\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\""
                                                 + USER_DISPLAY_NAME + "\",\"toolEntries\":[]}",
                                                 Toolbox.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidToolboxException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: Exception message did not contain the word 'bookmarks'. Message: " + e.getMessage(),
                       e.getMessage().contains("bookmarks"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertFalse("FAIL: An incomplete Catalog with toolEntries should have a non-zero hashcode",
                    0 == incompleteObj.hashCode());
    }

    @Test
    public void incompleteJsonNoOwnerId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Toolbox incompleteObj = mapper.readValue("{\"_metadata\":{\"lastModified\":123,\"isDefault\":true},\"ownerDisplayName\":\"bob\",\"preferences\":{},\"toolEntries\":[],\"bookmarks\":[]}",
                                                 Toolbox.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidToolboxException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: Exception message did not contain the word 'ownerId'. Message: " + e.getMessage(),
                       e.getMessage().contains("ownerId"));
        }
        assertFalse("FAIL: An incomplete Toolbox should not be considered equal to a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Toolbox with toolEntries should have a non-zero hashcode",
                   0 != incompleteObj.hashCode());
    }

    @Test
    public void incompleteJsonNoOwnerDisplayname() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Toolbox incompleteObj = mapper.readValue("{\"_metadata\":{\"lastModified\":123,\"isDefault\":true},\"ownerId\":\"bob\",\"preferences\":{},\"toolEntries\":[],\"bookmarks\":[]}",
                                                 Toolbox.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidToolboxException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: Exception message did not contain the word 'ownerDisplayName'. Message: " + e.getMessage(),
                       e.getMessage().contains("ownerDisplayName"));
        }
        assertFalse("FAIL: An incomplete Toolbox should not be considered equal to a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Toolbox with toolEntries should have a non-zero hashcode",
                   0 != incompleteObj.hashCode());
    }

    @Test
    public void incompleteJsonNoToolEntries() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Toolbox incompleteObj = mapper.readValue("{\"_metadata\":{\"lastModified\":0,\"isDefault\":true},\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\""
                                                 + USER_DISPLAY_NAME + "\",\"preferences\":{},\"bookmarks\":[]}",
                                                 Toolbox.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidToolboxException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: Exception message did not contain the word 'tools'. Message: " + e.getMessage(),
                       e.getMessage().contains("toolEntries"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Catalog with no toolEntries should have a zero hashcode",
                   0 == incompleteObj.hashCode());
    }

    @Test
    public void incompleteJsonNoBookmarks() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Toolbox incompleteObj = mapper.readValue("{\"_metadata\":{\"lastModified\":0,\"isDefault\":true},\"ownerId\":\"" + USER_ID + "\",\"ownerDisplayName\":\""
                                                 + USER_DISPLAY_NAME + "\",\"preferences\":{},\"toolEntries\":[]}",
                                                 Toolbox.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidToolboxException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: Exception message did not contain the word 'bookmarks'. Message: " + e.getMessage(),
                       e.getMessage().contains("bookmarks"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertFalse("FAIL: An incomplete Catalog with toolEntries should have a non-zero hashcode",
                    0 == incompleteObj.hashCode());
    }

    /** Valdiate Self! */
    @Override
    protected void extraPojoMatchesSourceJSONChecks(SelfValidatingPOJO unmarshalledPojo) throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockCatalog).getTool(featureTool.getId());
                will(returnValue(featureTool));

                one(mockCatalog).getTool(bookmark.getId());
                will(returnValue(bookmark));
            }
        });

        Toolbox toolbox = ((Toolbox) unmarshalledPojo);
        toolbox.setCatalog(mockCatalog);
        toolbox.setPersistenceProvider(mockPersistence);
        unmarshalledPojo.validateSelf();
    }

    /**
     * Worker class to get the Toolbox 100 times in the addAndDeleteBookmarkMultiThreads test case
     */
    public static class GetToolboxWorker implements Callable<String> {

        private final IToolbox toolbox;

        public GetToolboxWorker(IToolbox toolbox) {
            this.toolbox = toolbox;
        }

        /** {@inheritDoc} */
        @Override
        public String call() throws Exception {
            int i = 0;
            try {
                // Each thread will get the toolbox 100 times
                for (; i < 100; i++) {
                    for (Bookmark bookmark : toolbox.getBookmarks()) {
                        // Iterate through to try and break things
                        bookmark.getId();
                    }
                }
                return null;
            } catch (Exception e) {
                throw new Exception("Error while in getting toolbox iteration " + i + ". Error:" + e.getMessage(), e);
            }
        }
    }

    /**
     * Worker class to get the Toolbox metadata 100 times in the addAndDeleteBookmarkMultiThreads test case
     */
    public static class GetMetadataWorker implements Callable<String> {

        private final IToolbox toolbox;

        public GetMetadataWorker(IToolbox toolbox) {
            this.toolbox = toolbox;
        }

        /** {@inheritDoc} */
        @Override
        public String call() throws Exception {
            int i = 0;
            try {
                // Each thread will get the toolbox 100 times
                for (; i < 100; i++) {
                    Map<String, Object> metadata = toolbox.get_metadata();
                    for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                        // Iterate through to try and break things
                        entry.getKey();
                        entry.getValue();
                    }
                }
                return null;
            } catch (Exception e) {
                throw new Exception("Error while in getting toolbox metadata iteration " + i + ". Error:" + e.getMessage(), e);
            }
        }
    }

    /**
     * Worker class to add 100 bookmarks in the addAndDeleteBookmarkMultiThreads test case
     */
    public static class AddBookmarkWorker implements Callable<String> {

        private final IToolbox toolbox;
        private final String namePrefix;

        public AddBookmarkWorker(IToolbox toolbox, String name) {
            this.toolbox = toolbox;
            this.namePrefix = name;
        }

        /** {@inheritDoc} */
        @Override
        public String call() throws Exception {
            int i = 0;
            try {
                // Each thread will create 100 tools
                String name = namePrefix;
                for (; i < 100; i++) {
                    Bookmark featureTool = new Bookmark(name + "-" + i, "http://www.ebay.com", "http://p.ebaystatic.com/aw/pics/globalheader/spr11.png");
                    toolbox.addBookmark(featureTool);
                }
                return name;
            } catch (Exception e) {
                throw new Exception("Error while in adding tool " + namePrefix + ", iteration " + i + ". Error:" + e.getMessage(), e);
            }
        }
    }

    /**
     * Worker class to delete 100 bookmarks in the addAndDeleteBookmarkMultiThreads test case
     */
    public static class DeleteBookmarkWorker implements Callable<String> {

        private final IToolbox toolbox;
        private final String name;

        public DeleteBookmarkWorker(IToolbox toolbox, String name) {
            this.toolbox = toolbox;
            this.name = name;
        }

        /** {@inheritDoc} */
        @Override
        public String call() throws Exception {
            int i = 0;
            try {
                // Each thread will create 100 tools
                for (; i < 100; i++) {
                    String toolId = name + "-" + i;
                    this.toolbox.deleteBookmark(toolId);
                }
                return name;
            } catch (Exception e) {
                throw new Exception("Error while in delete loop for " + name + ", iteration " + i + ". Error:" + e.getMessage(), e);
            }
        }
    }

    @Test
    public void getMetadataAndgetTools_returnsSnapshot() throws Exception {
        List<ToolEntry> firstToolEntries = toolbox.getToolEntries();
        List<Bookmark> firstBookmarks = toolbox.getBookmarks();

        setMockPersistStore();
        toolbox.addBookmark(new Bookmark("T", "ibm.com", "ibm.png", "desc"));

        List<ToolEntry> secondToolEntries = toolbox.getToolEntries();
        List<Bookmark> secondBookmarks = toolbox.getBookmarks();

        assertEquals("FAIL: the initial tool entriess list should have one less count compared to the second tools list",
                     firstToolEntries.size() + 1, secondToolEntries.size());
        assertEquals("FAIL: the initial bookmarks list should have one less count compared to the second tools list",
                     firstBookmarks.size() + 1, secondBookmarks.size());
    }

    @Test
    public void test_toString() {
        assertEquals("FAIL: Toolbox toString did not respond with the user name",
                     "Toolbox(" + USER_ID + ")", toolbox.toString());
    }
}
