/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import jakarta.data.metamodel.Attribute;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.impl.AttributeRecord;
import jakarta.data.metamodel.impl.SortableAttributeRecord;

/**
 * Static metamodel for the TaxPayer entity.
 */
@StaticMetamodel(TaxPayer.class)
public interface _TaxPayer {
    Attribute<TaxPayer> bankAccounts = new AttributeRecord<>("bankAccounts");
    SortableAttribute<TaxPayer> filingStatus = new SortableAttributeRecord<>("filingStatus");
    SortableAttribute<TaxPayer> income = new SortableAttributeRecord<>("income");
    SortableAttribute<TaxPayer> numDependents = new SortableAttributeRecord<>("numDependents");
    SortableAttribute<TaxPayer> ssn = new SortableAttributeRecord<>("ssn"); // the id attribute
}