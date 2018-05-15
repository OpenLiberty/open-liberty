/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.spec1346;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletCache;
import javax.faces.view.facelets.FaceletCacheFactory;

/**
 *
 */
public class CustomFaceletCacheFactory extends FaceletCacheFactory {

    private final FaceletCacheFactory wrapped;

    public CustomFaceletCacheFactory(FaceletCacheFactory wrapped) {
        this.wrapped = wrapped;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.view.facelets.FaceletCacheFactory#getFaceletCache()
     */
    @Override
    public FaceletCache getFaceletCache() {
        Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        requestMap.put("CustomFaceletCacheFactory", "getFaceletCache Invoked!");

        return new CustomFaceletCache();
    }

    @Override
    public FaceletCacheFactory getWrapped() {
        return wrapped;
    }

}
