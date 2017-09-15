/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import java.util.List;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import com.ibm.websphere.csi.J2EEName;

import static com.ibm.ws.ejbcontainer.util.AmbiguousEJBRefObjectFactory.ADDR_TYPE;

/**
 * Used to create Reference objects for AmbiguousEJBReferences, which the
 * caller then binds to a JNDI name space. When the object is looked up,
 * the associated factory uses the reference data to thrown an
 * AmbiguousEJBReferenceException. <p>
 * 
 * AmbiguousEJBReferences may occur for the following scenarios :
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
 * There is a seperate constructor for each scenario, so that
 * the exception message text may reflect the scenario. <p>
 * 
 * @see AmbiguousEJBRefObjectFactory
 **/
public class AmbiguousEJBRefReferenceFactory
{
    /**
     * Name of the Naming ObjectFactory used to resolve References for
     * an AmbiguousEJBReference.
     **/
    private static final String AMBIGUOUS_FACTORY_NAME =
                    AmbiguousEJBRefObjectFactory.class.getName();

    /**
     * Creates an AmbiguousEJBReference Reference based on a simple-name
     * binding and the J2EEName of the corresponding EJB. <p>
     * 
     * Intended for use in the following scenario: <p>
     * 
     * A 'simple name' binding has been provided for an EJB which has
     * more than one remote or local interface, and an attempt is made
     * to lookup the EJB using the simple name. <p>
     * 
     * Since the 'simple name' does not uniquely identify a single
     * interface, it is considered ambiguous. Instead, the lookup
     * String should have '#<interface>' appended, where <interface>
     * is the desired local or remote interface. <p>
     * 
     * @param simpleBindingName simple-name binding for an EJB.
     * @param j2eeName unique bean identifier.
     * 
     * @return the created AmbiguousEJBReference Reference.
     **/
    public static Reference createAmbiguousReference
                    (String simpleBindingName,
                     J2EEName j2eeName)
    {
        String message = "The simple-binding-name '" + simpleBindingName +
                         "' for bean " + j2eeName + " is ambiguous because the bean " +
                         "implements multiple interfaces.  Provide an interface specific " +
                         "binding or add #<interface> to the simple-binding-name on lookup.";

        StringRefAddr refAddr = new StringRefAddr(ADDR_TYPE, message);
        Reference ref = new Reference(ADDR_TYPE, refAddr,
                        AMBIGUOUS_FACTORY_NAME, null);
        return ref;
    }

    /**
     * Creates an AmbiguousEJBReference Reference based on a short-form
     * default binding (interface) and a list of J2EENames for all EJB
     * implementing that interface, and using short-form bindings. <p>
     * 
     * Intended for use in the following scenario: <p>
     * 
     * Default short form bindings are being used, and multiple EJBs
     * are configured to implement the same interface. <p>
     * 
     * Since the short form binding does not uniquely identify a
     * specific EJB, it is considered ambiguous. Instead, the
     * lookup should be performed using the long form binding name,
     * or a specific binding should be provided.
     * 
     * @param shortDefaultBinding short-form default binding for an EJB.
     * @param j2eeNames list of all EJBs implementing the same interface.
     * 
     * @return the created AmbiguousEJBReference Reference.
     **/
    public static Reference createAmbiguousReference
                    (String shortDefaultBinding,
                     List<J2EEName> j2eeNames)
    {
        String message = "The short-form default binding '" + shortDefaultBinding +
                         "' is ambiguous because multiple beans implement the interface : " +
                         j2eeNames + ". Provide an interface specific binding or use " +
                         "the long-form default binding on lookup.";

        StringRefAddr refAddr = new StringRefAddr(ADDR_TYPE, message);
        Reference ref = new Reference(ADDR_TYPE, refAddr,
                        AMBIGUOUS_FACTORY_NAME, null);
        return ref;
    }
}
