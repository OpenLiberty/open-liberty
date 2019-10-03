/*******************************************************************************
 * Copyright (c) 1997, 2019 IBM Corporation and others.
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
 class JaxRsPmiModule implements StatisticActionListener,PmiConstants
 {
     // Using Impl classes instead of SPI to avoid the interface overhead
     // since this class is owned by PMI -- "perfOptimization". 
     
     //Jim... must change       
     private static final long serialVersionUID = 3692271975902012581L;
     CountStatisticImpl numErrors = null;        
     CountStatisticImpl totalRequests = null;
     RangeStatisticImpl currentRequests = null; // note this counter may be inaccurate since we don't track it when level is none.
     TimeStatisticImpl responseTime = null;

     private StatsGroup uriStatsGroup = null;
     
     private StatsInstance servletStatsInstance = null;
     private HashMap uriData = null;
     
     private String webAppFullName = null;
     private String servletName = null;
     
     TimeStatisticImpl asyncContextResponseTime; 
     
     public JaxRsPmiModule(String webAppFullName, String servletName, StatsInstance statsInstance, StatsGroup servletStatsGroup)
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
	            	            
	        }
	        catch (StatsFactoryException sfe)
	        {            
	            //Do nothing.
	        }
	       
     }
     
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
         
     }


     final public void incRequests(String url)
     {
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
         
     }

     public void destroy()
     {
     	try {
			StatsFactory.removeStatsInstance(servletStatsInstance);
	      } catch (StatsFactoryException e) {
			// LoggerHelper.logParamsAndException(logger, Level.SEVERE, CLASS_NAME,"failed", "failed.to.remove.pmi.stats", new Object[]{servletStatsInstance.getName()} , e );
		}

     }

     public void incNumErrors()
     {
         if(numErrors != null)
         {
             numErrors.increment();
         }
     }
     
		/* (non-Javadoc)
		 * @see com.ibm.wsspi.pmi.factory.StatisticActionListener#statisticCreated(com.ibm.wsspi.pmi.stat.SPIStatistic)
		 */
		public void statisticCreated(SPIStatistic data) {
         switch(data.getId())
         {
             case JaxRsModule.TOTAL_REQUESTS: 
             	totalRequests = (CountStatisticImpl)data; 
             	break;
             case JaxRsModule.NUM_ERRORS: numErrors = (CountStatisticImpl)data; break;
             case JaxRsModule.RESPONSE_TIME: responseTime = (TimeStatisticImpl)data;break;
             case JaxRsModule.CONCURRENT_REQUESTS: currentRequests = (RangeStatisticImpl)data; break;
             case JaxRsModule.ASYNC_CONTEXT_RESPONSE_TIME: this.asyncContextResponseTime = (TimeStatisticImpl)data;break;
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

		public String getWebAppFullName() {
			return this.webAppFullName;
		}
		
		public String getServletName() {
			return this.servletName;
		}
		

		public void onAsyncContextComplete(long responseTime, String url) {
			 long lastUpdated = 0; 
	         if(asyncContextResponseTime != null && responseTime >= 0){
	        	 lastUpdated = System.currentTimeMillis();
	             asyncContextResponseTime.add(lastUpdated, responseTime);
	         }
	         
		}

 }
