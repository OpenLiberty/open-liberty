/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.auth.WSLoginFailedException;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.jwtsso.config.JwtSsoBuilderConfig;
import com.ibm.ws.security.jwtsso.internal.JwtSsoBuilderComponent;
import com.ibm.ws.security.jwtsso.utils.JwtSsoConstants;
import com.ibm.ws.security.jwtsso.utils.JwtSsoTokenUtils;
import com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

import test.common.SharedOutputManager;

public class JwtSSOTokenImplTest {
	protected final Mockery context = new JUnit4Mockery() {
		{
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private final ComponentContext cc = context.mock(ComponentContext.class);

	protected final AtomicServiceReference<TrustAssociationInterceptor> mpJwtTaiServiceRef = context
			.mock(AtomicServiceReference.class);

	private final JwtToken jwt = context.mock(JwtToken.class, "jwt");

	private static SharedOutputManager outputMgr = SharedOutputManager.getInstance()
			.trace("com.ibm.ws.security.jwtsso.*=all");

	private static String tokenstr = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic3ViIjoidXNlcjEiLCJ1cG4iOiJ1c2VyMSIsImp0aSI6IklwVGZPVFJnQmtET0RHSzQiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjkwODAvand0c3NvL2RlZmF1bHRKd3RTc28iLCJleHAiOjE1MjIyNTExODIsImlhdCI6MTUyMjI1MTE4Mn0.fe-FyUAkoByE3fQvSHdXzZiHy5PwGd3E2Ne6ocGZPTeE0okNkCAP3FiWuk2jspD5Q4niACW0h94x8hTy1KiaDsy5N9ibgb_D4iolhMvuZ_vH3K868qf8r4ZD82ljbb2DZYsm-kuem-eS2jsy4uSMBf95wP3YGmPrL5yHkFT89LfW0cZ7HgQurnOZSwRdW0te7wSS0wTTVhoVCfWjKs_Ji-4Wf1bwgntI6CmUAy0TqvMf3IqSfrtS1AwXo7MxoABlZXZ1RGTgsBrTvC3jzH3i529M7t2EndWkSznDKCOoDfveR1LwwxQlJFZT1rDwPg0enx9m_P83SuE1j7GQFen6zA";

	private final JwtSSOTokenImpl jwtssotokenproxy = new JwtSSOTokenImpl();
	private final JwtSsoBuilderComponent jwtssobuilderconfig = new JwtSsoBuilderComponent();

	@Rule
	public final TestName testName = new TestName();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		outputMgr.captureStreams();
	}

	@Before
	public void setUp() {
		System.out.println("Entering test: " + testName.getMethodName());
		// builderref.
		// jwtssotokenproxy.setJwtSsoBuilderConfig(builderref);
		// jwtssotokenproxy.setJwtSsoConfig(consumerref);
		final Map<String, Object> props = createProps(true);
		jwtssobuilderconfig.activate(props, cc);
	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
		System.out.println("Exiting test: " + testName.getMethodName());
		outputMgr.resetStreams();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		outputMgr.dumpStreams();
		outputMgr.restoreStreams();
	}

	private class JwtSSOTokenImplTestAuth extends JwtSSOTokenImpl {
		public JwtSSOTokenImplTestAuth() {
			super();
		}

		@Override
		protected JwtSsoBuilderConfig getJwtSSOBuilderConfig() {

			return jwtssobuilderconfig;
		}

		@Override
		protected JwtSsoTokenUtils getJwtSsoTokenBuilderUtils() {
			System.out.println("DEBUG: getting the configuration");
			return new JwtSsoTokenUtilsTest("defaultJwtSso");
		}

		@Override
		protected JwtSsoTokenUtils getJwtSsoTokenConsumerUtils() {
			System.out.println("DEBUG: getting the configuration");
			return new JwtSsoTokenUtilsTest("defaultJwtSso", mpJwtTaiServiceRef);
		}

	}

	private class JwtSsoTokenUtilsTest extends JwtSsoTokenUtils {

		public JwtSsoTokenUtilsTest(String builder) {
			super(builder);
		}

		public JwtSsoTokenUtilsTest(String consumer,
				AtomicServiceReference<TrustAssociationInterceptor> mpJwtTaiServiceRef) {
			super(consumer, mpJwtTaiServiceRef);
		}

		@Override
		public JsonWebToken buildTokenFromSecuritySubject(Subject subject) throws Exception {
			// SubjectHelper subjectHelper = new SubjectHelper();
			// String user =
			// subjectHelper.getWSCredential(subject).getSecurityName();
			return new DefaultJsonWebTokenImpl(tokenstr, JwtSsoConstants.TOKEN_TYPE_JWT, "user1");

		}

		@Override
		protected JwtToken recreateJwt(String token) throws Exception {
			return jwt;
		}
	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#createJwtSSOToken(javax.security.auth.Subject)}.
	 */
	@Test
	public void testCreateJwtSSOTokenUnAuthenticatedSubject() {
		Subject subject = new Subject();
		setWSPrincipal(subject, "UNAUTHENTICATED", "user:BasicRealm/UNAUTHENTICATE");
		try {
			jwtssotokenproxy.createJwtSSOToken(subject);
			assertTrue("JWT principal should not exist.", subject.getPrincipals(JsonWebToken.class).isEmpty());
		} catch (WSLoginFailedException e) {

			outputMgr.failWithThrowable(testName.getMethodName(), e);
		}

	}

	@Test
	public void testCreateJwtSSOTokenInvalidJwtSsoConfig() {
		Subject subject = new Subject();
		setWSPrincipal(subject, "user1", "user:BasicRealm/user1");
		try {
			jwtssotokenproxy.createJwtSSOToken(subject);
			fail("Should have thrown exception creating JwtSso token but did not..");
		} catch (WSLoginFailedException e) {
		}
	}

	@Test
	public void testCreateJwtSSOTokenAuthenticatedSubject() {
		Subject subject = new Subject();
		setWSPrincipal(subject, "user1", "user:BasicRealm/user1");
		try {
			JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
			jwtssotokenproxyauth.activate(cc);
			jwtssotokenproxyauth.createJwtSSOToken(subject);
			assertTrue("JWT principal should exist.", !(subject.getPrincipals(JsonWebToken.class).isEmpty()));
		} catch (WSLoginFailedException e) {
			outputMgr.failWithThrowable(testName.getMethodName(), e);
		}
	}

	@Test
	public void testCreateJwtSSOTokenAuthenticatedSubjectAndJwtPrincipal() {
		Subject subject = new Subject();
		setWSPrincipal(subject, "user1", "user:BasicRealm/user1");
		try {
			JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
			jwtssotokenproxyauth.activate(cc);
			jwtssotokenproxyauth.createJwtSSOToken(subject);
			jwtssotokenproxyauth.createJwtSSOToken(subject);
			assertEquals("One JWT principal should exist.", 1, subject.getPrincipals(JsonWebToken.class).size());

		} catch (WSLoginFailedException e) {
			outputMgr.failWithThrowable(testName.getMethodName(), e);

		}

	}

	@Test
	public void testCreateJwtSSOTokenNullSubject() {
		try {
			jwtssotokenproxy.createJwtSSOToken(null);
			// should not encounter any exception
		} catch (WSLoginFailedException e) {
			outputMgr.failWithThrowable(testName.getMethodName(), e);
		}
	}

	private void setWSPrincipal(Subject subject, String securityName, String accessId) {

		// String accessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER,
		// realm,
		// uniqueName);
		// String accessId = "user:BasicRealm/UNAUTHENTICATED";
		WSPrincipal wsPrincipal = new WSPrincipal(securityName, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
		subject.getPrincipals().add(wsPrincipal);
	}

	private Set<JsonWebToken> getJwtPrincipals(Subject subject) {
		// TODO Auto-generated method stub
		return subject != null ? subject.getPrincipals(JsonWebToken.class) : null;

	}

	public Map<String, Object> createProps(boolean value) {
		final Map<String, Object> props = new Hashtable<String, Object>();

		props.put("id", "jwtSsoIdentifier");
		props.put(JwtSsoConstants.CFG_KEY_COOKIENAME, "jwtSsoCookieName");
		props.put(JwtSsoConstants.CFG_USE_LTPA_IF_JWT_ABSENT, value);
		props.put(JwtSsoConstants.CFG_KEY_INCLUDELTPACOOKIE, value);
		props.put(JwtSsoConstants.CFG_KEY_DISABLE_JWT_COOKIE, value);
		props.put(JwtSsoConstants.CFG_KEY_SETCOOKIEPATHTOWEBAPPCONTEXTPATH, value);
		props.put(JwtSsoConstants.CFG_KEY_COOKIESECUREFLAG, value);
		props.put(JwtSsoConstants.CFG_KEY_JWTBUILDERREF, "");
		props.put(JwtSsoConstants.CFG_KEY_JWTCONSUMERREF, "");
		return props;
	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#getJwtSSOToken(javax.security.auth.Subject)}.
	 */
	@Test
	public void testGetJwtSSOToken() {
		Subject subject = new Subject();
		setWSPrincipal(subject, "user1", "user:BasicRealm/user1");
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);
		try {
			jwtssotokenproxyauth.createJwtSSOToken(subject);
			String jwtsso = jwtssotokenproxyauth.getJwtSSOToken(subject);
			assertEquals("Jwt SSo token is not what we expected ..", tokenstr, jwtsso);

		} catch (WSLoginFailedException e) {
			outputMgr.failWithThrowable(testName.getMethodName(), e);
		}
	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#handleJwtSSOTokenValidation(javax.security.auth.Subject, java.lang.String)}.
	 */
	@Test
	public void testHandleJwtSSOTokenValidation() {
		Subject subject = new Subject();
		setWSPrincipal(subject, "user1", "user:BasicRealm/user1");
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);
		try {
			jwtssotokenproxyauth.createJwtSSOToken(subject);
			String jwtsso = jwtssotokenproxyauth.getJwtSSOToken(subject);
			Subject ret = jwtssotokenproxyauth.handleJwtSSOTokenValidation(subject, jwtsso);
			jwtsso = jwtssotokenproxyauth.getJwtSSOToken(ret);

			assertEquals("Jwt SSo token is not what we expected ..", tokenstr, jwtsso);

		} catch (WSLoginFailedException e) {
			outputMgr.failWithThrowable(testName.getMethodName(), e);
		}
	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#getCustomCacheKeyFromJwtSSOToken(java.lang.String)}.
	 */
	@Test
	public void testGetCustomCacheKeyFromJwtSSOToken() {
		// {"token_type":"Bearer","sub":"user1","upn":"user1","realm":"BasicRealm","sid":"thisisjwtssocustomcachekey","iss":"https://localhost:9443/jwtsso/realdeal","exp":1527186443,"iat":1527186441}
		String tokenStrWithCCK = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic3ViIjoidXNlcjEiLCJ1cG4iOiJ1c2VyMSIsInJlYWxtIjoiQmFzaWNSZWFsbSIsInNpZCI6InRoaXNpc2p3dHNzb2N1c3RvbWNhY2hla2V5IiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTQ0My9qd3Rzc28vcmVhbGRlYWwiLCJleHAiOjE1MjcxODY0NDMsImlhdCI6MTUyNzE4NjQ0MX0.LvBSxhn3SaR76isAOdIp5VC8K3qq0FjyLafxntAMv96CZOBpnxailmwzkfRqFE6Tb5STbmci4isOiNZ8VeJw7Obo0NIWV4jFT5GSlzM4sEZCHq7O40-V8DJxQso_ykU4r7F5BatPoZ0CBKIKnWkyuoTw2NwvKYvvtq8eWlFRV5mxyPf9pZ8ndT9UG96l-C63s6zXC3bB-EMI2Qy51ibJWXzmGb0VWM6jneZN-Te2BwDmplf8qXIxEJV0UWnNvvXvrtJpmWUYq7TpS-Gx3okLg9aR7wFHC7eqXH82RdusZt9Atqp-Xq-qVj7A1tAEddtYjegYxyV9kWWQGEFEwfe7mw";
		String expected = "thisisjwtssocustomcachekey";
		// SubjectHelper subjectHelper = new SubjectHelper();
		// String[] hashtableProperties = {
		// AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };

		String cck = jwtssotokenproxy.getCustomCacheKeyFromJwtSSOToken(tokenStrWithCCK);
		assertEquals("Jwt SSo token custom cache key is not what we expected ..", expected, cck);
	}

	@Test
	public void testGetCustomCacheKeyFromJwtSSOTokenMissingCCK() {
		String cck = jwtssotokenproxy.getCustomCacheKeyFromJwtSSOToken(tokenstr);
		assertNull("msg", cck);
	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#getCacheKeyForJwtSSOToken(javax.security.auth.Subject, java.lang.String)}.
	 */
	@Test
	public void testGetCacheKeyForJwtSSOToken() {
		String tokenStrWithCCK = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic3ViIjoidXNlcjEiLCJ1cG4iOiJ1c2VyMSIsInJlYWxtIjoiQmFzaWNSZWFsbSIsInNpZCI6InRoaXNpc2p3dHNzb2N1c3RvbWNhY2hla2V5IiwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTQ0My9qd3Rzc28vcmVhbGRlYWwiLCJleHAiOjE1MjcxODY0NDMsImlhdCI6MTUyNzE4NjQ0MX0.LvBSxhn3SaR76isAOdIp5VC8K3qq0FjyLafxntAMv96CZOBpnxailmwzkfRqFE6Tb5STbmci4isOiNZ8VeJw7Obo0NIWV4jFT5GSlzM4sEZCHq7O40-V8DJxQso_ykU4r7F5BatPoZ0CBKIKnWkyuoTw2NwvKYvvtq8eWlFRV5mxyPf9pZ8ndT9UG96l-C63s6zXC3bB-EMI2Qy51ibJWXzmGb0VWM6jneZN-Te2BwDmplf8qXIxEJV0UWnNvvXvrtJpmWUYq7TpS-Gx3okLg9aR7wFHC7eqXH82RdusZt9Atqp-Xq-qVj7A1tAEddtYjegYxyV9kWWQGEFEwfe7mw";
		String expected = "9AgE+zbuTMtbPeQKN1skySQS+glylqrFBP411eWh//w=";

		String ck = jwtssotokenproxy.getCacheKeyForJwtSSOToken(null, tokenStrWithCCK);
		assertEquals("Jwt SSo token custom cache key is not what we expected ..", expected, ck);
	}

	@Test
	public void testGetCacheKeyNullTokenValidSubject() {
		String tokenStrWithCCK = null;
		String expected = "Nfjs0ZrVoxNHwzuI2vNblyCfj76BjBohCqVxpGouCTY=";

		Subject subject = new Subject();
		setWSPrincipal(subject, "user1", "user:BasicRealm/user1");
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);
		try {
			jwtssotokenproxyauth.createJwtSSOToken(subject);
		} catch (WSLoginFailedException e) {
			outputMgr.failWithThrowable(testName.getMethodName(), e);
		}

		String ck = jwtssotokenproxy.getCacheKeyForJwtSSOToken(subject, tokenStrWithCCK);
		assertEquals("Jwt SSo token custom cache key is not what we expected ..", expected, ck);
	}

	@Test
	public void testGetCacheKeyNullTokenNullSubject() {

		String ck = jwtssotokenproxy.getCacheKeyForJwtSSOToken(null, null);
		assertEquals("jwt sso token custom cachek key should be null", null, ck);
	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#addCustomCacheKeyToJwtSSOToken(javax.security.auth.Subject, java.lang.String)}.
	 */
	@Test
	public void testAddCustomCacheKeyAndRealmToJwtSSOToken() {
		Subject subject = new Subject();
		setWSPrincipal(subject, "user1", "user:BasicRealm/user1");
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);
		try {
			jwtssotokenproxyauth.createJwtSSOToken(subject);
			jwtssotokenproxyauth.addAttributesToJwtSSOToken(subject);
			assertEquals("One JWT principal should exist.", 1, subject.getPrincipals(JsonWebToken.class).size());

		} catch (WSLoginFailedException e) {
			outputMgr.failWithThrowable(testName.getMethodName(), e);
		}

	}

	@Test
	public void testAddCustomCacheKeyAndRealmToJwtSSOTokenNullSubject() {
		Subject subject = new Subject();
		setWSPrincipal(subject, "user1", "user:BasicRealm/user1");
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);
		try {
			jwtssotokenproxyauth.createJwtSSOToken(subject);
			jwtssotokenproxyauth.addAttributesToJwtSSOToken(null);
			assertEquals("One JWT principal should exist.", 1, subject.getPrincipals(JsonWebToken.class).size());

		} catch (WSLoginFailedException e) {
			outputMgr.failWithThrowable(testName.getMethodName(), e);
		}

	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#isJwtSSOTokenValid(javax.security.auth.Subject)}.
	 */
	@Test
	public void testIsSubjectValid() {
		Subject subject = new Subject();
		setWSPrincipal(subject, "user1", "user:BasicRealm/user1");
		try {
			JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
			jwtssotokenproxyauth.activate(cc);
			jwtssotokenproxyauth.createJwtSSOToken(subject);
			boolean valid = jwtssotokenproxyauth.isSubjectValid(subject);
			assertTrue("The subject should be valid", valid);
		} catch (WSLoginFailedException e) {

			outputMgr.failWithThrowable(testName.getMethodName(), e);
		}
	}

	@Test
	public void testIsSubjectValidMissingJwtPrincipal() {
		Subject subject = new Subject();
		setWSPrincipal(subject, "user1", "user:BasicRealm/user1");
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);

		boolean valid = jwtssotokenproxyauth.isSubjectValid(subject);
		assertFalse("The subject should not be valid", valid);
	}

	@Test
	public void testIsSubjectValidNullSubject() {
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);

		boolean valid = jwtssotokenproxyauth.isSubjectValid(null);
		assertFalse("The subject should not be valid", valid);
	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#getJwtCookieName()}.
	 */
	@Test
	public void testGetJwtCookieName() {
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);
		String cookieName = jwtssotokenproxyauth.getJwtCookieName();

		assertEquals("Cookie name is not what expected..", "jwtSsoCookieName", cookieName);
	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#shouldSetJwtCookiePathToWebAppContext()}.
	 */
	@Test
	public void testShouldSetJwtCookiePathToWebAppContext() {
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);

		boolean cookiePath = jwtssotokenproxyauth.shouldSetJwtCookiePathToWebAppContext();
		assertTrue("cookie path to WebApp context should be true", cookiePath);
	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#shouldAlsoIncludeLtpaCookie()}.
	 */
	@Test
	public void testShouldAlsoIncludeLtpaCookie() {
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);
		boolean includeLtpa = jwtssotokenproxy.shouldAlsoIncludeLtpaCookie();

		assertTrue("should also include Ltpa cookie", includeLtpa);
	}

	/**
	 * Test method for
	 * {@link com.ibm.ws.security.jwtsso.token.JwtSSOTokenImpl#shouldFallbackToLtpaCookie()}.
	 */
	@Test
	public void testShouldUseToLtpaIfJwtAbsent() {
		JwtSSOTokenImplTestAuth jwtssotokenproxyauth = new JwtSSOTokenImplTestAuth();
		jwtssotokenproxyauth.activate(cc);

		boolean shouldUseLtpa = jwtssotokenproxy.shouldUseLtpaIfJwtAbsent();
		assertTrue("should use Ltpa cookie if Jwt is absent", shouldUseLtpa);
	}

}