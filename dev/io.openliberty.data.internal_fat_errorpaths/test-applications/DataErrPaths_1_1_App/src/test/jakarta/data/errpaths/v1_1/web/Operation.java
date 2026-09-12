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
package test.jakarta.data.errpaths.v1_1.web;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * A valid entity. Represents an arithmetic operation such as +
 */
@Entity
public class Operation {

    @Id
    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public int numArgs;

    public Character symbol;

    public Operation() {
    }

    public static Operation of(String name,
                               int numArgs,
                               Character symbol) {
        Operation op = new Operation();
        op.name = name;
        op.numArgs = numArgs;
        op.symbol = symbol;
        return op;
    }

    @Override
    public String toString() {
        return "Operation:" + name + " " + numArgs + " args " + symbol;
    }
}
