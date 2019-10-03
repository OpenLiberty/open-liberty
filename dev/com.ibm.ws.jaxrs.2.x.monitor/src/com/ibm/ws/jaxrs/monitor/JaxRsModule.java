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
package com.ibm.ws.jaxrs.monitor;

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

public class JaxRsModule extends StatisticActions
implements JaxRsPerf, PmiConstants
{

    // Stats template
    // If you are instrumenting a web app then package the template xml and resource bundle
    // in the war file
    private static final String template = "/com/ibm/ws/jaxrs/pmi/resourceMethodStats.xml";
//    private static final String servletTemplate = "/com/ibm/ws/webcontainer/pmi/servletStats.xml";


    // a Stats Group <static - one for this class>
    private static StatsGroup jaxRsStatsGroup;
//    StatsGroup servletStatsGroup = null;
    
    // stats instance
    private StatsInstance jaxRsStatsInstance;
    
    //Jim... must change
	private static final long serialVersionUID = -8923368384399206317L;
	
	protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.jaxrs.stats");
	private static final String CLASS_NAME="com.ibm.ws.jaxrs.stats.JaxRsModule";

    public final static String WEBAPP_MODULE = "webAppModule";
    public final static String SERVLET_MODULE = "webAppModule.servlets";
    
    // number of servlets loaded
    private SPICountStatistic sgLoadedServlets = null;

    // number of time servlets are reloaded
    private SPICountStatistic sgNumReloads = null;

    private HashMap resourceMethodData = null;//specific to the application
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
            Class lookupClass = Class.forName ("com.ibm.ws.pmi.preprocess.jaxrs_StatsTemplateLookup");                                
            StatsFactory.registerStatsTemplateLookup ((StatsTemplateLookup)lookupClass.newInstance());
        }
        catch (Exception e)
        {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"updateStatisticOnRequest", "PMI StatsTemplateLookup class not found.");                                                                                                            
        }

    }
    
    // 3@539186A
    public JaxRsModule(String appName) {
      this(appName, false);
    }

    // constructor
    public JaxRsModule(String appName, boolean removeOldStatsInstance) // @539186C
    {
        this.appPmiName = appName;
        resourceMethodData = new HashMap();
        
        try
        {            
        	//Begin 295175, sync on our webAppModule template to prevent collision
        	//when multiple webapps are initializing at the same time.
            // if there is no stats group create one
            
        	synchronized(template){
	        	if (jaxRsStatsGroup == null)
	            {
	                // create a Stats group for my component
	                // passing 'null' since there is no MBean for this "group" 
	                // ** Typically, the Stats group (PMI logical group) will NOT have any MBean **
	                // If you have an MBean for this "group" then pass that ObjectName
	                // "myStats.Group" is translated in myStats.nlsprops                
	                // NOTE: Associating an MBean is OPTIONAL. The statistics can be accessed without an MBean.
	        	    jaxRsStatsGroup = StatsFactory.createStatsGroup (WEBAPP_MODULE, template, null,this);
	            }
        	}
        	//End 295175
          
            // create a Stats instance under the Stats group that we created above
            // passing 'null' since there is no MBean for this instance 
            // Typically, the Stats instance WILL have an MBean.
            // If you have an MBean for this instance then pass that ObjectName
            // NOTE: Associating an MBean is OPTIONAL. The statistics can be accessed without an MBean.
            if (removeOldStatsInstance) { // 6@539186A
                StatsInstance oldInstance = StatsFactory.getStatsInstance(new String[] {JaxRsModule.WEBAPP_MODULE, appName});
                if (oldInstance != null) { 
                    StatsFactory.removeStatsInstance(oldInstance);
                }
            }
            jaxRsStatsInstance = StatsFactory.createStatsInstance (appName, jaxRsStatsGroup, null, this);

        }
        catch (StatsFactoryException sfe)
        {            
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"JaxRsModule", "PMI StatsFactoryException."+sfe.getMessage());                                                                                                            

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
			logger.logp(Level.FINE, CLASS_NAME,"onApplicationStart","pmi JaxRsModule received onApplicationStart");
        if (sgNumReloads != null)
        {
            sgNumReloads.increment();    
        }        
    }

    public void onApplicationEnd()
    {
    	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"onApplicationEnd","pmi JaxRsModule received onApplicationEnd");
    	try {
			StatsFactory.removeStatsInstance(jaxRsStatsInstance);
			StatsFactory.removeStatsGroup(jaxRsStatsGroup);
		} catch (StatsFactoryException e) {
			 LoggerHelper.logParamsAndException(logger, Level.SEVERE, CLASS_NAME,"onApplicationEnd", "failed.to.remove.pmi.stats", new Object[]{appPmiName} , e );
		}
    }

    final public void onResourceMethodStart(String resourceMethodName, String url)
    {
    	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"onResourceMethodStart","pmi JaxRsModule received onResourceMethodStart for resource method:" + resourceMethodName + ", url:" + url);
        //if(currentLevel <= LEVEL_NONE) return;        
        // Servlet module instrumentation. Can't check if(bAllCountersDisabled) at the
        // webAppModule level. 
        // If level is less than basic then we just return instead of looking the hashmap
        // calling the servlet module which calls countstatistic.increment which simply
        // returns because the counter is disabled.
        //if(PMIServiceState.iStatisticSet < PMIServiceState.STATISTIC_SETID_BASIC) return;
                
        JaxRsPmiModule data = (JaxRsPmiModule)(resourceMethodData.get(resourceMethodName));
        if(data == null)
        {
        	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    			logger.logp(Level.FINE, CLASS_NAME,"onServletStartService","could not find a servlet pmi module, returning");
        	return;
        }

        data.incRequests(url);
    }

    final public void onResourceMethodStop(String resourceMethodName, long responseTime , String url)
    {
       	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"onServletFinishService","pmi WebAppModule received onServletFinishService for servlet:" + resourceMethodName + ", url:" + url);
        //if(PMIServiceState.iStatisticSet < PMIServiceState.STATISTIC_SETID_BASIC) return;
                
        JaxRsPmiModule data = (JaxRsPmiModule)(resourceMethodData.get(resourceMethodName));
        if(data == null)
        {
        	if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    			logger.logp(Level.FINE, CLASS_NAME,"onServletFinishService","could not find a servlet pmi module, returning");
        	return;
        }

        data.decRequests(responseTime, url);
    }

	@Override
	public void onResourceMethodStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onResourceMethodEnd() {
		// TODO Auto-generated method stub
		
	}



} 
