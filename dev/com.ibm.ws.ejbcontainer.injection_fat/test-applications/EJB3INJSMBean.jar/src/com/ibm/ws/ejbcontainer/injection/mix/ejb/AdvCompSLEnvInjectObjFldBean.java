// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejb/AdvCompSLEnvInjectObjFldBean.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:52:04
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  AdvCompSLEnvInjectObjFldBean.java
//
// Source File Description:
//
//     Component/Compatibility Stateless Bean implementation for testing Environment
//     Injection of primitive object fields.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d423446   EJB3      20070212 bmdecker : New Part
// d439081   EJB3      20070508 bmdecker : Add Remote
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------

package com.ibm.ws.ejbcontainer.injection.mix.ejb;

import javax.annotation.Resources;
import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * Component/Compatibility Stateless Bean implementation for testing Environment
 * Injection of primitive object fields.
 **/
@SuppressWarnings( "unused" )
@Stateless (name="AdvCompSLEnvInjectObjFld")
@Local (EnvInjectionLocal.class)
@LocalHome (EnvInjectionEJBLocalHome.class)
@Remote (EnvInjectionRemote.class)
@RemoteHome(EnvInjectionEJBRemoteHome.class)
@Resources ({
    @Resource (name="ClassLevelString",type=String.class),
    @Resource (name="AnotherClassLevelString")
})
public class AdvCompSLEnvInjectObjFldBean implements SessionBean
{
   private static final long serialVersionUID = -5171705747511772180L;
   private static final String CLASS_NAME =
      AdvCompSLEnvInjectObjFldBean.class.getName();
   private static final String PASSED = "Passed";

   // Expected Injected Value Constants
   private static final String    A_STRING  = new String("You have been overridden.");
   private static final String    B_STRING  = new String("Yo!");
   private static final String    C_STRING  = null;
   private static final String    D_STRING  = null;
   private static final String    E_STRING  = new String("You'll never find me!");
   private static final String    F_STRING  = new String("They found me!  I don't know how, but they found me!");
   private static final String    G_STRING  = new String("Look me up");
   private static final String    H_STRING  = new String("Head of the class");
   private static final String    I_STRING  = new String("Also in class");
   private static final Integer   I_INTEGER = new Integer(42);
   private static final Object    I_OBJECT  = I_INTEGER;

   /** New Line character for this system.                                   **/
   public static final String NL = System.getProperty("line.separator", "\n");

   @Resource public    String    overrideString = "Yo!";
   @Resource protected String    defaultString = "Yo!";
   @Resource private   String    noString;
   @Resource public    String    cannotFindString;
   @Resource (name="newNameString")
   String    renamedString;
   @Resource private   Object    integerObject;

   @Resource
   private SessionContext ivContext;


   /**
    * Verify Environment Injection (field or method) occurred properly.
    **/
   public String verifyEnvInjection(int testpoint)
   {
      String envName = null;

      // Assert that all of the primitive object method types are injected
      // correctly from the Environment Entries.
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "overrideString is You have been overridden. : " + overrideString,
                   A_STRING, overrideString);
      ++testpoint;
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "defaultString is Yo! : " + defaultString,
                   B_STRING, defaultString);
      ++testpoint;
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "noString is null : " + noString,
                   C_STRING, noString);
      ++testpoint;
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "cannotFindString is null : " + cannotFindString,
                   D_STRING, cannotFindString);
      ++testpoint;
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "renamedString is Doc Brown saying : " + renamedString,
                   F_STRING, renamedString);
      ++testpoint;

      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "integerObject is 42 : " + integerObject,
                   I_OBJECT, integerObject);
      ++testpoint;

      // Next, insure the above may be looked up in the global namespace
      try
      {
         Context initCtx = new InitialContext();
         Context myEnv   = (Context)initCtx.lookup("java:comp/env");

         envName = CLASS_NAME + "/overrideString";
         Object zString = myEnv.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + zString,
                      A_STRING, zString);
         ++testpoint;

         try
         {
            envName = CLASS_NAME + "/defaultString";
            Object yString = myEnv.lookup(envName);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 envName + " should not have been found");
         }
         catch (NameNotFoundException e)
         {
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "16.4.1.3 : " + envName + " correctly not found by JNDI lookup", e);
            ++testpoint;
         }

         try
         {
            envName = CLASS_NAME + "/noString";
            Object xString = myEnv.lookup(envName);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 envName + " should not have been found");
         }
         catch (NameNotFoundException e)
         {
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "16.4.1.3 : " + envName + " correctly not found by JNDI lookup", e);
            ++testpoint;
         }

         try
         {
            envName = CLASS_NAME + "/cannotFindString";
            Object wString = myEnv.lookup(envName);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 envName + " should not have been found");
         }
         catch (NameNotFoundException e)
         {
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "16.4.1.3 : " + envName + " correctly not found by JNDI lookup", e);
            ++testpoint;
         }

         envName = CLASS_NAME + "/cantFindString";
         Object vString = myEnv.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + vString,
                      E_STRING, vString);
         ++testpoint;

         envName = "newNameString";
         Object uString = myEnv.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + uString,
                      F_STRING, uString);
         ++testpoint;

         envName = CLASS_NAME + "/renamedString";
         Object tString = myEnv.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + tString,
                      G_STRING, tString);
         ++testpoint;

         envName = "ClassLevelString";
         Object sString = myEnv.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + sString,
                      H_STRING, sString);
         ++testpoint;

         envName = "AnotherClassLevelString";
         Object rString = myEnv.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + rString,
                      I_STRING, rString);
         ++testpoint;

         envName = CLASS_NAME + "/integerObject";
         Object gInteger = myEnv.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + gInteger,
                      I_INTEGER, gInteger);
         ++testpoint;
      }
      catch (AssertionError error)
      {
         throw error;
      }
      catch (Throwable ex)
      {
         ex.printStackTrace(System.out);
         fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
              "Global Environment Property lookup failed : " +
              envName + " : " + ex);
      }

      // Next, insure the above may be looked up from the SessionContext
      try
      {
         envName = CLASS_NAME + "/overrideString";
         Object aString = ivContext.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + aString,
                      A_STRING, aString);
         ++testpoint;

         try
         {
            envName = CLASS_NAME + "/defaultString";
            Object bString = ivContext.lookup(envName);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 envName + "should not have been found");
         }
         catch (IllegalArgumentException e)
         {
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "16.4.1.3 : " + envName + " correctly not found by EJBContext.lookup", e);
            ++testpoint;
         }

         try
         {
            envName = CLASS_NAME + "/noString";
            Object cString = ivContext.lookup(envName);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 envName + "should not have been found");
         }
         catch (IllegalArgumentException e)
         {
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "16.4.1.3 : " + envName + " correctly not found by EJBContext.lookup", e);
            ++testpoint;
         }

         try
         {
            envName = CLASS_NAME + "/cannotFindString";
            Object dString = ivContext.lookup(envName);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 envName + "should not have been found");
         }
         catch (IllegalArgumentException e)
         {
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "16.4.1.3 : " + envName + " correctly not found by EJBContext.lookup", e);
            ++testpoint;
         }

         envName = CLASS_NAME + "/cantFindString";
         Object eString = ivContext.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + eString,
                      E_STRING, eString);
         ++testpoint;

         envName = "newNameString";
         Object fString = ivContext.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + fString,
                      F_STRING, fString);
         ++testpoint;

         envName = CLASS_NAME + "/renamedString";
         Object gString = ivContext.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + gString,
                      G_STRING, gString);
         ++testpoint;

         envName = "ClassLevelString";
         Object hString = ivContext.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + hString,
                      H_STRING, hString);
         ++testpoint;

         envName = "AnotherClassLevelString";
         Object iString = ivContext.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + iString,
                      I_STRING, iString);
         ++testpoint;

         envName = CLASS_NAME + "/integerObject";
         Object cInteger = ivContext.lookup(envName);
         assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName + ":" + cInteger,
                      I_INTEGER, cInteger);
         ++testpoint;
      }
      catch (AssertionError error)
      {
         throw error;
      }
      catch (Throwable ex)
      {
         ex.printStackTrace(System.out);
         fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
              "SessionContext Environment lookup failed : " +
              envName + " : " + ex);
      }

      return PASSED;
   }

   /**
    * Verify No Environment Injection (field or method) occurred when
    * an method is called using an instance from the pool.
    **/
   public String verifyNoEnvInjection(int testpoint)
   {
      return PASSED;
   }

   /* Added this method for SLSB to provide a way for a client to remove a bean from the cache */
   public void discardInstance()
   {
      throw new javax.ejb.EJBException("discardInstance");
   }

   public AdvCompSLEnvInjectObjFldBean()
   {
      // Intentionally blank
   }

   public void ejbCreate() throws CreateException
   {
      // Intentionally blank
   }

   public void ejbRemove()
   {
      // Intentionally blank
   }

   public void ejbActivate()
   {
      // Intentionally blank
   }

   public void ejbPassivate()
   {
      // Intentionally blank
   }

   public void setSessionContext(SessionContext sc)
   {
      ivContext = sc;
   }
}
