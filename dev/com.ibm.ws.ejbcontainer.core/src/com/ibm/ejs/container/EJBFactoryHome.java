/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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

import javax.ejb.CreateException;
import javax.ejb.Handle;

import com.ibm.ejs.container.activator.ActivationStrategy;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * EJBFactoryHome acts as the Home of the HomeOfHomes (or EJBFactory). <p>
 *
 * Beginning with EJB 3.0, the HomesOfHomes is now exposed as an EJBFactory
 * to support 'ejb-link' and 'auto-link'. EJBFactory is the interface,
 * EJBFactoryImpl is the Wrapper, and HomeOfHomes is the factory itself. <p>
 *
 * Client access to the EJBFactory is achieved by binding it into the global
 * name space, once for each application, and once for each module; to insure
 * proper WLM routing. The EJBLink factory on the client will lookup the
 * appropriate EJBFactory binding. <p>
 *
 * Although all bindings map to the single HomeOfHomes, each binding must
 * have a unique key (BeanId) so that it may be mapped to the correct
 * cluster. To accomplish this, a special BeanId is used, where the
 * J2EEName is the identifier "__homeOfHomes#__homeOfHomes#__EJBFactory"
 * and the primary key is the J2EEName of either the application or
 * module. <p>
 *
 * To rest of EJBContainer and the ORB, the EJBFactoryHome and EJBFactoryImpl
 * look much like the Home and Wrapper of a Stateless Session bean. But,
 * unlike other Homes, the HomeOfHomes has not BeanMetaData, so the home,
 * as well as the wrapper (EJBFactoryImpl) are not fully functional. <p>
 *
 * Most of the methods on this Home will fail, and are never expected to
 * be called. The only purpose of this home is to allow incoming ORB
 * requests to map the IOR to an EJBFactory wrapper (EJBFactoryImpl). <p>
 */
public final class EJBFactoryHome implements HomeInternal
{
    private static final TraceComponent tc =
                    Tr.register(EJBFactoryHome.class, "EJBContainer",
                                "com.ibm.ejs.container.container");

    private final EJSContainer ivContainer;
    private final EJBLinkResolver ivEJBLinkResolver;

    /**
     * Special EJBFactory J2EEName : "__homeOfHomes#__homeOfHomes#__EJBFactory"
     **/
    protected final J2EEName ivJ2eeName;

    /**
     * Constructor that is intended for use by the HomeOfHomes. Only a single
     * instance of this Home should ever be needed/created.
     **/
    EJBFactoryHome(EJSContainer container,
                   HomeOfHomes homeOfHomes,
                   J2EEName j2eeName,
                   J2EENameFactory j2eeNameFactory)
    {
        ivContainer = container;
        ivJ2eeName = j2eeName;
        ivEJBLinkResolver = new EJBLinkResolver().initialize(homeOfHomes, j2eeNameFactory);
    }

    /**
     * Get the JNDI of this home. <p>
     **/
    @Override
    public String getJNDIName(Object homeKey)
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

    /**
     * Get the Java EE name of the EJBFactory Home. <p>
     */
    @Override
    public J2EEName getJ2EEName()
    {
        return ivJ2eeName;
    }

    /**
     * Get the id of the home bean. <p>
     **/
    @Override
    public BeanId getId()
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

    /**
     * Get a wrapper for this home. <p>
     **/
    @Override
    public EJSWrapperCommon getWrapper()
                    throws CSIException, RemoteException
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

    /**
     * Get a wrapper for the given BeanId <p>
     **/
    @Override
    public EJSWrapperCommon getWrapper(BeanId id)
                    throws CSIException, RemoteException
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

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
     * @param threadData the <code>EJBThreadData</code> associated with the
     *            currently running thread <p>
     * @param tx the <code>ContainerTx</code> to associate with the newly
     *            created <code>BeanO</code> <p>
     * @param id the <code>BeanId</code> to associate with the newly
     *            created <code>BeanO</code> <p>
     *
     * @return newly created <code>BeanO</code> associated with a newly
     *         created bean instance of type of beans managed by this
     *         home <p>
     */
    @Override
    public BeanO createBeanO(EJBThreadData threadData, ContainerTx tx, BeanId id)
                    throws RemoteException
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

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
     **/
    @Override
    public EJSWrapperCommon internalCreateWrapper(BeanId beanId)
                    throws CreateException, RemoteException, CSIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "internalCreateWrapper : " + beanId);

        // -----------------------------------------------------------------------
        // Creating the wrappers for an EJBFactory is fairly simple; there is
        // only one remote wrapper, which is really just a wrapper around the
        // HomeOfHomes.
        //
        // A different instance of the wrapper class (EJBFactory) is requred
        // for every binding, to insure it is associated with the correct
        // cluster, for proper WLM routing.  The 'primary key' of an EJBFactory
        // is the J2EEName of either the applicaton or module, and is used
        // to obtain the proper cluster identity.
        //
        // A special EJSWrapperCommon constructor is used, that processes
        // just the one wrapper, and avoids the requirement of passing
        // BeanMetaData.  There really is no BMD for the EJBFactory, as it
        // just represents the HomeOfHomes.
        // -----------------------------------------------------------------------

        EJBFactoryImpl ejbFactory = new EJBFactoryImpl(ivEJBLinkResolver);
        J2EEName factoryKey = (J2EEName) beanId.getPrimaryKey();

        Object cluster = ivContainer.getEJBRuntime().getClusterIdentity(factoryKey);

        EJSWrapperCommon wrappers = new EJSWrapperCommon(ejbFactory,
                        beanId,
                        cluster,
                        ivContainer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "internalCreateWrapper : " + wrappers);

        return wrappers;
    }

    /**
     * Return true if this home contains stateless session beans. <p>
     **/
    @Override
    public boolean isStatelessSessionHome()
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

    /**
     * Return true if this home contains stateful session beans. <p>
     **/
    @Override
    public boolean isStatefulSessionHome()
    {
        return false;
    }

    /**
     * Return true if this home contains Message Driven Beans <p>
     **/
    @Override
    public boolean isMessageDrivenHome()
    {
        return false;
    }

    /**
     * Return the BeanMetaData object for beans associated with this
     * home. <p>
     **/
    @Override
    public BeanMetaData getBeanMetaData(Object homeKey)
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

    /**
     * Return the ActivationStrategy for beans associated with this
     * home. <p>
     **/
    @Override
    public ActivationStrategy getActivationStrategy()
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

    /**
     * Get method name for the the given method id on the home identified
     * by the give home name.
     **/
    @Override
    public String getMethodName(Object homeKey, int id, boolean isHome)
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

    /**
     * Return the name of the class that implements the beans owned
     * by this home.
     **/
    @Override
    public String getEnterpriseBeanClassName(Object homeKey)
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

    /**
     * Create a Handle for the given BeanId.
     **/
    @Override
    public Handle createHandle(BeanId id) throws RemoteException
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

    /**
     * Get the class loader for a home.
     **/
    @Override
    public ClassLoader getClassLoader()
    {
        throw new ContainerEJBException("EJBContainer internal error");
    }

    /**
     * Return true iff this home contains singleton session bean. <p>
     */
    @Override
    public final boolean isSingletonSessionHome() //d565527
    {
        return false;
    }
}
