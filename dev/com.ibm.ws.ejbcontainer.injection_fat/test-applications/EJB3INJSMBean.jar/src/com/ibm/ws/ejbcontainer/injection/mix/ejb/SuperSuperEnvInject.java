// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejb/SuperSuperEnvInject.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:53:33
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  SuperSuperEnvInject.java
//
// Source File Description:
//
//     Super class of a bean.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d435060   EJB3      20070504 kabecker : New Part
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------

package com.ibm.ws.ejbcontainer.injection.mix.ejb;

import javax.annotation.Resource;

public class SuperSuperEnvInject extends SuperOfSuperSuperEnvInject
{
   // used to test field injection when name is not defaulted
   @Resource (name="superProtectedDouble")
   protected Double superProtectedDouble=new Double(00.00);

   @Resource (name="superPrivateNumber")
   private int superPrivateNumber=0;

   @Resource (name="superPublicChar", description="This is the superclasses public character.")
   public char superPublicChar='A';

   // TODO: what does this one do?
   @SuppressWarnings( "hiding" )
   protected int myNumber=90210;

   // (isMyInjectionDefaulted is Used to test injection when the name is defaulted
   @Resource
   private boolean isMyInjectionDefaulted=false;

   @Resource
   protected String isMyInjectionDefaulted2="No";

   @Resource
   public Character isMyInjectionDefaulted3='n';

   protected int isMyInjectionDefaulted4=0;
   private float isMyInjectionDefaulted5=1.11F;
   public short isMyInjectionDefaulted6=1;

   // Test inherited method injection of defaulted names
   @Resource
   protected void setIsMyInjectionDefaulted4(int isMyInjectionDefaulted4)
   {
      this.isMyInjectionDefaulted4=isMyInjectionDefaulted4;
   }

   @SuppressWarnings( "unused" )
   @Resource
   private void setIsMyInjectionDefaulted5(float isMyInjectionDefaulted5)
   {
      this.isMyInjectionDefaulted5=isMyInjectionDefaulted5;
   }

   @Resource
   public void setIsMyInjectionDefaulted6(short isMyInjectionDefaulted6)
   {
      this.isMyInjectionDefaulted6=isMyInjectionDefaulted6;
   }

   protected String  willIBeInjected  = "No I won't";
   private   boolean willIBeInjected2 = false;
   public    Long    willIBeInjected3 = new Long(0000);

   // willIBeInjected is used to test inherited method injection when the name is not defaulted
   @Resource (name="willIBeInjected")
   protected void setWillIBeInjected(String willIBeInjected)
   {
      this.willIBeInjected=willIBeInjected;
   }

   @SuppressWarnings( "unused" )
   @Resource (name="willIBeInjected2")
   private void setWillIBeInjected2(boolean willIBeInjected2)
   {
      this.willIBeInjected2=willIBeInjected2;
   }

   @Resource (name="willIBeInjected3")
   public void setWillIBeInjected3(Long willIBeInjected3)
   {
      this.willIBeInjected3=willIBeInjected3;
   }

   protected String  iamOverridden  = "No I won't";
   public    Long    iamOverridden2 = new Long(0000);
   protected short   iamOverridden3 = 1;

   // iamOverriden is used to test overridden method injection when the name is not defaulted
   @Resource
   protected void setIamOverriden(String iamOverridden)
   {
      this.iamOverridden=iamOverridden;
   }

   @Resource
   public void setIamOverriden2(Long iamOverridden2)
   {
      this.iamOverridden2=iamOverridden2;
   }

   @Resource
   protected void setIamOverriden3(short iamOverridden3)
   {
      this.iamOverridden3=iamOverridden3;
   }

   public String getWillIBeInjected()
   {
      return willIBeInjected;
   }
   public boolean isWillIBeInjected2()
   {
      return willIBeInjected2;
   }
   public Long getWillIBeInjected3()
   {
      return willIBeInjected3;
   }
   public Double getSuperProtectedDouble()
   {
      return superProtectedDouble;
   }
   protected int getSuperPrivateNumber()
   {
      return superPrivateNumber;
   }
   public char getSuperPublicChar()
   {
      return superPublicChar;
   }
   protected void setMyNumber(int myNumber)
   {
      this.myNumber=myNumber;
   }
   public int getMyNumber()
   {
      return myNumber;
   }
   public boolean getIsMyInjectionDefaulted()
   {
      return this.isMyInjectionDefaulted;
   }
   public float getIsMyInjectionDefaulted5()
   {
      return this.isMyInjectionDefaulted5;
   }
}
