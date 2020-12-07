// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejbint/MixedSFInterceptorBean.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:53:43
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88, 5655-N02, 5733-W70 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  MixedSFInterceptorBean.java
//
// Source File Description:
//
//     This SFSB is used for testing ejb-ref and ejb-local-ref injection into an
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

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.ExcludeDefaultInterceptors;

@Local(MixedSFLocal.class)
@Remote(MixedSFRemote.class)
@Stateful
@ExcludeDefaultInterceptors
public class MixedSFInterceptorBean
{
   private String ivString;

   public void setString(String str)
   {
      ivString = str;
   }

   public String getString()
   {
      return ivString;
   }

   @Remove
   public void destroy()
   {
      // Intentionally blank
   }
}
