/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.naming;

import javax.naming.Reference;

/**
 * Reference object that is basically a wrapper for an EJBBinding. Specifically used
 * for legacy remote EJBBindings as we need to bind a Reference pointing to an ObjectFactory
 * to have the naming lookup code instantiate the object.
 */
@SuppressWarnings("serial")
public class EJBRemoteReferenceBinding extends Reference {

    private final EJBBinding ivBinding;

    public EJBRemoteReferenceBinding(EJBBinding binding) {
        // pass className and ObjectFactory className
        super(EJBRemoteReferenceBinding.class.getName(), EJBRemoteBeanFactory.class.getName(), null);
        this.ivBinding = binding;
    }

    public EJBBinding getReferenceBinding() {
        return ivBinding;
    }

}
