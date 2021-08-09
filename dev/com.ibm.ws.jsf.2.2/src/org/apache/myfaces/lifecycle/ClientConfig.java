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


import javax.faces.context.FacesContext;
import java.io.Serializable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Contains information about whether the user has
 * JavaScript enabled on his client, etc.
 * It also contains the windowhandler html which gets sent to
 * the browser to detect the current windowId.
 *
 * This allows the 'customisation' of this html file to e.g.
 * adopt the background colour to avoid screen flickering.
 *
 * Please note that all subclasses of ClientConfig should also
 * be &#064;SessionScoped as well!
 */
public class ClientConfig implements Serializable
{
    private static final long serialVersionUID = 581351549574404793L;

    /** We will set a cookie with this very name if a noscript link got clicked by the user */
    public static final String COOKIE_NAME_NOSCRIPT_ENABLED = "mfNoScriptEnabled";

    private volatile Boolean javaScriptEnabled = null;

    protected String windowHandlerHtml;

    /** lazily initiated via {@link #getUserAgent(javax.faces.context.FacesContext)} */
    private volatile String userAgent = null;

    /**
     * The location of the default windowhandler resource
     */
    //private static final String DEFAULT_WINDOW_HANDLER_HTML_FILE = "static/windowhandler.html";

    /**
     * Defaults to <code>true</code>.
     * @return if the user has JavaScript enabled
     */
    public boolean isJavaScriptEnabled()
    {
        if (javaScriptEnabled == null)
        {
            synchronized(this)
            {
                // double lock checking idiom on volatile variable works since java5
                if (javaScriptEnabled == null)
                {
                    // no info means that it is default -> true
                    javaScriptEnabled = Boolean.TRUE;

                    FacesContext facesContext = FacesContext.getCurrentInstance();
                    if (facesContext != null)
                    {
                        Cookie cookie = (Cookie) facesContext.getExternalContext().
                                getRequestCookieMap().get(COOKIE_NAME_NOSCRIPT_ENABLED);
                        if (cookie!= null)
                        {
                            javaScriptEnabled = Boolean.parseBoolean((String) cookie.getValue());
                        }
                    }
                }
            }
        }
        return javaScriptEnabled;
    }

    /**
     * Set it to <code>false</code> if you don't like to use the
     * JavaScript based client side windowhandler. In this case
     * the request will be returned directly.
     * @param javaScriptEnabled
     */
    public void setJavaScriptEnabled(boolean javaScriptEnabled)
    {
        this.javaScriptEnabled = javaScriptEnabled;

        // and now also store this information inside a cookie!
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null)
        {
            Object r = facesContext.getExternalContext().getResponse();
            if (r instanceof HttpServletResponse)
            {
                Cookie cookie = new Cookie(COOKIE_NAME_NOSCRIPT_ENABLED, "" + javaScriptEnabled);
                cookie.setPath("/"); // for all the server
                HttpServletResponse response = (HttpServletResponse) r;
                response.addCookie(cookie);
            }
        }
    }

    /**
     * For branding the whole windowhandler page - e.g. not only change the
     * background color, add some images and empty menu structure
     * or the language of the message text - you can just copy the content of the
     * {@link #DEFAULT_WINDOW_HANDLER_HTML_FILE} and adopt it to your needs.
     *
     * The reason for this is to minimize visual side effects on browsers who do
     * not properly support html5 localstorage.
     *
     * @return the location of the <i>windowhandler.html</i> resource
     *         which should be sent to the users browser.
     */
    /*
    public String getWindowHandlerResourceLocation()
    {
        return DEFAULT_WINDOW_HANDLER_HTML_FILE;
    }*/

    /**
     * This might return different windowhandlers based on user settings like
     * his language, an affiliation, etc
     * @return a String containing the whole windowhandler.html file.
     * @throws IOException
     */
    /*
    public String getWindowHandlerHtml() throws IOException
    {
        if (FacesContext.getCurrentInstance().isProjectStage(ProjectStage.Development) 
                && windowHandlerHtml != null)
        {
            // use cached windowHandlerHtml except in Development
            return windowHandlerHtml;
        }

        InputStream is = ClassUtils.getContextClassLoader().getResourceAsStream(
                getWindowHandlerResourceLocation());
        StringBuffer sb = new StringBuffer();
        try
        {
            byte[] buf = new byte[16 * 1024];
            int bytesRead;
            while ((bytesRead = is.read(buf)) != -1)
            {
                String sbuf = new String(buf, 0, bytesRead);
                sb.append(sbuf);
            }
        }
        finally
        {
            is.close();
        }

        windowHandlerHtml = sb.toString();

        return windowHandlerHtml;
    }*/


    /**
     * This information will get stored as it cannot
     * change during the session anyway.
     * @return the UserAgent of the request.
     */
    public String getUserAgent(FacesContext facesContext)
    {
        if (userAgent == null)
        {
            synchronized (this)
            {
                if (userAgent == null)
                {
                    Map<String, String[]> requestHeaders =
                            facesContext.getExternalContext().getRequestHeaderValuesMap();

                    if (requestHeaders != null &&
                            requestHeaders.containsKey("User-Agent"))
                    {
                        String[] userAgents = requestHeaders.get("User-Agent");
                        userAgent = userAgents.length > 0 ? userAgents[0] : null;
                    }
                }
            }
        }

        return userAgent;
    }

    /**
     * Users can overload this method to define in which scenarios a request should result
     * in an 'intercepted' page with proper windowId detection. This can e.g. contain
     * blocklisting some userAgents.
     * By default the following User-Agents will be served directly:
     * <ul>
     *     <li>.*bot.*</li>
     *     <li>.*Bot.*</li>
     *     <li>.*Slurp.*</li>
     *     <li>.*Crawler.*</li>
     * </ul>
     * @return <code>true</code> if the Request should get 'intercepted' and the intermediate
     *        windowhandler.html page should get rendered first. By returning <code>false</code>
     *        the requested page will get rendered intermediately.
     * @see #getUserAgent(javax.faces.context.FacesContext) for determining the UserAgent
     */
    public boolean isClientSideWindowHandlerRequest(FacesContext facesContext)
    {
        if (!isJavaScriptEnabled())
        {
            return false;
        }

        String userAgent = getUserAgent(facesContext);

        if (userAgent != null &&
            ( userAgent.indexOf("bot")     >= 0 || // Googlebot, etc
              userAgent.indexOf("Bot")     >= 0 || // BingBot, etc
              userAgent.indexOf("Slurp")   >= 0 || // Yahoo Slurp
              userAgent.indexOf("Crawler") >= 0    // various other Crawlers
            ) )
        {
            return false;
        }

        return true;
    }

}
