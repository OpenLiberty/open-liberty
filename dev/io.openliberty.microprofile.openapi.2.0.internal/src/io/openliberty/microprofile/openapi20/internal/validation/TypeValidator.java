/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.internal.validation;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public abstract class TypeValidator<T> {

    public void validate(ValidationHelper helper, Context context, T t) {
        validate(helper, context, null, t);
    }

    public abstract void validate(ValidationHelper helper, Context context, String key, T t);
}
