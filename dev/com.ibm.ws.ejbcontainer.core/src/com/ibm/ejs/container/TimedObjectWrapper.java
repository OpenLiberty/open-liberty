/*******************************************************************************
 * Copyright (c) 2003, 2014 IBM Corporation and others.
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
import java.rmi.RemoteException;

import javax.ejb.TimedObject;
import javax.ejb.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A <code>TimedObjectWrapper</code> wraps a TimedObject and interposes
 * calls to the container before and after every method call on the
 * EJB (i.e ejbTimeout or timeout method). <p>
 *
 * The <code>TimedObjectWrapper</code> is designed to contain a minimum amount
 * of state. No additional state is required beyond the state defined in
 * EJSWrapperBase. Note, it does not directly maintain a reference to the EJB.
 * It relies on the container to supply it with the appropriate EJB. <p>
 *
 * Unlike Remote and Local wrappers, there is no deployed/generated subclass
 * for the TimedObjectWrapper. The TimedObject interface is the same accross
 * all timed objects, so there is no need for bean specific code generation,
 * though reflection is used for @Timeout methods. <p>
 *
 * Since the TimedObjectWrapper implementation is bean type independent,
 * is never accessed outside of EJB Container, and is only in use for the
 * duration of the ejbTimeout method invocation, instances of TimedObjectWrapper
 * may be pooled, and re-used for any TimedObject bean type. Only the bean
 * specific state data must be reset. <p>
 **/

public final class TimedObjectWrapper
                extends EJSWrapperBase
{
    private static final TraceComponent tc =
                    Tr.register(TimedObjectWrapper.class, "EJBContainer",
                                "com.ibm.ejs.container.container");

    private static final String CLASS_NAME = "com.ibm.ejs.container.TimedObjectWrapper";

    /**
     * Invoked by TimerHandler or TimerNpListener upon timer expiration.
     * Interposes calls to the EJB Container preInvoke and postInvoke around the
     * actual method invocation (ejbTimeout, @Timeout, or @Schedule). <p>
     *
     * This code is essentially the same as the deployed code for a local
     * wrapper. Since it is not performance critical, reflection is used to
     * invoke non-ejbTimeout methods rather than generating a wrapper. <p>
     *
     * @param timer Timer whose expiration caused this notification.
     * @param methodId The method to invoke. If this is an ejbTimeout or
     * @Timeout timer, then the method id will be 0.
     * @param persistentGlobalTx true if the timer callback is for a persistent timer
     *            that will run in the scope of a global transaction.
     */
    public void invokeCallback(Timer timer, int methodId, boolean persistentGlobalTx)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "invokeCallback: " + timer);

        EJSDeployedSupport s = new EJSDeployedSupport();
        s.isPersistentTimeoutGlobalTx = persistentGlobalTx; // RTC126471
        try
        {
            // The 'TimedObject' wrapper now uses the EJB 3.0 style preInvoke,
            // to support POJOs that implement TimedObject.               d438133.1

            EJBMethodInfoImpl methodInfo = methodInfos[methodId];
            Object[] args = (methodInfo.ivNumberOfMethodParms == 1) ? new Object[] { timer } : new Object[0]; //F743-15870
            Object timedObj = container.EjbPreInvoke(this, methodId, s, args); // F743-506

            if (methodInfo.getAroundInterceptorProxies() != null) // F743-17763, F743-17763.1
            {
                container.invoke(s, timer);
            }
            else
            {
                // If the EJB implements TimedObject, then the 'ejbTimeout' method is
                // invoked directly (better performance); otherwise, reflections is
                // used to call the user specified 'timeout' method.          d438133.2
                // F743-506 - This optimization is only relevant for programmatically
                // created timers.
                if (methodId == 0 && bmd.isTimedObject && bmd.ivTimeoutMethod == null)
                {
                    ((TimedObject) timedObj).ejbTimeout(timer);
                }
                else
                {
                    s.methodInfo.ivMethod.invoke(timedObj, args);
                }
            }
        } catch (InvocationTargetException itex)
        {
            // All exceptions are considered Unchecked Exceptions
            Throwable targetEx = itex.getCause();
            if (targetEx == null)
                targetEx = itex;
            s.setUncheckedLocalException(targetEx);
        } catch (Throwable ex)
        {
            // All exceptions are considered Unchecked Exceptions
            FFDCFilter.processException(ex, CLASS_NAME + ".invokeCallback", "83", this);
            s.setUncheckedLocalException(ex);
        } finally
        {
            try
            {
                container.postInvoke(this, methodId, s); // F743-506
            } catch (RemoteException rex)
            {
                // This should never occur, but must be caught, since
                // RemoteException is on the throws clause of postInvoke.
                FFDCFilter.processException(rex, CLASS_NAME + ".invokeCallback",
                                            "91", this);
                s.setUncheckedLocalException(rex);
            } finally
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "invokeCallback");
            }
        }
        return;
    }

} // TimedObjectWrapper
