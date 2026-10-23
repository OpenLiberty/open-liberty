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

import java.time.LocalDateTime;

import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TemporalAttribute;
import jakarta.data.metamodel.TextAttribute;

/**
 * Static metamodel for the Commercial entity.
 */
@StaticMetamodel(Commercial.class)
public interface _Commercial extends _Advertisement {

    String NETWORK = "network";
    String NUMSECONDS = "numSeconds";
    String SHOWAT = "showAt";

    TextAttribute<Commercial> network = TextAttribute //
                    .of(Commercial.class, NETWORK);

    NumericAttribute<Commercial, Integer> numSeconds = NumericAttribute //
                    .of(Commercial.class, NUMSECONDS, int.class);

    TemporalAttribute<Commercial, LocalDateTime> showAt = TemporalAttribute //
                    .of(Commercial.class, SHOWAT, LocalDateTime.class);

}
