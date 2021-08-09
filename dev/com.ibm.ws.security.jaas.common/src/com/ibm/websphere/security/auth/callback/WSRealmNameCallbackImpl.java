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
package com.ibm.websphere.security.auth.callback;

import javax.security.auth.callback.Callback;

/**
 * <p>
 * The <code>WSRealmNameCallbackImpl</code> allows realm name to be gathered by
 * <code>CallbackHandler</code> and pass it to the <code>LoginModule</code>.
 * </p>
 * 
 * @ibm-api
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 * @ibm-spi
 */
public class WSRealmNameCallbackImpl implements Callback {

    private String defaultRealmName;
    private String realmName;
    private final String prompt;

    /**
     * <p>
     * Construct a <code>WSRealmNameCallbackImpl</code> object with a prompt hint.
     * </p>
     * 
     * @param prompt The prompt hint.
     */
    public WSRealmNameCallbackImpl(String prompt) {
        this.prompt = prompt;
    }

    /**
     * <p>
     * Construct a <code>WSRealmNameCallbackImpl</code> object with a prompt hint and
     * a default realm name.
     * </p>
     * 
     * @param prompt The prompt hint.
     * @param defaultRealmName
     *            The default realm name.
     */
    public WSRealmNameCallbackImpl(String prompt, String defaultRealmName) {
        this.prompt = prompt;
        this.defaultRealmName = defaultRealmName;
    }

    /**
     * <p>
     * Set the realm name.
     * </p>
     * 
     * @param realmName The realm name.
     */
    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    /**
     * <p>
     * Return the realm name. If the realm name set in <code>WSRealmNameCallbackImpl.setRealmName()</code>
     * is <code>null</code>, then <code>null</code> is returned.
     * </p>
     * 
     * @return The realm name, could be <code>null</code>.
     */
    public String getRealmName() {
        if (realmName == null || realmName.trim().equals("")) {
            realmName = getDefaultRealmName();
        }
        return realmName;
    }

    /**
     * <p>
     * Return the default realm name. If the default realm name set in Constructor
     * is <code>null</code>, then <code>null</code> is returned.
     * </p>
     * 
     * @return The default realm name, could be <code>null</code>.
     */
    public String getDefaultRealmName() {
        if (defaultRealmName == null || defaultRealmName.equals("")) {
            // Set to <default> since there is currently no application realm in the process.
            defaultRealmName = "defaultRealm";
        }
        return defaultRealmName;
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
