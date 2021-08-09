/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;

import javax.rmi.CORBA.Tie;

import junit.framework.Assert;

import org.junit.Test;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.CORBA.portable.UnknownException;

import com.ibm.ws.ejbcontainer.jitdeploy._underscore.TestUnderScoreException;
import com.ibm.ws.ejbcontainer.jitdeploy.lowercase.exception.TestLowerCaseException;
import com.ibm.ws.ejbcontainer.jitdeploy.uppercase.EXCEPTION.TestUpperCaseException;

public abstract class AbstractTieTestBase
                extends AbstractRMITestBase
{
    protected abstract Class<?> defineTieClass(Class<?> targetClass, Class<?> remoteInterface, int rmicCompatible, TestClassLoader loader);

    protected Tie createTie(Class<? extends Remote> targetClass, Class<?> remoteInterface, int rmicCompatible)
    {
        Class<?> klass = defineTieClass(targetClass, remoteInterface, rmicCompatible, new TestClassLoader());
        Class<? extends Tie> tieClass = klass.asSubclass(Tie.class);

        try
        {
            return tieClass.newInstance();
        } catch (IllegalAccessException ex)
        {
            throw new IllegalStateException(ex);
        } catch (InstantiationException ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    protected void invoke(Class<? extends Remote> targetClass, Class<?> remoteInterface, int rmicCompatible, String operation, TestMethodCall... calls)
    {
        Tie tie = createTie(targetClass, remoteInterface, rmicCompatible);
        final TestMethodCalls methodCalls = new TestMethodCalls(null, false, calls);

        try
        {
            tie.setTarget(targetClass.getConstructor(TestMethodCalls.class).newInstance(methodCalls));
        } catch (Exception ex)
        {
            throw new IllegalStateException(ex);
        }

        TestInputStreamImpl in = new TestInputStreamImpl(methodCalls);
        ResponseHandler responseHandler = new ResponseHandler()
        {
            @Override
            public OutputStream createReply()
            {
                return new TestOutputStreamImpl(methodCalls);
            }

            @Override
            public OutputStream createExceptionReply()
            {
                return new TestOutputStreamImpl(methodCalls);
            }
        };

        try
        {
            tie._invoke(operation, in, responseHandler);
        } catch (UnknownException ex)
        {
            if (ex.getCause() == null)
            {
                ex.initCause(ex.originalEx);
            }
            throw ex;
        }
    }

    private static class TargetMethodCall
                    implements TestMethodCall
    {
        private final String ivMethodName;
        private final Object[] ivArgs;

        TargetMethodCall(String methodName, Object[] args)
        {
            ivMethodName = methodName;
            ivArgs = args;
        }

        @Override
        public Object invoke(String methodName, Object[] args)
        {
            Assert.assertEquals(ivMethodName, methodName);
            Assert.assertEquals(Arrays.asList(ivArgs), Arrays.asList(args));
            return null;
        }
    }

    protected TestMethodCall target(String methodName, Object... args)
    {
        return new TargetMethodCall("target@" + methodName, args);
    }

    @Test
    public void testMutableIds()
    {
        for (int rmicCompatible : getRMICCompatible())
        {
            Tie tie = createTie(TestMutableIdsImpl.class, TestMutableIdsIntf.class, rmicCompatible);
            String[] ids = ids(tie);
            String[] idsClone = ids.clone();
            ids[0] = "mutable";
            Assert.assertEquals(Arrays.asList(idsClone), Arrays.asList(ids(tie)));
        }
    }

    protected String[] ids(Tie tie) {
        return ((ObjectImpl) tie)._ids();
    }

    public interface TestMutableIdsIntf
                    extends Remote
    {
        // Nothing.
    }

    public class TestMutableIdsImpl
                    implements TestMutableIdsIntf
    {
        // Nothing.
    }

    @Test
    public void testExceptionMangling()
    {
        for (int rmicCompatible : getRMICCompatible())
        {
            boolean rmicCompatibleExceptions = com.ibm.wsspi.ejbcontainer.JITDeploy.isRMICCompatibleExceptions(rmicCompatible);

            invoke(TestExceptionManglingImpl.class, TestExceptionManglingIntf.class, rmicCompatible,
                   isKeywordOperationMangled() ? "_exception" : "exception",
                   target("exception"));

            invoke(TestExceptionManglingImpl.class, TestExceptionManglingIntf.class, rmicCompatible,
                   "throwException",
                   target("throwException"),
                   write("string", "IDL:java/lang/Ex:1.0"),
                   write("value@2", TestExceptionManglingImpl.EXCEPTION));

            invoke(TestExceptionManglingImpl.class, TestExceptionManglingIntf.class, rmicCompatible,
                   "throwTestLowerCaseException",
                   target("throwTestLowerCaseException"),
                   write("string", rmicCompatibleExceptions ? "IDL:com/ibm/ws/ejbcontainer/jitdeploy/lowercase/_exception/TestLowerCaseEx:1.0" :
                                   "IDL:com/ibm/ws/ejbcontainer/jitdeploy/lowercase/exception/TestLowerCaseEx:1.0"),
                   write("value@2", TestExceptionManglingImpl.LOWER_CASE_EXCEPTION));

            invoke(TestExceptionManglingImpl.class, TestExceptionManglingIntf.class, rmicCompatible,
                   "throwTestUpperCaseException",
                   target("throwTestUpperCaseException"),
                   write("string", rmicCompatibleExceptions ? "IDL:com/ibm/ws/ejbcontainer/jitdeploy/uppercase/_EXCEPTION/TestUpperCaseEx:1.0" :
                                   "IDL:com/ibm/ws/ejbcontainer/jitdeploy/uppercase/EXCEPTION/TestUpperCaseEx:1.0"),
                   write("value@2", TestExceptionManglingImpl.UPPER_CASE_EXCEPTION));

            invoke(TestExceptionManglingImpl.class, TestExceptionManglingIntf.class, rmicCompatible,
                   "throwTestUnderScoreException",
                   target("throwTestUnderScoreException"),
                   write("string", rmicCompatibleExceptions ? "IDL:com/ibm/ws/ejbcontainer/jitdeploy/J_underscore/TestUnderScoreEx:1.0" :
                                   "IDL:com/ibm/ws/ejbcontainer/jitdeploy/_underscore/TestUnderScoreEx:1.0"),
                   write("value@2", TestExceptionManglingImpl.UNDER_SCORE_EXCEPTION));
        }
    }

    public interface TestExceptionManglingIntf
                    extends Remote
    {
        void exception()
                        throws RemoteException;

        void throwException()
                        throws RemoteException, Exception;

        void throwTestLowerCaseException()
                        throws RemoteException, TestLowerCaseException;

        void throwTestUpperCaseException()
                        throws RemoteException, TestUpperCaseException;

        void throwTestUnderScoreException()
                        throws RemoteException, TestUnderScoreException;
    }

    public static class TestExceptionManglingImpl
                    implements TestExceptionManglingIntf
    {
        private final TestMethodCalls ivMethodCalls;

        public TestExceptionManglingImpl(TestMethodCalls methodCalls)
        {
            this.ivMethodCalls = methodCalls;
        }

        @Override
        public void exception()
        {
            ivMethodCalls.invoke("target@exception");
        }

        public static final Exception EXCEPTION = new Exception();

        @Override
        public void throwException()
                        throws Exception
        {
            ivMethodCalls.invoke("target@throwException");
            throw EXCEPTION;
        }

        public static final TestLowerCaseException LOWER_CASE_EXCEPTION = new TestLowerCaseException();

        @Override
        public void throwTestLowerCaseException()
                        throws TestLowerCaseException
        {
            ivMethodCalls.invoke("target@throwTestLowerCaseException");
            throw LOWER_CASE_EXCEPTION;
        }

        public static final TestUpperCaseException UPPER_CASE_EXCEPTION = new TestUpperCaseException();

        @Override
        public void throwTestUpperCaseException()
                        throws TestUpperCaseException
        {
            ivMethodCalls.invoke("target@throwTestUpperCaseException");
            throw UPPER_CASE_EXCEPTION;
        }

        public static final TestUnderScoreException UNDER_SCORE_EXCEPTION = new TestUnderScoreException();

        @Override
        public void throwTestUnderScoreException()
                        throws TestUnderScoreException
        {
            ivMethodCalls.invoke("target@throwTestUnderScoreException");
            throw UNDER_SCORE_EXCEPTION;
        }
    }
}
