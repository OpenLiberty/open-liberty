/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi;

import java.lang.reflect.Method;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.RemoveException;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ejbcontainer.EJBReference;
import com.ibm.ws.ejbcontainer.EJBReferenceFactory;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.ejbcontainer.WSEJBEndpointManager;

public interface EJBContainer {
    /**
     * Create a module metadata for an EJB-in-WAR module.
     *
     * @param moduleInfo the WAR module info
     * @return the EJB module metadata, or null if no EJBs are found
     * @throws MetaDataException if an exception occurs
     */
    ModuleMetaData createEJBInWARModuleMetaData(ModuleInfo moduleInfo) throws MetaDataException;

    /**
     * Populates the ReferenceContext for an EJB-in-WAR module. This must happen
     * after the moduleMetaDataCreated event.
     *
     * @param mmd
     * @throws MetaDataException
     */
    void populateEJBInWARReferenceContext(ModuleInfo moduleInfo, ModuleMetaData mmd) throws MetaDataException;

    /**
     * Starts the EJBs in a WAR.
     *
     * @param mmd the metadata returned from {@link #createEJBInWARModuleMetaData}
     * @param container the module container
     */
    void startEJBInWARModule(ModuleMetaData mmd, Container container) throws StateChangeException;

    /**
     * Notification that an EJB-in-WAR module has started.
     *
     * @param mmd the metadata returned from {@link #createEJBInWARModuleMetaData}
     * @param container the module container
     */
    void startedEJBInWARModule(ModuleMetaData mmd, Container container) throws StateChangeException;

    /**
     * Stops the EJBs in a WAR.
     *
     * @param mmd the metadata returned from {@link #createEJBInWARModuleMetaData}
     * @param container the module container
     */
    void stopEJBInWARModule(ModuleMetaData mmd, Container container);

    /**
     * Starts a system EJB module. System EJBs do not have java:global bindings
     * or MBeans, and no messages are printed when the module is started or
     * stopped (the caller must print any messages). System EJBs are not
     * integrated with EE services such as injection or JPA. System EJBs should
     * not rely on the context class loader or other services that rely on the
     * context class loader. System EJBs do not have support for
     * container-managed security; the EJBs must implement perform role checking
     * manually as necessary. System EJBs always use container-managed
     * transactions. System EJB modules are EJB 3 modules, but they always use
     * RMIC-compatible stubs and ties.
     *
     * @param moduleName the name of the module (e.g., "MyEJB.jar"), which must
     *            be unique in the server, is part of the persistent identity of
     *            references to the EJBs in this module, and must not be changed
     *            or else serialized proxies to EJBs will no longer work
     * @param classLoader the module class loader, which must have visibility to
     *            the following packages:
     *            <ul>
     *            <li>com.ibm.ejs.container
     *            <li>org.omg.CORBA
     *            <li>org.omg.CORBA.portable
     *            <li>org.omg.CORBA_2_3.portable
     *            <li>org.omg.PortableServer
     *            <li>org.omg.PortableServer.POAPackage
     *            <li>javax.rmi.CORBA
     *            </ul>
     * @param ejbs the configuration for the EJBs in this module
     * @param ejbRemoteRuntime reference to the EJBRemoteRuntime service
     * @return a system module handle, which must be stopped when the system
     *         EJBs should no longer be accessible
     */
    EJBSystemModule startSystemModule(String moduleName,
                                      ClassLoader classLoader,
                                      EJBSystemBeanConfig[] ejbs,
                                      EJBRemoteRuntime ejbRemoteRuntime);

    /**
     * Returns the binding context for the currently active extended-scoped
     * persistence context for the thread of execution. Null will be returned
     * if an extended-scoped persistence context is not currently active. <p>
     *
     * @return binding context for currently active extended-scoped
     *         persistence context.
     */
    public Object getExPcBindingContext();

    /**
     * Returns a WSEJBEndpointManager instance which may be used to establish
     * the proper EJB environment to invoke a WebService Endpoint method,
     * associated with the specified stateless session bean or singleton bean. <p>
     *
     * A new Endpoint Manager instance must be obtained for every
     * WebService Endpoint request. Endpoint Manager instances are
     * NOT thread safe. <p>
     *
     * @param name is the J2EEName of the stateless session bean or singleton bean.
     * @param methods are the methods that must be implemented by the
     *            bean, and supported on the returned endpoint manager.
     *
     * @return Web Service endpoint manager for the EJB indicated by the
     *         specified J2EEName, supporting the specified methods.
     *
     * @exception EJBConfigurationException is thrown if the EJB does not
     *                implement the required methods or the methods violate
     *                the EJB Specification, or the bean is not a Stateless
     *                session EJB or singleton bean.
     * @exception EJBException if a unexpected Throwable occurred that
     *                prevented this method from generating the bean reference.
     *                Use the getCause method to recover unexpected Throwable
     *                that occurred.
     **/
    public WSEJBEndpointManager createWebServiceEndpointManager(J2EEName name,
                                                                Method[] methods)
                    throws EJBException, EJBConfigurationException;

    /**
     * Returns a WSEJBEndpointManager instance which may be used to establish
     * the proper EJB environment to invoke a WebService Endpoint Provider
     * method, associated with the specified stateless session bean or singleton bean. <p>
     *
     * A new Endpoint Manager instance must be obtained for every
     * WebService Endpoint Provider request. Endpoint Manager instances
     * are NOT thread safe. <p>
     *
     * @param name is the J2EEName of the stateless session bean or singleton bean.
     * @param provider javax.xml.ws.Provider.class if the bean has been
     *            annotated {@code @WebServiceProvider}. Must be null if
     *            methods is not null.
     *
     * @return Web Service endpoint manager for the EJB indicated by the
     *         specified J2EEName, supporting the specified Provider interface.
     *
     * @exception EJBConfigurationException is thrown if the EJB does not
     *                implement the required methods or the methods violate
     *                the EJB Specification, or the bean is not a Stateless
     *                session EJB or singleton bean.
     * @exception EJBException if a unexpected Throwable occurred that
     *                prevented this method from generating the bean reference.
     *                Use the getCause method to recover unexpected Throwable
     *                that occurred.
     **/
    public WSEJBEndpointManager createWebServiceEndpointManager(J2EEName name,
                                                                Class<?> provider)
                    throws EJBException, EJBConfigurationException;

    /**
     * Returns an aggregate reference for all of the bean's business local
     * interfaces, including the no-interface view (if present). <p>
     *
     * Only session beans support an aggregate local reference. <p>
     *
     * Caution: When using the returned aggregate reference, the SessionContext
     * method getInvokedBusinessInterface() will not function properly
     * for methods that are common to multiple local interfaces. An
     * IllegalStateException will be thrown. <p>
     *
     * @param beanName is the unique J2EEName of the bean.
     * @param context the context for creating the object, or null
     * @return an aggregate reference for all of the beans business local
     *         interfaces, including the no-interface view.
     * @throws CreateException if an application-level failure occurs creating
     *             an instance of the bean.
     * @throws EJBNotFoundException if the specified bean cannot be found; the
     *             application may have been stopped or has not been installed.
     * @throws EJBException if the bean is not a session bean, has no business
     *             local interfaces or a failure occurs attempting to generate
     *             the aggregate wrapper class or create an instance of it.
     * @deprecated Use {@link EJBReferenceFactory} instead
     */
    @Deprecated
    public Object createAggregateLocalReference(J2EEName beanName, ManagedObjectContext context)
                    throws CreateException, EJBNotFoundException;

    /**
     * Removes the specified stateful session bean. <p>
     *
     * True is returned if the bean is successfully removed, and false is returned
     * if the bean has already been removed. An exception will occur if the bean
     * exists but cannot be removed. <p>
     *
     * @param bean reference to a stateful session bean
     *
     * @throws RemoteException when the bean cannot be removed (for example, in use).
     * @throws RemoveException if the specified object is not a stateful bean
     *             reference or the bean cannot be removed.
     *
     * @return true if removed, false if previously removed
     * @deprecated Use {@link EJBReference#remote} instead
     */
    @Deprecated
    public boolean removeStatefulBean(Object bean) throws RemoteException, RemoveException;

}
