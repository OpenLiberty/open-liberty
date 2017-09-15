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

import com.ibm.ws.request.probe.bci.internal.RequestProbeBCIManagerImpl;

/**
 * This class is used internally by request probe framework as a helper/utility
 * class and should not be used outside of the scope of request probe framework.
 * 
 * It is used to get the utility functionality i.e : The method defined in this
 * class to operates on the instrumentation object(List of loaded classes) to
 * help us find the classes which are of our interest.
 */
public class RequestProbeHelper {

	/**
	 * @param className
	 *            : Get the className of string type to check if we are
	 *            interested in that class
	 * @return : true if the interested class is available in the loaded class
	 *         else false.
	 */
	public static boolean interestedClass(String className) {

		for (RequestProbeTransformDescriptor td : RequestProbeBCIManagerImpl
				.getRequestProbeTransformDescriptors().values()) {
			if (td != null && td.getClassName().equalsIgnoreCase(className)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param className
	 *            :Get the className of string type to check if we are
	 *            interested in that class
	 * @param client
	 *            :TransformDescriptor of the specific feature
	 * @return : true if the class name is the one which we are looking for and
	 *         the transformed descriptor is available, else false.
	 */
	public static boolean interestedClassForSpecifiedClient(String className,
			RequestProbeTransformDescriptor client) {
		if (client != null && client.getClassName().equalsIgnoreCase(className)) {
			return true;
		}
		return false;
	}

}
