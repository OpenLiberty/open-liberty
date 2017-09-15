/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.security.PrivilegedExceptionAction;

import javax.interceptor.InvocationContext;

import org.jmock.Mockery;
import org.junit.Test;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;

public class InterceptorMetaDataHelperTest {
    final Mockery mockery = new Mockery();

    public static class TestAround {
        public Object pass(InvocationContext ic) {
            return null;
        }

        public void passVoid(InvocationContext ic) {}

        public final Object failFinal(InvocationContext ic) {
            return null;
        }

        public static Object failStatic(InvocationContext ic) {
            return null;
        }

        public Object failParamType(boolean b) {
            return null;
        }

        public Object failNoParams() {
            return null;
        }

        public Object failExtraParam(InvocationContext ic, boolean b) {
            return null;
        }

        public void failReturnVoid(InvocationContext ic) {}

        public boolean failReturnPrimitive(InvocationContext ic) {
            return false;
        }

        public String failReturnString(InvocationContext ic) {
            return null;
        }
    }

    @Test
    public void testValidateAroundSignature() throws Exception {
        InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_INVOKE,
                                                          TestAround.class.getDeclaredMethod("pass", InvocationContext.class), null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateAroundSignatureFinal() throws Exception {
        InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_INVOKE,
                                                          TestAround.class.getDeclaredMethod("failFinal", InvocationContext.class), null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateAroundSignatureStatic() throws Exception {
        InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_INVOKE,
                                                          TestAround.class.getDeclaredMethod("failStatic", InvocationContext.class), null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateAroundSignatureParamType() throws Exception {
        InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_INVOKE,
                                                          TestAround.class.getDeclaredMethod("failParamType", boolean.class), null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateAroundSignatureNoParams() throws Exception {
        InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_INVOKE,
                                                          TestAround.class.getDeclaredMethod("failNoParams"), null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateAroundSignatureExtraParam() throws Exception {
        InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_INVOKE,
                                                          TestAround.class.getDeclaredMethod("failExtraParam", InvocationContext.class, boolean.class), null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateAroundSignatureReturnVoid() throws Exception {
        InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_INVOKE,
                                                          TestAround.class.getDeclaredMethod("failReturnVoid", InvocationContext.class), null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateAroundSignatureReturnPrimitive() throws Exception {
        InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_INVOKE,
                                                          TestAround.class.getDeclaredMethod("failReturnPrimitive", InvocationContext.class), null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateAroundSignatureReturnString() throws Exception {
        CheckEJBAppConfigHelperHelper.failable(new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws Exception {
                InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_INVOKE,
                                                                  TestAround.class.getDeclaredMethod("failReturnString", InvocationContext.class), null);
                return null;
            }
        });
    }

    @Test
    public void testValidateAroundSignatureReturnStringCompatibility() throws Exception {
        InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_INVOKE,
                                                          TestAround.class.getDeclaredMethod("failReturnString", InvocationContext.class), null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateAroundSignatureReturnStringTimeout() throws Exception {
        InterceptorMetaDataHelper.validateAroundSignature(InterceptorMethodKind.AROUND_TIMEOUT,
                                                          TestAround.class.getDeclaredMethod("failReturnString", InvocationContext.class), null);
    }

    public static class TestLifeCycle {
        public void pass() {}

        public Object pass(InvocationContext ic) {
            return null;
        }

        public final void failFinal() {}

        public static void failStatic() {}

        public boolean failReturnPrimitive() {
            return false;
        }

        public String failReturnObject() {
            return null;
        }
    }

    @Test
    public void testValidateLifeCycleSignatureExceptParameters() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.POST_CONSTRUCT, null, TestLifeCycle.class.getDeclaredMethod("pass"), false, null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateLifeCycleSignatureExceptParametersFinal() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.POST_CONSTRUCT, null, TestLifeCycle.class.getDeclaredMethod("failFinal"), false,
                                                                             null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateLifeCycleSignatureExceptParametersStatic() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.POST_CONSTRUCT, null, TestLifeCycle.class.getDeclaredMethod("failStatic"),
                                                                             false,
                                                                             null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateLifeCycleSignatureExceptParametersReturnPrimitive() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.POST_CONSTRUCT, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("failReturnPrimitive"), false, null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateLifeCycleSignatureExceptParametersReturnObject() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.POST_CONSTRUCT, null, TestLifeCycle.class.getDeclaredMethod("failReturnObject"),
                                                                             false, null);
    }

    @Test
    public void testValidateLifeCycleSignaturePassPostConstructOnEJBClass() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.POST_CONSTRUCT, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass"), true,
                                                                             new J2EENameImpl("a", "m", "c"));
    }

    @Test
    public void testValidateLifeCycleSignaturePassPostConstructNotOnEJBClass() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.POST_CONSTRUCT, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass"), false,
                                                                             new J2EENameImpl("a", "m", "c"));
    }

    @Test
    public void testValidateLifeCycleSignaturePassPrePassivateOnEJBClass() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.PRE_PASSIVATE, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass"), true,
                                                                             new J2EENameImpl("a", "m", "c"));
    }

    @Test
    public void testValidateLifeCycleSignaturePassPrePassivateNotOnEJBClass() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.PRE_PASSIVATE, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass"), false,
                                                                             new J2EENameImpl("a", "m", "c"));
    }

    @Test
    public void testValidateLifeCycleSignaturePassPostActivateOnEJBClass() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.POST_ACTIVATE, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass"), true,
                                                                             new J2EENameImpl("a", "m", "c"));
    }

    @Test
    public void testValidateLifeCycleSignaturePassPostActivateNotOnEJBClass() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.POST_ACTIVATE, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass"), false,
                                                                             new J2EENameImpl("a", "m", "c"));
    }

    @Test
    public void testValidateLifeCycleSignaturePassPreDestroyOnEJBClass() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.PRE_DESTROY, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass"), true,
                                                                             new J2EENameImpl("a", "m", "c"));
    }

    @Test
    public void testValidateLifeCycleSignaturePassPreDestroyNotOnEJBClass() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.PRE_DESTROY, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass"), false,
                                                                             new J2EENameImpl("a", "m", "c"));
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateLifeCycleSignatureFailAroundConstructOnEJBClass() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.AROUND_CONSTRUCT, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("failReturnObject"), true,
                                                                             new J2EENameImpl("a", "m", "c"), true);
    }

    @Test
    public void testValidateLifeCycleSignaturePassAroundConstructNotOnEJBClass() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.AROUND_CONSTRUCT, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass", InvocationContext.class), false,
                                                                             new J2EENameImpl("a", "m", "c"), true);
    }

    @Test
    public void testValidateLifeCycleSignatureReturnObjectPassAroundConstruct() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.AROUND_CONSTRUCT, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass", InvocationContext.class), false,
                                                                             new J2EENameImpl("a", "m", "c"), true);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testValidateLifeCycleSignatureReturnStringAroundConstruct() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.AROUND_CONSTRUCT, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("failReturnObject"), false,
                                                                             new J2EENameImpl("a", "m", "c"), true);
    }

    @Test
    public void testValidateLifeCycleSignatureVoidPassAroundConstruct() throws Exception {
        InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(InterceptorMethodKind.AROUND_CONSTRUCT, null,
                                                                             TestLifeCycle.class.getDeclaredMethod("pass"), false,
                                                                             new J2EENameImpl("a", "m", "c"), true);
    }
}
