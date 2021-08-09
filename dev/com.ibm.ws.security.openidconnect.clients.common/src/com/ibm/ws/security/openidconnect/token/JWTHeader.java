/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.token;

import java.util.concurrent.ConcurrentHashMap;

public abstract class JWTHeader extends ConcurrentHashMap<String, Object> implements Cloneable {

    private static final long serialVersionUID = 7181534321592821579L;

    /**
     * Header as specified in <a
     * href="http://tools.ietf.org/html/draft-ietf-oauth-json-web-token-08#section-5">JWT Header</a>.
     */

    /** Type header parameter used to declare the type of this object or {@code null} for none. */
    //("typ")
    private String type;

    /**
     * Content type header parameter used to declare structural information about the JWT or {@code null} for none.
     */
    //("cty")
    private String contentType;

    /**
     * Returns the type header parameter used to declare the type of this object or {@code null} for
     * none.
     */
    public final String getType() {
        return type;
    }

    /**
     * Sets the type header parameter used to declare the type of this object or {@code null} for
     * none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTHeader setType(String type) {
        this.type = type;
        this.put(HeaderConstants.TYPE, type);
        return this;
    }

    /**
     * Returns the content type header parameter used to declare structural information about the
     * JWT or {@code null} for none.
     */
    public final String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type header parameter used to declare structural information about the JWT
     * or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTHeader setContentType(String contentType) {
        this.contentType = contentType;
        this.put(HeaderConstants.CONTENT_TYPE, contentType);
        return this;
    }

    @Override
    public JWTHeader clone() throws CloneNotSupportedException {
        return (JWTHeader) super.clone();
    }

    public JWTHeader getHeader() {
        // TODO Auto-generated method stub
        return null;
    }
}
