/*******************************************************************************
 * Copyright (c) 2001, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import static com.ibm.ejs.container.ContainerProperties.AllowSpecViolationOnRollback;
import static com.ibm.ejs.container.ContainerProperties.IncludeNestedExceptionsExtended;

import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

import javax.ejb.AccessLocalException;
import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.TransactionRequiredLocalException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
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
 * The LocalExceptionStrategy is utilized by the EJSDeployedSupport class
 * for mapping exceptions to be thrown for method invocations involving
 * EJB 2.x LocalEJB References. <p>
 *
 * This class does not apply to EJB 3.0 business interfaces. All mappings
 * should be based on the EJB 2.1 specification, and the exceptions new
 * in EJB 3.0 should not be used, even for EJB 2.1 component interfaces
 * in an EJB 3.0 module. <p>
 *
 * However, note that the new ApplicationException annotation (and
 * corresponding xml) does apply to EJB 2.1 component interfaces that are
 * in an EJB 3.0 (or later) module. <p>
 **/
public class LocalExceptionMappingStrategy extends ExceptionMappingStrategy
{
    private static final String CLASS_NAME = LocalExceptionMappingStrategy.class.getName();
    private static final TraceComponent tc = Tr.register(LocalExceptionMappingStrategy.class,
                                                         "EJBContainer", "com.ibm.ejs.container.container");
    static final ExceptionMappingStrategy INSTANCE = new LocalExceptionMappingStrategy(); // F61004.3T

    private LocalExceptionMappingStrategy()
    {
        // Private
    }

    /**
     * Map a CSIException to the appropriate EJBException. <p>
     *
     * This method does not apply to EJB 3.0 business interfaces. All mappings
     * should be based on the EJB 2.1 specification, and the exceptions new
     * in EJB 3.0 should not be used, even for EJB 2.1 component interfaces
     * in an EJB 3.0 module. <p>
     */
    private EJBException mapCSIException(EJSDeployedSupport s, CSIException e, Exception rootEx)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "mapCSIException: " + e);
        }

        String message = " ";
        EJBException ejbex;
        if (e instanceof CSITransactionRolledbackException)
        {
            //445903 start: We must look for a HeuristicMixedException nested in the CSITransactionRolledbackException
            //and if found, make sure it is nested in a RemoteException so the user will see the HeuristicMixedException.
            //If we throw a TransactionRolledbackException with the HeuristicMixedException nested within it, or if we
            //throw only the TransactionRolledbackException (pre-d445903), this will give the impression that the transaction
            //has been rolledback.  However, by its definition, a HeuristicMixedException indicates that some updates/resources
            //have been committed and other may have been committed.  As such, throwing only a TransactionRolledbackException
            //or TransactionRolledbackException + HeuristicMixedException can be misleading.
            //
            //PK87857 note: When this APAR was put in earlier releases, the 'instanceof' check for HME was included in the
            //same 'if' check as is included in the 'else if' block to follow.  However, in v7.0 dev, the following 'if'
            //block was added via d445903.  As such, we must leave this code 'as-is' so as not to regress anyone.  It
            //should also be noted that this 'if' block is a potential spec violation.  That is, if the HME is caused by
            //a component other than the Tx 'beginner', the EJBException + HME will flow back up stream rather than the
            //spec mandated TransactionRolledbackLocalException; needless to say this causes a spec violation.
            if (e.detail instanceof HeuristicMixedException) {
                //Nest the HeuristicMixedException in an EJBException.
                ejbex = ExceptionUtil.EJBException(message, rootEx);
            }
            //PK87857: We must look for a HeuristicRollbackException nested in the
            //CSITransactionRolledbackException and if found, we must make sure it is nested in an
            //EJBException so the user will see the Heuristic Exception.  Let me explain why this should be done:
            //  If we throw a TransactionRolledbackLocalException with the HeuristicRollbackException nested within
            //  it, or if we throw only the TransactionRolledbackLocalException (pre-PK87857), this will give the
            //  impression that the transaction has been rolledback.  In the case of a HeuristicRollbackException, the
            //  transaction has been rolledback so a TransactionRolledbackLocalException is valid, however, it may be
            //  important for the user to know that a Heuristic decision was made to rollback the transaction.
            else if (IncludeNestedExceptionsExtended
                     && (s.began || AllowSpecViolationOnRollback)
                     && (e.detail instanceof HeuristicRollbackException)) {
                //Nest the Heuristic Exception in an EJBException.
                ejbex = ExceptionUtil.EJBException(message, rootEx);
            }
            else {
                ejbex = new TransactionRolledbackLocalException(message, rootEx);
            }
            //445903 end
        }
        else if (e instanceof CSIAccessException)
        {
            ejbex = new AccessLocalException(message, rootEx);
        }
        else if (e instanceof CSIInvalidTransactionException)
        {
            ejbex = new InvalidTransactionLocalException(message, rootEx);
        }
        else if (e instanceof CSINoSuchObjectException)
        {
            ejbex = new NoSuchObjectLocalException(message, rootEx);
        }
        else if (e instanceof CSITransactionRequiredException)
        {
            ejbex = new TransactionRequiredLocalException(message);
        }
        //        else if (e instanceof CSIInvalidActivityException)
//        {
//            ejbex = new InvalidActivityLocalException(message, rootEx);
//        }
//        else if (e instanceof CSIActivityRequiredException)
//        {
//            ejbex = new ActivityRequiredLocalException(message, rootEx);
//        }
//        else if (e instanceof CSIActivityCompletedException)
//        {
//            ejbex = new ActivityCompletedLocalException(message, rootEx);
//        }
        else if (rootEx instanceof EJBException)
        {
            ejbex = (EJBException) rootEx;
        }
        else
        {
            // Spec requires an EJBException; there is no longer any value add in
            // using the deprecated nested exception interface.            F71894.1
            ejbex = ExceptionUtil.EJBException(rootEx);
        }

        // EJBException doesn't normally set the cause on Throwable, so
        // let's do that to be nice :-)                                    d354591
        // Geronimo EJBException.getCause returns getCausedbyException, so
        // we do not expect this code to be used.                           F53643
        if (rootEx != null &&
            rootEx != ejbex &&
            ejbex.getCause() == null)
        {
            ejbex.initCause(rootEx);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "mapCSIException returning: " + ejbex);
        }

        return ejbex;
    }

    // f111627 Begin
    /**
     * Map the exception to an EJBException exception for local
     * interface. This is the exception that will be thrown to the client.
     * All fun and games in order to comply with both the Java EE spec.
     *
     * This method does not apply to EJB 3.0 business interfaces. All mappings
     * should be based on the EJB 2.1 specification, and the exceptions new
     * in EJB 3.0 should not be used, even for EJB 2.1 component interfaces
     * in an EJB 3.0 module. <p>
     */
    //150727 - rewrote entire method.
    private EJBException mapException(EJSDeployedSupport s, Throwable ex) //117817.1
    {
        EJBException ejbex;
        String message = null;

        // If necessary, wrap root with WsEJBException.  This is done if
        // root is not an instance of Exception.
        Exception rootEx = ExceptionUtil.Exception(s.rootEx); // F71894.1

        // Now map Throwable to a EJBException to return.
        if (ex instanceof CSIException)
        {
            ejbex = mapCSIException(s, (CSIException) ex, rootEx);
        }
        else if (ex instanceof NoSuchObjectException)
        {
            ejbex = new NoSuchObjectLocalException(message, rootEx);
        }
        else if (ex instanceof EJBStoppedException) // d350987
        {
            ejbex = new NoSuchObjectLocalException(message, rootEx);
        }
        else if (ex instanceof HomeDisabledException) // d350987
        {
            ejbex = new NoSuchObjectLocalException(message, rootEx);
        }
        else if (ex instanceof TransactionRequiredException)
        {
            ejbex = new TransactionRequiredLocalException(message);
        }
        else if (ex instanceof TransactionRolledbackException)
        {
            ejbex = new TransactionRolledbackLocalException(message, rootEx);
        }
        else if (ex instanceof InvalidTransactionException)
        {
            ejbex = new InvalidTransactionLocalException(message, rootEx);
        }
        else if (ex instanceof AccessException)
        {
            ejbex = new AccessLocalException(message, rootEx);
        }
        //        else if (ex instanceof ActivityRequiredException)
//        {
//            ejbex = new ActivityRequiredLocalException(message, rootEx);
//        }
//        else if (ex instanceof InvalidActivityException) //126946.3
//        {
//            ejbex = new InvalidActivityLocalException(message, rootEx); //126946.3
//        }
//        else if (ex instanceof ActivityCompletedException)
//        {
//            ejbex = new ActivityCompletedLocalException(message, rootEx); //126946.3
//        }
        else if (ex instanceof EJBException)
        {
            // Even though this is already an EJBException we will nest it inside another EJBException that we create.
            // This is done to ensure that but the getCause() and getCausedByException() methods will work on it.   //253963
            ejbex = ExceptionUtil.EJBException(ex); //253963
        }
        else if (ex instanceof RemoteException)
        {
            // If the RemoteException is already wrapping an EJBException, then
            // use that exception, otherwise create a new EJBException with the root  //253963
            if (((RemoteException) ex).detail instanceof EJBException)
            {
                ejbex = ExceptionUtil.EJBException(ex); //253963
            }
            else
                ejbex = ExceptionUtil.EJBException(rootEx); //253963
        }
        else if (ex instanceof IllegalArgumentException) // d184523
        {
            ejbex = ExceptionUtil.EJBException(rootEx); //253963
        }
        else
        {
            ejbex = new UnknownLocalException(message, rootEx);
        }

        // EJBException doesn't normally set the cause on Throwable, so
        // let's do that to be nice :-)                                    d354591
        // Geronimo EJBException.getCause returns getCausedbyException, so
        // we do not expect this code to be used.                           F53643
        if (rootEx != null &&
            rootEx != ejbex &&
            ejbex.getCause() == null)
        {
            ejbex.initCause(rootEx);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "mapException returning: " + ejbex); // d402055
        }

        return ejbex;
    }

    // f11627 End

    // f111627 Begin
    /**
     * Capture an unchecked exception thrown by a bean method and
     * reraise it as an EJBException. <p>
     *
     * An unchecked exception is a runtime exception or one that is not declared in
     * the signature of the bean method. <p>
     *
     * Although this method does not apply to the new EJB 3.0 business
     * interfaces; note that the new ApplicationException annotation
     * (and corresponding xml) does apply to EJB 2.1 component interfaces
     * that are in an EJB 3.0 (or later) module. This may result in an
     * unchecked exception being converted to a checked exception. <p>
     *
     * @param ex the <code>Exception</code> thrown by bean method <p>
     */
    @Override
    public final Throwable setUncheckedException(EJSDeployedSupport s, Throwable ex) // d395666
    {
        // d161864 Begins
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setUncheckedException in param:" + ex); //150727
        }
        // d161864 Ends

        // d395666 start
        // Entering this method for a second time?
        if (s.ivException != null)
        {
            // Yep, this is the second time, so we will just keep the original exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            {
                Tr.event(tc, "setting unchecked exception again", ex);
                Tr.event(tc, "original exception", s.ivException);
                Tr.exit(tc, "setUncheckedException returning: " + s.ivException); // d402055
            }
            return s.ivException;
        }

        // Nope, this is first time this method entered.

        // If the method was invoked on an interface from an EJB 3.0
        // (or later) module and a non-RemoteException occurred, then the
        // new applicaiton exception processing must be performed.
        // This new support is enabled for both component and business
        // interfaces in an EJB 3.0 module.                              396839
        Boolean applicationExceptionRollback = null;
        int moduleVersion = s.ivWrapper.bmd.ivModuleVersion;
        if (moduleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0 &&
            ex instanceof Exception && // d423621
            !(ex instanceof RemoteException))
        {
            // Yep, check whether it is annotated as an ApplicationException.
            applicationExceptionRollback = s.getApplicationExceptionRollback(ex);
        }

        // Did above code result in ExceptionType being set to a
        // checked exception?
        if (applicationExceptionRollback != null)
        {
            // Yep, it must be an AppplicationException, so just store it
            // in ivException so that it gets rethrown.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            {
                Tr.event(tc, "ApplicationException with rollback set to true, changing to a checked exception", ex);
            }

            s.exType = ExceptionType.CHECKED_EXCEPTION;
            s.ivException = ex;
            s.rootEx = ex;

            // Is rollback set to true for the ApplicationException?
            if (applicationExceptionRollback == Boolean.TRUE)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                {
                    Tr.event(tc, "ApplicationException with rollback set to true, setting rollback only", ex);
                }
                s.currentTx.setRollbackOnly();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                Tr.exit(tc, "setUncheckedException returning: " + s.ivException);
            }

            return s.ivException;
        }
        // d395666 end

        // Not a EJB 3 ApplicationException, so pre-EJB 3 checking.

        // If the bean failed to activate because it was found to not
        // exist (original exception is NoSuchObjectException), assume the
        // exception type to be checked exception so that no rollback will
        // be performed during transaction control in post-invoke processing.
        // Otherwise, set the type to unchecked exception and log it
        // as required by the EJB specification.                      d116274.1
        // Also, as determined by CTS, when BeanNotReentrantException
        // occurs in preInvoke, it is not considered that the method was
        // ever invoked, and so it should be a checked exception and
        // should not cause a rollback.                                 d159491
        // Also, as determined by CTS, an AccessException from the Security
        // Collaborator should not cause a rollback.                    d184062
        // Also, as determined by CTS, for CMR set methods (local only),
        // if an IllegalArgumentException occurs, it should not cause a
        // rollback - the method is not considered invoked.             d184523
        // If a HomeDisableException occurs, or if a CreateFailureException occurs
        // which has nested in it a HomeDisabledException, make this a checked exception.
        // This will have the effect of suppressing the printing of the HomeDisabledException
        // from the system logs as was requested in APAR PK30272.
        if ((s.preInvokeException &&
            (ex instanceof NoSuchObjectException ||
             ex instanceof EJBStoppedException || // d350987
             ex instanceof HomeDisabledException || // d350987
             ex instanceof CSIAccessException ||
            ex instanceof BeanNotReentrantException)) ||
            (!s.preInvokeException &&
             s.methodInfo.isCMRSetMethod &&
            ex instanceof IllegalArgumentException) ||
            ex instanceof HomeDisabledException ||
            (ex instanceof CreateFailureException &&
            (ex.getCause() instanceof HomeDisabledException ||
            ex.getCause() instanceof EJBStoppedException))) // d350987
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                Tr.debug(tc
                         , "Exception should not cause rollback, "
                           + "changing to checked exception");
            }

            // For NoSuchObjectException, the beanO is already null since
            // it occurs during activation, but for BeanNotReentrant, the
            // activation may have succeeded (for Entity beans), but since
            // the method is never invoked, postInvoke processing should
            // not occur on the beanO; so null it out.                   d159491
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
            FFDCFilter.processException(ex
                                        , CLASS_NAME + ".setUncheckedException"
                                        , "178"
                                        , this); //123338
        }

        // Find the root cause of the exception.                      d109641.1
        s.rootEx = findRootCause(ex);

        // map the exception to the appropriate EJBException        // 117817.1
        s.ivException = mapException(s, ex);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setUncheckedException returning: " + s.ivException); //150727
        }

        // set the stack trace to be the exception stack for
        // the root cause of the exception.
        s.ivException.setStackTrace(s.rootEx.getStackTrace()); // d180095

        return s.ivException; // d117817 // d395666
    } // setUncheckedException

    // f111627 end

    /**
     * This method does not apply to EJB 3.0 business interfaces. All mappings
     * should be based on the EJB 2.1 specification, and the exceptions new
     * in EJB 3.0 should not be used, even for EJB 2.1 component interfaces
     * in an EJB 3.0 module. <p>
     **/
    @Override
    public Exception
                    mapCSITransactionRolledBackException(EJSDeployedSupport s, CSITransactionRolledbackException ex)
                                    throws com.ibm.websphere.csi.CSIException
    {
        //d180095
        // Ensure root is set before doing the mapping.
        if (s.rootEx == null)
        {
            s.rootEx = ExceptionUtil.findRootCause(ex);
        }

        Exception mappedEx = mapException(s, ex);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "MappedException = " + mappedEx);
        }
        // set the stack trace in CORBA exception to the exception stack for
        // the root cause of the exception.
        mappedEx.setStackTrace(s.rootEx.getStackTrace());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "returning: " + mappedEx); //150727 d402055
        }
        return mappedEx;
    }
}
