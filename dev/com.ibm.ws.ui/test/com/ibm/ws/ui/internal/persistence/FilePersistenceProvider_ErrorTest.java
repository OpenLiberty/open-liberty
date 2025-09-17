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
package com.ibm.ws.ui.internal.persistence;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
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
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.ws.ui.internal.v1.pojo.Catalog;
import com.ibm.ws.ui.persistence.IPersistenceDebugger;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 * This test DOES back-end to disk. However it does so under the build directory
 * so any effects this has will be resolved with a clean.
 */
public class FilePersistenceProvider_ErrorTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WsLocationAdmin> locRef = mock.mock(ServiceReference.class);
    private final WsLocationAdmin loc = mock.mock(WsLocationAdmin.class);
    private final WsResource resource = mock.mock(WsResource.class);
    private final IPersistenceDebugger mockDebugger = mock.mock(IPersistenceDebugger.class);
    private final JSON mockJson = mock.mock(JSON.class);

    private FilePersistenceProvider persist;

    /**
     * Activate the service using with mocks.
     */
    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(FilePersistenceProvider.KEY_LOCATION_SERVICE, locRef);
                will(returnValue(loc));
            }
        });

        persist = new FilePersistenceProvider(mockJson, mockDebugger);
        persist.setLocationService(locRef);
        persist.activate(cc);
    }

    /**
     * Deactive the service.
     */
    @After
    public void tearDown() {
        persist.deactivate();
        persist.unsetLocationService(locRef);
        persist = null;

        mock.assertIsSatisfied();
    }

    /**
     * Sets up the mock so that the requested name will return a File object.
     * That File can be the file passed in, or it will be created if null.
     * 
     * @param name Expected persistence name
     * @param mockFile Desired file to return. Can be {@code null}
     * @return The File (which may be a mock)
     */
    private File setMockFileLocation(final String name, final File mockFile) {
        final File f = (mockFile != null) ? mockFile : new File("build/unittest/testdata/" + name + ".json");

        mock.checking(new Expectations() {
            {
                one(loc).resolveResource(FilePersistenceProvider.DEFAULT_PERSIST_LOCATION + name + ".json");
                will(returnValue(resource));

                one(resource).asFile();
                will(returnValue(f));
            }
        });

        return f;
    }

    /**
     * Set the mapper to expect a read call to the given file and class, and
     * to respond with the specified exception.
     * 
     * @param f Expected file
     * @param c Expected class
     * @param ex Exception to throw
     */
    private void setMapperExpectedException(final File f, final Class<?> c, final Exception ex) throws Exception {
        mock.checking(new Expectations() {
            {
                one(mockJson).parse(f, c);
                will(throwException(ex));
            }
        });
    }

    /**
     * Set the mock to expect a debugger call to get the file contents.
     * 
     * @param f The file to debug
     */
    private void setMockDebugException(final File f) throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(mockDebugger).getFileContents(f);
                will(returnValue("MockedContents"));
            }
        });
    }

    /**
     * Create a mock File which will fail to create its parent directories.
     * 
     * @return The mock File
     */
    private File createMockStoreFileMkdirsFails(final String name) {
        final File mockFile = mock.mock(File.class, "mockFile");
        final File mockParentFile = mock.mock(File.class, "mockParentFile");

        mock.checking(new Expectations() {
            {
                allowing(mockFile).getAbsolutePath();
                will(returnValue(name));

                one(mockFile).getParentFile();
                will(returnValue(mockParentFile));

                one(mockParentFile).exists();
                will(returnValue(false));

                one(mockParentFile).mkdirs();
                will(returnValue(false));
            }
        });
        return mockFile;
    }

    /**
     * Create a mock File which will fail to create its parent directories.
     * 
     * @return The mock File
     */
    private File createMockStoreFile(final String name) {
        final File mockFile = mock.mock(File.class, "mockFile");
        final File mockParentFile = mock.mock(File.class, "mockParentFile");

        mock.checking(new Expectations() {
            {
                allowing(mockFile).getAbsolutePath();
                will(returnValue(name));

                one(mockFile).getParentFile();
                will(returnValue(mockParentFile));

                one(mockParentFile).exists();
                will(returnValue(true));
            }
        });
        return mockFile;
    }

    @Test
    public void storeCanNotCreateParentDirs() throws Exception {
        final String name = "idontexist";
        final String expectedMsgContent = "Unable to create required parent directories";

        final File mockFile = createMockStoreFileMkdirsFails(name);
        setMockFileLocation("idontexist", mockFile);

        try {
            persist.store(name, new Object());
            fail("Store should throw a IOException when it can not create the parent directories");
        } catch (IOException e) {
            assertTrue("FAIL: The expected message did not contain '" + expectedMsgContent + "'",
                       e.getMessage().contains(expectedMsgContent));
        }

        assertTrue("FAIL: the expected error message was not logged",
                   outputMgr.checkForMessages("CWWKX1014E:.*" + name + ".*" + expectedMsgContent));
    }

    @Test
    public void storeJsonGenerationException() throws Exception {
        final String name = "idontexist";
        final Object pojo = new Object();

        final File mockFile = createMockStoreFile(name);
        setMockFileLocation("idontexist", mockFile);

        mock.checking(new Expectations() {
            {
                one(mockJson).serializeToFile(mockFile, pojo);
                will(throwException(new JSONMarshallException("TestException")));
            }
        });

        try {
            persist.store(name, pojo);
            fail("Load should throw a JSONMarshallException");
        } catch (JSONMarshallException e) {
            assertTrue("FAIL: The JSONMarshallException was not re-thrown as-is",
                       e.getMessage().equals("TestException"));
        }

        assertTrue("FAIL: an unexpected message was logged",
                   outputMgr.isEmptyMessageLog());
    }

    @Test
    public void storeCantConvertPOJO() throws Exception {
        final String name = "idontexist";
        final Object pojo = new Object();

        final File mockFile = createMockStoreFile(name);
        setMockFileLocation("idontexist", mockFile);

        mock.checking(new Expectations() {
            {
                one(mockJson).serializeToFile(mockFile, pojo);
                will(throwException(new JSONMarshallException("TestException")));
            }
        });

        try {
            persist.store(name, pojo);
            fail("Load should throw a JSONMarshallException");
        } catch (JSONMarshallException e) {
            assertTrue("FAIL: The JSONMarshallException was not re-thrown as-is",
                       e.getMessage().equals("TestException"));
        }

        assertTrue("FAIL: an unexpected message was logged",
                   outputMgr.isEmptyMessageLog());
    }

    @Test
    public void storeJacksonIOError() throws Exception {
        final String name = "idontexist";
        final Object pojo = new Object();

        final File mockFile = createMockStoreFile(name);
        setMockFileLocation("idontexist", mockFile);

        mock.checking(new Expectations() {
            {
                one(mockJson).serializeToFile(mockFile, pojo);
                will(throwException(new JSONMarshallException("I/O exception of some sort has occurred")));
            }
        });

        try {
            persist.store(name, pojo);
            fail("Load should throw a JSONMarshallException when the JSON API does");
        } catch (JSONMarshallException e) {
            assertTrue("FAIL: The JSONMarshallException was not re-thrown as-is",
                       e.getMessage().equals("I/O exception of some sort has occurred"));
        }

        assertTrue("FAIL: the expected error message was not logged",
                   outputMgr.checkForMessages("CWWKX1014E:.*" + name + ".*I/O exception of some sort has occurred"));
    }

    @Test
    public void loadDoesntExist() throws Exception {
        final String name = "idontexist";
        final Class<Catalog> c = Catalog.class;

        final File f = setMockFileLocation("idontexist", null);
        setMapperExpectedException(f, c, new FileNotFoundException("TestException"));

        try {
            persist.load(name, c);
            fail("FAIL: Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException e) {
            assertTrue("FAIL: The FileNotFoundException was not rethrown as-is",
                       e.getMessage().equals("TestException"));
        }

        assertTrue("No message should be logged on a FileNotFoundException",
                   outputMgr.isEmptyMessageLog());
    }

    @Test
    public void loadBadJsonSyntax() throws Exception {
        final String name = "idontexist";
        final Class<Catalog> c = Catalog.class;

        final File f = setMockFileLocation("idontexist", null);
        setMapperExpectedException(f, c, new JSONMarshallException("Unable to parse non-well-formed content"));
        setMockDebugException(f);

        try {
            persist.load(name, c);
            fail("FAIL: Expected JSONMarshallException was not thrown");
        } catch (JSONMarshallException e) {
            assertTrue("FAIL: The JSONMarshallException was not rethrown as-is",
                       e.getMessage().equals("Unable to parse non-well-formed content"));
        }

        assertTrue("FAIL: Did not find expected CWWKX1009E message. Possibly the injected variables were bad",
                   outputMgr.checkForMessages("CWWKX1009E:.*idontexist.*MockedContents"));
    }

    @Test
    public void loadWrongJsonContent() throws Exception {
        final String name = "idontexist";
        final Class<Catalog> c = Catalog.class;

        final File f = setMockFileLocation("idontexist", null);
        setMapperExpectedException(f, c, new JSONMarshallException("Fatal problems occurred while mapping content"));
        setMockDebugException(f);

        try {
            persist.load(name, c);
            fail("FAIL: Expected JSONMarshallException was not thrown");
        } catch (JSONMarshallException e) {
            assertTrue("FAIL: The JSONMarshallException was not rethrown as-is",
                       e.getMessage().equals("Fatal problems occurred while mapping content"));
        }

        assertTrue("FAIL: Did not find expected CWWKX1010E message. Possibly the injected variables were bad",
                   outputMgr.checkForMessages("CWWKX1010E:.*idontexist.*Catalog.*MockedContents"));
    }

    @Test
    public void loadIOError() throws Exception {
        final String name = "idontexist";
        final Class<Catalog> c = Catalog.class;

        final File f = setMockFileLocation("idontexist", null);
        setMapperExpectedException(f, c, new IOException("TestException"));

        try {
            persist.load(name, c);
            fail("FAIL: Expected IOException was not thrown");
        } catch (IOException e) {
            assertTrue("FAIL: The IOException was not rethrown as-is",
                       e.getMessage().equals("TestException"));
        }

        assertTrue("FAIL: Did not find expected CWWKX1011E message. Possibly the injected variables were bad",
                   outputMgr.checkForMessages("CWWKX1011E:.*idontexist.*TestException"));
    }
}
