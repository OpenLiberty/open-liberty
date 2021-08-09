/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.concurrent.Future;

import javax.ejb.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ejbcontainer.runtime.AbstractEJBRuntime;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * An AsyncMethodWrapper is a Runnable that packages an EJB asynchronous method
 * for execution on an asynchronous thread and makes the results available to
 * the client. It also acts as an EJB wrapper when its "run" method is called
 * (i.e. it wraps the asynchronous method call with preInvoke and postInvoke
 * calls to the container). <p>
 *
 * An AsyncMethodWrapper instance keeps a small amount of state data in order to
 * serve as both a Wrapper and a Runnable object. This is different than most
 * wrappers which keep no state data beyond what is stored in EJSWrapperBase.
 * Thus, unlike other wrappers, AsyncMethodWrapper instances may not be used
 * concurrently by multiple async method requests. <p>
 *
 * An AsyncMethodWrapper is bean type independent so it does not need to be
 * generated in order to include type specific information like many of the
 * other wrapper classes do.
 */
public class AsyncMethodWrapper extends EJSWrapperBase implements Runnable {
    private static final String CLASS_NAME = AsyncMethodWrapper.class.getName();
    private static final TraceComponent tc = Tr.register(AsyncMethodWrapper.class, "EJBContainer", "com.ibm.ejs.container.container");

    // The methodId for the async method that will be invoked
    protected final int ivMethodId;

    // The method arguments passed with the method call
    private Object[] ivArgs = null;

    // The server-side future object. Used to report results from
    // execution of the async method. Null if void return type.
    private ServerAsyncResult ivServerFuture = null;

    //  F743-22763
    // The time the wrapper was initially created and submitted; used to
    // calculate the time the method was on the queue before starting execution.
    private long ivStartTime;

    /**
     * Constructor
     *
     * @param theCallingWrapper -
     *            a pointer to the wrapper object which originally intercepted the
     *            client call. The calling wrapper determined that the method was
     *            asynchronous and sent it to the container for scheduling on a
     *            work manager.
     * @param theMethodId -
     *            the container's internal method ID for the async method that
     *            will be invoked.
     * @param theMethodArgs -
     *            the method parameters for the async method that will be
     *            invoked.
     * @param theServerFuture -
     *            the server-side Future object that results will be set into.
     */
    public AsyncMethodWrapper(EJSWrapperBase theCallingWrapper, int theMethodId, Object[] theMethodArgs, ServerAsyncResult theServerFuture) {
        super();

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init> : " + theCallingWrapper + ", " + theMethodId + ", " + theServerFuture);

        // Initialize superclass (i.e. EJSWrapperBase) with data from the wrapper
        // that was originally called for this asynchronous method.
        super.container = theCallingWrapper.container;
        super.wrapperManager = theCallingWrapper.wrapperManager;
        super.beanId = theCallingWrapper.beanId;
        super.bmd = theCallingWrapper.bmd;
        super.isolationAttrs = theCallingWrapper.isolationAttrs;

        // This wrapper isn't managed, but we copy ivCommon since it holds some
        // extra state for optimizations.                                 F61004.6
        super.ivCommon = theCallingWrapper.ivCommon;
        super.isManagedWrapper = false; // Not a managed wrapper.
        super.ivPmiBean = theCallingWrapper.ivPmiBean;
        super.methodInfos = theCallingWrapper.methodInfos;
        super.methodNames = theCallingWrapper.methodNames;
        super.ivInterface = theCallingWrapper.ivInterface;
        super.ivBusinessInterfaceIndex = theCallingWrapper.ivBusinessInterfaceIndex; // F743-24429

        // We are breaking the rule a little, but this combination runnable and
        // wrapper does need a little bit of state data to do it's job
        ivMethodId = theMethodId;
        ivArgs = theMethodArgs;
        ivServerFuture = theServerFuture;

        if (ivPmiBean != null) {
            ivStartTime = ivPmiBean.initialTime(EJBPMICollaborator.ASYNC_WAIT_TIME);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init> : " + this);
    }

    // --------------------------------------------------------------------------
    //
    // Methods from Runnable interface
    //
    // --------------------------------------------------------------------------

    /**
     * This method is invoked by the asynchronous task service (i.e. WorkManager
     * or Executor) as soon as a thread is available to dispatch the asynchronous
     * method. This Runnable instance interposes calls to EJB Container preInvoke
     * and postInvoke around the actual async method call (i.e. acts as an EJB
     * wrapper) and also takes care of making any results available to the client
     * via the server result future, if applicable.
     */
    @Override
    public void run() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "run : async method " + methodInfos[ivMethodId].getMethodName() + " : " + this);

        // F743-22763
        // Add this methods wait time to the PMI statistics and decrement
        // the queue size if PMI is enabled and startTime was successfully set.
        if ((ivPmiBean != null) && (ivStartTime > 0)) {
            ivPmiBean.finalTime(EJBPMICollaborator.ASYNC_WAIT_TIME, ivStartTime);
            ivPmiBean.asyncQueSizeDecrement();
        }

        // Result from the async method call
        Future<?> theResult = null;

        EJBMethodInfoImpl methodInfo = methodInfos[ivMethodId];
        Method theMethod = methodInfo.ivMethod;

        EJSDeployedSupport s = new EJSDeployedSupport();

        // Provide the server result to the method context to support
        // EJBContext.wasCancelled().
        s.ivAsyncResult = ivServerFuture;

        boolean isVoidReturnType = (theMethod.getReturnType() == Void.TYPE); // F743-22763

        // F743761.CodRv - Void methods do not throw application exceptions.
        s.ivIgnoreApplicationExceptions = isVoidReturnType;

        AbstractEJBRuntime rt = (AbstractEJBRuntime) super.container.getEJBRuntime();

        // If the server is stopping, don't invoke the async method
        if (rt.isStopping()) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Async method " + methodInfo.getBeanClassName() + "." + methodInfo.getMethodName() + " will not be invoked because server is stopping");
        } else {
            try {
                try {
                    Object theBean = container.EjbPreInvoke(this, ivMethodId, s, ivArgs);

                    // If preInvoke did not have an exception, continue on and invoke the
                    // bean's method, using the reflection object or via interceptors.
                    // F743-24429
                    if (methodInfo.getAroundInterceptorProxies() == null) {
                        try {
                            theResult = (Future<?>) theMethod.invoke(theBean, ivArgs); // d650178
                        } catch (InvocationTargetException ite) {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "Caught InvocationTargetException, unwrapping : " + ite);
                            throw ite.getCause(); // PI13514
                        }
                    } else {
                        theResult = (Future<?>) container.invoke(s, (Timer) null); // d650178
                    }

                    // Note: results are not set in the server result future yet,
                    // as there may still be an exception during postInvoke.

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Async method completed successfully");
                } catch (Throwable ex) {
                    // For void methods, metadata validation has already ensured that
                    // the throws clause is empty, so declared will always be false.
                    boolean declared = isApplicationException(ex, methodInfo); // d660332

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Caught Throwable (declared=" + declared + "): " + ex);

                    // Update deployed support so postInvoke will handle the exception.
                    if (declared) {
                        // F743-761 - Simulate synchronous wrappers by calling
                        // setCheckedException for exceptions on the throws clause.
                        s.setCheckedException((Exception) ex);
                        throw ex;
                    }

                    // For void methods, ivIgnoreApplicationExceptions is true, so
                    // setUncheckedLocalException will always log this exception and
                    // then throw EJBException.
                    s.setUncheckedLocalException(ex);
                } finally {
                    try {
                        container.postInvoke(this, ivMethodId, s);
                    } catch (RemoteException re) {
                        // This should never occur, but must be caught, since
                        // RemoteException is on the throws clause of postInvoke.
                        FFDCFilter.processException(re, CLASS_NAME + ".run", "242", this);
                        s.setUncheckedLocalException(re);
                    }
                }
            } catch (Throwable ex) {
                // Unchecked Exceptions will have already been logged with FFDC
                // above, no need to repeat that here.

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Async method completed with exception : " + ex);

                // If there is a server-side future object set the exception there
                // too.  FireAndForget async methods will not have a server-side
                // future object so the exception cannot be reported.
                if (ivServerFuture != null) {
                    // This wrapper class has to act like a stub class. In the remote
                    // code path, the container has already mapped RemoteException to
                    // SystemException for it to be sent to the ORB
                    // (OrbUtilsImpl.mapException). Since the resulting exception will
                    // never go through the ORB directly (we're catching it here, and
                    // RemoteAsyncResultImpl will wrap it in ExecutionException), we
                    // need to do the reverse operation and map from SystemException
                    // back to RemoteException just like a generated stub would do.
                    ex = mapSystemExceptionBackToRemoteException(ex);

                    ivServerFuture.setException(ex);
                }

                // F743-22763
                // For fire and forget methods, if PMI statistics are being collected
                // increment the number of fire and forget methods failed counter
                if ((ivPmiBean != null) && isVoidReturnType) {
                    ivPmiBean.asyncFNFFailed();
                }

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "run : " + ex);
                return;
            }
        }

        // If a server-side future object is present, the async method is
        // supposed to return results. Otherwise, it is a "fire-and-forget"
        // type method, where no results are returned. Note: null is a
        // valid method result so we can't use a result == null check here
        // to identify a "fire-and-forget" type method.
        if (ivServerFuture != null) {
            ivServerFuture.setResult(theResult);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "run");
    }

    /**
     * Determines whether the exception extends an exception should be
     * considered an application exception.
     *
     * @param ex the exception that was caught
     * @return true if the exception is a checked exception
     */
    // F743-761
    private boolean isApplicationException(Throwable ex, EJBMethodInfoImpl methodInfo) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "isApplicationException : " + ex.getClass().getName() + ", " + methodInfo);

        // d660332
        if (ex instanceof Exception && (!ContainerProperties.DeclaredUncheckedAreSystemExceptions || !(ex instanceof RuntimeException))) {
            Class<?>[] declaredExceptions = null;

            // Checks if the method interface is business or component.
            // If it's a component interface, there is only one interface to worry about   // d734957
            if (ivInterface == WrapperInterface.LOCAL || ivInterface == WrapperInterface.REMOTE) {
                declaredExceptions = methodInfo.ivDeclaredExceptionsComp;
            } else {
                // Determine if there were any declared exceptions for the method
                // interface being used. However, if the wrapper is an aggregate
                // wrapper (all interfaces), then just find the first set of
                // non-null declared exceptions. It is possible (thought not likely)
                // this could result in odd behavior, but a warning was logged
                // when the wrapper class was created.                       F743-34304
                if (ivBusinessInterfaceIndex == AGGREGATE_LOCAL_INDEX) {
                    for (Class<?>[] declaredEx : methodInfo.ivDeclaredExceptions) {
                        if (declaredEx != null) {
                            declaredExceptions = declaredEx;
                            break;
                        }
                    }
                } else {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "ivBusinessInterfaceIndex=" + ivBusinessInterfaceIndex);
                    declaredExceptions = methodInfo.ivDeclaredExceptions[ivBusinessInterfaceIndex]; // F743-24429
                }
            }

            if (declaredExceptions != null) {
                for (Class<?> declaredException : declaredExceptions) {
                    if (declaredException.isAssignableFrom(ex.getClass())) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(tc, "isApplicationException : true");
                        return true;
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "isApplicationException : false");

        return false;
    }

    /**
     * Maps a CORBA SystemException back to a RemoteException just like a
     * generated stub would do or returns the original exception if mapping
     * is not required. <p>
     *
     * This method should be overridden in environments that support
     * Remote interfaces. <p>
     */
    protected Throwable mapSystemExceptionBackToRemoteException(Throwable ex) {
        // Remote interfaces are not supported in the core EJBContainer,
        // so no mapping is required by default.
        return ex;
    }
}
