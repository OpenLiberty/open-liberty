// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejbint/StatefulInterceptorInjectionLocal.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 4/5/10 09:38:45
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88, 5655-N02, 5733-W70 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  StatefulInterceptorInjectionLocal.java
//
// Source File Description:
//
//     Interface for SFSB is used for testing env-entry and reference injection
//     into an interceptor.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d468938   EJB3      20070921 jrbauer  : New part
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import javax.ejb.Local;

@Local
public interface StatefulInterceptorInjectionLocal
{
   public String getAnnotationInterceptorResults();
   public String getAnnotationInterceptor2Results();
   public String getXMLInterceptorResults();
   public String getXMLInterceptor2Results();

   public void finish();
   public String getXMLInterceptor4Results();
   public String getXMLInterceptor3Results();
}