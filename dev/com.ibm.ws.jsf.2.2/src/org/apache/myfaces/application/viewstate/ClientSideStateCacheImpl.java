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
package org.apache.myfaces.application.viewstate;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.myfaces.application.StateCache;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

class ClientSideStateCacheImpl extends StateCache<Object, Object>
{
    
    /**
     * Define the time in minutes where the view state is valid when
     * client side state saving is used. By default it is set to 0
     * (infinite).
     */
    @JSFWebConfigParam(since="2.1.9, 2.0.15", defaultValue="0", group="state")
    public static final String INIT_PARAM_CLIENT_VIEW_STATE_TIMEOUT = 
            "org.apache.myfaces.CLIENT_VIEW_STATE_TIMEOUT";
    public static final Long INIT_PARAM_CLIENT_VIEW_STATE_TIMEOUT_DEFAULT = 0L;
    
    private static final int STATE_PARAM = 0;
    private static final int VIEWID_PARAM = 1;
    private static final int TIMESTAMP_PARAM = 2;
    
    private static final Object[] EMPTY_STATES = new Object[]{null, null};
    
    private Long _clientViewStateTimeout;
    
    private CsrfSessionTokenFactory csrfSessionTokenFactory;
    
    public ClientSideStateCacheImpl()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        String csrfRandomMode = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(),
                RANDOM_KEY_IN_CSRF_SESSION_TOKEN_PARAM, 
                RANDOM_KEY_IN_CSRF_SESSION_TOKEN_PARAM_DEFAULT);
        if (RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM.equals(csrfRandomMode))
        {
            csrfSessionTokenFactory = new SecureRandomCsrfSessionTokenFactory(facesContext);
        }
        else
        {
            csrfSessionTokenFactory = new RandomCsrfSessionTokenFactory(facesContext);
        }
    }

    @Override
    public Object saveSerializedView(FacesContext facesContext,
            Object serializedView)
    {
        // The state in this case the state is saved on the token sent to
        // the client (isWriteStateAfterRenderViewRequired() is set to true).
        // No additional operation is required here.
        return encodeSerializedState(facesContext, serializedView);
    }

    @Override
    public Object restoreSerializedView(FacesContext facesContext,
            String viewId, Object viewState)
    {
        Object[] state = (Object[]) viewState;
        long clientViewStateTimeout = getClientViewStateTimeout(facesContext.getExternalContext());
        
        if (clientViewStateTimeout > 0L)
        {
            Long timeStamp = (Long) state[TIMESTAMP_PARAM];
            if (timeStamp == null)
            {
                //If no timestamp, state is invalid.
                return null;
            }
            long passedTime = (System.currentTimeMillis() - timeStamp.longValue()) / 60000;
            
            if (passedTime > clientViewStateTimeout)
            {
                //expire
                return null;
            }
        }
        
        String restoredViewId = (String) state[VIEWID_PARAM];
        
        if (viewId != null && !viewId.equals(restoredViewId))
        {
            //invalid viewId, expire
            return null;
        }
        
        //return the state
        if (state[STATE_PARAM] == null)
        {
            return EMPTY_STATES;
        }
        else
        {
            Object serializedView = state[STATE_PARAM];
            if (serializedView instanceof Object[] &&
            ((Object[])serializedView).length == 2 &&
            ((Object[])serializedView)[0] == null &&
            ((Object[])serializedView)[1] == null)
            {
                // Remember inside the state null is stored as an empty array.
                return null;
            }
            
            return state[STATE_PARAM];
        }
    }

    @Override
    public Object encodeSerializedState(FacesContext facesContext,
            Object serializedView)
    {
        Object[] state = null;
        
        if (getClientViewStateTimeout(facesContext.getExternalContext()).longValue() > 0L)
        {
            state = new Object[3];
            state[TIMESTAMP_PARAM] = System.currentTimeMillis();
        }
        else
        {
            state = new Object[2];
        }
        
        if (serializedView == null)
        {
            state[STATE_PARAM] = EMPTY_STATES;
        }
        else if (serializedView instanceof Object[] &&
            ((Object[])serializedView).length == 2 &&
            ((Object[])serializedView)[0] == null &&
            ((Object[])serializedView)[1] == null)
        {
            // The generated state can be considered zero, set it as null
            // into the map.
            state[STATE_PARAM] = null;
        }
        else
        {
            state[STATE_PARAM] = serializedView;
        }

        state[VIEWID_PARAM] = facesContext.getViewRoot().getViewId();
        
        return state;
    }

    @Override
    public boolean isWriteStateAfterRenderViewRequired(FacesContext facesContext)
    {
        return true;
    }

    /**
     * @return the _clientViewStateTimeout
     */
    protected Long getClientViewStateTimeout(ExternalContext context)
    {
        if (_clientViewStateTimeout == null)
        {
            _clientViewStateTimeout = WebConfigParamUtils.getLongInitParameter(
                    context, INIT_PARAM_CLIENT_VIEW_STATE_TIMEOUT,
                    INIT_PARAM_CLIENT_VIEW_STATE_TIMEOUT_DEFAULT);
            if (_clientViewStateTimeout.longValue() < 0L)
            {
                _clientViewStateTimeout = 0L;
            }
        }
        return _clientViewStateTimeout;
    }

    @Override
    public String createCryptographicallyStrongTokenFromSession(FacesContext context)
    {
        return csrfSessionTokenFactory.createCryptographicallyStrongTokenFromSession(context);
    }
}
