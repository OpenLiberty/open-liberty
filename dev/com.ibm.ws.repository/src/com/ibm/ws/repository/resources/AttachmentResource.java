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
package com.ibm.ws.repository.resources;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;

/**
 * A read-only view of an attachment stored in a repository.
 * <p>
 * An attachment is a file associated with a repository resource
 * <p>
 * The attachment itself can be read by calling {@link #getInputStream()}.
 */
public interface AttachmentResource {

    /**
     * Gets the repository connection which this resource is associated with
     *
     * @return the associated repository connection
     */
    public RepositoryConnection getRepositoryConnection();

    /**
     * Gets the attachment ID
     * <p>
     * This is an arbitrary string which uniquely identifies the attachment within the resource it is attached to. Attachment IDs may change if the resource is updated in the
     * repository.
     *
     * @return the attachment ID, or null if the attachment was not retrieved from a repository
     */
    public String getId();

    /**
     * Gets the name of the attachment (which is usually the filename of
     * the attachment, but not for features).
     *
     * @return The name of the attachment
     */
    public String getName();

    /**
     * Gets the attachment type
     *
     * @return The {@link AttachmentType attachment type} for the attachment
     */
    public AttachmentType getType();

    /**
     * Gets an input stream that can be used to read the attachment contents from
     *
     * @return An input stream where the attachment can be read from
     * @throws RepositoryBackendException
     * @throws RepositoryBadDataException
     */
    public InputStream getInputStream() throws RepositoryBackendException, RepositoryBadDataException;

    /**
     * Returns a URL which points to the attachment content
     * <p>
     * Usually clients should call {@link #getInputStream()} to get the content of the attachment rather than retrieving it from a URL but having the actual URL can be useful for
     * logging/debugging.
     *
     * @return the URL where the attachment content is available
     */
    public String getURL();

    /**
     * Gets the size (in bytes) of the attachment
     *
     * @return The size (in bytes) of the attachment
     */
    public long getSize();

    /**
     * Gets the locale of the attachment.
     * <p>
     * If the attachment is not associated with any particular locale then this will return null.
     *
     * @return the locale for the attachment
     */
    public Locale getLocale();

    /**
     * Gets the CRC of the attachment
     *
     * @return the CRC of the attachment
     */
    public long getCRC();

    /**
     * Gets the height of the image if this attachment is an image
     * <p>
     * If the attachment is not an image, this method will return 0.
     *
     * @return the height of the attached image, or 0 if the attachment is not an image
     */
    public int getImageHeight();

    /**
     * Gets the width of the image if this attachment is an image
     * <p>
     * If the attachment is not an image, this method will return 0.
     *
     * @return the width of the attached image, or 0 if the attachment is not an image
     */
    public int getImageWidth();

    /**
     * Dumps a formatted JSON string to the supplied out stream
     * <p>
     * This is mostly useful for debugging
     *
     * @param os The output stream the JSON should be dumped too.
     */
    public void dump(OutputStream os);

}