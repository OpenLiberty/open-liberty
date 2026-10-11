/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.internal.validation;

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker;

/**
 *
 */
public interface ValidationHelper {

    /**
     * Adds a validation event to the ValidationResult.
     */
    void addValidationEvent(ValidationEvent event);

    /**
     * Adds an operationId to the set of IDs. Returns true if the ID was already in the set.
     */
    boolean addOperationId(String operationId);

    /**
     * Adds an operationId and location of Link object specifying it to the map of IDs and locations.
     */
    void addLinkOperationId(String operationId, String location);

    /**
     * Validates a reference and returns the referenced object.
     * <p>
     * Validation events are recorded to this {@code ValidationHelper} for any problems found.
     * <p>
     * The validator may choose to ignore certain types of reference that it can't or doesn't validate, so it's possible to have no validation events registered and {@code null} be
     * returned.
     *
     * @param context the walker context
     * @param key the key of the referring object
     * @param ref the JSON pointer reference
     * @return the object being referenced if one was found, otherwise {@code null}
     */
    Object validateReference(OpenAPIModelWalker.Context context, String key, String ref);

    /**
     * Validates a reference and checks the referenced object is of the correct type.
     * <p>
     * If a referenced object of the correct type is found, it is returned.
     * <p>
     * Validation events are recorded to this {@code ValidationHelper} for any problems found.
     * <p>
     * The validator may choose to ignore certain types of reference that it can't or doesn't validate, so it's possible to have no validation events registered and {@code null} be
     * returned.
     *
     * @param context the walker context
     * @param key the key of the referring object
     * @param ref the JSON pointer reference
     * @param clazz the expected type of the referenced object
     * @return the object being referenced if one was found, otherwise {@code null}
     */
    <T> T validateReference(OpenAPIModelWalker.Context context, String key, String ref, Class<T> clazz);
}