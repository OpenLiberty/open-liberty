// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejb/SuperSFEnvInjectBean.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:53:30
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  SuperSFEnvInjectBean.java
//
// Source File Description:
//
//     Stateful Bean implementation for superclass test
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d435060   EJB3      20070504 kabecker : New Part
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------

package com.ibm.ws.ejbcontainer.injection.mix.ejb;

import java.io.Serializable;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@Stateful (name="SuperSFEnvInject")
@Local (SuperEnvInjectionLocal.class)
@Remote (SuperEnvInjectionRemote.class)
public class SuperSFEnvInjectBean extends SuperSuperEnvInject implements Serializable
{
   private static final long serialVersionUID = -1441010757341309478L;
   private static final String PASSED = "Passed";
   private static final float FDELTA = 0.0F;

   private static final String    A_STRING    = new String("Yes I will!");
   private static final Boolean   A_BOOLEAN   = new Boolean(true);
   private static final Long      A_LONG      = new Long(1111L);
   private static final Double    B_DOUBLE    = new Double(00.7);
   private static final Integer   B_INTEGER   = new Integer(5);
   private static final Character B_CHARACTER = new Character('Y');
   private static final Integer   C_INTEGER  = new Integer(8675309);
   private static final Integer   D_INTEGER  = new Integer(90210);
   private static final Integer   E_INTEGER  = new Integer(8);
   private static final String    F_STRING   = new String("Yes");
   private static final Integer   F_INTEGER  = new Integer(1);
   private static final Float     F_FLOAT    = new Float(55.55F);
   private static final Short     F_SHORT    = new Short((short) 5);

   @Resource
   private SessionContext ivContext;

   @SuppressWarnings( "hiding" )
   @Resource
   private int myNumber=0;

   protected void setMyNumber(int myNumber){
      this.myNumber=myNumber;
   }

   public int getMyNumber(){
      return myNumber;
   }

   String iwasOverridden="Not";
   Long iwasOverridden2=9L;
   short iwasOverridden3=9;

   // iamOverriden is used to test overridden method injection when the name is not defaulted
   @Resource (name="iamOverridden")
   protected void setIamOverridden(String iamOverridden) {
      this.iwasOverridden=iamOverridden;
   }
   @Resource (name="iamOverridden2")
   public void setIamOverridden2(Long iamOverridden2) {
      this.iwasOverridden2=iamOverridden2;
   }
   @Resource (name="iamOverridden3")
   public void setIamOverridden3(short iamOverridden3) {
      this.iwasOverridden3=iamOverridden3;
   }

   /**
    * Verify Environment Injection (field or method) occurred properly.
    **/
   public String verifyEnvInjection(int testpoint)
   {
      String envName = null;

      // Assert that a PROTECTED METHOD of a superclass of a SF bean is injected correctly
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "Super field is Yes I will! : " + getWillIBeInjected(),
                   A_STRING, getWillIBeInjected());
      ++testpoint;

      // Assert that a PRIVATE METHOD of a superclass of a SF bean is injected correctly
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "Super field2 is true : " + isWillIBeInjected2(),
                   A_BOOLEAN, isWillIBeInjected2());
      ++testpoint;

      // Assert that a PUBLIC METHOD of a superclass of a SF bean is injected correctly
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "Super field3 is 1111 : " + getWillIBeInjected3(),
                   A_LONG, getWillIBeInjected3());
      ++testpoint;

      // Assert that a PROTECTED FIELD of a superclass of a SF bean is injected correctly
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "superProtectedDouble is 00.7 : " + getSuperProtectedDouble(),
                   B_DOUBLE, getSuperProtectedDouble());
      ++testpoint;

      // Assert that a PRIVATE FIELD of a superclass of a SF bean is injected correctly
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "superPrivateNumber is 5 : " + getSuperPrivateNumber(),
                   B_INTEGER.intValue(), getSuperPrivateNumber());
      ++testpoint;

      // Assert that a PUBLIC FIELD of a superclass of a SF bean is injected correctly
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "superPublicChar is Y : " + getSuperPublicChar(),
                   B_CHARACTER.charValue(), getSuperPublicChar());
      ++testpoint;

      // Assert that myNumber within this class, not the superclass myNumber is injected correctly
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "myNumber is 8675309 : " + getMyNumber(),
                   C_INTEGER.intValue(), getMyNumber());
      ++testpoint;

      // Assert that the superclass's myNumber was not injected
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "my supers myNumber is 90210 : " + super.getMyNumber(),
                   D_INTEGER.intValue(), super.getMyNumber());
      ++testpoint;

      // Assert that the superclass of the superclass's myNumber was injected
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "my supers supers myNumber is 8 : " + getSuperSuperMyNumber(),
                   E_INTEGER.intValue(), getSuperSuperMyNumber());
      ++testpoint;

      // Assert that the superclass was injected into a PRIVATE FIELD using the default name
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "My supers isMyInjectionDefaulted is true : " + getIsMyInjectionDefaulted(),
                   A_BOOLEAN, getIsMyInjectionDefaulted());
      ++testpoint;

      // Assert that the superclass was injected into a PROTECTED FIELD using the default name
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "My supers isMyInjectionDefaulted2 is yes : " + isMyInjectionDefaulted2,
                   F_STRING, isMyInjectionDefaulted2);
      ++testpoint;

      // Assert that the superclass was injected into a PUBLIC FIELD using the default name
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "My supers isMyInjectionDefaulted3 is Y : " + isMyInjectionDefaulted3,
                   B_CHARACTER, isMyInjectionDefaulted3);
      ++testpoint;

      // Assert that the superclass was injected into a PROTECTED METHOD using the default name
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "My supers isMyInjectionDefaulted4 is 1 : " + isMyInjectionDefaulted4,
                   F_INTEGER.intValue(), isMyInjectionDefaulted4);
      ++testpoint;

      // Assert that the superclass was injected into a PRIVATE METHOD using the default name
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "My supers isMyInjectionDefaulted5 is 55.55 : " + getIsMyInjectionDefaulted5(),
                   F_FLOAT.floatValue(), getIsMyInjectionDefaulted5(), FDELTA);
      ++testpoint;

      // Assert that the superclass was injected into a PUBLIC METHOD using the default name
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "My supers isMyInjectionDefaulted6 is 5 : " + isMyInjectionDefaulted6,
                   F_SHORT.shortValue(), isMyInjectionDefaulted6);
      ++testpoint;

      // Assert that the superclass of a superclass was properly injected with a String
      // into a PRIVATE METHOD using the default name
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "My superSuperPrivateString is Yes I will! : " + getSuperSuperPrivateString(),
                   A_STRING, getSuperSuperPrivateString());
      ++testpoint;

      // Check if injection worked, by calling the overridden method
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "iwasOverridden is Yes: " + iwasOverridden,
                   "Yes", iwasOverridden);
      ++testpoint;

      // Check if injection worked, by calling the overridden method
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "iwasOverridden2 is 5: " + iwasOverridden2,
                   5L, iwasOverridden2.longValue());
      ++testpoint;

      // Check if injection worked, by calling the overridden method
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "iwasOverridden3 is 5: " + iwasOverridden3,
                   (short) 5, iwasOverridden3);
      ++testpoint;

      // Check if superclass injection worked, without calling the overridden method
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "iamOverridden is Yessirree: " + iamOverridden,
                   "Yessirree", iamOverridden);
      ++testpoint;

      // Check if superclass injection worked, without calling the overridden method
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "iamOverridden2 is 6: " + iamOverridden2,
                   6L, iamOverridden2.longValue());
      ++testpoint;

      // Check if superclass injection worked, without calling the overridden method
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "iamOverridden3 is 6: " + iamOverridden3,
                   (short) 6, iamOverridden3);
      ++testpoint;

      // Next, insure the above may be looked up in the global namespace
      Object aString = null;
      try
      {
         Context initCtx = new InitialContext();
         Context myEnv   = (Context)initCtx.lookup("java:comp/env");

         envName = "willIBeInjected";
         aString = myEnv.lookup(envName);
      }
      catch (Throwable ex)
      {
         ex.printStackTrace(System.out);
         fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
              "Global Environment Property lookup failed : " +
              envName + " : " + ex);
      }
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "lookup:" + envName + ":" + aString,
                   A_STRING, aString);
      ++testpoint;

      // Next, insure the above may be looked up from the SessionContext
      try
      {
         envName = "willIBeInjected";
         aString = ivContext.lookup(envName);
      }
      catch (Throwable ex)
      {
         ex.printStackTrace(System.out);
         fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
              "SessionContext Environment lookup failed : " +
              envName + " : " + ex);
      }
      assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "lookup:" + envName + ":" + aString,
                   A_STRING, aString);
      ++testpoint;

      return PASSED;
   }

   /* Provided for interface completeness, used by SLSB tests */
   public void discardInstance()
   {
      return;
   }

   public SuperSFEnvInjectBean()
   {
      // Intentionally blank
   }
}
