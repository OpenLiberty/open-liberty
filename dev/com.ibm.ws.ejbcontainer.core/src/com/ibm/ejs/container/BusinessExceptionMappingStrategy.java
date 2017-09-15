/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

import javax.ejb.ConcurrentAccessException;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.NoSuchEJBException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.TransactionRequiredException;
import javax.transaction.TransactionRolledbackException;

import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.csi.CSIAccessException;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSIInvalidTransactionException;
import com.ibm.websphere.csi.CSINoSuchObjectException;
import com.ibm.websphere.csi.CSITransactionRequiredException;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.ExceptionType;
import com.ibm.websphere.ejbcontainer.EJBStoppedException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * The BusinessExceptionMappingStrategy is utilized by the EJSDeployedSupport
 * class for mapping exceptions to be thrown for method invocations involving
 * the EJB 3.0 Business interfaces, except the Remote Business interfaces
 * that implement java.rmi.Remote. <p>
 *
 * The Remote Business interfaces that implement java.rmi.Remote will use
 * the RemoteExceptionMappingStrategy, just like the Remote Component
 * interfaces. <p>
 **/
public class BusinessExceptionMappingStrategy extends ExceptionMappingStrategy
{
    private static final String CLASS_NAME = BusinessExceptionMappingStrategy.class.getName();
    private static final TraceComponent tc = Tr.register(BusinessExceptionMappingStrategy.class,
                                                         "EJBContainer", "com.ibm.ejs.container.container");
    static final ExceptionMappingStrategy INSTANCE = new BusinessExceptionMappingStrategy(); // F61004.3

    /**
     * Construct a BusinessExceptionMappingStrategy.
     **/
    private BusinessExceptionMappingStrategy()
    {
        // Private.
    }

    /**
     * Map a CSIException to the appropriate EJBException. <p>
     *
     * CSIException is an internal Container exception and a RemoteException,
     * so should not be exposed to the customer. This method will convert a
     * CSIException subclass to its corresponding EJBException or to a plain
     * EJBException. <p>
     *
     * The CSIException will NOT appear in the returned EJBException message
     * text, nor in the cause/nested exception stack. <p>
     *
     * The stack of the returned EJBException will be empty if there is a cause
     * exception, or that of the CSIException if there is no cause exception. <p>
     *
     * @param ex The CSIException to be mapped to an EJBException.
     * @param causeEx The exception to be set as the root/cause of the
     *            returned EJBException. Should NOT be a CSIException.
     * @param cause The real cause exception, which should only be different
     *            from causeEx if it is NOT a subclass of Exception.
     *
     * @return The mapped EJBException.
     **/
    private EJBException mapCSIException(CSIException ex,
                                         Exception causeEx,
                                         Throwable cause)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "mapCSIException: " + ex + ", cause = " + cause);

        EJBException ejbex;
        boolean createdEx = true;

        // -----------------------------------------------------------------------
        // First, obtain the message text used to create the CSIException.
        //
        // CSIException is a RemoteException, which contains a 'detail' field
        // that holds the 'cause', instead of the normal cause defined by
        // Throwable.  When detail is set, getMessage() will include the text
        // and stack of the detail, which ends up duplicating the message text.
        // To avoid this, 'detail' is cleared, resulting in getMessage()
        // returning only the message text specified when the CSIException was
        // created.
        //
        // Also, toString() must be avoided, as it would include the name of
        // the CSIException in the message text.
        // -----------------------------------------------------------------------
        String message = ExceptionUtil.getBaseMessage(ex);

        // -----------------------------------------------------------------------
        // Second, map the CSIException to its corresponding EJBException.
        //
        // If there isn't a specific/corresponding EJBException, then either
        // the cause will be returned (if it is an EJBException), or a plain
        // EJBException will be created with the nested cause.
        // -----------------------------------------------------------------------
        if (ex instanceof CSITransactionRolledbackException)
        {
            ejbex = new EJBTransactionRolledbackException(message, causeEx); // d396839
        }
        else if (ex instanceof CSIAccessException)
        {
            ejbex = new EJBAccessException(message); // d396839
        }
        else if (ex instanceof CSIInvalidTransactionException)
        {
            ejbex = new InvalidTransactionLocalException(message, causeEx);
        }
        else if (ex instanceof CSINoSuchObjectException)
        {
            ejbex = new NoSuchEJBException(message, causeEx);
        }
        else if (ex instanceof CSITransactionRequiredException)
        {
            ejbex = new EJBTransactionRequiredException(message); // d396839
        }
        //        else if (ex instanceof CSIInvalidActivityException)
//        {
//            ejbex = new InvalidActivityLocalException(message, causeEx);
//        }
//        else if (ex instanceof CSIActivityRequiredException)
//        {
//            ejbex = new ActivityRequiredLocalException(message, causeEx);
//        }
//        else if (ex instanceof CSIActivityCompletedException)
//        {
//            ejbex = new ActivityCompletedLocalException(message, causeEx);
//        }
        else
        {
            // This will just return the cause, if it is already an EJBException,
            // and will otherwise create a new EJBException and take care of
            // setting the cause on Throwable and the stack.
            ejbex = ExceptionUtil.EJBException(message, causeEx);

            // If there was a cause, then either a new EJBException was not
            // created, or the cause and stack have already been filled in.
            if (causeEx != null)
                createdEx = false;
        }

        // -----------------------------------------------------------------------
        // Finally, set the EJBException stack to something meaningful.
        //
        // If the returned EJBException was created in this mapping code, then
        // the exception stack pointing at this code is generally misleading,
        // and for sure not helpful.... and may cause very similar stacks to be
        // printed out / logged twice.
        //
        // If the EJBException is just 'replacing' a CSIException, with no
        // real 'cause', then set the stack to that of the CSIException that
        // is being replaced, so at least we know which EJB Container code
        // decided an exception was necessary.
        //
        // If the EJBException does have a 'root/cause', then just clear
        // the exception stack, so debug efforts will focus on the cause
        // of the problem, and not EJB Container's reporting of it.
        //
        // And, since EJBException doesn't normally set the cause on
        // Throwable, let's do that to be nice :-)
        // -----------------------------------------------------------------------
        if (createdEx)
        {
            if (causeEx == null)
            {
                ejbex.setStackTrace(ex.getStackTrace());
            }
            else
            {
                // Geronimo EJBException.getCause returns getCausedbyException, so
                // we do not expect this code to be used.                     F53643
                if (ejbex.getCause() == null) { //F743-16279
                    // Use the 'real' cause here, not the possibly wrapped one.
                    ejbex.initCause(cause);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "mapCSIException returning: " + ejbex);

        return ejbex;
    }

    /**
     * Map the exception to an EJBException exception for the EJB 3.0 Business
     * interfaces. This is the exception that will be thrown to the client. <p>
     *
     * All fun and games in order to comply with both the Java EE spec. <p>
     */
    private EJBException mapException(Throwable ex)
    {
        EJBException ejbex;
        boolean createdEx = true;
        Exception causeEx = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "mapException: " + ex);

        // -----------------------------------------------------------------------
        // First, obtain the message text used to create the exception.
        //
        // If the message being mapped ends up being 'replaced', then the
        // replacement should have the same message text.
        //
        // getBaseMessage insures that the message text does NOT include the
        // name of the exception class, nor any chained exception text.
        // -----------------------------------------------------------------------
        String message = ExceptionUtil.getBaseMessage(ex);

        // -----------------------------------------------------------------------
        // Second, get the cause of the exception being mapped.
        //
        // findCause will remove any internal EJB Container exceptions from
        // the exception chain.
        //
        // Also, if the cause is not a subclass of Throwable, then it must
        // be 'wrapped' in an Exception, as EJBException does not allow
        // Throwable on any of its constructors.  However, the 'cause' on
        // throwable will still be set to the intended cause.  This will allow
        // getCause() to work as expected, even though EJBException does not
        // support Throwable.
        // -----------------------------------------------------------------------
        Throwable cause = ExceptionUtil.findCause(ex);

        if (cause != null)
        {
            if (cause instanceof Exception)
            {
                causeEx = (Exception) cause;
            }
            else
            {
                causeEx = new Exception("See nested Throwable", cause);
            }
        }

        // -----------------------------------------------------------------------
        // Next, map Throwable to an EJBException to return.
        //
        // These mappings are prescribed by the EJB & ActivitySession specs.
        //
        // Note: When adding new mappings, do NOT call 'initCause', as that
        //       will be performed below, and handles the fact that EJBException
        //       may not be created with a throwable.
        //
        // Caution: Do NOT look at the cause to perform mapping. If an exception
        //          is 'wrapped' in another exception, it likely was wrapped
        //          by another EJB method call, and a failure from a downstream
        //          method call should NOT be treated the same here.
        //          If it appears the cause needs to be examined, then instead,
        //          find the code that wrapped the cause, and stop wrapping it.
        // -----------------------------------------------------------------------
        if (ex instanceof CSIException)
        {
            ejbex = mapCSIException((CSIException) ex, causeEx, cause);
            createdEx = false;
        }
        else if (ex instanceof NoSuchObjectException)
        {
            ejbex = new NoSuchEJBException(message, causeEx);
        }
        else if (ex instanceof EJBStoppedException) // d350987
        {
            // Don't just replace the EJBStoppedException, but nest it as the
            // cause so applications may check for it.
            cause = ex;
            causeEx = (Exception) ex;
            ejbex = new NoSuchEJBException((String) null, causeEx);
        }
        else if (ex instanceof TransactionRequiredException)
        {
            ejbex = new EJBTransactionRequiredException(message); // d395666
        }
        else if (ex instanceof TransactionRolledbackException)
        {
            ejbex = new EJBTransactionRolledbackException(message, causeEx); // d395666
        }
        else if (ex instanceof InvalidTransactionException)
        {
            ejbex = new InvalidTransactionLocalException(message, causeEx);
        }
        else if (ex instanceof AccessException)
        {
            ejbex = new EJBAccessException(message); // d395666
        }
        //        else if (ex instanceof ActivityRequiredException)
//        {
//            ejbex = new ActivityRequiredLocalException(message, causeEx);
//        }
//        else if (ex instanceof InvalidActivityException)
//        {
//            ejbex = new InvalidActivityLocalException(message, causeEx);
//        }
//        else if (ex instanceof ActivityCompletedException)
//        {
//            ejbex = new ActivityCompletedLocalException(message, causeEx);
//        }
        else if (ex instanceof BeanNotReentrantException)
        {
            BeanNotReentrantException bnre = (BeanNotReentrantException) ex;
            if (bnre.isTimeout())
            {
                // EJB 3.1 has defined an exception for this purpose.      d653777.1
                ejbex = new ConcurrentAccessTimeoutException(message);
            }
            else
            {
                // EJB 3.0 has defined an exception for this purpose.      d366807.6
                ejbex = new ConcurrentAccessException(message, causeEx);
            }
        }
        else
        {
            // --------------------------------------------------------------------
            // All other exceptions are just be mapped to an EJBException.
            //
            // If the exception is an EJBException, it will just be returned
            // (with the throwable cause filled in), else the exception will
            // just be wrapped in an EJBException, with the stack and cause
            // set appropriately.
            // --------------------------------------------------------------------
            ejbex = ExceptionUtil.EJBException(ex);
            createdEx = false;
        }

        // -----------------------------------------------------------------------
        // Finally, EJBException doesn't normally set the cause on Throwable,
        // so let's do that to be nice :-) and clear the stack of the
        // EJBException, as it is mostly confusing and causes very
        // similar stacks to be printed out twice.
        //
        // Note that this code, especially clearing of the stack, is only
        // performed for EJBExceptions that were created in this method.
        // EJBExceptions that will just be propagated should NOT have the
        // stack cleared, as they were likely thrown by the customer.
        // -----------------------------------------------------------------------
        if (createdEx)
        {
            if (causeEx != null)
            {
                // Intentionally, the 'cause' is set as the 'cause', and not
                // 'causeEx'.  They will be the same, except when 'causeEx'
                // is just a silly wrapper around a Throwable.
                //
                // Geronimo EJBException.getCause returns getCausedbyException, so
                // we do not expect this code to be used.                     F53643
                if (ejbex.getCause() == null) { //F743-16279
                    // Use the 'real' cause here, not the possibly wrapped one.
                    ejbex.initCause(cause);
                }
            }
            else
            {
                ejbex.setStackTrace(ex.getStackTrace());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "mapException returning: " + ejbex);

        return ejbex;
    }

    /**
     * Capture an unchecked exception thrown by a bean method and
     * reraise it as an EJBException. <p>
     *
     * An unchecked exception is a runtime exception or one that is not
     * declared in the signature of the bean method. <p>
     *
     * @param ex the <code>Exception</code> thrown by bean method <p>
     */
    @Override
    public final Throwable setUncheckedException(EJSDeployedSupport s, Throwable ex)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setUncheckedException:" + ex);

        // d395666 start
        // Entering this method for a second time?
        if (s.ivException != null)
        {
            // Yep, this is the second time, so we will just keep the original exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            {
                Tr.event(tc, "setting unchecked exception again", ex);
                Tr.event(tc, "original exception", s.ivException);
            }
            return s.ivException;
        }

        // Nope, this is first time this method entered.
        // Did a non-RemoteException occur?
        Boolean applicationExceptionRollback = null;
        if (ex instanceof Exception && // d423621
            !(ex instanceof RemoteException))
        {
            // Yep, check whether it is annotated as an ApplicationException.
            applicationExceptionRollback = s.getApplicationExceptionRollback(ex);
        }

        // Did above code result in determing Throwable is a an ApplicationException?
        if (applicationExceptionRollback != null)
        {
            // Yep, it is an AppplicationException, so just store it
            // in ivException so that it gets rethrown.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "ApplicationException with rollback set to true, changing to a checked exception", ex);

            // Set exceptiion type to checked exception since it is an ApplicationException.
            s.exType = ExceptionType.CHECKED_EXCEPTION;
            s.ivException = ex;
            s.rootEx = ex;

            // Is rollback set to true for the ApplicationException?
            if (applicationExceptionRollback == Boolean.TRUE)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "ApplicationException with rollback set to true, setting rollback only", ex);

                s.currentTx.setRollbackOnly();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "setUncheckedException returning: " + s.ivException);

            return s.ivException;
        }
        // d395666 end

        // Not a EJB 3 ApplicationException, so do pre-EJB 3 checking.

        // If the bean failed to activate because it was found to not
        // exist (original exception is NoSuchObjectException or
        // NoSuchEJBException for Singleton failures), assume the
        // exception type to be checked exception so that no rollback will
        // be performed during transaction control in post-invoke processing.
        // Otherwise, set the type to unchecked exception and log it
        // as required by the EJB specification.
        // Also, as determined by CTS, when BeanNotReentrantException
        // occurs in preInvoke, it is not considered that the method was
        // ever invoked, and so it should be a checked exception and
        // should not cause a rollback.
        // Also, as determined by CTS, an AccessException from the Security
        // Collaborator should not cause a rollback.
        if ((s.preInvokeException && (ex instanceof NoSuchObjectException ||
                                      ex instanceof NoSuchEJBException || // d632115
                                      ex instanceof EJBStoppedException || // d350987
                                      ex instanceof CSIAccessException || ex instanceof BeanNotReentrantException)))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception should not cause rollback, "
                             + "changing to checked exception");

            // For NoSuchObjectException, the beanO is already null since
            // it occurs during activation, but for BeanNotReentrant, the
            // activation may have succeeded (for Entity beans), but since
            // the method is never invoked, postInvoke processing should
            // not occur on the beanO; so null it out.
            if (ex instanceof BeanNotReentrantException)
            {
                s.beanO = null;
            }
            s.exType = ExceptionType.CHECKED_EXCEPTION;
        }
        else if (EJSContainer.defaultContainer == null)
        {
            // If the EJBContainer has been stopped, then likely an ugly exception occurred,
            // like NullPointerException. In this case, just discard the original exception
            // and throw a meaningful one indicating the EJB feature has been removed.
            s.exType = ExceptionType.UNCHECKED_EXCEPTION;
            ex = new EJBException("The Enterprise JavaBeans (EJB) features have been deactivated. No further EJB processing is allowed");

            // Log the error message at this point
            ExceptionUtil.logException(tc, ex, s.getEJBMethodMetaData(), s.getBeanO());

            // Don't bother with FFDC, it will just be clutter at this point.
        }
        else
        {
            s.exType = ExceptionType.UNCHECKED_EXCEPTION;

            // Log the error message at this point
            ExceptionUtil.logException(tc, ex, s.getEJBMethodMetaData(), s.getBeanO());

            // FFDC log any unchecked / unexpected exceptions.  d331740
            FFDCFilter.processException(ex, CLASS_NAME + ".setUncheckedException", "506", this);
        }

        // Find the root cause of the exception.
        s.rootEx = findRootCause(ex);

        // map the exception to the appropriate EJBException
        s.ivException = mapException(ex);

        // At this point, it is assumed that the exception being returned
        // is an EJBException created by the customer, with the stack
        // as create by the customer, or is an EJBException created by
        // EJB Container, with an empty stack (if there is a cause), or the
        // stack of the exception that was mapped to an EJBException
        // (if there is no cause).  No need to set the stack here.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setUncheckedException returning: " + s.ivException);

        return s.ivException;
    } // setUncheckedException

    /**
     * This method is specifically designed for use during EJSContainer
     * postInvoke processing. It maps the internal exception indicating
     * a rollback has occurred to the appropriate one for the interface
     * (i.e. local, remote, business, etc). <p>
     **/
    @Override
    public Exception mapCSITransactionRolledBackException
                    (EJSDeployedSupport s, CSITransactionRolledbackException ex)
                                    throws com.ibm.websphere.csi.CSIException
    {
        Throwable cause = null;
        Exception causeEx = null;

        // If the invoked method threw a System exception, or an Application
        // exception that was marked for rollback, or the application called
        // setRollBackOnly... then use the exception thrown by the method
        // as the cause of the rollback exception.
        if (s.exType == ExceptionType.UNCHECKED_EXCEPTION)
        {
            cause = s.ivException;
        }

        // Otherwise, use the cause of the CSIException as the cause,
        // unless it has no cause... then use the app exception.
        else
        {
            cause = ExceptionUtil.findCause(ex);
            if (cause == null)
                cause = s.ivException;
        }

        // Because this will be mapped to an EJBException, and Throwable
        // is not supported on the constructor... insure the cause is
        // either an Exception, or wrap it in an Exception.
        if (cause != null)
        {
            if (cause instanceof Exception)
            {
                causeEx = (Exception) cause;
            }
            else
            {
                causeEx = new Exception("See nested Throwable", cause);
            }
        }

        // Now, map this CSIException... this will take care of getting
        // the stack set appropriately and will set the 'cause' on
        // Throwable, so getCause works.
        Exception mappedEx = mapCSIException(ex, causeEx, cause);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "mapped exception = " + mappedEx);

        return mappedEx;
    }
}
