/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.jaxrs20.multipart;

import java.util.List;

import javax.ws.rs.core.MediaType;

/**
 * This interface abstracts the IBM API for MultipartBody operations.
 */
public interface IMultipartBody {

    /**
     * Retrieve MediaType for the IMultipartBody implementation object.
     * 
     * @return MediaType for the IMultipartBody implementation object.
     */
    public MediaType getType();

    /**
     * Retrieve all IAttachment in the IMultipartBody implementation object.
     * 
     * @return List of all IAttachment in the IMultipartBody implementation object.
     */
    public List<IAttachment> getAllAttachments();

    /**
     * Retrieve all child IAttachment in the IMultipartBody implementation object.
     * 
     * @return List of all child IAttachment in the IMultipartBody implementation object.
     */
    public List<IAttachment> getChildAttachments();

    /**
     * Retrieve the root IAttachment in the IMultipartBody implementation object.
     * 
     * @return the root IAttachment in the IMultipartBody implementation object.
     */
    public IAttachment getRootAttachment();

    /**
     * Retrieve IAttachment in the IMultipartBody implementation object according to the contentId.
     * 
     * @param content-id value in header
     * @return the IAttachment in the IMultipartBody implementation object according to the contentId.
     */
    public IAttachment getAttachment(String contentId);

}
