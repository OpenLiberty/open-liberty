// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejbint/XMLDSInjectionInterceptor.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 4/5/10 09:38:51
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88, 5655-N02, 5733-W70 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  AnnotationInjectionInterceptor.java
//
// Source File Description:
//
//     This interceptor is used to test annotation-based injection of all the reference
//     types and env-entry.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d468938   EJB3      20070921 jrbauer  : New part
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import java.lang.reflect.Method;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.sql.DataSource;

/**
 * This interceptor class is used when testing injection into a field
 * of an interceptor class when <injection-target> is used inside of a
 * <resource-ref> stanza that is within a <interceptor>
 * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
 * There are 2 different resource ref injections, each is for a datasource.
 * One datasource resource ref is to test the scenario where the
 * ibm-ejb-jar-bnd.xml binding file has a resource-ref binding that uses
 * authentication-alias. The other datasource has a resource-ref binding that
 * uses the custom-login-configuration properties.  The bindings for each
 * of these reference type is inside of an <interceptor> stanza in the
 * ibm-ejb-jar-bnd.xml binding file.
 */
public class XMLDSInjectionInterceptor
{
   //@Resource(name="XMLDS/jdbc/dsAuthAlias")
   DataSource dsAuthAlias;

   //@Resource(name="XMLDS/jdbc/dsCustomLogin")
   DataSource dsCustomLogin;

   @SuppressWarnings("unused")
   @AroundInvoke
   private Object aroundInvoke( InvocationContext inv ) throws Exception
   {
      Method m = inv.getMethod();
      StatelessInterceptorInjectionBean ejb = (StatelessInterceptorInjectionBean) inv.getTarget();
      ejb.setAuthAliasDS( dsAuthAlias );
      ejb.setCustomLoginDS( dsCustomLogin );
      Object rv = inv.proceed();
      return rv;
   }
}
