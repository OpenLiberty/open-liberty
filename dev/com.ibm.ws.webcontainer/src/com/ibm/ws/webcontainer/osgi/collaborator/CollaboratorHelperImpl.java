/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.collaborator;

/*
 * LIBERTY overrides:
 * 1.  Get registered collaborators from CollaboratorService
 * 2.  Support only the spi generic WebAppInvocationCollaborators
 * 3.  Leave all the transaction work to the transaction collaborator
 */
import java.io.IOException;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.ws.container.DeployedModule;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.collaborator.WebAppSecurityCollaborator;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer.spiadapter.collaborator.IInvocationCollaborator;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.webapp.WebAppErrorReport;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.collaborator.CollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.CollaboratorInvocationEnum;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorMetaData;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInvocationCollaborator;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;

public class CollaboratorHelperImpl extends CollaboratorHelper
{
  private Set<WebAppInvocationCollaborator> webAppInvCollabs;
  private String securityDomainForApp = null;
  protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.collaborator");
  
  public CollaboratorHelperImpl(WebApp webApp, DeployedModule deployedMod)
  {
    super(webApp);
    if (webApp != null)
    {      
      nameSpaceCollaborator = new WebAppNameSpaceCollaboratorImpl();      
      transactionCollaborator = CollaboratorServiceImpl.getWebAppTransactionCollaborator(); 
      // IBM-Authorization-Roles header used to map security collaborator - may be null
      Dictionary<String,String> headers = ((WebAppConfiguration)webApp.getConfiguration()).getBundleHeaders();
      if (headers != null)
          securityDomainForApp = headers.get("IBM-Authorization-Roles");
      connectionCollaborator = CollaboratorServiceImpl.getWebAppConnectionCollaborator();
      webAppInvCollabs = CollaboratorServiceImpl.getWebAppInvocationCollaborators();
    }
  }
  
  /*
   * Returns the security collaborator that has been registered (by security services) for the
   * SecurityDomain specified by the application.  If no collaborator has been registered for
   * that domain then the super class provides a default security collaborator implementation.
   */
  
  private static WebAppSecurityCollaborator staticDefaultSecurityCollaborator = new WebAppSecurityCollaborator();
  @Override
  public IWebAppSecurityCollaborator getSecurityCollaborator() {
      // Security service may have been added or removed since app was installed so get 'live' collab service
      IWebAppSecurityCollaborator service = CollaboratorServiceImpl.getWebAppSecurityCollaborator(securityDomainForApp);
      if (service != null) 
      {
          // set for use by super class on pre/postInvoke calls
          securityCollaborator = service;
          return securityCollaborator;
      }
      else
      {
          // Reset the local variable so we can get the default implementation.
          securityCollaborator = staticDefaultSecurityCollaborator; //this can be static since it's a stubbed out version and just has no-op methods
          return securityCollaborator;
      }
  }

 
  /*
   * LIBERTY: collaborators are not passed through each web app but are managed within this class
   * (so ignore the null arg and use the local list)
   */
  public void doInvocationCollaboratorsPreInvoke(IInvocationCollaborator[] webAppInvocationCollaborators, WebComponentMetaData cmd,
                                                 ServletRequest request, ServletResponse response)
  {
    if (webAppInvCollabs != null && !webAppInvCollabs.isEmpty())
    {
      for (WebAppInvocationCollaborator inv : webAppInvCollabs)
      {
        inv.preInvoke(cmd,request,response);
      }
    }
  }

  public void doInvocationCollaboratorsPostInvoke(IInvocationCollaborator[] webAppInvocationCollaborators, WebComponentMetaData cmd,
                                                  ServletRequest request, ServletResponse response)
  {
    if (webAppInvCollabs != null && !webAppInvCollabs.isEmpty())
    {
      for (WebAppInvocationCollaborator inv : webAppInvCollabs)
      {
        inv.postInvoke(cmd,request,response);
      }
    }
  }

  public void doInvocationCollaboratorsPreInvoke(IInvocationCollaborator[] webAppInvocationCollaborators, com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData cmd)
  {
    if (webAppInvCollabs != null && !webAppInvCollabs.isEmpty())
    {
      for (WebAppInvocationCollaborator inv : webAppInvCollabs)
      {
        inv.preInvoke(cmd);
      }
    }
  }

  public void doInvocationCollaboratorsPostInvoke(IInvocationCollaborator[] webAppInvocationCollaborators, com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData cmd)
  {
    if (webAppInvCollabs != null && !webAppInvCollabs.isEmpty())
    {
      for (WebAppInvocationCollaborator inv : webAppInvCollabs)
      {
        inv.postInvoke(cmd);
      }
    }
  }

  // The following 3 methods are concrete implementations of the abstract ones in
  // CollaboratorHelper. Eventually those abstract methods should be removed.

  protected void checkTransaction(Object tx1)
  {
      /**
       * LIBERTY: This function moved into the transaction collaborator
       */
  }

  protected void checkForRollback()
  {
      /**
       * LIBERTY: This function moved into the transaction collaborator
       */
  }

  @Override
  protected Object getTransaction() throws Exception
  {
      /**
       * LIBERTY: This function moved into the transaction collaborator 
       */
    return null;
  }
  
  /*
   * (non-Javadoc)
   * 
   * @seecom.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper#
   * processSecurityPreInvokeException
   * (com.ibm.wsspi.webcontainer.security.SecurityViolationException,
   * com.ibm.wsspi.webcontainer.RequestProcessor,
   * javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse,
   * com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext,
   * com.ibm.ws.webcontainer.webapp.WebApp, java.lang.String)
   */
  public Object processSecurityPreInvokeException(SecurityViolationException sve, RequestProcessor requestProcessor, HttpServletRequest request,
          HttpServletResponse response, WebAppDispatcherContext dispatchContext, WebApp context, String name) throws ServletErrorReport {

      Object secObject = null;

     
      secObject = sve.getWebSecurityContext();
      int sc = sve.getStatusCode(); 
      Throwable cause = sve.getCause();

            if (sc == HttpServletResponse.SC_FORBIDDEN) {
          // If the user has defined a custom error page for
          // SC_FORBIDDEN (HTTP status code 403) then send
          // it to the client ...
          if (context.isErrorPageDefined(sc) == true) {
      
              WebAppErrorReport wErrorReport = new WebAppErrorReport(cause);
              wErrorReport.setErrorCode(sc);
              context.sendError(request, response, wErrorReport);
          } else {
              // ... otherwise, use the one provided by the
              // SecurityCollaborator
                            try {
                  securityCollaborator.handleException(request, response, cause);
              } catch (Exception ex) {
                  if (requestProcessor != null) {
                      throw WebAppErrorReport.constructErrorReport(ex, requestProcessor);
                  } else {
                      throw WebAppErrorReport.constructErrorReport(ex, name);
                  }
              }
              // reply.sendError(wResp);
          } // end if-else
      } else if (sc == HttpServletResponse.SC_UNAUTHORIZED) {
          // Invoking handleException will add the necessary headers
          // to the response ...
          try {
              securityCollaborator.handleException(request, response, cause);
          } catch (Exception ex) {
              if (requestProcessor != null) {
                  throw WebAppErrorReport.constructErrorReport(ex, requestProcessor);
              } else {
                  throw WebAppErrorReport.constructErrorReport(ex, name);
              }
          }

          // ... if the user has defined a custom error page for
          // SC_UNAUTHORIZED (HTTP status code 401) then
          // send it to the client
          if (context.isErrorPageDefined(sc) == true) {
              
              WebAppErrorReport wErrorReport = new WebAppErrorReport(cause);
              wErrorReport.setErrorCode(sc);
              context.sendError(request, response, wErrorReport);
          } else {
              // reply.sendError(wResp); comment-out 140967
          }

      } else {
          // Unexpected status code ... not SC_UNAUTHORIZED or SC_FORBIDDEN
          try {
              securityCollaborator.handleException(request, response, cause);
          } catch (Exception ex) {
              if (requestProcessor != null) {
                  throw WebAppErrorReport.constructErrorReport(ex, requestProcessor);
              } else {
                  throw WebAppErrorReport.constructErrorReport(ex, name);
              }
          }
      }
      return secObject;
  }

  @Override
  public void preInvokeCollaborators(ICollaboratorMetaData collabMetaData, EnumSet<CollaboratorInvocationEnum> colEnum) throws ServletException,
  IOException, Exception {
      // refresh dynamic collaborators before using
      getSecurityCollaborator();
      super.preInvokeCollaborators(collabMetaData, colEnum);
  }
  
  @Override
  public void postInvokeCollaborators(ICollaboratorMetaData collabMetaData, EnumSet<CollaboratorInvocationEnum> colEnum) throws ServletException,
  IOException, Exception {
   // refresh dynamic collaborators before using
      getSecurityCollaborator();
      super.postInvokeCollaborators(collabMetaData, colEnum);
  }
  
  @FFDCIgnore(ClassCastException.class)
  public static IWebAppSecurityCollaborator getCurrentSecurityCollaborator(ServletContext sc) {
      IWebAppSecurityCollaborator secCollab = null;
      ICollaboratorHelper instance = null;
      try {
          instance = ((WebApp)sc).getCollaboratorHelper();
      } catch (ClassCastException cce) {
          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
              logger.logp(Level.FINE, "CollaboratorHelperImpl", "getCurrentSecurityCollaborator", "ClassCastException on ServletContext - returning null");
          }
          //check if the security information was added during preInvoke
          return CollaboratorHelperImpl.getCurrentSecurityCollaborator();
      }
      if (instance != null)
          secCollab = instance.getSecurityCollaborator();
      return secCollab;
  }   
  
  /*
   * Returns a security collaborator for the currently active web application - can be called
   * while a request is being processed for the application.
   */
  public static IWebAppSecurityCollaborator getCurrentSecurityCollaborator()
  {
      IWebAppSecurityCollaborator currentCollab = null;
      ICollaboratorHelper instance = getCurrentInstance();
      if (instance != null)
          currentCollab = instance.getSecurityCollaborator();
      return currentCollab;
  }
  
  /*
   * Returns true/false to indicate whether there is a 'real' collaborator registered for the current application (based on the SecurityDomain 
   * specified by the application).
   * 
   */
  public static boolean getCurrentSecurityEnabled()
  {
      boolean enabled = false;
      ICollaboratorHelper instance = getCurrentInstance();
      if (instance != null)
          enabled = ((CollaboratorHelperImpl)instance).isSecurityEnabled();
      return enabled;
  }
  
  /*
   * Returns the instance of this class for the currently active web application.  Will return null if there is no active component.
   */
  private static ICollaboratorHelper getCurrentInstance()
  {
      ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
      if (cmd != null)
      {
          WebModuleMetaData wmmd = (WebModuleMetaData)cmd.getModuleMetaData();
          return ((WebAppConfiguration)wmmd.getConfiguration()).getWebApp().getCollaboratorHelper();
      }
      else
      {
          return null;
      }        
  }
 
  /*
   * Returns true/false to indicate whether there is a 'real' collaborator registered for the current application 
   * (based on the SecurityDomain specified by the application).
   * 
   */
  public boolean isSecurityEnabled()
  {
      return (CollaboratorServiceImpl.getWebAppSecurityCollaborator(securityDomainForApp) != null);
  }
  
  /*
   * Returns true/false based on the registration of a collaborator for the specified security domain
   */
 
  public static boolean isSecurityDomainEnabled(String secDomain)
  {
      return (CollaboratorServiceImpl.getWebAppSecurityCollaborator(secDomain) != null);
  }
}
