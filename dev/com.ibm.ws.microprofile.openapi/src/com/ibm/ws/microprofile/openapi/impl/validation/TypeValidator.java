/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.validation;

import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public abstract class TypeValidator<T> {

    public void validate(ValidationHelper helper, Context context, T t) {
        validate(helper, context, null, t);
    }

    public abstract void validate(ValidationHelper helper, Context context, String key, T t);

}
