/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.repository.resources;

import com.ibm.ws.repository.common.enums.ResourceType;

/**
 * Represents a Product Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to products.
 * <p>
 * Products represented by this interface can either be of type {@link ResourceType#INSTALL} or {@link ResourceType#ADDON}.
 */
public interface ProductResource extends ProductRelatedResource, ApplicableToProduct {

}
