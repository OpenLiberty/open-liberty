/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.grpc.internal.client.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import io.openliberty.grpc.internal.client.GrpcClientConstants;
import test.common.SharedOutputManager;

/**
 * Basic unit tests for the grpcServlet feature
 */
public class GrpcClientConfigTest {

	private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

	@Rule
	public TestRule rule = outputMgr;

	@Rule
	public TestName name = new TestName();

	@After
	public void tearDown() {
	}

	/**
	 * verify a basic grpcClient configuration
	 */
	@Test
	public void testSimpleClientConfig() {
		GrpcClientConfigImpl config = new GrpcClientConfigImpl();
		Map<String, Object> basicProps = new HashMap<String, Object>();
		
		// this properties map should map to all requests 
		basicProps.put(GrpcClientConstants.HOST_PROP, "*");
		basicProps.put(GrpcClientConstants.KEEP_ALIVE_WITHOUT_CALLS_PROP, true);
		basicProps.put(GrpcClientConstants.KEEP_ALIVE_TIME_PROP, 6);
		basicProps.put(GrpcClientConstants.KEEP_ALIVE_TIMEOUT_PROP, 6);
		basicProps.put(GrpcClientConstants.MAX_INBOUND_MSG_SIZE_PROP, 9001);
		basicProps.put(GrpcClientConstants.SSL_CFG_PROP, "fakeSSLCfg");
		basicProps.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "some.example.Class");
		basicProps.put(GrpcClientConstants.MAX_INBOUND_MSG_SIZE_PROP, 9001);
		basicProps.put(GrpcClientConstants.MAX_INBOUND_METADATA_SIZE_PROP, 9001);
		basicProps.put(GrpcClientConstants.OVERRIDE_AUTHORITY_PROP, "fakeDomain");
		basicProps.put(GrpcClientConstants.USER_AGENT_PROP, "fake_client");


		config.activate(basicProps);

		List<String> urisToTest = new ArrayList<String>();
		urisToTest.add("localhost");
		urisToTest.add("cncf.io");
		urisToTest.add("grpc.io");
		
		for (String uri : urisToTest) {
			Assert.assertTrue(Boolean.parseBoolean(GrpcClientConfigHolder.getKeepAliveWithoutCalls(uri)));
			Assert.assertEquals(6, Integer.parseInt(GrpcClientConfigHolder.getKeepAliveTime(uri)));
			Assert.assertEquals(9001, Integer.parseInt(GrpcClientConfigHolder.getMaxInboundMessageSize(uri)));
			Assert.assertEquals(9001, Integer.parseInt(GrpcClientConfigHolder.getMaxInboundMetadataSize(uri)));
			Assert.assertEquals(6, Integer.parseInt(GrpcClientConfigHolder.getKeepAliveTimeout(uri)));
			Assert.assertEquals("fakeSSLCfg", GrpcClientConfigHolder.getSSLConfig(uri));
			Assert.assertEquals("some.example.Class", GrpcClientConfigHolder.getClientInterceptors(uri));
			Assert.assertEquals("fakeDomain", GrpcClientConfigHolder.getOverrideAuthority(uri));
			Assert.assertEquals("fake_client", GrpcClientConfigHolder.getUserAgent(uri));

		}
		config.deactivate();
	}
	
	/**
	 * verify updating a single grpcClient config
	 */
	@Test
	public void testConfigUpdate() {
		GrpcClientConfigImpl config = new GrpcClientConfigImpl();
		Map<String, Object> basicProps1 = new HashMap<String, Object>();
		Map<String, Object> basicProps2 = new HashMap<String, Object>();
		Map<String, Object> basicProps3 = new HashMap<String, Object>();
		String uri1 = "localhost";
		String uri2 = "openliberty.io";
		String uri3 = "test.openliberty.io";

		basicProps2.put(GrpcClientConstants.HOST_PROP, uri1);
		basicProps2.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "host.match");
		config.activate(basicProps2);

		Assert.assertEquals("host.match", GrpcClientConfigHolder.getClientInterceptors(uri1));

		basicProps1.put(GrpcClientConstants.HOST_PROP, "*openliberty.io");
		basicProps1.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "partial.match");
		config.modified(basicProps1);

		Assert.assertEquals("partial.match", GrpcClientConfigHolder.getClientInterceptors(uri2));
		Assert.assertEquals("partial.match", GrpcClientConfigHolder.getClientInterceptors(uri3));

		basicProps3.put(GrpcClientConstants.HOST_PROP, "*");
		basicProps3.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "wildcard");
		config.modified(basicProps3);
		
		Assert.assertEquals("wildcard", GrpcClientConfigHolder.getClientInterceptors(uri3));

		config.deactivate();

		Assert.assertEquals(null, GrpcClientConfigHolder.getClientInterceptors(uri1));
	}
	
	/**
	 * verify precedence with multiple grpcClient elements
	 */
	@Test
	public void testMultipleElements() {
		GrpcClientConfigImpl config1 = new GrpcClientConfigImpl();
		GrpcClientConfigImpl config2 = new GrpcClientConfigImpl();
		GrpcClientConfigImpl config3 = new GrpcClientConfigImpl();
		Map<String, Object> basicProps1 = new HashMap<String, Object>();
		Map<String, Object> basicProps3 = new HashMap<String, Object>();
		String uri1 = "localhost";
		String uri2 = "openliberty.io";
		String uri3 = "test.openliberty.io";

		basicProps1.put(GrpcClientConstants.HOST_PROP, uri1);
		basicProps1.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "host.match");
		config1.activate(basicProps1);

		Assert.assertEquals("host.match", GrpcClientConfigHolder.getClientInterceptors(uri1));

		basicProps3.put(GrpcClientConstants.HOST_PROP, "*openliberty.io");
		basicProps3.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "partial.match");
		config2.activate(basicProps3);

		basicProps3.put(GrpcClientConstants.HOST_PROP, "*");
		basicProps3.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "wildcard");
		config3.activate(basicProps3);

		Assert.assertEquals("host.match", GrpcClientConfigHolder.getClientInterceptors(uri1));
		Assert.assertEquals("partial.match", GrpcClientConfigHolder.getClientInterceptors(uri3));
		Assert.assertEquals("partial.match", GrpcClientConfigHolder.getClientInterceptors(uri2));

		config1.deactivate();
		config2.deactivate();
		config3.deactivate();
	}

	/**
	 * verify precedence with distinct registered paths
	 */
	@Test
	public void testSameHostMultiplePaths() {
		GrpcClientConfigImpl config1 = new GrpcClientConfigImpl();
		GrpcClientConfigImpl config2 = new GrpcClientConfigImpl();
		Map<String, Object> basicProps1 = new HashMap<String, Object>();
		Map<String, Object> basicProps2 = new HashMap<String, Object>();
		String host = "localhost";
		String path1 = "service/Method1";
		String path2 = "service/Method2";
		String header1 = "header1";
		String header2 = "header2";
		String customAuthority = "myserver";
		String sslRef = "mySSLConfig";

		basicProps1.put(GrpcClientConstants.HOST_PROP, host);
		basicProps1.put(GrpcClientConstants.PATH_PROP, path1);
		basicProps1.put(GrpcClientConstants.KEEP_ALIVE_TIMEOUT_PROP, 1);
		basicProps1.put(GrpcClientConstants.HEADER_PROPAGATION_PROP, header1);
		basicProps1.put(GrpcClientConstants.OVERRIDE_AUTHORITY_PROP, customAuthority);
		config1.activate(basicProps1);

		basicProps2.put(GrpcClientConstants.HOST_PROP, host);
		basicProps2.put(GrpcClientConstants.PATH_PROP, path2);
		basicProps2.put(GrpcClientConstants.HEADER_PROPAGATION_PROP, header2);
		basicProps2.put(GrpcClientConstants.SSL_CFG_PROP, sslRef);
		config2.activate(basicProps2);

		Assert.assertEquals(header1, GrpcClientConfigHolder.getHeaderPropagationSupport(host, path1));
		Assert.assertEquals(header2, GrpcClientConfigHolder.getHeaderPropagationSupport(host, path2));
		Assert.assertEquals(1, Integer.parseInt(GrpcClientConfigHolder.getKeepAliveTimeout(host)));
		Assert.assertEquals(sslRef, GrpcClientConfigHolder.getSSLConfig(host));
		Assert.assertEquals(customAuthority, GrpcClientConfigHolder.getOverrideAuthority(host));

		config1.deactivate();
		config2.deactivate();
	}
	
	/**
	 * verify precedence with distinct 
	 */
	@Test
	public void testWildcardPath() {
		GrpcClientConfigImpl config1 = new GrpcClientConfigImpl();
		GrpcClientConfigImpl config2 = new GrpcClientConfigImpl();
		Map<String, Object> basicProps1 = new HashMap<String, Object>();
		Map<String, Object> basicProps2 = new HashMap<String, Object>();
		String host = "localhost";
		String path1 = "service/Method1";
		String path2 = "service/Method2";
		String header1 = "header1";
		String header2 = "header2";
		String customAuthority = "myserver";
		String sslRef = "mySSLConfig";

		basicProps1.put(GrpcClientConstants.HOST_PROP, host);
		basicProps1.put(GrpcClientConstants.PATH_PROP, "service/*");
		basicProps1.put(GrpcClientConstants.KEEP_ALIVE_TIMEOUT_PROP, 1);
		basicProps1.put(GrpcClientConstants.HEADER_PROPAGATION_PROP, header1);
		basicProps1.put(GrpcClientConstants.OVERRIDE_AUTHORITY_PROP, customAuthority);
		config1.activate(basicProps1);

		basicProps2.put(GrpcClientConstants.HOST_PROP, host);
		basicProps2.put(GrpcClientConstants.PATH_PROP, path2);
		basicProps2.put(GrpcClientConstants.HEADER_PROPAGATION_PROP, header2);
		basicProps2.put(GrpcClientConstants.SSL_CFG_PROP, sslRef);
		config2.activate(basicProps2);

		Assert.assertEquals(header1, GrpcClientConfigHolder.getHeaderPropagationSupport(host, path1));
		// most specific config should match
		Assert.assertEquals(header2, GrpcClientConfigHolder.getHeaderPropagationSupport(host, path2));
		Assert.assertEquals(1, Integer.parseInt(GrpcClientConfigHolder.getKeepAliveTimeout(host)));
		Assert.assertEquals(sslRef, GrpcClientConfigHolder.getSSLConfig(host));
		Assert.assertEquals(customAuthority, GrpcClientConfigHolder.getOverrideAuthority(host));

		config1.deactivate();
		config2.deactivate();
	}
}
