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
package com.ibm.jbatch.container.ws.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.management.DynamicMBean;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.websphere.kernel.server.ServerInfoMBean;


public class BatchLocationServiceImplTest {

    /**
     * Mock environment.
     */
    private Mockery mockery = null;

    /**
     * Create the mockery environment for each test. Setting up a new mockery
     * environment for each test helps isolate Expectation sets, making it easier to
     * debug when some Expectation fails and all the Expectations are dumped
     * to the error log.
     */
    @Before
    public void before() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    /**
     * There are alternative ways to do this.
     * 1) Use @RunWith(JMock.class) (deprecated)
     * 2) Declare the field: @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();
     * (this version of Junit is not in our codebase).
     * 
     * Doing it the manual way for now.
     */
    @After
    public void after() {
        mockery.assertIsSatisfied();
    }
    
    /**
     * 
     */
    @Test
    public void testResolveHostNullDefaultHostname() throws Exception {
        
        final ServerInfoMBean mock = mockery.mock(ServerInfoMBean.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mock).getDefaultHostname();
                will(returnValue(null));  
            }
        });
        
        BatchLocationServiceImpl batchLocationService = new BatchLocationServiceImpl();
        batchLocationService.setServerInfoMBean(mock);
        
        assertEquals( batchLocationService.getLocalHostIpAddress(), batchLocationService.resolveHost("*") );
        assertEquals( "abc.com", batchLocationService.resolveHost("abc.com") );
    }
    
    /**
     * 
     */
    @Test
    public void testGetBatchRestUrlUnavailable() throws Exception {
        
        assertEquals( BatchLocationServiceImpl.BatchRestUrlUnavailable, new BatchLocationServiceImpl().getBatchRestUrl());
    }
    
    /**
     * 
     */
    @Test
    public void testResolveHostLocalhostDefaultHostname() throws Exception {
        
        final ServerInfoMBean mock = mockery.mock(ServerInfoMBean.class);

        mockery.checking( new Expectations() {
            {
                oneOf(mock).getDefaultHostname();
                will(returnValue("localhost"));  
            }
        });
        
        BatchLocationServiceImpl batchLocationService = new BatchLocationServiceImpl();
        batchLocationService.setServerInfoMBean(mock);
        
        assertEquals( batchLocationService.getLocalHostIpAddress(), batchLocationService.resolveHost("*") );
    }
    
    /**
     * 
     */
    @Test
    public void testGetBatchRestUrl() throws Exception {
        
        final DynamicMBean mockEndpointInfo = mockery.mock(DynamicMBean.class);
        final ServerInfoMBean mockServerInfo = mockery.mock(ServerInfoMBean.class);

        final int port = 1234;
        final String host = "*";
        final String defaultHostname = null;
        
        mockery.checking( new Expectations() {
            {
                oneOf(mockEndpointInfo).getAttribute("Host");
                will(returnValue(host));
                
                oneOf(mockEndpointInfo).getAttribute("Port");
                will(returnValue(port));
                
                oneOf(mockServerInfo).getDefaultHostname();
                will(returnValue(defaultHostname));  
            }
        });
        
        BatchLocationServiceImpl batchLocationService = new BatchLocationServiceImpl();
        batchLocationService.setServerInfoMBean(mockServerInfo);
        batchLocationService.setEndPointInfoMBean(mockEndpointInfo);
        
        
        String expected = "https://" + batchLocationService.getLocalHostIpAddress() + ":" + port + "/ibm/api/batch";
        
        assertEquals( expected, batchLocationService.getBatchRestUrl() );
    }
    
    /**
     * 
     */
    @Test
    public void testGetBatchRestUrlNamedHost() throws Exception {
        
        final DynamicMBean mockEndpointInfo = mockery.mock(DynamicMBean.class);
        final ServerInfoMBean mockServerInfo = mockery.mock(ServerInfoMBean.class);

        final int port = 1234;
        final String host = "abc.com";
        final String defaultHostname = null;
        
        mockery.checking( new Expectations() {
            {
                oneOf(mockEndpointInfo).getAttribute("Host");
                will(returnValue(host));
                
                oneOf(mockEndpointInfo).getAttribute("Port");
                will(returnValue(port));
            }
        });
        
        BatchLocationServiceImpl batchLocationService = new BatchLocationServiceImpl();
        batchLocationService.setServerInfoMBean(mockServerInfo);
        batchLocationService.setEndPointInfoMBean(mockEndpointInfo);
        
        String expected = "https://" + host + ":" + port + "/ibm/api/batch";
        
        assertEquals( expected, batchLocationService.getBatchRestUrl() );
    }
    
    /**
     * 
     */
    @Test
    public void testGetBatchRestUrlNamedDefaultHost() throws Exception {
        
        final DynamicMBean mockEndpointInfo = mockery.mock(DynamicMBean.class);
        final ServerInfoMBean mockServerInfo = mockery.mock(ServerInfoMBean.class);

        final int port = 1234;
        final String host = "*";
        final String defaultHostname = "def.com";
        
        mockery.checking( new Expectations() {
            {
                oneOf(mockEndpointInfo).getAttribute("Host");
                will(returnValue(host));
                
                oneOf(mockEndpointInfo).getAttribute("Port");
                will(returnValue(port));
                
                oneOf(mockServerInfo).getDefaultHostname();
                will(returnValue(defaultHostname)); 
            }
        });
        
        BatchLocationServiceImpl batchLocationService = new BatchLocationServiceImpl();
        batchLocationService.setServerInfoMBean(mockServerInfo);
        batchLocationService.setEndPointInfoMBean(mockEndpointInfo);
        
        String expected = "https://" + defaultHostname + ":" + port + "/ibm/api/batch";
        
        assertEquals( expected, batchLocationService.getBatchRestUrl() );
    }
    
    /**
     * 
     */
    @Test
    public void testIsLocalJobExecution() throws Exception {
        final DynamicMBean mockEndpointInfo = mockery.mock(DynamicMBean.class);
        final ServerInfoMBean mockServerInfo = mockery.mock(ServerInfoMBean.class);
        final WSJobExecution mockWSJobExecution = mockery.mock(WSJobExecution.class);

        final int port = 1234;
        final String host = "*";
        final String defaultHostname = "def.com";
        final String jobExecRestUrl = "https://" + defaultHostname + ":" + port + "/ibm/api/batch";
        final String userDir = "/wlp/usr/"; // note: the trailing '/' should get trimmed by BatchLocationService
        final String serverName = "myserver";
        final String jobExecServerId = defaultHostname + "/" + userDir + serverName;

        mockery.checking(new Expectations() {
            {
                oneOf(mockEndpointInfo).getAttribute("Host");
                will(returnValue(host));
                
                oneOf(mockEndpointInfo).getAttribute("Port");
                will(returnValue(port));
                
                oneOf(mockServerInfo).getDefaultHostname();
                will(returnValue(defaultHostname)); 
                
                allowing(mockWSJobExecution).getRestUrl();
                will(returnValue(jobExecRestUrl));

                oneOf(mockWSJobExecution).getServerId();
                will(returnValue(jobExecServerId));
            }
        });
        
        BatchLocationServiceImpl batchLocationService = new BatchLocationServiceImpl();
        batchLocationService.setServerInfoMBean(mockServerInfo);
        batchLocationService.setEndPointInfoMBean(mockEndpointInfo);
        
        assertTrue( batchLocationService.isLocalJobExecution(mockWSJobExecution) );
    }
    
    /**
     * 
     */
    @Test
    public void testIsLocalJobExecutionFalse() throws Exception {
        final DynamicMBean mockEndpointInfo = mockery.mock(DynamicMBean.class);
        final ServerInfoMBean mockServerInfo = mockery.mock(ServerInfoMBean.class);
        final WSJobExecution mockWSJobExecution = mockery.mock(WSJobExecution.class);

        final int port = 1234;
        final String host = "*";
        final String defaultHostname = "def.com";
        final String jobExecRestUrl = "https://abc.com:" + port + "/ibm/api/batch";
        final String userDir = "/wlp/usr/"; // note: the trailing '/' should get trimmed by BatchLocationService
        final String serverName = "myserver";
        final String jobExecServerId = defaultHostname + "/" + userDir + serverName;

        mockery.checking(new Expectations() {
            {
                oneOf(mockEndpointInfo).getAttribute("Host");
                will(returnValue(host));
                
                oneOf(mockEndpointInfo).getAttribute("Port");
                will(returnValue(port));
                
                oneOf(mockServerInfo).getDefaultHostname();
                will(returnValue(defaultHostname)); 
                
                allowing(mockWSJobExecution).getRestUrl();
                will(returnValue(jobExecRestUrl));

                oneOf(mockWSJobExecution).getServerId();
                will(returnValue(jobExecServerId));
            }
        });
        
        BatchLocationServiceImpl batchLocationService = new BatchLocationServiceImpl();
        batchLocationService.setServerInfoMBean(mockServerInfo);
        batchLocationService.setEndPointInfoMBean(mockEndpointInfo);
        
        assertFalse( batchLocationService.isLocalJobExecution(mockWSJobExecution) );
    }
    
    /**
     * 
     */
    @Test
    public void testIsLocalJobExecutionServerId() throws Exception {
        final DynamicMBean mockEndpointInfo = mockery.mock(DynamicMBean.class);
        final ServerInfoMBean mockServerInfo = mockery.mock(ServerInfoMBean.class);
        final WSJobExecution mockWSJobExecution = mockery.mock(WSJobExecution.class);

        final int port = 1234;
        final String host = "*";
        final String defaultHostname = "def.com";
        final String userDir = "/wlp/usr/";     // note: the trailing '/' should get trimmed by BatchLocationService
        final String serverName = "myserver";
        final String jobExecRestUrl = BatchLocationServiceImpl.BatchRestUrlUnavailable;
        final String jobExecServerId = defaultHostname + "/" + userDir + serverName;
        
        mockery.checking( new Expectations() {
            {
                oneOf(mockEndpointInfo).getAttribute("Host");
                will(returnValue(host));
                
                oneOf(mockEndpointInfo).getAttribute("Port");
                will(returnValue(port));
                
                oneOf(mockServerInfo).getDefaultHostname();
                will(returnValue(defaultHostname)); 
                
                allowing(mockWSJobExecution).getRestUrl();
                will(returnValue(jobExecRestUrl));
                
                oneOf(mockServerInfo).getUserDirectory();
                will(returnValue(userDir));
                
                oneOf(mockServerInfo).getDefaultHostname();
                will(returnValue(defaultHostname));
                
                oneOf(mockServerInfo).getName();
                will(returnValue(serverName));

                exactly(2).of(mockWSJobExecution).getServerId();
                will(returnValue(jobExecServerId));
            }
        });
        
        BatchLocationServiceImpl batchLocationService = new BatchLocationServiceImpl();
        batchLocationService.setServerInfoMBean(mockServerInfo);
        batchLocationService.setEndPointInfoMBean(mockEndpointInfo);
        
        assertTrue( batchLocationService.isLocalJobExecution(mockWSJobExecution) );
    }
    
    /**
     * 
     */
    @Test
    public void testIsLocalJobExecutionServerIdFalse() throws Exception {
        final DynamicMBean mockEndpointInfo = mockery.mock(DynamicMBean.class);
        final ServerInfoMBean mockServerInfo = mockery.mock(ServerInfoMBean.class);
        final WSJobExecution mockWSJobExecution = mockery.mock(WSJobExecution.class);

        final int port = 1234;
        final String host = "*";
        final String defaultHostname = "def.com";
        final String userDir = "/wlp/usr/";     // note: the trailing '/' should get trimmed by BatchLocationService
        final String serverName = "myserver";
        final String jobExecRestUrl = BatchLocationServiceImpl.BatchRestUrlUnavailable;
        final String jobExecServerId = "not/the/same";
        
        mockery.checking( new Expectations() {
            {
                oneOf(mockEndpointInfo).getAttribute("Host");
                will(returnValue(host));
                
                oneOf(mockEndpointInfo).getAttribute("Port");
                will(returnValue(port));
                
                oneOf(mockServerInfo).getDefaultHostname();
                will(returnValue(defaultHostname)); 
                
                allowing(mockWSJobExecution).getRestUrl();
                will(returnValue(jobExecRestUrl));
                
                oneOf(mockServerInfo).getUserDirectory();
                will(returnValue(userDir));
                
                oneOf(mockServerInfo).getDefaultHostname();
                will(returnValue(defaultHostname));
                
                oneOf(mockServerInfo).getName();
                will(returnValue(serverName));

                exactly(2).of(mockWSJobExecution).getServerId();
                will(returnValue(jobExecServerId));
            }
        });
        
        BatchLocationServiceImpl batchLocationService = new BatchLocationServiceImpl();
        batchLocationService.setServerInfoMBean(mockServerInfo);
        batchLocationService.setEndPointInfoMBean(mockEndpointInfo);
        
        assertFalse( batchLocationService.isLocalJobExecution(mockWSJobExecution) );
    }
    
    /**
     * 
     */
    @Test
    public void testIsLocalJobExecutionBothUnavailable() throws Exception {
        final DynamicMBean mockEndpointInfo = mockery.mock(DynamicMBean.class);
        final ServerInfoMBean mockServerInfo = mockery.mock(ServerInfoMBean.class);
        final WSJobExecution mockWSJobExecution = mockery.mock(WSJobExecution.class);

        final String defaultHostname = "def.com";
        final String userDir = "/wlp/usr/"; // note: the trailing '/' should get trimmed by BatchLocationService
        final String serverName = "myserver";
        final String jobExecRestUrl = BatchLocationServiceImpl.BatchRestUrlUnavailable;
        final String jobExecServerId = defaultHostname + "/" + userDir + serverName;

        mockery.checking(new Expectations() {
            {
                allowing(mockWSJobExecution).getRestUrl();
                will(returnValue(jobExecRestUrl));

                oneOf(mockWSJobExecution).getServerId();
                will(returnValue(jobExecServerId));
            }
        });
        
        BatchLocationServiceImpl batchLocationService = new BatchLocationServiceImpl();
        assertTrue( batchLocationService.isLocalJobExecution(mockWSJobExecution) );
    }
}
