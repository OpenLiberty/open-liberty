// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejb/SuperEnvInjectionEJBLocalHome.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:53:18
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  SuperEnvInjectionEJBLocalHome.java
//
// Source File Description:
//
//     EJBLocalHome interface for Session beans with methods to verify Environment Injection.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d435060   EJB3      20070504 kabecker : New Part
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------

package com.ibm.ws.ejbcontainer.injection.mix.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

public interface SuperEnvInjectionEJBLocalHome extends EJBLocalHome
{
   /**
    * @return SuperEnvInjectionEJBLocal The SessionBean EJB object.
    * @exception javax.ejb.CreateException SessionBean EJB object was not created.
    */
   public SuperEnvInjectionEJBLocal create() throws CreateException;
}
