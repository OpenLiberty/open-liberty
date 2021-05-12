/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation;

import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.smallrye.openapi.runtime.io.tag.TagConstant;

/**
 *
 */
public class TagValidator extends TypeValidator<Tag> {

    private static final TraceComponent tc = Tr.register(TagValidator.class);

    private static final TagValidator INSTANCE = new TagValidator();

    public static TagValidator getInstance() {
        return INSTANCE;
    }

    private TagValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Tag t) {
        if (t != null) {
            ValidatorUtils.validateRequiredField(t.getName(), context, TagConstant.PROP_NAME).ifPresent(helper::addValidationEvent);
        }
    }
}
