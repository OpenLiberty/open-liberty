/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


/**
 * This abstract class enforces the constraint that the subclass will
 * get a wakeUp call at the end of each expired time interval.
 * The subclass must implement the wakeUp method.
 * The logic subtracts the execution time of the wakeUp call from
 * the time interval to sleep the appropriate about of time.
 * If the wakeUp execution takes longer than the time interval,
 * it waits until the next expired time interval.
 */
public abstract class RealTimeDaemon {

   private static TraceComponent tc = Tr.register(RealTimeDaemon.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
   
   /**
    * This is the time interval between waking up.
    */
   protected long timeInterval = 0;

   /**
    * This is the time when this daemon started.
    */
   protected long startDaemonTime = 0;

   /** 
    * setting this to true stops the daemon
    */
   protected boolean stopDaemon = false;
   
   /**
    * This method must be overridden by the subclass to do the work
    * required by the subclass when it is awakened.
    *
    * @param startDaemonTime The time (in milliseconds) when the daemon
    * was started.
    * @param startWakeUpTime The time (in milliseconds) just before
    * the wakeUp method was called.
    */
   protected abstract void wakeUp(long startDaemonTime, long startWakeUpTime);

   /**
    * Constructor with parameter.
    *
    * @param timeInterval The time (in milliseconds) between calls
    * to the wakeUp method.
    */
   protected RealTimeDaemon(long timeInterval) {
      if (timeInterval <= 0) {
         throw new IllegalArgumentException("timeInterval must be positive");
      }
      this.timeInterval = timeInterval;
   }

	public void start() {
		startDaemonTime = System.currentTimeMillis();
		stopDaemon = false;
		Scheduler.createNonDeferrable(timeInterval, this, new Runnable() {
			@Override
			public void run() {
				alarm(this);
			}
		});
	}

   public void stop(){
	   stopDaemon = true;
   }   
   
   /**
    * It runs in a loop that tries to wake every timeInterval.
    * If it gets behind due to an overload, the subclass' implementation
    * of the wakeUp method is responsible for resynching itself based
    * on the startDaemonTime and startWakeUpTime.
    */

   public void alarm(final Object context) {
      long sleepInterval = 0;
      do {
         long startWakeUpTime = System.currentTimeMillis();
         try {
            wakeUp(startDaemonTime, startWakeUpTime);
         } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.RealTimeDaemon.alarm", "83", this);
            if (tc.isDebugEnabled())
               Tr.debug(tc, "exception during wakeUp", ex);
         }
         sleepInterval = timeInterval - (System.currentTimeMillis() - startWakeUpTime);
         // keep looping while we are behind... (i.e. execution time is greater than the time interval)
      } while (sleepInterval <= 0);
      
      if (false == stopDaemon){    	  
			Scheduler.createNonDeferrable(sleepInterval, context,
					new Runnable() {
						@Override
						public void run() {
							alarm(context);
						}
					}); 	  
      }      
   }
   
}
