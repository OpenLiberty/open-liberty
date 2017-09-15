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
package com.ibm.ejs.container;

import java.util.ArrayList;
import com.ibm.websphere.csi.J2EEName;

/**
 * Provides the data for a specific JNDI binding name. <p>
 **/
public final class BindingData
{
    /**
     * The Java EE Name of the bean that was explicitly bound to the
     * associated JNDI location. <p>
     * 
     * The following are considered explit bindings:
     * <ul>
     * <li> An interface specific binding.
     * <li> A simple-binding-name, whether it includes #<interface> or not.
     * <li> A long form default binding name (since they are unique).
     * </ul>
     * 
     * This field will be null if this binding is not the result of an
     * explicit binding. See ivShortDefaultBeans.
     **/
    public J2EEName ivExplicitBean;

    /**
     * The interface class name associated with the explicit binding.
     **/
    public String ivExplicitInterface;

    /**
     * Java EE Name list of all of the beans that were implicitly bound to
     * the associated JNDI location. <p>
     * 
     * The short form default binding names define an implicit binding. <p>
     * 
     * When an explicit binding is present, this field will be ignored,
     * though it will be maintained with all beans that have an implicit
     * binding to the associated JNDI location. <p>
     * 
     * When no explict binding and this list contains a single entry,
     * that bean interface will be bound to the associated JNDI location.
     * When no explicit binding and the list contains multiple entries,
     * an AmbiguousEJBReference will be bound to the JNDI location. <p>
     **/
    public ArrayList<J2EEName> ivImplicitBeans;
}
