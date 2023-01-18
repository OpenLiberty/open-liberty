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
package org.apache.myfaces.context.flash;

import org.apache.myfaces.util.lang.SubKeyMap;
import org.apache.myfaces.config.webparameters.MyfacesConfig;
import org.apache.myfaces.util.ExternalContextUtils;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import jakarta.faces.event.PhaseId;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.faces.event.PostKeepFlashValueEvent;
import jakarta.faces.event.PostPutFlashValueEvent;
import jakarta.faces.event.PreClearFlashEvent;
import jakarta.faces.event.PreRemoveFlashValueEvent;
import jakarta.faces.lifecycle.ClientWindow;
import org.apache.myfaces.util.ExternalSpecifications;

import org.apache.myfaces.util.lang.StringUtils;
import org.apache.myfaces.util.token.TokenGenerator;

/**
 * Implementation of Flash object
 */
public class FlashImpl extends Flash implements ReleasableFlash
{
    
    // ~ static fields --------------------------------------------------------
    
    private static final Logger log = Logger.getLogger(FlashImpl.class.getName());
    
    /**
     * Use this prefix instead of the whole class name, because
     * this makes the Cookies and the SubKeyMap operations (actually
     * every String based operation where this is used as a key) faster.
     */
    private static final String FLASH_PREFIX = "oam.Flash";
    
    /**
     * Key on application map to keep current instance
     */
    static final String FLASH_INSTANCE = FLASH_PREFIX + ".INSTANCE";

    /**
     * Key to store if this setRedirect(true) was called on this request,
     * and to store the redirect Cookie.
     */
    static final String FLASH_REDIRECT = FLASH_PREFIX + ".REDIRECT";
    
    /**
     * Key to store the value of the redirect cookie
     */
    public static final String FLASH_PREVIOUS_REQUEST_REDIRECT 
            = FLASH_PREFIX + ".REDIRECT.PREVIOUSREQUEST";
    
    /**
     * Key used to check if this request should keep messages
     */
    static final String FLASH_KEEP_MESSAGES = FLASH_PREFIX + ".KEEP_MESSAGES";

    /**
     * Key used to store the messages list in the render FlashMap.
     */
    static final String FLASH_KEEP_MESSAGES_LIST = "KEEPMESSAGESLIST";

    /**
     * Session map prefix to flash maps
     */
    static final String FLASH_SESSION_MAP_SUBKEY_PREFIX = FLASH_PREFIX + ".SCOPE";
    
    /**
     * Key for the cached render FlashMap instance on the request map.
     */
    static final String FLASH_RENDER_MAP = FLASH_PREFIX + ".RENDERMAP";
    
    /**
     * Key for the current render FlashMap token.
     */
    static final String FLASH_RENDER_MAP_TOKEN = FLASH_PREFIX + ".RENDERMAP.TOKEN";
    
    /**
     * Key for the cached execute FlashMap instance on the request map.
     */
    static final String FLASH_EXECUTE_MAP = FLASH_PREFIX + ".EXECUTEMAP";

    /**
     * Key for the current execute FlashMap token.
     */
    static final String FLASH_EXECUTE_MAP_TOKEN = FLASH_PREFIX + ".EXECUTEMAP.TOKEN";
    
    static final String FLASH_CW_LRU_MAP = FLASH_PREFIX + ".CW.LRUMAP";
    
    /**
     * Token separator.
     */
    static final char SEPARATOR_CHAR = '.';
    
     // ~ static methods  -----------------------------------------------------
    
    /**
     * Return a Flash instance from the application map
     * 
     * @param context
     * @return
     */
    public static Flash getCurrentInstance(ExternalContext context)
    {
        return getCurrentInstance(context, true);
    }
    
    public static Flash getCurrentInstance(ExternalContext context, boolean create)
    {
        Map<String, Object> applicationMap = context.getApplicationMap();
        
        Flash flash = (Flash) applicationMap.get(FLASH_INSTANCE);
        if (flash == null && create)
        {
            // synchronize the ApplicationMap to ensure that only
            // once instance of FlashImpl is created and stored in it.
            synchronized (applicationMap)
            {
                // check again, because first try was un-synchronized
                flash = (Flash) applicationMap.get(FLASH_INSTANCE);
                if (flash == null)
                {
                    flash = new FlashImpl(context);
                    applicationMap.put(FLASH_INSTANCE, flash);
                }
            }
        }

        return flash;
    }
    
    // ~ private fields and constructor ---------------------------------------

    private boolean _flashScopeDisabled;
    
    public FlashImpl(ExternalContext externalContext)
    {
        // Read whether flash scope is disabled.
        _flashScopeDisabled = MyfacesConfig.getCurrentInstance(externalContext).isFlashScopeDisabled();
    }
    
    // ~ methods from jakarta.faces.context.Flash -------------------------------

    /**
     * Used to restore the redirect value and the FacesMessages of the previous 
     * request and to manage the flashMap tokens for this request before phase
     * restore view starts.
     */
    @Override
    public void doPrePhaseActions(FacesContext facesContext)
    {
        if (!_flashScopeDisabled)
        {
            final PhaseId currentPhaseId = facesContext.getCurrentPhaseId();
        
            if (PhaseId.RESTORE_VIEW.equals(currentPhaseId))
            {
                // restore the redirect value
                // note that the result of this method is used in many places, 
                // thus it has to be the first thing to do
                _restoreRedirectValue(facesContext);
            
                // restore the FlashMap token from the previous request
                // and create a new token for this request
                _manageFlashMapTokens(facesContext);
            
                // try to restore any saved messages
                _restoreMessages(facesContext);
            }
        }
    }
    
    /**
     * Used to destroy the executeMap and to save all FacesMessages for the
     * next request, but only if this is the last invocation of this method
     * in the current lifecycle (if redirect phase 5, otherwise phase 6).
     */
    @Override
    public void doPostPhaseActions(FacesContext facesContext)
    {
        if (!_flashScopeDisabled)
        {
            // do the actions only if this is the last time
            // doPostPhaseActions() is called on this request
            if (_isLastPhaseInRequest(facesContext))
            {
                if (_isRedirectTrueOnThisRequest(facesContext))
                {
                    // copy entries from executeMap to renderMap, if they do not exist
                    Map<String, Object> renderMap = _getRenderFlashMap(facesContext);

                    for (Map.Entry<String, Object> entry 
                        : _getExecuteFlashMap(facesContext).entrySet())
                    {
                        if (!renderMap.containsKey(entry.getKey()))
                        {
                            renderMap.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            
                // remove execute Map entries from session (--> "destroy" executeMap)
                _clearExecuteFlashMap(facesContext);
            
                // save the current FacesMessages in the renderMap, if wanted
                // Note that this also works on a redirect even though the redirect
                // was already performed and the response has already been committed,
                // because the renderMap is stored in the session.
                _saveMessages(facesContext);
                
                _clearRenderFlashTokenIfMapEmpty(facesContext);
            }
        }
    }
    
    /**
     * Return the value of this property for the flash for this session.
     * 
     * This must be false unless:
     *   - setRedirect(boolean) was called for the current lifecycle traversal
     *     with true as the argument.
     *   - The current lifecycle traversal for this session is in the "execute"
     *     phase and the previous traversal had setRedirect(boolean) called with
     *     true as the argument.
     */
    @Override
    public boolean isRedirect()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        boolean thisRedirect = _isRedirectTrueOnThisRequest(facesContext);
        boolean prevRedirect = _isRedirectTrueOnPreviousRequest(facesContext);
        boolean executePhase = !PhaseId.RENDER_RESPONSE.equals(facesContext.getCurrentPhaseId());
        
        return thisRedirect || (executePhase && prevRedirect);
    }
    
    @Override
    public void setRedirect(boolean redirect)
    {
        if (_flashScopeDisabled)
        {
            return;
        }

        // FIXME this method has a design flaw, because the only valid value
        // is true and it should only be called by the NavigationHandler
        // in a redirect case RIGHT BEFORE ExternalContext.redirect().
        // Maybe a PreRedirectEvent issued by the ExternalContext would be a good
        // choice for Faces 2.1.
        
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> requestMap = externalContext.getRequestMap();
        
        // save the value on the requestMap for this request
        Boolean alreadySet = (Boolean) requestMap.get(FLASH_REDIRECT);
        alreadySet = (alreadySet == null ? Boolean.FALSE : Boolean.TRUE);
        
        // if true and not already set, store it for the following request
        if (!alreadySet && redirect)
        {
            requestMap.put(FLASH_REDIRECT, Boolean.TRUE);
            
            // save redirect=true for the next request
            _saveRedirectValue(facesContext);
        }
        else
        {
            if (alreadySet)
            {
                log.warning("Multiple call to setRedirect() ignored.");
            }
            else // redirect = false
            {
                log.warning("Ignored call to setRedirect(false), because this "
                        + "should only be set to true by the NavigationHandler. "
                        + "No one else should change it.");
            }
        }
    }
    
    /**
     * Take a value from the requestMap, or if it does not exist from the
     * execute FlashMap, and put it on the render FlashMap, so it is visible on
     * the next request.
     */
    @Override
    public void keep(String key)
    {
        _checkFlashScopeDisabled();
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        Object value = requestMap.get(key);
        
        // if the key does not exist in the requestMap,
        // try to get it from the execute FlashMap
        if (value == null)
        {
            Map<String, Object> executeMap = _getExecuteFlashMap(facesContext);
            // Null-check, because in the GET request of a POST-REDIRECT-GET 
            // pattern there is no execute map
            if (executeMap != null)
            {
                value = executeMap.get(key);
                // Store it on request map so we can get it later. For example, 
                // this is used by org.apache.myfaces.el.FlashELResolver to return
                // the value that has been promoted.
                requestMap.put(key, value);
            }
        }
        
        // put it in the render FlashMap
        _getRenderFlashMap(facesContext).put(key, value);
        
        facesContext.getApplication().publishEvent(facesContext,
                PostKeepFlashValueEvent.class, key);
    }

    /**
     * This is just an alias for the request scope map.
     */
    @Override
    public void putNow(String key, Object value)
    {
        _checkFlashScopeDisabled();
        FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().put(key, value);
    }

    /**
     * Returns the value of a previous call to setKeepMessages() from this
     * request. If there was no call yet, false is returned.
     */
    @Override
    public boolean isKeepMessages()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Boolean keepMessages = null;
        if (facesContext != null)
        {
            ExternalContext externalContext = facesContext.getExternalContext();
            Map<String, Object> requestMap = externalContext.getRequestMap();
            keepMessages = (Boolean) requestMap.get(FLASH_KEEP_MESSAGES);
        }
        
        return (keepMessages == null ? Boolean.FALSE : keepMessages);
    }
    
    /**
     * If this property is true, the messages should be kept for the next 
     * request, no matter if it is a normal postback case or a POST-
     * REDIRECT-GET case.
     * 
     * Note that we don't have to store this value for the next request
     * (like setRedirect()), because we will know if it was true on the 
     * next request, if we can find any stored messages in the FlashMap.
     * (also see _saveMessages() and _restoreMessages()).
     */
    @Override
    public void setKeepMessages(boolean keepMessages)
    {
        if (_flashScopeDisabled)
        {
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> requestMap = externalContext.getRequestMap();
        requestMap.put(FLASH_KEEP_MESSAGES, keepMessages);
    }
    
    // ~ Methods from Map interface -------------------------------------------
    
    // NOTE that all these methods do not necessarily delegate to the same Map,
    // because we differentiate between reading and writing operations.
    
    @Override
    public void clear()
    {
        _checkFlashScopeDisabled();
        _getFlashMapForWriting().clear();
    }

    @Override
    public boolean containsKey(Object key)
    {
        _checkFlashScopeDisabled();
        return _getFlashMapForReading().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        _checkFlashScopeDisabled();
        return _getFlashMapForReading().containsValue(value);
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet()
    {
        _checkFlashScopeDisabled();
        return _getFlashMapForReading().entrySet();
    }

    @Override
    public Object get(Object key)
    {
        _checkFlashScopeDisabled();
        if (key == null)
        {
            return null;
        }
        
        if ("keepMessages".equals(key))
        {
            return isKeepMessages();
        }
        else if ("redirect".equals(key))
        {
            return isRedirect();
        }
        
        return _getFlashMapForReading().get(key);
    }
    
    @Override
    public boolean isEmpty()
    {
        _checkFlashScopeDisabled();
        return _getFlashMapForReading().isEmpty();
    }

    @Override
    public Set<String> keySet()
    {
        _checkFlashScopeDisabled();
        return _getFlashMapForReading().keySet();
    }

    @Override
    public Object put(String key, Object value)
    {
        _checkFlashScopeDisabled();
        if (key == null)
        {
            return null;
        }
        
        if ("keepMessages".equals(key))
        {
            Boolean booleanValue = _convertToBoolean(value);
            this.setKeepMessages(booleanValue);
            return booleanValue;
        }
        else if ("redirect".equals(key))
        {
            Boolean booleanValue = _convertToBoolean(value);
            this.setRedirect(booleanValue);
            return booleanValue;
        }
        else
        {
            Object resp = _getFlashMapForWriting().put(key, value); 
            
            FacesContext facesContext = FacesContext.getCurrentInstance();
            facesContext.getApplication().publishEvent(facesContext,
                PostPutFlashValueEvent.class, key);

            return resp;
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m)
    {
        _checkFlashScopeDisabled();
        _getFlashMapForWriting().putAll(m);
    }

    @Override
    public Object remove(Object key)
    {
        _checkFlashScopeDisabled();
        
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.getApplication().publishEvent(facesContext,
            PreRemoveFlashValueEvent.class, key);
        
        return _getFlashMapForWriting().remove(key);
    }

    @Override
    public int size()
    {
        _checkFlashScopeDisabled();
        return _getFlashMapForReading().size();
    }

    @Override
    public Collection<Object> values()
    {
        _checkFlashScopeDisabled();
        return _getFlashMapForReading().values();
    }
    
    // ~ Implementation methods ----------------------------------------------- 
    
    /**
     * Returns true if the current phase is the last phase in the request
     * and thus if doPostPhaseActions() is called for the last time.
     * 
     * This will be true if either we are in phase 6 (render response)
     * or if setRedirect(true) was called on this request and we are
     * in phase 5 (invoke application).
     */
    private boolean _isLastPhaseInRequest(FacesContext facesContext)
    {
        final PhaseId currentPhaseId = facesContext.getCurrentPhaseId();
        
        boolean lastPhaseNormalRequest = PhaseId.RENDER_RESPONSE.equals(currentPhaseId);
        // According to the spec, if there is a redirect, responseComplete() 
        // has been called, and Flash.setRedirect() has been called too,
        // so we just need to check both are present.
        boolean lastPhaseIfRedirect = facesContext.getResponseComplete()
                && _isRedirectTrueOnThisRequest(facesContext);
        
        return lastPhaseNormalRequest || lastPhaseIfRedirect;
    }
    
    /**
     * Return true if setRedirect(true) was called on this request.
     * @param facesContext
     * @return
     */
    private boolean _isRedirectTrueOnThisRequest(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> requestMap = externalContext.getRequestMap();
        Boolean redirect = (Boolean) requestMap.get(FLASH_REDIRECT);
        
        return Boolean.TRUE.equals(redirect);
    }
    
    /**
     * Return true if setRedirect(true) was called on the previous request.
     * Precondition: doPrePhaseActions() must have been called on restore view phase.
     * @param facesContext
     * @return
     */
    private boolean _isRedirectTrueOnPreviousRequest(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> requestMap = externalContext.getRequestMap();
        Boolean redirect = (Boolean) requestMap.get(FLASH_PREVIOUS_REQUEST_REDIRECT);
        
        return Boolean.TRUE.equals(redirect);
    }
    
    /**
     * Saves the value of setRedirect() for the next request, if it was true
     */
    private void _saveRedirectValue(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        
        // This request contains a redirect. This condition is in general 
        // triggered by a NavigationHandler. After a redirect all request scope
        // values get lost, so in order to preserve this value we need to
        // pass it between request. One strategy is use a cookie that is never sent
        // to the client. Other alternative is use the session map.
        // See _restoreRedirectValue() for restoring this value.
        HttpServletResponse httpResponse = ExternalContextUtils
                .getHttpServletResponse(externalContext);
        if (httpResponse != null)
        {
            Cookie cookie = _createFlashCookie(FLASH_REDIRECT, "true", externalContext);
            httpResponse.addCookie(cookie);
        }
        else
        {
            externalContext.getSessionMap().put(FLASH_REDIRECT, true);
        }
    }

    /**
     * Restores the redirect value of the previous request and saves
     * it in the RequestMap under the key FLASH_PREVIOUS_REQUEST_REDIRECT.
     * Must not be called more than once per request.
     * After this method was invoked, the requestMap will contain Boolean.TRUE
     * if setRedirect(true) was called on the previous request or Boolean.FALSE
     * or null otherwise.
     */
    private void _restoreRedirectValue(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletResponse httpResponse = ExternalContextUtils
                .getHttpServletResponse(externalContext);
        if (httpResponse != null)
        {
            // Request values are lost after a redirect. We can create a 
            // temporal cookie to pass the params between redirect calls.
            // It is better than use HttpSession object, because this cookie
            // is never sent by the server.
            Cookie cookie = (Cookie) externalContext
                    .getRequestCookieMap().get(FLASH_REDIRECT);
            if (cookie != null)
            {
                // the cookie exists means there was a redirect, regardless of the value
                externalContext.getRequestMap().put(
                        FLASH_PREVIOUS_REQUEST_REDIRECT, Boolean.TRUE);
                
                // A redirect happened, so it is safe to remove the cookie, setting
                // the maxAge to 0 seconds. The effect is we passed FLASH_REDIRECT param 
                // to this request object
                cookie.setMaxAge(0);
                cookie.setPath(_getCookiePath(externalContext));
                //MYFACES-3354 jetty 6.1.5 does not allow this,
                //call setMaxAge(0) is enough
                //cookie.setValue(null);
                httpResponse.addCookie(cookie);
            }
        }
        else
        {
            // Note that on portlet world we can't create cookies, so we are forced to use the session map. Anyway, 
            // according to the Bridge implementation
            // (for example see org.apache.myfaces.portlet.faces.bridge.BridgeImpl)
            // session object is created at start faces request
            Map<String, Object> sessionMap = externalContext.getSessionMap();
            
            // remove the value from the sessionMap
            Boolean redirect = (Boolean) sessionMap.remove(FLASH_REDIRECT);
            
            // put the value into the requestMap
            externalContext.getRequestMap().put(
                    FLASH_PREVIOUS_REQUEST_REDIRECT, redirect);
        }
    }
    
    /**
     * Saves the current FacesMessages as a List on the render FlashMap for the
     * next request if isKeepMessages() is true. Otherwise it removes any
     * existing FacesMessages-List from the renderFlashMap. 
     * @param facesContext
     */
    private void _saveMessages(FacesContext facesContext)
    {
        if (isKeepMessages()) 
        {
            // get all messages from the FacesContext and store
            // them on the renderMap
            List<MessageEntry> messageList = null;
                
            Iterator<String> iterClientIds = facesContext.getClientIdsWithMessages();
            while (iterClientIds.hasNext())
            {
                String clientId = (String) iterClientIds.next();
                Iterator<FacesMessage> iterMessages = facesContext.getMessages(clientId);
                
                while (iterMessages.hasNext())
                {
                    FacesMessage message = iterMessages.next();
    
                    if (messageList == null)
                    {
                        messageList = new ArrayList<>();
                    }
                    messageList.add(new MessageEntry(clientId, message));
                }
            }
    
            _getRenderFlashMap(facesContext).put(FLASH_KEEP_MESSAGES_LIST, messageList);
        }
        else
        {
            // do not keep messages --> remove messagesList from renderMap
            _getRenderFlashMap(facesContext).remove(FLASH_KEEP_MESSAGES_LIST);
        }
    }

    /**
     * Restore any saved FacesMessages from the previous request.
     * Note that we don't need to save the keepMessages value for this request,
     * because we just have to check if the value for FLASH_KEEP_MESSAGES_LIST exists.
     * @param facesContext
     */
    private void _restoreMessages(FacesContext facesContext)
    {
        List<MessageEntry> messageList = (List<MessageEntry>) 
                _getExecuteFlashMap(facesContext).get(FLASH_KEEP_MESSAGES_LIST);

        if (messageList != null)
        {
            Iterator<MessageEntry> iterMessages = messageList.iterator();

            while (iterMessages.hasNext())
            {
                MessageEntry entry = iterMessages.next();
                facesContext.addMessage(entry.clientId, entry.message);
            }

            // we can now remove the messagesList from the flashMap
            _getExecuteFlashMap(facesContext).remove(FLASH_KEEP_MESSAGES_LIST);
        }
    }
    
    /**
     * Take the render map key and store it as a key for the next request.
     * 
     * On the next request we can get it with _getRenderFlashMapTokenFromPreviousRequest().
     * @param facesContext
     */
    private void _saveRenderFlashMapTokenForNextRequest(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        String tokenValue = (String) externalContext.getRequestMap().get(FLASH_RENDER_MAP_TOKEN);
        ClientWindow clientWindow = externalContext.getClientWindow();
        if (clientWindow != null)
        {
            if (facesContext.getApplication().getStateManager().isSavingStateInClient(facesContext))
            {
                Map<String, Object> sessionMap = externalContext.getSessionMap();
                sessionMap.put(FLASH_RENDER_MAP_TOKEN+SEPARATOR_CHAR+clientWindow.getId(), tokenValue);
            }
            else
            {
                FlashClientWindowTokenCollection lruMap = getFlashClientWindowTokenCollection(externalContext, true);
                if (lruMap != null)
                {
                    lruMap.put(clientWindow.getId(), tokenValue);
                }
            }
        }
        else
        {
            HttpServletResponse httpResponse = ExternalContextUtils.getHttpServletResponse(externalContext);
            if (httpResponse != null)
            {
                Cookie cookie = _createFlashCookie(FLASH_RENDER_MAP_TOKEN, tokenValue, externalContext);
                httpResponse.addCookie(cookie);
            }
            else
            {
                Map<String, Object> sessionMap = externalContext.getSessionMap();
                sessionMap.put(FLASH_RENDER_MAP_TOKEN, tokenValue);
            }
        }
    }
    
    /**
     * Retrieve the map token of the render map from the previous request.
     * 
     * Returns the value of _saveRenderFlashMapTokenForNextRequest() from
     * the previous request.
     * @param facesContext
     * @return
     */
    private String _getRenderFlashMapTokenFromPreviousRequest(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        String tokenValue = null;
        ClientWindow clientWindow = externalContext.getClientWindow();
        if (clientWindow != null)
        {
            if (facesContext.getApplication().getStateManager().isSavingStateInClient(facesContext))
            {
                Map<String, Object> sessionMap = externalContext.getSessionMap();
                tokenValue = (String) sessionMap.get(FLASH_RENDER_MAP_TOKEN+
                        SEPARATOR_CHAR+clientWindow.getId());                
            }
            else
            {
                FlashClientWindowTokenCollection lruMap = getFlashClientWindowTokenCollection(externalContext, false);
                if (lruMap != null)
                {
                    tokenValue = (String) lruMap.get(clientWindow.getId());
                }
            }
        }
        else
        {
            HttpServletResponse httpResponse = ExternalContextUtils.getHttpServletResponse(externalContext);
            if (httpResponse != null)
            {
                //Use a cookie
                Cookie cookie = (Cookie) externalContext.getRequestCookieMap().get(FLASH_RENDER_MAP_TOKEN);
                if (cookie != null)
                {
                    tokenValue = cookie.getValue();
                }
            }
            else
            {
                Map<String, Object> sessionMap = externalContext.getSessionMap();
                tokenValue = (String) sessionMap.get(FLASH_RENDER_MAP_TOKEN);
            }
        }
        return tokenValue;
    }
    
    /**
     * Restores the render FlashMap token from the previous request.
     * This is the token of the executeMap for this request.
     * Furthermore it also creates a new token for this request's renderMap
     * (and thus implicitly a new renderMap).
     * @param facesContext
     */
    private void _manageFlashMapTokens(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> requestMap = externalContext.getRequestMap();

        TokenGenerator tokenGenerator = new TokenGenerator();
        
        final String previousRenderToken 
                = _getRenderFlashMapTokenFromPreviousRequest(facesContext);
        if (previousRenderToken != null)
        {
            // "restore" the renderMap from the previous request
            // and put it as the executeMap for this request
            requestMap.put(FLASH_EXECUTE_MAP_TOKEN, previousRenderToken);
        }
        else
        {
            if (facesContext.isPostback())
            {
                if (facesContext.getExternalContext().getClientWindow() == null)
                {
                    // on a postback, we should always have a previousToken
                    log.warning("Identifier for execute FlashMap was lost on " +
                            "the postback, thus FlashScope information is gone.");
                }
                else
                {
                    // Next token was not preserved in session, which means flash map
                    // is empty. Create a new token and store it as execute map, which
                    // will be empty.
                    requestMap.put(FLASH_EXECUTE_MAP_TOKEN, tokenGenerator.getNextToken());
                }
            }
            
            // create a new token (and thus a new Map) for this request's 
            // executeMap so that we have an executeMap in any possible case.
            requestMap.put(FLASH_EXECUTE_MAP_TOKEN, tokenGenerator.getNextToken());
        }
        
        // create a new token (and thus a new Map) for this request's renderMap
        requestMap.put(FLASH_RENDER_MAP_TOKEN, tokenGenerator.getNextToken());
        
        // we now have the final render token for this request, thus we can
        // already save it for the next request, because it won't change
        _saveRenderFlashMapTokenForNextRequest(facesContext);
    }

    /**
     * Create a new subkey-wrapper of the session map with the given prefix.
     * This wrapper is used to implement the maps for the flash scope.
     * For more information see the SubKeyMap doc.
     */
    private Map<String, Object> _createSubKeyMap(FacesContext context, String prefix)
    {
        ExternalContext external = context.getExternalContext();
        Map<String, Object> sessionMap = external.getSessionMap();

        return new SubKeyMap<>(sessionMap, prefix);
    }

    /**
     * Return the flash map created on this traversal.
     * 
     * This FlashMap will be the execute FlashMap of the next traversal.
     * 
     * Note that it is supposed that FLASH_RENDER_MAP_TOKEN is initialized
     * before restore view phase (see doPrePhaseActions() for details).
     * 
     * @param context
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> _getRenderFlashMap(FacesContext context)
    {
        // Note that we don't have to synchronize here, because it is no problem
        // if we create more SubKeyMaps with the same subkey, because they are
        // totally equal and point to the same entries in the SessionMap.
        
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        Map<String, Object> map = (Map<String, Object>) requestMap.get(FLASH_RENDER_MAP);
        if (map == null)
        {
            String token = (String) requestMap.get(FLASH_RENDER_MAP_TOKEN);
            String fullToken = FLASH_SESSION_MAP_SUBKEY_PREFIX + SEPARATOR_CHAR + token + SEPARATOR_CHAR;
            map =  _createSubKeyMap(context, fullToken);
            requestMap.put(FLASH_RENDER_MAP, map);
        }
        return map;
    }
    
    /**
     * Return the execute Flash Map.
     * 
     * This FlashMap was the render FlashMap of the previous traversal. 
     * 
     * @param context
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> _getExecuteFlashMap(FacesContext context)
    {
        // Note that we don't have to synchronize here, because it is no problem
        // if we create more SubKeyMaps with the same subkey, because they are
        // totally equal and point to the same entries in the SessionMap.
        
        Map<String, Object> requestMap = context != null && context.getExternalContext() != null ?
                context.getExternalContext().getRequestMap() : null;
        Map<String, Object> map = requestMap != null ? (Map<String, Object>) requestMap.get(FLASH_EXECUTE_MAP) : null;
        if (map == null)
        {
            if (requestMap != null)
            {
                String token = (String) requestMap.get(FLASH_EXECUTE_MAP_TOKEN);
                String fullToken = FLASH_SESSION_MAP_SUBKEY_PREFIX + SEPARATOR_CHAR + token + SEPARATOR_CHAR;
                map = _createSubKeyMap(context, fullToken);
                requestMap.put(FLASH_EXECUTE_MAP, map);
            }
            else
            {
                map = Collections.emptyMap();
            }
        }
        return map;
    }
    
    /**
     * Get the proper map according to the current phase:
     * 
     * Normal case:
     * 
     * - First request, restore view phase (create a new one): render map n
     * - First request, execute phase: Skipped
     * - First request, render  phase: render map n
     * 
     *   Render map n saved and put as execute map n
     * 
     * - Second request, execute phase: execute map n
     * - Second request, render  phase: render map n+1
     * 
     * Post Redirect Get case: Redirect is triggered by a call to setRedirect(true) from NavigationHandler
     * or earlier using c:set tag.
     * 
     * - First request, restore view phase (create a new one): render map n
     * - First request, execute phase: Skipped
     * - First request, render  phase: render map n
     * 
     *   Render map n saved and put as execute map n
     * 
     * POST
     * 
     * - Second request, execute phase: execute map n
     *   Note that render map n+1 is also created here to perform keep().
     * 
     * REDIRECT
     * 
     * - NavigationHandler do the redirect, requestMap data lost, called Flash.setRedirect(true)
     * 
     *   Render map n+1 saved and put as render map n+1 on GET request.
     * 
     * GET
     * 
     * - Third  request, restore view phase (create a new one): render map n+1 (restorred)
     *   (isRedirect() should return true as javadoc says)
     * - Third  request, execute phase: skipped
     * - Third  request, render  phase: render map n+1
     * 
     * In this way proper behavior is preserved even in the case of redirect, since the GET part is handled as
     * the "render" part of the current traversal, keeping the semantic of flash object.
     * 
     * @return
     */
    private Map<String, Object> _getActiveFlashMap()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (PhaseId.RENDER_RESPONSE.equals(facesContext.getCurrentPhaseId()) 
                || !facesContext.isPostback())
        {
            return _getRenderFlashMap(facesContext);
        }
        else
        {
            return _getExecuteFlashMap(facesContext);
        }
    }
    
    /**
     * Returns the FlashMap used in the reading methods of java.util.Map
     * like e.g. get() or values().
     * @return
     */
    private Map<String, Object> _getFlashMapForReading()
    {
        return _getExecuteFlashMap(FacesContext.getCurrentInstance());
    }
    
    /**
     * Returns the FlashMap used in the writing methods of java.util.Map
     * like e.g. put() or clear().
     * @return
     */
    private Map<String, Object> _getFlashMapForWriting()
    {
        return _getActiveFlashMap();
    }

    /**
     * Destroy the execute FlashMap, because it is not needed anymore.
     * @param facesContext
     */
    private void _clearExecuteFlashMap(FacesContext facesContext)
    {
        Map<String, Object> map = _getExecuteFlashMap(facesContext);

        if (!map.isEmpty())
        {
            //Faces 2.2 invoke PreClearFlashEvent
            facesContext.getApplication().publishEvent(facesContext, 
                PreClearFlashEvent.class, map);

            // Clear everything - note that because of naming conventions,
            // this will in fact automatically recurse through all children
            // grandchildren etc. - which is kind of a design flaw of SubKeyMap,
            // but one we're relying on

            // NOTE that we do not need a null check here, because there will
            // always be an execute Map, however sometimes an empty one!
            map.clear();
        }
    }
    
    private void _clearRenderFlashTokenIfMapEmpty(FacesContext facesContext)
    {
        // Keep in mind that we cannot remove a cookie once the response has been sent,
        // but we can remove an attribute from session anytime, so the idea is check
        // if the map is empty or not and if that so, do not save the token. The effect
        // is we can reduce the session size a bit.
        ExternalContext externalContext = facesContext.getExternalContext();
        Object session = facesContext.getExternalContext().getSession(false);
        ClientWindow clientWindow = externalContext.getClientWindow();
        if (session != null && clientWindow != null)
        {
            Map<String, Object> map = _getRenderFlashMap(facesContext);
            if (map.isEmpty())
            {
                if (facesContext.getApplication().getStateManager().isSavingStateInClient(facesContext))
                {
                    Map<String, Object> sessionMap = externalContext.getSessionMap();
                    sessionMap.remove(FLASH_RENDER_MAP_TOKEN+SEPARATOR_CHAR+clientWindow.getId());                
                }
                else
                {
                    // Remove token, because it is not necessary
                    FlashClientWindowTokenCollection lruMap = getFlashClientWindowTokenCollection(
                            externalContext, false);
                    if (lruMap != null)
                    {
                        lruMap.remove(clientWindow.getId());
                        Map<String, Object> sessionMap = externalContext.getSessionMap();
                        if (lruMap.isEmpty())
                        {
                            sessionMap.remove(FLASH_CW_LRU_MAP);
                        }
                        else
                        {
                            //refresh remove
                            sessionMap.put(FLASH_CW_LRU_MAP, lruMap);
                        }
                    }
                }
            }
        }
    }
    
    protected FlashClientWindowTokenCollection getFlashClientWindowTokenCollection(
            ExternalContext externalContext, boolean create)
    {
        Object session = externalContext.getSession(false);
        if (session == null && !create)
        {
            return null;
        }
        if (session == null && externalContext.getResponse() instanceof HttpServletResponse)
        {
            HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
            if (response.isCommitted())
            {
                return null;
            }
        }

        Map<String, Object> sessionMap = externalContext.getSessionMap();
        FlashClientWindowTokenCollection lruMap = (FlashClientWindowTokenCollection)
                sessionMap.get(FLASH_CW_LRU_MAP);
        if (lruMap == null)
        {
            Integer numberOfFlashTokensInSession =
                    MyfacesConfig.getCurrentInstance(externalContext).getNumberOfFlashTokensInSession();
            lruMap = new FlashClientWindowTokenCollection(numberOfFlashTokensInSession);
        }    

        if (create)
        {
            sessionMap.put(FLASH_CW_LRU_MAP, lruMap);
        }
        return lruMap;
    }

    @Override
    public void clearFlashMap(FacesContext facesContext, String clientWindowId, String token)
    {
        if ((!_flashScopeDisabled) && 
                (!facesContext.getApplication().getStateManager().isSavingStateInClient(facesContext)))
        {
            ExternalContext externalContext = facesContext.getExternalContext();
            ClientWindow clientWindow = externalContext.getClientWindow();
            if (clientWindow != null)
            {
                if (token != null)
                {
                    String fullToken = FLASH_SESSION_MAP_SUBKEY_PREFIX + SEPARATOR_CHAR + token + SEPARATOR_CHAR;
                    Map<String, Object> map =  _createSubKeyMap(facesContext, fullToken);
                    map.clear();
                }
            }
        }
    }

    /**
     * Creates a Cookie with the given name and value.
     * In addition, it will be configured with maxAge=-1, the current request path and secure value.
     *
     * @param name
     * @param value
     * @param externalContext
     * @return
     */
    private Cookie _createFlashCookie(String name, String value, ExternalContext externalContext)
    {
        Cookie cookie = new Cookie(name, value);

        cookie.setMaxAge(-1);
        cookie.setPath(_getCookiePath(externalContext));
        cookie.setSecure(externalContext.isSecure());
        cookie.setHttpOnly(true);
        Object context = externalContext.getContext();

        if (context instanceof ServletContext && ExternalSpecifications.isServlet6Available())
        {
            ServletContext servletContext = (ServletContext)context;
            String sameSite = servletContext.getSessionCookieConfig().getAttribute("SameSite");
            cookie.setAttribute("SameSite", Objects.toString(sameSite, "Strict"));
        }

        return cookie;
    }

    /**
     * Returns the path for the Flash-Cookies.
     * @param externalContext
     * @return
     */
    private String _getCookiePath(ExternalContext externalContext)
    {
        String contextPath = externalContext.getRequestContextPath();
        if (StringUtils.isEmpty(contextPath))
        {
            contextPath = "/";
        }

        return contextPath;
    }
    
    /**
     * Convert the Object to a Boolean.
     * @param value
     * @return
     */
    private Boolean _convertToBoolean(Object value)
    {
        Boolean booleanValue;
        if (value instanceof Boolean)
        {
            booleanValue = (Boolean) value;
        }
        else
        {
            booleanValue = Boolean.parseBoolean(value.toString());
        }
        return booleanValue;
    }
    
    /**
     * Checks whether flash scope is disabled.
     * @throws FlashScopeDisabledException if flash scope is disabled
     */
    private void _checkFlashScopeDisabled()
    {
        if (_flashScopeDisabled)
        {
            throw new FlashScopeDisabledException("Flash scope was disabled by context param " 
                + MyfacesConfig.FLASH_SCOPE_DISABLED + " but erroneously accessed");
        }
    }
    
    // ~ Inner classes --------------------------------------------------------
    
    /**
     * Class used to store a FacesMessage with its clientId.
     */
    private static class MessageEntry implements Serializable
    {
        private static final long serialVersionUID = -690264660230199234L;
        private final String clientId;
        private final FacesMessage message;

        public MessageEntry(String clientId, FacesMessage message)
        {
            this.clientId = clientId;
            this.message = message;
        }
    }
    
}
