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
package com.ibm.wsspi.webcontainer;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/**
 *  
 * This class enables users to get at an instance (singleton) of the 
 * <b>WebContainer</b> so that they can delegate request processing. The
 * webcontainer returned is the single instance (per JVM) of the runtime
 * container that handles Servlets, JSP, and all registered ExtensionProcessors.
 * 
 * @ibm-private-in-use
 */
public class WebContainer extends com.ibm.websphere.servlet.container.WebContainer
{
    private volatile static WebContainer self;
    private static com.ibm.ws.webcontainer.WebContainer webcontainer;

    //Begin 277095
    private WebContainerConfig wcConfig;
    //	End 277095
    //322749
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer");
	private static final String CLASS_NAME="com.ibm.wsspi.webcontainer.WebContainer";

	protected static TraceNLS nls = TraceNLS.getTraceNLS(WebContainer.class, "com.ibm.ws.webcontainer.resources.Messages");
    /**
     * 
     */
    private WebContainer(com.ibm.ws.webcontainer.WebContainer webcontainer)
    {
        WebContainer.webcontainer = webcontainer;
        if (webcontainer == null){
        	//This should never happen. Don't bother NLSing
            logger.logp(Level.WARNING, CLASS_NAME,"WebContainer", "WebContainer has not been initialized");
        }
    }

    public String getURIEncoding()
    {
        if (webcontainer != null)  //322749
            return webcontainer.getURIEncoding();
        else //322749
            throw new RuntimeException(nls.getString(
                    "webcontainer.not.initialized",
                    "WebContainer has not been initialized."));
    }

    /**
     *
     * @return The instance of the WebContainer
     * 
     * Call this method to get at an instance of the WebContainer
     */

    public static WebContainer getWebContainer()
    {
        if (self == null)
            self = new WebContainer(com.ibm.ws.webcontainer.WebContainer.getWebContainer());

        return self;
    }

    /**
     *
     * @param req
     * @param res
     * @throws Exception
     * 
     * Call this method to force the webcontainer to handle the request. The request
     * should have enough information in it for the webcontainer to handle the request.
     */
    public void handleRequest(IRequest req, IResponse res) throws Exception
    {
        if (webcontainer != null)  //322749
            webcontainer.handleRequest(req, res);
        else //322749
            throw new RuntimeException(nls.getString(
                    "webcontainer.not.initialized",
                    "WebContainer has not been initialized."));
    }
    /**
     *
     * @param fac The factory that provides ExtensionProcessors which the webcontainer
     * will leverage to handle requests.
     * 
     * Register an extension factory with this webcontainer
     * @see com.ibm.wsspi.webcontainer.extension.ExtensionFactory
     */
    public static void registerExtensionFactory(ExtensionFactory fac)
    {
        webcontainer.addExtensionFactory(fac);
    }

    /**
     *
     * @return The additional/custom properties configured for this webcontainer
     */
    public static Properties getWebContainerProperties() 
    {
        return com.ibm.ws.webcontainer.WebContainer.getWebContainerProperties();
    }

    //	Begin 277095	
    public WebContainerConfig getWebContainerConfig (){
        //Begin 284318
        if (webcontainer==null)
            return null;
        // End 284318
        return(WebContainerConfig) (webcontainer.getWebContainerConfig());
    }
    //	End 277095

    public boolean isCollaboratorEnabled(String type){
    	Properties props = getWebContainerProperties();
    	return Boolean.valueOf(props.getProperty("com.ibm.wsspi.webcontainer.enablecollab", "true")).booleanValue();
    }

    public void setServletCachingInitNeeded (boolean bool){
    	com.ibm.ws.webcontainer.WebContainer.setServletCachingInitNeeded(bool);
    }
    
    public boolean isCachingEnabled() {
    	return webcontainer.isCachingEnabled();
    }
    
	
	public IPlatformHelper getPlatformHelper(){
		return webcontainer.getPlatformHelper();
	}
	
	public Integer getKeySize(String cipherSuite) {
		return webcontainer.getKeySize(cipherSuite);
	}

	public ClassLoader getExtClassLoader() {
		if (webcontainer==null){
			return null;
		}
		else {
			return webcontainer.getExtClassLoader();
		}
	}

	public static TraceNLS getNls() {
		return nls;
	}
	
	//582053
	public synchronized void setWebContainerStopping(boolean isStopped){	    	
	    webcontainer.setWebContainerStopping(isStopped);
	}

	public void decrementNumRequests() {
		webcontainer.decrementNumRequests();
	}
	

}
