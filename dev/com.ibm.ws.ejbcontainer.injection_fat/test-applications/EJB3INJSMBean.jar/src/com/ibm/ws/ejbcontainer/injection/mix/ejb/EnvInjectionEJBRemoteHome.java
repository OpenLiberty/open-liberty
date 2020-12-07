// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejb/EnvInjectionEJBRemoteHome.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:53:07
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  EnvInjectionEJBRemoteHome.java
//
// Source File Description:
//
//     Compatibility EJBHome interface for Session beans with
//     methods to verify Environment Injection.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d420547   EJB3      20070110 jrbauer  : New part
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------

package com.ibm.ws.ejbcontainer.injection.mix.ejb;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * Compatibility EJBHome interface for Session beans with
 * methods to verify Environment Injection.
 **/
public interface EnvInjectionEJBRemoteHome extends EJBHome
{
   /**
    * @return EnvInjectionEJBRemote The SessionBean EJB object.
    * @exception javax.ejb.CreateException SessionBean EJB object was not created.
    */
   public EnvInjectionEJBRemote create()
         throws CreateException, RemoteException;

}
