/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.experimental.web;

import java.util.stream.Stream;

import jakarta.data.Order;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

/**
 * Repository for An entity that tries out some temporal and CharSequence types
 * that aren't mentioned in the Jakarta Persistence specification (except for
 * Year, which is mentioned but is not indicated whether or not it can be an Id).
 */
@Repository
@StaticMetamodel(YearlyTotal.class)
public interface YearlyTotals {
    // Experiment with an application embedding the static metamodel within the
    // repository interface:
    String BESTDAY = "bestDay";
    String BESTMONTH = "bestMonth";
    String BUFFER = "buffer";
    String BUILDER = "builder";
    String COMMENTS = "comments";
    String YEAR = "year";

    SortableAttribute<YearlyTotal> bestDay = new SortableAttributeRecord<>(BESTDAY);
    SortableAttribute<YearlyTotal> bestMonth = new SortableAttributeRecord<>(BESTMONTH);
    TextAttribute<YearlyTotal> buffer = new TextAttributeRecord<>(BUFFER);
    TextAttribute<YearlyTotal> builder = new TextAttributeRecord<>(BUILDER);
    TextAttribute<YearlyTotal> comments = new TextAttributeRecord<>(COMMENTS);
    SortableAttribute<YearlyTotal> year = new SortableAttributeRecord<>(YEAR);

    @Delete
    void erase();

    @Insert
    void publish(YearlyTotal total);

    @Find
    Stream<YearlyTotal> obtain(Order<YearlyTotal> order);
}
