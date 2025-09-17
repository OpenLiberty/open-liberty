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
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.ui.internal.v1.utils.LogRule;
import com.ibm.ws.ui.persistence.IPersistenceProvider;

import test.common.SharedOutputManager;

/**
 *
 */
public class PlainTextLoaderServiceTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = new LogRule(outputMgr);

    private static final String USER_ID = "bob_johnson";
    private static final String ENCRYPTED_USER_ID = "Ym9iX2pvaG5zb24=";

    private static final String TOOL_NAME = "tool";
    private static final String TOOL_DATA = "{\"key\":\"value\"}";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final IPersistenceProvider mockFilePersistence = mock.mock(IPersistenceProvider.class, "mockFilePersistence");
    private final IPersistenceProvider mockRepositoryPersistence = mock.mock(IPersistenceProvider.class, "mockRepositoryPersistence");
    private PlainTextLoaderService loader;

    @Before
    public void setUp() {
        loader = new PlainTextLoaderService();
        loader.setIPersistenceProviderFILE(mockFilePersistence);
        loader.activate();
    }

    @After
    public void tearDown() {
        loader.deactive();
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        loader.unsetIPersistenceProviderFILE(mockFilePersistence);
        loader = null;

        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#setIPersistenceProvider(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setIPersistenceProviderFILE_printsCorrectMessage() {
        loader.setIPersistenceProviderFILE(mockFilePersistence);

        assertTrue("FAIL: The FILE provider type was not reported correctly",
                   outputMgr.checkForMessages("CWWKX1063I:.*FILE"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#setIPersistenceProvider(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setIPersistenceProviderCOLLECTIVE_printsCorrectMessage() {
        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);

        assertTrue("FAIL: The COLLECTIVE provider type was not reported correctly",
                   outputMgr.checkForMessages("CWWKX1063I:.*COLLECTIVE"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#setIPersistenceProvider(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetIPersistenceProviderCOLLECTIVE_printsCorrectMessage() {
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);

        assertTrue("FAIL: The FILE provider type was not reported correctly when COLLECTIVE is unset",
                   outputMgr.checkForMessages("CWWKX1063I:.*FILE"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#exists()}.
     */
    @Test
    public void exists_filePersistence() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
            }
        });

        assertTrue("FAIL: expected true to be returned", loader.exists(USER_ID, TOOL_NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#exists()}.
     */
    @Test
    public void exists_RepoPersistence() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        assertTrue("FAIL: expected true to be returned", loader.exists(USER_ID, TOOL_NAME));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#promoteIfPossible()}.
     */
    @Test
    public void promoteIfPossible_fileExist_noPromote() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        loader.promoteIfPossible(USER_ID, TOOL_NAME);
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#promoteIfPossible()}.
     */
    @Test
    public void promoteIfPossible_noPromote() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(false));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(false));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + USER_ID);
                will(returnValue(false));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        loader.promoteIfPossible(USER_ID, TOOL_NAME);
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#promoteIfPossible()}.
     */
    @Test
    public void promoteIfPossible_fileFoundEncryptedName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(false));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(TOOL_DATA));
                one(mockRepositoryPersistence).storePlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID, TOOL_DATA);
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        loader.promoteIfPossible(USER_ID, TOOL_NAME);
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#promoteIfPossible()}.
     */
    @Test
    public void promoteIfPossible_fileFoundNonEncryptedName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(false));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(false));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + USER_ID);
                will(returnValue(TOOL_DATA));
                one(mockRepositoryPersistence).storePlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID, TOOL_DATA);
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        loader.promoteIfPossible(USER_ID, TOOL_NAME);
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#promoteIfPossible()}.
     */
    @Test
    public void promoteIfPossible_filenotfoundException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(false));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(throwException(new FileNotFoundException("Test Exception")));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + USER_ID);
                will(returnValue(false));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        loader.promoteIfPossible(USER_ID, TOOL_NAME);
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#promoteIfPossible()}.
     */
    @Test
    public void promoteIfPossible_ioException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(false));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(false));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + USER_ID);
                will(throwException(new IOException("Test Exception")));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        loader.promoteIfPossible(USER_ID, TOOL_NAME);
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#deleteToolData()}.
     */
    @Test
    public void deleteToolData_IOE_FilePersistence() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
                one(mockRepositoryPersistence).delete(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(throwException(new IOException("Test Exception")));
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + USER_ID);
                will(returnValue(true));
                one(mockRepositoryPersistence).delete(TOOL_NAME + "/" + USER_ID);
                will(returnValue(true));

                one(mockFilePersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).delete(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).delete(TOOL_NAME + "/" + USER_ID);
                will(returnValue(true));

            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        assertFalse("Delete should be false", loader.deleteToolData(USER_ID, TOOL_NAME));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#deleteToolData()}.
     */
    @Test
    public void deleteToolData_IOE_RepoPersistence() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + USER_ID);
                will(returnValue(false));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + USER_ID);
                will(returnValue(false));
                one(mockRepositoryPersistence).delete(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).delete(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(throwException(new IOException("Test Exception")));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        assertFalse("Delete should be false", loader.deleteToolData(USER_ID, TOOL_NAME));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#getToolData()}.
     */
    @Test
    public void getToolData_fromRepo_encryptedName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).loadPlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(TOOL_DATA));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        assertEquals("Load from REPO using encrypted nmae should return '" + TOOL_DATA + "'", TOOL_DATA, loader.getToolData(USER_ID, TOOL_NAME));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#getToolData()}.
     */
    @Test
    public void getToolData_fromRepo_nonEncryptedName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).loadPlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(throwException(new FileNotFoundException()));
                one(mockRepositoryPersistence).loadPlainText(TOOL_NAME + "/" + USER_ID);
                will(returnValue(TOOL_DATA));
                one(mockRepositoryPersistence).exists(TOOL_NAME + "/" + USER_ID);
                will(returnValue(true));
                one(mockRepositoryPersistence).delete(TOOL_NAME + "/" + USER_ID);
                will(returnValue(true));
                one(mockRepositoryPersistence).storePlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID, TOOL_DATA);
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        assertEquals("Load from REPO using non encrypted name should return '" + TOOL_DATA + "'", TOOL_DATA, loader.getToolData(USER_ID, TOOL_NAME));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#getToolData()}.
     */
    @Test
    public void getToolData_fromFILE_encryptedName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(returnValue(TOOL_DATA));
            }
        });

        assertEquals("Load from FILE using encrypted name should return '" + TOOL_DATA + "'", TOOL_DATA, loader.getToolData(USER_ID, TOOL_NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#getToolData()}.
     */
    @Test
    public void getToolData_fromFILE_nonEncryptedName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(throwException(new FileNotFoundException()));
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + USER_ID);
                will(returnValue(TOOL_DATA));
                one(mockFilePersistence).exists(TOOL_NAME + "/" + USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).delete(TOOL_NAME + "/" + USER_ID);
                will(returnValue(true));
                one(mockFilePersistence).storePlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID, TOOL_DATA);
            }
        });

        assertEquals("Load from FILE using non encrypted name should return '" + TOOL_DATA + "'", TOOL_DATA, loader.getToolData(USER_ID, TOOL_NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#getToolData()}.
     */
    @Test
    public void getToolData_FNFException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(throwException(new FileNotFoundException()));
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + USER_ID);
                will(throwException(new FileNotFoundException()));
            }
        });

        assertEquals("Load from FILE should return null.", null, loader.getToolData(USER_ID, TOOL_NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#getToolData()}.
     */
    @Test
    public void getToolData_IOException_encryptedName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(throwException(new IOException()));
            }
        });

        assertEquals("Load from FILE using encryption name with IO Exception should return null.", null, loader.getToolData(USER_ID, TOOL_NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#getToolData()}.
     */
    @Test
    public void getToolData_IOException_nonEncryptedName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID);
                will(throwException(new FileNotFoundException()));
                one(mockFilePersistence).loadPlainText(TOOL_NAME + "/" + USER_ID);
                will(throwException(new IOException()));
            }
        });

        assertEquals("Load from FILE using non encryption name with IO Exception should return null.", null, loader.getToolData(USER_ID, TOOL_NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#addToolData()}.
     */
    @Test
    public void addToolData_fromFILE() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).storePlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID, TOOL_DATA);
            }
        });

        assertEquals("add to FILE should return '" + TOOL_DATA + "'", TOOL_DATA, loader.addToolData(USER_ID, TOOL_NAME, TOOL_DATA));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#addToolData()}.
     */
    @Test
    public void addToolData_fromFile_IOE() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockFilePersistence).storePlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID, TOOL_DATA);
                will(throwException(new IOException()));
            }
        });

        assertEquals("add to FILE should return null", null, loader.addToolData(USER_ID, TOOL_NAME, TOOL_DATA));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#addToolData()}.
     */
    @Test
    public void addToolData_fromRepo() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).storePlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID, TOOL_DATA);
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        assertEquals("post to Repository should return '" + TOOL_DATA + "'", TOOL_DATA, loader.addToolData(USER_ID, TOOL_NAME, TOOL_DATA));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.PlainTextLoaderService#addToolData()}.
     */
    @Test
    public void addToolData_fromRepo_IOE() throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockRepositoryPersistence).storePlainText(TOOL_NAME + "/" + ENCRYPTED_USER_ID, TOOL_DATA);
                will(throwException(new IOException()));
            }
        });

        loader.setIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
        assertEquals("post to Repository should return null", null, loader.addToolData(USER_ID, TOOL_NAME, TOOL_DATA));
        loader.unsetIPersistenceProviderCOLLECTIVE(mockRepositoryPersistence);
    }

}
