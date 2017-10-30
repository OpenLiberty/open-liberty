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
package com.ibm.ws.security.javaeesec.identitystore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;

import org.jmock.Expectations;
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

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.security.javaeesec.CDIHelperTestWrapper;

import test.common.SharedOutputManager;

public class IdentityStoreHandlerImplTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String IS_MANDATORY_POLICY = "javax.security.auth.message.MessagePolicy.isMandatory";
    private final String USER1 = "user1";
    private final String USER2 = "user2";
    private final String USER3 = "user3";
    private final String GROUP1A = "group1a";
    private final String GROUP1B = "group1b";
    private final String GROUP2A = "group2a";
    private final String GROUP2B = "group2b";
    private final String GROUP3A = "group3a";
    private final String GROUP3B = "group3b";
    private IdentityStoreHandlerImpl ish;
    private BeanManager beanManager;
    @SuppressWarnings("rawtypes")
    private CDI cdi;
    private IdentityStore is1;
    private IdentityStore is2;
    private IdentityStore is3;
    private CredentialValidationResult result1;
    private final Set<String> group1 = new HashSet<String>();
    private final Set<String> group2 = new HashSet<String>();
    private final Set<String> group3 = new HashSet<String>();

    private CDIService cdiService;
    private CDIHelperTestWrapper cdiHelperTestWrapper;

//    private HttpAuthenticationMechanism httpAuthenticationMechanism;
//    private HttpServletRequest request;
//    private HttpServletResponse response;
//    private Subject clientSubject;
//    private Subject serviceSubject;

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.javaeesec.*=all");

    @Rule
    public final TestName testName = new TestName();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        cdi = mockery.mock(CDI.class);
        is1 = mockery.mock(IdentityStore.class, "IS1");
        is2 = mockery.mock(IdentityStore.class, "IS2");
        is3 = mockery.mock(IdentityStore.class, "IS3");
        group1.add(GROUP1A);
        group1.add(GROUP1B);
        result1 = new CredentialValidationResult(new CallerPrincipal("user1"), group1);
        group2.add(GROUP2A);
        group2.add(GROUP2B);
        group3.add(GROUP3A);
        group3.add(GROUP3B);

        beanManager = mockery.mock(BeanManager.class, "beanManager");
        cdiService = mockery.mock(CDIService.class);
        cdiHelperTestWrapper = new CDIHelperTestWrapper(mockery, beanManager);
        cdiHelperTestWrapper.setCDIService(cdiService);
//        httpAuthenticationMechanism = mockery.mock(HttpAuthenticationMechanism.class);
//        request = mockery.mock(HttpServletRequest.class);
//        response = mockery.mock(HttpServletResponse.class);
//        clientSubject = new Subject();
//        serviceSubject = null;

        ish = new IdentityStoreHandlerImpl() {
            @SuppressWarnings("rawtypes")
            @Override
            protected CDI getCDI() {
                return cdi;
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        cdiHelperTestWrapper.unsetCDIService(cdiService);
        mockery.assertIsSatisfied();
    }

    /**
     * 1st IdentityStore successfully validate user and returns groups as well.
     * 2nd and 3rd supports group only.
     * Make sure that 2nd and 3rd group lookup are invoked merged the result.
     */
    @Test
    public void testValidate1ValidateGroup2Group3Group() throws Exception {
        withBeanInstance().with1ValidateAndGroup2Group3Group().with1stValidateAndGroup2Group3Group();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        iss.clear();
        UsernamePasswordCredential cred = new UsernamePasswordCredential("user1", "security");
        CredentialValidationResult result = ish.validate(cred);
        assertEquals("The result shuld be VALID.", CredentialValidationResult.Status.VALID, result.getStatus());
        assertEquals("CallerPrincipal name should be user1", USER1, result.getCallerPrincipal().getName());
        Set<String> groups = result.getCallerGroups();
        assertTrue("Size of the group should be 6", groups.size() == 6);
        assertTrue("The contents of the groups are not valid.", groups.contains(GROUP1A) && groups.contains(GROUP1B) && groups.contains(GROUP2A) && groups.contains(GROUP2B)
                                                                && groups.contains(GROUP3A) && groups.contains(GROUP3B));
    }

    /**
     * All 3 returns not validated.
     * Make sure that all identityStore are called.
     */
    @Test
    public void testValidate1NotValidated2Error3NotValidated() throws Exception {
        withBeanInstance().withAllValidateAndGroup().with1NotValidated2Error3NotValidated();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        iss.clear();
        UsernamePasswordCredential cred = new UsernamePasswordCredential("user1", "security");
        CredentialValidationResult result = ish.validate(cred);
        assertEquals("The result shuld be INVALID.", CredentialValidationResult.INVALID_RESULT, result);
    }

    /**
     * All 3 returns not validated.
     * Make sure that all identityStore are called.
     */
    @Test
    public void testValidateAllNotValidated() throws Exception {
        withBeanInstance().withAllValidateAndGroup().withAllNotValidated();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        iss.clear();
        UsernamePasswordCredential cred = new UsernamePasswordCredential("user1", "security");
        CredentialValidationResult result = ish.validate(cred);
        assertEquals("The result shuld be INVALID.", CredentialValidationResult.NOT_VALIDATED_RESULT, result);
    }

    /**
     * All 3 returns error.
     * Make sure that all identityStore are called.
     */
    @Test
    public void testValidateAllError() throws Exception {
        withBeanInstance().withAllValidateAndGroup().withAllError();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        iss.clear();
        UsernamePasswordCredential cred = new UsernamePasswordCredential("user1", "security");
        CredentialValidationResult result = ish.validate(cred);
        assertEquals("The result shuld be INVALID.", CredentialValidationResult.INVALID_RESULT, result);
    }

    /**
     * 1st returns invalid, 2nd returns not validate.
     * 3rd identity store validate the user.
     * Make sure that all identityStore are called.
     */
    @Test
    public void testValidate1Error2Novalidate3ValidateGroup() throws Exception {
        withBeanInstance().withAllValidateAndGroup().with3rdValidateAndGroup1Error2NoValidate();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        iss.clear();
        UsernamePasswordCredential cred = new UsernamePasswordCredential("user1", "security");
        CredentialValidationResult result = ish.validate(cred);
        assertEquals("The result shuld be VALID.", CredentialValidationResult.Status.VALID, result.getStatus());
        assertEquals("CallerPrincipal name should be user1", USER1, result.getCallerPrincipal().getName());
        Set<String> groups = result.getCallerGroups();
        assertTrue("Size of the group should be two", groups.size() == 2);
        assertTrue("group1a and group1b should be in the groups", groups.contains(GROUP1A) && groups.contains(GROUP1B));
    }

    /**
     * 3rd identity store validate the user.
     * Make sure that all identityStore are called.
     */
    @Test
    public void testValidate1Novalidate2Novalidate3ValidateGroup() throws Exception {
        withBeanInstance().withAllValidateAndGroup().with3rdValidateAndGroup();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        iss.clear();
        UsernamePasswordCredential cred = new UsernamePasswordCredential("user1", "security");
        CredentialValidationResult result = ish.validate(cred);
        assertEquals("The result shuld be VALID.", CredentialValidationResult.Status.VALID, result.getStatus());
        assertEquals("CallerPrincipal name should be user1", USER1, result.getCallerPrincipal().getName());
        Set<String> groups = result.getCallerGroups();
        assertTrue("Size of the group should be two", groups.size() == 2);
        assertTrue("group1a and group1b should be in the groups", groups.contains(GROUP1A) && groups.contains(GROUP1B));
    }

    /**
     * 1st IdentityStore successfully validate user and returns groups as well.
     * 2nd and 3rd supports both validate and group.
     * Make sure that 2nd and 3rd group lookup won't be invoked.
     */
    @Test
    public void testValidate1ValidateGroup2Validate3Validate() throws Exception {
        withBeanInstance().withAllValidateAndGroup().with1stValidateAndGroup();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        iss.clear();
        UsernamePasswordCredential cred = new UsernamePasswordCredential("user1", "security");
        CredentialValidationResult result = ish.validate(cred);
        assertEquals("The result shuld be VALID.", CredentialValidationResult.Status.VALID, result.getStatus());
        assertEquals("CallerPrincipal name should be user1", USER1, result.getCallerPrincipal().getName());
        Set<String> groups = result.getCallerGroups();
        assertTrue("Size of the group should be two", groups.size() == 2);
        assertTrue("group1a and group1b should be in the groups", groups.contains(GROUP1A) && groups.contains(GROUP1B));
    }

    @Test
    public void testValidateNoIdentityStoreSupportsValidation() throws Exception {
        withBeanInstance().withNoValidationSupport();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        iss.clear();
        UsernamePasswordCredential cred = new UsernamePasswordCredential("user1", "security");
        CredentialValidationResult result = ish.validate(cred);
        assertEquals("The result shuld be NOT_VALIDATED_RESULT.", CredentialValidationResult.NOT_VALIDATED_RESULT, result);
        assertTrue("CWWKS1911E error message was not logged", outputMgr.checkForStandardErr("CWWKS1911E:"));
    }

    @Test
    public void testValidateNoIdentityStore() throws Exception {
        withoutBeanInstance();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        iss.clear();
        UsernamePasswordCredential cred = new UsernamePasswordCredential("user1", "security");
        CredentialValidationResult result = ish.validate(cred);
        assertEquals("The result shuld be NT_VALIDATED_RESULT.", CredentialValidationResult.NOT_VALIDATED_RESULT, result);
        assertTrue("CWWKS1910E error message was not logged", outputMgr.checkForStandardErr("CWWKS1910E:"));
    }

    @Test
    public void testScanIdentityStoresValid() throws Exception {
        withBeanInstance().withNoValidationSupport();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        iss.clear();
        ish.scanIdentityStores(iss);
        assertEquals("Two identityStores should be placed", 3, iss.size());
        Iterator<IdentityStore> itr = iss.iterator();
        assertEquals("identityStore1 should be placed at the first", is1, itr.next());
        assertEquals("identityStore2 should be placed at the second", is2, itr.next());
        assertEquals("identityStore3 should be placed at the last", is3, itr.next());
    }

    @Test
    public void testScanIdentityStoresNoIdentityStore() throws Exception {
        withoutBeanInstance();
        TreeSet<IdentityStore> iss = ish.getIdentityStores();
        ish.scanIdentityStores(iss);
        assertEquals("no identityStores should be placed", 0, iss.size());
        assertTrue("Expected error message was not logged", outputMgr.checkForStandardErr("CWWKS1910E:"));
    }

    /*************** support methods **************/
    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest withBeanInstance() throws Exception {
        final myInstance inst = new myInstance();
        inst.add(is3);
        inst.add(is2);
        inst.add(is1);

        mockery.checking(new Expectations() {
            {
                one(cdi).select(IdentityStore.class);
                will(returnValue(inst));
                one(cdi).getBeanManager();
                will(returnValue(beanManager));
                allowing(is1).priority();
                will(returnValue(1));
                allowing(is2).priority();
                will(returnValue(2));
                allowing(is3).priority();
                will(returnValue(3));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private void withoutBeanInstance() {
        mockery.checking(new Expectations() {
            {
                one(cdi).select(IdentityStore.class);
                will(returnValue(null));
                one(cdi).getBeanManager();
                will(returnValue(beanManager));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest withNoValidationSupport() throws Exception {
        final HashSet<ValidationType> set = new HashSet<ValidationType>();
        set.add(ValidationType.PROVIDE_GROUPS);
        mockery.checking(new Expectations() {
            {
                allowing(is1).validationTypes();
                will(returnValue(set));
                allowing(is2).validationTypes();
                will(returnValue(set));
                allowing(is3).validationTypes();
                will(returnValue(set));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest withAllValidateAndGroup() throws Exception {
        final HashSet<ValidationType> set = new HashSet<ValidationType>();
        set.add(ValidationType.PROVIDE_GROUPS);
        set.add(ValidationType.VALIDATE);
        mockery.checking(new Expectations() {
            {
                allowing(is1).validationTypes();
                will(returnValue(set));
                allowing(is2).validationTypes();
                will(returnValue(set));
                allowing(is3).validationTypes();
                will(returnValue(set));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest with1ValidateAndGroup2Group3Group() throws Exception {
        final HashSet<ValidationType> setAll = new HashSet<ValidationType>();
        setAll.add(ValidationType.PROVIDE_GROUPS);
        setAll.add(ValidationType.VALIDATE);
        final HashSet<ValidationType> setGroups = new HashSet<ValidationType>();
        setGroups.add(ValidationType.PROVIDE_GROUPS);
        mockery.checking(new Expectations() {
            {
                allowing(is1).validationTypes();
                will(returnValue(setAll));
                allowing(is2).validationTypes();
                will(returnValue(setGroups));
                allowing(is3).validationTypes();
                will(returnValue(setGroups));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest with1stValidateAndGroup() throws Exception {
        final CredentialValidationResult result1f = result1;
        mockery.checking(new Expectations() {
            {
                one(is1).validate(with(any(Credential.class)));
                will(returnValue(result1f));
                never(is2).validate(with(any(Credential.class)));
                never(is3).validate(with(any(Credential.class)));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest with3rdValidateAndGroup() throws Exception {
        final CredentialValidationResult result1f = result1;
        mockery.checking(new Expectations() {
            {
                one(is1).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.NOT_VALIDATED_RESULT));
                one(is2).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.NOT_VALIDATED_RESULT));
                one(is3).validate(with(any(Credential.class)));
                will(returnValue(result1f));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest with3rdValidateAndGroup1Error2NoValidate() throws Exception {
        final CredentialValidationResult result1f = result1;
        mockery.checking(new Expectations() {
            {
                one(is1).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.INVALID_RESULT));
                one(is2).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.NOT_VALIDATED_RESULT));
                one(is3).validate(with(any(Credential.class)));
                will(returnValue(result1f));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest withAllError() throws Exception {
        final CredentialValidationResult result1f = result1;
        mockery.checking(new Expectations() {
            {
                one(is1).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.INVALID_RESULT));
                one(is2).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.INVALID_RESULT));
                one(is3).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.INVALID_RESULT));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest withAllNotValidated() throws Exception {
        final CredentialValidationResult result1f = result1;
        mockery.checking(new Expectations() {
            {
                one(is1).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.NOT_VALIDATED_RESULT));
                one(is2).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.NOT_VALIDATED_RESULT));
                one(is3).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.NOT_VALIDATED_RESULT));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest with1NotValidated2Error3NotValidated() throws Exception {
        final CredentialValidationResult result1f = result1;
        mockery.checking(new Expectations() {
            {
                one(is1).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.NOT_VALIDATED_RESULT));
                one(is2).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.INVALID_RESULT));
                one(is3).validate(with(any(Credential.class)));
                will(returnValue(CredentialValidationResult.NOT_VALIDATED_RESULT));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerImplTest with1stValidateAndGroup2Group3Group() throws Exception {
        final CredentialValidationResult result1f = result1;
        mockery.checking(new Expectations() {
            {
                one(is1).validate(with(any(Credential.class)));
                will(returnValue(result1f));
                never(is2).validate(with(any(Credential.class)));
                never(is3).validate(with(any(Credential.class)));
                one(is2).getCallerGroups(with(any(CredentialValidationResult.class)));
                will(returnValue(group2));
                one(is3).getCallerGroups(with(any(CredentialValidationResult.class)));
                will(returnValue(group3));

            }
        });
        return this;
    }

    @SuppressWarnings("rawtypes")
    class myInstance<T> implements Instance {
        final HashSet set = new HashSet<Instance<Object>>();

        @Override
        @SuppressWarnings("unchecked")
        public Iterator<T> iterator() {
            return set.iterator();
        }

        @SuppressWarnings("unchecked")
        public void add(Object obj) {
            set.add(obj);
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.inject.Provider#get()
         */
        @Override
        public Object get() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.enterprise.inject.Instance#destroy(java.lang.Object)
         */
        @Override
        public void destroy(Object arg0) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see javax.enterprise.inject.Instance#select(java.lang.Class, java.lang.annotation.Annotation[])
         */
        @Override
        public Instance select(Class arg0, Annotation... arg1) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.enterprise.inject.Instance#select(javax.enterprise.util.TypeLiteral, java.lang.annotation.Annotation[])
         */
        @Override
        public Instance select(TypeLiteral arg0, Annotation... arg1) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.enterprise.inject.Instance#isAmbiguous()
         */
        @Override
        public boolean isAmbiguous() {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.enterprise.inject.Instance#isUnsatisfied()
         */
        @Override
        public boolean isUnsatisfied() {
            // TODO Auto-generated method stub
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.enterprise.inject.Instance#select(java.lang.annotation.Annotation[])
         */
        @Override
        public Instance select(Annotation... arg0) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
