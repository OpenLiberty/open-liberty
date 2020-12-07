// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejbint/StatefulInterceptorInjectionBean.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 4/5/10 09:38:44
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88, 5655-N02, 5733-W70 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  StatefulInterceptorInjectionBean.java
//
// Source File Description:
//
//     This SFSB is used for testing env-entry and reference injection into an
//     interceptor.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d468938   EJB3     9/21/2007 jrbauer  : New part
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import static javax.ejb.TransactionManagementType.BEAN;

import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

import static org.junit.Assert.assertNotNull;


/**
 * Class for testing injection into fields of an interceptor class.
 * Only the StatefulInterceptorInjectionLocal interface method has
 * an implementation. The MixedSFLocal and MixedSFRemote have empty
 * implementation in this class.  This is done to ensure auto-link is
 * not used when a @EJB or <ejb-ref> is used to refer to either the
 * MixedSFLocal or MixedSFRemote business interfaces that is implemented
 * by the MixedSFInterceptorBean class.  This allows us to force the
 * explicit binding file to be used when resolving EJB references.
 */
@Stateful
@Local( {MixedSFLocal.class, StatefulInterceptorInjectionLocal.class } )
@Remote(MixedSFRemote.class)
@ExcludeDefaultInterceptors
@TransactionManagement(BEAN)
@Interceptors( { AnnotationInjectionInterceptor.class, AnnotationInjectionInterceptor2.class
  , XMLInjectionInterceptor.class, XMLInjectionInterceptor2.class, XMLInjectionInterceptor3.class, XMLInjectionInterceptor4.class  })
public class StatefulInterceptorInjectionBean
       implements StatefulInterceptorInjectionLocal, MixedSFLocal, MixedSFRemote
{
   private static final String CLASS_NAME = StatefulInterceptorInjectionBean.class.getName();
   private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

   /**
    * This method is used when testing injection into a field
    * of an interceptor class when the @EJB or @Resource annotation is used
    * to annotate a field in this interceptor class and the
    * bindings for each of these reference types is inside of a
    * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
    */
   @ExcludeClassInterceptors
   @Interceptors( { AnnotationInjectionInterceptor.class })
   public String getAnnotationInterceptorResults()
   {
      String qcfJndiName = "java:comp/env/AnnotationInjectionInterceptor/jms/WSTestQCF";
      String qJndiName = "java:comp/env/AnnotationInjectionInterceptor/jms/RequestQueue";
      svLogger.info("Waiting for message on: " + qJndiName);

      try
      {
         String results = (String)FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName );
         assertNotNull("No results returned from queue", results);
         return results;
      }
      catch ( Exception ex )
      {
         ex.printStackTrace( System.out );
         svLogger.info("Unable to getQueueMessage : " +
                       ex.getClass().getName() + " : " + ex.getMessage());
      }
      return "FAILED : " + CLASS_NAME + ".getAnnotationInterceptorResults";
   }

   /**
    * This method is used when testing injection into a field
    * of an interceptor class when the @EJB or @Resource annotation is used
    * to annotate a field in this interceptor class and the bindings for
    * each of these reference types is inside of the <session> stanza
    * in the ibm-ejb-jar-bnd.xml binding file.
    */
   @ExcludeClassInterceptors
   @Interceptors( { AnnotationInjectionInterceptor2.class })
   public String getAnnotationInterceptor2Results()
   {
      String qcfJndiName = "java:comp/env/AnnotationInjectionInterceptor2/jms/WSTestQCF";
      String qJndiName = "java:comp/env/AnnotationInjectionInterceptor2/jms/RequestQueue";
      svLogger.info("Waiting for message on: " + qJndiName);

      try
      {
         String results = (String)FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName );
         assertNotNull("No results returned from queue", results);
         return results;
      }
      catch ( Exception ex )
      {
         ex.printStackTrace( System.out );
         svLogger.info("Unable to getQueueMessage : " +
                       ex.getClass().getName() + " : " + ex.getMessage());
      }
      return "FAILED : " + CLASS_NAME + ".getAnnotationInterceptor2Results";
   }

   /**
    * This method is used when testing injection into a field
    * of an interceptor class when <injection-target> is used inside of a
    * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
    * and <message-destination-ref> stanza that is within a <interceptor>
    * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
    * The bindings for each of these reference type is inside of a
    * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
    */
   @ExcludeClassInterceptors
   @Interceptors( { XMLInjectionInterceptor.class })
   public String getXMLInterceptorResults()
   {
      String qcfJndiName = "java:comp/env/XMLInjectionInterceptor/jms/WSTestQCF";
      String qJndiName = "java:comp/env/XMLInjectionInterceptor/jms/RequestQueue";
      svLogger.info("Waiting for message on: " + qJndiName);

      try
      {
         String results = (String)FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName );
         assertNotNull("No results returned from queue", results);
         return results;
      }
      catch ( Exception ex )
      {
         ex.printStackTrace( System.out );
         svLogger.info("Unable to getQueueMessage : " +
                       ex.getClass().getName() + " : " + ex.getMessage());
      }
      return "FAILED : " + CLASS_NAME + ".getXMLInterceptorResults";
   }

   /**
    * This method is used when testing injection into a field
    * of an interceptor class when <injection-target> is used inside of a
    * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
    * and <message-destination-ref> stanza that is within a <interceptor>
    * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
    * Also, the bindings for each of these reference type is inside of a
    * a <session> stanza in the ibm-ejb-jar-bnd.xml binding file.
    */
   @ExcludeClassInterceptors
   @Interceptors( { XMLInjectionInterceptor2.class })
   public String getXMLInterceptor2Results()
   {
      String qcfJndiName = "java:comp/env/XMLInjectionInterceptor2/jms/WSTestQCF";
      String qJndiName = "java:comp/env/XMLInjectionInterceptor2/jms/RequestQueue";
      svLogger.info("Waiting for message on: " + qJndiName);

      try
      {
         String results = (String)FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName );
         assertNotNull("No results returned from queue", results);
         return results;
      }
      catch ( Exception ex )
      {
         ex.printStackTrace( System.out );
         svLogger.info("Unable to getQueueMessage : " +
                       ex.getClass().getName() + " : " + ex.getMessage());
      }
      return "FAILED : " + CLASS_NAME + ".getXMLInterceptor2Results";
   }

   /**
    * This method is used when testing injection into a field
    * of an interceptor class when <injection-target> is used inside of a
    * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
    * and <message-destination-ref> stanza that is within a <session>
    * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
    * Also, the bindings for each of these reference type is inside of a
    * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
    */
   @ExcludeClassInterceptors
   @Interceptors( { XMLInjectionInterceptor3.class })
   public String getXMLInterceptor3Results()
   {
      String qcfJndiName = "java:comp/env/XMLInjectionInterceptor3/jms/WSTestQCF";
      String qJndiName = "java:comp/env/XMLInjectionInterceptor3/jms/RequestQueue";
      svLogger.info("Waiting for message on: " + qJndiName);

      try
      {
         String results = (String)FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName );
         assertNotNull("No results returned from queue", results);
         return results;
      }
      catch ( Exception ex )
      {
         ex.printStackTrace( System.out );
         svLogger.info("Unable to getQueueMessage : " +
                       ex.getClass().getName() + " : " + ex.getMessage());
      }
      return "FAILED : " + CLASS_NAME + ".getXMLInterceptor3Results";
   }

   /**
    * This method is used when testing injection into a field
    * of an interceptor class when <injection-target> is used inside of a
    * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
    * and <message-destination-ref> stanza that is within a <session>
    * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
    * Also, the bindings for each of these reference type is inside of a
    * <session> stanza in the ibm-ejb-jar-bnd.xml binding file.
    */
   @ExcludeClassInterceptors
   @Interceptors( { XMLInjectionInterceptor4.class })
   public String getXMLInterceptor4Results()
   {
      String qcfJndiName = "java:comp/env/XMLInjectionInterceptor4/jms/WSTestQCF";
      String qJndiName = "java:comp/env/XMLInjectionInterceptor4/jms/RequestQueue";
      svLogger.info("Waiting for message on: " + qJndiName);

      try
      {
         String results = (String)FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName );
         assertNotNull("No results returned from queue", results);
         return results;
      }
      catch ( Exception ex )
      {
         ex.printStackTrace( System.out );
         svLogger.info("Unable to getQueueMessage : " +
                       ex.getClass().getName() + " : " + ex.getMessage());
      }
      return "FAILED : " + CLASS_NAME + ".getXMLInterceptor4Results";
   }

   @Remove
   public void finish()
   {
      // intentional empty method stub
   }
   @ExcludeClassInterceptors
   public void destroy()
   {
      // intentional empty method stub
   }

   @ExcludeClassInterceptors
   public String getString()
   {
      //     intentional empty method stub
      return null;
   }

   @ExcludeClassInterceptors
   public void setString(String str)
   {
      //     intentional empty method stub
   }

}
