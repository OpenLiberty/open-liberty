/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.messages;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Allows other bundles to access a common set of NLS messages.
 */
public class ConcurrencyNLS {
    private static final TraceComponent tc = Tr.register(ConcurrencyNLS.class);

    /**
     * Obtains an NLS message that is shared across multiple bundles for the Concurrency componentry.
     *
     * @param messageKey NLS message key. For example: CWWKC1403.unsupported.tx.type
     * @param args       message parameters, if any.
     */
    public static final String getMessage(String messageKey, Object... args) {
        return Tr.formatMessage(tc, messageKey, args);
    }
}
