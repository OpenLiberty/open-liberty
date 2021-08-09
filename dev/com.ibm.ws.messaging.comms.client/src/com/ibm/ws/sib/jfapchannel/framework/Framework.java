/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.framework;

import java.net.InetAddress;
import java.util.Map;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.ws.sib.jfapchannel.approxtime.QuickApproxTime;
import com.ibm.ws.sib.jfapchannel.impl.CommsClientServiceFacade;
import com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class is the 'root of all evil'. This class provides services that allow a client to
 * obtain transport factories as well as other framework-y functions.
 * <p>
 * Notice how I am really trying hard not to say the words 'Channel Framework'? This class is the
 * abstraction layer that means that it is possible for the JFap channel to operate completely
 * standalone from the channel framework (when running in the Portly client for example). This class
 * should be implemented differently for the portly client. The Rich client implementation will
 * actually interact directly with the channel framework.
 *
 * @author Gareth Matthews
 */
public abstract class Framework
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = Framework.class.getName();

   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(Framework.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);


   /** The singleton instance of the particular framework implementation */
   private static Framework instance = null;

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client/src/com/ibm/ws/sib/jfapchannel/framework/Framework.java, SIB.comms, WASX.SIB, uu1215.01 1.6");
   }

   /** The instance of the approx time keeper */
   private QuickApproxTime approxTimeImpl = null;

   /** The instance of the alarm manager */
   private AlarmManager alarmManagerImpl = null;

   /**
    * @return Returns the correct instance of the Framework depending on the environment we are in.
    */
   public static Framework getInstance()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getInstance");

      if (instance == null)
      {
         // Are we running in the Portly client?
         if (RuntimeInfo.isThinClient())
         {
            try
            {
               Class clazz = Class.forName(JFapChannelConstants.THIN_CLIENT_FRAMEWORK_CLASS);
               instance = (Framework) clazz.newInstance();
            }
            catch (Exception e)
            {
               FFDCFilter.processException(e, CLASS_NAME + ".getInstance",
                                           JFapChannelConstants.FRAMEWORK_GETINSTANCE_01,
                                           JFapChannelConstants.THIN_CLIENT_FRAMEWORK_CLASS);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to instantiate thin client framework", e);

               // Nothin we can do throw this on...
               throw new SIErrorException(e);
            }
         }
         else
         {
            try
            {
               Class clazz = Class.forName(JFapChannelConstants.RICH_CLIENT_FRAMEWORK_CLASS);
               instance = (Framework) clazz.newInstance();
            }
            catch (Exception e)
            {
               FFDCFilter.processException(e, CLASS_NAME + ".getInstance",
                                           JFapChannelConstants.FRAMEWORK_GETINSTANCE_02,
                                           JFapChannelConstants.RICH_CLIENT_FRAMEWORK_CLASS);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to instantiate rich client framework", e);

               // Nothin we can do throw this on...
               throw new SIErrorException(e);
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getInstance", instance);
      return instance;
   }

   /**
    * @return Returns an instance of the approximate time keeper implementation for this framework.
    *         There is only 1 instance per instance of the framework.
    */
   public QuickApproxTime getApproximateTimeKeeper()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getApproximateTimeKeeper");

      if (approxTimeImpl == null)
      {
         if (RuntimeInfo.isThinClient())
         {
            try
            {
               Class clazz = Class.forName(JFapChannelConstants.THIN_CLIENT_APPROXTIME_CLASS);
               approxTimeImpl = (QuickApproxTime) clazz.newInstance();
            }
            catch (Exception e)
            {
               FFDCFilter.processException(e, CLASS_NAME + ".getInstance",
                                           JFapChannelConstants.FRAMEWORK_GETAPPROXTIME_01,
                                           JFapChannelConstants.THIN_CLIENT_APPROXTIME_CLASS);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to instantiate thin client approx time keeper", e);

               // Nothin we can do throw this on...
               throw new SIErrorException(e);
            }
         }
         else
         {
            try
            {
               Class clazz = Class.forName(JFapChannelConstants.RICH_CLIENT_APPROXTIME_CLASS);
               approxTimeImpl = (QuickApproxTime) clazz.newInstance();
            }
            catch (Exception e)
            {
               FFDCFilter.processException(e, CLASS_NAME + ".getInstance",
                                           JFapChannelConstants.FRAMEWORK_GETAPPROXTIME_02,
                                           JFapChannelConstants.RICH_CLIENT_APPROXTIME_CLASS);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to instantiate rich client approx time keeper", e);

               // Nothin we can do throw this on...
               throw new SIErrorException(e);
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getApproximateTimeKeeper", approxTimeImpl);
      return approxTimeImpl;
   }

   /**
    * Retrieves a thread pool that is backed by the appropriate framework implementation.
    *
    * @param threadPoolName The name for the new thread pool.
    * @param minSize The minimum size for the new pool.
    * @param maxSize The maximum size for the new pool.
    *
    * @return Returns a thread pool of the correct type (this is different depending on whether we
    *         are a thin client or a rich client).
    */
   public ThreadPool getThreadPool(String threadPoolName, int minSize, int maxSize)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getThreadPool",
                                           new Object[]{threadPoolName, minSize, maxSize});

      ThreadPool threadPool = null;

      if (RuntimeInfo.isThinClient())
      {
         try
         {
            Class clazz = Class.forName(JFapChannelConstants.THIN_CLIENT_THREADPOOL_CLASS);
            threadPool = (ThreadPool) clazz.newInstance();
         }
         catch (Exception e)
         {
            FFDCFilter.processException(e, CLASS_NAME + ".getInstance",
                                        JFapChannelConstants.FRAMEWORK_GETTHREADPOOL_01,
                                        JFapChannelConstants.THIN_CLIENT_THREADPOOL_CLASS);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to instantiate thin client thread pool", e);

            // Nothin we can do throw this on...
            throw new SIErrorException(e);
         }
      }
      else
      {
         try
         {
            Class clazz = Class.forName(JFapChannelConstants.RICH_CLIENT_THREADPOOL_CLASS);
            threadPool = (ThreadPool) clazz.newInstance();
         }
         catch (Exception e)
         {
            FFDCFilter.processException(e, CLASS_NAME + ".getInstance",
                                        JFapChannelConstants.FRAMEWORK_GETTHREADPOOL_02,
                                        JFapChannelConstants.RICH_CLIENT_THREADPOOL_CLASS);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to instantiate rich client thread pool", e);

            // Nothin we can do throw this on...
            throw new SIErrorException(e);
         }
      }

      threadPool.initialise(threadPoolName, minSize, maxSize);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getThreadPool", threadPool);
      return threadPool;
   }

   /**
    * @return Returns an AlarmManager instance that is valid for the specified framework. There is
    *         only one per instance of the framework.
    */
   public AlarmManager getAlarmManager()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getAlarmManager");

      if (alarmManagerImpl == null)
      {
         if (RuntimeInfo.isThinClient())
         {
            try
            {
               Class clazz = Class.forName(JFapChannelConstants.THIN_CLIENT_ALARMMGR_CLASS);
               alarmManagerImpl = (AlarmManager) clazz.newInstance();
            }
            catch (Exception e)
            {
               FFDCFilter.processException(e, CLASS_NAME + ".getInstance",
                                           JFapChannelConstants.FRAMEWORK_GETALARMMGR_01,
                                           JFapChannelConstants.THIN_CLIENT_ALARMMGR_CLASS);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to instantiate thin client alarm manager", e);

               // Nothin we can do throw this on...
               throw new SIErrorException(e);
            }
         }
         else
         {
            alarmManagerImpl = CommsClientServiceFacade.getAlarmManager();
            
                     }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getAlarmManager", alarmManagerImpl);
      return alarmManagerImpl;
   }

   /**
    * This method will return the correct instance of the NetworkTransportFactory which allows
    * access to the right connection factories to create connections.
    *
    * @return Returns the appropriate network transport factory.
    */
   public abstract NetworkTransportFactory getNetworkTransportFactory();

   /**
    * This method will return a Map of properties that are associated the specified outbound
    * transport.
    *
    * @param outboundTransportName
    * @return Returns the connection properties
    */
   public abstract Map getOutboundConnectionProperties(String outboundTransportName);

   /**
    * This method will return a Map of properties that are associated the specified endpoint.
    *
    * @param endPoint
    * @return Returns the connection properties
    */
   public abstract Map getOutboundConnectionProperties(Object endPoint);

   /**
    * This method will modify the supplied endpoint to append any SIB specific SSL properties to
    * the endpoint's configuration. It will also modify any TCP properties so that the right thread
    * pool is used before preparing the endpoint itself.
    *
    * @param endPoint
    * 
    * @return a potentially modified endpoint.
    *
    * @throws FrameworkException if the prepare fails for some reason.
    */
   public abstract Object prepareOutboundConnection(Object endPoint) throws FrameworkException;

   /**
    * From the specified endpoint, this method returns the InetAddress buried within it.
    *
    * @param endPoint The endpoint.
    * @return Returns the InetAddress
    */
   public abstract InetAddress getHostAddress(Object endPoint);

   /**
    * From the specified endpoint, this method returns the port number buried within it.
    *
    * @param endPoint The endpoint.
    * @return Returns the port number
    */
   public abstract int getHostPort(Object endPoint);

   /**
    * This method will issue a warning message if the transport name requires SSL but
    * no SSL properties have been specified in the external properties file.
    *
    * @param outputTransportName
    */
   public abstract void warnIfSSLAndPropertiesFileMissing(String outputTransportName);

   /**
    * This method will issue a warning message if the endpoint has specified to use SSL but
    * no SSL properties have been specified in the external properties file.
    *
    * @param endPoint The endpoint.
    */
   public abstract void warnIfSSLAndPropertiesFileMissing(Object endPoint);

   /**
    * As different endpoints are used in different environements and some of them do not implement
    * their own equals() method, this method is needed to provide a way of intro-specting at the
    * parts of an EndPoint to determine whether they are equal.
    *
    * @param ep1 EP1
    * @param ep2 EP2
    *
    * @return Returns true if the endpoints are equal.
    */
   public abstract boolean areEndPointsEqual(Object ep1, Object ep2);

   /**
    * As different endpoints are used in different environements and some of them do not implement
    * their own hashCode() method, this method is needed to provide a way of intro-specting at the
    * parts of an EndPoint to determine its hash code.
    *
    * @param ep The end point.
    *
    * @return Returns a hashcode for this endpoint.
    */
   public abstract int getEndPointHashCode(Object ep);
}
