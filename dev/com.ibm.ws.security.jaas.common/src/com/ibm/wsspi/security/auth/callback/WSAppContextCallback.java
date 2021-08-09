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

import java.util.Map;

import javax.security.auth.callback.Callback;

/**
 * <p>
 * The <code>WSAppContextCallback</code> allows an Context object to be gathered by
 * <code>CallbackHandler</code> and pass it to the <code>LoginModule</code> stack.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 * @ibm-spi
 */
public class WSAppContextCallback implements Callback {

    @SuppressWarnings("unchecked")
    private Map context;
    private final String prompt;

    /**
     * <p>
     * Construct a <code>WSAppContextCallback</code> object with a prompt hint.
     * </p>
     * 
     * @param prompt The prompt hint.
     */
    public WSAppContextCallback(String prompt) {
        this.prompt = prompt;
    }

    /**
     * <p>
     * Construct a <code>WSAppContextCallback</code> object with a prompt hint and
     * an Context instance.
     * </p>
     * 
     * @param prompt The prompt hint.
     * @param Context context
     */
    @SuppressWarnings("unchecked")
    public WSAppContextCallback(String prompt, Map context) {
        this.prompt = prompt;
        this.context = context;
    }

    /**
     * <p>
     * Set the application context.
     * </p>
     * 
     * @param context: The application context.
     */
    @SuppressWarnings("unchecked")
    public void setContext(Map context) {
        this.context = context;
    }

    /**
     * <p>
     * Return the Context. If the Context instance set in
     * Constructor is <code>null</code>, then <code>null</code> is returned.
     * </p>
     * 
     * @return The Context, could be <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public Map getContext() {
        return context;
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
