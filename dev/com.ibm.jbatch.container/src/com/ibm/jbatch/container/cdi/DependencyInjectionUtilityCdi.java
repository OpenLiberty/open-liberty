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

import com.ibm.jbatch.container.util.DependencyInjectionUtility;
import com.ibm.jbatch.jsl.model.Property;

/**
 * A a bridge for BatchProducerBean.  This class's package is exported 
 * from the com.ibm.jbatch.container bundle so that BatchProducerBean, 
 * in the com.ibm.ws.jbatch.cdi bundle, can invoke the DependencyInjectionUtility,
 * which itself is located in a non-exported package.
 */
public class DependencyInjectionUtilityCdi {

    public static String getPropertyValue(List<Property> propList, String batchPropName) {
        return DependencyInjectionUtility.getPropertyValue(propList, batchPropName);
    }

}
