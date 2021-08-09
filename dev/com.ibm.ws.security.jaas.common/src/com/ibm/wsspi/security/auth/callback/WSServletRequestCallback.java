/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.auth.callback;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * The <code>WSServletRequestCallback</code> allows an HttpServletRequest object to be gathered by
 * <code>CallbackHandler</code> and pass it to the <code>LoginModule</code> stack.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 * @ibm-spi
 */
public class WSServletRequestCallback implements Callback {

    private HttpServletRequest req;
    private final String prompt;

    /**
     * <p>
     * Construct a <code>WSServletRequestCallback</code> object with a prompt hint.
     * </p>
     * 
     * @param prompt The prompt hint.
     */
    public WSServletRequestCallback(String prompt) {
        this.prompt = prompt;
    }

    /**
     * <p>
     * Construct a <code>WSServletRequestCallback</code> object with a prompt hint and
     * an HttpServletRequest instance.
     * </p>
     * 
     * @param prompt The prompt hint.
     * @param HttpServletRequest req
     */
    public WSServletRequestCallback(String prompt, HttpServletRequest req) {
        this.prompt = prompt;
        this.req = req;
    }

    /**
     * <p>
     * Set the HttpServletRequest instance.
     * </p>
     * 
     * @param req The HttpServletRequest object.
     */
    public void setHttpServletRequest(HttpServletRequest req) {
        this.req = req;
    }

    /**
     * <p>
     * Return the HttpServletRequest. If the HttpServletRequest instance set in
     * Constructor is <code>null</code>, then <code>null</code> is returned.
     * </p>
     * 
     * @return The HttpServletRequest, could be <code>null</code>.
     */
    public HttpServletRequest getHttpServletRequest() {
        return req;
    }

    /**
     * <p>
     * Return the prompt. If the prompt set in Constructor
     * is <code>null</code>, then <code>null</code> is returned.
     * </p>
     * 
     * @return The prompt, could be <code>null</code>.
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * <p>
     * Returns the name of the Callback. Typically, it is the name of the class.
     * </p>
     * 
     * @return The name of the Callback.
     */
    @Override
    public String toString() {
        return getClass().getName();
    }

}
