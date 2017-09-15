/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.ibm.websphere.ejbcontainer.AmbiguousEJBReferenceException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This JNDI object factory throws an AmbiguousEJBReferenceException
 * with the message contained in the reference data. A Reference for
 * this factory is bound into the name space for a JNDI name which
 * has been configured for an EJB, but does not uniquely identify an
 * EJB or EJB interface. <p>
 * 
 * This may occur for the following scenarios :
 * <ul>
 * <li> A 'simple name' binding has been provided for an EJB which has
 * more than one remote or local interface, and an attempt is made
 * to lookup the EJB using the simple name. <p>
 * 
 * Since the 'simple name' does not uniquely identify a single
 * interface, it is considered ambiguous. Instead, the lookup
 * String should have '#<interface>' appended, where <interface>
 * is the desired local or remote interface.
 * 
 * <li> Default short form bindings are being used, and multiple EJBs
 * are configured to implement the same interface. <p>
 * 
 * Since the short form binding does not uniquely identify a
 * specific EJB, it is considered ambiguous. Instead, the
 * lookup should be performed using the long form binding name,
 * or a specific binding should be provided.
 * </ul>
 * 
 * This JNDI object factory is used to process a Reference created by the
 * Reference factory, AmbiguousEJBRefReferenceFactory. When an
 * AmbiguousEJBReference object is bound to a name space, the Reference object
 * obtained from it is the object actually bound to the name space. When the
 * Reference object is looked up, this factory is invoked to process it. <p>
 * 
 * The reference data for this object factory is an instance of the class
 * javax.naming.StringRefAddr, which contains a String that represents the
 * message text for the AmbiguousEJBReferenceException to be thrown. The
 * RefAddr type is identified as 'AmbiguousEJBReference'. <p>
 **/
public class AmbiguousEJBRefObjectFactory implements ObjectFactory
{
    private static final String CLASS_NAME = AmbiguousEJBRefObjectFactory.class.getName();

    private static final TraceComponent tc = Tr.register(AmbiguousEJBRefObjectFactory.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    static final String ADDR_TYPE = "AmbiguousEJBReference";

    /**
     * Construct an AmbiguousEJBRefObjectFactory object. <p>
     * 
     * This is the public, no parameter constructor required by Naming.
     */
    public AmbiguousEJBRefObjectFactory()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>");
    }

    /**
     * The method is part of the java.naming.spi.ObjectFactory interface and is
     * invoked by javax.naming.spi.NamingManager to process a Reference object.
     * 
     * @return the results of the lookup driven from Reference data. If the
     *         Reference is unrecognized, this method returns null.
     **/
    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment)
                    throws Exception
    {
        AmbiguousEJBReferenceException ambiguousEx;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance : " + obj);

        // -----------------------------------------------------------------------
        // Is obj a Reference?
        // -----------------------------------------------------------------------
        if (!(obj instanceof Reference))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : " +
                            "Reference object not provided : " + obj);
            return null;
        }

        Reference ref = (Reference) obj;

        // -----------------------------------------------------------------------
        // Is the right factory for this reference?
        // -----------------------------------------------------------------------
        if (!ref.getFactoryClassName().equals(CLASS_NAME))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : " +
                            "Incorrect factory for Reference : " + obj);
            return null;
        }

        // -----------------------------------------------------------------------
        // Is address null?
        // -----------------------------------------------------------------------
        RefAddr addr = ref.get(ADDR_TYPE);
        if (addr == null)
        {
            NamingException nex = new NamingException("The address for this Reference is empty (null)");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : " + nex);
            throw nex;
        }

        // -----------------------------------------------------------------------
        // Reference has the right factory and non empty address, so it is OK
        // to generate the AmbiguousEJBReferenceException object now.
        // -----------------------------------------------------------------------
        String message = (String) addr.getContent();

        ambiguousEx = new AmbiguousEJBReferenceException(message);

        // Note : A Tr.warning should NOT be logged here, as this may be
        //        normal path for ejb-ref resolution.

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance : " + ambiguousEx.getMessage());

        throw ambiguousEx;
    }
}
