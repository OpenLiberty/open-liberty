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

import javax.activation.DataHandler;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * This interface represents an attachment; generally a multipart part.
 */
public interface IAttachment {

    /**
     * Retrieve the value for "Content-ID" in headers.
     * 
     * @return the value for "Content-ID" in headers.
     */
    public String getContentId();

    /**
     * Retrieve the value for "Content-Type" in headers.
     * 
     * @return the value for "Content-Type" in headers.
     */
    public MediaType getContentType();

    /**
     * Retrieve DataHandler of IAttachmetn.
     * 
     * @return DataHandler of IAttachmetn.
     */
    public DataHandler getDataHandler();

    /**
     * Retrieve the value whose key is name in header.
     * 
     * @param name - the key in header.
     * @return
     */
    public String getHeader(String name);

    /**
     * Retrieve header in IAttachment.
     * 
     * @return header in IAttachment.
     */
    public MultivaluedMap<String, String> getHeaders();

}
