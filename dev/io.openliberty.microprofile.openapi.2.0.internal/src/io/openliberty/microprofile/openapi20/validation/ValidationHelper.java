/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation;

import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;

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