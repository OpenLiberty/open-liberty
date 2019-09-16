/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.sipAppAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.xml.sax.SAXException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipXMLParser;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * The class is used to install sip applications - 
 * we use the webAppadapter to parse the web.xml and add the original WebApp into the SipAppDesc.
 * @author SAGIA
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, configurationPid = "com.ibm.ws.sip.sipAppAdapter.SipAppAdapter", service = ContainerAdapter.class, property = { "service.vendor=IBM","toType=com.ibm.ws.javaee.dd.web.WebApp","service.ranking:Integer=1" })
public class SipAppAdapter implements ContainerAdapter<WebApp>{




	private static final TraceComponent tc = Tr.register(SipAppAdapter.class);

	
	
	/**
	 * a collection of all the application that are currently being installed (per thread).
	 * we use this list to know that we are in the recursive call during the parsing and not a dffirent call  that was initalized.
	 */
	private ThreadLocal<Collection<Container>> _currentlyProcessing = null;

	/**
	 * DS method to activate this component.
	 * 
	 * @param	context 	: Component context of the component 
	 * @param 	properties 	: Map containing service & config properties
	 *            populated/provided by config admin
	 */
	protected void activate(ComponentContext context, Map<String, Object> properties) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "SipAppAdapter activated", properties);
		
		 _currentlyProcessing = new ThreadLocal<Collection<Container>>() {
		        protected Collection<Container> initialValue() {
		            return new ArrayList<Container>(1);
		        }
		    };

	}


	/**
	 * DS method to deactivate this component.
	 * 
	 * @param reason int representation of reason the component is stopping
	 */
	protected void deactivate(int reason) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
			Tr.event(tc, "SipAppAdapter deactivated, reason=" + reason);

	}

	/**
	 * create a WebApp using the webContainer and save a reference to the sip.xml if it exists.
	 * the function use a recursive call to adapt and return null at the second entry to use the adaption mechanism
	 * the adapt call will be passed to the next lower priority adapter that will parse the web.xml and return the WebApp object.
	 * We need to identify that we are in the recursive call from the same thread and for that we use a thread local collation of containers.
	 * if the container is in the list we are in the recursive call otherwise we are not.
	 * @see com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter#adapt(com.ibm.wsspi.adaptable.module.Container, com.ibm.wsspi.artifact.overlay.OverlayContainer, com.ibm.wsspi.artifact.ArtifactContainer, com.ibm.wsspi.adaptable.module.Container)
	 */
	@Override
	public WebApp adapt(Container root, OverlayContainer rootOverlay,
			ArtifactContainer artifactContainer, Container containerToAdapt)
			throws UnableToAdaptException {

		//making sure that the application wasn't installed already.
		SipAppDesc appDesc = (SipAppDesc) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), SipAppDesc.class);
        if (appDesc != null) {
            return appDesc;
        }
		//making sure that the application wasn't installed already.
		 WebApp webApp = (WebApp) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), WebApp.class);
        if (webApp != null) {
            return webApp;
        }
        final Collection<Container> inFlight = _currentlyProcessing.get();
        if (inFlight.contains(containerToAdapt)) {
        	
            return null;
        } else try {
            inFlight.add(containerToAdapt);
	         //call adapt again to get back the real WebApp
	          webApp = containerToAdapt.adapt(WebApp.class);

	          //start parsing the sip.xml
	  		ArtifactEntry sipXMLEntry = artifactContainer.getEntry("/WEB-INF/sip.xml");

	  		InputStream xmlStream = null;
	  		if(sipXMLEntry !=null) {
	  		  try {
	  			  xmlStream =  sipXMLEntry.getInputStream() ;

	  				if( xmlStream != null){
	  					 appDesc = loadDataFromSipXml(xmlStream);

	  					if(appDesc != null) {
	  						// update flag for proprietary context-param in sip.xml
	  						appDesc.updateShouldExcludeFromApplicationRouting();
	  						
	  						if(webApp != null) {
	  							appDesc.setddWebApp(webApp);
	  							appDesc.mergeWebApptoSip();
	  						}
	  						 rootOverlay.addToNonPersistentCache(artifactContainer.getPath(), SipAppDesc.class, appDesc);
	  						return appDesc;
	  					
	  					}

	  				}

	  				
	  		} catch (IOException e) {
	  			
	  			return webApp;
	  		}
	  		  finally{
	  			  if(xmlStream != null) {
	  				  try {
	  					xmlStream.close();
	  				} catch (IOException e) {
	  					
	  				}
	  			  }
	  		  }
	  		
	  		}  

            return webApp;
        } finally {
            inFlight.remove(containerToAdapt);
        }

	}
	


	/**
	 * The function create a parser for the sip.xml input stream and parse the data.
	 * @param sipXML the input stream of the sip.xml file
	 * @return sipAppDesc representing the application meta data from the sip.xml
	 */
	private SipAppDesc loadDataFromSipXml( InputStream sipXML) {
		

		SipAppDesc sipApp = null;
		try {
		 	SipXMLParser parser = new SipXMLParser();
			sipApp = parser.parse(sipXML);
		} catch (SAXException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc,  "loadDataFromSipXml",       
		                    ", failed reading sip.xml=" , e);
			}
		} catch (IOException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc,  "loadDataFromSipXml", 
						", failed reading sip.xml=" , e);
			}
		} catch (ParserConfigurationException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc,  "loadDataFromSipXml",           
		                ", failed reading sip.xml=" , e);
			}
		}




		if (TraceComponent.isAnyTracingEnabled()&& tc.isDebugEnabled()) {
            
            StringBuilder buffer = new StringBuilder();
            buffer.append("SIP Application: ");
	        buffer.append(sipApp.getApplicationName());
		    buffer.append(" ,Root URI: ");
	        buffer.append(sipApp.getRootURI());
	        Tr.debug(tc,  "loadSipXml", buffer.toString());                                                             
		    }
	
	    
	        return sipApp;
	    }
	
	

	
}
