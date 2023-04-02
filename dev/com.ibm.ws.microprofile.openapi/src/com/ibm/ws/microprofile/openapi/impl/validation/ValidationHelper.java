/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.microprofile.openapi.impl.validation;

import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;

/**
 *
 */
public interface ValidationHelper {

    /**
     * Adds a validation event to the ValidationResult.
     */
    public void addValidationEvent(ValidationEvent event);

    /**
     * Adds an operationId to the set of IDs. Returns true if the ID was already in the set.
     */
    public boolean addOperationId(String operationId);

    /**
     * Adds an operationId and location of Link object specifying it to the map of IDs and locations.
     */
    public void addLinkOperationId(String operationId, String location);

}