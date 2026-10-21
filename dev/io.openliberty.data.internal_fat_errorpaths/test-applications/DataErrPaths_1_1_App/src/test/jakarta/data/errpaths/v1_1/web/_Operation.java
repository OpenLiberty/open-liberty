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

import jakarta.data.metamodel.ComparableAttribute;
import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

/**
 * Static metamodel for the Operation entity.
 */
@StaticMetamodel(Operation.class)
public interface _Operation {

    String NAME = "name";
    String NUM_ARGS = "numArgs";
    String SYMBOL = "symbol";

    TextAttribute<Operation> name = //
                    TextAttribute.of(Operation.class, NAME);

    NumericAttribute<Operation, Integer> numArgs = //
                    NumericAttribute.of(Operation.class, NUM_ARGS, int.class);

    ComparableAttribute<Operation, Character> symbol = //
                    ComparableAttribute.of(Operation.class, SYMBOL, Character.class);
}
