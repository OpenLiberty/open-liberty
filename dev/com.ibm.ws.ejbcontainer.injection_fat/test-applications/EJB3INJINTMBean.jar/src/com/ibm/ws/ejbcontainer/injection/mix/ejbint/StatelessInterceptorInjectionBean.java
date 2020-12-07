// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.2 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/ejbint/StatelessInterceptorInjectionBean.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 1/31/11 17:32:17
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88, 5655-N02, 5733-W70 (C) COPYRIGHT International Business Machines Corp. 2006, 2011
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  StatelessInterceptorInjectionBean.java
//
// Source File Description:
//
//     This SLSB is used for testing env-entry and reference injection into an
//     interceptor.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d468938    EJB3    9/21/2007 jrbauer  : New part
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// d646777    WAS70    20110131 bkail    : Enable
// --------- --------- -------- --------- -----------------------------------------
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import static javax.ejb.TransactionManagementType.BEAN;

import javax.ejb.CreateException;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.Interceptors;
import javax.sql.DataSource;

/**
 * Class for testing injection into datasource fields of an interceptor class
 * to show that authentication alias and custom-login-configuration specified
 * in bindings file gets used.
 */
@SuppressWarnings("serial")
@Stateless
@TransactionManagement(BEAN)
@RemoteHome(StatelessInterceptorInjectionRemoteHome.class)
@Interceptors({ AnnotationDSInjectionInterceptor.class, XMLDSInjectionInterceptor.class })
public class StatelessInterceptorInjectionBean implements SessionBean {
    private static final String PASSED = "Passed";

    /**
     * Resource reference to datasource when authentication alias is specified
     * in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    private DataSource ivAuthAliasDS;

    /**
     * Resource reference to datasource when custom-login-configuration is used
     * in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    private DataSource ivCustomLoginDS;

    /**
     * Set reference to datasource when authentication alias is specified
     * in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    public void setAuthAliasDS(DataSource ds) {
        ivAuthAliasDS = ds;
    }

    /**
     * Set reference to datasource when custom-login-configuration is used
     * in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    public void setCustomLoginDS(DataSource ds) {
        ivCustomLoginDS = ds;
    }

    /**
     * SessionContext.
     */
    SessionContext ivCtx;

    /**
     * Test injection of datasource references into an interceptor
     * class that uses the @Resource annotation when authentication alias
     * is specified in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    @ExcludeClassInterceptors
    @Interceptors({ AnnotationDSInjectionInterceptor.class })
    public String getAnnotationDSInterceptorResults() {
        // Verify following resource ref in the bindings file:
        //
        //       <resource-ref name="AnnotationDS/jdbc/dsAuthAlias" binding-name="...">
        //            <authentication-alias name="dsAuthAlias" />
        //       </resource-ref>
//      assertNotNull("2 ---> Checking for non-null dsAuthAlias resource ref", ivAuthAliasDS);
//      ResRefList rrl = FATPrivilegedHelper.getResRefList();
//      ResRef rr = (ResRef) rrl.findByName( "AnnotationDS/jdbc/dsAuthAlias" );
//      String loginName = rr.getLoginConfigurationName();
//      assertNotNull("3 ---> dsAuthAlias login config name not null", loginName );
//      assertEquals("4 ---> dsAuthAlias login config name", "DefaultPrincipalMapping", loginName );
//
//      Map<String, String> props = getLoginProperties( rr );
//      assertNotNull("5 ---> dsAuthAlias login config properties not null", props );
//      assertEquals("6 ---> dsAuthAlias number of properties is 1", 1, props.size() );
//
//      String authAlias = props.get("com.ibm.mapping.authDataAlias");
//      assertNotNull("7 ---> dsAuthAlias com.ibm.mapping.authDataAlias property is not null", authAlias );
//      assertEquals("8 ---> dsAuthAlias authentication alias is dsAuthAlias", "dsAuthAlias", authAlias );
//
//      // Verify following resource ref in the bindings file:
//      //
//      // <resource-ref name="AnnotationDS/jdbc/dsCustomLogin" binding-name="...">
//      //   <custom-login-configuration name="TrustedConnectionMapping">
//      //     <property name="com.ibm.mapping.authDataAlias" value="dsCustomLogin" />
//      //     <property name="com.ibm.mapping.propagateSecAttrs" value="false" />
//      //     <property name="com.ibm.mapping.targetRealmName" value="&quot;&quot;" />
//      //     <property name="com.ibm.mapping.unauthenticatedUser" value="RYAN" />
//      //     <property name="com.ibm.mapping.useCallerIdentity" value="false" />
//      //   </custom-login-configuration>
//      // </resource-ref>
//      assertNotNull("9 ---> Checking for non-null dsCustomLoginDS resource ref", ivCustomLoginDS);
//      rr = rrl.findByName( "AnnotationDS/jdbc/dsCustomLogin" );
//      loginName = rr.getLoginConfigurationName();
//      assertNotNull("10 ---> dsCustomLoginDS login config name not null", loginName );
//      assertEquals("11 ---> dsCustomLoginDS login config name", "TrustedConnectionMapping", loginName );
//
//      props = getLoginProperties( rr );
//      assertNotNull("12 ---> dsCustomLoginDS login config properties not null", props );
//      assertEquals("13 ---> dsCustomLoginDS number of properties is 5", 5, props.size() );
//
//      String propValue = props.get("com.ibm.mapping.authDataAlias");
//      assertNotNull("14 ---> dsCustomLoginDS com.ibm.mapping.authDataAlias property is not null", propValue );
//      assertEquals("15 ---> dsCustomLoginDS authentication alias is dsAuthAlias", "dsCustomLogin", propValue );
//
//      propValue = props.get("com.ibm.mapping.propagateSecAttrs");
//      assertNotNull("16 ---> dsCustomLoginDS com.ibm.mapping.propagateSecAttrs property is not null", propValue );
//      assertEquals("17 ---> dsCustomLoginDS com.ibm.mapping.propagateSecAttrs is false", "false", propValue );
//
//      propValue = props.get("com.ibm.mapping.targetRealmName");
//      assertNotNull("18 ---> dsCustomLoginDS com.ibm.mapping.targetRealmName property is not null", propValue );
//      assertEquals("19 ---> dsCustomLoginDS com.ibm.mapping.targetRealmName is empty string", "", propValue );
//
//      propValue = props.get("com.ibm.mapping.unauthenticatedUser");
//      assertNotNull("20 ---> dsCustomLoginDS com.ibm.mapping.unauthenticatedUser property is not null", propValue );
//      assertEquals("21 ---> dsCustomLoginDS com.ibm.mapping.unauthenticatedUser is RYAN", "RYAN", propValue );
//
//      propValue = props.get("com.ibm.mapping.useCallerIdentity");
//      assertNotNull("22 ---> dsCustomLoginDS com.ibm.mapping.useCallerIdentity property is not null", propValue );
//      assertEquals("23 --->dsCustomLoginDS com.ibm.mapping.useCallerIdentity is false", "false", propValue );
//
//      return PASSED;

        return "Failed";
    }

    /**
     * Test injection of datasource references into an interceptor
     * class that uses the <injection-target> inside of a <resource-ref>
     * that is within a <interceptor> stanza in the ejb-jar.xml file
     * and the custom-login-configuration is used
     * in the resource ref binding in ibm-ejb-jar-bnd.xml file.
     */
    @ExcludeClassInterceptors
    @Interceptors({ XMLDSInjectionInterceptor.class })
    public String getXMLDSInterceptorResults() {
        // Verify following resource ref in the bindings file:
        //
        //       <resource-ref name="XMLDS/jdbc/dsAuthAlias" binding-name="jdbc/dsCustomLogin">
        //            <authentication-alias name="dsAuthAlias" />
        //       </resource-ref>
//      assertNotNull("2 ---> Checking for non-null dsAuthAlias resource ref", ivAuthAliasDS);
//      ResRefList rrl = FATPrivilegedHelper.getResRefList();
//      ResRef rr = rrl.findByName( "XMLDS/jdbc/dsAuthAlias" );
//      String loginName = rr.getLoginConfigurationName();
//      assertNotNull("3 ---> dsAuthAlias login config name not null", loginName );
//      assertEquals("4 ---> dsAuthAlias login config name", "DefaultPrincipalMapping", loginName );
//
//      Map<String, String> props = getLoginProperties( rr );
//      assertNotNull("5 ---> dsAuthAlias login config properties not null", props );
//      assertEquals("6 ---> dsAuthAlias number of properties is 1", 1, props.size() );
//
//      String authAlias = props.get("com.ibm.mapping.authDataAlias");
//      assertNotNull("7 ---> dsAuthAlias com.ibm.mapping.authDataAlias property is not null", authAlias );
//      assertEquals("8 ---> dsAuthAlias authentication alias is dsAuthAlias", "dsAuthAlias", authAlias );
//
//      // Verify following resource ref in the bindings file:
//      //
//      // <resource-ref name="XMLDS/jdbc/dsCustomLogin" binding-name="...">
//      //   <custom-login-configuration name="TrustedConnectionMapping">
//      //     <property name="com.ibm.mapping.authDataAlias" value="dsCustomLogin" />
//      //     <property name="com.ibm.mapping.propagateSecAttrs" value="false" />
//      //     <property name="com.ibm.mapping.targetRealmName" value="&quot;&quot;" />
//      //     <property name="com.ibm.mapping.unauthenticatedUser" value="RYAN" />
//      //     <property name="com.ibm.mapping.useCallerIdentity" value="false" />
//      //   </custom-login-configuration>
//      // </resource-ref>
//      assertNotNull("9 ---> Checking for non-null dsCustomLoginDS resource ref", ivCustomLoginDS);
//      rr = rrl.findByName( "XMLDS/jdbc/dsCustomLogin" );
//      loginName = rr.getLoginConfigurationName();
//      assertNotNull("10 ---> dsCustomLoginDS login config name not null", loginName );
//      assertEquals("11 ---> dsCustomLoginDS login config name", "TrustedConnectionMapping", loginName );
//
//      props = getLoginProperties( rr );
//      assertNotNull("12 ---> dsCustomLoginDS login config properties not null", props );
//      assertEquals("13 ---> dsCustomLoginDS number of properties is 5", 5, props.size() );
//
//      String propValue = props.get("com.ibm.mapping.authDataAlias");
//      assertNotNull("14 ---> dsCustomLoginDS com.ibm.mapping.authDataAlias property is not null", propValue );
//      assertEquals("15 ---> dsCustomLoginDS authentication alias is dsAuthAlias", "dsCustomLogin", propValue );
//
//      propValue = props.get("com.ibm.mapping.propagateSecAttrs");
//      assertNotNull("16 ---> dsCustomLoginDS com.ibm.mapping.propagateSecAttrs property is not null", propValue );
//      assertEquals("17 ---> dsCustomLoginDS com.ibm.mapping.propagateSecAttrs is false", "false", propValue );
//
//      propValue = props.get("com.ibm.mapping.targetRealmName");
//      assertNotNull("18 ---> dsCustomLoginDS com.ibm.mapping.targetRealmName property is not null", propValue );
//      assertEquals("19 ---> dsCustomLoginDS com.ibm.mapping.targetRealmName is empty string", "", propValue );
//
//      propValue = props.get("com.ibm.mapping.unauthenticatedUser");
//      assertNotNull("20 ---> dsCustomLoginDS com.ibm.mapping.unauthenticatedUser property is not null", propValue );
//      assertEquals("21 ---> dsCustomLoginDS com.ibm.mapping.unauthenticatedUser is RYAN", "RYAN", propValue );
//
//      propValue = props.get("com.ibm.mapping.useCallerIdentity");
//      assertNotNull("22 ---> dsCustomLoginDS com.ibm.mapping.useCallerIdentity property is not null", propValue );
//      assertEquals("23 --->dsCustomLoginDS com.ibm.mapping.useCallerIdentity is false", "false", propValue );
//
//      return PASSED;

        return "Failed";
    }

    public void ejbCreate() throws CreateException {}

    @Override
    public void ejbRemove() {}

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void setSessionContext(SessionContext sc) {
        ivCtx = sc;
    }

//   private static Map<String, String> getLoginProperties( ResRef rr )
//   {
//      Map<String, String> map = new HashMap<String, String>();
//      for ( ResRef.Property property : rr.getLoginPropertyList() )
//      {
//         map.put( property.getName(), property.getValue() );
//      }
//
//      return map;
//   }
}
