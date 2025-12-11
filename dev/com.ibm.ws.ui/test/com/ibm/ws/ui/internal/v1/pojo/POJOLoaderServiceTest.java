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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.ws.ui.internal.v1.ICatalog;
import com.ibm.ws.ui.internal.v1.IFeatureToolService;
import com.ibm.ws.ui.internal.v1.IToolbox;
import com.ibm.ws.ui.internal.v1.utils.LogRule;
import com.ibm.ws.ui.internal.validation.InvalidCatalogException;
import com.ibm.ws.ui.internal.validation.InvalidToolboxException;
import com.ibm.ws.ui.persistence.IPersistenceProvider;

import test.common.SharedOutputManager;

/**
 *
 */
public class POJOLoaderServiceTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = new LogRule(outputMgr);

    private static final String USER_ID = "bob_johnson";
    private static final String ENCRYPTED_USER_ID = "Ym9iX2pvaG5zb24=";
    private static final String USER_DISPLAY_NAME = "Bob Johnson";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final WSSecurityService wsSecurityService = mock.mock(WSSecurityService.class);
    private final UserRegistry mockUserRegistry = mock.mock(UserRegistry.class);
    private final IFeatureToolService mockFeatureToolService = mock.mock(IFeatureToolService.class);
    private final IPersistenceProvider mockFilePersistence = mock.mock(IPersistenceProvider.class, "mockFilePersistence");
    private final IPersistenceProvider mockRepositoryPersistence = mock.mock(IPersistenceProvider.class, "mockRepositoryPersistence");
    private final Catalog mockPersistedCatalog = mock.mock(Catalog.class);
    private final Toolbox mockPersistedToolbox = mock.mock(Toolbox.class);
    private POJOLoaderService loader;

    @Before
    public void setUp() {
        loader = new POJOLoaderService();
        loader.setWSSecurityService(wsSecurityService);
        loader.setIPersistenceProviderFILE(mockFilePersistence);
        loader.setIFeatureToolService(mockFeatureToolService);
        loader.activate();
    }

    @After
    public void tearDown() {
        loader.deactive();
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        loader.unsetIPersistenceProviderFILE(mockFilePersistence);
        loader.unsetIFeatureToolService(mockFeatureToolService);
        loader.unsetWSSecurityService(wsSecurityService);
        loader = null;

        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#setIPersistenceProvider(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setIPersistenceProviderFILE_printsCorrectMessage() {
        loader.setIPersistenceProviderFILE(mockFilePersistence);

        assertTrue("FAIL: The FILE provider type was not reported correctly",
                   outputMgr.checkForMessages("CWWKX1015I:.*FILE"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#setIPersistenceProvider(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setIPersistenceProviderCOLLECTIVE_printsCorrectMessage() {
        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);

        assertTrue("FAIL: The COLLECTIVE provider type was not reported correctly",
                   outputMgr.checkForMessages("CWWKX1015I:.*COLLECTIVE"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#setIPersistenceProvider(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetIPersistenceProviderCOLLECTIVE_printsCorrectMessage() {
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);

        assertTrue("FAIL: The FILE provider type was not reported correctly when COLLECTIVE is unset",
                   outputMgr.checkForMessages("CWWKX1015I:.*FILE"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getCatalog_createDefault() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                exactly(1).of(mockFeatureToolService).getToolsForRequestLocale();
                exactly(2).of(mockFeatureToolService).getTools();
            }
        });

        ICatalog catalog = loader.getCatalog();

        assertTrue("FAIL: expected default toolbox to be returned",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default catalog' message CWWKX1000I",
                   outputMgr.checkForMessages("CWWKX1000I:.*"));

        assertSame("FAIL: getCatalog() should return the same instance",
                   catalog, loader.getCatalog());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getCatalog_badSyntax() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(throwException(new JSONMarshallException("Unable to parse non-well-formed content")));

                exactly(1).of(mockFeatureToolService).getToolsForRequestLocale();
                exactly(2).of(mockFeatureToolService).getTools();
            }
        });

        ICatalog catalog = loader.getCatalog();

        assertTrue("FAIL: expected default toolbox to be returned",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default catalog' message CWWKX1000I",
                   outputMgr.checkForMessages("CWWKX1000I:.*"));

        assertTrue("FAIL: Did not find the 'bad syntax' message CWWKX1002E",
                   outputMgr.checkForMessages("CWWKX1002E:.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getCatalog_badContent() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(throwException(new JSONMarshallException("Fatal problems occurred while mapping content")));

                exactly(1).of(mockFeatureToolService).getToolsForRequestLocale();
                exactly(2).of(mockFeatureToolService).getTools();
            }
        });

        ICatalog catalog = loader.getCatalog();

        assertTrue("FAIL: expected default toolbox to be returned",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default catalog' message CWWKX1000I",
                   outputMgr.checkForMessages("CWWKX1000I:.*"));

        assertTrue("FAIL: Did not find the 'bad content' message CWWKX1003E",
                   outputMgr.checkForMessages("CWWKX1003E:.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getCatalog_IOError() throws Exception {
        mock.checking(new Expectations() {
            {
                atMost(2).of(mockFilePersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(throwException(new IOException("Test Exception")));

                exactly(1).of(mockFeatureToolService).getToolsForRequestLocale();
                exactly(2).of(mockFeatureToolService).getTools();
            }
        });

        ICatalog catalog = loader.getCatalog();

        assertTrue("FAIL: expected default catalog to be returned",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default catalog' message CWWKX1000I",
                   outputMgr.checkForMessages("CWWKX1000I:.*"));

        assertTrue("FAIL: Did not find the 'io error' message CWWKX1004E",
                   outputMgr.checkForMessages("CWWKX1004E:.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getCatalog_invalidCatalog() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(returnValue(mockPersistedCatalog));

                one(mockPersistedCatalog).setIToolboxService(loader);

                one(mockPersistedCatalog).setIPersistenceProvider(mockFilePersistence);

                one(mockPersistedCatalog).setIFeatureToolService(mockFeatureToolService);

                one(mockPersistedCatalog).initialProcessFeatures();

                one(mockPersistedCatalog).validateSelf();
                will(throwException(new InvalidCatalogException("TestException")));

                exactly(1).of(mockFeatureToolService).getToolsForRequestLocale();
                exactly(2).of(mockFeatureToolService).getTools();
            }
        });

        ICatalog catalog = loader.getCatalog();

        assertTrue("FAIL: expected default catalog to be returned",
                   (Boolean) catalog.get_metadata().get(ICatalog.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default catalog' message CWWKX1000I",
                   outputMgr.checkForMessages("CWWKX1000I:.*"));

        assertTrue("FAIL: Did not find the 'not valid catalog' message CWWKX1005E",
                   outputMgr.checkForMessages("CWWKX1005E:.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getCatalog_loadedCatalog() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(returnValue(mockPersistedCatalog));

                one(mockPersistedCatalog).setIToolboxService(loader);

                one(mockPersistedCatalog).setIPersistenceProvider(mockFilePersistence);

                one(mockPersistedCatalog).setIFeatureToolService(mockFeatureToolService);

                one(mockPersistedCatalog).initialProcessFeatures();

                one(mockPersistedCatalog).validateSelf();
            }
        });

        ICatalog catalog = loader.getCatalog();
        assertSame("FAIL: Did not get back the mocked Catalog instance",
                   mockPersistedCatalog, catalog);
        assertTrue("FAIL: Did not find the 'loaded catalog' message CWWKX1006I",
                   outputMgr.checkForMessages("CWWKX1006I:.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getCatalog_loadedCatalogFromCollective() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(returnValue(mockPersistedCatalog));

                one(mockPersistedCatalog).setIToolboxService(loader);

                one(mockPersistedCatalog).setIPersistenceProvider(mockRepositoryPersistence);

                one(mockPersistedCatalog).setIFeatureToolService(mockFeatureToolService);

                one(mockPersistedCatalog).initialProcessFeatures();

                one(mockPersistedCatalog).validateSelf();

                one(mockPersistedCatalog).get_metadata();
                will(returnValue(Collections.singletonMap("isDefault", false)));

                one(mockPersistedCatalog).getServerVersion();
                will(returnValue("21.0.0.8"));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        ICatalog catalog = loader.getCatalog();
        assertSame("FAIL: Did not get back the mocked Catalog instance",
                   mockPersistedCatalog, catalog);
        assertTrue("FAIL: Did not find the 'loaded catalog' message CWWKX1006I",
                   outputMgr.checkForMessages("CWWKX1006I:.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     *
     * Testing the path with default catalog created from an earlier server causing the default
     * catalog to be recreated using the current server.
     */
    @Test
    public void getCatalog_loadedCatalogFromCollectiveWithDefaultAndNoServerVersion() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(returnValue(mockPersistedCatalog));

                one(mockPersistedCatalog).setIToolboxService(loader);

                one(mockPersistedCatalog).setIPersistenceProvider(mockRepositoryPersistence);

                one(mockPersistedCatalog).setIFeatureToolService(mockFeatureToolService);

                one(mockPersistedCatalog).initialProcessFeatures();

                one(mockPersistedCatalog).validateSelf();

                one(mockPersistedCatalog).get_metadata();
                will(returnValue(Collections.singletonMap("isDefault", true)));

                one(mockPersistedCatalog).getServerVersion();
                will(returnValue("21.0.0.8"));

                one(mockRepositoryPersistence).delete("catalog");
                will(returnValue(true));

                one(mockFeatureToolService).getTools();

                one(mockRepositoryPersistence).store(with(Catalog.PERSIST_NAME), with(any(Catalog.class)));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        ICatalog catalog = loader.getCatalog();
        assertTrue("FAIL: Did not find the 'loaded catalog to the current server version' message CWWKX1070I",
                   outputMgr.checkForMessages("CWWKX1070I:.*"));
        assertTrue("FAIL: Did not find the 'loaded catalog' message CWWKX1006I",
                   outputMgr.checkForMessages("CWWKX1006I:.*"));
        assertTrue("FAIL: Did not find the 'loaded default catalog' message CWWKX1000I",
                   outputMgr.checkForMessages("CWWKX1000I:.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     *
     * Testing the path with default catalog created from the same server.
     */
    @Test
    public void getCatalog_loadedCatalogFromCollectiveWithSameServerVersion() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(returnValue(mockPersistedCatalog));

                one(mockPersistedCatalog).setIToolboxService(loader);

                one(mockPersistedCatalog).setIPersistenceProvider(mockRepositoryPersistence);

                one(mockPersistedCatalog).setIFeatureToolService(mockFeatureToolService);

                one(mockPersistedCatalog).initialProcessFeatures();

                one(mockPersistedCatalog).validateSelf();

                one(mockPersistedCatalog).get_metadata();
                will(returnValue(new HashMap<String, Object>() {
                    {
                        put("isDefault", true);
                        put("serverVersion", "21.0.0.8");
                    }
                }));

                one(mockPersistedCatalog).getServerVersion();
                will(returnValue("21.0.0.8"));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        ICatalog catalog = loader.getCatalog();
        assertFalse("FAIL: Should not find the 'loaded catalog to the current server version' message CWWKX1070I",
                    outputMgr.checkForMessages("CWWKX1070I:.*"));
        assertTrue("FAIL: Did not find the 'loaded catalog' message CWWKX1006I",
                   outputMgr.checkForMessages("CWWKX1006I:.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getCatalog_loadedCatalogPromotedToCollective() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(throwException(new FileNotFoundException("TestException")));

                one(mockFilePersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(returnValue(mockPersistedCatalog));

                one(mockPersistedCatalog).setIToolboxService(loader);

                one(mockPersistedCatalog).setIPersistenceProvider(mockRepositoryPersistence);

                one(mockPersistedCatalog).setIFeatureToolService(mockFeatureToolService);

                one(mockPersistedCatalog).initialProcessFeatures();

                one(mockPersistedCatalog).validateSelf();

                one(mockPersistedCatalog).storeCatalog();
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        ICatalog catalog = loader.getCatalog();
        assertSame("FAIL: Did not get back the mocked Catalog instance",
                   mockPersistedCatalog, catalog);
        assertTrue("FAIL: Did not find the 'promoted catalog' message CWWKX1006I",
                   outputMgr.checkForMessages("CWWKX1038I:.*FILE.*COLLECTIVE"));
        assertTrue("FAIL: Did not find the 'loaded catalog' message CWWKX1006I",
                   outputMgr.checkForMessages("CWWKX1006I:.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getCatalog_defaultCatalogStoredToCollective() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(throwException(new FileNotFoundException("TestException")));

                one(mockFilePersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(throwException(new FileNotFoundException("TestException")));

                one(mockFeatureToolService).getTools();

                one(mockRepositoryPersistence).store(with(Catalog.PERSIST_NAME), with(any(Catalog.class)));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        loader.getCatalog();
        assertTrue("FAIL: Did not find the 'loaded default catalog' message CWWKX1000I",
                   outputMgr.checkForMessages("CWWKX1000I:.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getCatalog_changeProviderResetsCache() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(returnValue(mockPersistedCatalog));

                one(mockPersistedCatalog).setIToolboxService(loader);

                one(mockPersistedCatalog).setIPersistenceProvider(mockFilePersistence);

                one(mockPersistedCatalog).setIFeatureToolService(mockFeatureToolService);

                one(mockPersistedCatalog).initialProcessFeatures();

                one(mockPersistedCatalog).validateSelf();
            }
        });

        ICatalog catalog = loader.getCatalog();
        assertSame("FAIL: Did not get back the mocked Catalog instance",
                   mockPersistedCatalog, catalog);

        // Set the COLLECTIVE provider, which should cause a cache clear
        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);

        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(returnValue(mockPersistedCatalog));

                one(mockPersistedCatalog).setIToolboxService(loader);

                one(mockPersistedCatalog).setIPersistenceProvider(mockRepositoryPersistence);

                one(mockPersistedCatalog).setIFeatureToolService(mockFeatureToolService);

                one(mockPersistedCatalog).initialProcessFeatures();

                one(mockPersistedCatalog).validateSelf();

                one(mockPersistedCatalog).get_metadata();
                will(returnValue(Collections.singletonMap("isDefault", false)));

                one(mockPersistedCatalog).getServerVersion();
                will(returnValue("21.0.0.8"));
            }
        });

        catalog = loader.getCatalog();
        assertSame("FAIL: Did not get back the mocked Catalog instance",
                   mockPersistedCatalog, catalog);

        // Unset the COLLECTIVE provider, which should cause a cache clear
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Catalog.PERSIST_NAME, Catalog.class);
                will(returnValue(mockPersistedCatalog));

                one(mockPersistedCatalog).setIToolboxService(loader);

                one(mockPersistedCatalog).setIPersistenceProvider(mockFilePersistence);

                one(mockPersistedCatalog).setIFeatureToolService(mockFeatureToolService);

                one(mockPersistedCatalog).initialProcessFeatures();

                one(mockPersistedCatalog).validateSelf();
            }
        });

        catalog = loader.getCatalog();
        assertSame("FAIL: Did not get back the mocked Catalog instance",
                   mockPersistedCatalog, catalog);
    }

    private final void setLoadedCatalogFromProviderExpectations(final IPersistenceProvider provider) throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(provider).load(Catalog.PERSIST_NAME, Catalog.class);
                will(returnValue(mockPersistedCatalog));

                allowing(mockPersistedCatalog).setIToolboxService(loader);

                allowing(mockPersistedCatalog).setIPersistenceProvider(provider);

                allowing(mockPersistedCatalog).setIFeatureToolService(mockFeatureToolService);

                allowing(mockPersistedCatalog).initialProcessFeatures();

                allowing(mockPersistedCatalog).validateSelf();

                allowing(mockPersistedCatalog).getFeatureTools();
                will(returnValue(new ArrayList<FeatureTool>()));

                allowing(mockPersistedCatalog).getBookmarks();
                will(returnValue(new ArrayList<Bookmark>()));
            }
        });
    }

    /**
     * Sets up the 'load catalog' expectations
     *
     * @throws Exception
     */
    private final void setLoadedCatalogExpectations() throws Exception {
        setLoadedCatalogFromProviderExpectations(mockFilePersistence);
    }

    /**
     * Sets up the 'load catalog' expectations
     *
     * @throws Exception
     */
    private final void setLoadedCatalogFromCollectiveExpectations() throws Exception {
        setLoadedCatalogFromProviderExpectations(mockRepositoryPersistence);

        mock.checking(new Expectations() {
            {
                allowing(mockPersistedCatalog).get_metadata();
                will(returnValue(Collections.singletonMap("isDefault", false)));

                allowing(mockPersistedCatalog).getServerVersion();
                will(returnValue("21.0.0.8"));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_createDefault() throws Exception {
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(mockUserRegistry));

                one(mockUserRegistry).getUserDisplayName(USER_ID);
                will(returnValue(USER_DISPLAY_NAME));
            }
        });

        IToolbox toolbox = loader.getToolbox(USER_ID);

        assertTrue("FAIL: expected default toolbox to be returned",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default toolbox' message CWWKX1029I",
                   outputMgr.checkForMessages("CWWKX1029I:.*" + USER_ID));

        assertEquals("FAIL: getToolbox() should return a toolbox with the right user ID",
                     USER_ID, toolbox.getOwnerId());

        assertEquals("FAIL: getToolbox() should return a toolbox with the right user display name",
                     USER_DISPLAY_NAME, toolbox.getOwnerDisplayName());

        // Second get
        assertSame("FAIL: getToolbox() should return the same instance",
                   toolbox, loader.getToolbox(USER_ID));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_createDefaultNoDisplayName() throws Exception {
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(mockUserRegistry));

                one(mockUserRegistry).getUserDisplayName(USER_ID);
                will(throwException(new EntryNotFoundException("TestException")));
            }
        });

        IToolbox toolbox = loader.getToolbox(USER_ID);

        assertTrue("FAIL: expected default toolbox to be returned",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'unable to determine display name' message CWWKX1016E",
                   outputMgr.checkForMessages("CWWKX1016E:.*" + USER_ID));

        assertTrue("FAIL: Did not find the 'loaded default toolbox' message CWWKX1029I",
                   outputMgr.checkForMessages("CWWKX1029I:.*" + USER_ID));

        assertEquals("FAIL: getToolbox() should return a toolbox with the right user ID",
                     USER_ID, toolbox.getOwnerId());

        assertEquals("FAIL: getToolbox() should return a toolbox with the user display name as the user ID because we could not resolve to a display name",
                     USER_ID, toolbox.getOwnerDisplayName());

        // Second get
        assertSame("FAIL: getToolbox() should return the same instance",
                   toolbox, loader.getToolbox(USER_ID));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_badSyntax() throws Exception {
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new JSONMarshallException("Unable to parse non-well-formed content")));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(mockUserRegistry));

                one(mockUserRegistry).getUserDisplayName(USER_ID);
                will(returnValue(USER_DISPLAY_NAME));
            }
        });

        IToolbox toolbox = loader.getToolbox(USER_ID);

        assertTrue("FAIL: expected default toolbox to be returned",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default toolbox' message CWWKX1029I",
                   outputMgr.checkForMessages("CWWKX1029I:.*" + USER_ID));

        assertTrue("FAIL: Did not find the 'bad syntax' message CWWKX1030E",
                   outputMgr.checkForMessages("CWWKX1030E:.*" + USER_ID));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_badContent() throws Exception {
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new JSONMarshallException("Fatal problems occurred while mapping content")));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(mockUserRegistry));

                one(mockUserRegistry).getUserDisplayName(USER_ID);
                will(returnValue(USER_DISPLAY_NAME));
            }
        });

        IToolbox toolbox = loader.getToolbox(USER_ID);

        assertTrue("FAIL: expected default toolbox to be returned",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default toolbox' message CWWKX1029I",
                   outputMgr.checkForMessages("CWWKX1029I:.*" + USER_ID));

        assertTrue("FAIL: Did not find the 'bad content' message CWWKX1031E",
                   outputMgr.checkForMessages("CWWKX1031E:.*" + USER_ID));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_IOError() throws Exception {
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new IOException("Test Exception")));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(mockUserRegistry));

                one(mockUserRegistry).getUserDisplayName(USER_ID);
                will(returnValue(USER_DISPLAY_NAME));
            }
        });

        IToolbox toolbox = loader.getToolbox(USER_ID);

        assertTrue("FAIL: expected default toolbox to be returned",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default toolbox' message CWWKX1029I",
                   outputMgr.checkForMessages("CWWKX1029I:.*" + USER_ID));

        assertTrue("FAIL: Did not find the 'io error' message CWWKX1032E",
                   outputMgr.checkForMessages("CWWKX1032E:.*" + USER_ID));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_invalidToolbox() throws Exception {
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(returnValue(mockPersistedToolbox));

                one(mockPersistedToolbox).setCatalog(mockPersistedCatalog);

                one(mockPersistedToolbox).setPersistenceProvider(mockFilePersistence);

                one(mockPersistedToolbox).validateSelf();
                will(throwException(new InvalidToolboxException("TestException")));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(mockUserRegistry));

                one(mockUserRegistry).getUserDisplayName(USER_ID);
                will(returnValue(USER_DISPLAY_NAME));

            }
        });

        IToolbox toolbox = loader.getToolbox(USER_ID);

        assertTrue("FAIL: expected default toolbox to be returned",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default toolbox' message CWWKX1029I",
                   outputMgr.checkForMessages("CWWKX1029I:.*" + USER_ID));

        assertTrue("FAIL: Did not find the 'not valid toolbox' message CWWKX1033E",
                   outputMgr.checkForMessages("CWWKX1033E:.*" + USER_ID));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_loadedToolbox() throws Exception {
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(returnValue(mockPersistedToolbox));

                one(mockPersistedToolbox).setCatalog(mockPersistedCatalog);

                one(mockPersistedToolbox).setPersistenceProvider(mockFilePersistence);

                one(mockPersistedToolbox).validateSelf();
            }
        });

        IToolbox toolbox = loader.getToolbox(USER_ID);
        assertSame("FAIL: Did not get back the mocked Toolbox instance",
                   mockPersistedToolbox, toolbox);
        assertTrue("FAIL: Did not find the 'loaded toolbox' message CWWKX1034I",
                   outputMgr.checkForMessages("CWWKX1034I:.*" + USER_ID));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_loadedToolbox_promotedFromNonEncryptedToEncryptedName() throws Exception {
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + USER_ID, Toolbox.class);
                will(returnValue(mockPersistedToolbox));

                one(mockPersistedToolbox).setCatalog(mockPersistedCatalog);

                one(mockPersistedToolbox).setPersistenceProvider(mockFilePersistence);

                one(mockPersistedToolbox).validateSelf();

                one(mockPersistedToolbox).storeToolbox();

                one(mockFilePersistence).delete(Toolbox.PERSIST_NAME + "-" + USER_ID);
                will(returnValue(true));
            }
        });

        IToolbox toolbox = loader.getToolbox(USER_ID);
        assertSame("FAIL: Did not get back the mocked Toolbox instance",
                   mockPersistedToolbox, toolbox);
        assertTrue("FAIL: Did not find the 'loaded toolbox' message CWWKX1034I",
                   outputMgr.checkForMessages("CWWKX1034I:.*" + USER_ID));
        assertTrue("FAIL: Did not find the 'promoted toolbox' from non encrypted to encrypted name message CWWKX1068I",
                   outputMgr.checkForMessages("CWWKX1068I:.*" + USER_ID));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_loadedToolboxFromCollective() throws Exception {
        setLoadedCatalogFromCollectiveExpectations();

        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(returnValue(mockPersistedToolbox));

                one(mockPersistedToolbox).setCatalog(mockPersistedCatalog);

                one(mockPersistedToolbox).setPersistenceProvider(mockRepositoryPersistence);

                one(mockPersistedToolbox).validateSelf();
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        IToolbox toolbox = loader.getToolbox(USER_ID);
        assertSame("FAIL: Did not get back the mocked Toolbox instance",
                   mockPersistedToolbox, toolbox);
        assertTrue("FAIL: Did not find the 'loaded toolbox' message CWWKX1034I",
                   outputMgr.checkForMessages("CWWKX1034I:.*" + USER_ID));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_loadedToolboxFromCollective_promotedFromNonEncryptedToEncryptedName() throws Exception {
        setLoadedCatalogFromCollectiveExpectations();

        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(mockRepositoryPersistence).load(Toolbox.PERSIST_NAME + "-" + USER_ID, Toolbox.class);
                will(returnValue(mockPersistedToolbox));

                one(mockPersistedToolbox).setCatalog(mockPersistedCatalog);

                one(mockPersistedToolbox).setPersistenceProvider(mockRepositoryPersistence);

                one(mockPersistedToolbox).validateSelf();

                one(mockPersistedToolbox).storeToolbox();

                one(mockRepositoryPersistence).delete(Toolbox.PERSIST_NAME + "-" + USER_ID);
                will(returnValue(true));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        IToolbox toolbox = loader.getToolbox(USER_ID);
        assertSame("FAIL: Did not get back the mocked Toolbox instance",
                   mockPersistedToolbox, toolbox);
        assertTrue("FAIL: Did not find the 'loaded toolbox' message CWWKX1034I",
                   outputMgr.checkForMessages("CWWKX1034I:.*" + USER_ID));
        assertTrue("FAIL: Did not find the 'promoted toolbox' from non encrypted to encrypted name message CWWKX1068I",
                   outputMgr.checkForMessages("CWWKX1068I:.*" + USER_ID));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_loadedToolboxPromotedFromFileToCollective() throws Exception {
        setLoadedCatalogFromCollectiveExpectations();

        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("TestException")));

                one(mockRepositoryPersistence).load(Toolbox.PERSIST_NAME + "-" + USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("TestException")));

                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("TestException")));

                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + USER_ID, Toolbox.class);
                will(returnValue(mockPersistedToolbox));

                one(mockPersistedToolbox).setCatalog(mockPersistedCatalog);

                one(mockPersistedToolbox).setPersistenceProvider(mockRepositoryPersistence);

                one(mockPersistedToolbox).validateSelf();

                one(mockPersistedToolbox).storeToolbox();
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        IToolbox toolbox = loader.getToolbox(USER_ID);
        assertSame("FAIL: Did not get back the mocked Toolbox instance",
                   mockPersistedToolbox, toolbox);
        assertTrue("FAIL: Did not find the 'promoted toolbox' from file to collective persistence message CWWKX1039I",
                   outputMgr.checkForMessages("CWWKX1039I:.*FILE.*COLLECTIVE"));
        assertTrue("FAIL: Did not find the 'loaded toolbox' message CWWKX1034I",
                   outputMgr.checkForMessages("CWWKX1034I:.*" + USER_ID));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getCatalog()}.
     */
    @Test
    public void getToolbox_defaultToolboxStoredToCollective() throws Exception {
        setLoadedCatalogFromCollectiveExpectations();

        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("TestException")));

                one(mockRepositoryPersistence).load(Toolbox.PERSIST_NAME + "-" + USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("TestException")));

                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("TestException")));

                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("TestException")));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(mockUserRegistry));

                one(mockUserRegistry).getUserDisplayName(USER_ID);
                will(returnValue(USER_DISPLAY_NAME));

                one(mockRepositoryPersistence).store(with(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID), with(any(Toolbox.class)));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        loader.getToolbox(USER_ID);
        assertTrue("FAIL: Did not find the 'loaded default toolbox' message CWWKX1029I",
                   outputMgr.checkForMessages("CWWKX1029I:.*" + USER_ID));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox()}.
     */
    @Test
    public void getToolbox_changeProviderResetsCache() throws Exception {
        setLoadedCatalogExpectations();
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(returnValue(mockPersistedToolbox));

                one(mockPersistedToolbox).setCatalog(mockPersistedCatalog);

                one(mockPersistedToolbox).setPersistenceProvider(mockFilePersistence);

                one(mockPersistedToolbox).validateSelf();
            }
        });

        IToolbox toolbox = loader.getToolbox(USER_ID);
        assertSame("FAIL: Did not get back the mocked Toolbox instance",
                   mockPersistedToolbox, toolbox);

        // Set the COLLECTIVE provider, which should cause a cache clear
        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);

        setLoadedCatalogFromCollectiveExpectations();
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(returnValue(mockPersistedToolbox));

                one(mockPersistedToolbox).setCatalog(mockPersistedCatalog);

                one(mockPersistedToolbox).setPersistenceProvider(mockRepositoryPersistence);

                one(mockPersistedToolbox).validateSelf();
            }
        });

        toolbox = loader.getToolbox(USER_ID);
        assertSame("FAIL: Did not get back the mocked Toolbox instance",
                   mockPersistedToolbox, toolbox);

        // Unset the COLLECTIVE provider, which should cause a cache clear
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);

        setLoadedCatalogExpectations();
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(returnValue(mockPersistedToolbox));

                one(mockPersistedToolbox).setCatalog(mockPersistedCatalog);

                one(mockPersistedToolbox).setPersistenceProvider(mockFilePersistence);

                one(mockPersistedToolbox).validateSelf();
            }
        });

        toolbox = loader.getToolbox(USER_ID);
        assertSame("FAIL: Did not get back the mocked Toolbox instance",
                   mockPersistedToolbox, toolbox);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#getToolbox(java.lang.String)}.
     * TODO: Should we behave this way? Right now we're documenting WHAT we do, but its not clear if we SHOULD
     * behave this way.
     */
    @Test
    public void getToolbox_nullUserId() throws Exception {
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                exactly(2).of(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-null", Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(mockUserRegistry));

                one(mockUserRegistry).getUserDisplayName(null);
                will(returnValue(null));
            }
        });

        IToolbox toolbox = loader.getToolbox(null);

        assertTrue("FAIL: expected default toolbox to be returned",
                   (Boolean) toolbox.get_metadata().get(IToolbox.METADATA_IS_DEFAULT));

        assertTrue("FAIL: Did not find the 'loaded default toolbox' message CWWKX1029I",
                   outputMgr.checkForMessages("CWWKX1029I:.*null"));

        assertEquals("FAIL: getToolbox() should return a toolbox with the right user ID",
                     null, toolbox.getOwnerId());

        assertEquals("FAIL: getToolbox() should return a toolbox with the right user display name",
                     null, toolbox.getOwnerDisplayName());

        // Second get
        assertSame("FAIL: getToolbox() should return the same instance",
                   toolbox, loader.getToolbox(null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#setInstance(java.lang.String, com.ibm.ws.ui.internal.v1.IToolbox)}.
     */
    @Test
    public void setInstance() {
        loader.setToolbox(USER_ID, mockPersistedToolbox);
        IToolbox toolbox = loader.getToolbox(USER_ID);
        assertSame("FAIL: Did not get back the mocked persisted Toolbox",
                   mockPersistedToolbox, toolbox);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#setInstance(java.lang.String, com.ibm.ws.ui.internal.v1.IToolbox)}.
     */
    @Test
    public void setInstance_null() throws Exception {
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(mockUserRegistry));

                one(mockUserRegistry).getUserDisplayName(USER_ID);
                will(returnValue(USER_DISPLAY_NAME));
            }
        });

        IToolbox toolbox1 = loader.getToolbox(USER_ID);

        // Clear the catalog
        loader.setToolbox(USER_ID, null);

        // Do it again!
        setLoadedCatalogExpectations();

        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + ENCRYPTED_USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(mockFilePersistence).load(Toolbox.PERSIST_NAME + "-" + USER_ID, Toolbox.class);
                will(throwException(new FileNotFoundException("Test Exception")));

                one(wsSecurityService).getUserRegistry(null);
                will(returnValue(mockUserRegistry));

                one(mockUserRegistry).getUserDisplayName(USER_ID);
                will(returnValue(USER_DISPLAY_NAME));
            }
        });
        IToolbox toolbox2 = loader.getToolbox(USER_ID);
        assertFalse("FAIL: Should get back a different instance of the toolbox",
                    toolbox1 == toolbox2);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#resetAllToolboxes()}.
     */
    @Test
    public void resetAllToolboxes_noToolboxes() {
        loader.resetAllToolboxes();
        // Nothing should blow up!
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#resetAllToolboxes()}.
     */
    @Test
    public void resetAllToolboxes_withToolboxes() {
        loader.setToolbox(USER_ID, mockPersistedToolbox);

        mock.checking(new Expectations() {
            {
                one(mockPersistedToolbox).reset();
            }
        });
        loader.resetAllToolboxes();
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#removeToolEntryFromAllToolboxes(String)}.
     */
    @Test
    public void removeToolFromAllToolboxes_noToolboxes() {
        final String toolId = "Tool-1.0";
        loader.removeToolEntryFromAllToolboxes(toolId);
        // Nothing should blow up!
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.POJOLoaderService#removeToolEntryFromAllToolboxes(java.lang.String)}.
     */
    @Test
    public void removeToolFromAllToolboxes() {
        loader.setToolbox(USER_ID, mockPersistedToolbox);

        final String toolId = "Tool-1.0";
        mock.checking(new Expectations() {
            {
                one(mockPersistedToolbox).deleteToolEntry(toolId);
            }
        });
        loader.removeToolEntryFromAllToolboxes(toolId);
    }

}
