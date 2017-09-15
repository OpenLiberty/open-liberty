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
package org.apache.myfaces.renderkit;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.el.Expression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIColumn;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialResponseWriter;
import javax.faces.context.ResponseWriter;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.render.Renderer;
import javax.faces.view.Location;
import javax.servlet.http.HttpServletResponse;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.lifecycle.ViewNotFoundException;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.renderkit.html.HtmlResponseWriterImpl;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.shared.util.StateUtils;
import org.apache.myfaces.spi.WebConfigProvider;
import org.apache.myfaces.spi.WebConfigProviderFactory;
import org.apache.myfaces.view.facelets.component.UIRepeat;
import org.apache.myfaces.view.facelets.el.ContextAware;

/**
 * This class provides utility methods to generate the
 * MyFaces error and debug pages. 
 *
 * @author Jacob Hookom (ICLA with ASF filed)
 * @author Jakob Korherr (refactored and moved here from javax.faces.webapp._ErrorPageWriter)
 */
public final class ErrorPageWriter
{

    /**
     * This bean aims to generate the error page html for inclusion on a facelet error page via
     * <ui:include src="javax.faces.error.xhtml" />. When performing this include the facelet
     * "myfaces-dev-error-include.xhtml" will be included. This facelet references to the ErrorPageBean.
     * This also works for custom error page templates.
     * The bean is added to the ViewMap of the UIViewRoot, which is 
     * displaying the error page, in RestoreViewExecutor.execute().
     * @author Jakob Korherr
     */
    public static class ErrorPageBean implements Serializable
    {

        private static final long serialVersionUID = -79513324193326616L;

        public String getErrorPageHtml() throws IOException
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();

            Throwable t = (Throwable) requestMap.get(EXCEPTION_KEY);
            if (t == null)
            {
                throw new IllegalStateException("No Exception to handle");
            }

            UIViewRoot view = (UIViewRoot) requestMap.get(VIEW_KEY);

            StringWriter writer = new StringWriter();
            ErrorPageWriter.debugHtml(writer, facesContext, view, null, t);
            String html = writer.toString();

            // change the HTML in the buffer to be included in an existing html page
            String body;
            try
            {
                body = html.substring(html.indexOf("<body>") + "<body>".length(), html.indexOf("</body>"));
            }
            catch (Exception e)
            {
                // no body found - return the entire html
                return html;
            }

            String head;
            try
            {
                head = html.substring(html.indexOf("<head>") + "<head>".length(), html.indexOf("</head>"));
            }
            catch (Exception e)
            {
                // no head found - return entire body
                return body;
            }

            // extract style and script information from head and add it to body
            StringBuilder builder = new StringBuilder(body);
            // extract <style>
            int startIndex = 0;
            while (true)
            {
                try
                {
                    int endIndex = head.indexOf("</style>", startIndex) + "</style>".length();
                    builder.append(head.substring(head.indexOf("<style", startIndex), endIndex));
                    startIndex = endIndex;
                }
                catch (Exception e)
                {
                    // no style found - break extraction
                    break;
                }
            }
            // extract <script>
            startIndex = 0;
            while (true)
            {
                try
                {
                    int endIndex = head.indexOf("</script>", startIndex) + "</script>".length();
                    builder.append(head.substring(head.indexOf("<script", startIndex), endIndex));
                    startIndex = endIndex;
                }
                catch (Exception e)
                {
                    // no script found - break extraction
                    break;
                }
            }

            return builder.toString();
        }

    }

    /**
     * The key which is used to store the ErrorPageBean in the view map of a facelet error page.
     */
    public static final String ERROR_PAGE_BEAN_KEY = "__myFacesErrorPageBean";

    private static final String EXCEPTION_KEY = "javax.servlet.error.exception";
    public static final String VIEW_KEY = "org.apache.myfaces.error.UIViewRoot";

    private static final Logger log = Logger.getLogger(ErrorPageWriter.class.getName());

    private final static String TS = "&lt;";

    private static final String ERROR_TEMPLATE = "META-INF/rsc/myfaces-dev-error.xml";

    /**
     * Indicate the template name used to render the default error page used by MyFaces specific 
     * error handler implementation. 
     *
     * <p>See org.apache.myfaces.ERROR_HANDLING for details about
     * how to enable/disable it.</p>
     */
    @JSFWebConfigParam(defaultValue="META-INF/rsc/myfaces-dev-error.xml", since="1.2.4")
    private static final String ERROR_TEMPLATE_RESOURCE = "org.apache.myfaces.ERROR_TEMPLATE_RESOURCE";

    private static String[] errorParts;

    private static final String DEBUG_TEMPLATE = "META-INF/rsc/myfaces-dev-debug.xml";

    /**
     * Indicate the template name used to render the default debug page (see ui:debug tag).
     */
    @JSFWebConfigParam(defaultValue="META-INF/rsc/myfaces-dev-debug.xml", since="1.2.4")
    private static final String DEBUG_TEMPLATE_RESOURCE = "org.apache.myfaces.DEBUG_TEMPLATE_RESOURCE";

    private static String[] debugParts;

    private static final String REGEX_PATTERN = ".*?\\Q,Id:\\E\\s*(\\S+)\\s*\\].*?";

    private final static String[] IGNORE = new String[] { "parent", "rendererType" };

    private final static String[] ALWAYS_WRITE = new String[] { "class", "clientId" };

    /**
     * Extended debug info is stored under this key in the request
     * map for every UIInput component when in Development mode.
     * ATTENTION: this constant is duplicate in javax.faces.component.UIInput
     */
    public static final String DEBUG_INFO_KEY = "org.apache.myfaces.debug.DEBUG_INFO";

    /**
     * The number of facets of this component which have already been visited while
     * creating the extended component tree is saved under this key in the component's
     * attribute map.
     */
    private static final String VISITED_FACET_COUNT_KEY = "org.apache.myfaces.debug.VISITED_FACET_COUNT";
    //private static Map<UIComponent, Integer> visitedFacetCount = new HashMap<UIComponent, Integer>();

    /**
     * Indicate if myfaces is responsible to handle errors. 
     * See http://wiki.apache.org/myfaces/Handling_Server_Errors for details.
     */
    @JSFWebConfigParam(defaultValue="false, on Development Project stage: true",
                       expectedValues="true,false", since="1.2.4")
    public static final String ERROR_HANDLING_PARAMETER = "org.apache.myfaces.ERROR_HANDLING";

    public ErrorPageWriter()
    {
        super();
    }

    /**
     * Generates the HTML error page for the given Throwable 
     * and writes it to the given writer.
     * @param writer
     * @param faces
     * @param e
     * @throws IOException
     */
    public static void debugHtml(Writer writer, FacesContext faces, Throwable e) throws IOException
    {
        debugHtml(writer, faces, faces.getViewRoot(), null,  e);
    }

    private static void debugHtml(Writer writer, FacesContext faces, UIViewRoot view,
                                  Collection<UIComponent> components, Throwable... exs) throws IOException
    {
        _init(faces);
        Date now = new Date();

        for (int i = 0; i < errorParts.length; i++)
        {
            if ("view".equals((errorParts[i])))
            {
                if (faces.getViewRoot() != null)
                {
                    String viewId = faces.getViewRoot().getViewId();
                    writer.write("viewId=" + viewId);
                    writer.write("<br/>");
                    String realPath = null;
                    try
                    {
                        //Could not work on tomcat 7 running by cargo
                        realPath = faces.getExternalContext().getRealPath(viewId);
                    }
                    catch(Throwable e)
                    {
                        //swallow it
                    }
                    if (realPath != null)
                    {
                        writer.write("location=" + realPath);
                        writer.write("<br/>");
                    }
                    writer.write("phaseId=" + faces.getCurrentPhaseId());
                    writer.write("<br/>");
                    writer.write("<br/>");
                }
            }
            else if ("message".equals(errorParts[i]))
            {
                boolean printed = false;
                //Iterator<UIComponent> iterator = null;
                //if (components != null)
                //{ 
                //    iterator = components.iterator();
                //}
                for (Throwable e : exs)
                {
                    String msg = e.getMessage();
                    if (printed)
                    {
                        writer.write("<br/>");
                    }
                    if (msg != null)
                    {
                        writer.write(msg.replaceAll("<", TS));
                    }
                    else
                    {
                        writer.write(e.getClass().getName());
                    }
                    printed = true;
                }
            }
            else if ("trace".equals(errorParts[i]))
            {
                boolean printed = false;
                for (Throwable e : exs)
                {
                    if (printed)
                    {
                        writer.write("\n");
                    }
                    _writeException(writer, e);
                    printed = true;
                }
            }
            else if ("now".equals(errorParts[i]))
            {
                writer.write(DateFormat.getDateTimeInstance().format(now));
            }
            else if ("tree".equals(errorParts[i]))
            {
                if (view != null)
                {
                    List<String> errorIds = _getErrorId(components, exs);
                    _writeComponent(faces, writer, view, errorIds, true);
                }
            }
            else if ("vars".equals(errorParts[i]))
            {
                _writeVariables(writer, faces, view);
            }
            else if ("cause".equals(errorParts[i]))
            {
                boolean printed = false;
                Iterator<UIComponent> iterator = null;
                if (components != null)
                {
                    iterator = components.iterator();
                }
                for (Throwable e : exs)
                {
                    if (printed)
                    {
                        writer.write("<br/>");
                    }
                    _writeCause(writer, e);
                    if (iterator != null)
                    {
                        UIComponent uiComponent = iterator.next();
                        if (uiComponent != null)
                        {
                            _writeComponent(faces, writer, uiComponent, null, /* writeChildren */false);
                        }
                    }
                    printed = true;
                }
            }
            else
            {
                writer.write(errorParts[i]);
            }
        }
    }

    /**
     * Generates the HTML debug page for the current view
     * and writes it to the given writer.
     * @param writer
     * @param faces
     * @throws IOException
     */
    public static void debugHtml(Writer writer, FacesContext faces) throws IOException
    {
        _init(faces);
        Date now = new Date();
        for (int i = 0; i < debugParts.length; i++)
        {
            if ("message".equals(debugParts[i]))
            {
                writer.write(faces.getViewRoot().getViewId());
            }
            else if ("now".equals(debugParts[i]))
            {
                writer.write(DateFormat.getDateTimeInstance().format(now));
            }
            else if ("tree".equals(debugParts[i]))
            {
                _writeComponent(faces, writer, faces.getViewRoot(), null, true);
            }
            else if ("extendedtree".equals(debugParts[i]))
            {
                _writeExtendedComponentTree(writer, faces);
            }
            else if ("vars".equals(debugParts[i]))
            {
                _writeVariables(writer, faces, faces.getViewRoot());
            }
            else
            {
                writer.write(debugParts[i]);
            }
        }
    }

    public static void handle(FacesContext facesContext, Collection<UIComponent> components,
                              Throwable... exs) throws FacesException
    {
        for (Throwable ex : exs)
        {
            _prepareExceptionStack(ex);
        }

        if (!facesContext.getExternalContext().isResponseCommitted())
        {
            facesContext.getExternalContext().responseReset();
        }

        int responseStatus = -1;
        for (Throwable ex : exs)
        {
            if (ex instanceof ViewNotFoundException)
            {
                responseStatus = HttpServletResponse.SC_NOT_FOUND;
                break;
            }
            else
            {
                responseStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
        }
        if (responseStatus != -1)
        {
            facesContext.getExternalContext().setResponseStatus(responseStatus);
        }

        // normal request --> html error page
        facesContext.getExternalContext().setResponseContentType("text/html");
        facesContext.getExternalContext().setResponseCharacterEncoding("UTF-8");
        try
        {
            // We need the real one, because the one returned from FacesContext.getResponseWriter()
            // is configured with the encoding of the view.
            Writer writer = facesContext.getExternalContext().getResponseOutputWriter();
            debugHtml(writer, facesContext, facesContext.getViewRoot(), components, exs);
        }
        catch(IOException ioe)
        {
            throw new FacesException("Could not write the error page", ioe);
        }

        // mark the response as complete
        facesContext.responseComplete();
    }

    /**
     * Handles the given Throwbale in the following way:
     * If there is no <error-page> entry in web.xml, try to reset the current HttpServletResponse,
     * generate the error page and call responseComplete(). If this fails, rethrow the Exception.
     * If there is an <error-page> entry in web.xml, save the current UIViewRoot in the RequestMap
     * with the key "org.apache.myfaces.error.UIViewRoot" to access it on the error page and
     * rethrow the Exception to let it flow up to FacesServlet.service() and thus be handled by the container.
     * @param facesContext
     * @param ex
     * @throws FacesException
     * @deprecated Use MyFacesExceptionHandlerWrapperImpl and handle() method
     */
    @Deprecated
    public static void handleThrowable(FacesContext facesContext, Throwable ex) throws FacesException
    {
        _prepareExceptionStack(ex);

        boolean errorPageWritten = false;

        // check if an error page is present in web.xml
        // if so, do not generate an error page
        //WebXml webXml = WebXml.getWebXml(facesContext.getExternalContext());
        //if (webXml.isErrorPagePresent())
        WebConfigProvider webConfigProvider = WebConfigProviderFactory.getWebConfigProviderFactory(
                facesContext.getExternalContext()).getWebConfigProvider(facesContext.getExternalContext());

        if(webConfigProvider.isErrorPagePresent(facesContext.getExternalContext()))
        {
            // save current view in the request map to access it on the error page
            facesContext.getExternalContext().getRequestMap().put(VIEW_KEY, facesContext.getViewRoot());
        }
        else
        {
            // check for org.apache.myfaces.ERROR_HANDLING
            // do not generate an error page if it is false
            String errorHandling = facesContext.getExternalContext().getInitParameter(ERROR_HANDLING_PARAMETER);
            boolean errorHandlingDisabled = (errorHandling != null && errorHandling.equalsIgnoreCase("false"));
            if (!errorHandlingDisabled)
            {
                // write the error page
                Object response = facesContext.getExternalContext().getResponse();
                if (response instanceof HttpServletResponse)
                {
                    HttpServletResponse httpResp = (HttpServletResponse) response;
                    if (!httpResp.isCommitted())
                    {
                        httpResp.reset();
                        if (facesContext.getPartialViewContext().isAjaxRequest())
                        {
                            // ajax request --> xml error page 
                            httpResp.setContentType("text/xml; charset=UTF-8");
                            try
                            {
                                Writer writer = httpResp.getWriter();
                                // can't use facesContext.getResponseWriter(), because it might not have been set
                                ResponseWriter responseWriter = new HtmlResponseWriterImpl(writer, "text/xml", "utf-8");
                                PartialResponseWriter partialWriter = new PartialResponseWriter(responseWriter);
                                partialWriter.startDocument();
                                partialWriter.startError(ex.getClass().getName());
                                if (ex.getCause() != null)
                                {
                                    partialWriter.write(ex.getCause().toString());
                                }
                                else if (ex.getMessage() != null)
                                {
                                    partialWriter.write(ex.getMessage());
                                }
                                partialWriter.endError();
                                partialWriter.endDocument();
                            }
                            catch(IOException ioe)
                            {
                                throw new FacesException("Could not write the error page", ioe);
                            }
                        }
                        else
                        {
                            // normal request --> html error page
                            httpResp.setContentType("text/html; charset=UTF-8");
                            try
                            {
                                Writer writer = httpResp.getWriter();
                                debugHtml(writer, facesContext, ex);
                            }
                            catch(IOException ioe)
                            {
                                throw new FacesException("Could not write the error page", ioe);
                            }
                        }
                        log.log(Level.SEVERE, "An exception occurred", ex);

                        // mark the response as complete
                        facesContext.responseComplete();

                        errorPageWritten = true;
                    }
                }
            }
        }

        // rethrow the throwable, if we did not write the error page
        if (!errorPageWritten)
        {
            if (ex instanceof FacesException)
            {
                throw (FacesException) ex;
            }
            if (ex instanceof RuntimeException)
            {
                throw (RuntimeException) ex;
            }
            throw new FacesException(ex);
        }

    }

    private static String _getErrorTemplate(FacesContext context)
    {
        String errorTemplate = context.getExternalContext().getInitParameter(ERROR_TEMPLATE_RESOURCE);
        if (errorTemplate != null)
        {
            return errorTemplate;
        }
        return ERROR_TEMPLATE;
    }

    private static String _getDebugTemplate(FacesContext context)
    {
        String debugTemplate = context.getExternalContext().getInitParameter(DEBUG_TEMPLATE_RESOURCE);
        if (debugTemplate != null)
        {
            return debugTemplate;
        }
        return DEBUG_TEMPLATE;
    }

    private static void _init(FacesContext context) throws IOException
    {
        if (errorParts == null)
        {
            errorParts = _splitTemplate(_getErrorTemplate(context));
        }

        if (debugParts == null)
        {
            debugParts = _splitTemplate(_getDebugTemplate(context));
        }
    }

    private static String[] _splitTemplate(String rsc) throws IOException
    {
        InputStream is = ClassUtils.getContextClassLoader().getResourceAsStream(rsc);
        if (is == null)
        {
            // try to get the resource from ExternalContext
            is = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(rsc);
            if (is == null)
            {
                // fallback
                is = ErrorPageWriter.class.getClassLoader().getResourceAsStream(rsc);
            }
        }

        if (is == null)
        {
            // throw an IllegalArgumentException instead of a FileNotFoundException,
            // because when using <ui:debug /> this error is hard to trace,
            // because the Exception is thrown in the Renderer and so it seems like
            // the facelet (or jsp) does not exist.
            throw new IllegalArgumentException("Could not find resource " + rsc);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[512];
        int read;
        while ((read = is.read(buff)) != -1)
        {
            baos.write(buff, 0, read);
        }
        String str = baos.toString();
        return str.split("@@");
    }

    private static List<String> _getErrorId(Collection<UIComponent> components, Throwable... exs)
    {
        List<String> list = null;
        for (Throwable e : exs)
        {
            String message = e.getMessage();

            if (message == null)
            {
                continue;
            }

            Pattern pattern = Pattern.compile(REGEX_PATTERN);
            Matcher matcher = pattern.matcher(message);

            while (matcher.find())
            {
                if (list == null)
                {
                    list = new ArrayList<String>();
                }
                list.add(matcher.group(1));
            }
        }
        if (list != null && list.size() > 0)
        {
            return list;
        }
        else if (components != null)
        {
            list = new ArrayList<String>();
            for (UIComponent uiComponent : components)
            {
                if (uiComponent  != null)
                {
                    list.add(uiComponent.getId());
                }
            }
            return list;
        }
        return null;
    }

    private static void _writeException(Writer writer, Throwable e) throws IOException
    {
        StringWriter str = new StringWriter(256);
        PrintWriter pstr = new PrintWriter(str);
        e.printStackTrace(pstr);
        pstr.close();
        writer.write(str.toString().replaceAll("<", TS));
    }

    private static void _writeCause(Writer writer, Throwable ex) throws IOException
    {
        String msg = ex.getMessage();
        String contextAwareLocation = null;
        if (ex instanceof ContextAware)
        {
            ContextAware caex = (ContextAware) ex;
            contextAwareLocation = caex.getLocation().toString() + "    " +
                                   caex.getQName() + "=\"" +
                                   caex.getExpressionString() + "\"";
        }
        while (ex.getCause() != null)
        {
            ex = ex.getCause();
            if (ex instanceof ContextAware)
            {
                ContextAware caex = (ContextAware) ex;
                contextAwareLocation = caex.getLocation().toString() + "    " +
                                       caex.getQName() + "=\"" +
                                       caex.getExpressionString() + "\"";
            }
            if (ex.getMessage() != null)
            {
                msg = ex.getMessage();
            }
        }

        if (msg != null)
        {
            msg = ex.getClass().getName() + " - " + msg;
            writer.write(msg.replaceAll("<", TS));
        }
        else
        {
            writer.write(ex.getClass().getName());
        }
        StackTraceElement stackTraceElement = ex.getStackTrace()[0];
        writer.write("<br/> at " + stackTraceElement.toString());

        if (contextAwareLocation != null)
        {
            writer.write("<br/> <br/>");
            writer.write(contextAwareLocation);
            writer.write("<br/>");
        }
    }

    private static void _writeVariables(Writer writer, FacesContext faces, UIViewRoot view) throws IOException
    {
        ExternalContext ctx = faces.getExternalContext();
        _writeVariables(writer, ctx.getRequestParameterMap(), "Request Parameters");
        _writeVariables(writer, ctx.getRequestMap(), "Request Attributes");
        if (view != null)
        {
          _writeVariables(writer, view.getViewMap(), "View Attributes");
        }
        if (ctx.getSession(false) != null)
        {
            _writeVariables(writer, ctx.getSessionMap(), "Session Attributes");
        }
        MyfacesConfig config = MyfacesConfig.getCurrentInstance(ctx);
        if(config!=null && !config.isFlashScopeDisabled() && ctx.getFlash() != null)
        {
            _writeVariables(writer, ctx.getFlash(), "Flash Attributes");
        }
        _writeVariables(writer, ctx.getApplicationMap(), "Application Attributes");
    }

    private static void _writeVariables(Writer writer, Map<String, ? extends Object> vars, String caption)
            throws IOException
    {
        writer.write("<table><caption>");
        writer.write(caption);
        writer.write("</caption><thead><tr><th style=\"width: 10%; \">Name</th>"
                     + "<th style=\"width: 90%; \">Value</th></tr></thead><tbody>");
        boolean written = false;
        if (!vars.isEmpty())
        {
            SortedMap<String, Object> sortedMap = new TreeMap<String, Object>(vars);
            for (Map.Entry<String, Object> entry : sortedMap.entrySet())
            {
                String key = entry.getKey().toString();
                if (key.indexOf('.') == -1)
                {
                    writer.write("<tr><td>");
                    writer.write(key.replaceAll("<", TS));
                    writer.write("</td><td>");
                    Object value = entry.getValue();
                    // in some (very rare) situations value can be null or not null
                    // but with null toString() representation
                    if (value != null && value.toString() != null)
                    {
                        writer.write(value.toString().replaceAll("<", TS));
                    }
                    else
                    {
                        writer.write("null");
                    }
                    writer.write("</td></tr>");
                    written = true;
                }
            }
        }
        if (!written)
        {
            writer.write("<tr><td colspan=\"2\"><em>None</em></td></tr>");
        }
        writer.write("</tbody></table>");
    }

    private static void _writeComponent(FacesContext faces, Writer writer, UIComponent c, List<String> highlightId,
                                        boolean writeChildren) throws IOException
    {
        writer.write("<dl><dt");
        if (_isText(c))
        {
            writer.write(" class=\"uicText\"");
        }
        if (highlightId != null)
        {
            if ((highlightId.size() > 0))
            {
                String id = c.getId();
                if (highlightId.contains(id))
                {
                    writer.write(" class=\"highlightComponent\"");
                }
            }
        }
        writer.write(">");

        boolean hasChildren = (c.getChildCount() > 0 || c.getFacetCount() > 0) && writeChildren;

        int stateSize = 0;

        Object state = c.saveState(faces);
        if (state != null)
        {
            try
            {
                byte[] stateBytes = StateUtils.getAsByteArray(state, faces.getExternalContext());
                stateSize = stateBytes.length;
            }
            catch (Exception e)
            {
                stateSize = -1;
                if (log.isLoggable(Level.FINEST))
                {
                    log.fine("Could not determine state size: " + e.getMessage());
                }
            }
        }
        _writeStart(writer, c, hasChildren, true);
        writer.write(" - State size:" + stateSize + " bytes");
        writer.write("</dt>");
        if (hasChildren)
        {
            if (c.getFacetCount() > 0)
            {
                for (Map.Entry<String, UIComponent> entry : c.getFacets().entrySet())
                {
                    writer.write("<dd class=\"uicFacet\">");
                    writer.write("<span>");
                    writer.write(entry.getKey());
                    writer.write("</span>");
                    _writeComponent(faces, writer, entry.getValue(), highlightId, true);
                    writer.write("</dd>");
                }
            }
            if (c.getChildCount() > 0)
            {
                for (int i = 0, childCount = c.getChildCount(); i < childCount; i++)
                {
                    UIComponent child = c.getChildren().get(i);
                    writer.write("<dd>");
                    _writeComponent(faces, writer, child, highlightId, writeChildren);
                    writer.write("</dd>");
                }
            }
            writer.write("<dt>");
            _writeEnd(writer, c);
            writer.write("</dt>");
        }
        writer.write("</dl>");
    }

    /**
     * Creates the Extended Component Tree via UIViewRoot.visitTree()
     * and ExtendedComponentTreeVisitCallback as VisitCallback.
     *
     * @param writer
     * @param facesContext
     * @throws IOException
     */
    private static void _writeExtendedComponentTree(Writer writer,
            FacesContext facesContext) throws IOException
    {
        VisitContext visitContext = VisitContext.createVisitContext(
                facesContext, null, EnumSet.of(VisitHint.SKIP_UNRENDERED));
        facesContext.getViewRoot().visitTree(visitContext, new ExtendedComponentTreeVisitCallback(writer));
        _clearVisitedFacetCountMap(facesContext);
    }

    /**
     * The VisitCallback that is used to create the Extended Component Tree.
     *
     * @author Jakob Korherr
     */
    private static class ExtendedComponentTreeVisitCallback implements VisitCallback
    {

        private Writer _writer;

        public ExtendedComponentTreeVisitCallback(Writer writer)
        {
            _writer = writer;
        }

        @SuppressWarnings("unchecked")
        public VisitResult visit(VisitContext context, UIComponent target)
        {
            final Map<String, Object> requestMap = context.getFacesContext()
                    .getExternalContext().getRequestMap();

            try
            {
                if (!(target instanceof UIViewRoot))
                {
                    _writer.write("<dd>");
                }

                UIComponent parent = target.getParent();
                boolean hasChildren = (target.getChildCount() > 0 || target.getFacetCount() > 0);
                String facetName = _getFacetName(target);

                if (!(target instanceof UIColumn))
                {
                    if (parent instanceof UIColumn
                            && ((parent.getChildCount() > 0 && parent.getChildren().get(0) == target)
                                    ||  (facetName != null &&
                                            _getVisitedFacetCount(context.getFacesContext(), parent) == 0)))
                    {
                        if (parent.getParent() instanceof UIData
                                && _isFirstUIColumn(parent.getParent(), (UIColumn) parent))
                        {
                            _writer.write("<span>Row: ");
                            int rowIndex = ((UIData) parent.getParent()).getRowIndex();
                            _writer.write("" + rowIndex);
                            if (rowIndex == -1)
                            {
                                // tell the user that rowIndex == -1 stands for visiting column-facets
                                _writer.write(" (all column facets)");
                            }
                            _writer.write("</span>");
                        }
                        _writer.write("<dl><dt>");
                        _writeStart(_writer, parent, true, false);
                        _writer.write("</dt><dd>");
                    }

                    if (facetName != null)
                    {
                        _writer.write("<span>" + facetName + "</span>");
                        _incrementVisitedFacetCount(context.getFacesContext(), parent);
                    }
                    _writer.write("<dl><dt");
                    if (_isText(target))
                    {
                        _writer.write(" class=\"uicText\"");
                    }
                    _writer.write(">");

                    Map<String, List<Object[]>> debugInfos = null;
                    // is the target a EditableValueHolder component?
                    // If so, debug infos from DebugPhaseListener should be available
                    if (target instanceof EditableValueHolder)
                    {
                        // get the debug info
                        debugInfos = (Map<String, List<Object[]>>) requestMap
                                .get(DEBUG_INFO_KEY + target.getClientId());
                    }

                    // Get the component's renderer.
                    // Note that getRenderer(FacesContext context) is definded in UIComponent,
                    // but it is protected, so we have to use reflection!
                    Renderer renderer = null;
                    try
                    {
                        Method getRenderer = UIComponent.class.getDeclaredMethod(
                                "getRenderer", FacesContext.class);
                        // make it accessible for us!
                        getRenderer.setAccessible(true);
                        renderer = (Renderer) getRenderer.invoke(target, context.getFacesContext());
                    }
                    catch (Exception e)
                    {
                        // nothing - do not output renderer information
                    }

                    // write the component start
                    _writeStart(_writer, target, (hasChildren || debugInfos != null || renderer != null), false);
                    _writer.write("</dt>");

                    if (renderer != null)
                    {
                        // write renderer info
                        _writer.write("<div class=\"renderer\">Rendered by ");
                        _writer.write(renderer.getClass().getCanonicalName());
                        _writer.write("</div>");

                        if (!hasChildren && debugInfos == null)
                        {
                            // close the component
                            _writer.write("<dt>");
                            _writeEnd(_writer, target);
                            _writer.write("</dt>");
                        }
                    }

                    if (debugInfos != null)
                    {
                        final String fieldid = target.getClientId() + "_lifecycle";
                        _writer.write("<div class=\"lifecycle_values_wrapper\">");
                        _writer.write("<a href=\"#\" onclick=\"toggle('");
                        _writer.write(fieldid);
                        _writer.write("'); return false;\"><span id=\"");
                        _writer.write(fieldid);
                        _writer.write("Off\">+</span><span id=\"");
                        _writer.write(fieldid);
                        _writer.write("On\" style=\"display: none;\">-</span> Value Lifecycle</a>");
                        _writer.write("<div id=\"");
                        _writer.write(fieldid);
                        _writer.write("\" class=\"lifecycle_values\">");

                        // process any available debug info
                        for (Map.Entry<String, List<Object[]>> entry : debugInfos.entrySet())
                        {
                            _writer.write("<span>");
                            _writer.write(entry.getKey());
                            _writer.write("</span><ol>");
                            int i = 0;
                            for (Object[] debugInfo : entry.getValue())
                            {
                                // structure of the debug-info array:
                                //     - 0: phase
                                //     - 1: old value
                                //     - 2: new value
                                //     - 3: StackTraceElement List

                                // oldValue and newValue could be null
                                String oldValue = debugInfo[1] == null ? "null" : debugInfo[1].toString();
                                String newValue = debugInfo[2] == null ? "null" : debugInfo[2].toString();
                                _writer.write("<li><b>");
                                _writer.write(entry.getKey());
                                _writer.write("</b> set from <b>");
                                _writer.write(oldValue);
                                _writer.write("</b> to <b>");
                                _writer.write(newValue);
                                _writer.write("</b> in Phase ");
                                _writer.write(debugInfo[0].toString());

                                // check if a call stack is available
                                if (debugInfo[3] != null)
                                {
                                    final String stackTraceId = fieldid + "_" + entry.getKey() + "_" + i;
                                    _writer.write("<div class=\"stacktrace_wrapper\">");
                                    _writer.write("<a href=\"#\" onclick=\"toggle('");
                                    _writer.write(stackTraceId);
                                    _writer.write("'); return false;\"><span id=\"");
                                    _writer.write(stackTraceId);
                                    _writer.write("Off\">+</span><span id=\"");
                                    _writer.write(stackTraceId);
                                    _writer.write("On\" style=\"display: none;\">-</span> Call Stack</a>");
                                    _writer.write("<div id=\"");
                                    _writer.write(stackTraceId);
                                    _writer.write("\" class=\"stacktrace_values\">");
                                    _writer.write("<ul>");
                                    for (StackTraceElement stackTraceElement
                                            : (List<StackTraceElement>) debugInfo[3])
                                    {
                                        _writer.write("<li>");
                                        _writer.write(stackTraceElement.toString());
                                        _writer.write("</li>");
                                    }
                                    _writer.write("</ul></div></div>");
                                }

                                _writer.write("</li>");

                                i++;
                            }
                            _writer.write("</ol>");
                        }

                        _writer.write("</div></div>");

                        // now remove the debug info from the request map, 
                        // so that it does not appear in the scope values of the debug page 
                        requestMap.remove(DEBUG_INFO_KEY + target.getClientId());

                        if (!hasChildren)
                        {
                            // close the component
                            _writer.write("<dt>");
                            _writeEnd(_writer, target);
                            _writer.write("</dt>");
                        }
                    }
                }

                if (!hasChildren)
                {
                    _writer.write("</dl>");

                    while (parent != null &&
                           ((parent.getChildCount()>0 && parent.getChildren().get(parent.getChildCount()-1) == target)
                                    || (parent.getFacetCount() != 0
                                            && _getVisitedFacetCount(context.getFacesContext(), parent) == 
                                                    parent.getFacetCount())))
                    {
                        // target is last child of parent or the "last" facet

                        // remove the visited facet count from the attribute map
                        _removeVisitedFacetCount(context.getFacesContext(), parent);

                        // check for componentes that visit their children multiple times
                        if (parent instanceof UIData)
                        {
                            UIData uidata = (UIData) parent;
                            if (uidata.getRowIndex() != uidata.getRowCount() - 1)
                            {
                                // only continue if we're in the last row
                                break;
                            }
                        }
                        else if (parent instanceof UIRepeat)
                        {
                            UIRepeat uirepeat = (UIRepeat) parent;
                            if (uirepeat.getIndex() + uirepeat.getStep() < uirepeat.getRowCount())
                            {
                                // only continue if we're in the last row
                                break;
                            }
                        }

                        _writer.write("</dd><dt>");
                        _writeEnd(_writer, parent);
                        _writer.write("</dt></dl>");

                        if (!(parent instanceof UIViewRoot))
                        {
                            _writer.write("</dd>");
                        }

                        target = parent;
                        parent = target.getParent();
                    }
                }
            }
            catch (IOException ioe)
            {
                throw new FacesException(ioe);
            }

            return VisitResult.ACCEPT;
        }

    }

    private static boolean _isFirstUIColumn(UIComponent uidata, UIColumn uicolumn)
    {
        for (int i = 0, childCount = uidata.getChildCount(); i < childCount; i++)
        {
            UIComponent child = uidata.getChildren().get(i);
            if (child instanceof UIColumn)
            {
                return (child == uicolumn);
            }
        }
        return false;
    }

    private static String _getFacetName(UIComponent component)
    {
        UIComponent parent = component.getParent();
        if (parent != null)
        {
            if (parent.getFacetCount() > 0)
            {
                for (Map.Entry<String, UIComponent> entry : parent.getFacets().entrySet())
                {
                    if (entry.getValue() == component)
                    {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }

    private static int _getVisitedFacetCount(FacesContext facesContext, UIComponent component)
    {
        Map<UIComponent, Integer> visitedFacetCount = (Map<UIComponent, Integer>)
            facesContext.getAttributes().get(VISITED_FACET_COUNT_KEY);
        if (visitedFacetCount == null)
        {
            return 0;
        }
        Integer count = visitedFacetCount.get(component);
        if (count != null)
        {
            return count;
        }
        return 0;
    }

    private static void _incrementVisitedFacetCount(FacesContext facesContext, UIComponent component)
    {
        Map<UIComponent, Integer> visitedFacetCount = (Map<UIComponent, Integer>)
            facesContext.getAttributes().get(VISITED_FACET_COUNT_KEY);
        if (visitedFacetCount == null)
        {
            visitedFacetCount = new HashMap<UIComponent, Integer>();
            facesContext.getAttributes().put(VISITED_FACET_COUNT_KEY, visitedFacetCount);
        }
        visitedFacetCount.put(component, _getVisitedFacetCount(facesContext, component) + 1);
    }

    private static void _removeVisitedFacetCount(FacesContext facesContext, UIComponent component)
    {
        Map<UIComponent, Integer> visitedFacetCount = (Map<UIComponent, Integer>)
            facesContext.getAttributes().get(VISITED_FACET_COUNT_KEY);
        if (visitedFacetCount == null)
        {
            return;
        }
        visitedFacetCount.remove(component);
    }
    
    private static void _clearVisitedFacetCountMap(FacesContext facesContext)
    {
        Map<UIComponent, Integer> visitedFacetCount = (Map<UIComponent, Integer>)
            facesContext.getAttributes().get(VISITED_FACET_COUNT_KEY);
        if (visitedFacetCount != null)
        {
            visitedFacetCount.clear();
            facesContext.getAttributes().remove(VISITED_FACET_COUNT_KEY);
        }
    }

    private static void _writeEnd(Writer writer, UIComponent c) throws IOException
    {
        if (!_isText(c))
        {
            writer.write(TS);
            writer.write('/');
            writer.write(_getName(c));
            writer.write('>');
        }
    }

    private static void _writeAttributes(Writer writer, UIComponent c, boolean valueExpressionValues)
    {
        try
        {
            BeanInfo info = Introspector.getBeanInfo(c.getClass());
            PropertyDescriptor[] pd = info.getPropertyDescriptors();
            Method m = null;
            Object v = null;
            ValueExpression valueExpression = null;
            String str = null;
            for (int i = 0; i < pd.length; i++)
            {
                if ((pd[i].getWriteMethod() != null || Arrays.binarySearch(ALWAYS_WRITE, pd[i].getName()) > -1)
                    && Arrays.binarySearch(IGNORE, pd[i].getName()) < 0)
                {
                    m = pd[i].getReadMethod();
                    if (m != null)
                    {
                        try
                        {
                            // first check if the property is a ValueExpression
                            valueExpression = c.getValueExpression(pd[i].getName());
                            if (valueExpressionValues && valueExpression != null)
                            {
                                String expressionString = valueExpression.getExpressionString();
                                if (null == expressionString)
                                {
                                    expressionString = "";
                                }
                                _writeAttribute(writer, pd[i].getName(), expressionString);
                            }
                            else
                            {
                                v = m.invoke(c, null);
                                if (v != null)
                                {
                                    if (v instanceof Collection || v instanceof Map || v instanceof Iterator)
                                    {
                                        continue;
                                    }
                                    if (v instanceof Expression)
                                    {
                                        str = ((Expression)v).getExpressionString();
                                    }
                                    else if (v instanceof ValueBinding)
                                    {
                                        str = ((ValueBinding) v).getExpressionString();
                                    }
                                    else if (v instanceof MethodBinding)
                                    {
                                        str = ((MethodBinding) v).getExpressionString();
                                    }
                                    else
                                    {
                                        str = v.toString();
                                    }

                                    _writeAttribute(writer, pd[i].getName(), str);
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            // do nothing
                        }
                    }
                }
            }

            ValueExpression binding = c.getValueExpression("binding");
            if (binding != null)
            {
                _writeAttribute(writer, "binding", binding.getExpressionString());
            }

            // write the location
            String location = _getComponentLocation(c);
            if (location != null)
            {
                _writeAttribute(writer, "location", location);
            }
        }
        catch (Exception e)
        {
            // do nothing
        }
    }

    private static void _writeAttribute(Writer writer, String name, String value) throws IOException
    {
        writer.write(" ");
        writer.write(name);
        writer.write("=\"");
        writer.write(value.replaceAll("<", TS));
        writer.write("\"");
    }

    private static void _writeStart(Writer writer, UIComponent c,
            boolean children, boolean valueExpressionValues) throws IOException
    {
        if (_isText(c))
        {
            String str = c.toString().trim();
            writer.write(str.replaceAll("<", TS));
        }
        else
        {
            writer.write(TS);
            writer.write(_getName(c));
            _writeAttributes(writer, c, valueExpressionValues);
            if (children)
            {
                writer.write('>');
            }
            else
            {
                writer.write("/>");
            }
        }
    }

    private static String _getName(UIComponent c)
    {
        String nm = c.getClass().getName();
        return nm.substring(nm.lastIndexOf('.') + 1);
    }

    private static boolean _isText(UIComponent c)
    {
        return (c.getClass().getName().startsWith("org.apache.myfaces.view.facelets.compiler"));
    }

    private static void _prepareExceptionStack(Throwable ex)
    {

        if (ex == null)
        {
            return;
        }

        // check for getRootCause and getCause-methods
        if (!_initCausePerReflection(ex, "getRootCause"))
        {
            _initCausePerReflection(ex, "getCause");
        }

        _prepareExceptionStack(ex.getCause());
    }

    private static boolean _initCausePerReflection(Throwable ex, String methodName)
    {
        try
        {
            Method causeGetter = ex.getClass().getMethod(methodName, (Class[])null);
            Throwable rootCause = (Throwable)causeGetter.invoke(ex, (Object[])null);
            return _initCauseIfAvailable(ex, rootCause);
        }
        catch (Exception e1)
        {
            return false;
        }
    }

    private static boolean _initCauseIfAvailable(Throwable th, Throwable cause)
    {
        if (cause == null)
        {
            return false;
        }

        try
        {
            Method m = Throwable.class.getMethod("initCause", new Class[] { Throwable.class });
            m.invoke(th, new Object[] { cause });
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Gets the Location of the given UIComponent from its attribute map.
     * @param component
     * @return
     */
    private static String _getComponentLocation(UIComponent component)
    {
        Location location = (Location) component.getAttributes()
                .get(UIComponent.VIEW_LOCATION_KEY);
        if (location != null)
        {
            return location.toString();
        }
        return null;
    }
}
