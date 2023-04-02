/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.security.common.serialization.Beta;

public class GsonStrategies {

    /**
     * Strategy used to avoid serializing member variables with the @Beta annotation unless we're using a beta image.
     */
    public static ExclusionStrategy BETA_STRATEGY = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes field) {
            boolean hasBetaAnnotation = field.getAnnotation(Beta.class) != null;
            boolean isRunningBeta = ProductInfo.getBetaEdition();
            return hasBetaAnnotation && !isRunningBeta;
        }
    };

}
