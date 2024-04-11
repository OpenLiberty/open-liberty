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
import jakarta.data.metamodel.impl.AttributeRecord;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

/**
 * Metamodel for the Prime entity.
 */
@StaticMetamodel(Prime.class)
public interface _Prime {
    String BINARYDIGITS = "binaryDigits";
    String EVEN = "even";
    String HEX = "hex";
    String NAME = "name";
    String NUMBERID = "numberId";
    String ROMANNUMERAL = "romanNumeral";
    String ROMANNUMERALSYMBOLS = "romanNumeralSymbols";
    String SUMOFBITS = "sumOfBits";

    TextAttribute<Prime> binaryDigits = new TextAttributeRecord<>(BINARYDIGITS);

    SortableAttribute<Prime> even = new SortableAttributeRecord<>(EVEN);

    TextAttribute<Prime> hex = new TextAttributeRecord<>(HEX);

    TextAttribute<Prime> name = new TextAttributeRecord<>(NAME);

    SortableAttribute<Prime> numberId = new SortableAttributeRecord<>(NUMBERID);

    TextAttribute<Prime> romanNumeral = new TextAttributeRecord<>(ROMANNUMERAL);

    Attribute<Prime> romanNumeralSymbols = new AttributeRecord<>(ROMANNUMERALSYMBOLS);

    SortableAttribute<Prime> sumOfBits = new SortableAttributeRecord<>(SUMOFBITS);
}
