/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EnterpriseBean;
import javax.ejb.RemoveException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.rmi.PortableRemoteObject;
import javax.transaction.UserTransaction;

import com.ibm.ejs.container.activator.ActivationStrategy;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.ejs.csi.NullSecurityCollaborator;
import com.ibm.ejs.j2c.HandleList;
import com.ibm.ejs.j2c.HandleListInterface;
import com.ibm.websphere.csi.BeanInstanceInfo;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.MethodInterface;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.ejbcontainer.EJBContextExtension;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.csi.DispatchEventListener;
import com.ibm.ws.csi.DispatchEventListenerCookie;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ejbcontainer.EJBSecurityCollaborator;
import com.ibm.ws.ejbcontainer.util.Pool;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.runtime.metadata.MethodMetaData;
import com.ibm.ws.traceinfo.ejbcontainer.TEBeanLifeCycleInfo;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;

/**
 * A <code>BeanO</code> manages the lifecycle of a single enterprise bean
 * instance and provides the context implementation for its associated
 * enterprise bean. <p>
 *
 * A <code>BeanO</code> (an abbreviation of Bean Object) lives and dies
 * with its associated enterprise bean, maintains state information for that
 * bean, moves the enterprise bean through the state transitions
 * defined by the EJB spec, and supplies the EJB context implementation for
 * the bean. <p>
 *
 * A <code>BeanO</code> is an abstract class. It is designed to be
 * subclassed to provide implementations for each bean type that has
 * different lifecycle requirements: stateful session, stateless session,
 * and entity beans.
 *
 * Once created the association between a <code>BeanO</code> and an
 * enterprise bean instance is fixed. Likewise, a <code>BeanO</code>
 * is owned by a single container, and cannot be moved between containers.
 * However, the identity associated with the <code>BeanO</code> may change
 * over time. <p>
 */
public abstract class BeanO implements EJBContextExtension, // LI3492-2
                TimerService, // LI2281.07
                InjectionTargetContext, // F49213.1
                BeanInstanceInfo //LIDB2617.11
{
    private static final String CLASS_NAME = BeanO.class.getName();
    private static final TraceComponent tc = Tr.register(BeanO.class, "EJBContainer", "com.ibm.ejs.container.container");

    // LIDB2775-23.1 ASV60
    protected static final boolean isZOS = EJSPlatformHelper.isZOS(); // LIDB2775-23.7

    /**
     * State of this <code>BeanO</code>. <p>
     */
    protected int state;

    /**
     * This BeanO's container <p>
     */
    transient protected final EJSContainer container;

    /**
     * This BeanO's home. <p>
     */
    transient protected final EJSHome home;

    /**
     * Perf data object for this beanO.
     */
    transient protected EJBPMICollaborator pmiBean = null;//86523.3

    /**
     * Identity of this <code>BeanO</code>. <p>
     */
    protected BeanId beanId;

    /**
     * Bean pool this BeanO allocated from, must be returned to.
     */
    transient protected Pool beanPool;

    /**
     * The list of handles being used by this bean. This list is managed
     * using HandleCollaborator by the preInvoke, postInvoke, and destroy
     * methods of interested subclasses. By default, this list is not safe
     * to be used by re-entrant beans.
     *
     * Note that EJSHome.preConnectionHandleMgmt relies on this list
     * remaining null until getHandleList is called, which should not occur
     * unless BeanMetaData.skipConnectionHandleMgmt == false.
     */
    transient private HandleList connectionHandleList = null;//LIDB1181,d119173, d131085

    /**
     * The sessional activation policies get/set the ContainerTx on the beanO
     * during its life cycle events. It is used to assure activity session
     * requirements are maintained.
     *
     * Persistence Manager also requires the ContainerTx (PMTxInfo), and
     * it is cached on the BeanO for improved performance.
     */
    // LIDB441.5, d139352-2
    transient protected ContainerTx ivContainerTx = null;

    public ContainerTx getContainerTx() {
        return ivContainerTx;
    }

    public void setContainerTx(ContainerTx ctx) {
        ivContainerTx = ctx;
    }

    /**
     * ActivationStrategy for beans in the associated home. Cached for
     * performance.
     **/
    // d199233
    transient ActivationStrategy ivActivationStrategy;

    /**
     * Key into the EJB Cache for this BeanO. Will be null when the BeanO
     * is not in the EJB Cache.
     **/
    // d199233
    public transient Object ivCacheKey;

    /**
     * Create a new <code>BeanO</code> instance. <p>
     */

    public BeanO(EJSContainer c, EJSHome h) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "BeanO");
        }
        container = c;
        home = h;
        if (home != null) {
            pmiBean = home.pmiBean;
            if (pmiBean != null) {
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "pmiBean is non-null");
                }
                pmiBean.beanInstantiated();
            }
            beanPool = home.beanPool;
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "BeanO");
        }
    } // BeanO

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + beanId + ", " + getStateName(state) + ')';
    }

    /**
     * Initialize the <code>BeanO</code> and transition it to the POOLED state.
     * This method must be called immediately after constructing the object, and
     * it must not be called any time after that time.
     *
     * <p>This method is called with thread contexts established, but callback
     * bean is NOT set.
     *
     * @param reactivate true if a passivated stateful session bean is being reactivated
     *
     * @throws InvocationTargetException if the bean instance cannot be created
     */
    abstract void initialize(boolean reactivate) // d623673.1
                    throws RemoteException, InvocationTargetException;

    /**
     * Returns the name for the specified bean state.
     */
    protected abstract String getStateName(int state);

    /**
     * Set the current state of this <code>BeanO</code>. <p>
     *
     * The current state of this <code>BeanO</code> must be old state,
     * else this method fails. <p>
     *
     * @param oldState the old state <p>
     * @param newState the new state <p>
     */
    protected final synchronized void setState(int newState) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && // d527372
            TEBeanLifeCycleInfo.isTraceEnabled())
            TEBeanLifeCycleInfo.traceBeanState(state, getStateName(state), newState,
                                               getStateName(newState)); // d167264

        state = newState;
    } // setState

    /**
     * Set the current state of this <code>BeanO</code>. <p>
     *
     * The current state of this <code>BeanO</code> must be old state,
     * else this method fails. <p>
     *
     * @param oldState the old state <p>
     * @param newState the new state <p>
     */
    protected final synchronized void setState(int oldState, int newState) throws InvalidBeanOStateException, BeanNotReentrantException // LIDB2775-23.7
    {
        //------------------------------------------------------------------------
        // Inlined assertState(oldState); for performance.               d154342.6
        //------------------------------------------------------------------------
        if (state != oldState) {
            throw new InvalidBeanOStateException(getStateName(state), getStateName(oldState));
        }

        if (TraceComponent.isAnyTracingEnabled() && // d527372
            TEBeanLifeCycleInfo.isTraceEnabled())
            TEBeanLifeCycleInfo.traceBeanState(state, getStateName(state), newState,
                                               getStateName(newState)); // d167264

        //------------------------------------------------------------------------
        // Inlined setState(newState); for performance.                  d154342.6
        //------------------------------------------------------------------------
        state = newState;
    } // setState

    /**
     * Verify that this <code>BeanO</code> is in the specified state. <p>
     *
     * @param expected an <code>int</code> specifying the expected state <p>
     *
     * @exception InvalidBeanOStateException thrown if
     *                                           the current state of this <code>BeanO</code> is not equal
     *                                           to the expected state <p>
     */
    public final void assertState(int expected) throws InvalidBeanOStateException {
        if (state != expected) {
            throw new InvalidBeanOStateException(getStateName(state), getStateName(expected));
        }
    } // assertState

    /**
     * Return container this <code>BeanO</code> is owned by. <p>
     */

    public final EJSContainer getContainer() {

        return container;
    } // getContainer

    /**
     * Return identity of this <code>BeanO</code>. <p>
     */

    public final BeanId getId() {

        return beanId;

    } // getId

    /**
     * Set the identity of this <code>BeanO</code>. <p.
     */

    public final void setId(BeanId id) {

        beanId = id;

    } // setBeanId

    /**
     * Get the <code>EJSHome</code> associated with this <code>BeanO</code>.
     */

    public final EJSHome getHome() {
        return home;
    } // getHome

    /**
     * Return enterprise bean associate with this <code>BeanO</code>. <p>
     *
     * Note, this method is for use solely by the deployed code during
     * creation. This method will raise an exception if this
     * <code>BeanO</code> is not in the CREATING state. <p>
     */

    public abstract EnterpriseBean getEnterpriseBean() throws RemoteException;

    /**
     * Returns an array of Interceptor instances when ivCallbackKind is set to
     * CallbackKind.InvocationContext. For all other CallbackKind values,
     * null is returned.
     *
     * This includes around invoke, around timeout, and non-EnterpriseBean
     * lifecycle callback interceptors.
     **/
    // d630824
    public abstract Object[] getInterceptors();

    /**
     * Returns the ActivationStrategy associated with the Home
     * for this BeanO. <p>
     *
     * This method was added to improve performance. It will cache
     * the activation strategy in the BeanO, so that the method
     * call to get the ActivationStrategy from the Home may be
     * avoided for future operations. <p>
     *
     * Note: ActivationStrategy is NOT cached in the constructor
     * and deserialize, so that it is only cached when needed,
     * and not to impact the performance of deserialize which
     * may run in a synchronized block. <p>
     *
     * @return ActivationStrategy associated with the Home for
     *         this BeanO.
     **/
    // d199233
    public final ActivationStrategy getActivationStrategy() {
        if (ivActivationStrategy == null) {
            if (home == null)
                ivActivationStrategy = EJSContainer.homeOfHomes.getActivationStrategy();
            else
                ivActivationStrategy = home.getActivationStrategy();
        }

        return ivActivationStrategy;
    }

    /**
     * Destroy this <code>BeanO</code> instance. <p>
     *
     * This method must be called whenever this BeanO instance is no
     * longer valid. It transitions the BeanO to the DESTROYED state,
     * transitions the associated enterprise bean (if any) to the
     * does not exist state, and releases the reference to the
     * associated enterprise bean. <p>
     *
     * <p>This method is NOT always called with thread contexts established.
     */

    public abstract void destroy();

    /**
     * Activate this <code>BeanO</code> instance and its associated
     * enterprise bean. <p>
     *
     * This method is called with thread contexts established. <p>
     *
     * @param id the <code>BeanId</code> to use when activating this
     *               <code>BeanO</code>.
     * @param tx the current <code>ContainerTx</code> when this instance is being
     *               activated.
     */
    public abstract void activate(BeanId id, ContainerTx tx) // d114677 d139352-2
                    throws RemoteException;

    /**
     * Indicates the <code>BeanO</code> is about to be used for
     * creation. <p>
     *
     * This method must be called prior to calling both
     * <code>ejbCreate</code> and <code>ejbPostCreate</code> . <p>
     */
    // d140886.1
    public void preEjbCreate() throws CreateException //144144
    {
        // Most bean types don't need to do anything here.
        return;
    }

    /**
     * Complete the creation of this <code>BeanO</code> instance. <p>
     *
     * This method must be called after this <code>BeanO</code> has been
     * assigned an identity. If successful the <code>BeanO</code> is
     * moved to the ACTIVE state. <p>
     *
     * @param supportEjbPostCreateChanges indicates if ejb field changes
     *                                        in ejbPostCreate will be persisted to the database <p>
     *
     * @exception CreateException thrown if create-specific
     *                                error occurs <p>
     *
     * @exception RemoteException thrown if a container
     *                                error occurs <p>
     */
    public abstract void postCreate(boolean supportEjbPostCreateChanges) // d142250
                    throws CreateException, RemoteException;

    /**
     * Completes the creation of this <code>BeanO</code> instance. <p>
     *
     * This method will be called after both <code>ejbCreate</code> and
     * <code>ejbPostCreate</code> have been called. <p>
     */
    // d142250
    public void afterPostCreate() throws CreateException, RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "afterPostCreate : no implementation");

        // Most bean types don't need to do anything here.
        return;
    }

    /**
     * Completes the creation of this <code>BeanO</code> instance. <p>
     *
     * This method must be called after both <code>ejbCreate</code> and
     * <code>ejbPostCreate</code> have been called, and the bean is
     * considered to now be in the 'READY' state. <p>
     */
    // d142250
    public void afterPostCreateCompletion() throws CreateException {
        // Most bean types don't need to do anything here.
        return;
    }

    /**
     * Enlist this <code>BeanO</code> instance in the given transaction. <p>
     *
     * This method is called with thread contexts established. <p>
     *
     * @param tx the <code>ContainerTx</code> this instance is being
     *               enlisted in.
     *
     * @return true if a reference must be taken on the BeanO, otherwise false.
     */
    public abstract boolean enlist(ContainerTx tx) // d114677
                    throws RemoteException;

    /**
     * Retrieve this <code>BeanO's</code> associated enterprise bean, and
     * inform this <code>BeanO</code> that a method is about to be
     * invoked on its associated enterprise bean. <p>
     *
     * @param s  the <code>EJSDeployedSupport</code> instance associated
     *               with the pre/postInvoke, which contains an indication of
     *               which method is being invoked on this <code>BeanO</code>.
     * @param tx the <code>ContainerTx</code> for the transaction which
     *               this method is being invoked in.
     *
     * @return the Enterprise Bean instance the method will be invoke on.
     */
    // Chanced EnterpriseBean to Object. d366807.1
    public abstract Object preInvoke(EJSDeployedSupport s,
                                     ContainerTx tx) // d139352-2
                    throws RemoteException;

    /**
     * Inform this <code>BeanO</code> that a method invocation has
     * completed on its associated enterprise bean. <p>
     *
     * @param id an <code>int</code> indicating which method was being
     *               invoked on this <code>BeanO</code> <p>
     *
     * @param s  the <code>EJSDeployedSupport</code> instance associated
     *               with the pre, postInvoke <p>
     */

    public abstract void postInvoke(int id, EJSDeployedSupport s) throws RemoteException;

    /**
     * Inform this <code>BeanO</code> that a method invocation and any
     * corresponding transaction has been completed, so the bean can be returned
     * to the pool. <p>
     *
     * This method will only be called on stateless and message-driven beans.
     * This method will not be called on home beans.
     */
    public void returnToPool() throws RemoteException {
        throw new UnsupportedOperationException();
    }

    /**
     * Inform this <code>BeanO</code> that the transaction it was
     * enlisted with has committed. <p>
     *
     * <p>This method is NOT called with thread contexts established.
     */

    public abstract void commit(ContainerTx tx) throws RemoteException;

    /**
     * Inform this <code>BeanO</code> that the transaction it was
     * enlisted with has rolled back. <p>
     *
     * <p>This method is NOT called with thread contexts established.
     */

    public abstract void rollback(ContainerTx tx) throws RemoteException;

    /**
     * Ask this <code>BeanO</code> to write its associated enterprise
     * bean to persistent storage. <p>
     *
     * This method is called with thread contexts established for BMP only.
     * For CMP, this method is called with HandleList thread context
     * established only. <p>
     */

    public abstract void store() throws RemoteException;

    /**
     * Ask this <code>BeanO</code> to passivate its associated enterprise
     * bean. <p>
     *
     * <p>This method is NOT called with thread contexts established.
     */

    public abstract void passivate() throws RemoteException;

    /**
     * Remove this <code>BeanO</code> instance because the user has
     * explicitly removed it:
     * <ul>
     * <li>{@link javax.ejb.EJBObject#remove} <li>{@link javax.ejb.EJBLocalObject#remove} <li>{@link javax.ejb.EJBHome#remove(javax.ejb.Object)} <li>
     * {@link javax.ejb.EJBHome#remove(javax.ejb.Handle)} <li>{@link javax.ejb.EJBLocalHome} <li>{@link javax.ejb.Remove} </ul>
     *
     * This method is called with thread contexts established, but callback
     * bean is NOT set.
     *
     * @see #destroy
     */

    public abstract void remove() throws RemoteException, RemoveException;

    public abstract void discard();

    /**
     * Inform this <code>BeanO</code> that the transaction it is enlisted
     * with is about to complete. <p>
     */

    public abstract void beforeCompletion() throws RemoteException;

    /**
     * Ask this <code>BeanO</code> whether or not it has been removed
     */

    public abstract boolean isRemoved();

    /**
     * Method to allow container to condition removal of EJB from the EJB
     * cache on whether the BeanO is in destroyed state (EJB's are not in
     * the cache if there state is destroyed).
     **/
    // dPQ51806
    public abstract boolean isDestroyed();

    /**
     * Ask this <code>BeanO</code> whether or not it has been discarded
     */

    public abstract boolean isDiscarded();

    /**
     * Invalidate this <code>BeanO</code> instance.
     */

    public abstract void invalidate();

    /**
     * Determines if timer service methods are allowed based on the current state
     * of this bean instance. This includes the methods on the javax.ejb.Timer
     * and javax.ejb.TimerService interfaces. <P>
     *
     * Must be called by all Timer Service Methods to insure EJB Specification
     * compliance. <p>
     *
     * Note: This method does not apply to the EJBContext.getTimerService()
     * method, as getTimerService may be called for more bean states.
     * getTimerServcie() must provide its own checking. <p>
     *
     * @exception IllegalStateException If this instance is in a state that does
     *                                      not allow timer service method operations.
     **/
    // LI2281.07
    public abstract void checkTimerServiceAccess() throws IllegalStateException;

    // --------------------------------------------------------------------------
    //
    // Methods from EJBContext interface
    //
    // --------------------------------------------------------------------------

    /**
     * Obtain the <code>Identity</code> of the bean associated with
     * this <code>BeanO</code>. <p>
     */
    @Override
    @Deprecated
    public java.security.Identity getCallerIdentity() {
        EJSDeployedSupport s = EJSContainer.getMethodContext();

        // Method not allowed from ejbTimeout.                           LI2281.07
        if (s != null && s.methodInfo.ivInterface == MethodInterface.TIMED_OBJECT) {
            IllegalStateException ise = new IllegalStateException("getCallerIdentity() not " +
                                                                  "allowed from ejbTimeout");

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getCallerIdentity: " + ise);

            throw ise;
        }

        EJBSecurityCollaborator<?> securityCollaborator = container.ivSecurityCollaborator;
        if (securityCollaborator == null) {
            return null; // d740575
        }

        return getCallerIdentity(securityCollaborator, s);
    } // getCallerIdentity

    @Deprecated
    private <T> java.security.Identity getCallerIdentity(EJBSecurityCollaborator<T> collaborator, EJSDeployedSupport s) {
        @SuppressWarnings("unchecked")
        T uncheckedCookie = s == null ? null : (T) s.securityCookie;
        return collaborator.getCallerIdentity(home.beanMetaData, s, uncheckedCookie);
    }

    /**
     * Not implemented yet
     */
    @Override
    public Principal getCallerPrincipal() {
        EJBSecurityCollaborator<?> securityCollaborator = container.ivSecurityCollaborator;
        if (securityCollaborator == null) {
            return NullSecurityCollaborator.UNAUTHENTICATED;
        }

        return getCallerPrincipal(securityCollaborator, EJSContainer.getMethodContext());
    }

    private <T> Principal getCallerPrincipal(EJBSecurityCollaborator<T> collaborator, EJSDeployedSupport s) {
        @SuppressWarnings("unchecked")
        T uncheckedCookie = s == null ? null : (T) s.securityCookie;
        return collaborator.getCallerPrincipal(home.beanMetaData, s, uncheckedCookie);
    }

    /**
     * Return <code>EJBHome</code> instance associated with
     * this <code>BeanO</code>. <p>
     */
    @Override
    public EJBHome getEJBHome() {
        try {
            EJSWrapper wrapper = home.getWrapper().getRemoteWrapper();
            Object wrapperRef = container.getEJBRuntime().getRemoteReference(wrapper);

            // The EJB spec does not require us to narrow to the actual home
            // interface, but since we have to narrow to EJBHome anyway, we
            // might as well narrow to the specific interface.  It seems like it
            // might be an oversight anyway given that the spec does require
            // SessionContext.getEJBObject to narrow.
            return (EJBHome) PortableRemoteObject.narrow(wrapperRef, home.beanMetaData.homeInterfaceClass);
        } catch (IllegalStateException ise) { // d116480
            // FFDC not logged for this spec required scenario
            throw ise;
        } catch (Exception ex) {
            // p97440 - start of change

            // This should never happen. Something is wrong.
            // Need to throw a runtime exception since this is a
            // Java EE architected interface that does not allow a checked
            // exceptions to be thrown. So, throw ContainerEJBException.
            FFDCFilter.processException(ex, CLASS_NAME + ".getEJBHome", "522", this);
            ContainerEJBException ex2 = new ContainerEJBException("Failed to get the wrapper for home.", ex);

            Tr.error(tc, "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E", new Object[] { ex, ex2.toString() }); // d194031

            throw ex2;

            // p97440 - end of change
        }

    } // getEJBHome

    // d111408, d112442 - start of change
    /**
     * Return <code>EJBLocalHome</code> instance associated with
     * this <code>BeanO</code>. <p>
     */
    @Override
    public EJBLocalHome getEJBLocalHome() {
        EJSWrapperCommon wCommon = null; // d116480
        try {
            wCommon = home.getWrapper(); // d116337
        } catch (Exception ex) {
            // This is bad
            FFDCFilter.processException(ex, CLASS_NAME + ".getEJBLocalHome",
                                        "547", this);
            Tr.warning(tc, "FAILED_TO_GET_WRAPPER_FOR_HOME_CNTR0002W", //p111002.3
                       new Object[] { ex });
            return null; // d116480
        }
        return (EJBLocalHome) wCommon.getLocalObject(); // d116480
    } // getEJBLocalHome  d111408, d112442 - end of change

    /**
     * Get the environment properties associated with this
     * <code>BeanO</code>. <p>
     */

    @Override
    @Deprecated
    public Properties getEnvironment() {

        return home.getEnvironment();

    } // getEnvironment

    /**
     * Get user transaction object that bean can use to demarcate
     * transactions.
     */

    @Override
    public synchronized UserTransaction getUserTransaction() {

        // Only TX_BEAN_MANAGED beans are allowed to call this method

        throw new IllegalStateException();

    } // getUserTransaction

    /**
     * NOTE: This is not supported by the EJS and never will be.
     * It is expected to deprecated in a future revision of
     * the EJB spec.
     */

    @Override
    @Deprecated
    public boolean isCallerInRole(java.security.Identity id) {

        throw new UnsupportedOperationException(); // OK

    } // isCallerInRole

    /**
     * Subclass must implement and call the
     * isCallerInRole(roleName, bean) method in this class.
     */
    @Override
    abstract public boolean isCallerInRole(String roleName); //LIDB2617.11

    /**
     * Check whether caller is executing in a specified role.
     *
     * @param roleName is the name of the role to check.
     *
     * @param bean     instance if isCallerInRole is called from a business
     *                     method rather than from a container callback method (e.g. subclass
     *                     BeanO state indicates in a business method). If not in a
     *                     business method, then a null reference must be passed.
     *
     * @return boolean true if caller has specified role.
     */
    //LIDB2617.11 - added entire method.
    // Chanced EnterpriseBean to Object.                                d366807.1
    public boolean isCallerInRole(String roleName, Object bean) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "isCallerInRole, role = " + roleName
                         + " EJB = " + bean); //182011
        }

        // Check whether security is enabled or not. Note, the subclasses
        // do override this method to do additional processing and then they
        // call super.isCallerInRole which usually causes this code to
        // execute.  However, the subclass will throw IllegalStateException
        // (e.g. MessageDrivenBeanO) if not valid to call isCallerInRole.
        boolean inRole; //d444696.2
        EJBSecurityCollaborator<?> securityCollaborator = container.ivSecurityCollaborator;
        if (securityCollaborator == null) //d444696.2
        {
            // Security is disabled, so return false for 1.x and 2.x modules to
            // ensure pre EJB 3 applications see no behavior change. For EJB 3 modules
            // or later, return true so that we are consistent with web container.
            BeanMetaData bmd = home.beanMetaData;
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "isCallerInRole called with security disabled for EJB module version = "
                             + bmd.ivModuleVersion);
            }
            inRole = (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0);
        } else //d444696.2
        {
            // Pass null EJSDeployedSupport for callback methods.            d739835
            EJSDeployedSupport s = bean == null ? null : EJSContainer.getMethodContext();

            try {
                inRole = isCallerInRole(securityCollaborator, roleName, s);
            } catch (RuntimeException ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".isCallerInRole", "982", this);
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "isCallerInRole collaborator throwing", ex);
                throw ex;
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) //d444696.2
        {
            Tr.exit(tc, "isCallerInRole returning " + inRole);
        }
        return inRole; //d444696.2
    }

    private <T> boolean isCallerInRole(EJBSecurityCollaborator<T> collaborator, String roleName, EJSDeployedSupport s) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // Security is enabled, so we need to invoke SecurityCollaborator to
        // determine if the caller is in specified Role.
        //
        // The roleName parameter may be a link.  So we will look in the
        // ivRoleLinkMap HashMap in BeanMetaData.   If it turns out to be
        // a link then store the roleName parameter value as roleLink and
        // store the "real" roleName returned from the HashMap as roleName.
        // d366845.11.2
        String roleLink = null;

        // If no links exist then the Map will be null     //428146
        BeanMetaData bmd = home.beanMetaData;
        if (bmd.ivRoleLinkMap != null) {
            String tempRoleLink = home.beanMetaData.ivRoleLinkMap.get(roleName);
            // TODO - Don't insert empty string into the Map.
            if (!"".equals(tempRoleLink)) {
                roleLink = tempRoleLink;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Role Link Found = " + roleLink);
            }
        }

        @SuppressWarnings("unchecked")
        T uncheckedCookie = s == null ? null : (T) s.securityCookie;

        return collaborator.isCallerInRole(bmd, s, uncheckedCookie, roleName, roleLink);
    }

    /**
     * Get the status of the current transaction. <p>
     *
     * Invoking the getRollbackOnly method is disallowed in the bean
     * methods for which the container does not have a meaningful
     * transaction context. These are the methods that have the
     * NotSupported, Never, or Supports transaction attribute. <p>
     */
    @Override
    public boolean getRollbackOnly() {
        boolean rollbackOnly = false;
        ContainerTx tx = null;
        IllegalStateException ise = null;

        // -----------------------------------------------------------------------
        // getRollbackOnly is not allowed when the container does not have
        // a meaningful transaction context, which includes the tx attrs
        // NotSupported, Never, and Supports.
        // -----------------------------------------------------------------------

        // First, obtain the ContainerTx object, to insure the container has a
        // meaningful transaction context.
        tx = container.getCurrentContainerTx();

        // If there is not a transaction context, or it is for a Local Tran,
        // then the method is either NotSupported, Never, or Supports
        // (without an inherited global tran), so throw the exception
        // required by the EJB Specification.
        if (tx == null || !tx.isTransactionGlobal()) {
            ise = new IllegalStateException("getRollbackOnly can not be called " +
                                            "without a Transaction Context");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getRollbackOnly: " + ise);

            throw ise;
        }

        // Finally, even if there is a global transaction, getRollbackOnly
        // is not allowed for methods with the Supports tran attribute.
        // The tran attribute for the method may be obtained from the
        // method context (EJSDeployedSupport).                            d161124
        EJSDeployedSupport methodContext = EJSContainer.getMethodContext();

        // During commit processing, from a java client or timer, there may not
        // be a method context so, no need to check for Supports.          d177348
        if (methodContext != null) {
            EJBMethodInfoImpl methodInfo = methodContext.methodInfo;
            if (methodInfo.getTransactionAttribute() == TransactionAttribute.TX_SUPPORTS) {
                ise = new IllegalStateException("getRollbackOnly can not be called " +
                                                "from a TX SUPPORTS method");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getRollbackOnly: " + ise);

                throw ise;
            }
        }

        // Must return true if the transaction has been marked for rollback
        // by the enterprise bean itself, by other enterprise beans, or by
        // other components (outside of the EJB specification scope) of the
        // transaction processing infrastructure.                         d186801
        rollbackOnly = tx.getRollbackOnly() || tx.getGlobalRollbackOnly();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getRollbackOnly: " + rollbackOnly + ": " + this);

        return rollbackOnly;
    } // getRollbackOnly

    /**
     * Mark the current global transaction for rollback. <p>
     *
     * Invoking the setRollbackOnly method is disallowed in the bean
     * methods for which the container does not have a meaningful
     * transaction context. These are the methods that have the
     * NotSupported, Never, or Supports transaction attribute. <p>
     */
    @Override
    public void setRollbackOnly() {
        ContainerTx tx = null;
        IllegalStateException ise = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setRollbackOnly: " + this);

        // -----------------------------------------------------------------------
        // setRollbackOnly is not allowed when the container does not have
        // a meaningful transaction context, which includes the tx attrs
        // NotSupported, Never, and Supports.
        // -----------------------------------------------------------------------

        // First, obtain the ContainerTx object, to insure the container has a
        // meaningful transaction context.
        tx = container.getCurrentContainerTx();

        // If there is not a transaction context, or it is for a Local Tran,
        // then the method is either NotSupported, Never, or Supports
        // (without an inherited global tran), so throw the exception
        // required by the EJB Specification.
        if (tx == null || !tx.isTransactionGlobal()) {
            ise = new IllegalStateException("setRollbackOnly can not be called " +
                                            "without a Transaction Context");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "setRollbackOnly: " + ise);

            throw ise;
        }

        // Finally, even if there is a global transaction, setRollbackOnly
        // is not allowed for methods with the Supports tran attribute.
        // The tran attribute for the method may be obtained from the
        // method context (EJSDeployedSupport).                            d161124
        EJSDeployedSupport methodContext = EJSContainer.getMethodContext();

        // During commit processing, from a java client or timer, there may not
        // be a method context so, no need to check for Supports.          d177348
        if (methodContext != null) {
            // d161124 setRollbackOnly can not be called from supports methods
            EJBMethodInfoImpl methodInfo = methodContext.methodInfo;
            if (methodInfo.getTransactionAttribute() == TransactionAttribute.TX_SUPPORTS) {
                ise = new IllegalStateException("setRollbackOnly can not be called " +
                                                "from a TX SUPPORTS method");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "setRollbackOnly: " + ise);

                throw ise;
            }
            // d161124

            // If the method on the current bean is the beginner of the
            // transaction, then this needs to be recorded, so that
            // postInvoke can avoid throwing an exception if the bean does
            // not throw an exception; per the EJB Spec.                    d186801
            if (tx.beganInThisScope()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "setRollbackOnly called by beginner");
                methodContext.ivBeginnerSetRollbackOnly = true;
            }
        }

        tx.setRollbackOnly();
    }

    /**
     * Get access to the EJB Timer Service. <p>
     *
     * @return The EJB Timer Service.
     *
     * @exception IllegalStateException The Container throws the exception
     *                                      if the instance is not allowed to use this method (e.g. if the bean
     *                                      is a stateful session bean)
     **/
    // LI2281
    @Override
    public TimerService getTimerService() throws IllegalStateException {
        // The BeanO is the Timer Service object. That is how the specific
        // bean instance is tied to any created Timers. Subclasses must
        // override to check for IllegalStateException situations.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            // EJB Spec does allow the timer service to be obtained even if the
            // bean does not have timers, but add a trace point, as that is an odd
            // thing to do...
            if (home.beanMetaData.timedMethodInfos == null) // F743-506
            {
                Tr.debug(tc, "getTimerService: ***** Does not have timers *****");
            }

            Tr.debug(tc, "getTimerService: " + this);
        }

        return this;
    }

    /**
     * Lookup a resource within the component's private naming context. <p>
     *
     * @param name Name of the entry (relative to java:comp/env).
     *
     * @throws IllegalArgumentException - The Container throws the exception
     *                                      if the given name does not match an entry within the
     *                                      component's environment.
     **/
    // New for EJB 3.0    d366807.1
    @Override
    public Object lookup(String name) {
        // Note: this context method is allowed from all bean methods,
        //       except the constructor... which has not way to access
        //       the context.  Therefore, no 'state' checking needs
        //       to be performed... just validate the parameter.
        if (name == null)
            throw new IllegalArgumentException("null 'name' parameter.");

        Object result = null;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "lookup: " + name);

        // Previously, this method used Naming for comp/env, which tolerated the
        // 'name' starting with 'java:comp/env/', but that is NOT part of the
        // name in our map.                                                d473811
        String lookupName = name;
        if (name.startsWith("java:comp/env/")) // F743-609 remove useless null check
        {
            lookupName = name.substring(14);
        }

        // -----------------------------------------------------------------------
        // Rather than perform a lookup using Naming, the InjectionBinding
        // that was created during populateJavaNameSpace will be located
        // and used to obtain/create the result object, just like it
        // would be done for injection.                                    d473811
        // -----------------------------------------------------------------------

        InjectionBinding<?> binding = home.beanMetaData.ivJavaColonCompEnvMap.get(lookupName);

        if (binding != null) {
            try {
                result = binding.getInjectionObject();
            } catch (InjectionException ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".lookup",
                                            "1342", this);
                IllegalArgumentException iae = new IllegalArgumentException("Failure occurred obtaining object for " + name +
                                                                            " reference defined for " + home.beanMetaData.j2eeName, ex);

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "lookup: " + iae);

                throw iae;
            }
        } else {
            result = container.getEJBRuntime().javaColonLookup(name, home);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "lookup: " + result.getClass().getName());

        return result;
    }

    // RTC19920
    /**
     * Returns the context data associated with this invocation or lifecycle
     * callback. If there is no context data, an empty Map object will be
     * returned.
     **/
    // F743-21028
    @Override
    public Map<String, Object> getContextData() {
        return EJSContainer.getThreadData().getContextData(); // d644886
    }

    // --------------------------------------------------------------------------
    //
    // Methods from TimerService interface                           // LI2281.07
    //
    // --------------------------------------------------------------------------

    /**
     * Create a single-action timer that expires after a specified duration. <p>
     *
     * @param duration The number of milliseconds that must elapse before
     *                     the timer expires.
     * @param info     Application information to be delivered along with the
     *                     timer expiration notification. This can be null.
     *
     * @return The newly created Timer.
     *
     * @exception IllegalArgumentException If duration is negative.
     * @exception IllegalStateException    If this method is invoked while the
     *                                         instance is in a state that does not allow access to this method.
     * @exception EJBException             If this method fails due to a system-level failure.
     **/
    // LI2281.07
    @Override
    public Timer createTimer(long duration, Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createTimer: " + duration + ": " + info, this);

        // Bean must implement TimedObject interface or have a timeout method to create a timer.
        if (!home.beanMetaData.isTimedObject) {
            IllegalStateException ise;

            ise = new IllegalStateException("Timer Service: Bean does not " +
                                            "implement TimedObject: " +
                                            beanId);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + ise);

            throw ise;
        }

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        // Make sure the arguments are valid....
        if (duration < 0) {
            IllegalArgumentException iae;

            iae = new IllegalArgumentException("TimerService: duration not " +
                                               "a valid value: " + duration);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + iae);

            throw iae;
        }

        // Expiration is just the current time + the specified duration.
        long current = System.currentTimeMillis();
        Date expiration = new Date(current + duration);

        // Now create the Timer, which will also create the Task in the
        // Scheduler Service. Only EJBExceptions will be thrown.         LI2281.11
        Timer timer = container.getEJBRuntime().createTimer(this, expiration, -1, null, info, true); // F743-13022

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createTimer: " + timer);

        return timer;
    }

    /**
     * Create an interval timer whose first expiration occurs after a specified
     * duration, and whose subsequent expirations occur after a specified
     * interval. <p>
     *
     * @param initialDuration  The number of milliseconds that must elapse
     *                             before the first timer expiration notification.
     * @param intervalDuration The number of milliseconds that must elapse
     *                             between timer expiration notifications.
     *                             Expiration notifications are scheduled relative
     *                             to the time of the first expiration. If expiration
     *                             is delayed(e.g. due to the interleaving of other
     *                             method calls on the bean) two or more expiration
     *                             notifications may occur in close succession to
     *                             "catch up".
     * @param info             Application information to be delivered along with the timer
     *                             expiration notification. This can be null.
     *
     * @return The newly created Timer.
     *
     * @exception IllegalArgumentException If initialDuration is negative, or
     *                                         intervalDuration is negative.
     * @exception IllegalStateException    If this method is invoked while the
     *                                         instance is in a state that does not allow access to this method.
     * @exception EJBException             If this method fails due to a system-level failure.
     **/
    // LI2281.07
    @Override
    public Timer createTimer(long initialDuration, long intervalDuration,
                             Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createTimer: " + initialDuration + ", " +
                         intervalDuration + ": " + info,
                     this);

        // Bean must implement TimedObject interface or have a timeout method to create a timer.
        if (!home.beanMetaData.isTimedObject) {
            IllegalStateException ise;

            ise = new IllegalStateException("Timer Service: Bean does not " +
                                            "implement TimedObject: " +
                                            beanId);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + ise);

            throw ise;
        }

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        IllegalArgumentException iae = null;

        // Make sure the arguments are valid....
        if (initialDuration < 0) {
            iae = new IllegalArgumentException("TimerService: initialDuration not " +
                                               "a valid value: " + initialDuration);
        } else if (intervalDuration < 0) {
            iae = new IllegalArgumentException("TimerService: intervalDuration not " +
                                               "a valid value: " + intervalDuration);
        }

        if (iae != null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + iae);

            throw iae;
        }

        // Expiration is just the current time + the specified duration.
        long current = System.currentTimeMillis();
        Date expiration = new Date(current + initialDuration);

        // Now create the Timer, which will also create the Task in the
        // Scheduler Service. Only EJBExceptions will be thrown.         LI2281.11
        Timer timer = container.getEJBRuntime().createTimer(this, expiration, intervalDuration, null, info, true); // F743-13022

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createTimer: " + timer);

        return timer;
    }

    /**
     * Create a single-action timer that expires at a given point in time. <p>
     *
     * @param expiration The point in time at which the timer must expire.
     * @param info       Application information to be delivered along with the timer
     *                       expiration notification. This can be null.
     *
     * @return The newly created Timer.
     *
     * @exception IllegalArgumentException If expiration is null, or
     *                                         expiration.getTime() is negative.
     * @exception IllegalStateException    If this method is invoked while the
     *                                         instance is in a state that does not allow access to this method.
     * @exception EJBException             If this method fails due to a system-level failure.
     **/
    // LI2281.07
    @Override
    public Timer createTimer(Date expiration, Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createTimer: " + expiration + ": " + info, this);

        // Bean must implement TimedObject interface or have a timeout method to create a timer.
        if (!home.beanMetaData.isTimedObject) {
            IllegalStateException ise;

            ise = new IllegalStateException("Timer Service: Bean does not " +
                                            "implement TimedObject: " +
                                            beanId);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + ise);

            throw ise;
        }

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        // Make sure the arguments are valid....
        if (expiration == null ||
            expiration.getTime() < 0) {
            IllegalArgumentException iae;

            iae = new IllegalArgumentException("TimerService: expiration not " +
                                               "a valid value: " + expiration);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + iae);

            throw iae;
        }

        // Now create the Timer, which will also create the Task in the
        // Scheduler Service. Only EJBExceptions will be thrown.         LI2281.11
        Timer timer = container.getEJBRuntime().createTimer(this, expiration, -1, null, info, true); // F743-13022

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createTimer: " + timer);

        return timer;
    }

    /**
     * Create an interval timer whose first expiration occurs at a given
     * point in time and whose subsequent expirations occur after a
     * specified interval. <p>
     *
     * @param initialExpiration The point in time at which the first timer
     *                              expiration must occur.
     * @param intervalDuration  The number of milliseconds that must elapse
     *                              between timer expiration notifications.
     *                              Expiration notifications are scheduled relative
     *                              to the time of the first expiration. If expiration
     *                              is delayed(e.g. due to the interleaving of other
     *                              method calls on the bean) two or more expiration
     *                              notifications may occur in close succession to
     *                              "catch up".
     * @param info              Application information to be delivered along with the timer
     *                              expiration notification. This can be null.
     *
     * @return The newly created Timer.
     *
     * @exception IllegalArgumentException If initialExpiration is null, or
     *                                         initialExpiration.getTime() is negative, or intervalDuration
     *                                         is negative.
     * @exception IllegalStateException    If this method is invoked while the
     *                                         instance is in a state that does not allow access to this method.
     * @exception EJBException             If this method fails due to a system-level failure.
     **/
    // LI2281.07
    @Override
    public Timer createTimer(Date initialExpiration, long intervalDuration,
                             Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createTimer: " + initialExpiration + ", " +
                         intervalDuration + ": " + info,
                     this);

        // Bean must implement TimedObject interface or have a timeout method to create a timer.
        if (!home.beanMetaData.isTimedObject) {
            IllegalStateException ise;

            ise = new IllegalStateException("Timer Service: Bean does not " +
                                            "implement TimedObject: " +
                                            beanId);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + ise);

            throw ise;
        }

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        IllegalArgumentException iae = null;

        // Make sure the arguments are valid....
        if (initialExpiration == null ||
            initialExpiration.getTime() < 0) {
            iae = new IllegalArgumentException("TimerService: initialExpiration not " +
                                               "a valid value: " + initialExpiration);
        } else if (intervalDuration < 0) {
            iae = new IllegalArgumentException("TimerService: intervalDuration not " +
                                               "a valid value: " + intervalDuration);
        }

        if (iae != null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + iae);

            throw iae;
        }

        // Now create the Timer, which will also create the Task in the
        // Scheduler Service. Only EJBExceptions will be thrown.         LI2281.11
        Timer timer = container.getEJBRuntime().createTimer(this, initialExpiration, intervalDuration, null, info, true); // F743-13022

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createTimer: " + timer);

        return timer;
    }

    /**
     * Get all the active timers associated with this bean. <p>
     *
     * @return A collection of javax.ejb.Timer objects. Cannot return null,
     *         may return an empty collection.
     *
     * @exception IllegalStateException If this method is invoked while the
     *                                      instance is in a state that does not allow access to this method.
     * @exception EJBException          If this method fails due to a system-level failure.
     **/
    // LI2281.07
    @Override
    public Collection<Timer> getTimers() // F743-425.1
                    throws IllegalStateException, EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // F743-425.CodRev

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getTimers: " + this);

        // Bean must be capable of having timers to call this API.
        if (home.beanMetaData.timedMethodInfos == null) // F743-506
        {
            IllegalStateException ise;

            ise = new IllegalStateException("Timer Service: Bean does not " +
                                            "have timers: " +
                                            beanId);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "getTimers: " + ise);

            throw ise;
        }

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        Collection<Timer> timers = container.getEJBRuntime().getTimers(this); // F743-13022

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getTimers: " + timers);

        return timers;
    }

    /**
     * Returns all active timers associated with the beans in the same module
     * in which the caller bean is packaged. These include both the
     * programmatically-created timers and the automatically-created timers. <p>
     *
     * @return a collection of javax.ejb.Timer objects.
     *
     * @exception IllegalStateException If this method is invoked while the
     *                                      instance is in a state that does not allow access to this method.
     * @exception EJBException          If this method fails due to a system-level failure.
     **/
    public Collection<Timer> getAllTimers() throws IllegalStateException, EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getAllTimers: " + this);

        // Note, this bean does not need to have a timeout callback, as this
        // method will return timers for all beans in the module.

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        Collection<Timer> timers = container.getEJBRuntime().getAllTimers(beanId.getBeanMetaData()._moduleMetaData);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getAllTimers: " + timers);

        return timers;
    }

    // --------------------------------------------------------------------------
    //
    // Methods from EJBCacheControl interface
    //
    // --------------------------------------------------------------------------

    /**
     * Flush the persistent state of all entity EJB instances that have
     * been modified in the current transaction. <p>
     *
     * @exception RemoteException is thrown if an error occurs while trying
     *                                to flush the EJB cache.
     */
    @Deprecated
    public void flush() throws RemoteException {
        container.flush();
    } // flush

    // --------------------------------------------------------------------------
    //
    // Methods from EJBContextExtension interface
    //
    // --------------------------------------------------------------------------

    /**
     * Returns true when the current thread is associated with a global
     * transaction; otherwise, returns false. <p>
     *
     *
     * See EJBContextExtension.isTransactionGlobal() for details. <p>
     **/
    @Override
    public boolean isTransactionGlobal() {
        ContainerTx tx = container.getCurrentContainerTx();
        boolean isGlobal = (tx == null) ? false : tx.isTransactionGlobal();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isTransactionGlobal : " + isGlobal);

        return isGlobal;
    }

    // --------------------------------------------------------------------------
    //
    // Methods from InjectionTargetContext interface                     F49213.1
    //
    // --------------------------------------------------------------------------

    @Override
    public <T> T getInjectionTargetContextData(Class<T> data) {
        if (data.isAssignableFrom(getClass()))
            return data.cast(this);

        // Return null if requested context data is not available
        return null;
    }

    // --------------------------------------------------------------------------
    //
    // End Interface Methods
    //
    // --------------------------------------------------------------------------

    /**
     * Obtains the InjectionEngine for the current EJB Runtime.
     */
    // F73338
    InjectionEngine getInjectionEngine() {
        return container.getEJBRuntime().getInjectionEngine();
    }

    /**
     * Gets the handle list associated with this bean, optionally creating one
     * if the bean does not have a handle list yet.
     *
     * @param create true if a handle list should be created if the bean does
     *                   not already have a handle list
     */
    HandleList getHandleList(boolean create) // d662032
    {
        if (connectionHandleList == null && create) {
            connectionHandleList = new HandleList();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getHandleList: created " + connectionHandleList);
        }

        return connectionHandleList;
    }

    /**
     * Reassociates handles in the handle list associated with this bean, and
     * returns a handle list to be pushed onto the thread handle list stack.
     *
     * @return the handle list to push onto the thread stack
     */
    HandleListInterface reAssociateHandleList() // d662032
                    throws CSIException {
        HandleListInterface hl;

        if (connectionHandleList == null) {
            hl = HandleListProxy.INSTANCE;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "reAssociateHandleList: " + connectionHandleList);
            hl = connectionHandleList;

            try {
                hl.reAssociate();
            } catch (Exception ex) {
                throw new CSIException("", ex);
            }
        }

        return hl;
    }

    /**
     * Parks handles in the handle list associated with this bean.
     */
    void parkHandleList() // d662032
    {
        if (connectionHandleList != null) {
            final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "parkHandleList: " + connectionHandleList);

            try {
                connectionHandleList.parkHandle();
            } catch (Exception ex) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "parkHandleList: exception", ex);
            }
        }
    }

    /**
     * Destroy the handle list associated with this bean if necessary.
     */
    protected final void destroyHandleList() // d662032
    {
        if (connectionHandleList != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "destroyHandleList: destroying " + connectionHandleList);
            connectionHandleList.componentDestroyed();
            connectionHandleList = null;
        }
    }

    //115597
    /**
     * This method was added so that the Activator has and interface to get the
     * state
     */
    public synchronized int getState() {
        return state;
    }

    //115597

    /**
     * This method is called by the optc and optb activators to make sure that
     * load is called before a store for optimistic concurrency cases.
     *
     * @param tx ContainerTx for the current transaction.
     */
    // d126506
    public abstract void ensurePersistentState(ContainerTx tx) // d139352-2
                    throws RemoteException;

    // LIDB2775-23.1

    protected BeanOCallDispatchToken callDispatchEventListeners(int dispatchEventCode,
                                                                BeanOCallDispatchToken token) {
        DispatchEventListenerManager dispatchEventListenerManager = container.ivDispatchEventListenerManager; // d646413.2
        DispatchEventListenerCookie[] dispatchEventListenerCookies = null;
        EJBMethodMetaData methodMetaData = null;
        BeanOCallDispatchToken retToken = null;
        boolean doBeforeDispatch = false;
        boolean doAfterDispatch = false;

        // first check if listeners are active, if not skip everything else ...
        if (dispatchEventListenerManager != null &&
            dispatchEventListenerManager.dispatchEventListenersAreActive()) // @539186C, d646413.2
        {
            final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
            if (isTraceOn && tc.isEntryEnabled())
                Tr.entry(tc, "callDispatchEventListeners: " + dispatchEventCode + ", " + this);

            // if one of the "before" callbacks
            //
            // Build temporary EJBMethodInfo object to represent this callback method.
            // If no dispatch cookies assigned to this bean, create new cookie array.
            // If no dispatch context on thread, this is not method dispatch, but rather
            // is end of tran processing.
            //
            if (dispatchEventCode == DispatchEventListener.BEFORE_EJBACTIVATE ||
                dispatchEventCode == DispatchEventListener.BEFORE_EJBLOAD ||
                dispatchEventCode == DispatchEventListener.BEFORE_EJBSTORE ||
                dispatchEventCode == DispatchEventListener.BEFORE_EJBPASSIVATE) {
                methodMetaData = buildTempEJBMethodMetaData(dispatchEventCode, home.getBeanMetaData());
                retToken = new BeanOCallDispatchToken(); // return value to communicate between "before" and "after" call
                retToken.setMethodMetaData(methodMetaData); // save away methodMetaData object for "after" call

                // d646413.2 - Check if a dispatch context was already created
                // for this bean by EJSContainer.preInvoke.
                EJSDeployedSupport s = EJSContainer.getMethodContext();
                if (s != null && s.beanO == this) // d646413.2
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "using dispatch context from method context");
                    // use cookie array already assigned to bean for "after" callback
                    dispatchEventListenerCookies = s.ivDispatchEventListenerCookies; // @MD11200.6A
                }

                if (dispatchEventListenerCookies == null) {
                    // create new cookie array - @MD11200.6A
                    dispatchEventListenerCookies = dispatchEventListenerManager.getNewDispatchEventListenerCookieArray(); // @MD11200.6A
                    doBeforeDispatch = true; // must drive beforeDispatch to collect cookies from event listeners - // @MD11200.6A
                    retToken.setDoAfterDispatch(true); /*
                                                        * since doing beforeDispatch on "before" callback, must issue afterDispatch
                                                        * during "after" callback
                                                        */
                }

                // save cookie array in token for "after" call
                retToken.setDispatchEventListenerCookies(dispatchEventListenerCookies); // @MD11200.6A
            }
            // else if one of the "after" callbacks
            //
            // get methodInfo and dispatch cookies from input token
            // plus check "afterDispatch" flag to see if beforeDispatch was issued
            // when "before" callback was done and if we now have to do afterDispatch
            // call to finish up
            else if (dispatchEventCode == DispatchEventListener.AFTER_EJBACTIVATE ||
                     dispatchEventCode == DispatchEventListener.AFTER_EJBLOAD ||
                     dispatchEventCode == DispatchEventListener.AFTER_EJBSTORE ||
                     dispatchEventCode == DispatchEventListener.AFTER_EJBPASSIVATE) {
                methodMetaData = token.getMethodMetaData(); // @MD11200.6A
                doAfterDispatch = token.getDoAfterDispatch(); // @MD11200.6A d621610
                dispatchEventListenerCookies = token.getDispatchEventListenerCookies(); // @MD11200.6A
            }

            if (doBeforeDispatch) { // @MD11200.6A
                dispatchEventListenerManager.callDispatchEventListeners(DispatchEventListener.BEGIN_DISPATCH, dispatchEventListenerCookies, methodMetaData); // @MD11200.6A
            } // @MD11200.6A

            dispatchEventListenerManager.callDispatchEventListeners(dispatchEventCode, dispatchEventListenerCookies, methodMetaData); // @MD11200.6A

            if (doAfterDispatch) { // @MD11200.6A
                dispatchEventListenerManager.callDispatchEventListeners(DispatchEventListener.END_DISPATCH, dispatchEventListenerCookies, methodMetaData); // @MD11200.6A
            } // @MD11200.6A
            if (isTraceOn && tc.isEntryEnabled()) { // @MD11200.6A
                Tr.exit(tc, "callDispatchEventListeners", retToken); // @MD11200.6A
            } // @MD11200.6A

        } // if listeners are active
        return retToken;
    } // callDispatchEventListeners

    /**
     * Create a temporary EJBMethodMetaData object. This method is used by the callDispatchEventListeners method to
     * create a 'fake' EJBMethodMetaData object to report framework methods (activate/load/store/passivate) to the
     * DispatchEventListeners.
     */

    private EJBMethodMetaData buildTempEJBMethodMetaData(int dispatchEventCode, BeanMetaData bmd) {
        String methName = "";
        String methSig = "";
        switch (dispatchEventCode) {
            case DispatchEventListener.BEFORE_EJBACTIVATE:
                methName = "ejbActivate";
                methSig = "ejbActivate:";
                break;
            case DispatchEventListener.BEFORE_EJBLOAD:
                methName = "ejbLoad";
                methSig = "ejbLoad:";
                break;
            case DispatchEventListener.BEFORE_EJBSTORE:
                methName = "ejbStore";
                methSig = "ejbStore:";
                break;
            case DispatchEventListener.BEFORE_EJBPASSIVATE:
                methName = "ejbPassivate";
                methSig = "ejbPassivate:";
                break;
            default:
                Tr.error(tc, "Unsupported dispatchEventCode code passed to buildTempEJBMethodInfo - code = " + dispatchEventCode);
                break;
        }

        EJBMethodInfoImpl methodInfo = bmd.createEJBMethodInfoImpl(bmd.container.getEJBRuntime().getMetaDataSlotSize(MethodMetaData.class));
        methodInfo.initializeInstanceData(methSig, methName, bmd, MethodInterface.REMOTE, TransactionAttribute.TX_NOT_SUPPORTED, false);
        methodInfo.setMethodDescriptor("");
        return methodInfo;
    } // buildTempEJBMethodInfo

    //add stubs for new Java EE 6 methods   d560195.3

    /**
     * Create a calendar-based timer based on the input schedule expression.
     *
     * @param schedule A schedule expression describing the timeouts for this
     *                     timer.
     *
     * @return The newly created Timer.
     *
     * @exception IllegalArgumentException If Schedule represents an invalid
     *                                         schedule expression.
     * @exception IllegalStateException    If this method is invoked while the
     *                                         instance is in a state that does not allow access to this
     *                                         method.
     * @exception EJBException             If this method could not complete due to a
     *                                         system-level failure.
     */
    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule) {
        return createCalendarTimer(schedule, null); // F743-500
    }

    /**
     * Create a calendar-based timer based on the input schedule expression.
     *
     * @param schedule    A schedule expression describing the timeouts for this
     *                        timer.
     * @param timerConfig Timer configuration.
     *
     * @return The newly created Timer.
     *
     * @exception IllegalArgumentException If Schedule represents an invalid
     *                                         schedule expression.
     * @exception IllegalStateException    If this method is invoked while the
     *                                         instance is in a state that does not allow access to this
     *                                         method.
     * @exception EJBException             If this method could not complete due to a
     *                                         system-level failure.
     */
    // F7437591.codRev
    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig) {
        Serializable info = timerConfig == null ? null : timerConfig.getInfo();
        boolean persistent = timerConfig == null || timerConfig.isPersistent();

        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createCalendarTimer: " + persistent, this);

        // Bean must implement TimedObject interface or have a timeout method to create a timer.
        if (!home.beanMetaData.isTimedObject) {
            IllegalStateException ise = new IllegalStateException("Timer Service: Bean does not implement TimedObject: " + beanId);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createCalendarTimer: " + ise);
            throw ise;
        }

        if (home.beanMetaData.isEntityBean()) // d595255
        {
            IllegalStateException ise = new IllegalStateException("Timer Service: Entity beans cannot use calendar-based timers: " + beanId);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createCalendarTimer: " + ise);
            throw ise;
        }

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        // Make sure the arguments are valid....
        if (schedule == null) {
            IllegalArgumentException ise = new IllegalArgumentException("TimerService: schedule not a valid value: null");

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createCalendarTimer: " + ise);
            throw ise;
        }

        Timer timer = container.getEJBRuntime().createTimer(this, null, -1, schedule, info, persistent); // F743-13022

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createCalendarTimer: " + timer);
        return timer;
    }

    /**
     * Create an interval timer whose first expiration occurs at a given
     * point in time and whose subsequent expirations occur after a
     * specified interval. <p>
     *
     * @param initialExpiration The point in time at which the first timer
     *                              expiration must occur.
     * @param intervalDuration  The number of milliseconds that must elapse
     *                              between timer expiration notifications.
     *                              Expiration notifications are scheduled relative
     *                              to the time of the first expiration. If expiration
     *                              is delayed(e.g. due to the interleaving of other
     *                              method calls on the bean) two or more expiration
     *                              notifications may occur in close succession to
     *                              "catch up".
     * @param timerConfig       Wrapper of application information to be delivered along with the timer
     *                              expiration notification. Has indication of persistent vs NP.
     *
     * @return The newly created Timer.
     *
     * @exception IllegalArgumentException If initialExpiration is null, or
     *                                         initialExpiration.getTime() is negative, or intervalDuration
     *                                         is negative.
     * @exception IllegalStateException    If this method is invoked while the
     *                                         instance is in a state that does not allow access to this method.
     * @exception EJBException             If this method fails due to a system-level failure.
     **/
    @Override
    public Timer createIntervalTimer(Date initialExpiration,
                                     long intervalDuration,
                                     TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // F743-425.CodRev

        boolean persistent = (timerConfig == null ? true : timerConfig.isPersistent());
        Serializable info = (timerConfig == null ? (Serializable) null : timerConfig.getInfo());

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "createIntervalTimer: " + initialExpiration + ", " +
                         intervalDuration + ": " + info + ", " + persistent,
                     this); // F743-425.1
        }

        // Bean must implement TimedObject interface or have a timeout method to create a timer.
        if (!home.beanMetaData.isTimedObject) {

            IllegalStateException ise;

            ise = new IllegalStateException("Timer Service: Bean does not " +
                                            "implement TimedObject: " +
                                            beanId);
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "createIntervalTimer: " + ise); // F743-425.1
            }

            throw ise;

        }

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        IllegalArgumentException iae = null;

        // Make sure the arguments are valid....
        if (initialExpiration == null ||
            initialExpiration.getTime() < 0) {
            iae = new IllegalArgumentException("TimerService: initialExpiration not " +
                                               "a valid value: " + initialExpiration);
        } else if (intervalDuration < 0) {
            iae = new IllegalArgumentException("TimerService: intervalDuration not " +
                                               "a valid value: " + intervalDuration);
        }

        if (iae != null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createIntervalTimer: " + iae); // F743-425.1

            throw iae;
        }

        Timer timer = container.getEJBRuntime().createTimer(this, initialExpiration, intervalDuration, null, info, persistent); // F743-13022

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createIntervalTimer: " + timer); // F743-425.1

        return timer;
    }

    /**
     * Create an interval timer whose first expiration occurs after a specified
     * duration, and whose subsequent expirations occur after a specified
     * interval. <p>
     *
     * @param initialDuration  The number of milliseconds that must elapse
     *                             before the first timer expiration notification.
     * @param intervalDuration The number of milliseconds that must elapse
     *                             between timer expiration notifications.
     *                             Expiration notifications are scheduled relative
     *                             to the time of the first expiration. If expiration
     *                             is delayed(e.g. due to the interleaving of other
     *                             method calls on the bean) two or more expiration
     *                             notifications may occur in close succession to
     *                             "catch up".
     * @param timerConfig      Wrapper of application information to be delivered along with the timer
     *                             expiration notification. Has indication of persistent vs NP.
     *
     * @return The newly created Timer.
     *
     * @exception IllegalArgumentException If initialDuration is negative, or
     *                                         intervalDuration is negative.
     * @exception IllegalStateException    If this method is invoked while the
     *                                         instance is in a state that does not allow access to this method.
     * @exception EJBException             If this method fails due to a system-level failure.
     **/
    @Override
    public Timer createIntervalTimer(long initialDuration,
                                     long intervalDuration,
                                     TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {

        boolean persistent = (timerConfig == null ? true : timerConfig.isPersistent());
        Serializable info = (timerConfig == null ? (Serializable) null : timerConfig.getInfo());

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "createIntervalTimer: " + initialDuration + ", " + // F743-425.1
                         intervalDuration + ": " + info,
                     this);
        }

        // Bean must implement TimedObject interface or have a timeout method to create a timer.
        if (!home.beanMetaData.isTimedObject) {
            IllegalStateException ise;

            ise = new IllegalStateException("Timer Service: Bean does not " +
                                            "implement TimedObject: " +
                                            beanId);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createIntervalTimer: " + ise); // F743-425.1

            throw ise;
        }

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        IllegalArgumentException iae = null;

        // Make sure the arguments are valid....
        if (initialDuration < 0) {
            iae = new IllegalArgumentException("TimerService: initialDuration not " +
                                               "a valid value: " + initialDuration);
        } else if (intervalDuration < 0) {
            iae = new IllegalArgumentException("TimerService: intervalDuration not " +
                                               "a valid value: " + intervalDuration);
        }

        if (iae != null) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "createIntervalTimer: " + iae); // F743-425.1
            }

            throw iae;
        }

        // Expiration is just the current time + the specified duration.
        long current = System.currentTimeMillis();
        Date expiration = new Date(current + initialDuration);

        Timer timer = container.getEJBRuntime().createTimer(this, expiration, intervalDuration, null, info, persistent); // F743-13022

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "createIntervalTimer: " + timer); // F743-425.1
        }

        return timer;

    }

    /**
     * Create a single-action timer that expires at a given point in time. <p>
     *
     * @param expiration  The point in time at which the timer must expire.
     * @param timerConfig Wrapper of application information to be delivered along with the timer
     *                        expiration notification. Has indication of persistent vs NP.
     *
     * @return The newly created Timer.
     *
     * @exception IllegalArgumentException If expiration is null, or
     *                                         expiration.getTime() is negative.
     * @exception IllegalStateException    If this method is invoked while the
     *                                         instance is in a state that does not allow access to this method.
     * @exception EJBException             If this method fails due to a system-level failure.
     **/
    @Override
    public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {

        boolean persistent = (timerConfig == null ? true : timerConfig.isPersistent());
        Serializable info = (timerConfig == null ? (Serializable) null : timerConfig.getInfo());

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "createSingleActionTimer: " + expiration + ": " + info, this); // F743-425.1
        }

        // Bean must implement TimedObject interface or have a timeout method to create a timer.
        if (!home.beanMetaData.isTimedObject) {
            IllegalStateException ise;

            ise = new IllegalStateException("Timer Service: Bean does not " +
                                            "implement TimedObject: " +
                                            beanId);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + ise);

            throw ise;
        }

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        // Make sure the arguments are valid....
        if (expiration == null ||
            expiration.getTime() < 0) {
            IllegalArgumentException iae;

            iae = new IllegalArgumentException("TimerService: expiration not " +
                                               "a valid value: " + expiration);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + iae);

            throw iae;
        }

        Timer timer = container.getEJBRuntime().createTimer(this, expiration, -1, null, info, persistent); // F743-13022

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "createSingleActionTimer: " + timer);
        }

        return timer;

    }

    /**
     * Create a single-action timer that expires after a specified duration. <p>
     *
     * @param duration    The number of milliseconds that must elapse before
     *                        the timer expires.
     * @param timerConfig Wrapper of application information to be delivered along with the timer
     *                        expiration notification. Has indication of persistent vs NP.
     *
     * @return The newly created Timer.
     *
     * @exception IllegalArgumentException If duration is negative.
     * @exception IllegalStateException    If this method is invoked while the
     *                                         instance is in a state that does not allow access to this method.
     * @exception EJBException             If this method fails due to a system-level failure.
     **/
    @Override
    public Timer createSingleActionTimer(long duration, TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {

        boolean persistent = (timerConfig == null ? true : timerConfig.isPersistent());
        Serializable info = (timerConfig == null ? (Serializable) null : timerConfig.getInfo());

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "createSingleActionTimer: " + duration + ": " + info, this); // F743-425.1
        }

        // Bean must implement TimedObject interface or have a timeout method to create a timer.
        if (!home.beanMetaData.isTimedObject) {
            IllegalStateException ise;

            ise = new IllegalStateException("Timer Service: Bean does not " +
                                            "implement TimedObject: " +
                                            beanId);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + ise);

            throw ise;
        }

        // Determine if this bean is in a state that allows timer service
        // method access - throws IllegalStateException if not allowed.
        checkTimerServiceAccess();

        // Make sure the arguments are valid....
        if (duration < 0) {
            IllegalArgumentException iae;

            iae = new IllegalArgumentException("TimerService: duration not " +
                                               "a valid value: " + duration);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createTimer: " + iae);

            throw iae;
        }

        // Expiration is just the current time + the specified duration.
        long current = System.currentTimeMillis();
        Date expiration = new Date(current + duration);

        Timer timer = container.getEJBRuntime().createTimer(this, expiration, -1, null, info, persistent); // F743-13022

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "createSingleActionTimer: " + timer);
        }

        return timer;

    }

} // BeanO
