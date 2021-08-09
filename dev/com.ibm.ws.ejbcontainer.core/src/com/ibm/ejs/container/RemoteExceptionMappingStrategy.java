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

import static com.ibm.ejs.container.ContainerProperties.AllowSpecViolationOnRollback; //PK87857
import static com.ibm.ejs.container.ContainerProperties.IncludeNestedExceptions;
import static com.ibm.ejs.container.ContainerProperties.IncludeNestedExceptionsExtended; //PK87857
import static com.ibm.ejs.container.WrapperInterface.BUSINESS_RMI_REMOTE;
import static com.ibm.ejs.container.WrapperInterface.SERVICE_ENDPOINT;

import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

import javax.ejb.AccessLocalException;
import javax.ejb.ConcurrentAccessException;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.TransactionRequiredLocalException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.transaction.HeuristicMixedException; //PK87857
import javax.transaction.HeuristicRollbackException; //PK87857
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
import com.ibm.websphere.csi.OrbUtils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * The RemoteExceptionMappingStrategy is utilized by the EJSDeployedSupport class
 * for mapping exceptions to be thrown for method invocations involving
 * Remote EJB References. <p>
 *
 * This class is used for EJB 2.1 (and earlier) component remote interfaces
 * as well as EJB 3.0 business remote interfaces that implement java.rmi.Remote.
 * Generally, the exception mapping for the component and java.rmi.Remote
 * business interfaces is the same, but for those differences that do exist,
 * the code should run conditionally based on the wrapper interface type. <p>
 *
 * However, note that the new ApplicationException annotation (and
 * corresponding xml) does apply to EJB 2.1 component interfaces that are
 * in an EJB 3.0 (or later) module. <p>
 **/
public class RemoteExceptionMappingStrategy extends ExceptionMappingStrategy
{
    private static final String CLASS_NAME = RemoteExceptionMappingStrategy.class.getName();
    private static final TraceComponent tc = Tr.register(RemoteExceptionMappingStrategy.class,
                                                         "EJBContainer", "com.ibm.ejs.container.container");
    static final ExceptionMappingStrategy INSTANCE = new RemoteExceptionMappingStrategy(); // F61004.3

    /**
     * Construct a RemoteExceptionMappingStrategy.
     **/
    private RemoteExceptionMappingStrategy()
    {
        // Private.
    }

    private static OrbUtils getOrbUtils() // F61004.3
    {
        return EJSContainer.getDefaultContainer().getOrbUtils();
    }

    /**
     * Map the remote exception to a CORBA system exception. This is the
     * exception that will be thrown to the client stub. The client stub
     * will then reconvert this exception back to a remote exception.
     * All fun and games in order to comply with both the Java EE spec
     * and the RMI/IIOP spec.
     *
     * This method is used for EJB 2.1 (and earlier) component remote interfaces
     * as well as EJB 3.0 business remote interfaces that implement java.rmi.Remote.
     * Generally, the exception mapping for the component and java.rmi.Remote
     * business interfaces is the same, but for those differences that do exist,
     * the code should run conditionally based on the wrapper interface type. <p>
     */
    //150727 - rewrote entire method.
    private Throwable mapException(EJSDeployedSupport s, RemoteException ex) //d135584
    {
        try
        {
            if (ex instanceof CSIException) {
                CSIException csiex = (CSIException) ex;
                return mapCSIException(s, csiex);
            }
            else if (ex.detail instanceof CSIException)
            {
                CSIException csiex = (CSIException) ex.detail; //d177787
                return mapCSIException(s, csiex);
            }
            else if (ex instanceof CreateFailureException)
            {
                // For consistency, just map/return the CreateFailureException
                // even if it contains an EJBException, as in most cases the
                // EJBException would just be wrapped in a plain RemoteException,
                // which CreateFailureException already is.                 d187050
                // Since this is already a RemoteException, it only needs to be
                // "mapped" if the method request is throught the ORB.       d228774
                if (!s.ivWrapper.ivInterface.ivORB)
                {
                    return ex;
                }

                return getOrbUtils().mapException(ex);
            }
            else if (ex instanceof BeanNotReentrantException &&
                     (s.ivWrapper.ivInterface == BUSINESS_RMI_REMOTE ||
                     s.ivWrapper.ivInterface == SERVICE_ENDPOINT)) // d350987
            {
                // EJB 3.0 has defined an exception for this purpose.      d366807.6
                // This mapping is only done for business interfaces to insure
                // compatibility with EJB 2.1 clients.                        396839
                BeanNotReentrantException bnre = (BeanNotReentrantException) ex;
                EJBException caex = bnre.isTimeout() ? new ConcurrentAccessTimeoutException(ex.getMessage()) // d653777.1
                : new ConcurrentAccessException(ex.getMessage());
                caex.setStackTrace(ex.getStackTrace());
                s.rootEx = caex;
                return mapEJBException(s, caex);
            }
            else if (ex.detail instanceof EJBException)
            {
                EJBException ejbex = (EJBException) ex.detail;
                return mapEJBException(s, ejbex);
            }
            else
            {
                // Since this is already a RemoteException, it only needs to be
                // "mapped" if the method request is throught the ORB.       d228774
                if (!s.ivWrapper.ivInterface.ivORB)
                {
                    return ex;
                }

                return getOrbUtils().mapException(ex);
            }
        } catch (CSIException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".mapException", "119", this);
            Tr.warning(tc, "UNABLE_TO_MAP_EXCEPTION_CNTR0013W"
                       , new Object[] { ex, e }); //p111002.5
            return ex;
        }
    }

    /**
     * Capture an unchecked exception thrown by a bean method and
     * map it to the appropriate CORBA exception for transport.
     * The ORB will map these CORBA exceptions back to the appropriate
     * RemoteException on the Client side. <p>
     *
     * An unchecked exception is a runtime exception, or one that is not
     * declared in the signature of the bean method. <p>
     *
     * Although this method does apply to EJB 2.1 component interfaces
     * interfaces; note that the new ApplicationException annotation
     * (and corresponding xml) also applies to EJB 2.1 component interfaces
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
        // exist (original exception is NoSuchObjectException or
        // NoSuchEJBException for Singleton failures), assume the
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
        if (s.preInvokeException &&
            (ex instanceof NoSuchObjectException ||
             ex instanceof NoSuchEJBException || // d632115
             ex instanceof CSIAccessException ||
            ex instanceof BeanNotReentrantException))
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

            // For NoSuchEJBException, it is mapped to the corresponding
            // remote NoSuchObjectException here, so it doesn't get wrapped
            // in a RemoteException below.                            d632115
            else if (ex instanceof NoSuchEJBException)
            {
                StackTraceElement[] stack = ex.getStackTrace();
                ex = new NoSuchObjectException(ExceptionUtil.getBaseMessage(ex));
                ex.setStackTrace(stack);
            }

            s.exType = ExceptionType.CHECKED_EXCEPTION;
        }
        else if (EJSContainer.defaultContainer == null)
        {
            // If the EJBContainer has been stopped, then likely an ugly exception occurred,
            // like NullPointerException. In this case, just discard the original exception
            // and throw a meaningful one indicating the EJB feature has been removed.
            s.exType = ExceptionType.UNCHECKED_EXCEPTION;
            ex = new RemoteException("The Enterprise JavaBeans (EJB) features have been deactivated. No further EJB processing is allowed");

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
                                        , "200"
                                        , this); //123338
        }

        // Find the root cause of the exception.                      d109641.1
        s.rootEx = findRootCause(ex);

        //117817.1 begin

        // if  the exception that was passed in is not a
        // RemoteException, then we must nest it inside a RemoteException
        if (!(ex instanceof RemoteException)) //117817.1
        {
            ex = new RemoteException("", ex);

            // set the stack trace to the exception stack for
            // the root cause of the exception.
            ex.setStackTrace(s.rootEx.getStackTrace()); //180095
        }

        // Now we will map these RemoteExceptions to the proper CORBA exception.
        // The ORB will map these exceptions back to the appropriate RemoteException on
        // the client side.  117817.1
        s.ivException = mapException(s, (RemoteException) ex);

        // 117817.1 end

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "setUncheckedException returning : " + s.ivException);
        }

        // set the stack trace in CORBA exception to the exception stack for
        // the root exception so that the problem does look like container has a problem.
        s.ivException.setStackTrace(s.rootEx.getStackTrace()); //d180095

        return s.ivException; // d395666
    } // setUncheckedException

    /**
     * Map a CSIException to a CORBA system exception. This is the
     * exception that will be thrown to the client stub.
     *
     * This method is used for EJB 2.1 (and earlier) component remote interfaces
     * as well as EJB 3.0 business remote interfaces that implement java.rmi.Remote.
     * Generally, the exception mapping for the component and java.rmi.Remote
     * business interfaces is the same, but for those differences that do exist,
     * the code should run conditionally based on the wrapper interface type. <p>
     *
     * @param e is the CSIException to map.
     */
    //150727 - rewrote most of this method and changed signature.
    private Exception mapCSIException(EJSDeployedSupport s, CSIException e) throws CSIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "mapCSIException: " + e);
        }

        Exception mappedException = null;
        String message = " ";

        // First map CSIException to a RemoteException.
        RemoteException rex;

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
            //a component other than the Tx 'beginner', the RemoteException + HME will flow back up stream rather than the
            //spec mandated TransactionRolledbackException; needless to say this causes a spec violation.
            if (e.detail instanceof HeuristicMixedException) {
                //Nest the HeuristicMixedException within a RemoteException.
                rex = new RemoteException("", e.detail);
            }
            //PK87857: We must look for a HeuristicRollbackException nested in the
            //CSITransactionRolledbackException and if found, we must make sure it is nested in an
            //RemoteException so the user will see the Heuristic Exception.  Let me explain why this should be done:
            //  If we throw a TransactionRolledbackException with the HeuristicRollbackException nested within it, or
            //  if we throw only the TransactionRolledbackException (pre-PK87857), this will give the impression that the
            //  transaction has been rolledback.  In the case of a HeuristicRollbackException, the transaction has
            //  been rolledback so a TransactionRolledbackException is valid, however, it may be important for the user
            //  to know that a Heuristic decision was made to rollback the transaction.
            else if (IncludeNestedExceptionsExtended
                     && (s.began || AllowSpecViolationOnRollback)
                     && (e.detail instanceof HeuristicRollbackException)) {
                //Nest the Heuristic Exception in a RemoteException.
                rex = new RemoteException("", e.detail);
            }
            else {
                rex = new TransactionRolledbackException(message);
            }
            //445903 end
        }
        else if (e instanceof CSIAccessException)
        {
            rex = new AccessException(message);
        }
        else if (e instanceof CSIInvalidTransactionException)
        {
            rex = new InvalidTransactionException(message);
        }
        else if (e instanceof CSITransactionRequiredException)
        {
            rex = new TransactionRequiredException(message);
        }
        //        else if (e instanceof CSIInvalidActivityException)
//        {
//            rex = new InvalidActivityException(message);
//        }
//        else if (e instanceof CSIActivityRequiredException)
//        {
//            rex = new ActivityRequiredException(message);
//        }
//        else if (e instanceof CSIActivityCompletedException)
//        {
//            rex = new ActivityCompletedException(message);
//        }
        else if (e instanceof CSINoSuchObjectException)
        {
            rex = new java.rmi.NoSuchObjectException(message);
        }
        else
        {
            // CSIException is a RemoteException, but should not be exposed
            // to the customer, so convert to a plain RemoteException.      d228774
            // Note that 'detail' or its cause has been saved in 'root', so
            // clear 'detail' before calling getMessage(), or getMessage will
            // include the text/stack of detail, which ends up duplicating
            // the message text.                                            d228774
            e.detail = null;
            rex = new RemoteException(e.getMessage());
        }

        // Check the system property to determine if the customer wishes to have
        // nested exceptions included on the RemoteException when possible.
        // This option not the default because the client will need to ensure that
        // all possible nested exceptions are included in their classpath.  253963
        // Property access & default now handled by ContainerProperties.    391302
        // PK87857 note: recall that the property
        // ContainerProperties.IncludeNestedExceptionsExtended will automatically
        // enabled ContainerProperties.IncludeNestedExceptions
        if (IncludeNestedExceptions)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Nested exceptions will be included on rollback when possible. " +
                             rex + ": " + s.began);

            // If the transaction was begun within the context of the method that
            // was called then per the spec. we should return a generic
            // RemoteException so that the nested excpetions will not be stripped
            // off by the ORB                                               253963
            // PK87857- Or if the user allows for the RemoteException even when the the Tx rollback
            // is caused by someone other than the "beginner" (this is a violation
            // of the spec).
            if ((rex instanceof TransactionRolledbackException) &&
                (s.began || AllowSpecViolationOnRollback)) //PK87857
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Transaction was begun in context of this bean method, or "
                                 + "the user allows the spec to be violated "
                                 + "(ContainerProperties.AllowSpecViolationOnRollback=" + AllowSpecViolationOnRollback + ").");
                rex = new RemoteException("", rex);
            }
        }

        // Now set detail in RemoteException.
        rex.detail = s.rootEx;

        // Since this code created the RemoteException being passed
        // to OrbUtilsImpl.mapException method, set the stack trace
        // in it to be the exception stack from the root exception
        // so that any exception chain the ORB returns to the
        // client (e.g. CORBA UnknownException case) does not point
        // to this code as being the root cause of the problem.
        rex.setStackTrace(s.rootEx.getStackTrace()); //180095

        // If the current method request came through the ORB, then the final
        // RemoteException must be mapped to the appropriate CORBA exception.
        // Otherwise, for example for WebService Endpoint methods, the
        // RemoteException does not need to be mapped to anything,
        // WebServices will do that.                                       d228774
        if (s.ivWrapper.ivInterface.ivORB)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "mapCSIException calling OrbUtils.mapException: " + rex);
            }

            // Now have OrbUtils map to a CORBA exception for remote client stub.

            // Perform standard exception mapping using the exception
            // mapping utilities (just like EJSDeployedSupport). This
            // will wrap the CSI exception in a
            // TransactionRolledBackException, which is the exception
            // required by the EJB spec, then that will be mapped to
            // a CORBA TRANSACTION_ROLLEDBACK exception, as required
            // by the ORB.  The ORB should convert the
            // TRANSACTION_ROLLEDBACK to a TransactionRolledBackException
            // on the client.  Note that the root cause will be lost,
            // but will be embeded as part of the message text.  d109641.1

            int minorCode = e.getMinorCode();
            if (minorCode == CSIException.NO_MINOR_CODE)
            {
                mappedException = getOrbUtils().mapException(rex);
            }
            else
            {
                mappedException = getOrbUtils().mapException(rex, minorCode);
            }
        }
        else
        {
            mappedException = rex;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "mapCSIException returning: " + mappedException);
        }

        return mappedException;
    }

    /**
     * This method is used for EJB 2.1 (and earlier) component remote interfaces
     * as well as EJB 3.0 business remote interfaces that implement java.rmi.Remote.
     * Generally, the exception mapping for the component and java.rmi.Remote
     * business interfaces is the same, but for those differences that do exist,
     * the code should run conditionally based on the wrapper interface type. <p>
     **/
    @Override
    public Exception mapCSITransactionRolledBackException
                    (EJSDeployedSupport s, CSITransactionRolledbackException ex)
                                    throws CSIException
    {
        //d180095
        // Ensure root is set before doing the mapping.
        if (s.rootEx == null)
        {
            s.rootEx = ExceptionUtil.findRootCause(ex);
        }

        Exception mappedEx = mapCSIException(s, ex);

        // set the stack trace in CORBA exception to the exception stack for
        // the root cause of the exception.
        mappedEx.setStackTrace(s.rootEx.getStackTrace());
        return mappedEx;
    }

    /**
     * Map an EJBException to a CORBA system exception. This is the
     * exception that will be returned to the client stub.
     *
     * This method is used for EJB 2.1 (and earlier) component remote interfaces
     * as well as EJB 3.0 business remote interfaces that implement java.rmi.Remote.
     * Generally, the exception mapping for the component and java.rmi.Remote
     * business interfaces is the same, but for those differences that do exist,
     * the code should run conditionally based on the wrapper interface type. <p>
     *
     * @param e is the EJBException to map.
     */
    //150727 - added entire method.
    private Exception mapEJBException(EJSDeployedSupport s, EJBException e) throws CSIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "mapEJBException: " + e);
        }

        Exception mappedException = null;
        String message = " ";

        // First map EJBException to a RemoteException.
        RemoteException rex;

        if (e instanceof TransactionRolledbackLocalException)
        {
            rex = new TransactionRolledbackException(message);
        }
        else if (e instanceof AccessLocalException)
        {
            rex = new AccessException(message);
        }
        else if (e instanceof InvalidTransactionLocalException)
        {
            rex = new InvalidTransactionException(message);
        }
        else if (e instanceof NoSuchObjectLocalException)
        {
            rex = new NoSuchObjectException(message);
        }
        else if (e instanceof TransactionRequiredLocalException)
        {
            rex = new TransactionRequiredException(message);
        }
        //        else if (e instanceof InvalidActivityLocalException)
//        {
//            rex = new InvalidActivityException(message);
//        }
//        else if (e instanceof ActivityRequiredLocalException)
//        {
//            rex = new ActivityRequiredException(message);
//        }
//        else if (e instanceof ActivityCompletedLocalException)
//        {
//            rex = new ActivityCompletedException(message);
//        }
        else
        {
            rex = new RemoteException(message);
        }

        // Now set detail in RemoteException.
        rex.detail = s.rootEx;

        // Since this code created the RemoteException being passed
        // to OrbUtilsImpl.mapException method, set the stack trace
        // in it to be the exception stack from the root exception
        // so that any exception chain the ORB returns to the
        // client (e.g. CORBA UnknownException case) does not point
        // to this code as being the root cause of the problem.
        rex.setStackTrace(s.rootEx.getStackTrace()); //180095

        // If the current method request came through the ORB, then the final
        // RemoteException must be mapped to the appropriate CORBA exception.
        // Otherwise, for example for WebService Endpoint methods, the
        // RemoteException does not need to be mapped to anything,
        // WebServices will do that.                                       d228774
        if (s.ivWrapper.ivInterface.ivORB)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "mapEJBException calling OrbUtils.mapException: " + rex);
            }

            // Now have OrbUtils map to a CORBA exception for remote client stub.
            mappedException = getOrbUtils().mapException(rex);
        }
        else
        {
            mappedException = rex;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "mapEJBException returning: " + mappedException);
        }
        return mappedException;
    }
}
