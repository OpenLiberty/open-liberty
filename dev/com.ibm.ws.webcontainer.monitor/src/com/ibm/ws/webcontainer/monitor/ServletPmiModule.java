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
package com.ibm.ws.webcontainer.monitor;

import java.util.HashMap;

import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.ws.pmi.stat.CountStatisticImpl;
import com.ibm.ws.pmi.stat.RangeStatisticImpl;
import com.ibm.ws.pmi.stat.TimeStatisticImpl;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.wsspi.pmi.factory.StatisticActionListener;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.factory.StatsInstance;
import com.ibm.wsspi.pmi.stat.SPIStatistic;
 // ============= Inner class =============
 //  PMI data for each servlet module
 class ServletPmiModule implements StatisticActionListener,PmiConstants
 {
     // Using Impl classes instead of SPI to avoid the interface overhead
     // since this class is owned by PMI -- "perfOptimization".    
     private static final long serialVersionUID = 3692271975902012581L;
     CountStatisticImpl numErrors = null;        
     CountStatisticImpl totalRequests = null;
     RangeStatisticImpl currentRequests = null; // note this counter may be inaccurate since we don't track it when level is none.
     TimeStatisticImpl responseTime = null;
     //private static final String uriTemplate = "/com/ibm/wsspi/webcontainer/pmi/urlStats.xml";
     String subinstanceName = "";

     private StatsGroup uriStatsGroup = null;
     
     private StatsInstance servletStatsInstance = null;
     private HashMap uriData = null;
     
     private String webAppFullName = null;
     private String servletName = null;
     
     // PK64697 Start
     private static final String URL_MODULE="webAppModule.servlets.urls";
     private static final boolean alwaysCreateURLObjects = (Boolean.valueOf(WebContainer.getWebContainerProperties().
    		                          getProperty("com.ibm.ws.webcontainer.runtimeuristatenablement"))).booleanValue();
	// private static final PmiCollaborator pmiCollab = PmiCollaboratorFactory.getPmiCollaborator(); 
    // private StatDescriptor urlStatDesc = null;
     TimeStatisticImpl asyncContextResponseTime; 
     // PK64697 End
     
     public ServletPmiModule (String webAppFullName, String servletName, StatsInstance statsInstance, StatsGroup servletStatsGroup)
     {
     	this.webAppFullName = webAppFullName;
     	this.servletName = servletName;
         
         try
	        {

	            // create a Stats instance under the Stats group that we created above
	            // passing 'null' since there is no MBean for this instance 
	            // Typically, the Stats instance WILL have an MBean.
	            // If you have an MBean for this instance then pass that ObjectName
	            // NOTE: Associating an MBean is OPTIONAL. The statistics can be accessed without an MBean.             
	            servletStatsInstance = StatsFactory.createStatsInstance (servletName, servletStatsGroup, null, this);
	           // createUriStatsGroup();

	            // PK64697 Start
	            // urlStatDesc is used to determne if url stats are enabled for this servlet.
            	/*if (!alwaysCreateURLObjects)
	            {	            	
	            	urlStatDesc = new StatDescriptor(new String[]{WebAppModule.WEBAPP_MODULE,this.webAppFullName,WebAppModule.SERVLET_MODULE,this.servletName,URL_MODULE});
	            }*/
            	// PK64697 End
	            	            
	        }
	        catch (StatsFactoryException sfe)
	        {            
	            //logger.logp(Level.SEVERE, CLASS_NAME,"ServletPmiModule", "error.creating.stats.instance", new Object [] {servletName,servletStatsGroup,sfe});    //@283348.1
	        }
	       
     }
     
     /**
   	 * 
   	 */
/*   	private void createUriStatsGroup() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"createUriStatsGroup", "creating UriStatsGroup for the servlet:"+servletName);
        
           // if there is no stats group create one
           if (uriStatsGroup == null)
           {
               try {
   				// create a Stats group for my component
   				// passing 'null' since there is no MBean for this "group" 
   				// ** Typically, the Stats group (PMI logical group) will NOT have any MBean **
   				// If you have an MBean for this "group" then pass that ObjectName
   				// "myStats.Group" is translated in myStats.nlsprops                
   				// NOTE: Associating an MBean is OPTIONAL. The statistics can be accessed without an MBean.
            	uriStatsGroup = StatsFactory.createStatsGroup (URL_MODULE, uriTemplate, servletStatsInstance, null);
   			} catch (StatsFactoryException e) {
   				LoggerHelper.logParamsAndException(logger, Level.SEVERE, CLASS_NAME,"createUriStatsGroup", "error.creating.stats.instance", new Object[]{webAppFullName,servletName} , e );
   			}
           }
   		
   	}*/

     // use tmp var to avoid possible problem due to multithreads
     final public void decRequests(long execTime,  String url)
     {
    	
         long lst = 0; 
         if(responseTime != null && execTime >= 0)
         {
             lst = System.currentTimeMillis();
             responseTime.add(lst, execTime);
         }
                                 
         if(currentRequests != null)
         {
             // Servlet/JSP concurrent request RangeStatistic is NOT synchronized
             // until synchronizedUpdate = true
             if (lst <= 0)
            	 lst = System.currentTimeMillis();
             currentRequests.decrement(lst, 1);
         }
         
         // PK64697 Only collect url stats if they are enabled or if the custom property which requires
         // them UrlPmiModule objects to be created irrespective of whether or not url stats are enabled.
         //if (collectUrlStats()) {
        	 // PK64297 false parameter on getUrlMod indicates not to create a new UrlPMiModule object 
        	 // just to record the end of a request. i.e.: url stats were enabled whilst the request was 
        	 // in progress.
            /* UrlPmiModule urlMod = getUrlMod(url,false);
             if (urlMod!=null){
        	    if (lst <= 0)
            	     lst = System.currentTimeMillis();
         	    urlMod.decRequests (execTime,lst);
         }*/
     //}
     }


     final public void incRequests(String url)
     {
    	 /*if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
    		 if (url!=null)
    			 logger.logp(Level.FINE, CLASS_NAME,"incRequests", "incrementing requests for the servlet:"+servletName+ ", url:" + url);
    		 else
    			 logger.logp(Level.FINE, CLASS_NAME,"incRequests", "incrementing requests for the servlet:"+servletName);
    	 }*/
    	 long lst = 0;
         if(totalRequests != null)
         {
             lst = System.currentTimeMillis();
             totalRequests.increment(lst, 1);
         }

         
         if(currentRequests != null)
         {                
             if(lst <= 0)
            	 lst = System.currentTimeMillis();
             currentRequests.increment(lst, 1);
         }
         
         // PK64697 Only collect url stats if they are enabled or if the custom property which requires
         // them UrlPmiModule objects to be created irrespective of whether or not url stats are enabled.
         /*if (collectUrlStats()) {
             UrlPmiModule urlMod = getUrlMod(url,true);
		 //Begin 292130
         if (urlMod!=null){
        	 if (lst <= 0)
            	 lst = System.currentTimeMillis();
        	 urlMod.incRequests (lst);
         }
		 //End 292130
     }*/
     }

     public void destroy()
     {
	      try {
			StatsFactory.removeStatsInstance(servletStatsInstance);
			/*if (uriStatsGroup!=null)
				StatsFactory.removeStatsGroup(uriStatsGroup);*/
		 } catch (StatsFactoryException e) {
			// LoggerHelper.logParamsAndException(logger, Level.SEVERE, CLASS_NAME,"failed", "failed.to.remove.pmi.stats", new Object[]{servletStatsInstance.getName()} , e );
		 }
		/*if (uriData!=null){
			Set entrySet = uriData.entrySet();
			Iterator iter = entrySet.iterator();
			while (iter.hasNext()){
				((UrlPmiModule)((Map.Entry)iter.next()).getValue()).destroy();
			}
		}*/
		//Begin 292130
		//uriData=null;
		//End 292130

     }

     public void incNumErrors()
     {
         if(numErrors != null)
         {
             numErrors.increment();
         }
     }
     
     // PK64697 Add support for create paramter. If not setto true don't create a new
     // UrlPmiModuleObject if one does not already exist for the specified url. 
     /*private UrlPmiModule getUrlMod(String url, boolean create){
    	 
    	 if (url==null) return null;
		 //Begin 292130
    	 UrlPmiModule urlMod = null;
    	 //In the case, the servletStatsInstance wasn't created correctly, you should never create UriData.
    	if (servletStatsInstance!=null){ 
    		// PK71856 start 
	    	if (uriData!=null){
	    		urlMod = (UrlPmiModule) uriData.get(url);
	    	}
	        if (urlMod==null && create)
	        {
	        	synchronized(this)
	            {
	    	    	if (uriData==null){
		     		   uriData = new HashMap();
	    	    	}
		        	urlMod = (UrlPmiModule) uriData.get(url);
		        	if ((urlMod==null))
		        	{
		        		//urlMod = new UrlPmiModule(webAppFullName,servletName,url,uriStatsGroup);
		        		urlMod = new UrlPmiModule(this,url,uriStatsGroup);
		        		uriData.put(url,urlMod);
		        	}
	            }
	        }
     	}
		//End 292130
        return urlMod;
     }*/

		/* (non-Javadoc)
		 * @see com.ibm.wsspi.pmi.factory.StatisticActionListener#statisticCreated(com.ibm.wsspi.pmi.stat.SPIStatistic)
		 */
		public void statisticCreated(SPIStatistic data) {
         switch(data.getId())
         {
             case WebAppModule.TOTAL_REQUESTS: 
             	totalRequests = (CountStatisticImpl)data; 
             	break;
             case WebAppModule.NUM_ERRORS: numErrors = (CountStatisticImpl)data; break;
             case WebAppModule.RESPONSE_TIME: responseTime = (TimeStatisticImpl)data;break;
             case WebAppModule.CONCURRENT_REQUESTS: currentRequests = (RangeStatisticImpl)data; break;
             case WebAppModule.ASYNC_CONTEXT_RESPONSE_TIME: this.asyncContextResponseTime = (TimeStatisticImpl)data;break;
             default:
                 break;
         }
		}

		/* (non-Javadoc)
		 * @see com.ibm.wsspi.pmi.factory.StatisticActionListener#updateStatisticOnRequest(int)
		 */
		public void updateStatisticOnRequest(int dataId) {
			// TODO Auto-generated method stub
			
		}

		// PK64697 start
		public String getWebAppFullName() {
			return this.webAppFullName;
		}
		
		public String getServletName() {
			return this.servletName;
		}
		
/*		private boolean collectUrlStats()
		{
			
			if (alwaysCreateURLObjects || (pmiCollab==null) || (this.urlStatDesc == null)){
				if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            		logger.logp(Level.FINE, CLASS_NAME,"collectUrlStats","returning true",new Object[] {alwaysCreateURLObjects,pmiCollab,urlStatDesc});
				return true;
			}
			else {
				// Check if url stats are enabled for this servlet
				// results take account of any runtime changes so can chnage at any time 
                StatLevelSpec[] enbld = pmiCollab.getInstrumentationLevel(this.urlStatDesc,Boolean.valueOf(false));
                if (enbld!=null) {
                	int[] k = enbld[0].getEnabled();
                    for(int z=0;z<k.length;z++) {
                    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    		logger.logp(Level.FINE, CLASS_NAME,"collectUrlStats","Stat " + k[z] + " enabled.");
    	               if ((k[z] == UrlPmiModule.CONCURRENT_REQUESTS) || (k[z]==UrlPmiModule.RESPONSE_TIME) || (k[z]==UrlPmiModule.TOTAL_REQUESTS))
    	        	       return true;
                    }   
                }
		    }
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
        		logger.logp(Level.FINE, CLASS_NAME,"collectUrlStats","returning false");
            return false;
		}*/
		// PK64697 end

		public void onAsyncContextComplete(long responseTime, String url) {
	/*		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			 {
	             }*/
			 long lastUpdated = 0; 
	         if(asyncContextResponseTime != null && responseTime >= 0){
	        	 lastUpdated = System.currentTimeMillis();
	             asyncContextResponseTime.add(lastUpdated, responseTime);
	         }
	         
	         // PK64697 Only collect url stats if they are enabled or if the custom property which requires
	         // them UrlPmiModule objects to be created irrespective of whether or not url stats are enabled.
/*	         if (collectUrlStats()) {
	        	 // PK64297 false parameter on getUrlMod indicates not to create a new UrlPMiModule object 
	        	 // just to record the end of a request. i.e.: url stats were enabled whilst the request was 
	        	 // in progress.
	             UrlPmiModule urlMod = getUrlMod(url,false);
	             if (urlMod!=null){
		        	 if (lastUpdated <= 0)
		        		lastUpdated = System.currentTimeMillis();
		         	 	urlMod.onAsyncContextComplete (responseTime,lastUpdated);
	             }
	         }*/
	         
/*	         if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			 {
	             logger.exiting(CLASS_NAME,"onAsyncContextComplete");
			 }*/
		}

 }
