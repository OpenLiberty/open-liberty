/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.extraproviders;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

public class NoAnnotationNoPublicConstructorProvider implements DynamicFeature {

    private NoAnnotationNoPublicConstructorProvider() {
        System.out.println("private no-arg ctor");
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.container.DynamicFeature#configure(javax.ws.rs.container.ResourceInfo, javax.ws.rs.core.FeatureContext)
     */
    @Override
    public void configure(ResourceInfo arg0, FeatureContext arg1) {
        throw new UnsupportedOperationException();

    }

}
