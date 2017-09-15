/*******************************************************************************
 * Copyright (c) 1998, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.RemoteException;
import javax.ejb.Handle;

import com.ibm.ejs.container.activator.ActivationStrategy;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.J2EEName;

/**
 * <code>HomeInternal</code> provides Home methods which are internal to
 * the container. Right now, this interface is used to unify deployed
 * home EJBs and the "home of homes", which the container uses to track
 * all installed enterprise beans.
 */
public interface HomeInternal {
    /**
     * Get the JNDI of this home. <p>
     */
    public String getJNDIName(Object homeKey);//89554

    /**
     * Get the Java EE name of this home. <p>
     */
    //89554
    public J2EEName getJ2EEName();

    /**
     * Get the id of the home bean. <p>
     */
    public BeanId getId();

    /**
     * Get a wrapper for this home. <p>
     */
    public EJSWrapperCommon getWrapper() // f111627
    throws CSIException, RemoteException;

    /**
     * Get a wrapper for the given BeanId <p>
     */
    public EJSWrapperCommon getWrapper(BeanId id) // f111627
    throws CSIException, RemoteException;

    /**
     * This method creates and returns a new <code>BeanO</code> instance
     * appropriate for this home. <p>
     * 
     * The returned <code>BeanO</code> has a newly created enterprise
     * bean instance associated with it, and the enterprise bean instance
     * has had its set...Context() method called on it to set its context
     * to the returned <code>BeanO</code>. <p>
     * 
     * This method must only be called when a new <code>BeanO</code>
     * instance is needed. It always creates a new <code>BeanO</code>
     * instance and a new instance of the associated enterprise bean. <p>
     * 
     * If this method returns successfully, then {@link EJBThreadData#pushCallbackBeanO} will have been called, and the
     * caller must ensure that {@link EJBThreadData#popCallbackBeanO} is
     * eventually called. <p>
     * 
     * @param threadData the <code>EJBThreadData</code> associated with the
     *            currently running thread
     * @param id the <code>BeanId</code> to associate with the newly
     *            created <code>BeanO</code> <p>
     * @param tx the <code>ContainerTx</code> to associate with the newly
     *            created <code>BeanO</code> <p>
     * 
     * @return newly created <code>BeanO</code> associated with a newly
     *         created bean instance of type of beans managed by this
     *         home <p>
     */
    // Added ContainerTx d168509
    public BeanO createBeanO(EJBThreadData threadData, ContainerTx tx, BeanId id) // d630940
    throws RemoteException;

    /**
     * Create wrapper instance of the type of wrappers managed by this
     * home. <p>
     * 
     * This method provides a wrapper factory capability.
     * 
     * @param id The <code>BeanId</code> to associate with the wrapper
     * 
     * @return <code>EJSWrapper</code> instance whose most specific
     *         type is the type of wrappers managed by this home <p>
     * 
     */
    public EJSWrapperCommon internalCreateWrapper(BeanId id) // f111627
    throws javax.ejb.CreateException, RemoteException, CSIException;

    /**
     * Return true if this home contains stateless session beans. <p>
     */
    public boolean isStatelessSessionHome();

    /**
     * Return true if this home contains stateful session beans. <p>
     */
    public boolean isStatefulSessionHome();

    /**
     * Return true if this home contains Message Driven Beans <p>
     */
    public boolean isMessageDrivenHome(); // d121554

    /**
     * Return the BeanMetaData object for beans associated with this
     * home. <p>
     */
    public BeanMetaData getBeanMetaData(Object homeKey);

    /**
     * Return the ActivationStrategy for beans associated with this
     * home. <p>
     */
    public ActivationStrategy getActivationStrategy();

    /**
     * Get method name for the the give method id on the home identified
     * by the give home name.
     */
    public String getMethodName(Object homeKey, int id, boolean isHome);

    /**
     * Return the name of the class that implements the beans owned
     * by this home.
     */
    public String getEnterpriseBeanClassName(Object homeKey);

    /**
     * Create a Handle for the given BeanId
     */
    public Handle createHandle(BeanId id) throws RemoteException;

    /**
     * Get the class loader for a home
     */
    public ClassLoader getClassLoader();

    /**
     * Return true if this home contains singleton session bean. <p>
     */
    public boolean isSingletonSessionHome(); // d565527
}
