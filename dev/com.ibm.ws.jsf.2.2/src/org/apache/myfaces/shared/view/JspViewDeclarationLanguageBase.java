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
package org.apache.myfaces.shared.view;

import java.beans.BeanInfo;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FactoryFinder;
import javax.faces.application.Resource;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.view.StateManagementStrategy;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewMetadata;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.myfaces.shared.application.DefaultViewHandlerSupport;
import org.apache.myfaces.shared.application.ViewHandlerSupport;
import org.apache.myfaces.shared.config.MyfacesConfig;


public abstract class JspViewDeclarationLanguageBase extends ViewDeclarationLanguageBase
{
  private static final Logger log = Logger.getLogger(JspViewDeclarationLanguageBase.class.getName());
  
  private static final String FORM_STATE_MARKER = "<!--@@JSF_FORM_STATE_MARKER@@-->";
  private static final String AFTER_VIEW_TAG_CONTENT_PARAM = JspViewDeclarationLanguageBase.class
              + ".AFTER_VIEW_TAG_CONTENT";
  private static final int FORM_STATE_MARKER_LEN = FORM_STATE_MARKER.length();

  private ViewHandlerSupport _cachedViewHandlerSupport;
  
  @Override
  public String getId()
  {
      return ViewDeclarationLanguage.JSP_VIEW_DECLARATION_LANGUAGE_ID;
  }
  
  @Override
  public void buildView(FacesContext context, UIViewRoot view) throws IOException
  {
      // memorize that buildView() has been called for this view
      setViewBuilt(context, view);
      
      if (context.getPartialViewContext().isPartialRequest())
      {
          // try to get (or create) a ResponseSwitch and turn off the output
          Object origResponse = context.getExternalContext().getResponse();
          ResponseSwitch responseSwitch = getResponseSwitch(origResponse);
          if (responseSwitch == null)
          {
              // no ResponseSwitch installed yet - create one 
              responseSwitch = createResponseSwitch(origResponse);
              if (responseSwitch != null)
              {
                  // install the ResponseSwitch
                  context.getExternalContext().setResponse(responseSwitch);
              }
          }
          if (responseSwitch != null)
          {
              // turn the output off
              responseSwitch.setEnabled(false);
          }
      }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public BeanInfo getComponentMetadata(FacesContext context, Resource componentResource)
  {
      throw new UnsupportedOperationException();
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public Resource getScriptComponentResource(FacesContext context, Resource componentResource)
  {
      throw new UnsupportedOperationException();
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public void renderView(FacesContext context, UIViewRoot view) throws IOException
  {
      //Try not to use native objects in this class.  Both MyFaces and the bridge
      //provide implementations of buildView but they do not override this class.
      checkNull(context, "context");
      checkNull(view, "view");
      
      // do not render the view if the rendered attribute for the view is false
      if (!view.isRendered())
      {
          if (log.isLoggable(Level.FINEST))
          {
              log.finest("View is not rendered");
          }
          return;
      }
      
      // Check if the current view has already been built via VDL.buildView()
      // and if not, build it from here. This is necessary because legacy ViewHandler
      // implementations return null on getViewDeclarationLanguage() and thus
      // VDL.buildView() is never called. Furthermore, before JSF 2.0 introduced 
      // the VDLs, the code that built the view was in ViewHandler.renderView().
      if (!isViewBuilt(context, view))
      {
          buildView(context, view);
      }
  
      ExternalContext externalContext = context.getExternalContext();
  
      String viewId = context.getViewRoot().getViewId();
  
      if (log.isLoggable(Level.FINEST))
      {
          log.finest("Rendering JSP view: " + viewId);
      }
  
  
      // handle character encoding as of section 2.5.2.2 of JSF 1.1
      if(null != externalContext.getSession(false))
      {
        externalContext.getSessionMap().put(ViewHandler.CHARACTER_ENCODING_KEY, 
                externalContext.getResponseCharacterEncoding());
      }
  
      // render the view in this method (since JSF 1.2)
      RenderKitFactory renderFactory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
      RenderKit renderKit = renderFactory.getRenderKit(context, view.getRenderKitId());
  
      ResponseWriter responseWriter = context.getResponseWriter();
      if (responseWriter == null)
      {
          responseWriter = renderKit.createResponseWriter(externalContext.getResponseOutputWriter(), 
                  null, externalContext.getRequestCharacterEncoding());
          context.setResponseWriter(responseWriter);
      }
      
      // try to enable the ResponseSwitch again (disabled in buildView())
      Object response = context.getExternalContext().getResponse();
      ResponseSwitch responseSwitch = getResponseSwitch(response);
      if (responseSwitch != null)
      {
          responseSwitch.setEnabled(true);
      }
  
      ResponseWriter oldResponseWriter = responseWriter;
      StringWriter stateAwareWriter = null;
      
      StateManager stateManager = context.getApplication().getStateManager();
      boolean viewStateAlreadyEncoded = isViewStateAlreadyEncoded(context);
      
      if (!viewStateAlreadyEncoded)
      {
        // we will need to parse the reponse and replace the view_state token with the actual state
        stateAwareWriter = new StringWriter();
  
        // Create a new response-writer using as an underlying writer the stateAwareWriter
        // Effectively, all output will be buffered in the stateAwareWriter so that later
        // this writer can replace the state-markers with the actual state.
        responseWriter = oldResponseWriter.cloneWithWriter(stateAwareWriter);
        context.setResponseWriter(responseWriter);
      }
  
      try
      {
        if (!actuallyRenderView(context, view))
        {
          return;
        }
      }
      finally
      {
        if(oldResponseWriter != null)
        {
            context.setResponseWriter(oldResponseWriter);    
        }
      }
  
      if (!viewStateAlreadyEncoded)
      {
        // parse the response and replace the token wit the state
        flushBufferToWriter(stateAwareWriter.getBuffer(), externalContext.getResponseOutputWriter());
      }
      else
      {
        stateManager.saveView(context);
      }
      
      // now disable the ResponseSwitch again
      if (responseSwitch != null)
      {
          responseSwitch.setEnabled(false);
      }
  
      // Final step - we output any content in the wrappedResponse response from above to the response,
      // removing the wrappedResponse response from the request, we don't need it anymore
      ViewResponseWrapper afterViewTagResponse = (ViewResponseWrapper) externalContext.getRequestMap()
              .get(AFTER_VIEW_TAG_CONTENT_PARAM);
      externalContext.getRequestMap().remove(AFTER_VIEW_TAG_CONTENT_PARAM);
      
      // afterViewTagResponse is null if the current request is a partial request
      if (afterViewTagResponse != null)
      {
          afterViewTagResponse.flushToWriter(externalContext.getResponseOutputWriter(), 
                  externalContext.getResponseCharacterEncoding());
      }
  
      //TODO sobryan: Is this right?
      context.getResponseWriter().flush();
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public ViewMetadata getViewMetadata(FacesContext context, String viewId)
  {
      // Not necessary given that this method always returns null, but staying true to
      // the spec.
  
      checkNull(context, "context");
      //checkNull(viewId, "viewId");
  
      // JSP impl must return null.
  
      return null;
  }
  
  protected boolean isViewStateAlreadyEncoded(FacesContext context)
  {
    if (MyfacesConfig.getCurrentInstance(context.getExternalContext()).isMyfacesImplAvailable())
    {
      // In MyFaces the viewState key is already encoded is server side state saving is being used
      return !context.getApplication().getStateManager().isSavingStateInClient(context);
    }
    else
    {
      return false;
    }
  }
  
  protected void setAfterViewTagResponseWrapper(ExternalContext ec, ViewResponseWrapper wrapper)
  {
    ec.getRequestMap().put(AFTER_VIEW_TAG_CONTENT_PARAM, wrapper);
  }
  
  protected void flushBufferToWriter(StringBuffer buff, Writer writer) throws IOException
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    StateManager stateManager = facesContext.getApplication().getStateManager();

    StringWriter stateWriter = new StringWriter();
    ResponseWriter realWriter = facesContext.getResponseWriter();
    facesContext.setResponseWriter(realWriter.cloneWithWriter(stateWriter));

    Object serializedView = stateManager.saveView(facesContext);

    stateManager.writeState(facesContext, serializedView);
    facesContext.setResponseWriter(realWriter);

    String state = stateWriter.getBuffer().toString();

    // State markers must be replaced
    int lastFormMarkerPos = 0;
    int formMarkerPos = 0;
    // Find all state markers and write out actual state instead
    while ((formMarkerPos = buff.indexOf(JspViewDeclarationLanguageBase.FORM_STATE_MARKER, formMarkerPos)) > -1)
    {
      // Write content before state marker
      writePartialBuffer(buff, lastFormMarkerPos, formMarkerPos, writer);
      // Write state and move position in buffer after marker
      writer.write(state);
      formMarkerPos += JspViewDeclarationLanguageBase.FORM_STATE_MARKER_LEN;
      lastFormMarkerPos = formMarkerPos;
    }

    // Write content after last state marker
    if (lastFormMarkerPos < buff.length())
    {
      writePartialBuffer(buff, lastFormMarkerPos, buff.length(), writer);
    }
  }
  
  protected void writePartialBuffer(StringBuffer contentBuffer, int beginIndex, 
          int endIndex, Writer writer) throws IOException
  {
    int index = beginIndex;
    int bufferSize = 2048;
    char[] bufToWrite = new char[bufferSize];

    while (index < endIndex)
    {
      int maxSize = Math.min(bufferSize, endIndex - index);

      contentBuffer.getChars(index, index + maxSize, bufToWrite, 0);
      writer.write(bufToWrite, 0, maxSize);

      index += bufferSize;
    }
  }

  /**
   * Render the view now - properly setting and resetting the response writer
   * [MF] Modified to return a boolean so subclass that delegates can determine
   * whether the rendering succeeded or not. TRUE means success.
   */
  protected boolean actuallyRenderView(FacesContext facesContext, UIViewRoot viewToRender)
      throws IOException
  {
      // Set the new ResponseWriter into the FacesContext, saving the old one aside.
      ResponseWriter responseWriter = facesContext.getResponseWriter();
  
      // Now we actually render the document
      // Call startDocument() on the ResponseWriter.
      responseWriter.startDocument();
  
      // Call encodeAll() on the UIViewRoot
      viewToRender.encodeAll(facesContext);
  
      // Call endDocument() on the ResponseWriter
      responseWriter.endDocument();
  
      responseWriter.flush();
      
      // rendered successfully -- forge ahead
      return true;
  }
  
  @Override
  public StateManagementStrategy getStateManagementStrategy(FacesContext context, String viewId)
  {
      return null;
  }

  @Override
  protected String calculateViewId(FacesContext context, String viewId)
  {
      if (_cachedViewHandlerSupport == null)
      {
          _cachedViewHandlerSupport = new DefaultViewHandlerSupport();
      }
  
      return _cachedViewHandlerSupport.calculateViewId(context, viewId);
  }
  
  /**
   * Returns true if the given UIViewRoot has already been built via VDL.buildView().
   * This is necessary because legacy ViewHandler implementations return null on 
   * getViewDeclarationLanguage() and thus VDL.buildView() is never called. 
   * So we have to check this in renderView() and, if it is false, we have to
   * call buildView() manually before the rendering.
   *  
   * @param facesContext
   * @param view
   * @return
   */
  protected boolean isViewBuilt(FacesContext facesContext, UIViewRoot view)
  {
      return Boolean.TRUE.equals(facesContext.getAttributes().get(view));
  }
  
  /**
   * Saves a flag in the attribute map of the FacesContext to indicate
   * that the given UIViewRoot was already built with VDL.buildView().
   * 
   * @param facesContext
   * @param view
   */
  protected void setViewBuilt(FacesContext facesContext, UIViewRoot view)
  {
      facesContext.getAttributes().put(view, Boolean.TRUE);
  }

  /**
   * Trys to obtain a ResponseSwitch from the Response.
   * @param response
   * @return if found, the ResponseSwitch, null otherwise
   */
  private static ResponseSwitch getResponseSwitch(Object response)
  {
      // unwrap the response until we find a ResponseSwitch
      while (response != null)
      {
          if (response instanceof ResponseSwitch)
          {
              // found
              return (ResponseSwitch) response;
          }
          if (response instanceof ServletResponseWrapper)
          {
              // unwrap
              response = ((ServletResponseWrapper) response).getResponse();
          }
          // no more possibilities to find a ResponseSwitch
          break; 
      }
      return null; // not found
  }
  
  /**
   * Try to create a ResponseSwitch for this response.
   * @param response
   * @return the created ResponseSwitch, if there is a ResponseSwitch 
   *         implementation for the given response, null otherwise
   */
  private static ResponseSwitch createResponseSwitch(Object response)
  {
      if (response instanceof HttpServletResponse)
      {
          return new HttpServletResponseSwitch((HttpServletResponse) response);
      }
      else if (response instanceof ServletResponse)
      {
          return new ServletResponseSwitch((ServletResponse) response);
      }
      return null;
  }

  /**
   * Writes the response and replaces the state marker tags with the state information for the current context
   */
/*  private static class StateMarkerAwareWriter extends Writer
  {
    private StringBuilder buf;

    public StateMarkerAwareWriter()
    {
        this.buf = new StringBuilder();
    }

    @Override
    public void close() throws IOException
    {
    }

    @Override
    public void flush() throws IOException
    {
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
      if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0))
      {
        throw new IndexOutOfBoundsException();
      }
      else if (len == 0)
      {
        return;
      }
      buf.append(cbuf, off, len);
    }

    public StringBuilder getStringBuilder()
    {
      return buf;
    }
  }*/
}
