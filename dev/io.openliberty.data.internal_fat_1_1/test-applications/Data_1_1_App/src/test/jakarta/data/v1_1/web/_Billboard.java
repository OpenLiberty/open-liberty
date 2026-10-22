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

import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TemporalAttribute;
import jakarta.data.metamodel.TextAttribute;

/**
 * Static metamodel for the Billboard entity.
 */
@StaticMetamodel(Billboard.class)
public interface _Billboard extends _Advertisement {

    String FIRSTDAY = "firstDay";
    String LASTDAY = "lastDay";
    String LOCATION = "location";

    TemporalAttribute<Billboard, LocalDate> firstDay = TemporalAttribute //
                    .of(Billboard.class, FIRSTDAY, LocalDate.class);

    TemporalAttribute<Billboard, LocalDate> lastDay = TemporalAttribute //
                    .of(Billboard.class, LASTDAY, LocalDate.class);

    TextAttribute<Billboard> sponsor = TextAttribute //
                    .of(Billboard.class, LOCATION);

}
