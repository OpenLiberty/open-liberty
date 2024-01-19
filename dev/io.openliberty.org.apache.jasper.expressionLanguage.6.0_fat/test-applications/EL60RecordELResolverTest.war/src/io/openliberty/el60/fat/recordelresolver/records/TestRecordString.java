/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.recordelresolver.records;

/**
 * A Java record that contains a String.
 */
public record TestRecordString(String data) {

    /**
     * Throw an UnsupportedOperationException to ensure the RecordELResolver.getValue method
     * throws an ELException and includes the UnsupportedOperationException as the cause.
     */
    public void throwException() {
        throw new UnsupportedOperationException();
    }
}
