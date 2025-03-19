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
package test.jakarta.data.errpaths.web;

import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

/**
 * Static metamodel for the Voter entity.
 */
@StaticMetamodel(Voter.class)
public interface _Voter {
    String ADDRESS = "address";
    String BIRTHDAY = "birthday";
    String NAME = "name";
    String PHONE_NUMBER = "phoneNumber"; // not an attribute of the entity
    String SSN = "ssn";

    TextAttribute<Voter> address = new TextAttributeRecord<>(ADDRESS);

    SortableAttribute<Voter> birthday = new SortableAttributeRecord<>(BIRTHDAY);

    TextAttribute<Voter> name = new TextAttributeRecord<>(NAME);

    // not an attribute of the entity
    SortableAttribute<Voter> phoneNumber = new SortableAttributeRecord<>(PHONE_NUMBER);

    SortableAttribute<Voter> ssn = new SortableAttributeRecord<>(SSN);
}
