/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.sip.URI;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.servlet.session.IBMApplicationSession;
import com.ibm.websphere.sip.IBMSipSession;
import com.ibm.websphere.sip.WSApplicationSession;
import com.ibm.ws.sip.container.converged.servlet.session.ConvergedAppUtils;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;

/**
 * Extends Base SipApplicationSessionImpl to support converged Application, (HTTP and SIP)
 * @author dror
 *
 */
public class WASXSipApplicationSessionImpl extends SipApplicationSessionImpl implements IBMApplicationSession, WSApplicationSession {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
     * Class Logger.
     */
    private static final transient LogMgr c_logger = Log.get(WASXSipApplicationSessionImpl.class);
    
	 /**
     * For converge App support, the HttpSession IDs list
     */
    private Set<String> m_httpSessions;
    
    /**
	 * Default CTor
	 */
	public WASXSipApplicationSessionImpl() {
		super();
	}
	
	/**
	 * @param id Unique Application Session Identifier
	 */
	public WASXSipApplicationSessionImpl(String id) {
		super(id);
	}

	 /**
     * Destroy all related sessions
     */
    private void destoryHttpSession(){
//    	invalidate all http session
        if(m_httpSessions!=null){
	        
	        
	        String vhost=null;
	        String uri = null;
	        //Invalidate all sessions belonging to this application session.
	        synchronized (m_httpSessions){
	        	Iterator<String> iterHttpSessions = m_httpSessions.iterator();
		        while (iterHttpSessions.hasNext()) {
		        	if(vhost==null){
		        		vhost= getAppDescriptor().getVirtualHostName();
		    	        uri = getAppDescriptor().getRootURI();
		    	        if(c_logger.isTraceDebugEnabled()){
			            	c_logger.traceDebug(this,"destoryHttpSession","getting vhost["+vhost+"],uri["+uri+"]");
			            }
		        	}
		            String id = iterHttpSessions.next();
	
		            if(c_logger.isTraceDebugEnabled()){
		            	c_logger.traceDebug(this,"destoryHttpSession","getting httpSession["+id+"]");
		            }
		            HttpSession httpSession = ConvergedAppUtils.getHttpSessionById(vhost,uri,id);
		            if(httpSession!=null){
		            	if(c_logger.isTraceDebugEnabled()){
			            	c_logger.traceDebug(this,"destoryHttpSession","invalidating httpSession["+id+"]");
			            }
		            	httpSession.invalidate();
		            }
		            
		            iterHttpSessions.remove();
		        }
	        }
        }
    }

    /**
     * @see javax.servlet.sip.SipApplicationSession#invalidate()
     */
    public void invalidate() {
		synchronized (getSynchronizer()) {
			destoryHttpSession();
			super.invalidate();
		}
    }

	/**
	 * @see javax.servlet.sip.SipApplicationSession#getSessions(java.lang.String)
	 */
	public Iterator getSessions(String protocol) {
		if (protocol.equalsIgnoreCase("HTTP")){
        	if(m_httpSessions == null){
        		return Collections.EMPTY_SET.iterator();
        	}
            
        	return getAllHttpSessions().iterator();
        }
		return super.getSessions(protocol);
	}
	
	/**
	 *  @see javax.servlet.sip.SipApplicationSession#getSession(java.lang.String, javax.servlet.sip.SipApplicationSession.Protocol)
	 */
	public Object getSession(String id, Protocol protocol) throws NullPointerException, IllegalStateException {
		if (c_logger.isTraceEntryExitEnabled()) {
  			Object[] params = { id, protocol };
  			c_logger.traceEntry(this, " getSession", params);
  		}
		if (protocol == Protocol.HTTP){
        	if(m_httpSessions == null){
        		if(c_logger.isTraceDebugEnabled()){
	            	c_logger.traceDebug(this,"getSession", " m_httpSessions is null");
	            }
        		return Collections.EMPTY_SET.iterator();
        	}
        	String vhost=null;
	        String uri = null;
        	vhost= getAppDescriptor().getVirtualHostName();
	        uri = getAppDescriptor().getRootURI();
	        if(c_logger.isTraceDebugEnabled()){
            	c_logger.traceDebug(this,"getSession","getting vhost["+vhost+"],uri["+uri+"]");
            }
	         for (Iterator<String> iter = m_httpSessions.iterator(); iter.hasNext();) {
				String sessionId = (String) iter.next();
				 if(sessionId.equalsIgnoreCase(id)){
					Object httpSession = ConvergedAppUtils.getHttpSessionById(vhost,uri,id);
					if(c_logger.isTraceDebugEnabled()){
		            	c_logger.traceDebug(this,"getSession", "Requested HTTP session found = " + httpSession);
		            }
					return httpSession;
				}
			}
		}
		return super.getSession(id,protocol);
	}
	
	/**
	 * @see com.ibm.wsspi.servlet.session.IBMApplicationSession#encodeURI(java.lang.Object)
	 */
	public void encodeURI(Object arg0) {
		super.encodeURI((URI)arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.servlet.session.IBMApplicationSession#encodeURI(java.lang.String)
	 */
	public String encodeURI(String uri) {
		StringBuffer buf = new StringBuffer(uri);
		int startPos  = Integer.MAX_VALUE;
		int qmPos = buf.indexOf("?"); //session id must be inserted before any appearance of ? or #.
		if( qmPos > -1){
			startPos = qmPos;
		}
		int pPos = buf.indexOf("#");
		if( pPos > -1){
			startPos = Math.min(startPos, pPos);
		}
		
		StringBuffer buf2 = new StringBuffer();
		buf2.append(";");
		buf2.append(ENCODED_APP_SESSION_ID);
		buf2.append("=");
		buf2.append(getId());
		if( startPos == Integer.MAX_VALUE ){
			buf.append(buf2);// no special chars found, append at the end of the uri
		}else{
			buf.insert(startPos, buf2);
		}
		return buf.toString();
	}
	
	    
	 /**
     * add http session
     * @param httpSession
     */
    public void addHttpSession(HttpSession httpSession){
    	if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry("WASXSipApplicationSessionImpl", "addHttpSession ",new Object[]{httpSession.getId(),httpSession});
        }
    	if(httpSession!=null){
    		createHttpSessionsSetIfNeeded();
    		m_httpSessions.add(httpSession.getId());
    		//set expire of http session  -1 (never expired), it should invalidate once ApplicationSession is expired/invalidated
//    		httpSession.setMaxInactiveInterval(-1);
//    		if (c_logger.isTraceDebugEnabled()) {
//                c_logger.traceDebug("WASXSipApplicationSessionImpl", "addHttpSession"," sessionId["+httpSession.getId()+"] added, set its experation to -1");
//            }
    		//Moti: OG:
    		SessionRepository.getInstance().put(this);
    	}
    	if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit("WASXSipApplicationSessionImpl", "addHttpSession ");
        }
    }
    
    /**
     * Creating a m_httpSessions in a thread safe manner 
     */
    private void createHttpSessionsSetIfNeeded(){
    	if(m_httpSessions==null){
    		synchronized (this) {
	    		if(m_httpSessions==null){
	    			m_httpSessions = Collections.synchronizedSet(new HashSet<String>(1));
	    		}
    		}
		}
    }
    /**
     * remove http session
     * @param id
     */
    public void removeHttpSession(String id){
    	if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry("WASXSipApplicationSessionImpl", "removeHttpSession ",new Object[]{id});
        }
    	synchronized (m_httpSessions) {
    		if((id!=null)&&(m_httpSessions!=null)){
	    		m_httpSessions.remove(id);
	    		setLastAccessedTime();
	    		store();
	    	}
    	}
    	if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry("WASXSipApplicationSessionImpl", "removeHttpSession ",new Object[]{id});
        }
    }

    /**
     * 
     * @return a list of all associated http sessions
     */
    protected List getAllHttpSessions()
    {
    	List <HttpSession> result = Collections.EMPTY_LIST;
    	synchronized (m_httpSessions) {
    		if (m_httpSessions.isEmpty()) {
    			return result;
    		}
    		String vhost=(String) getAppDescriptor().getVirtualHostName();;
    		String uri = getAppDescriptor().getRootURI();
    		if(c_logger.isTraceDebugEnabled()){
    			c_logger.traceDebug(this,"getAllHttpSessions","getting vhost["+vhost+"],uri["+uri+"]");
    		}

    		result = new ArrayList<HttpSession>(m_httpSessions.size());
    		Iterator<String> iter = m_httpSessions.iterator();
    		//  Activate all htppSession related to that sipApplication session

    		for (; iter.hasNext();) {

    			String id = (String) iter.next();
    			if(c_logger.isTraceDebugEnabled()){
    				c_logger.traceDebug(this,"getAllHttpSessions","getting httpSession["+id+"]");
    			}
    			HttpSession httpSession = ConvergedAppUtils.getHttpSessionById(vhost,uri,id);
    			if (c_logger.isTraceDebugEnabled() && (id == null || httpSession == null)) {
    				if(c_logger.isTraceDebugEnabled()){
        				c_logger.traceDebug(this,"getAllHttpSessions",
        						"mismatch between http sessions in web container and SIP container. check flow." );
        			}
    			}
    			// getHttpSessionById() returns a non-null session if it is still valid, so if the httpSession is null we should not add it to the list
    			if (httpSession != null) {
    				result.add(httpSession);
    			}
    		}
    		if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "getAllHttpSessions", "found sessions. count:"+result.size());
            } 
    		return result;
    	}
    }

    
	/**
	 * @return an iterator for all the SIP and HTTP sessions (if it is converged application).
	 */
	public Iterator getSessions() {
		if (m_httpSessions == null || m_httpSessions.isEmpty()) {
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(this,"getSessions","no HTTP sessions. will give only SIP sessions.");
			}
			return super.getSessions();
		}
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug(this,"getSessions","getting SIP and HTTP sessions");
		}
		List<IBMSipSession> combineHttpAndSIP = getAllSIPSessions();
		if (combineHttpAndSIP.isEmpty()) {
			if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(this,"getSessions","SIP session list is empty. returning only HTTP.");
			}
			// here there is a catch (defect 447184) if getAllSIPSessions
			// returns a Collections.EMPTY_LIST - it is a muteable Java object.
			// therefore , we need to create one if we want to add anything.
			return getAllHttpSessions().iterator();
		}
		combineHttpAndSIP.addAll(getAllHttpSessions());
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug(this,"getSessions","found total of "
					+combineHttpAndSIP.size() +" sessions.");
		}
		return combineHttpAndSIP.iterator();
	}
	
	/**
	 * 
	 * @see com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl#encodeURL(java.net.URL)
	 */
	public URL encodeURL(URL url) throws IllegalStateException{
		super.encodeURL(url);
		try {
			return new URL( encodeURI(url.toString()));
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}
    

	/**
	 * @see com.ibm.websphere.sip.WSApplicationSession#createEPR(java.lang.String)
	 */
	public W3CEndpointReference createEPR(String appSessionId) throws Exception {
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "createEPR", appSessionId);
		}
	    
	    final String namespace = new String("http://impl.webservice.commsvc.ws.ibm.com/");
	    final QName portQName = new QName(namespace,"ControllerPort");
	    final QName serviceQName = new QName(namespace,"ControllerService");

	    W3CEndpointReference epr = createEPR(appSessionId, portQName, serviceQName, null);
	    
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "createEPR");
		}

	    return epr;
	}

	/**
	 * @see com.ibm.websphere.sip.WSApplicationSession#createEPR(...)
	 */
	public W3CEndpointReference createEPR(String appSessionId, 
										  QName portQName, 
										  QName serviceQName,
										  WebServiceContext context) throws Exception {
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "createEPR", appSessionId);
		}

		
		
//		TODO Liberty Anat - remove this code - should be implemeted correctly

		
		W3CEndpointReferenceBuilder w3cBuilder=new W3CEndpointReferenceBuilder();
		w3cBuilder.serviceName(serviceQName);
		w3cBuilder.endpointName(portQName);
		return w3cBuilder.build();
		
//		TODO Liberty Anat - remove remark and implement EPR
//		if (c_logger.isTraceEntryExitEnabled()) {
//			c_logger.traceEntry(null, "createEPR", appSessionId);
//		}
//	    
//	    com.ibm.ws.wsaddressing.EndpointReference epr = 
//			(com.ibm.ws.wsaddressing.EndpointReference)com.ibm.wsspi.wsaddressing.EndpointReferenceManager.createEndpointReference(serviceQName, portQName.getLocalPart());
//	    epr.setReferenceParameter(portQName, appSessionId);
//	    
//	    if (context != null) {
//		    // Update the address so it points to the cluster and port (from the request URI)
//			// Access the underlying HTTP request object that made this web service request.
//			MessageContext messageContext = context.getMessageContext();
//			HttpServletRequest httpRequest = (HttpServletRequest)messageContext.get(MessageContext.SERVLET_REQUEST);
//			StringBuffer sbURL = httpRequest.getRequestURL();
//			if (c_logger.isTraceDebugEnabled()) {
//				c_logger.traceDebug("context requestURL = [" + sbURL + "]");
//				c_logger.traceDebug("epr default address = [" + epr.getAddress());
//			}
//		    AttributedURI attributedUri = epr.getAddress();
//		    attributedUri.setURI(new java.net.URI(sbURL.toString()));
//		    epr.setAddress(attributedUri);
//	    }
//	    
//		// 
//		// If we are in a cluster, then we need to set the cluster id such that the proxy
//		// will recogonize any subsequent SOAP requests to route correctly to the same
//		// server that origionated the session.
//		//
//		String clusterName = SipClusterUtil.getClusterName();
//		if (clusterName != null) {
//			Identity clusterId = IdentityMapping.getClusterIdentityFromClusterName(getCellName(),clusterName);
//			if (c_logger.isTraceDebugEnabled()) {
//				c_logger.traceDebug("clusterId = [" + clusterId + "]");
//			}
//			final Identity scaIdentity = IdentityMapping.generateSCAAttributeIdentity(clusterId, appSessionId);
//			HAResource har = new HAResource() {
//				public Identity getAffinityKey() {
//					return scaIdentity;
//				}
//			};
//			
//			if (c_logger.isTraceDebugEnabled()) {
//				c_logger.traceDebug("setting Affinity in EPR");
//			}			
//			epr.setAffinity(har);
//		}
//		if (c_logger.isTraceDebugEnabled()) {
//			c_logger.traceDebug("converting from IBM proprietary format to W3C format and returning it.");
//		}
//		
//		if (c_logger.isTraceEntryExitEnabled()) {
//			c_logger.traceExit(null, "createEPR");
//		}
//		return com.ibm.websphere.wsaddressing.jaxws21.EndpointReferenceConverter.createW3CEndpointReference(epr);
//	}

//		End - TODO Liberty Anat - remove remark and implement EPR
	}

		/*
		 * Internal version to get the cluster name from the server.xml file.  This HAS TO BE have a class 
		 * reference to call WsServiceRegistry.getService.  This will return null if the app server is not in a cluster.
		 * 
		 * @return the clusterName of the server that CEA is running in.
		 * @throws Exception
		 */
//	  private String getClusterName() throws Exception{
//		   String clusterName = null;
//		   if (c_logger.isTraceEntryExitEnabled()) {
//				c_logger.traceEntry(null, "getClusterNameNonStatic");
//			}
//		   // Iterate through server.xml, find server object, and determine if it is a cluster member or
//		   // not and set scope accordingly.
//		   ConfigService configService = (ConfigService)WsServiceRegistry.getService( this, ConfigService.class);
//		   List list = configService.getDocumentObjects(configService.getScope(ConfigScope.SERVER), "server.xml");
//		   ConfigObject configObject = (ConfigObject)list.get(0);
//		   clusterName = configObject.getString(CT_Server.CLUSTERNAME_NAME, CT_Server.CLUSTERNAME_DEFAULT);
//		   if (clusterName == null || clusterName == "") {
//			   // Make sure its set to null and not the null string.
//			   clusterName = null;
//			   if (c_logger.isTraceDebugEnabled()) {
//					c_logger.traceDebug("CEA APP NOT running in a cluster.");
//				}		
//		   } 
//		   else {
//			   if (c_logger.isTraceDebugEnabled()) {
//					c_logger.traceDebug("CEA APP is running in a cluster [" + clusterName + "]");
//				}
//		   }
//		   if (c_logger.isTraceEntryExitEnabled()) {
//				c_logger.traceExit(null, "getClusterNameNonStatic");
//			}
//		   return clusterName;
//	   }
	  
	//TODO liberty Anat: Look how to get cellName in Liberty
//	  /**
//		 * Method used to get the cell name from the AdminService file.  
//		 * 
//		 * @return the cell name of the server that CEA is running in.
//		 */
//		public static String getCellName() {
//			
//			if (cellName == null) {
//				/*TODO Liberty - also remove the filter in findbugs.exclude.xml ( MS_SHOULD_BE_FINAL )*/
//				Adminservice as = AdminServiceFactory.getAdminService();
//				cellName = as.getCellName();
//				if (c_logger.isTraceDebugEnabled()) {
//					c_logger.traceDebug("cellName = [" + cellName + "]");
//			 	}			
//			}
//			return cellName;
//		}
	   
		/**
		 * @see com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl#isReadyToInvalidate()
		 */
		public boolean isReadyToInvalidate() throws IllegalStateException {
			if (c_logger.isTraceEntryExitEnabled()) {
	            c_logger.traceEntry("WASXSipApplicationSessionImpl", "isReadyToInvalidate ");
	        }
			boolean result = m_httpSessions == null || m_httpSessions.isEmpty();
			if(result){
				result = super.isReadyToInvalidate();
			}
			else{
				if (c_logger.isTraceDebugEnabled()) {
		            c_logger.traceDebug("WASXSipApplicationSessionImpl.isReadyToInvalidate there are still valid HTTP sessions, SAS is not ready to be invalidated");
		        }
			}
			
			if (c_logger.isTraceEntryExitEnabled()) {
	            c_logger.traceExit("WASXSipApplicationSessionImpl", "isReadyToInvalidate result=" + result);
	        }
			return result;
		}

		@Override
		public void sync() {
			// TODO Auto-generated method stub
			
		}
}
