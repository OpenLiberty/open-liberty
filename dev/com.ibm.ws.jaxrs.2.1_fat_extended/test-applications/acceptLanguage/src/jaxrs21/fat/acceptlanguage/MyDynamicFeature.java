/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.acceptlanguage;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class MyDynamicFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resInfo, FeatureContext context) {
        if ("getAcceptableLanguagesFromRequestFilter".equals(resInfo.getResourceMethod().getName())) {
            context.register(MyFilter.class);
        }
    }
}
