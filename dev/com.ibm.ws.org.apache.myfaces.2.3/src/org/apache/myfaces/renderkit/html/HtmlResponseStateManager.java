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
package org.apache.myfaces.renderkit.html;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.lifecycle.ClientWindow;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.ResponseStateManager;

import org.apache.myfaces.application.StateCache;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.renderkit.MyfacesResponseStateManager;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.spi.StateCacheProvider;
import org.apache.myfaces.spi.StateCacheProviderFactory;

/**
 * @author Manfred Geiler (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public class HtmlResponseStateManager extends MyfacesResponseStateManager
{
    private static final Logger log = Logger.getLogger(HtmlResponseStateManager.class.getName());

    public static final String STANDARD_STATE_SAVING_PARAM = "javax.faces.ViewState";
    
    private static final String VIEW_STATE_COUNTER = "oam.partial.VIEW_STATE_COUNTER";
    private static final String CLIENT_WINDOW_COUNTER = "oam.partial.CLIENT_WINDOW_COUNTER";
    
    private static final String SESSION_TOKEN = "oam.rsm.SESSION_TOKEN";
    
    /**
     * Add autocomplete="off" to the view state hidden field. Enabled (true) by default.
     */
    @JSFWebConfigParam(since="2.2.8, 2.1.18, 2.0.24", expectedValues="true, false, one-time-code", 
           defaultValue="true", group="state")
    public static final String INIT_PARAM_AUTOCOMPLETE_OFF_VIEW_STATE = 
            "org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE";
            
    private StateCacheProvider _stateCacheFactory;
    
    private String _autoCompleteOffViewState;

    public HtmlResponseStateManager()
    {
        _autoCompleteOffViewState = null;
    }
    
    @Override
    public void writeState(FacesContext facesContext, Object state) throws IOException
    {
        ResponseWriter responseWriter = facesContext.getResponseWriter();

        Object savedStateObject = null;
        
        if (!facesContext.getViewRoot().isTransient())
        {
            // Only if the view is not transient needs to be saved
            savedStateObject = getStateCache(facesContext).encodeSerializedState(facesContext, state);
        }

        // write the view state field
        writeViewStateField(facesContext, responseWriter, savedStateObject);

        // renderKitId field
        writeRenderKitIdField(facesContext, responseWriter);
        
        // windowId field
        writeWindowIdField(facesContext, responseWriter);
    }
    
    private void writeWindowIdField(FacesContext facesContext, ResponseWriter responseWriter) throws IOException
    {
        ClientWindow clientWindow = facesContext.getExternalContext().getClientWindow();
        if (clientWindow != null)
        {
            responseWriter.startElement(HTML.INPUT_ELEM, null);
            responseWriter.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_HIDDEN, null);
            responseWriter.writeAttribute(HTML.ID_ATTR, generateUpdateClientWindowId(facesContext), null);
            responseWriter.writeAttribute(HTML.NAME_ATTR, ResponseStateManager.CLIENT_WINDOW_PARAM, null);
            responseWriter.writeAttribute(HTML.VALUE_ATTR, clientWindow.getId(), null);
            responseWriter.endElement(HTML.INPUT_ELEM);
        }
    }
    
    @Override
    public void saveState(FacesContext facesContext, Object state)
    {
        if (!facesContext.getViewRoot().isTransient())
        {
            getStateCache(facesContext).saveSerializedView(facesContext, state);
        }
    }

    private void writeViewStateField(FacesContext facesContext, ResponseWriter responseWriter, Object savedState)
        throws IOException
    {
        String serializedState = getStateCache(facesContext).getStateTokenProcessor(facesContext)
                .encode(facesContext, savedState);

        if (log.isLoggable(Level.FINE)) 
        {
             log.fine("Writing serialized viewstate string with hashCode : " + serializedState.hashCode());
        } 

        ExternalContext extContext = facesContext.getExternalContext();
        MyfacesConfig myfacesConfig = MyfacesConfig.getCurrentInstance(extContext);

        responseWriter.startElement(HTML.INPUT_ELEM, null);
        responseWriter.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_HIDDEN, null);
        responseWriter.writeAttribute(HTML.NAME_ATTR, STANDARD_STATE_SAVING_PARAM, null);
        if (myfacesConfig.isRenderViewStateId())
        {
            // responseWriter.writeAttribute(HTML.ID_ATTR, STANDARD_STATE_SAVING_PARAM, null);
            // JSF 2.2 if javax.faces.ViewState is used as the id, in portlet
            // case it will be duplicate ids and that not xml friendly.
            responseWriter.writeAttribute(HTML.ID_ATTR,
                HtmlResponseStateManager.generateUpdateViewStateId(
                    facesContext), null);
        }
        responseWriter.writeAttribute(HTML.VALUE_ATTR, serializedState, null);
        // only off, null, or one-time-code
        String autoCompleteValue = this.getAutocompleteViewStateValue(facesContext);
        if (autoCompleteValue != null)
        {
            responseWriter.writeAttribute(HTML.AUTOCOMPLETE_ATTR, autoCompleteValue, null);
        }
        responseWriter.endElement(HTML.INPUT_ELEM);
    }

    private void writeRenderKitIdField(FacesContext facesContext, ResponseWriter responseWriter) throws IOException
    {

        String defaultRenderKitId = facesContext.getApplication().getDefaultRenderKitId();
        if (defaultRenderKitId != null && !RenderKitFactory.HTML_BASIC_RENDER_KIT.equals(defaultRenderKitId))
        {
            responseWriter.startElement(HTML.INPUT_ELEM, null);
            responseWriter.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_HIDDEN, null);
            responseWriter.writeAttribute(HTML.NAME_ATTR, ResponseStateManager.RENDER_KIT_ID_PARAM, null);
            responseWriter.writeAttribute(HTML.VALUE_ATTR, defaultRenderKitId, null);
            responseWriter.endElement(HTML.INPUT_ELEM);
        }
    }

    @Override
    public Object getState(FacesContext facesContext, String viewId)
    {
        Object savedState = getSavedState(facesContext);
        if (savedState == null)
        {
            return null;
        }

        return getStateCache(facesContext).restoreSerializedView(facesContext, viewId, savedState);
    }

    /**
     * Reconstructs the state from the "javax.faces.ViewState" request parameter.
     * 
     * @param facesContext
     *            the current FacesContext
     * 
     * @return the reconstructed state, or <code>null</code> if there was no saved state
     */
    private Object getSavedState(FacesContext facesContext)
    {
        Object encodedState = 
            facesContext.getExternalContext().getRequestParameterMap().get(STANDARD_STATE_SAVING_PARAM);
        if(encodedState==null || (((String) encodedState).length() == 0))
        {
            return null;
        }

        Object savedStateObject = getStateCache(facesContext).getStateTokenProcessor(facesContext)
                .decode(facesContext, (String)encodedState);
        
        return savedStateObject;
    }

    /**
     * Checks if the current request is a postback
     * 
     * @since 1.2
     */
    @Override
    public boolean isPostback(FacesContext context)
    {
        return context.getExternalContext().getRequestParameterMap().containsKey(ResponseStateManager.VIEW_STATE_PARAM);
    }

    @Override
    public String getViewState(FacesContext facesContext, Object baseState)
    {
        // If the view is transient, baseState is null, so it should return null.
        // In this way, PartialViewContext will skip <update ...> section related
        // to view state (stateless view does not have state, so it does not need
        // to update the view state section). 
        if (baseState == null)
        {
            return null;
        }
        if (facesContext.getViewRoot().isTransient())
        {
            return null;
        }
        
        Object state = getStateCache(facesContext).saveSerializedView(facesContext, baseState);

        return getStateCache(facesContext).getStateTokenProcessor(facesContext).encode(facesContext, state);
    }

    @Override
    public boolean isStateless(FacesContext context, String viewId)
    {
        if (context.isPostback())
        {
            String encodedState = 
                context.getExternalContext().getRequestParameterMap().get(STANDARD_STATE_SAVING_PARAM);
            if(encodedState==null || (((String) encodedState).length() == 0))
            {
                return false;
            }

            return getStateCache(context).getStateTokenProcessor(context).isStateless(context, encodedState);
        }
        else 
        {
            // "... java.lang.IllegalStateException - if this method is invoked 
            // and the statefulness of the preceding call to writeState(
            // javax.faces.context.FacesContext, java.lang.Object) cannot be determined.
            throw new IllegalStateException(
                "Cannot decide if the view is stateless or not, since the request is "
                + "not postback (no preceding writeState(...)).");
        }
    }

    @Override
    public String getCryptographicallyStrongTokenFromSession(FacesContext context)
    {
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        String savedToken = (String) sessionMap.get(SESSION_TOKEN);
        if (savedToken == null)
        {
            savedToken = getStateCache(context).createCryptographicallyStrongTokenFromSession(context);
            sessionMap.put(SESSION_TOKEN, savedToken);
        }
        return savedToken;
    }
    
    @Override
    public boolean isWriteStateAfterRenderViewRequired(FacesContext facesContext)
    {
        return getStateCache(facesContext).isWriteStateAfterRenderViewRequired(facesContext);
    }

    protected StateCache getStateCache(FacesContext facesContext)
    {
        if (_stateCacheFactory == null)
        {
            _stateCacheFactory = StateCacheProviderFactory
                    .getStateCacheProviderFactory(facesContext.getExternalContext())
                    .getStateCacheProvider(facesContext.getExternalContext());
        }
        return _stateCacheFactory.getStateCache(facesContext);
    }

    public static String generateUpdateClientWindowId(FacesContext facesContext)
    {
        // JSF 2.2 section 2.2.6.1 Partial State Rendering
        // According to the javascript doc of jsf.ajax.response,
        //
        // The new syntax looks like this:
        // <update id="<VIEW_ROOT_CONTAINER_CLIENT_ID><SEP>javax.faces.ClientWindow<SEP><UNIQUE_PER_VIEW_NUMBER>">
        //    <![CDATA[...]]>
        // </update>
        //
        // UNIQUE_PER_VIEW_NUMBER aim for portlet case. In that case it is possible to have
        // multiple sections for update. In servlet case there is only one update section per
        // ajax request.
        
        String id;
        char separator = facesContext.getNamingContainerSeparatorChar();
        Integer count = (Integer) facesContext.getAttributes().get(CLIENT_WINDOW_COUNTER);
        if (count == null)
        {
            count = Integer.valueOf(0);
        }
        count += 1;
        id = facesContext.getViewRoot().getContainerClientId(facesContext) + 
            separator + ResponseStateManager.CLIENT_WINDOW_PARAM + separator + count;
        facesContext.getAttributes().put(CLIENT_WINDOW_COUNTER, count);
        return id;
    }
    
    public static String generateUpdateViewStateId(FacesContext facesContext)
    {
        // JSF 2.2 section 2.2.6.1 Partial State Rendering
        // According to the javascript doc of jsf.ajax.response,
        //
        // The new syntax looks like this:
        // <update id="<VIEW_ROOT_CONTAINER_CLIENT_ID><SEP>javax.faces.ViewState<SEP><UNIQUE_PER_VIEW_NUMBER>">
        //    <![CDATA[...]]>
        // </update>
        //
        // UNIQUE_PER_VIEW_NUMBER aim for portlet case. In that case it is possible to have
        // multiple sections for update. In servlet case there is only one update section per
        // ajax request.
        
        String id;
        char separator = facesContext.getNamingContainerSeparatorChar();
        Integer count = (Integer) facesContext.getAttributes().get(VIEW_STATE_COUNTER);
        if (count == null)
        {
            count = Integer.valueOf(0);
        }
        count += 1;
        id = facesContext.getViewRoot().getContainerClientId(facesContext) + 
            separator + ResponseStateManager.VIEW_STATE_PARAM + separator + count;
        facesContext.getAttributes().put(VIEW_STATE_COUNTER, count);
        return id;
    }

    // Changed from Boolean to String in MYFACES-4721
    private String getAutocompleteViewStateValue(FacesContext facesContext)
    {
        if(_autoCompleteOffViewState == null) 
        {
            _autoCompleteOffViewState = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(),
            INIT_PARAM_AUTOCOMPLETE_OFF_VIEW_STATE, "true");
    
            _autoCompleteOffViewState =  _autoCompleteOffViewState.toLowerCase();
    
            if(_autoCompleteOffViewState.equals("false"))
            {
                _autoCompleteOffViewState = null;  // null means autocomplete isn't written
            }
            else if(_autoCompleteOffViewState.equals("one-time-code"))
            {
               // keep it as is
            } 
            else // default (true) and wrong config scenario
            {
                _autoCompleteOffViewState = "off";
            }
        }

        return _autoCompleteOffViewState;
    }
}
