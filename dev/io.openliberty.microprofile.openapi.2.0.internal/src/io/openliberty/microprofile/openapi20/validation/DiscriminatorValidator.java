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

import org.eclipse.microprofile.openapi.models.media.Discriminator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.smallrye.openapi.runtime.io.discriminator.DiscriminatorConstant;

/**
 *
 */
public class DiscriminatorValidator extends TypeValidator<Discriminator> {

    private static final TraceComponent tc = Tr.register(DiscriminatorValidator.class);

    private static final DiscriminatorValidator INSTANCE = new DiscriminatorValidator();

    public static DiscriminatorValidator getInstance() {
        return INSTANCE;
    }

    private DiscriminatorValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Discriminator t) {

        if (t != null) {
            ValidatorUtils.validateRequiredField(t.getPropertyName(), context, DiscriminatorConstant.PROP_PROPERTY_NAME).ifPresent(helper::addValidationEvent);
        }
    }
}
