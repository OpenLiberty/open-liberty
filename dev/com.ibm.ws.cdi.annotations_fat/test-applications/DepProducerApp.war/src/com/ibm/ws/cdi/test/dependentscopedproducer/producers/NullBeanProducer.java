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

import java.util.Random;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;

import com.ibm.ws.cdi.test.dependentscopedproducer.DependentSterotype;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBean;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBeanFour;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBeanThree;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBeanTwo;

@Priority(value = 100)
@ApplicationScoped
public class NullBeanProducer {
	
	private static boolean hasProduced = false;
	
	private final Random random = new Random();
	
	private static boolean toggleOne, toggleTwo, toggleThree, toggleFour;
	
	public static boolean isNullOne() {
		return toggleOne;
	}

	public static boolean isNullTwo() {
		return toggleTwo;
	}

	public static boolean isNullThree() {
		return toggleThree;
	}
	
	public static boolean isNullFour() {
		return toggleFour;
	}

	@Dependent
	@Produces
	@Alternative
	public NullBean produceNull(){
		hasProduced = true;
		
		if (random.nextBoolean()) {
			toggleOne = true;
			return null;
		} else {
			toggleOne = false;
			return new NullBean();
		}
	}
	
	@Produces
	@Alternative
	@DependentSterotype
	public NullBeanTwo produceNullTwo(){
		hasProduced = true;
		if (random.nextBoolean()) {
			toggleTwo = true;
			return null;
		} else {
			toggleTwo = false;
			return new NullBeanTwo();
		}
	}
	
	@Produces
	@Dependent
	@Alternative
	public NullBeanThree produceNullThree(){
		hasProduced = true;
		if (random.nextBoolean()) {
			toggleThree = true;
			return null;
		} else {
			toggleThree = false;
			return new NullBeanThree();
		}
	}
	
	@Produces
	@Dependent
	@Alternative
	public NullBeanFour produceNullFour(){
		hasProduced = true;
		if (random.nextBoolean()) {
			toggleFour = true;
			return null;
		} else {
			toggleFour = false;
			return new NullBeanFour();
		}
	}
	
	public static boolean hasProduced() {
		return hasProduced;
	}

}
