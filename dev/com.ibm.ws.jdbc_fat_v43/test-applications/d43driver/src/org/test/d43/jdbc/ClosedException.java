/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.d43.jdbc;

import java.sql.SQLNonTransientConnectionException;

public class ClosedException extends SQLNonTransientConnectionException {
    private static final long serialVersionUID = 1L;

    public ClosedException(Class<?> type) {
        super(type.getSimpleName() + " is closed", "08003", 1);
    }
}