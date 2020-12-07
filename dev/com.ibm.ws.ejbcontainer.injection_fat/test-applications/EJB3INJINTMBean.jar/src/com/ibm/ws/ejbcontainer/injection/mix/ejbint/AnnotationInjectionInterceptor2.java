// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejbint/AnnotationInjectionInterceptor2.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 4/5/10 09:38:42
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88, 5655-N02, 5733-W70 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  AnnotationInjectionInterceptor.java
//
// Source File Description:
//
//     This interceptor is used to test annotation-based injection of all the reference
//     types and env-entry.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d468938   EJB3      20070921 jrbauer  : New part
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import static javax.annotation.Resource.AuthenticationType.APPLICATION;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * This interceptor class is used when testing injection into a field
 * of an interceptor class when the @EJB or @Resource annotation is used
 * to annotate a field in this interceptor class.
 * Also, the bindings for each of these reference types is inside of a
 * <session> or <message-driven> stanza in the ibm-ejb-jar-bnd.xml binding file.
 */
public class AnnotationInjectionInterceptor2
{
   private static final String CLASS_NAME = AnnotationInjectionInterceptor2.class.getName();
   private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

   private static final String PASSED = "AII2_PASSED";

   private static final String CF_NAME = "java:comp/env/AnnotationInjectionInterceptor/jms/WSTestQCF";
   private static final String REQUEST_QUEUE = "java:comp/env/AnnotationInjectionInterceptor/jms/RequestQueue";

   @EJB(name="AnnotationInjectionInterceptor2/ejbLocalRef",beanName="MixedSFInterceptorBean")
   MixedSFLocal ejbLocalRef;  // EJB Local ref

   @EJB(name="AnnotationInjectionInterceptor2/ejbRemoteRef",beanName="MixedSFInterceptorBean")
   MixedSFRemote ejbRemoteRef;  // EJB Remote ref

   @Resource (name="AnnotationInjectionInterceptor2/jms/WSTestQCF", authenticationType=APPLICATION, shareable=true, description="Queue conn factory")
   public QueueConnectionFactory qcf;

   @Resource(name="AnnotationInjectionInterceptor2/jms/RequestQueue")
   public Queue reqQueue;

   @Resource(name="AnnotationInjectionInterceptor2/jms/ResponseQueue")
   public Queue resQueue;

   @Resource(name="AnnotationInjectionInterceptor2/StringVal")
   public String envEntry;

   @SuppressWarnings("unused")
   @AroundInvoke
   private Object aroundInvoke( InvocationContext inv ) throws Exception
   {
      Method m = inv.getMethod();
      String methodName = m.getName();
      svLogger.info(CLASS_NAME + ".aroundInvoke: " + methodName );
      if ( methodName.equals("getAnnotationInterceptor2Results") || methodName.equals("onMessage") )
      {
         FATMDBHelper.emptyQueue(CF_NAME, REQUEST_QUEUE);
         svLogger.info("AnnotationInjectionInterceptor2 qcf = " + qcf );
         svLogger.info("AnnotationInjectionInterceptor2 reqQueue = " + reqQueue );
         svLogger.info("AnnotationInjectionInterceptor2 resQueue = " + resQueue );

         assertNotNull("Checking for non-null EJB Local Ref", ejbLocalRef);
         ejbLocalRef.setString("AnnotationInjectionInterceptor2");
         assertEquals("Checking ejbLocalRef set/getString",
                      "AnnotationInjectionInterceptor2", ejbLocalRef.getString());

         assertNotNull("Checking for non-null EJB Remote Ref", ejbRemoteRef);
         ejbRemoteRef.setString("AnnotationInjectionInterceptor2");
         assertEquals("Checking ejb remote ref set/getString",
                      "AnnotationInjectionInterceptor2", ejbRemoteRef.getString());

         assertNotNull("Checking for non-null env entry", envEntry);
         assertEquals("Checking value of env entry",
                      "Hello AnnotationInjectionInterceptor2!", envEntry);

         // Using the Queue connection factory and queue will prove those injections were successful
         assertNotNull("Checking for non-null Resource ref", qcf);
         assertNotNull("Checking for non-null message destination ref(reqQueue)", reqQueue);
         assertNotNull("Checking for non-null resource env ref(resQueue)", resQueue);
         svLogger.info("AnnotationInjectionInterceptor pre-send results: " + PASSED);
         if ( methodName.equals("onMessage") )
         {
            MessageDrivenInjectionBean mdb = (MessageDrivenInjectionBean) inv.getTarget();
            mdb.setResults( PASSED );
         }
         else
         {
            if (qcf != null && reqQueue != null)
            {
               FATMDBHelper.putQueueMessage(PASSED, qcf, reqQueue);
            }
            else
            {
               svLogger.info("Could not use qcf or queue, cannot post results!!");
            }
         }
      }

      Object rv = inv.proceed();
      return rv;
   }

   @SuppressWarnings("unused")
   @PreDestroy
   private void preDestroy(InvocationContext inv)
   {
      svLogger.info(CLASS_NAME + ".preDestroy");
      if ( ejbLocalRef != null )
      {
         ejbLocalRef.destroy();
         ejbLocalRef = null;
      }
      if ( ejbRemoteRef != null )
      {
         ejbRemoteRef.destroy();
         ejbRemoteRef = null;
      }
      try
      {
         inv.proceed();
      }
      catch (Exception e)
      {
         throw new EJBException( "unexpected Exception", e );
      }
   }
}
