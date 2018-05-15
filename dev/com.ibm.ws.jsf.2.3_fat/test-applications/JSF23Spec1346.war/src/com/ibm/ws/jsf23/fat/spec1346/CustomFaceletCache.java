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

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletCache;

/**
 *
 */
public class CustomFaceletCache extends FaceletCache {

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.view.facelets.FaceletCache#getFacelet(java.net.URL)
     */
    @Override
    public Object getFacelet(URL arg0) throws IOException {
        Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        requestMap.put("CustomFaceletCache", "getFacelet Invoked!");
        return getMemberFactory().newInstance(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.view.facelets.FaceletCache#getViewMetadataFacelet(java.net.URL)
     */
    @Override
    public Object getViewMetadataFacelet(URL arg0) throws IOException {
        return getMemberFactory().newInstance(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.view.facelets.FaceletCache#isFaceletCached(java.net.URL)
     */
    @Override
    public boolean isFaceletCached(URL arg0) {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.view.facelets.FaceletCache#isViewMetadataFaceletCached(java.net.URL)
     */
    @Override
    public boolean isViewMetadataFaceletCached(URL arg0) {
        return false;
    }

}
