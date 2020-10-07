/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip.header;

/**
 * <p>
 * This interface represents the Content-Encoding entity-header.
 * A ContentEncodingHeader is used as a modifier to the
 * "media-type". When present, its value indicates what additional
 * content codings have been applied to the entity-body, and thus what
 * decoding mechanisms must be applied in order to obtain the media-type
 * referenced by the ContentTypeHeader. The ContentEncodingHeader is
 * primarily used to allow a body to be compressed without losing the
 * identity of its underlying media type.
 * </p><p>
 * If multiple encodings have been applied to an entity, the ContentEncodingHeaders
 * must be listed in the order in which they were applied.
 * </p><p>
 * All content-coding values are case-insensitive. The Internet Assigned
 * Numbers Authority (IANA) acts as a registry for content-coding values
 * </p><p>
 * Clients may apply content encodings to the body in Requests. If the
 * server is not capable of decoding the body, or does not recognize any
 * of the content-coding values, it must send a UNSUPPORTED_MEDIA_TYPE
 * Response, listing acceptable encodings in an AcceptEncodingHeader.
 * A server may apply content encodings to the bodies in
 * Response. The server must only use encodings listed in the AcceptEncodingHeader
 * in the Response.
 *
 * @version 1.0
 *
 */
public interface ContentEncodingHeader extends EncodingHeader
{
    
    /**
     * Name of ContentEncodingHeader
     */
    public final static String name = "Content-Encoding";
}
