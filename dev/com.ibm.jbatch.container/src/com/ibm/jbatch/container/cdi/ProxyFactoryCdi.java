/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.cdi;

import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;

/**
 * A a bridge for BatchProducerBean.  This class's package is exported 
 * from the com.ibm.jbatch.container bundle so that BatchProducerBean, 
 * in the com.ibm.ws.jbatch.cdi bundle, can invoke the ProxyFactory,
 * which itself is located in a non-exported package.
 */
public class ProxyFactoryCdi {

    public static InjectionReferencesCdi getInjectionReferences() {
       return new InjectionReferencesCdi( ProxyFactory.getInjectionReferences() );
    }

}
