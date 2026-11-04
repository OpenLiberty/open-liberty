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

import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;

/**
 * Static metamodel for the Advertisement entity.
 */
@StaticMetamodel(Advertisement.class)
public interface _Advertisement {

    String ID = "id";
    String SPONSOR = "sponsor";

    NumericAttribute<Advertisement, Integer> id = NumericAttribute //
                    .of(Advertisement.class, ID, int.class);

    TextAttribute<Advertisement> sponsor = TextAttribute //
                    .of(Advertisement.class, SPONSOR);
}
