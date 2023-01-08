/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.classloading;

import java.util.EnumSet;

import com.ibm.wsspi.classloading.ApiType;

/**
 * This is the interface of classloaders created by the Liberty profile's
 * class loading service. It is provided purely for internal type safety.
 * Some method signatures will require that you provide a class loader
 * of this type simply to ensure that it is a Liberty class loader.
 * Do not create your own extension of this class as it will <strong>not
 * </strong> work predictably with the Liberty
 */
public interface LibertyClassLoader {

    /** @return the set of {@link ApiType}s which this class provider can access */
    EnumSet<ApiType> getApiTypeVisibility();
}
