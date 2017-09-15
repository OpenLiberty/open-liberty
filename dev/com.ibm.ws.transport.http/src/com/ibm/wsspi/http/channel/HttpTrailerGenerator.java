/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel;

import com.ibm.wsspi.genericbnf.HeaderKeys;

/**
 * <code>HttpTrailerGenerator</code> defines an interface for
 * creating the values for a trailer dynamically.
 * 
 * <p>
 * These objects are intended to be called after the 0-byte chunk is sent.
 * </p>
 * 
 * @ibm-private-in-use
 */
public interface HttpTrailerGenerator {

    /**
     * Create a value for a specifc trailer.
     * 
     * @param hdr
     *            the HTTP header to generate as a trailer.
     * @param message
     *            the message to append the trailer to.
     * @return byte[] - the value of the trailer.
     */
    byte[] generateTrailerValue(HeaderKeys hdr, HttpTrailers message);

    /**
     * Create a value for a specifc trailer.
     * 
     * @param hdr
     *            the HTTP header to generate as a trailer.
     * @param message
     *            the message to append the trailer to.
     * @return byte[] - the value of the trailer.
     */
    byte[] generateTrailerValue(String hdr, HttpTrailers message);

}
