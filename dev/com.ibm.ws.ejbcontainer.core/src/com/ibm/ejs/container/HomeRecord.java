/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.csi.J2EEName;

/**
 * Simple data structure to store all data related to a home instance
 * needed for initialization.
 */
public class HomeRecord
{
    protected BeanO beanO; //LIDB859-4
    protected volatile HomeInternal homeInternal; //LIDB859-4, d648522
    protected BeanMetaData bmd; //LIDB859-4
    protected EJBModuleMetaDataImpl ejbMmd; //LIDB859-4
    protected HomeOfHomes homeOfHomes; //LIDB859-4
    protected J2EEName j2eeName; //LIDB859-4
    protected int cmpVersion; //LIDB859-4
    protected String connFactoryName; //LIDB859-4
    protected String ejbName; //LIDB859-4
    protected ClassLoader classLoader; //d200714
    protected int beanType;

    /**
     * Cached references to helper classes used to generate appropriate binding names
     */
    protected BindingsHelper ivRemoteBindingsHelper = null;
    protected BindingsHelper ivLocalBindingsHelper = null;
    public final List<String> ivJavaGlobalBindings = new ArrayList<String>(); // F743-26137
    public final List<String> ivJavaAppBindings = new ArrayList<String>(); // d660700

    /**
     * Beans associated with this class of enterprise bean. This hashtable
     * contains a list of beans which are children of this bean.
     */
    public Hashtable<String, HomeRecord> childBeans = new Hashtable<String, HomeRecord>(); // d366845.3

    /**
     * True iff this bean has inheritance (i.e. child beans).
     **/
    public boolean ivHasInheritance = false;

    public boolean ivIsChild = false;

    /**
     * Set to true when Short Default Bindings are enabled for this EJB type.
     **/
    // d457053
    private Boolean ivShortDefaultBindingsEnabled = null;

    //LIDB859-4
    public HomeRecord(BeanMetaData bmd,
                      HomeOfHomes homeOfHomes) //d200714
    {
        this.beanO = null;
        this.homeInternal = null;
        this.bmd = bmd;
        this.ejbMmd = bmd._moduleMetaData;
        this.homeOfHomes = homeOfHomes;
        this.j2eeName = bmd.j2eeName;
        this.cmpVersion = bmd.cmpVersion;
        this.connFactoryName = bmd.connFactoryName;
        this.ejbName = bmd.enterpriseBeanName;
        this.classLoader = bmd.classLoader; //d200714
        this.beanType = bmd.type;
    }

    public boolean hasInheritance()
    {
        return ivHasInheritance;
    }

    /**
     * getAppName returns the <code>String</code> that represents
     * the name of the application that this EJB resides in.
     * 
     * @return <code>String</code> representing the name of the Application
     */
    //d390389
    public String getAppName()
    {
        return this.bmd.j2eeName.getApplication(); // d642679
    }

    /**
     * getTargetHome returns the <code>EJSHome</code> associated with the indicated
     * child bean.
     * 
     * @param child String representing the child bean to be found.
     * 
     * @return <code>EJSHome</code> associated with the child HomeRecord.
     */
    public EJSHome getTargetHome(String child)
    {
        EJSHome targetHome = null;
        HomeRecord targetHomeRecord = childBeans.get(child);
        if (targetHomeRecord == null) {
            //try recursively
            Enumeration<HomeRecord> children = childBeans.elements();
            while (children.hasMoreElements()) {
                HomeRecord childHomeRecord = children.nextElement();
                targetHome = childHomeRecord.getTargetHome(child);
                if (targetHome != null)
                    break;
            }
        }
        // Get the EJSHome from the homeRecord.  If the home has not been
        // initialized yet, call HomeOfHomes.getHome() to trigger it.
        if (targetHomeRecord != null) {
            targetHome = targetHomeRecord.getHomeAndInitialize(); // d648522
        }
        return targetHome;
    }

    /**
     * getBeanMetaData returns the bean metadata for this <code>HomeRecord</code>.
     * 
     * @return <code>BeanMetaData</code> associated with this HomeRecord.
     */
    public BeanMetaData getBeanMetaData() {
        return bmd;
    }

    /**
     * getJ2EEName returns the Java EE name of the EJB associated with this <code>HomeRecord</code>.<p>
     * 
     * @return <code>J2EEName</code> associated with this HomeRecord.
     */
    public J2EEName getJ2EEName() {
        return j2eeName;
    }

    /**
     * getJndiName returns the jndiname of the EJB associated with this <code>HomeRecord</code>.<p>
     * 
     * @return <code>String</code> representing the jndiName associated with this HomeRecord.
     */
    public String getJndiName() {
        return bmd.getJndiName();
    }

    /**
     * getJndiName returns the jndiname of the EJB associated with this <code>HomeRecord</code>.
     * This method is only needed because HomeRecord implements PMHomeInfo. <p>
     * 
     * @param pkey <code>Object</code> representing the PrimaryKey... however it is not used.
     * 
     * @return <code>String</code> representing the jndiName associated with this HomeRecord.
     */
    public String getJNDIName(Object pkey)
    {
        return bmd.getJndiName();
    } // getJNDIName

    /**
     * Return the cmp version associated with this home record.<p>
     */
    public int getCmpVersion() {
        return cmpVersion;
    }

    /**
     * Return the name of the connection factory associated with this home record.<p>
     */
    public String getConnFactoryName() {
        return connFactoryName;
    }

    /**
     * getHome() is called to return the <code>EJSHome</code> reference
     * stored in this HomeRecord. <p>
     * 
     * @return <code>EJSHome</code> associated with this HomeRecord.
     **/
    //199071
    public EJSHome getHome() {
        EJSHome result = (EJSHome) homeInternal;
        return result;
    }

    /**
     * getHomeAndInitialize() is called to return the <code>EJSHome</code> reference
     * stored in this HomeRecord. If this reference is null, then the initialization
     * of this EJB has been deferred. In that case the EJB initialization will be triggered
     * from this method.
     * <p>
     * 
     * @return <code>EJSHome</code> associated with this HomeRecord.
     */
    public EJSHome getHomeAndInitialize() // d648522
    {
        // For performance, use double-checked locking with volatile.
        EJSHome result = (EJSHome) homeInternal; // volatile
        if (result == null)
        {
            EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;
            EJBApplicationMetaData ejbAMD = mmd.getEJBApplicationMetaData();
            ejbAMD.checkIfEJBWorkAllowed(mmd);

            // We must lock on an application-wide object because common archive
            // is not thread-safe.
            synchronized (ejbAMD)
            {
                result = (EJSHome) homeInternal;
                if (result == null)
                {
                    // homeInternal is set by EJSContainer.startBean.
                    result = bmd.container.getEJBRuntime().initializeDeferredEJB(this);
                }
            }
        }

        return result;
    }

    /**
     * Return the metadata of the module associated with this home record.<p>
     */
    public EJBModuleMetaDataImpl getEJBModuleMetaData()
    {
        return this.ejbMmd;
    }

    /**
     * Return the name of the EJB associated with this home record.<p>
     */
    public String getEJBName()
    {
        return this.ejbName;
    }

    /**
     * Return the application classloader associated with this home record.<p>
     */
    // d200714
    public ClassLoader getClassLoader()
    {
        return this.classLoader;
    }

    /**
     * Retrun bean type for this Home
     */
    public int getBeanType()
    {
        return this.beanType;
    }

    /**
     * Return string representation of this HomeRecord. <p>
     * 
     * Overridden to improve trace.
     */
    // d196581.1
    @Override
    public String toString()
    {
        String initialized = (homeInternal == null) ? "deferred" : "initialized";

        return ("HomeRecord(" +
                j2eeName + ", " +
                bmd.getJndiName() + ", " +
                initialized + ")");
    }

    /**
     * Provides a mechanism for reading the system property for disabling short
     * form default bindings. <p>
     * 
     * com.ibm.websphere.ejbcontainer.disableShortDefaultBindings <p>
     * 
     * This property can be used to identify applications for which Short form
     * default jndi bindings are to be disabled.
     * 
     * @return true if short default interface bindings are enabled, and false
     *         if the property indicated they were to be disabled for
     *         the specified application name.
     **/
    // d457053
    public boolean shortDefaultBindingsEnabled()
    {
        if (ivShortDefaultBindingsEnabled == null)
        {
            ivShortDefaultBindingsEnabled = BindingsHelper.shortDefaultBindingsEnabled(getAppName()); // d642679
        }

        return ivShortDefaultBindingsEnabled.booleanValue();
    }

    /**
     * Returns true if this bean should be bound into at least one of the
     * java: name spaces (excluding java:comp) introduced in Java EE 1.6 :
     * <ul>
     * <li> java:global
     * <li> java:app
     * <li> java:module
     * </ul>
     */
    // F743-34301
    public boolean bindToJavaNameSpaces()
    {
        // bind all session beans and named managed beans
        return ((bmd.isSessionBean() &&
                 bmd._moduleMetaData.getEJBApplicationMetaData().isBindToJavaGlobal() && // F743-33812CdRv
        bmd.enterpriseBeanName.indexOf('/') == -1) || // d724614
        (bmd.isManagedBean() && !bmd.enterpriseBeanName.startsWith("$")));
    }

    /**
     * Returns true if this bean should be bound into the java:global name
     * space. This method should only be called if bindToJavaNameSpaces has
     * already returned true.
     */
    public boolean bindToJavaGlobalNameSpace() // d660700
    {
        return !bmd.isManagedBean();
    }

    /**
     * Returns true if this bean shold be bound into the java:global, java:app,
     * and java:module namespace under "Bean!pkg.Interface".
     */
    public boolean bindInterfaceNames() // d660700
    {
        return !bmd.isManagedBean();
    }

    /**
     * Returns true if this bean should be bound into the WebSphere "traditional"
     * global name space and/or "ejblocal" name space.
     */
    // F743-34301
    public boolean bindToContextRoot()
    {
        // All bean types except managed beans are bound here
        return !bmd.isManagedBean() &&
               (!bmd.isSessionBean() ||
               bmd._moduleMetaData.getEJBApplicationMetaData().isBindToServerRoot()); // F743-33812CdRv
    }

} // HomeRecord
