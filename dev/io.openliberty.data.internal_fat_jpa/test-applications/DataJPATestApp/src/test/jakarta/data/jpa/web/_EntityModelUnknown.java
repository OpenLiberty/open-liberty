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

import java.time.Period;

import jakarta.data.Sort;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;

/**
 * Static metamodel for an non-JPA entity type to ignore.
 */
@StaticMetamodel(Period.class)
public class _EntityModelUnknown {
    public static volatile SortableAttribute<Period> days = new Sortable("Days", Sort.asc("Days"), Sort.desc("Days"));
    public static volatile SortableAttribute<Period> months = new Sortable("Mon", Sort.asc("Mon"), Sort.desc("Mon"));
    public static volatile SortableAttribute<Period> years;

    private record Sortable(String name, Sort<Period> asc, Sort<Period> desc) implements SortableAttribute<Period> {
    }
}