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
package org.apache.myfaces.lifecycle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.faces.FacesException;
import javax.faces.application.Resource;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.lifecycle.ClientWindow;
import javax.faces.render.ResponseStateManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author lu4242
 */
public class CODIClientSideWindow extends ClientWindow
{
    /**
     * Key for storing the window-id e.g. in URLs
     */
    //private static final String WINDOW_CONTEXT_ID_PARAMETER_KEY = 
    //        ResponseStateManager.CLIENT_WINDOW_URL_PARAM;

    /**
     * Value which can be used as "window-id" by external clients which aren't aware of windows.
     * It deactivates e.g. the redirect for the initial request.
     */
    private static final String AUTOMATED_ENTRY_POINT_PARAMETER_KEY = "automatedEntryPoint";    
    
    private static final long serialVersionUID = 5293942986187078113L;

    private static final String WINDOW_ID_COOKIE_PREFIX = "jfwid-";
    private static final String CODI_REQUEST_TOKEN = "mfRid";

    private static final String UNINITIALIZED_WINDOW_ID_VALUE = "uninitializedWindowId";
    private static final String WINDOW_ID_REPLACE_PATTERN = "$$windowIdValue$$";
    private static final String NOSCRIPT_URL_REPLACE_PATTERN = "$$noscriptUrl$$";
    private static final String NOSCRIPT_PARAMETER = "mfDirect";

    private final ClientConfig clientConfig;

    private final WindowContextConfig windowContextConfig;
    
    private final TokenGenerator clientWindowTokenGenerator;    
    
    private String windowId;
    
    private String unparsedWindowHandlerHtml = null;
    
    private Map<String,String> queryParamsMap;

    /*
    protected CODIClientSideWindow()
    {
        // needed for proxying
    }*/

    protected CODIClientSideWindow(TokenGenerator clientWindowTokenGenerator,
            WindowContextConfig windowContextConfig,
            ClientConfig clientConfig)
    {
        this.windowContextConfig = windowContextConfig;
        this.clientConfig = clientConfig;
        this.clientWindowTokenGenerator = clientWindowTokenGenerator;
    }

    /**
     * {@inheritDoc}
     */
    /*
    public String restoreWindowId(ExternalContext externalContext)
    {
        if (this.clientConfig.isJavaScriptEnabled())
        {
            return (String) externalContext.getRequestMap().get(
                    WINDOW_CONTEXT_ID_PARAMETER_KEY);
        }
        else
        {
            // fallback
            //if(!this.useWindowAwareUrlEncoding)
            //{
            //    return null;
            //}

            return externalContext.getRequestParameterMap().get(WINDOW_CONTEXT_ID_PARAMETER_KEY);
        }
    }*/

    /**
     * {@inheritDoc}
     */
    //public void beforeLifecycleExecute(FacesContext facesContext)
    public void decode(FacesContext facesContext)
    {
        if (facesContext.isPostback())
        {
            // In postback, we can safely ignore the query param, because it is not useful
            if (getId() == null)
            {
                 setId(calculateWindowIdFromPost(facesContext));
            }
        }

        if (!isClientSideWindowHandlerRequest(facesContext))
        {
            return;
        }
        
        ExternalContext externalContext = facesContext.getExternalContext();

        if (isNoscriptRequest(externalContext))
        {
            // the client has JavaScript disabled
            clientConfig.setJavaScriptEnabled(false);
            return;
        }

        String windowId = getWindowIdFromCookie(externalContext);
        if (windowId == null)
        {
            // GET request without windowId - send windowhandlerfilter.html to get the windowId
            sendWindowHandlerHtml(facesContext, null);
            facesContext.responseComplete();
        }
        else
        {
            if (AUTOMATED_ENTRY_POINT_PARAMETER_KEY.equals(windowId) ||
                (!windowContextConfig.isUnknownWindowIdsAllowed() /*&&
                 !ConversationUtils.isWindowActive(this.windowContextManager, windowId)*/))
            {
                // no or invalid windowId --> create new one
                // don't use createWindowId() the following call will ensure the max. window context count,...
                //windowId = this.windowContextManager.getCurrentWindowContext().getId();
                windowId = createWindowId(facesContext);

                // GET request with NEW windowId - send windowhandlerfilter.html to set and re-get the windowId
                sendWindowHandlerHtml(facesContext, windowId);
                facesContext.responseComplete();
            }
            else
            {
                // we have a valid windowId - set it and continue with the request
                // TODO only set internally and provide via restoreWindowId()? 
                //externalContext.getRequestMap().put(WINDOW_CONTEXT_ID_PARAMETER_KEY, windowId);
                setId(windowId);
            }
        }
    }

    public String calculateWindowIdFromPost(FacesContext context)
    {
        //1. If it comes as parameter, it takes precedence over any other choice, because
        //   no browser is capable to do a POST and create a new window at the same time.
        String windowId = context.getExternalContext().getRequestParameterMap().get(
                ResponseStateManager.CLIENT_WINDOW_PARAM);
        if (windowId != null)
        {
            return windowId;
        }
        return null;
    }
    
    private boolean isClientSideWindowHandlerRequest(FacesContext facesContext)
    {
        // no POST request and javascript enabled
        // NOTE that for POST-requests the windowId is saved in the state (see WindowContextIdHolderComponent)
        return !facesContext.isPostback() && clientConfig.isClientSideWindowHandlerRequest(facesContext);
    }

    private boolean isNoscriptRequest(ExternalContext externalContext)
    {
        String noscript = externalContext.getRequestParameterMap().get(NOSCRIPT_PARAMETER);

        return (noscript != null && "true".equals(noscript));
    }

    private void sendWindowHandlerHtml(FacesContext facesContext, String windowId)
    {
        HttpServletResponse httpResponse = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        try
        {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            httpResponse.setContentType("text/html");

            if (unparsedWindowHandlerHtml == null)
            {
                Resource resource = facesContext.getApplication().getResourceHandler().createResource(
                        "windowhandler.html", "org.apache.myfaces.windowId");
                
                unparsedWindowHandlerHtml = convertStreamToString(resource.getInputStream());
            }
            
            String windowHandlerHtml = unparsedWindowHandlerHtml;

            if (windowId == null)
            {
                windowId = UNINITIALIZED_WINDOW_ID_VALUE;
            }

            // set the windowId value in the javascript code
            windowHandlerHtml = windowHandlerHtml.replace(WINDOW_ID_REPLACE_PATTERN, windowId);

            // set the noscript-URL for users with no JavaScript
            windowHandlerHtml = windowHandlerHtml.replace(
                    NOSCRIPT_URL_REPLACE_PATTERN, getNoscriptUrl(facesContext.getExternalContext()));

            OutputStream os = httpResponse.getOutputStream();
            try
            {
                os.write(windowHandlerHtml.getBytes());
            }
            finally
            {
                os.close();
            }
        }
        catch (IOException ioe)
        {
            throw new FacesException(ioe);
        }
    }
    
    private static String convertStreamToString(InputStream is)
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = reader.readLine()) != null)
            {
              sb.append(line + "\n");
            }
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    //No op
                }                
            }
        }
        return sb.toString();
    }    

    private String getNoscriptUrl(ExternalContext externalContext)
    {
        String url = externalContext.getRequestPathInfo();
        if (url == null)
        {
            url = "";
        }

        // only use the very last part of the url
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1)
        {
            url = url.substring(lastSlash + 1);
        }

        // add request parameter
        url = addParameters(externalContext, url, true, true, true);

        // add noscript parameter
        if (url.contains("?"))
        {
            url = url + "&";
        }
        else
        {
            url = url + "?";
        }
        url = url + NOSCRIPT_PARAMETER + "=true";

        // NOTE that the url could contain data for an XSS attack
        // like e.g. ?"></a><a href%3D"http://hacker.org/attack.html?a
        // DO NOT REMOVE THE FOLLOWING LINES!
        url = url.replace("\"", "");
        url = url.replace("\'", "");

        return url;
    }

    /**
     * Adds the current request-parameters to the given url
     * @param externalContext current external-context
     * @param url current url
     * @param addRequestParameter flag which indicates if the request params should be added or not
     * @param addPageParameter flag which indicates if the view params should be added or not {@see ViewParameter}
     * @param encodeValues flag which indicates if parameter values should be encoded or not
     * @return url with request-parameters
     */
    public static String addParameters(ExternalContext externalContext, String url,
                                       boolean addRequestParameter, boolean addPageParameter, boolean encodeValues)
    {
        StringBuilder finalUrl = new StringBuilder(url);
        boolean existingParameters = url.contains("?");
        boolean urlContainsWindowId = url.contains(ResponseStateManager.CLIENT_WINDOW_URL_PARAM + "=");

        /* TODO: implement me
        for(RequestParameter requestParam :
                getParameters(externalContext, true, addRequestParameter, addPageParameter))
        {
            String key = requestParam.getKey();

            //TODO eval if we should also filter the other params
            if(WindowContextManager.WINDOW_CONTEXT_ID_PARAMETER_KEY.equals(key) && urlContainsWindowId)
            {
                continue;
            }

            for(String parameterValue : requestParam.getValues())
            {
                if(!url.contains(key + "=" + parameterValue) &&
                        !url.contains(key + "=" + encodeURLParameterValue(parameterValue, externalContext)))
                {
                    if(!existingParameters)
                    {
                        finalUrl.append("?");
                        existingParameters = true;
                    }
                    else
                    {
                        finalUrl.append("&");
                    }
                    finalUrl.append(key);
                    finalUrl.append("=");

                    if(encodeValues)
                    {
                        finalUrl.append(encodeURLParameterValue(parameterValue, externalContext));
                    }
                    else
                    {
                        finalUrl.append(parameterValue);
                    }
                }
            }
        }
        */
        return finalUrl.toString();
    }
    
    protected String createWindowId(FacesContext context)
    {
        String windowId = clientWindowTokenGenerator._getNextToken();
        setId(windowId);
        return windowId;
    }
    
    private String getWindowIdFromCookie(ExternalContext externalContext)
    {
        String cookieName = WINDOW_ID_COOKIE_PREFIX + getRequestToken(externalContext);
        Cookie cookie = (Cookie) externalContext.getRequestCookieMap().get(cookieName);

        if (cookie != null)
        {
            // manually blast the cookie away, otherwise it pollutes the
            // cookie storage in some browsers. E.g. Firefox doesn't
            // cleanup properly, even if the max-age is reached.
            cookie.setMaxAge(0);

            return cookie.getValue();
        }

        return null;
    }

    private String getRequestToken(ExternalContext externalContext)
    {
        String requestToken = externalContext.getRequestParameterMap().get(CODI_REQUEST_TOKEN);
        if (requestToken != null)
        {
            return requestToken;
        }

        return "";
    }

    @Override
    public String getId()
    {
        return windowId;
    }
    
    public void setId(String id)
    {
        windowId = id;
        queryParamsMap = null;
    }
    
    @Override
    public Map<String, String> getQueryURLParameters(FacesContext context)
    {
        if (queryParamsMap == null)
        {
            String id = context.getExternalContext().getClientWindow().getId();
            if (id != null)
            {
                queryParamsMap = new HashMap<String, String>(2,1);
                queryParamsMap.put(ResponseStateManager.CLIENT_WINDOW_URL_PARAM, id);
            }
        }
        return queryParamsMap;
    }
}
