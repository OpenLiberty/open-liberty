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

package com.ibm.wsspi.request.probe.bci;

/**
 * 
 * class ContextInfoHelper : is used by the TransformDescriptior implementation to
 * encapsulate the current object of the class for which the instrumentation
 * will be performed. It also consists of the detail of the current method
 * argument list.
 */
public abstract class ContextInfoHelper {
	private Object instanceOfThisClass;
	private Object methodArgs;

	public ContextInfoHelper(Object instanceOfThisClass, Object methodArgs) {
		this.instanceOfThisClass = instanceOfThisClass;
		this.methodArgs = methodArgs;
	}

	/**
	 * Allowing the decedent object who will implement this class to have its
	 * own implementation of toString method.
	 */
	public abstract String toString();

	/**
	 * This method will return the current method and its list of argument for
	 * which transform descriptor will implement the instrumentation.
	 * 
	 * @return the list of the method argument for which the instrumentation
	 *         will be performed.
	 */
	public Object getMethodArgs() {
		return methodArgs;
	}

	/**
	 * This method will return the current instance of the implemented object
	 * for which transform descriptor will implement the instrumentation.
	 * 
	 * @return the current object of type Object for which the instrumentation
	 *         will be performed.
	 */
	public Object getInstanceOfThisClass() {
		return instanceOfThisClass;
	}

}
