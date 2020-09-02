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
	 * verify a basic grpcTarget configuration
	 */
	@Test
	public void testSimpleClientConfig() {
		GrpcClientConfigImpl config = new GrpcClientConfigImpl();
		Map<String, Object> basicProps = new HashMap<String, Object>();
		
		// this properties map should map to all requests 
		basicProps.put(GrpcClientConstants.TARGET_PROP, "*");
		basicProps.put(GrpcClientConstants.KEEP_ALIVE_WITHOUT_CALLS_PROP, true);
		basicProps.put(GrpcClientConstants.KEEP_ALIVE_TIME_PROP, 6);
		basicProps.put(GrpcClientConstants.AUTH_TOKEN_PROP, GrpcClientConstants.JWT);
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
		urisToTest.add("/some.Service/Method");
		urisToTest.add("/some.Service/Method2");
		urisToTest.add("/some.otherService/OK");
		
		for (String uri : urisToTest) {
			Assert.assertEquals(GrpcClientConstants.JWT, GrpcClientConfigHolder.getAuthnSupport(uri));
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
	 * verify updating a single grpcTarget config
	 */
	@Test
	public void testConfigUpdate() {
		GrpcClientConfigImpl config = new GrpcClientConfigImpl();
		Map<String, Object> basicProps1 = new HashMap<String, Object>();
		Map<String, Object> basicProps2 = new HashMap<String, Object>();
		Map<String, Object> basicProps3 = new HashMap<String, Object>();
		String uri1 = "/some.Service/Method";
		String uri2 = "/some.Service/Method2";
		String uri3 = "/some.OtherService/Method";

		basicProps2.put(GrpcClientConstants.TARGET_PROP, uri1);
		basicProps2.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "method.match");
		config.activate(basicProps2);

		Assert.assertEquals("method.match", GrpcClientConfigHolder.getClientInterceptors(uri1));

		basicProps1.put(GrpcClientConstants.TARGET_PROP, "/some.Service/*");
		basicProps1.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "service.match");
		config.modified(basicProps1);

		Assert.assertEquals("service.match", GrpcClientConfigHolder.getClientInterceptors(uri1));
		Assert.assertEquals("service.match", GrpcClientConfigHolder.getClientInterceptors(uri2));

		basicProps3.put(GrpcClientConstants.TARGET_PROP, "*");
		basicProps3.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "wildcard");
		config.modified(basicProps3);
		
		Assert.assertEquals("wildcard", GrpcClientConfigHolder.getClientInterceptors(uri3));

		config.deactivate();

		Assert.assertEquals(null, GrpcClientConfigHolder.getClientInterceptors(uri1));
	}
	
	/**
	 * verify precedence with multiple grpcTarget elements
	 */
	@Test
	public void testMultipleElements() {
		GrpcClientConfigImpl config1 = new GrpcClientConfigImpl();
		GrpcClientConfigImpl config2 = new GrpcClientConfigImpl();
		GrpcClientConfigImpl config3 = new GrpcClientConfigImpl();
		Map<String, Object> basicProps1 = new HashMap<String, Object>();
		Map<String, Object> basicProps3 = new HashMap<String, Object>();
		String uri1 = "/some.Service/Method";
		String uri2 = "/some.Service/Method2";
		String uri3 = "/some.OtherService/Method";

		basicProps1.put(GrpcClientConstants.TARGET_PROP, uri1);
		basicProps1.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "method.match");
		config1.activate(basicProps1);

		Assert.assertEquals("method.match", GrpcClientConfigHolder.getClientInterceptors(uri1));

		basicProps3.put(GrpcClientConstants.TARGET_PROP, "/some.Service/*");
		basicProps3.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "service.match");
		config2.activate(basicProps3);

		basicProps3.put(GrpcClientConstants.TARGET_PROP, "*");
		basicProps3.put(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP, "wildcard");
		config3.activate(basicProps3);

		Assert.assertEquals("service.match", GrpcClientConfigHolder.getClientInterceptors(uri2));
		Assert.assertEquals("wildcard", GrpcClientConfigHolder.getClientInterceptors(uri3));

		config1.deactivate();
		config2.deactivate();
		config3.deactivate();
	}
}
