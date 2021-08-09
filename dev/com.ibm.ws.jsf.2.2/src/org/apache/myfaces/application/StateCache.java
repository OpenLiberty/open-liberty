/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.application;

import javax.faces.context.FacesContext;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;

/**
 * This class provides an interface to separate the state caching operations (saving/restoring)
 * from the renderkit specific stuff that HtmlResponseStateManager should do.
 * 
 * @author Leonardo Uribe
 *
 */
public abstract class StateCache<K, V>
{
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM = "secureRandom";
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_RANDOM = "random";

    /**
     * Defines how to generate the csrf session token.
     */
    @JSFWebConfigParam(since="2.2.0", expectedValues="secureRandom, random", 
            defaultValue="secureRandom", group="state")
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_PARAM
            = "org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN";
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_PARAM_DEFAULT = 
            RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM;

    /**
     * Set the default length of the random key used for the csrf session token.
     * By default is 16. 
     */
    @JSFWebConfigParam(since="2.2.0", defaultValue="16", group="state")
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH_PARAM 
            = "org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH";
    public static final int RANDOM_KEY_IN_CSRF_SESSION_TOKEN_LENGTH_PARAM_DEFAULT = 16;

    /**
     * Sets the random class to initialize the secure random id generator. 
     * By default it uses java.security.SecureRandom
     */
    @JSFWebConfigParam(since="2.2.0", group="state")
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_CLASS_PARAM
            = "org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_CLASS";

    /**
     * Sets the random provider to initialize the secure random id generator.
     */
    @JSFWebConfigParam(since="2.2.0", group="state")
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_PROVIDER_PARAM
            = "org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_PROVIDER";
    
    /**
     * Sets the random algorithm to initialize the secure random id generator. 
     * By default is SHA1PRNG
     */
    @JSFWebConfigParam(since="2.2.0", defaultValue="SHA1PRNG", group="state")
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_ALGORITM_PARAM 
            = "org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_ALGORITM";

    /**
     * Put the state on the cache, to can be restored later.
     * 
     * @param facesContext
     * @param serializedView
     */
    public abstract K saveSerializedView(FacesContext facesContext, V serializedView);
    
    /**
     * Get the state from the cache is server side state saving is used,
     * or decode it from the passed viewState param if client side is used.
     * 
     * @param facesContext
     * @param viewId The viewId of the view to be restored
     * @param viewState A token usually retrieved from a call to ResponseStateManager.getState that will be
     *                  used to identify or restore the state.
     * @return
     */
    public abstract V restoreSerializedView(FacesContext facesContext, String viewId, K viewState);

    /**
     * Calculate the token to be used if server side state saving, or encode the view and return the
     * viewState that can be used by the underlying ResponseStateManager to write the state.
     * 
     * @param facesContext
     * @param state The state that will be used to derive the token returned.
     * @return A token (usually encoded on javax.faces.ViewState input hidden field) that will be passed to 
     *         ResponseStateManager.writeState or ResponseStateManager.getViewState to be 
     *         output to the client.
     */
    public abstract K encodeSerializedState(FacesContext facesContext, Object serializedView);
    
    /**
     * Indicates if the call to ResponseStateManager.writeState should be done after the view is fully rendered.
     * Usually this is required for client side state saving, but it is not for server side state saving, because
     * ResponseStateManager.writeState could render a just a marker and then StateManager.saveState could be called,
     * preventing use an additional buffer. 
     * 
     * @param facesContext
     * @return
     */
    public abstract boolean isWriteStateAfterRenderViewRequired(FacesContext facesContext);
    
    /**
     * @since 2.2
     * @param context
     * @return 
     */
    public abstract String createCryptographicallyStrongTokenFromSession(FacesContext context);
}
