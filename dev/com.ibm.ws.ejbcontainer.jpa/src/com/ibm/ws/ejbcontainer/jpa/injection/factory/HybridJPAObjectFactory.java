/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jpa.injection.factory;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.jpa.JPALookupDelegate;
import com.ibm.ws.jpa.container.osgi.jndi.JPAJndiLookupInfo;
import com.ibm.ws.jpa.container.osgi.jndi.JPAJndiLookupObjectFactory;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
* Subclass of JPAJndiLookupObjectFactory which checks the thread context
* to determine if the object creation is being performed in the context of
* a stateful session bean. <p>
*
* The default behavior is to determine whether the Persistence Unit or
* Context was declared by a stateful session bean at metadata processing
* time. However, for EJBs in Wars, any EJB in the war may lookup a PU or
* PC declared by any other EJB in the WAR... so the determination of which
* bean type is creating the EnityManager must be determine dynamically. <p>
*/
public class HybridJPAObjectFactory extends JPAJndiLookupObjectFactory
{
    private static final TraceComponent tc = Tr.register(HybridJPAObjectFactory.class);
    
    public HybridJPAObjectFactory()
    {
       if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() )
          Tr.debug(tc, "HybridJPAObjectFactory.<init>");
       
       // Override class name used in parameter validation
       ivInstanceClassName = HybridJPAObjectFactory.class.getName();
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
     * This method is overridden here to support the EJBs in War module scenario.
     * In this scenario, the type of EJB using a persistence context cannot
     * be determined until the time of injection or lookup. At that time,
     * the EJB type may be determined from the CMD on the thread.
     *
     * @param info the information associated with the current object creation
     * @return true if the object is being created in a Stateful bean context
     * @throws InjectionException if an invalid access is detected
     */
    @Override
    protected boolean checkSFSBAccess( JPAJndiLookupInfo info, boolean isSFSB )
          throws InjectionException
    {
       ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
    
       final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
       if (isTraceOn && tc.isEntryEnabled())
          Tr.entry(tc, "checkSFSBAccess: " + info + ", " + (cmd == null ? null : cmd.getJ2EEName()));
    
       if ( cmd instanceof BeanMetaData )
       {
          BeanMetaData bmd = (BeanMetaData)cmd;
          if (isTraceOn && tc.isDebugEnabled())
             Tr.debug(tc, "stateful session bean");
    
          isSFSB = bmd.isStatefulSessionBean();
    
          String refName = info.getReferenceName();
          if ( isSFSB && !bmd.ivPersistenceRefNames.contains( refName ) ) // F743-30682
          {
             Tr.error(tc, "PERSISTENCE_REF_DEPENDENCY_NOT_DECLARED_CNTR0315E",
                      bmd.j2eeName.getComponent(),
                      bmd.j2eeName.getModule(),
                      bmd.j2eeName.getApplication(),
                      refName);
             String msg = Tr.formatMessage(tc, "PERSISTENCE_REF_DEPENDENCY_NOT_DECLARED_CNTR0315E",
                                           bmd.j2eeName.getComponent(),
                                           bmd.j2eeName.getModule(),
                                           bmd.j2eeName.getApplication(),
                                           refName);
             throw new InjectionException(msg);
          }
       } 
       else
       {
           // even though the resource ref may have been defined in a stateful bean, the lookup
           // was not within the context of a stateful bean.  if this is a lookup of an ExPC, it
           // should not be allowed (super will verify).
           isSFSB = false;
       }
    
       boolean result = super.checkSFSBAccess( info, isSFSB ); // F743-30682
    
       if (isTraceOn && tc.isEntryEnabled())
          Tr.exit(tc, "checkSFSBAccess: " + result);
       return result;
    }
    
    public void setLookupDelegate(JPALookupDelegate ref) {
        super.setLookupDelegate(ref);
    }

    public void unsetLookupDelegate(JPALookupDelegate ref) {
        super.unsetLookupDelegate(ref);
    }
}

