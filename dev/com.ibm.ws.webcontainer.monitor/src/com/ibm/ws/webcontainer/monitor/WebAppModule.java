/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.factory.StatsInstance;
import com.ibm.wsspi.pmi.factory.StatsTemplateLookup;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.logging.LoggerHelper;

public class WebAppModule extends StatisticActions
implements WebAppPerf, PmiConstants
{

    // Stats template
    // If you are instrumenting a web app then package the template xml and resource bundle
    // in the war file
    private static final String template = "/com/ibm/ws/webcontainer/pmi/webAppModuleStats.xml";
    private static final String servletTemplate = "/com/ibm/ws/webcontainer/pmi/servletStats.xml";


    // a Stats Group <static - one for this class>
    private static StatsGroup webStatsGroup;
    StatsGroup servletStatsGroup = null;
    
    // stats instance
    private StatsInstance webStatsInstance;
    
	private static final long serialVersionUID = -8923368384399206317L;
	protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.stats");
	private static final String CLASS_NAME="com.ibm.ws.wswebcontainer.stats.WebAppModule";

    public final static String WEBAPP_MODULE = "webAppModule";
    public final static String SERVLET_MODULE = "webAppModule.servlets";
    
    // number of servlets loaded
    private SPICountStatistic sgLoadedServlets = null;

    // number of time servlets are reloaded
    private SPICountStatistic sgNumReloads = null;

    private HashMap servletData = null;//specific to the application
    private String appPmiName = null;

    /**
     * Constants used in this module
     */
    public final static int LOADED_SERVLETS = 1;
    public final static int NUM_RELOADS = 2;
    public final static int SERVLETS = 10;                 
    public final static int TOTAL_REQUESTS = 11;
    public final static int CONCURRENT_REQUESTS = 12;
    public final static int RESPONSE_TIME = 13;
    public final static int NUM_ERRORS = 14;
    public final static int ASYNC_CONTEXT_RESPONSE_TIME = 18;
    
    static {
        try
        {
        	// use Class.forName to locate/load the generated class
            Class lookupClass = Class.forName ("com.ibm.ws.pmi.preprocess.webcontainer_StatsTemplateLookup");                                
            StatsFactory.registerStatsTemplateLookup ((StatsTemplateLookup)lookupClass.newInstance());
        }
        catch (Exception e)
        {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"updateStatisticOnRequest", "PMI StatsTemplateLookup class not found.");                                                                                                            
        }

    }
    
    // 3@539186A
    public WebAppModule(String appName) {
      this(appName, false);
    }

    // constructor
    public WebAppModule(String appName, boolean removeOldStatsInstance) // @539186C
    {
        this.appPmiName = appName;
        servletData = new HashMap();
        
        try
        {            
        	//Begin 295175, sync on our webAppModule template to prevent collision
        	//when multiple webapps are initializing at the same time.
            // if there is no stats group create one
            
        	synchronized(template){
	        	if (webStatsGroup == null)
	            {
	                // create a Stats group for my component
	                // passing 'null' since there is no MBean for this "group" 
	                // ** Typically, the Stats group (PMI logical group) will NOT have any MBean **
	                // If you have an MBean for this "group" then pass that ObjectName
	                // "myStats.Group" is translated in myStats.nlsprops                
	                // NOTE: Associating an MBean is OPTIONAL. The statistics can be accessed without an MBean.
	        	    webStatsGroup = StatsFactory.createStatsGroup (WEBAPP_MODULE, template, null,this);
	            }
        	}
        	//End 295175
          
            // create a Stats instance under the Stats group that we created above
            // passing 'null' since there is no MBean for this instance 
            // Typically, the Stats instance WILL have an MBean.
            // If you have an MBean for this instance then pass that ObjectName
            // NOTE: Associating an MBean is OPTIONAL. The statistics can be accessed without an MBean.
            if (removeOldStatsInstance) { // 6@539186A
                StatsInstance oldInstance = StatsFactory.getStatsInstance(new String[] {WebAppModule.WEBAPP_MODULE, appName});
                if (oldInstance != null) { 
                    StatsFactory.removeStatsInstance(oldInstance);
                }
            }
            webStatsInstance = StatsFactory.createStatsInstance (appName, webStatsGroup, null, this);
            createServletStatsGroup();

        }
        catch (StatsFactoryException sfe)
        {            
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"WebAppModule", "PMI StatsFactoryException."+sfe.getMessage());                                                                                                            

        }
    }

    // implementing StatisticActionListener interface
    // grab a reference to the Statistic
    // use ID defined in template to identify a statistic
    public void statisticCreated (SPIStatistic s)
    {
    	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"statisticCreated","Servlet statistic created with id="+s.getId());
        if (s.getId() == LOADED_SERVLETS)
        {
        	sgLoadedServlets = (SPICountStatistic)s;
        }            
        else
        if (s.getId() == NUM_RELOADS)
        {
        	sgNumReloads = (SPICountStatistic)s;
        }            
    }

    
    public void onApplicationAvailableForService()
    {
    }

    public void onApplicationUnavailableForService()
    {
    }

    public void onApplicationStart()
    {   
    	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"onApplicationStart","pmi WebAppModule received onApplicationStart");
        if (sgNumReloads != null)
        {
            sgNumReloads.increment();    
        }        
    }

    public void onApplicationEnd()
    {
    	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"onApplicationEnd","pmi WebAppModule received onApplicationEnd");
    	try {
			StatsFactory.removeStatsInstance(webStatsInstance);
			StatsFactory.removeStatsGroup(servletStatsGroup);
		} catch (StatsFactoryException e) {
			 LoggerHelper.logParamsAndException(logger, Level.SEVERE, CLASS_NAME,"onApplicationEnd", "failed.to.remove.pmi.stats", new Object[]{appPmiName} , e );
		}
    }

    final public void onServletStartService(String servletName, String url)
    {
    	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"onServletStartService","pmi WebAppModule received onServletStartService for servlet:" + servletName + ", url:" + url);
        //if(currentLevel <= LEVEL_NONE) return;        
        // Servlet module instrumentation. Can't check if(bAllCountersDisabled) at the
        // webAppModule level. 
        // If level is less than basic then we just return instead of looking the hashmap
        // calling the servlet module which calls countstatistic.increment which simply
        // returns because the counter is disabled.
        //if(PMIServiceState.iStatisticSet < PMIServiceState.STATISTIC_SETID_BASIC) return;
                
        ServletPmiModule data = (ServletPmiModule)(servletData.get(servletName));
        if(data == null)
        {
        	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    			logger.logp(Level.FINE, CLASS_NAME,"onServletStartService","could not find a servlet pmi module, returning");
        	return;
        }

        data.incRequests(url);
    }

    final public void onServletFinishService(String servletName, long responseTime , String url)
    {
       	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"onServletFinishService","pmi WebAppModule received onServletFinishService for servlet:" + servletName + ", url:" + url);
        //if(PMIServiceState.iStatisticSet < PMIServiceState.STATISTIC_SETID_BASIC) return;
                
        ServletPmiModule data = (ServletPmiModule)(servletData.get(servletName));
        if(data == null)
        {
        	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    			logger.logp(Level.FINE, CLASS_NAME,"onServletFinishService","could not find a servlet pmi module, returning");
        	return;
        }

        data.decRequests(responseTime, url);
    }

    public void onServletStartInit(String j2eeName, String servletName) 
    {
        
       		ServletPmiModule data = new ServletPmiModule (appPmiName, servletName, webStatsInstance, servletStatsGroup);
        //Concurrent requests could result in mismatched data.
		synchronized(servletData)
        {
           servletData.put(servletName,  data);
       }
        
        if(sgLoadedServlets != null)
            sgLoadedServlets.increment();
    }

    /**
	 * 
	 */
	private void createServletStatsGroup() {
	   	
        // if there is no stats group create one
		//A new webmodule stat is created for each thread
		//starting up a webapp, so no need to synchronize
	    
        if (servletStatsGroup == null)
        {
            try {
				// create a Stats group for my component
				// passing 'null' since there is no MBean for this "group" 
				// ** Typically, the Stats group (PMI logical group) will NOT have any MBean **
				// If you have an MBean for this "group" then pass that ObjectName
				// "myStats.Group" is translated in myStats.nlsprops                
				// NOTE: Associating an MBean is OPTIONAL. The statistics can be accessed without an MBean.
                //servletStatsGroup = StatsFactory.createStatsGroup (SERVLET_MODULE, servletTemplate, null,this);
                servletStatsGroup = StatsFactory.createStatsGroup (SERVLET_MODULE, servletTemplate,webStatsInstance, null,this);
			} catch (StatsFactoryException e) {
		                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
		                        logger.logp(Level.FINE, CLASS_NAME,"onServletStartInit","StatsFactoryException : "+e.getMessage());
			}
        }

	}

	public void onServletFinishInit(String servletName)
    {
	
    }

    public void onServletStartDestroy(String servletName)
    {
    }

    public void onServletFinishDestroy(String servletName)
    {
    }

    public void onServletUnloaded(String servletName)
    {
       	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"onServletUnloaded","pmi WebAppModule unloading servlet:" + servletName);
        // unregister even if level is none
        ServletPmiModule data = (ServletPmiModule)(servletData.get(servletName));
        if(data != null)
        {
        	//Begin 295175, do not want the servletPmiModule getting updated
        	//after the data has already been destroyed
            synchronized (servletData)
            {
                servletData.remove (servletName);
            }
            
            data.destroy();
            //End 295175
        }
        else{
        	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    			logger.logp(Level.FINE, CLASS_NAME,"onServletUnloaded","could not find a servlet pmi module, returning");
        	return;
        }

        if(sgLoadedServlets != null)
            sgLoadedServlets.increment(-1);
    }

    public void onServletAvailableForService(String servletName)
    {
    }

    public void onServletUnavailableForService(String servletName)
    {
    }

    public void onServletInitError(String servletName)
    {
    	onServletError(servletName);
    }

    public void onServletServiceError(String servletName)
    {
        onServletError(servletName);
    }
    
    public void onServletError(String servletName)
    {
       	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"onServletError","pmi WebAppModule received error for servlet:" + servletName);
        //if(PMIServiceState.iStatisticSet < PMIServiceState.STATISTIC_SETID_BASIC) return;
        
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"onServletError", "onServletError(), servletName=" + servletName);

        ServletPmiModule data = (ServletPmiModule)(servletData.get (servletName));
        if(data == null)
        {
        	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    			logger.logp(Level.FINE, CLASS_NAME,"onServletError","could not find a servlet pmi module, returning");
        	return;
        }
        data.incNumErrors();
    }


    public void onServletServiceDenied(String servletName)
    {
    }

    public void onServletDestroyError(String servletName)
    {
    	onServletError(servletName);
    }

    // Note: we should not be here, but just in case "onServletStartInit" was not
    //       called before.
//    public ServletPmiModule RecoverFromServletNotFound(String servletName)
//    {
//       	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
//			logger.logp(Level.FINE, CLASS_NAME,"RecoverFromServletNotFound","pmi WebAppModule RecoverFromServletNotFound for servlet:" + servletName);
//    	ServletPmiModule data = new ServletPmiModule (appPmiName, servletName, webStatsInstance, servletStatsGroup);
//        
//        synchronized (servletData)
//        {
//            servletData.put(servletName,  data);    
//        }
//        
//        if(sgLoadedServlets != null)
//            sgLoadedServlets.increment();
//            
//        return data;
//    }
    
	/* (non-Javadoc)
	 * @see com.ibm.wsspi.pmi.factory.StatisticActionListener#updateStatisticOnRequest(int)
	 */
	public void updateStatisticOnRequest(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAsyncContextComplete(String servletName, long responseTime , String url) {
		if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"onAsyncContextComplete","pmi WebAppModule received onAsyncContextComplete for servlet:" + servletName + ", url:" + url);
        //if(PMIServiceState.iStatisticSet < PMIServiceState.STATISTIC_SETID_BASIC) return;
                
        ServletPmiModule data = (ServletPmiModule)(servletData.get(servletName));
        if(data == null)
        {
        	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    			logger.logp(Level.FINE, CLASS_NAME,"onAsyncContextComplete","could not find a servlet pmi module, returning");
        	return;
        }

        data.onAsyncContextComplete(responseTime, url);
	}




} 
