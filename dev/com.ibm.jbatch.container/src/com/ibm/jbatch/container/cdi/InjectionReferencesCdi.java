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

import java.util.List;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.jsl.model.Property;

/**
 * A a bridge for BatchProducerBean.  This class's package is exported 
 * from the com.ibm.jbatch.container bundle so that BatchProducerBean, 
 * in the com.ibm.ws.jbatch.cdi bundle, can invoke the InjectionReferences,
 * which itself is located in a non-exported package.
 */
public class InjectionReferencesCdi {

    private final InjectionReferences injectionReferences;
    
    public InjectionReferencesCdi(InjectionReferences injectionReferences) {
        this.injectionReferences = injectionReferences;
    }
    
    public static InjectionReferences getInjectionReferences() {
       return ProxyFactory.getInjectionReferences();
    }

    public List<Property> getProps() {
        return injectionReferences == null ? null : injectionReferences.getProps();
    }

    public JobContext getJobContext() {
        return injectionReferences == null ? null : injectionReferences.getJobContext();
    }

    public StepContext getStepContext() {
        return injectionReferences == null ? null : injectionReferences.getStepContext();
    }

}
