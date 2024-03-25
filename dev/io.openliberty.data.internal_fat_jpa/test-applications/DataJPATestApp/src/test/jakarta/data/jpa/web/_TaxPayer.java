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

/**
 * Static metamodel for the TaxPayer entity.
 */
@StaticMetamodel(TaxPayer.class)
public class _TaxPayer {
    public static volatile Attribute<TaxPayer> bankAccounts;
    public static volatile SortableAttribute<TaxPayer> filingStatus;
    public static volatile SortableAttribute<TaxPayer> income;
    public static volatile SortableAttribute<TaxPayer> numDependents;
    public static volatile SortableAttribute<TaxPayer> ssn; // the id attribute
}