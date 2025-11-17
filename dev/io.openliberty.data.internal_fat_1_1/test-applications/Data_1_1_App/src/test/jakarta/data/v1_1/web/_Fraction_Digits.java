/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.v1_1.web;

import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

import test.jakarta.data.v1_1.web.Fraction.Digits;

/**
 * Static metamodel for the Fraction.Digits embeddable.
 */
@StaticMetamodel(Digits.class)
public interface _Fraction_Digits {

    String NONREPEATING = "nonrepeating";
    String REPEATING = "repeating";

    TextAttribute<Digits> nonrepeating = //
                    TextAttribute.of(Digits.class, NONREPEATING);

    TextAttribute<Digits> repeating = //
                    TextAttribute.of(Digits.class, REPEATING);
}
