/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.jpa.persistence.tests.models;

import jakarta.persistence.EnumeratedValue;

public enum TicketStatus {

    OPEN(1), CLOSED(2), CANCELLED(-1);

    @EnumeratedValue
    final int intValue;

    TicketStatus(int value) {
        this.intValue = value;
    }

    /**
     * @return the intValue
     */
    public int getIntValue() {
        return intValue;
    }

}