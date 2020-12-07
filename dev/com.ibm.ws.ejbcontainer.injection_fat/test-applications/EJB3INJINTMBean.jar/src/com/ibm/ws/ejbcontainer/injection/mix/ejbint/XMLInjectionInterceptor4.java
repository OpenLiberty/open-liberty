// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejbint/XMLInjectionInterceptor4.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 4/5/10 09:38:56
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88, 5655-N02, 5733-W70 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  XMLInjectionInterceptor2.java
//
// Source File Description:
//
//  This is an Interceptor class used for testing env-entry and all types of
//  reference injection through the session bean element of the DD.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d447054   EJB3      20070619 jrbauer  : New copyright/prologue
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.interceptor.InvocationContext;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * This interceptor class is used when testing injection into a field
 * of an interceptor class when <injection-target> is used inside of a
 * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
 * and <message-destination-ref> stanza that is within a <session> or <message-driven>
 * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
 * Also, the bindings for each of these reference type is inside of a
 * <session> or <message-driven> stanza in the ibm-ejb-jar-bnd.xml binding file.
 */
public class XMLInjectionInterceptor4
{
   private static final String CLASS_NAME = XMLInjectionInterceptor4.class.getName();
   private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

   private static final String PASSED = "XII4_PASSED";

   private static final String CF_NAME = "java:comp/env/XMLInjectionInterceptor4/jms/WSTestQCF";
   private static final String REQUEST_QUEUE = "java:comp/env/XMLInjectionInterceptor4/jms/RequestQueue";

   //  @EJB(name="XMLInjectionInterceptor4/ejbLocalRef",beanName="MixedSFInterceptorBean")
   MixedSFLocal ejbLocalRef;  // EJB Local ref

   //  @EJB(name="XMLInjectionInterceptor4/ejbRemoteRef",beanName="MixedSFInterceptorBean")
   MixedSFRemote ejbRemoteRef;  // EJB Remote ref

   //   @Resource (name="XMLInjectionInterceptor4/jms/WSTestQCF", authenticationType=APPLICATION, shareable=true, description="Queue conn factory")
   public QueueConnectionFactory qcf;

   // @Resource(name="XMLInjectionInterceptor4/jms/RequestQueue")
   public Queue reqQueue;

   // @Resource(name="XMLInjectionInterceptor4/jms/ResponseQueue")
   public Queue resQueue;

   //  @Resource(name="XMLInjectionInterceptor4/StringVal")
   public String envEntry;

   @SuppressWarnings("unused")
   //@AroundInvoke
   private Object aroundInvoke( InvocationContext inv ) throws Exception
   {
      Method m = inv.getMethod();
      String methodName = m.getName();
      svLogger.info(CLASS_NAME + ".aroundInvoke: " + methodName );
      if ( methodName.equals("getXMLInterceptor4Results") || methodName.equals("onCreateDBEntryZiyad") )
      {
         FATMDBHelper.emptyQueue(CF_NAME, REQUEST_QUEUE);

         assertNotNull("Checking for non-null ejbLocalRef", ejbLocalRef);
         ejbLocalRef.setString("XMLInjectionInterceptor4");
         assertEquals("Checking ejb local ref set/getString",
                      "XMLInjectionInterceptor4", ejbLocalRef.getString());

         assertNotNull("Checking for non-null ejbRemoteRef", ejbRemoteRef);
         ejbRemoteRef.setString("XMLInjectionInterceptor4");
         assertEquals("Checking ejb remote ref set/getString",
                      "XMLInjectionInterceptor4", ejbRemoteRef.getString());

         assertNotNull("Checking for non-null env entry ref", envEntry);
         assertEquals("Checking value of env entry",
                      "Hello XMLInjectionInterceptor4!", envEntry);

         assertNotNull("Checking for non-null Resource ref", qcf);
         assertNotNull("Checking for non-null message destination ref(reqQueue)", reqQueue);
         assertNotNull("Checking fpr non-null resource env ref(resQueue)", resQueue);

         svLogger.info("XMLInjectionInterceptor4 pre-send results: " + PASSED);
         if ( methodName.equals("onCreateDBEntryZiyad") )
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
   //@PreDestroy
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