/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package web.assertion.cmt;

/**
 * Thrown by a CDI {@code @Transactional} bean after its web service call, to roll back the transaction that
 * the bean's interceptor owns. Carries the reply of the call so that the caller can still report it.
 */
public class RollbackRequested extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RollbackRequested(String reply) {
        super(reply);
    }
}
