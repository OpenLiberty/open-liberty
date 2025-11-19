/*
 * Copyright (c) 2012, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Blaise Doughan - 2.5 - initial implementation
package org.eclipse.persistence.internal.core.databaseaccess;

import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.internal.core.helper.CoreConversionManager;
import org.eclipse.persistence.internal.sessions.AbstractSession;

public interface CorePlatform<CONVERSION_MANAGER extends CoreConversionManager> {

    /**
     * Convert the object to the appropriate type by invoking the appropriate
     * ConversionManager method
     * @param object - the object that must be converted
     * @param javaClass - the class that the object must be converted to
     * @exception - ConversionException, all exceptions will be thrown as this type.
     * @return - the newly converted object
     */
    Object convertObject(Object sourceObject, Class javaClass);

    /**
     * Convert the object to the appropriate type by invoking the appropriate
     * ConversionManager method.
     * @param sourceObject the object that must be converted
     * @param javaClass the class that the object must be converted to
     * @param session current database session
     * @exception ConversionException all exceptions will be thrown as this type.
     * @return the newly converted object
     */
    Object convertObject(Object sourceObject, Class javaClass, AbstractSession session) throws ConversionException;

    /**
     * The platform hold its own instance of conversion manager to allow customization.
     */
    CONVERSION_MANAGER getConversionManager();

}
