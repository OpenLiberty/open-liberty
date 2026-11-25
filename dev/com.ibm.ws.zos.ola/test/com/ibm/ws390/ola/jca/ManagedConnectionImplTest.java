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
package com.ibm.ws390.ola.jca;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.resource.spi.SecurityException;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ola.ConnectionSpecImpl;
import com.ibm.websphere.security.auth.WSSubjectHelper;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.intfc.SubjectManagerService;
import com.ibm.ws390.ola.ManagedConnectionFactoryImpl;
import com.ibm.ws390.ola.jca.ManagedConnectionImpl;
import com.ibm.ws390.ola.unittest.util.ConnectionRequestInfoImplHelper;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * 
 */
public class ManagedConnectionImplTest {

	test.common.SharedOutputManager outputMgr = test.common.SharedOutputManager.getInstance().trace("*=info");

	@org.junit.Rule
	public org.junit.rules.TestRule outputRule = outputMgr;

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
                setImposteriser(ClassImposteriser.INSTANCE); // for mocking classes
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

	private ManagedConnectionFactoryImpl createManagedConnectionFactoryImpl(
			boolean debugMode, String registerNameFromMCF, int connectionID,
			ConnectionRequestInfoImpl crii, /* @F003705C */
			String OTMAGroupIDFromMCF, String OTMAServerNameFromMCF, String OTMAClientNameFromMCF,
			String OTMASyncLevelFromMCF, int OTMAMaxSegmentsFromMCF, int OTMAMaxRecvSizeFromMCF,
			RemoteProxyInformation rpi, String usernameFromMCF, String passwordFromMCF,
			int OTMARequestLLZZFromMCF, int OTMAResponseLLZZFromMCF, /* @F013381A */
			String linkTaskTranIDFromMCF, int useCICSContainerFromMCF, /* @F013381A */
			String linkTaskReqContIDFromMCF, int linkTaskReqContTypeFromMCF, /* @F013381A */
			String linkTaskRspContIDFromMCF, int linkTaskRspContTypeFromMCF, /* @F013381A */
			String linkTaskChanIDFromMCF, int linkTaskChanTypeFromMCF, /* @F014448A */
int ConnectionWaitTimeoutFromMCF, boolean RRSTransactionalFromMCF) throws SecurityException {
		
		ManagedConnectionFactoryImpl mcf = new ManagedConnectionFactoryImpl();
		mcf.setDebugMode(debugMode);
		mcf.setRegisterName(registerNameFromMCF);
		mcf.setOTMAGroupID(OTMAGroupIDFromMCF);
		mcf.setOTMAServerName(OTMAServerNameFromMCF);
		mcf.setOTMAClientName(OTMAClientNameFromMCF);
		mcf.setOTMASyncLevel(OTMASyncLevelFromMCF);
		mcf.setOTMAMaxSegments(OTMAMaxSegmentsFromMCF);
		mcf.setOTMAMaxRecvSize(OTMAMaxRecvSizeFromMCF);
		mcf.setUsername(usernameFromMCF);
		mcf.setPassword(passwordFromMCF);
		mcf.setOTMARequestLLZZ(OTMARequestLLZZFromMCF);
		mcf.setOTMAResponseLLZZ(OTMAResponseLLZZFromMCF);
		mcf.setLinkTaskTranID(linkTaskTranIDFromMCF);
		mcf.setUseCICSContainer(useCICSContainerFromMCF);
		mcf.setLinkTaskReqContID(linkTaskReqContIDFromMCF);
		mcf.setLinkTaskReqContType(linkTaskReqContTypeFromMCF);
		mcf.setLinkTaskRspContID(linkTaskRspContIDFromMCF);
		mcf.setLinkTaskRspContType(linkTaskRspContTypeFromMCF);
		mcf.setLinkTaskChanID(linkTaskChanIDFromMCF);
		mcf.setLinkTaskChanType(linkTaskChanTypeFromMCF);
		mcf.setConnectionWaitTimeout(ConnectionWaitTimeoutFromMCF);
		mcf.setRRSTransactional(RRSTransactionalFromMCF);
		
		return mcf;
	}
	
	private ManagedConnectionFactoryImpl createManagedConnectionFactoryImpl(String username, String password) throws SecurityException {
		return createManagedConnectionFactoryImpl(false, "registerName", 0, null, null, null, null, null, 0, 0, null, username,
				password, 0, 0, null, 0, null, 0, null, 0, null, 0, 0, false);
	}
	
	/**
	 * @return a ManagedConnectionImpl object, with most parms defaulted.
	 */
	private ManagedConnectionImpl buildManagedConnectionImpl() throws SecurityException {
		return buildManagedConnectionImpl("theuser", "passw0rd");
	}

	/**
	 * Build a managed connection impl with a username and password.  Note that
	 * the password is meaningless.
	 */
	private ManagedConnectionImpl buildManagedConnectionImpl(String username, String password) throws SecurityException {
		ManagedConnectionFactoryImpl mcf = createManagedConnectionFactoryImpl(username, password);
		
		return new ManagedConnectionImpl(false, mcf, 0, null, null);
	}
	
	
	/**
	 * 
	 */
	@Test
	public void testCreateAuthenticationData() {

		AuthenticationData authData = new ManagedConnectionImpl(0).createAuthenticationData("user1", "pass1");

		assertEquals("user1", authData.get(AuthenticationData.USERNAME));
		assertArrayEquals("pass1".toCharArray(), (char[]) authData.get(AuthenticationData.PASSWORD));
	}

	/**
	 * 
	 */
	@Test
	public void testGetSAFCredential() {

		Subject subject = new Subject();
		SAFCredential testSafCred = new TestSAFCredentialImpl("theuserid");
		subject.getPrivateCredentials().add(testSafCred);

		SAFCredential safCred = new ManagedConnectionImpl(0).getSAFCredential(subject);
		assertSame(testSafCred, safCred);
		assertEquals("theuserid", safCred.getUserId());
	}

	/**
	 * 
	 */
	@Test
	public void testGetSAFCredentialMultipleCreds() {

		Subject subject = new Subject();
		SAFCredential testSafCred = new TestSAFCredentialImpl("theuserid");
		subject.getPrivateCredentials().add(testSafCred);
		subject.getPrivateCredentials().add("some other cred");
		subject.getPrivateCredentials().add("yet another cred");

		SAFCredential safCred = new ManagedConnectionImpl(0).getSAFCredential(subject);
		assertSame(testSafCred, safCred);
		assertEquals("theuserid", safCred.getUserId());
	}

	/**
	 * 
	 */
	@Test
	public void testGetSAFCredentialMultipleSAFCreds() {

		Subject subject = new Subject();
		SAFCredential testSafCred = new TestSAFCredentialImpl("theuserid");
		SAFCredential testSafCred2 = new TestSAFCredentialImpl("theuserid2");

		subject.getPrivateCredentials().add(testSafCred);
		subject.getPrivateCredentials().add(testSafCred2);
		subject.getPrivateCredentials().add("some other cred");

		SAFCredential safCred = new ManagedConnectionImpl(0).getSAFCredential(subject);
        assertTrue( safCred == testSafCred || safCred == testSafCred2 );    // gonna be 1 or the other
	}

	/**
	 * 
	 */
	@Test
	public void testIsEmpty() {
		assertTrue(new ManagedConnectionImpl(0).isEmpty(""));
		assertTrue(new ManagedConnectionImpl(0).isEmpty(null));
		assertTrue(new ManagedConnectionImpl(0).isEmpty("   "));
		assertFalse(new ManagedConnectionImpl(0).isEmpty("x"));
		assertFalse(new ManagedConnectionImpl(0).isEmpty(" x  "));
	}

	/**
	 * 
	 */
	@Test
	public void testExtractMvsUserId() throws Exception {

		// Set up jmock objects and expectations for SecurityService stuff.
		final SecurityServiceTracker mockSecurityServiceTracker = mockery.mock(SecurityServiceTracker.class);
		SecurityServiceTracker.staticInstance = mockSecurityServiceTracker;

		final SecurityService mockSecurityService = mockery.mock(SecurityService.class);
		final AuthenticationService mockAuthService = mockery.mock(AuthenticationService.class);

		final Subject subject = new Subject();
		SAFCredential testSafCred = new TestSAFCredentialImpl("theuserid");
		subject.getPrivateCredentials().add(testSafCred);

		mockery.checking(new Expectations() {
			{
				allowing(mockSecurityServiceTracker).getService();
				will(returnValue(mockSecurityService));

				allowing(mockSecurityService).getAuthenticationService();
				will(returnValue(mockAuthService));

                allowing(mockAuthService).authenticate(with(equal(JaasLoginConfigConstants.SYSTEM_DEFAULT)), with(any(AuthenticationData.class)), with(aNull(Subject.class)));
				will(returnValue(subject));
			}
		});

        // Note: extractMvsUserId() is called by the CTOR.  User ID is folded to upper case.
		ManagedConnectionImpl mci = buildManagedConnectionImpl();
		mci.getConnection(null, null);
		assertEquals("THEUSERID", mci.getMvsUserID());
	}

	/**
	 * Make sure that when we re-authenticate the user on getConnection, we are updating
	 * the MVS user ID.
	 */
	@Test
	public void testReAuthMvsUserId() throws Exception {

		// Set up jmock objects and expectations for SecurityService stuff.
		final SecurityServiceTracker mockSecurityServiceTracker = mockery.mock(SecurityServiceTracker.class);
		SecurityServiceTracker.staticInstance = mockSecurityServiceTracker;

		final SecurityService mockSecurityService = mockery.mock(SecurityService.class);
		final AuthenticationService mockAuthService = new TestAuthService();

		mockery.checking(new Expectations() {
			{
				allowing(mockSecurityServiceTracker).getService();
				will(returnValue(mockSecurityService));

				allowing(mockSecurityService).getAuthenticationService();
				will(returnValue(mockAuthService));
			}
		});

        // Note: extractMvsUserId() is called by the CTOR.  User ID is folded to upper case.
		ManagedConnectionImpl mci = buildManagedConnectionImpl();
		ConnectionRequestInfoImpl crii = ConnectionRequestInfoImplHelper.create("bob", "bobpw");
		mci.getConnection(null, crii);
		assertEquals("BOB", mci.getMvsUserID());
		
		crii = ConnectionRequestInfoImplHelper.create("fred", "fredpw");
		mci.getConnection(null,  crii);
		assertEquals("Should have switched to user FRED", "FRED", mci.getMvsUserID());
	}
	
	@Test
	public void testReAuthRunAsSubjectMvsUserId() throws Exception {
		// Set up jmock objects and expectations for SecurityService stuff.
		final SecurityServiceTracker mockSecurityServiceTracker = mockery.mock(SecurityServiceTracker.class);
		SecurityServiceTracker.staticInstance = mockSecurityServiceTracker;

		final SecurityService mockSecurityService = mockery.mock(SecurityService.class);
		final AuthenticationService mockAuthService = new TestAuthService();
		
		// Set up to allow WSSubject to return us a Subject
		@SuppressWarnings("unchecked")
		final ServiceReference<SubjectManagerService> subManSvcRef = mockery.mock(ServiceReference.class);
		final SubjectManagerService sms = mockery.mock(SubjectManagerService.class);
		final ComponentContext securityComponentContext = mockery.mock(ComponentContext.class);
		WSSubjectHelper.setup(securityComponentContext, subManSvcRef);

		// Create our run-as subject
		final Subject invocationSubject = new Subject();
		SAFCredential testSafCred = new TestSAFCredentialImpl("JOE");
		invocationSubject.getPrivateCredentials().add(testSafCred);
		
		mockery.checking(new Expectations() {
			{
				allowing(mockSecurityServiceTracker).getService();
				will(returnValue(mockSecurityService));

				allowing(mockSecurityService).getAuthenticationService();
				will(returnValue(mockAuthService));
				
				allowing(securityComponentContext).locateService(with(any(String.class)), with(equal(subManSvcRef)));
				will(returnValue(sms));
				
				allowing(sms).getInvocationSubject();
				will(returnValue(invocationSubject));
			}
		});

        // Note: extractMvsUserId() is called by the CTOR.  User ID is folded to upper case.
		ManagedConnectionImpl mci = buildManagedConnectionImpl(null, null);
		ConnectionRequestInfoImpl crii = ConnectionRequestInfoImplHelper.create("bob", "bobpw");
		mci.getConnection(null, crii);
		assertEquals("BOB", mci.getMvsUserID());
		
		// Get a new connection without userid and password - should use run-as subject.
		mci.getConnection(null,  null);
		assertEquals("Should have used invocation subject of JOE", "JOE", mci.getMvsUserID());
		
		crii = ConnectionRequestInfoImplHelper.create("fred", "fredpw");
		mci.getConnection(null,  crii);
		assertEquals("Should have switched to user FRED", "FRED", mci.getMvsUserID());
	}
	
	@Test
	public void testReAuthJAASSubjectMvsUserId() throws Exception {

		// Set up jmock objects and expectations for SecurityService stuff.
		final SecurityServiceTracker mockSecurityServiceTracker = mockery.mock(SecurityServiceTracker.class);
		SecurityServiceTracker.staticInstance = mockSecurityServiceTracker;

		final SecurityService mockSecurityService = mockery.mock(SecurityService.class);
		final AuthenticationService mockAuthService = new TestAuthService();

		mockery.checking(new Expectations() {
			{
				allowing(mockSecurityServiceTracker).getService();
				will(returnValue(mockSecurityService));

				allowing(mockSecurityService).getAuthenticationService();
				will(returnValue(mockAuthService));
			}
		});

        // Note: extractMvsUserId() is called by the CTOR.  User ID is folded to upper case.
		ManagedConnectionFactoryImpl mcf = createManagedConnectionFactoryImpl("bob", "bobpw");
		ManagedConnectionImpl mci = new ManagedConnectionImpl(false, mcf, 0, null, null);
		mci.getConnection(null, null);
		assertEquals("BOB", mci.getMvsUserID());

		// Now use a JAAS subject to create the user.
		PasswordCredential pc = new PasswordCredential("lucy", "lucypw".toCharArray());
		pc.setManagedConnectionFactory(mcf);
		final Subject jaasSubject = new Subject();
		jaasSubject.getPrivateCredentials().add(pc);
		
		mci.getConnection(jaasSubject, null);
		assertEquals("Should have switched to user LUCY", "LUCY", mci.getMvsUserID());
		
		// Now try switching back to a regular user.
		mci.getConnection(null, null);
		assertEquals("Should have switched back to user BOB", "BOB", mci.getMvsUserID());
	}
	
	/**
	 */
	@Test
	public void testOTMAMemberName() throws Exception {

		String defaultOTMAClientName = ConnectionSpecImpl.DEFAULT_OTMA_CLIENT_NAME;

		String validOTMAMemberName1 = "VALIDOTMAMBRNM1";
		String validOTMAMemberName2 = "234@#$TMAMBRNM2";

		String invalidOMN_length = "THISISNOTAVALIDOTMAMEMBERNAME";
		String invalidOMN_empty = "";
		String invalidOMN_whitespace = "                ";
		String invalidOMN_whitespace_length = "                             ";
		String invalidOMN_badchar = ")(*&^%{}";
		String invalidOMN_whitespace_middle = "WHITE SPACE";
		String invalidOMN_null = null;

		List<String> otmaServerNames = new ArrayList<String>();
		otmaServerNames.add(validOTMAMemberName1);
		otmaServerNames.add(validOTMAMemberName2);
		otmaServerNames.add(invalidOMN_length);
		otmaServerNames.add(invalidOMN_empty);
		otmaServerNames.add(invalidOMN_whitespace);
		otmaServerNames.add(invalidOMN_whitespace_length);
		otmaServerNames.add(invalidOMN_badchar);
		otmaServerNames.add(invalidOMN_whitespace_middle);
		otmaServerNames.add(invalidOMN_null);

		List<String> otmaClientNames = new ArrayList<String>(otmaServerNames);
		otmaClientNames.add(defaultOTMAClientName);

		String validOTMAGroupName1 = "MEMBERNM";
		String validOTMAGroupName2 = "234@#$MN";

		String invalidOGN_length = "THISISNOTAVALIDOTMAMEMBERNAME";
		String invalidOGN_empty = "";
		String invalidOGN_whitespace = "        ";
		String invalidOGN_whitespace_length = "                             ";
		String invalidOGN_badchar = ")(*&^%{}";
		String invalidOGN_whitespace_middle = "WHT SP";
		String invalidOGN_null = null;

		List<String> otmaGroupNames = new ArrayList<String>();
		otmaGroupNames.add(validOTMAGroupName1);
		otmaGroupNames.add(validOTMAGroupName2);
		otmaGroupNames.add(invalidOGN_length);
		otmaGroupNames.add(invalidOGN_empty);
		otmaGroupNames.add(invalidOGN_whitespace);
		otmaGroupNames.add(invalidOGN_whitespace_length);
		otmaGroupNames.add(invalidOGN_badchar);
		otmaGroupNames.add(invalidOGN_whitespace_middle);
		otmaGroupNames.add(invalidOGN_null);

		int count = 1;
		for (int i = 0; i < otmaClientNames.size(); i++) {
			for (int j = 0; j < otmaServerNames.size(); j++) {
				for (int k = 0; k < otmaGroupNames.size(); k++) {

					String clientName = otmaClientNames.get(i);
					String serverName = otmaServerNames.get(j);
					String groupName = otmaGroupNames.get(k);

					String clientNameExpected = defaultOTMAClientName;
					if (clientName != null) {
						if (clientName.equals(validOTMAMemberName1))
							clientNameExpected = validOTMAMemberName1;
						if (clientName.equals(validOTMAMemberName2))
							clientNameExpected = validOTMAMemberName2;
					}

					String serverNameExpected = null;
					if (serverName != null) {
						if (serverName.equals(validOTMAMemberName1))
							serverNameExpected = validOTMAMemberName1;
						if (serverName.equals(validOTMAMemberName2))
							serverNameExpected = validOTMAMemberName2;
					}

					String groupNameExpected = null;
					if (groupName != null) {
						if (groupName.equals(validOTMAGroupName1))
							groupNameExpected = validOTMAGroupName1;
						if (groupName.equals(validOTMAGroupName2))
							groupNameExpected = validOTMAGroupName2;
					}

					testOTMAMemberNameHelper(null, clientName, clientNameExpected, null, serverName, serverNameExpected, null, groupName, groupNameExpected, count++);
					testOTMAMemberNameHelper(null, clientName, clientNameExpected, null, serverName, serverNameExpected, groupName, null, groupNameExpected, count++);
					testOTMAMemberNameHelper(null, clientName, clientNameExpected, serverName, null, serverNameExpected, groupName, null, groupNameExpected, count++);
					testOTMAMemberNameHelper(clientName, null, clientNameExpected, serverName, null, serverNameExpected, groupName, null, groupNameExpected, count++);
					testOTMAMemberNameHelper(clientName, null, clientNameExpected, serverName, null, serverNameExpected, null, groupName, groupNameExpected, count++);
					testOTMAMemberNameHelper(clientName, null, clientNameExpected, null, serverName, serverNameExpected, null, groupName, groupNameExpected, count++);
				}
			}
		}
		// System.out.println("client size "+otmaClientNames.size());
		// System.out.println("server size "+otmaServerNames.size());
		// System.out.println("group size "+otmaGroupNames.size());
		// System.out.println("total size: "+otmaClientNames.size() * otmaServerNames.size() * otmaGroupNames.size() * 6);
	}

	/**
	 */
	private void testOTMAMemberNameHelper(String clientNameConfig, String clientNameConnection,
			String clientNameExpected, String serverNameConfig, String serverNameConnection, String serverNameExpected,
			String groupNameConfig, String groupNameConnection, String groupNameExpected, int count) throws SecurityException {
                String clientNameReceived = null, serverNameReceived = null, groupNameReceived = null;
	            System.out.println("__________________________________________________________________________________________________________ " + count + " _________");
		        String format = "%15s%35s%35s%35s%n";
		        System.out.format(format + "%n", "",          "client",             "server",             "group");
		        System.out.format(format,        "config",     clientNameConfig,     serverNameConfig,     groupNameConfig);
		        System.out.format(format + "%n", "connection", clientNameConnection, serverNameConnection, groupNameConnection);
		        System.out.format(format,        "expected",   clientNameExpected,   serverNameExpected,   groupNameExpected);
		try {
			ManagedConnectionFactoryImpl mcf = new ManagedConnectionFactoryImpl();
			mcf.setOTMAClientName(clientNameConfig);
			mcf.setOTMAServerName(serverNameConfig);
			mcf.setOTMAGroupID(groupNameConfig);
			ConnectionRequestInfoImpl crii = new ConnectionRequestInfoImpl("blah", 0, "blah", "blah", 0, "blah", 0, false, "blah", 0, false,
					clientNameConnection, serverNameConnection, groupNameConnection, "1", 0, 0, false, false,
					"blah", "blah", false, false, false, false, false, false, false, false, false, false,
					false);
			ManagedConnectionImpl mci = new ManagedConnectionImpl(false, mcf, 0, crii, null);
			clientNameReceived = mci.getOTMAClientName();
			serverNameReceived = mci.getOTMAServerName();
			groupNameReceived = mci.getOTMAGroupID();

		        System.out.format(format,        "received",   clientNameReceived,   serverNameReceived,   groupNameReceived);

			assertEquals(clientNameExpected, clientNameReceived);
			assertEquals(serverNameExpected, serverNameReceived);
			assertEquals(groupNameExpected, groupNameReceived);
		} catch (IllegalArgumentException iae) {
			List<String> msgs = new ArrayList<String>();
			msgs.add("OTMA Client name (THISISNOTAVALIDOTMAMEMBERNAME) must be between 1 and 16 characters");
			msgs.add("OTMA Client name (                             ) must be between 1 and 16 characters");
			msgs.add("OTMA Client name (                ) must be between 1 and 16 characters");
			msgs.add("OTMA Client name () must be between 1 and 16 characters");
		        msgs.add("OTMA Client name ()(*&^%{}) can only use characters A-Z, 0-9, @, #, and $");
		        msgs.add("OTMA Client name (WHITE SPACE) can only use characters A-Z, 0-9, @, #, and $");
			msgs.add("CWWKB0395E: The specified OTMA Server name (THISISNOTAVALIDOTMAMEMBERNAME) is blank, greater than 16 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
			msgs.add("CWWKB0395E: The specified OTMA Server name (                             ) is blank, greater than 16 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
			msgs.add("CWWKB0395E: The specified OTMA Server name (                ) is blank, greater than 16 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
			msgs.add("CWWKB0395E: The specified OTMA Server name () is blank, greater than 16 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
			msgs.add("CWWKB0395E: The specified OTMA Server name (null) is blank, greater than 16 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
			msgs.add("CWWKB0395E: The specified OTMA Server name ()(*&^%{}) is blank, greater than 16 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
			msgs.add("CWWKB0395E: The specified OTMA Server name (WHITE SPACE) is blank, greater than 16 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
	                msgs.add("CWWKB0395E: The specified OTMA Group name (THISISNOTAVALIDOTMAMEMBERNAME) is blank, greater than 8 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
		        msgs.add("CWWKB0395E: The specified OTMA Group name (                             ) is blank, greater than 8 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
		        msgs.add("CWWKB0395E: The specified OTMA Group name (                ) is blank, greater than 8 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
		        msgs.add("CWWKB0395E: The specified OTMA Group name (        ) is blank, greater than 8 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
		        msgs.add("CWWKB0395E: The specified OTMA Group name () is blank, greater than 8 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
		        msgs.add("CWWKB0395E: The specified OTMA Group name (null) is blank, greater than 8 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
		        msgs.add("CWWKB0395E: The specified OTMA Group name ()(*&^%{}) is blank, greater than 8 characters long, or contains characters other than A-Z, 0-9, @, #, and $");
		        msgs.add("CWWKB0395E: The specified OTMA Group name (WHT SP) is blank, greater than 8 characters long, or contains characters other than A-Z, 0-9, @, #, and $");

			String msg = iae.getMessage();
            System.out.println(msg);
			boolean isExpected = false;
			for (String expmsg : msgs) {
			        isExpected |= (msg != null) && expmsg.equals(msg);
		        }

			if (!isExpected) {
			        iae.printStackTrace(System.out);
				assertTrue("The exception was not expected.", isExpected);
			}
		}
	}
	
	static class TestAuthService implements AuthenticationService {

		@Override
		public Subject authenticate(String jaasEntryName, CallbackHandler callbackHandler, Subject subject)
				throws AuthenticationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Subject authenticate(String jaasEntryName, AuthenticationData authenticationData, Subject ignore)
				throws AuthenticationException {
			final Subject subject = new Subject();
			SAFCredential testSafCred = new TestSAFCredentialImpl((String)authenticationData.get(AuthenticationData.USERNAME));
			subject.getPrivateCredentials().add(testSafCred);
			return subject;
		}

		@Override
		public Subject authenticate(String jaasEntryName, Subject inputSubject) throws AuthenticationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Subject delegate(String roleName, String appName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Boolean isAllowHashTableLoginWithIdOnly() {
			return Boolean.FALSE;
		}

		@Override
		public AuthCacheService getAuthCacheService() {
			return null;
		}

		@Override
		public String getInvalidDelegationUser() {
			// TODO Auto-generated method stub
			return null;
		}
	}
}

/**
 * Simple test implementation of a SAFCredential.
 */
class TestSAFCredentialImpl implements SAFCredential {

	private String userId;

	public TestSAFCredentialImpl(String userId) {
		this.userId = userId;
	}

	@Override
	public String getUserId() {
		return userId;
	}

	@Override
	public String getAuditString() {
		return null;
	}

	@Override
	public X509Certificate getCertificate() {
		return null;
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public boolean isAuthenticated() {
		return false;
	}

	@Override
	public String getMvsUserId() {
		return null;
	}

	@Override
	public String getRealm() {
		return null;
	}

	@Override
	public String getDistributedUserId() {
		return null;
	}
}
