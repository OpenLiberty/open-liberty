/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.jcache;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This exception indicates that there was an error deserializing an object from
 * the remote JCache. This typically indicates something is amiss in the customer's
 * configuration. For example, there are multiple versions of the class across
 * Liberty servers, or perhaps there is a classpath issue where the class cannot
 * be found.
 */
@Trivial
public class DeserializationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Instantiate a new {@link DeserializationException}.
     *
     * @param message The exception message.
     * @param cause   The cause.
     */
    public DeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
