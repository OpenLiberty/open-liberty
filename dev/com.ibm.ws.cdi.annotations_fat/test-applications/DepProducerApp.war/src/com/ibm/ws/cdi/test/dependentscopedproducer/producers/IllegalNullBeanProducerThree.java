/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.test.dependentscopedproducer.producers;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import com.ibm.ws.cdi.test.dependentscopedproducer.AppScopedSterotype;
import com.ibm.ws.cdi.test.dependentscopedproducer.NonNullBeanThree;

@ApplicationScoped
public class IllegalNullBeanProducerThree {
	
	private static boolean hasProduced = false;
	
	@Produces
	@AppScopedSterotype
	public NonNullBeanThree produceNull(){
		hasProduced = true;
		return null;
	}

	public static boolean hasProduced() {
		return hasProduced;
	}

}
