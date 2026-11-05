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
package test.jakarta.data.v1_1.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.data.metamodel.BasicAttribute;
import jakarta.data.metamodel.BooleanAttribute;
import jakarta.data.metamodel.ComparableAttribute;
import jakarta.data.metamodel.NavigableAttribute;
import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

/**
 * Static metamodel for the Grant entity.
 */
@StaticMetamodel(Grant.class)
public interface _Grant {

    String AMOUNT = "amount";
    String GIVER = "giver";
    String GIVER_CATEGORY = "giver.category";
    String GIVER_NAME = "giver.name";
    String ID = "id";
    String LAST_UPDATED = "lastUpdated";
    String RECIPIENT = "recipient";
    String RECIPIENT_FOCUS = "recipient.focus";
    String RECIPIENT_NAME = "recipient.name";
    String RENEWABLE = "renewable";
    String REPORTING_DATES = "reportingDates";
    String TITLE = "title";

    NumericAttribute<Grant, Integer> amount = NumericAttribute //
                    .of(Grant.class, AMOUNT, int.class);

    NavigableAttribute<Grant, Giver> giver = NavigableAttribute //
                    .of(Grant.class, GIVER, Giver.class);

    ComparableAttribute<Grant, Giver.Category> giver_category = ComparableAttribute //
                    .of(Grant.class, GIVER_CATEGORY, Giver.Category.class);

    TextAttribute<Grant> giver_name = TextAttribute //
                    .of(Grant.class, GIVER_NAME);

    NumericAttribute<Grant, Long> id = NumericAttribute //
                    .of(Grant.class, ID, long.class);

    ComparableAttribute<Grant, LocalDateTime> lastUpdated = ComparableAttribute //
                    .of(Grant.class, LAST_UPDATED, LocalDateTime.class);

    NavigableAttribute<Grant, Grantee> recipient = NavigableAttribute //
                    .of(Grant.class, RECIPIENT, Grantee.class);

    ComparableAttribute<Grant, Grantee.Focus> recipient_focus = ComparableAttribute //
                    .of(Grant.class, RECIPIENT_FOCUS, Grantee.Focus.class);

    TextAttribute<Grant> recipient_name = TextAttribute //
                    .of(Grant.class, RECIPIENT_NAME);

    BooleanAttribute<Grant> renewable = BooleanAttribute //
                    .of(Grant.class, RENEWABLE, boolean.class);

    @SuppressWarnings("unchecked")
    BasicAttribute<Grant, List<LocalDate>> reportingDates = //
                    (BasicAttribute<Grant, List<LocalDate>>) //
                    (BasicAttribute<?, ?>) BasicAttribute //
                    .of(Grant.class, REPORTING_DATES, List.class);

    TextAttribute<Grant> title = TextAttribute //
                    .of(Grant.class, TITLE);
}
