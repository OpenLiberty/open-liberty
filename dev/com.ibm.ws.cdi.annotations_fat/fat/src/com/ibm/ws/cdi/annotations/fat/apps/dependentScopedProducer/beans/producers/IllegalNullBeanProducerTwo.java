/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.annotations.fat.apps.dependentScopedProducer.beans.producers;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import com.ibm.ws.cdi.annotations.fat.apps.dependentScopedProducer.beans.NonNullBeanTwo;

@ApplicationScoped
public class IllegalNullBeanProducerTwo {
	
	private static boolean hasProduced = false;
	
	@Produces @ApplicationScoped
	public NonNullBeanTwo produceNull(){
		hasProduced = true;
		return null;
	}

	public static boolean hasProduced() {
		return hasProduced;
	}

}
