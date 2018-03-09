/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertPathBuilderException;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.internal.ExceptionUtils;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendIOException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.IfixResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

/**
 *
 */
public class ExceptionUtilsTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testCreateFailedToDownloadIOException() {
        RepositoryResourceImpl mr = new EsaResourceImpl(null);
        Exception e = new IOException();
        File toDir = new File("temp");
        InstallException ie = ExceptionUtils.createFailedToDownload(mr, e, toDir);
        assertTrue("message does not contain CWWKF1283E", ie.getMessage().contains("CWWKF1283E"));
        assertTrue("message does not contain feature", ie.getMessage().contains("feature"));
        assertTrue("message does not contain temp", ie.getMessage().contains("temp"));
        assertEquals("rc is not IO_FAILURE", InstallException.IO_FAILURE, ie.getRc());
    }

    @Test
    public void testCreateFailedToDownloadRepositoryResourceException() {
        SampleResourceImpl sr = new SampleResourceImpl(null);
        sr.setType(ResourceType.PRODUCTSAMPLE);
        Exception e = new RepositoryBadDataException(null, null, null);
        InstallException ie = ExceptionUtils.createFailedToDownload(sr, e, null);
        assertTrue("message does not contain CWWKF1284E", ie.getMessage().contains("CWWKF1284E"));
        assertTrue("message does not contain product sample", ie.getMessage().contains("product sample"));
        assertEquals("rc is not RUNTIME_EXCEPTION", InstallException.RUNTIME_EXCEPTION, ie.getRc());
    }

    @Test
    public void testCreateFailedToDownloadRepositoryBackendException() {
        SampleResourceImpl sr = new SampleResourceImpl(null);
        sr.setType(ResourceType.OPENSOURCE);
        RepositoryBackendIOException e = new RepositoryBackendIOException("", new AbstractRepositoryConnection() {
            @Override
            public boolean isRepositoryAvailable() {
                return false;
            }

            @Override
            public void checkRepositoryStatus() throws IOException, RequestFailureException {}

            @Override
            public String getRepositoryLocation() {
                return "http://abc";
            }

            @Override
            public RepositoryReadableClient createClient() {
                // TODO Auto-generated method stub
                return null;
            }
        });
        InstallException ie = ExceptionUtils.createFailedToDownload(sr, e, null);
        assertTrue("message does not contain CWWKF1285E", ie.getMessage().contains("CWWKF1285E"));
        assertTrue("message does not contain open source integration", ie.getMessage().contains("open source integration"));
        assertTrue("message does not contain http://abc", ie.getMessage().contains("http://abc"));
        assertEquals("rc is not RUNTIME_EXCEPTION", InstallException.RUNTIME_EXCEPTION, ie.getRc());
    }

    @Test
    public void testCreateFailedToDownloadDefaultRepository() {
        RepositoryResourceImpl mr = new EsaResourceImpl(null);
        RepositoryBackendIOException e = new RepositoryBackendIOException("", new AbstractRepositoryConnection() {
            @Override
            public boolean isRepositoryAvailable() {
                return false;
            }

            @Override
            public void checkRepositoryStatus() throws IOException, RequestFailureException {}

            @Override
            public String getRepositoryLocation() {
                return "https://asset-websphere.ibm.com/ma/v1";
            }

            @Override
            public RepositoryReadableClient createClient() {
                // TODO Auto-generated method stub
                return null;
            }
        });
        InstallException ie = ExceptionUtils.createFailedToDownload(mr, e, null);
        assertTrue("message does not contain CWWKF1286E", ie.getMessage().contains("CWWKF1286E"));
        assertTrue("message does not contain feature", ie.getMessage().contains("feature"));
        assertTrue("message does not contain IBM WebSphere Liberty Repository", ie.getMessage().contains("IBM WebSphere Liberty Repository"));
        assertEquals("rc is not RUNTIME_EXCEPTION", InstallException.RUNTIME_EXCEPTION, ie.getRc());
    }

    @Test
    public void testCreateFailedToDownloadDefaultRepositoryInvalidCert() {
        CertPathBuilderException caused = new CertPathBuilderException();
        RepositoryBackendIOException e = new RepositoryBackendIOException(caused, new AbstractRepositoryConnection() {
            @Override
            public boolean isRepositoryAvailable() {
                return false;
            }

            @Override
            public void checkRepositoryStatus() throws IOException, RequestFailureException {}

            @Override
            public String getRepositoryLocation() {
                return "https://asset-websphere.ibm.com/ma/v1";
            }

            @Override
            public RepositoryReadableClient createClient() {
                // TODO Auto-generated method stub
                return null;
            }
        });
        InstallException ie = ExceptionUtils.createFailedToDownload(null, e, null);
        assertTrue("message does not contain CWWKF1280E", ie.getMessage().contains("CWWKF1280E"));
        assertTrue("message does not contain IBM WebSphere Liberty Repository", ie.getMessage().contains("IBM WebSphere Liberty Repository"));
        assertEquals("rc is not RUNTIME_EXCEPTION", InstallException.RUNTIME_EXCEPTION, ie.getRc());
    }

    @Test
    public void testCreateFailedToDownloadInvalidCert() {
        CertPathBuilderException caused = new CertPathBuilderException();
        RepositoryBackendIOException e = new RepositoryBackendIOException(caused, new AbstractRepositoryConnection() {
            @Override
            public boolean isRepositoryAvailable() {
                return false;
            }

            @Override
            public void checkRepositoryStatus() throws IOException, RequestFailureException {}

            @Override
            public String getRepositoryLocation() {
                return "https://abc";
            }

            @Override
            public RepositoryReadableClient createClient() {
                // TODO Auto-generated method stub
                return null;
            }
        });
        InstallException ie = ExceptionUtils.createFailedToDownload(null, e, null);
        assertTrue("message does not contain CWWKF1282E", ie.getMessage().contains("CWWKF1282E"));
        assertTrue("message does not contain https://abc", ie.getMessage().contains("https://abc"));
        assertEquals("rc is not RUNTIME_EXCEPTION", InstallException.RUNTIME_EXCEPTION, ie.getRc());
    }

    @Test
    public void testCreateFailedToDownloadExceptionFeature() {
        RepositoryResourceImpl mr = new EsaResourceImpl(null);
        Exception e = new Exception();
        File toDir = new File("temp");
        InstallException ie = ExceptionUtils.createFailedToDownload(mr, e, toDir);
        assertTrue("message does not contain CWWKF1212E", ie.getMessage().contains("CWWKF1212E"));
        assertTrue("message does not contain feature", ie.getMessage().contains("feature"));
        assertTrue("message does not contain temp", ie.getMessage().contains("temp"));
        assertEquals("rc is not IO_FAILURE", InstallException.IO_FAILURE, ie.getRc());
    }

    @Test
    public void testCreateFailedToDownloadExceptionFix() {
        RepositoryResourceImpl mr = new IfixResourceImpl(null);
        Exception e = new Exception();
        File toDir = new File("temp");
        InstallException ie = ExceptionUtils.createFailedToDownload(mr, e, toDir);
        assertTrue("message does not contain CWWKF1213E", ie.getMessage().contains("CWWKF1213E"));
        assertTrue("message does not contain fix", ie.getMessage().contains("fix"));
        assertTrue("message does not contain temp", ie.getMessage().contains("temp"));
        assertEquals("rc is not IO_FAILURE", InstallException.IO_FAILURE, ie.getRc());
    }

    @Test
    public void testCreateFailedToDownloadExceptionSample() {
        SampleResourceImpl sr = new SampleResourceImpl(null);
        sr.setType(ResourceType.PRODUCTSAMPLE);
        Exception e = new Exception();
        File toDir = new File("temp");
        InstallException ie = ExceptionUtils.createFailedToDownload(sr, e, toDir);
        assertTrue("message does not contain CWWKF1251E", ie.getMessage().contains("CWWKF1251E"));
        assertTrue("message does not contain product sample", ie.getMessage().contains("product sample"));
        assertTrue("message does not contain temp", ie.getMessage().contains("temp"));
        assertEquals("rc is not IO_FAILURE", InstallException.IO_FAILURE, ie.getRc());
    }

    @Test
    public void testCreateFailedToDownloadExceptionOpenSource() {
        SampleResourceImpl sr = new SampleResourceImpl(null);
        sr.setType(ResourceType.OPENSOURCE);
        Exception e = new Exception();
        File toDir = new File("temp");
        InstallException ie = ExceptionUtils.createFailedToDownload(sr, e, toDir);
        assertTrue("message does not contain CWWKF1252E", ie.getMessage().contains("CWWKF1252E"));
        assertTrue("message does not contain open source integration", ie.getMessage().contains("open source integration"));
        assertTrue("message does not contain temp", ie.getMessage().contains("temp"));
        assertEquals("rc is not IO_FAILURE", InstallException.IO_FAILURE, ie.getRc());
    }
}
