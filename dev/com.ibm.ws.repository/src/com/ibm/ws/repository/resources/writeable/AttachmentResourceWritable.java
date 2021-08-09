/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resources.writeable;

import com.ibm.ws.repository.resources.AttachmentResource;

/**
 * A writable view of an attachment which can be stored in a repository.
 * <p>
 * An attachment is a file associated with a repository resource
 * <p>
 * The attachment itself can be read by calling {@link #getInputStream()}.
 */
public interface AttachmentResourceWritable extends AttachmentResource {

    /**
     * Sets the image dimensions in the attachment metadata
     *
     * @param height the height of the image
     * @param width the width of the image
     */
    public void setImageDimensions(int height, int width);

}