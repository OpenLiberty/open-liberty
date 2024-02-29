/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.web;

import jakarta.data.metamodel.Attribute;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

/**
 * Metamodel for the Prime entity.
 */
@StaticMetamodel(Prime.class)
public class _Prime {
    public static final String BINARYDIGITS = "binaryDigits";
    public static final String EVEN = "even";
    public static final String HEX = "hex";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String NUMBERID = "numberId";
    public static final String ROMANNUMERAL = "romanNumeral";
    public static final String ROMANNUMERALSYMBOLS = "romanNumeralSymbols";
    public static final String SUMOFBITS = "sumOfBits";

    public static volatile TextAttribute<Prime> binaryDigits;

    public static volatile SortableAttribute<Prime> even;

    public static volatile TextAttribute<Prime> hex;

    public static volatile SortableAttribute<Prime> id;

    public static volatile TextAttribute<Prime> name;

    public static volatile SortableAttribute<Prime> numberId;

    public static volatile TextAttribute<Prime> romanNumeral;

    public static volatile Attribute<Prime> romanNumeralSymbols;

    public static volatile SortableAttribute<Prime> sumOfBits;

}
