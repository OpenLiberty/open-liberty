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
 * This interface represents the Accept-Encoding request-header.
 * A client includes an AcceptEncodingHeader in a Request to
 * tell the server what coding schemes are acceptable in the Response
 * e.g. compress, gzip.
 * </p><p>
 * If no AcceptEncodingHeader is present in a Request, the server may
 * assume that the client will accept any content coding. If an
 * AcceptEncodingHeader is present, and if the server cannot send a Response
 * which is acceptable according to the AcceptEncodingHeader, then the
 * server should return a Response with a status code of NOT_ACCEPTABLE.
 * An empty encoding value indicates none are acceptable.
 * </p>
 *
 * @see EncodingHeader
 *
 * @version 1.0
 *
 */
public interface AcceptEncodingHeader extends EncodingHeader
{
    
    /**
     * Name of AcceptEncodingHeader
     */
    public final static String name = "Accept-Encoding";
}
