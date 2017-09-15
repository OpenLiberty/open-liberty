/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.jndi;

import static com.ibm.ws.jpa.container.osgi.jndi.JPAJndiLookupInfoRefAddr.Addr_Type;

import java.security.AccessController;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ejs.util.dopriv.SystemGetPropertyPrivileged;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAComponent;
import com.ibm.ws.jpa.JPALookupDelegate;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * Factory to create the EntityManagerFactory and EntityManager objects from JNDI
 * lookup for @PersistenceUnit and @PersistenceContext annotations respectively.
 */
@Component(service = { ObjectFactory.class, JPAJndiLookupObjectFactory.class })
@SuppressWarnings("deprecation")
public class JPAJndiLookupObjectFactory implements ObjectFactory {
    private static final String CLASS_NAME = JPAJndiLookupObjectFactory.class.getName();

    private static final TraceComponent tc = Tr.register(JPAJndiLookupObjectFactory.class,
                                                         "JPA", // d416151.3.1
                                                         "com.ibm.ws.jpa.jpa"); // d658638

    public static final String USE_EMF_PROXY = "com.ibm.websphere.persistence.useEntityManagerFactoryProxy"; // d706751

    @SuppressWarnings("unchecked")
    private static final boolean USE_EMF_PROXY_VALUE = Boolean.parseBoolean((String) AccessController.doPrivileged(
                    new SystemGetPropertyPrivileged(USE_EMF_PROXY, "true"))); // d706751

    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "useEntityManagerFactoryProxy = " + USE_EMF_PROXY_VALUE);
    }

    private final AtomicReference<JPALookupDelegate> lookupDelegate = new AtomicReference<JPALookupDelegate>();

    // Allow subclasses to override the class name used in parameter validation
    protected String ivInstanceClassName = CLASS_NAME;

    /**
     * Null constructor.
     */
    public JPAJndiLookupObjectFactory() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>");
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
     */
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> env)
                    throws Exception {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // -----------------------------------------------------------------------
        // Is obj a Reference?
        // -----------------------------------------------------------------------
        if (!(obj instanceof Reference)) {
            return null;
        }

        Reference ref = (Reference) obj;

        // -----------------------------------------------------------------------
        // Is the right factory for this reference?
        // -----------------------------------------------------------------------
        if (!ivInstanceClassName.equals(ref.getFactoryClassName())) {
            return null;
        }

        // -----------------------------------------------------------------------
        // Is address null?
        // -----------------------------------------------------------------------
        RefAddr addr = ref.get(Addr_Type);
        if (addr == null) {
            NamingException nex = new NamingException("The address for this Reference is empty (null)");
            throw nex;
        }

        // -----------------------------------------------------------------------
        // Reference has the right factory and non empty address, so it is OK
        // to generate the EntityManager or EntityManagerFactory object now.
        // -----------------------------------------------------------------------

        Object retObj = null;

        JPAJndiLookupInfo info = (JPAJndiLookupInfo) addr.getContent();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "getting EntityManagerFactory for " + info);

        JPAPuId puId = info.getPuId(); // d424494
        J2EEName j2eeName = info.getJ2EEName(); // d510184
        String refName = info.getReferenceName(); // d510184
        boolean isSFSB = checkSFSBAccess(info, info.isSFSB()); // F743-30682
        JPAComponent jpaService = JPAAccessor.getJPAComponent();

        //Check to see see if we can delegate to the OSGi-aware JPA container
        JPALookupDelegate delegate = lookupDelegate.get();

        // If we find a delegate then call it to see if it can provide the Object
        if (delegate != null) {
            // Call the right method
            if (info.isFactory())
                retObj = delegate.getEntityManagerFactory(puId.getPuName(), j2eeName);
            else
                retObj = delegate.getEntityManager(puId.getPuName(), j2eeName, info.isExtendedContextType(), info.getPersistenceProperties());
        }

        if (retObj == null) {
            if (info.isFactory()) {
                retObj = jpaService.getEntityManagerFactory(puId,
                                                            j2eeName,
                                                            USE_EMF_PROXY_VALUE || isSFSB); // d706751
            } else {
                Map<?, ?> pCtxtProperties = info.getPersistenceProperties();
                retObj = jpaService.getEntityManager(puId,
                                                     j2eeName,
                                                     refName,
                                                     info.isExtendedContextType(),
                                                     info.isUnsynchronized(),
                                                     pCtxtProperties);

            }
        }

        return retObj;
    }

    /**
     * Checks access to the specified JPA reference and returns true if the
     * current call to {@link #getObjectInstance} is in the context of a
     * Stateful Session bean. <p>
     * 
     * By default, this method will return what is stored in the info object as
     * passed by the isSFSB parameter, which will be the correct answer for EJB
     * modules and WAR modules that do not contain EJBs. <p>
     * 
     * This method should be overridden for scenarios where it is unclear
     * at the time the info object was created or when additional validation
     * needs to be performed; when EJBs are defined in WAR modules. <p>
     * 
     * @param info the information associated with the current object creation
     * @param isSFSB <tt>true</tt> if the context is an SFSB
     * @return true if the object is being created in a Stateful bean context
     */
    // d658638
    protected boolean checkSFSBAccess(JPAJndiLookupInfo info, boolean isSFSB)
                    throws InjectionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "checkSFSBAccess: " + info + ", isSFSB=" + isSFSB);

        if (!isSFSB && !info.isFactory() && info.isExtendedContextType()) // F743-30682
        {
            JPAPuId puId = info.getPuId();
            Tr.error(tc, "EXTEND_PC_NOT_IN_SFSB_CWWJP0003E", puId.getPuName() );
            throw new InjectionException("CWWJP0003E: The " + puId.getPuName() + " extended" +
                                         " persistence context can be initiated within the scope" +
                                         " of a stateful session bean only.");
        }

        return isSFSB;
    }

    @org.osgi.service.component.annotations.Reference(service = JPALookupDelegate.class,
                                                      cardinality = ReferenceCardinality.OPTIONAL,
                                                      policy = ReferencePolicy.DYNAMIC)
    protected void setLookupDelegate(JPALookupDelegate ref) {
        lookupDelegate.set(ref);
    }

    protected void unsetLookupDelegate(JPALookupDelegate ref) {
        lookupDelegate.lazySet(null);
    }
}
