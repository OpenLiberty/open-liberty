/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.util;

import java.rmi.RemoteException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.util.DeploymentUtil;
import com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType;

public class DeploymentUtilTest {
    private static final DeploymentUtil.DeploymentTarget WRAPPER = DeploymentUtil.DeploymentTarget.WRAPPER;
    private static final DeploymentUtil.DeploymentTarget STUB = DeploymentUtil.DeploymentTarget.STUB;
    private static final DeploymentUtil.DeploymentTarget TIE = DeploymentUtil.DeploymentTarget.TIE;

    // Default value for DeclaredUncheckedAreSystemExceptions.
    private static final boolean DUASE = true;
    // Default value for DeclaredRemoteAreApplicationExceptions.
    private static final boolean DRAAE = false;

    private static void assertEquals(Object[] a, Object[] b) {
        Assert.assertEquals(a == null ? null : Arrays.asList(a),
                            b == null ? null : Arrays.asList(b));
    }

    private static Class<?>[] getCheckedExceptions(Class<?> c,
                                                   String m,
                                                   boolean isRmiRemote,
                                                   DeploymentUtil.DeploymentTarget target,
                                                   EJBWrapperType wrapperType,
                                                   boolean declaredUncheckedAreSystemExceptions,
                                                   boolean declaredRemoteAreApplicationExceptions) throws EJBConfigurationException {
        try {
            return DeploymentUtil.getCheckedExceptions(c.getDeclaredMethod(m), isRmiRemote, target, wrapperType,
                                                       declaredUncheckedAreSystemExceptions,
                                                       declaredRemoteAreApplicationExceptions);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError(ex);
        }
    }

    private static Class<?>[] getCheckedExceptions(Class<?> c, String m, boolean isRmiRemote, DeploymentUtil.DeploymentTarget target) throws EJBConfigurationException {
        return getCheckedExceptions(c, m, isRmiRemote, target, null, DUASE, DRAAE);
    }

    private static void assertExceptionGetCheckedExceptions(Class<?> c,
                                                            String m,
                                                            boolean isRmiRemote,
                                                            DeploymentUtil.DeploymentTarget target,
                                                            EJBWrapperType wrapperType,
                                                            boolean declaredUncheckedAreSystemExceptions,
                                                            boolean declaredRemoteAreApplicationExceptions) {
        try {
            getCheckedExceptions(c, m, isRmiRemote, target, wrapperType,
                                 declaredUncheckedAreSystemExceptions,
                                 declaredRemoteAreApplicationExceptions);
            Assert.fail("expected EJBConfigurationException from getCheckedExceptions(" + c.getName() + '.' + m + "(), " + isRmiRemote + ", " + target + ')');
        } catch (EJBConfigurationException ex) {
            // Nothing.
        }
    }

    private static void assertExceptionGetCheckedExceptions(Class<?> c, String m, boolean isRmiRemote, DeploymentUtil.DeploymentTarget target) {
        assertExceptionGetCheckedExceptions(c, m, isRmiRemote, target, null, DUASE, DRAAE);
    }

    @Test
    public void testGetCheckedExceptions() throws Exception {
        final Class<?> c = TestGetCheckedExceptions.class;

        assertEquals(new Class<?>[0], getCheckedExceptions(c, "throwsNone", false, WRAPPER));
        assertEquals(new Class<?>[0], getCheckedExceptions(c, "throwsNone", false, STUB));
        assertEquals(new Class<?>[0], getCheckedExceptions(c, "throwsNone", false, TIE));

        // JIT_MISSING_REMOTE_EX_CNTR5104E
        assertExceptionGetCheckedExceptions(c, "throwsNone", true, WRAPPER);
        assertExceptionGetCheckedExceptions(c, "throwsNone", true, STUB);
        assertExceptionGetCheckedExceptions(c, "throwsNone", true, TIE);

        // JIT_INVALID_THROW_REMOTE_CNTR5101W
        assertEquals(new Class<?>[0], getCheckedExceptions(c, "throwsRemoteException", false, WRAPPER));
        assertEquals(new Class<?>[0], getCheckedExceptions(c, "throwsRemoteException", false, STUB));
        assertEquals(new Class<?>[0], getCheckedExceptions(c, "throwsRemoteException", false, TIE));

        assertEquals(new Class<?>[0], getCheckedExceptions(c, "throwsRemoteException", true, WRAPPER));
        assertEquals(new Class<?>[0], getCheckedExceptions(c, "throwsRemoteException", true, STUB));
        assertEquals(new Class<?>[0], getCheckedExceptions(c, "throwsRemoteException", true, TIE));

        // JIT_INVALID_SUBCLASS_REMOTE_EX_CNTR5102E
        assertExceptionGetCheckedExceptions(c, "throwsRemoteEx", false, WRAPPER);
        assertEquals(new Class<?>[] { TestRemoteEx.class }, getCheckedExceptions(c, "throwsRemoteEx", false, STUB));
        assertExceptionGetCheckedExceptions(c, "throwsRemoteEx", false, TIE);

        // JIT_INVALID_SUBCLASS_REMOTE_EX_CNTR5102E
        assertExceptionGetCheckedExceptions(c, "throwsRemoteEx", true, WRAPPER);
        // JIT_MISSING_REMOTE_EX_CNTR5104E
        assertExceptionGetCheckedExceptions(c, "throwsRemoteEx", true, STUB);
        // JIT_INVALID_SUBCLASS_REMOTE_EX_CNTR5102E
        assertExceptionGetCheckedExceptions(c, "throwsRemoteEx", true, TIE);

        for (boolean isRmiRemote : new boolean[] { false, true }) {
            if (!isRmiRemote) {
                // JIT_INVALID_SUBCLASS_REMOTE_EX_CNTR5102E
                assertExceptionGetCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER);
                assertEquals(new Class<?>[] { TestRemoteEx.class }, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, STUB));
                assertExceptionGetCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, TIE);
            } else {
                // RemoteException subclasses ignored for RMI Remote interfaces (included on STUB only)
                assertEquals(new Class<?>[] {}, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER));
                assertEquals(new Class<?>[] { TestRemoteEx.class }, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, STUB));
                assertEquals(new Class<?>[] {}, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, TIE));
            }

            assertEquals(new Class<?>[] { TestRemoteEx.class }, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER, null, DUASE, true));
            assertEquals(new Class<?>[] { TestRemoteEx.class }, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, STUB, null, DUASE, true));
            assertEquals(new Class<?>[] { TestRemoteEx.class }, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, TIE, null, DUASE, true));

            if (!isRmiRemote) {
                // JIT_INVALID_SUBCLASS_REMOTE_EX_CNTR5102E
                assertExceptionGetCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER, EJBWrapperType.BUSINESS_REMOTE, DUASE, DRAAE);
                assertEquals(new Class<?>[] { TestRemoteEx.class },
                             getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, STUB, EJBWrapperType.BUSINESS_REMOTE, DUASE, DRAAE));
                assertExceptionGetCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, TIE, EJBWrapperType.BUSINESS_REMOTE, DUASE, DRAAE);
            } else {
                // RemoteException subclasses ignored for RMI Remote interfaces (included on STUB only)
                assertEquals(new Class<?>[] {}, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER, EJBWrapperType.BUSINESS_REMOTE, DUASE, DRAAE));
                assertEquals(new Class<?>[] { TestRemoteEx.class },
                             getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, STUB, EJBWrapperType.BUSINESS_REMOTE, DUASE, DRAAE));
                assertEquals(new Class<?>[] {}, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, TIE, EJBWrapperType.BUSINESS_REMOTE, DUASE, DRAAE));
            }

            assertEquals(new Class<?>[] { TestRemoteEx.class },
                         getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER, EJBWrapperType.BUSINESS_REMOTE, DUASE, true));
            assertEquals(new Class<?>[] { TestRemoteEx.class },
                         getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, STUB, EJBWrapperType.BUSINESS_REMOTE, DUASE, true));
            assertEquals(new Class<?>[] { TestRemoteEx.class },
                         getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, TIE, EJBWrapperType.BUSINESS_REMOTE, DUASE, true));

            if (!isRmiRemote) {
                // JIT_INVALID_SUBCLASS_REMOTE_EX_CNTR5102E
                assertExceptionGetCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER, EJBWrapperType.BUSINESS_LOCAL, DUASE, DRAAE);
                assertEquals(new Class<?>[] { TestRemoteEx.class },
                             getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER, EJBWrapperType.BUSINESS_LOCAL, DUASE, true));
            }

            // For non-BUSINESS_REMOTE, DeclaredRemoteAreApplicationExceptions is
            // ignored for checking RemoteException.
            for (EJBWrapperType wrapperType : EJBWrapperType.values()) {
                if (wrapperType != EJBWrapperType.BUSINESS_REMOTE &&
                    wrapperType != EJBWrapperType.BUSINESS_LOCAL) {
                    // RemoteException is always ignored for 2.x remote and local views
                    if (!isRmiRemote && wrapperType != EJBWrapperType.LOCAL && wrapperType != EJBWrapperType.LOCAL_HOME) {
                        // JIT_INVALID_SUBCLASS_REMOTE_EX_CNTR5102E
                        assertExceptionGetCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER, wrapperType, DUASE, DRAAE);
                        assertEquals(new Class<?>[] { TestRemoteEx.class },
                                     getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, STUB, wrapperType, DUASE, DRAAE));
                        assertExceptionGetCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, TIE, wrapperType, DUASE, DRAAE);

                        // JIT_INVALID_SUBCLASS_REMOTE_EX_CNTR5102E
                        assertExceptionGetCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER, wrapperType, DUASE, true);
                        assertEquals(new Class<?>[] { TestRemoteEx.class }, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, STUB, wrapperType, DUASE, true));
                        assertExceptionGetCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, TIE, wrapperType, DUASE, true);
                    } else {
                        // RemoteException subclasses ignored for RMI Remote interfaces (included on STUB only)
                        assertEquals(new Class<?>[] {}, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER, wrapperType, DUASE, DRAAE));
                        assertEquals(new Class<?>[] { TestRemoteEx.class },
                                     getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, STUB, wrapperType, DUASE, DRAAE));
                        assertEquals(new Class<?>[] {}, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, TIE, wrapperType, DUASE, DRAAE));

                        // RemoteException subclasses ignored for RMI Remote interfaces (included on STUB only)
                        assertEquals(new Class<?>[] {}, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, WRAPPER, wrapperType, DUASE, true));
                        assertEquals(new Class<?>[] { TestRemoteEx.class }, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, STUB, wrapperType, DUASE, true));
                        assertEquals(new Class<?>[] {}, getCheckedExceptions(c, "throwsRemoteException_RemoteEx", isRmiRemote, TIE, wrapperType, DUASE, true));
                    }
                }
            }
        }

        assertEquals(new Class<?>[] { TestEx1.class }, getCheckedExceptions(c, "throwsEx1", false, WRAPPER));
        assertEquals(new Class<?>[] { TestEx1.class }, getCheckedExceptions(c, "throwsEx1", false, STUB));
        assertEquals(new Class<?>[] { TestEx1.class }, getCheckedExceptions(c, "throwsEx1", false, TIE));

        assertEquals(new Class<?>[] { TestEx1.class, TestEx2.class }, getCheckedExceptions(c, "throwsEx1_Ex2", false, WRAPPER));
        assertEquals(new Class<?>[] { TestEx1.class, TestEx2.class }, getCheckedExceptions(c, "throwsEx1_Ex2", false, STUB));
        assertEquals(new Class<?>[] { TestEx1.class, TestEx2.class }, getCheckedExceptions(c, "throwsEx1_Ex2", false, TIE));

        for (String m : new String[] {
                                       "throwsEx1_ExSub1",
                                       "throwsExSub1_Ex1"
        }) {
            assertEquals(new Class<?>[] { TestEx1.class }, getCheckedExceptions(c, m, false, WRAPPER));
            assertEquals(new Class<?>[] { TestExSub1.class, TestEx1.class }, getCheckedExceptions(c, m, false, STUB));
            assertEquals(new Class<?>[] { TestExSub1.class, TestEx1.class }, getCheckedExceptions(c, m, false, TIE));
        }

        for (String m : new String[] {
                                       "throwsEx1_ExSub1_ExSubSub1",
                                       "throwsExSub1_Ex1_ExSubSub1",
                                       "throwsExSubSub1_ExSub1_Ex1",
                                       "throwsExSubSub1_Ex1_ExSub1"
        }) {
            assertEquals(new Class<?>[] { TestEx1.class }, getCheckedExceptions(c, m, false, WRAPPER));
            assertEquals(new Class<?>[] { TestExSubSub1.class, TestExSub1.class, TestEx1.class }, getCheckedExceptions(c, m, false, STUB));
            assertEquals(new Class<?>[] { TestExSubSub1.class, TestExSub1.class, TestEx1.class }, getCheckedExceptions(c, m, false, TIE));
        }

        assertEquals(new Class<?>[] { Exception.class }, getCheckedExceptions(c, "throwsException", false, WRAPPER));
        assertEquals(new Class<?>[] { Exception.class }, getCheckedExceptions(c, "throwsException", false, STUB));
        assertEquals(new Class<?>[] { Exception.class }, getCheckedExceptions(c, "throwsException", false, TIE));

        // ??? JIT_MISSING_REMOTE_EX_CNTR5104E - rmic accepts this, now JITDeploy does too
        assertEquals(new Class<?>[] { Exception.class }, getCheckedExceptions(c, "throwsException", true, WRAPPER));
        assertEquals(new Class<?>[] { Exception.class }, getCheckedExceptions(c, "throwsException", true, STUB));
        assertEquals(new Class<?>[] { Exception.class }, getCheckedExceptions(c, "throwsException", true, TIE));

        // JIT_INVALID_NOT_EXCEPTION_SUBCLASS_CNTR5107E
        assertExceptionGetCheckedExceptions(c, "throwsThrowable", false, WRAPPER);
        assertExceptionGetCheckedExceptions(c, "throwsThrowable", false, STUB);
        assertExceptionGetCheckedExceptions(c, "throwsThrowable", false, TIE);

        // ??? JIT_MISSING_REMOTE_EX_CNTR5104E - rmic accepts this
        //    assertEquals(new Class<?>[] { Throwable.class }, getCheckedExceptions(c, "throwsThrowable", true, WRAPPER));
        //    assertEquals(new Class<?>[] { Throwable.class }, getCheckedExceptions(c, "throwsThrowable", true, STUB));
        //    assertEquals(new Class<?>[] { Throwable.class }, getCheckedExceptions(c, "throwsThrowable", true, TIE));

        for (Class<?> exClass : new Class<?>[] {
                                                 RuntimeException.class,
                                                 TestRuntimeEx.class,
                                                 Error.class,
                                                 TestErr.class,
        }) {
            String simpleName = exClass.getSimpleName();
            if (simpleName.startsWith("Test")) {
                simpleName = simpleName.substring(4);
            }
            String m = "throws" + simpleName;

            assertEquals(new Class<?>[0], getCheckedExceptions(c, m, false, WRAPPER));
            assertEquals(new Class<?>[] { exClass }, getCheckedExceptions(c, m, false, STUB));
            assertEquals(new Class<?>[0], getCheckedExceptions(c, m, false, TIE));

            assertEquals(new Class<?>[] { exClass }, getCheckedExceptions(c, m, false, WRAPPER, null, false, DRAAE));
            assertEquals(new Class<?>[] { exClass }, getCheckedExceptions(c, m, false, STUB, null, false, DRAAE));
            assertEquals(new Class<?>[0], getCheckedExceptions(c, m, false, TIE, null, false, DRAAE));
        }
    }

    public static interface TestGetCheckedExceptions {
        void throwsNone();

        void throwsRemoteException() throws RemoteException;

        void throwsRemoteEx() throws TestRemoteEx;

        void throwsRemoteException_RemoteEx() throws RemoteException, TestRemoteEx;

        void throwsEx1() throws TestEx1;

        void throwsEx1_Ex2() throws TestEx1, TestEx2;

        void throwsEx1_ExSub1() throws TestEx1, TestExSub1;

        void throwsExSub1_Ex1() throws TestExSub1, TestEx1;

        void throwsEx1_ExSub1_ExSubSub1() throws TestEx1, TestExSub1, TestExSubSub1;

        void throwsExSub1_Ex1_ExSubSub1() throws TestEx1, TestExSub1, TestExSubSub1;

        void throwsExSubSub1_ExSub1_Ex1() throws TestEx1, TestExSub1, TestExSubSub1;

        void throwsExSubSub1_Ex1_ExSub1() throws TestEx1, TestExSub1, TestExSubSub1;

        void throwsException() throws Exception;

        void throwsThrowable() throws Throwable;

        void throwsThr() throws TestThr;

        void throwsRuntimeException() throws RuntimeException;

        void throwsRuntimeEx() throws TestRuntimeEx;

        void throwsError() throws Error;

        void throwsErr() throws TestErr;
    }

    @SuppressWarnings("serial")
    public static class TestRemoteEx extends RemoteException {
        // Nothing
    }

    @SuppressWarnings("serial")
    public static class TestEx1 extends Exception {
        // Nothing
    }

    @SuppressWarnings("serial")
    public static class TestExSub1 extends TestEx1 {
        // Nothing
    }

    @SuppressWarnings("serial")
    public static class TestExSubSub1 extends TestExSub1 {
        // Nothing
    }

    @SuppressWarnings("serial")
    public static class TestEx2 extends Exception {
        // Nothing
    }

    @SuppressWarnings("serial")
    public static class TestRuntimeEx extends RuntimeException {
        // Nothing
    }

    @SuppressWarnings("serial")
    public static class TestErr extends Error {
        // Nothing
    }

    @SuppressWarnings("serial")
    public static class TestThr extends Throwable {
        // Nothing
    }
}
