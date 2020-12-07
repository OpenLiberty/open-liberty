// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejb/EnvInjectionLocal.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:53:09
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  EnvInjectionLocal.java
//
// Source File Description:
//
//     Local interface with methods to verify Environment Injection.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d372924   EJB3      20060717 tkb      : New Part
// d372924.4 EJB3      20060809 tkb      : Verify Injection only occurs on create
// d372924.5 EJB3      20060822 tkb      : move to injection package
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------

package com.ibm.ws.ejbcontainer.injection.mix.ejb;


/**
 * Local interface with methods to verify Environment Injection.
 **/
public interface EnvInjectionLocal
{
   /**
    * Verify Environment Injection (field or method) occurred properly.
    **/
   public String verifyEnvInjection(int testpoint);

   /**
    * Verify No Environment Injection (field or method) occurred when
    * an method is called using an instance from the pool (sl) or cache (sf).
    **/
   public String verifyNoEnvInjection(int testpoint);
}
