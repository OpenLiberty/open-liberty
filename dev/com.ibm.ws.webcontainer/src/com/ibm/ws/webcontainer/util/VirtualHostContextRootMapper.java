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
package com.ibm.ws.webcontainer.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.ejs.ras.TraceNLS;

import com.ibm.ws.webcontainer.core.Request;
import com.ibm.ws.webcontainer.core.RequestMapper;
import com.ibm.ws.webcontainer.webapp.WebGroup;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.util.URIMapper;

@SuppressWarnings("unchecked")
public final class VirtualHostContextRootMapper extends VirtualHostMapper implements RequestMapper
{   

    private static Map cachedMappers = Collections.synchronizedMap(new HashMap()); //PK62387
    
    protected static TraceNLS nls = TraceNLS.getTraceNLS(VirtualHostContextRootMapper.class, "com.ibm.ws.webcontainer.resources.Messages");
    
 
	protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.util");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.util.VirtualHostContextRootMapper";
    
    
    public RequestProcessor map(String matchString)
    {
        //TODO why isn't there a map(String vhost, String path) to avoid
        // the string concat in handleRequest and the immediate breakup here?

        // find delimeter between virtualHost ( <HOST>:<PORT>) and URI 
        int slashIndex = matchString.indexOf('/');
        if(slashIndex != -1){
            String vHost = matchString.substring(0, slashIndex);
            String relativePath = matchString.substring(slashIndex);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	                logger.logp(Level.FINE, CLASS_NAME,"map", " vHost [" + vHost +"] relativePath [" + relativePath +"]");
	        }
            //PK62387 Start
            RequestProcessor virtualHostProcessor = (RequestProcessor)cachedMappers.get(vHost);
            if (virtualHostProcessor == null) {
                virtualHostProcessor = (RequestProcessor) super.getMapping(vHost); // first find the appropriate VHost
            }
            //RequestProcessor virtualHostProcessor = (RequestProcessor) super.getMapping(vHost);
            //PK62387 End
            if (virtualHostProcessor==null){
            	//help out the use to find the correct vhost.
            	
        		Iterator targetMappings=super.targetMappings();
        	
         		
            	while (targetMappings.hasNext()){
            		Object vhostProcessorCandidate = targetMappings.next();
            		 if (vhostProcessorCandidate instanceof InternalURIMapper){
     	                RequestProcessor reqProc = ((URIMapper)vhostProcessorCandidate).map(relativePath);
     	                if (reqProc!=null){
     	                	WebGroup webGroup = (WebGroup) reqProc;
     	                	if (!webGroup.getName().equals("/*")){
     	                		logger.logp(Level.WARNING, CLASS_NAME,"map", 
     	                				nls.getFormattedMessage("request.matches.context.root",
     	                				new Object [] {webGroup.getName(),((InternalURIMapper)vhostProcessorCandidate).getVhostKey()},
     	                				"Request matches the context root["+webGroup.getName() 
     	                				+"] under the virtual host alias of["+((InternalURIMapper)vhostProcessorCandidate).getVhostKey()+"]."));
     	                		logger.logp(Level.WARNING, CLASS_NAME,"map", 
     	                				nls.getFormattedMessage("need.to.add.a.new.virtual.host.alias",
     	                				new Object [] {((InternalURIMapper)vhostProcessorCandidate).getVhostKey()},
     	                				"You may need to add a new virtual host alias of *:<your port> to the same virtual host ["
     	                				+((InternalURIMapper)vhostProcessorCandidate).getVhostKey()+"] is under."));
     	                		break;
     	                	} else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
	     	                		logger.logp(Level.FINE, CLASS_NAME,"map", "Request matches the context root["+webGroup.getName() 
	     	                				+"] under the virtual host alias of["+((InternalURIMapper)vhostProcessorCandidate).getVhostKey()+"].");
	     	                		logger.logp(Level.FINE, CLASS_NAME,"map", "You may need to add a new virtual host alias of *:<your port> to the same virtual host ["+((InternalURIMapper)vhostProcessorCandidate).getVhostKey()+"] is under.");
     	                	}
     	                }
     	            }
            	}
            	
            }
            else if (virtualHostProcessor instanceof InternalURIMapper){
                // if found and instanceof URIMapper then map the relativePath to find the WebGroup.
	                cachedMappers.put(vHost, virtualHostProcessor); //PK62387
	                return ((URIMapper)virtualHostProcessor).map(relativePath); 
	        }
            return virtualHostProcessor;
        }
        else{
           if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	                logger.logp(Level.FINE, CLASS_NAME,"map", " matchString [" + matchString +"]");  // fall back call.
	            }
            return (RequestProcessor)super.getMapping(matchString);
        }
    }

    /**
     * @see com.ibm.ws.core.RequestMapper#map(IWCCRequest)
     */
    public RequestProcessor map(Request req)
    {
        return null;
    }
    
    public void addMapping(String vHostKey, String contextRoot, RequestProcessor webGroup) throws Exception {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"addMapping", " vHost [" + vHostKey +"] relativePath [" + contextRoot +"]");
        }
        
        //PK65158
        RequestProcessor virtualHostProcessor = (RequestProcessor) findExactMatch(vHostKey);

        
        if(virtualHostProcessor == null){ // vhostKey does not exist in map.  Create an InternalURIMapper to add this contextRoot
            virtualHostProcessor = new InternalURIMapper(vHostKey, true); //586180
            ((InternalURIMapper)virtualHostProcessor).addMapping(contextRoot, webGroup);
	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	                logger.logp(Level.FINE, CLASS_NAME,"addMapping", "new virtual host processor vHost [" + vHostKey +"]");
            }
        }
        else if (virtualHostProcessor instanceof InternalURIMapper){    // already added this vhostKey. Add this context root to the mapper.
            ((InternalURIMapper)virtualHostProcessor).addMapping(contextRoot, webGroup);
	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	                logger.logp(Level.FINE, CLASS_NAME,"addMapping", "already existing virtual host processor vHost [" + vHostKey +"]");
            }

        }
        super.addMapping(vHostKey, virtualHostProcessor);   //update or add this vHostKey with modified VirtualHostProcessor.
    }
    
    public void addMapping (String vHostKey, Object target){    // used for VirtualHostExtensionProcessor.
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"addMapping", " vHost [" + vHostKey +"] target [" + target +"]");
        }
        super.addMapping(vHostKey, target);
    }

    public void removeMapping(String vHostKey, String contextRoot) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"removeMapping", " vHost [" + vHostKey +"] relativePath [" + contextRoot +"]");
        }
        RequestProcessor virtualHostProcessor = (RequestProcessor) super.findExactMatch(vHostKey);
        if (virtualHostProcessor instanceof InternalURIMapper){
            InternalURIMapper uriMapper = ((InternalURIMapper)virtualHostProcessor);
            uriMapper.removeMapping(contextRoot);
	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	                logger.logp(Level.FINE, CLASS_NAME,"removeMapping", "removing context [" + contextRoot +"]");
            }
//            if(uriMapper.targetMappings().hasNext() == false){  // is empty
//                super.removeMapping(vHostKey);   // remove this vHost from the VirtualHostContextRootMapper
//	                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
//	                    logger.logp(Level.FINE, CLASS_NAME,"removeMapping", "removing vhost [" + vHostKey +"]");

//	                }
//            }
        }
        
    }

    public void removeMapping (String matchString){
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"removeMapping", " matchString [" + matchString +"]");
        }
        super.removeMapping(matchString);
    }

    
    private final class InternalURIMapper extends URIMapper implements RequestProcessor{
    	private String vhostKey;

    	//586180
    	public InternalURIMapper(String vhostKey, boolean scalable){
    		super(scalable);
    		this.vhostKey=vhostKey;
    	}

		public InternalURIMapper (String vhostKey){
			super();
    		this.vhostKey=vhostKey;
    	}
    	
        public void handleRequest(ServletRequest req, ServletResponse res)
                throws Exception {
            throw new RuntimeException ("InternalURIMapper is not capable of handling requests");   // will never happen but just in case.
        }

		public String getVhostKey() {
			return vhostKey;
		}

		public String getName() {
			return "InternalURIMapper";
		}

		public boolean isInternal() {
			return true;
		}
    }
    
    public static void main (String args[]){
        String matchString = "localhost:9080/hello/there";
        int slashIndex = matchString.indexOf('/');
        String vHost = matchString.substring(0, slashIndex);
        String relativePath = matchString.substring(slashIndex);
        System.out.println("vHost -->"+ vHost +" relativePath -->" + relativePath);
        
        VirtualHostMapper myVirtualHostMapper = new VirtualHostMapper();
        try{
            myVirtualHostMapper.addMapping("*:9080", new java.lang.Object() );
        }catch (Exception e){
        	
        }
        if(myVirtualHostMapper.exactMatchExists("todd:9080")){
            System.out.println("match was found");
        }
        else{
            System.out.println("no match was found");
        }
        
        if(myVirtualHostMapper.exactMatchExists("*:9080")){
            System.out.println("match was found");
        }
        else{
            System.out.println("no match was found");
        }

        
    }
    
}
