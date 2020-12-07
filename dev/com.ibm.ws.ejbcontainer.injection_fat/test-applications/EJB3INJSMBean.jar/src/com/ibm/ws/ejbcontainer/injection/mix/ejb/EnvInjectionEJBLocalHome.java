// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejb/EnvInjectionEJBLocalHome.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:53:02
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  EnvInjectionEJBLocalHome.java
//
// Source File Description:
//
//     Compatibility EJBLocalHome interface for Session beans with
//     methods to verify Environment Injection.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d372924   EJB3      20060718 tkb      : New Part
// d372924.5 EJB3      20060822 tkb      : move to injection package
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------

package com.ibm.ws.ejbcontainer.injection.mix.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

/**
 * Compatibility EJBLocalHome interface for Session beans with
 * methods to verify Environment Injection.
 **/
public interface EnvInjectionEJBLocalHome extends EJBLocalHome
{
   /**
    * @return EnvInjectionEJBLocal The SessionBean EJB object.
    * @exception javax.ejb.CreateException SessionBean EJB object was not created.
    */
   public EnvInjectionEJBLocal create()
         throws CreateException;
}
