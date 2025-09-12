/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.restfulWS.internal.common.components;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openliberty.restfulWS.internal.common.api.RestfulWSEJBUtils;

@Component(name = "io.openliberty.restfulWS.internal.common.components.RestfulWSEJBUtilsLocator", immediate = true, property = { "service.vendor=IBM" })
public class RestfulWSEJBUtilsLocator {

    private static RestfulWSEJBUtils restfulWSEJBUtils;

    @Reference(service = RestfulWSEJBUtils.class, name = "io.openliberty.restfulws.internal.ejb.components.RestfulWSModuleMetaDataListener")
    public void setRestfulWSEJBUtils(RestfulWSEJBUtils restfulWSEJBUtils) {
        RestfulWSEJBUtilsLocator.restfulWSEJBUtils = restfulWSEJBUtils;
    }

    public static RestfulWSEJBUtils getRestfulWSEJBUtils() {
        return restfulWSEJBUtils;
    }
}
