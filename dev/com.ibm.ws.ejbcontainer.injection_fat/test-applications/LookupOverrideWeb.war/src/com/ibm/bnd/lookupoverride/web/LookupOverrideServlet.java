/*******************************************************************************
 * Copyright (c) 2010, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.bnd.lookupoverride.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.bnd.lookupoverride.shared.Bad;
import com.ibm.bnd.lookupoverride.shared.LookupNameBndDriver;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * Verifies that a default binding can be modified during app install, and that
 * the referenced resource can be looked up using the modified JNDI name.
 * <p>
 *
 * <b>Test Matrix:</b>
 * <p>
 * <ul>
 *
 * {@link #testE1LookupWithNoOtherBindings} - can ref and look up @EJB(lookup=xxx)
 * {@link #testE2BindingNameOverLookup} - binding-name overrides @EJB(lookup=xxx)
 * {@link #testE3BindingNameOverLookupName} - binding-name overrides <lookup-name>
 * {@link #testE35BindingNameOverLookupName} - binding-name overrides <lookup-name> for remote
 * {@link #testE4PreventLookupAndBeanName} - both lookup and beanName present on same annotation causes app install validation error
 * {@link #testE45BindingNameOverLookupAndBeanName} - binding-name overrides erroneous @EJB(lookup=xxx, beanName=xxx)
 * {@link #testE5PreventLookupAndBeanNameMultiFields} - CWNEN0054E when both lookup and beanName present
 * {@link #testE6PreventLookupNameAndEjbLink} - <lookup-name> and <ejb-link> on same ejb-ref causes app install validation error
 * {@link #testE7PreventLookupNameAndEjbLinkInterceptorAndBean} - "Conflicting ejb-link/lookup-name element values" with <lookup-name> and <ejb-link> between bean and interceptor
 * {@link #testE75PreventLookupAndBeanNameInterceptorAndBean} - CWNEN0054E with lookup and beanName across bean and interceptor
 * {@link #testE8LookupNameOverLookup} - <lookup-name> overrides @EJB(lookup=xxx)
 * {@link #testE9PreventEjbLinkAndLookup} - <ejb-link> with @EJB(lookup=xxx) causes app install validation error
 * {@link #testE10LookupNameOverBeanName} - <lookup-name> overrides @EJB(beanName=xxx)
 * {@link #testE11EjbLinkOverBeanName} - <ejb-link> overrides @EJB(beanName=xxx)
 * {@link #testE12MissingClassThwartsInjection}- CWNEN0047W when injection is ignored due to missing field
 * {@link #testR0EnvEntry} - <env-entry-value> is injected
 * {@link #testR1EnvEntryLookup} - bound Integer value is injected as an env-entry via lookup on @Resource annotation
 * {@link #testR3ResourceRefLookup} - DataSource is injected as a resource-ref via lookup on @Resource annotation
 * {@link #testR4MessageDestinationRefLookup} - Queue is injected as a message-destination-ref via lookup on @Resource annotation
 * {@link #testR5ResourceRefBindingNameOverLookup} - DataSource is injected as a resource-ref, taking binding-name over lookup
 * {@link #testR6ResourceEnvRefBindingNameOverLookup} - Queue is injected as a resource-env-ref, taking binding-name over lookup
 * {@link #testR7MessageDestinationRefBindingNameOverLookup} - Queue is injected as a message-destination-ref, taking binding-name over lookup
 * {@link #testR8ResourceRefBindingNameOverLookupName} - DataSource is injected as a resource-ref, taking binding-name over lookup-name
 * {@link #testR9ResourceEnvRefBindingNameOverLookupName} - Queue is injected as a resource-env-ref, taking binding-name over lookup-name
 * {@link #testR10MessageDestinationRefBindingNameOverLookupName} - Queue is injected as a message-destination-ref, taking binding-name over lookup-name
 * {@link #testR11ResourceRefLookupNameOverLookup} - DataSource is injected as a resource-ref, taking lookup-name over lookup
 * {@link #testR12ResourceEnvRefLookupNameOverLookup} - Queue is injected as a resource-env-ref, taking lookup-name over lookup
 * {@link #testR13MessageDestinationLookupNameOverLookup} - Queue is injected as a message-destination-ref, taking lookup-name over lookup
 * {@link #testR140NameAndLookupOnEnvEntry} - <env-entry-value> is injected
 *
 *
 *
 *
 * </ul>
 */
@SuppressWarnings("serial")

@WebServlet("/LookupOverrideServlet")
public class LookupOverrideServlet extends FATServlet {

    private static final String CLASS_NAME = LookupOverrideServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static LookupNameBndDriver svTestDriver2 = null;

    static {
        try {
            InitialContext initialCtx = new InitialContext();

            // For variation R1, bind an Integer into the Global namespace by name "envEntryIntegerBound".
            bindObject(initialCtx, "envEntryIntegerBound", new Integer(601));

            // For variation R2, bind a String into the Global namespace by name "resourceEnvRefStringBoundA".
            bindObject(initialCtx, "resourceEnvRefStringBoundA", new String("AAAA"));

            // For variation R6, bind a String into the Global namespace by name "resourceEnvRefStringBoundB".
            bindObject(initialCtx, "resourceEnvRefStringBoundB", new String("BBBB"));

            // For variation R12, bind a String into the Global namespace by name "resourceEnvRefStringBoundC".
            bindObject(initialCtx, "resourceEnvRefStringBoundC", new String("CCCC"));

            // For variation R20, bind an Integer into the Global namespace by name "EnvEntry_IntegerAlternate".
            bindObject(initialCtx, "EnvEntry_IntegerAlternate", new Integer(333));

            if (svTestDriver2 == null) {
                // Lookup the other test driver EJB
                Context ivContext = (Context) initialCtx.lookup("");
                Object obj = ivContext.lookup("ejb/LookupOverrideApp/LookupOverrideEJB.jar/LookupNameBndDriverBean#com.ibm.bnd.lookupoverride.shared.LookupNameBndDriver");
                svTestDriver2 = (LookupNameBndDriver) PortableRemoteObject.narrow(obj,
                                                                                  LookupNameBndDriver.class);
            }
        } catch (NamingException ex) {
            throw new Error(ex);
        }
    }

    /**
     * E4.
     * Annotate a bean's instance variable with @EJB(lookup="SLT9", beanName="ejblocal:SLT"),
     * with both the bean referee and referent in the same application.
     *
     * - Verify that an appropriate validation error is received during application
     * install, since lookup and beanName cannot coexist on the @EJB annotation.
     */
    @Test
    @ExpectedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testE4PreventLookupAndBeanName() throws Exception {

        // install app with badly-annotated bean (both lookup and beanName specified)
        try {
            Bad badBean = (Bad) new InitialContext().lookup("java:global/LookupOverrideApp/DoaApp1/Bad1Bean!com.ibm.bnd.lookupoverride.shared.Bad");
            badBean.boing();
            fail("accessing bean from BadApp1 should have failed");
        } catch (Exception e) {
            svLogger.log(Level.INFO, "caught exception", e);

            String msg = e.getMessage();
            if (!msg.contains("InjectionConfigurationException")) {
                fail("caught Exception with error accessing BadBean1 but it did not have the expected message text.  Text was: " + msg);
            }
        }

    }

    /**
     * E5.
     * Annotate a bean's instance variable with @EJB(name="CommonName", beanName="fooBean"), with
     * both the bean referee and referrant in the same application.
     * Annotate another instance var on the same bean with @EJB(name="CommonName", lookup="abc").
     *
     * - Verify that an appropriate validation error is received, since @EJB annotations with
     * the same name attribute cannot together (nor individually) have both a lookup and
     * a beanName attribute.
     */
    @Test
    @ExpectedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testE5PreventLookupAndBeanNameMultiFields() throws Exception {

        try {
            String result = svTestDriver2.verifyE5ErrorLookupAndBeanNameMultiFields();
            assertEquals("PASS", result);
        } catch (Exception e) {
            fail("testE5PreventLookupAndBeanNameMultiFields caught Exception calling verifyErrorLookupAndBeanNameMultiFields: " + e);
        }

    }

    /**
     * E6.
     * With no @EJB annotation, define an <ejb-local-ref> element in ejb-jar.xml containing
     * both a <lookup-name> subelement and an <ejb-link> subelement. The bean referee and
     * referent must be in the same application.
     *
     * -Verify that an appropriate validation error is received, since <lookup-name> and <ejb-link>
     * cannot coexist on an ejb reference.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.wsspi.injectionengine.InjectionConfigurationException" })
    public void testE6PreventLookupNameAndEjbLink() throws Exception {

        // install app with badly-annotated bean (both lookup and <ejb-link> specified)
        try {
            Bad badBean = (Bad) new InitialContext().lookup("java:global/LookupOverrideApp/DoaApp3/Bad3Bean!com.ibm.bnd.lookupoverride.shared.Bad");
            badBean.boing();
            fail("accessing bean from BadApp3 should have failed");
        } catch (Exception e) {
            svLogger.log(Level.INFO, "caught exception", e);

            String msg = e.getMessage();
            if (!msg.contains("InjectionConfigurationException")) {
                fail("caught Exception with error accessing BadBean3 but it did not have the expected message text.  Text was: " + msg);
            }
        }

    }

    /**
     * E7.
     * With NO annotation on a bean, nor it's interceptor,
     *
     * define an <ejb-local-ref> element under the bean's <session> element in ejb-jar.xml containing
     * <ejb-ref-name>commonName and <lookup-name>ejblocal:foo
     *
     * define an <ejb-local-ref> element under the <interceptor> element in ejb-jar.xml containing
     * <ejb-ref-name>commonName and <ejb-link>SLI
     *
     * a. Verify that an appropriate validation error is received, since multiple ejb reference elements
     * with the same name cannot together have both a lookup and a beanName annotation attribute.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.wsspi.injectionengine.InjectionConfigurationException" })
    public void testE7PreventLookupNameAndEjbLinkInterceptorAndBean() throws Exception {

        // install app with badly-annotated bean (both lookup and beanName specified)
        try {
            String result = svTestDriver2.verifyE7ErrorLookupNameAndEjbLinkInterceptorAndBean();
            assertEquals("PASS", result);
        } catch (Exception e) {
            fail("testE7PreventLookupNameAndEjbLinkInterceptorAndBean caught Exception calling verifyErrorLookupAndBeanNameInterceptorAndBean: " + e);
        }
    }

    /**
     * E7.5 Annotate a bean's instance var with @EJB(name="CommanName2", beanName="xxx").
     * Annotate an interceptor's instance var with @EJB(name="CommonName2", lookup="zzz").
     * a. Verify that an appropriate validation error is received, since multiple ejb reference elements
     * with the same name cannot together have both a lookup and a beanName annotation attribute.
     */
    @Test
    @ExpectedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testE75PreventLookupAndBeanNameInterceptorAndBean() throws Exception {

        // install app with badly-annotated bean (both lookup and beanName specified)
        try {
            String result = svTestDriver2.verifyE75ErrorLookupAndBeanNameInterceptorAndBean();
            assertEquals("PASS", result);
        } catch (Exception e) {
            fail("testE75PreventLookupAndBeanNameInterceptorAndBean caught Exception calling verifyE75ErrorLookupAndBeanNameInterceptorAndBean: " + e);
        }
    }

    /**
     * E9.
     * Annotate a bean's instance var with @EJB(name="SLT9", lookup="SLT9").
     *
     * Define an <ejb-link> subelement of <ejb-ref> in ejb-jar.xml:
     * <ejb-local-ref>
     * <ejb-ref-name>SLT9</ejb-ref-name>
     * <ejb-link>SimpleLookupTargetBean2</ejb-link>
     * </ejb-local-ref>
     *
     * - Verify that an appropriate validation error is received, sincethe lookup attribute on the annotation
     * cannot be used in conjunction with <ejb-link> in ejb-jar.xml.
     */
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException" })
    public void testE9PreventEjbLinkAndLookup() throws Exception {

        // install app with badly-annotated bean (both lookup and <ejb-link> specified)
        try {
            Bad badBean = (Bad) new InitialContext().lookup("java:global/LookupOverrideApp/DoaApp7/Bad7Bean!com.ibm.bnd.lookupoverride.shared.Bad");
            badBean.boing();
            fail("accessing bean from BadApp7 should have failed");
        } catch (Exception e) {
            svLogger.log(Level.INFO, "caught exception", e);

            String msg = e.getMessage();
            if (!msg.contains("EJBConfigurationException")) {
                fail("caught Exception with error accessing BadBean7 but it did not have the expected message text.  Text was: " + msg);
            }
        }
    }

    /**
     * E12.
     * Annotate a bean's instance var with valid annotation @EJB(lookup="ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean").
     * In the same bean, define an instance var which will be unreachable at runtime,
     * causing the CWNEN0047W message to be issued.
     */
    @Test
    @ExpectedFFDC("java.lang.NoClassDefFoundError")
    public void testE12MissingClassThwartsInjection() throws Exception {

        // install app with good @EJB annotation but with other field missing at runtime
        try {
            String status = svTestDriver2.verifyE12MissingClassThwartsInjection();
            assertEquals("PASS", status);
        } catch (Exception e) {
            fail("testE12MissingClassThwartsInjection caught Exception calling verifyE12MissingClassThwartsInjection: " + e);
        }
    }

    /**
     * E1.
     * Annotate a bean's instance variable referencing another bean, with @EJB(name="ejb/foo" lookup="bar").
     * Do NOT define an <ejb-ref> element in ejb-jar.xml, nor in ibm-ejb-jar-bnd.xml.
     *
     * - Verify that the bean reference can be used to invoke a method on the referenced bean.
     *
     * - Verify that the bean can be looked up using the value of the lookup attribute on the
     *
     * @EJB annotation, and that the returned ejb reference can be used to invoke a method
     *      on the referenced bean.
     *
     *      - Verify that the bean can be looked up using the value of the name attribute on the
     * @EJB annotation, and that the returned ejb reference can be used to invoke a method
     *      on the referenced bean.
     **/
    @Test
    public void testE1LookupWithNoOtherBindings() throws Exception {

        String status = svTestDriver2.verifyE1LookupWithNoOtherBindings();
        assertEquals("PASS", status);

    }

    /**
     * E2.
     * Annotate a bean's instance variable referencing another bean, with @EJB(lookup="abc").
     * Define a binding-name attribute on the <ejb-ref> element in ibm-ejb-jar-bnd.xml
     *
     * - Verify that the binding-name value takes precedence over the lookup value.
     */
    @Test
    public void testE2BindingNameOverLookup() throws Exception {

        String status = svTestDriver2.verifyE2BindingNameOverLookup();
        assertEquals("PASS", status);
    }

    /**
     * E3.
     * With no @EJB annotation, define a <lookup-name> sub-element of <ejb-local-ref> in ejb-jar.xml.
     * Define a binding-name attribute on the <ejb-ref> element in ibm-ejb-jar-bnd.xml.
     *
     * Use a local bean (<ejb-local-ref> in ejb-jar.xml)
     *
     * - Verify that the binding-name value takes precedence over the <lookup-name> value.
     */
    @Test
    public void testE3BindingNameOverLookupName() throws Exception {

        String status = svTestDriver2.verifyE3BindingNameOverLookupName();
        assertEquals("PASS", status);

    }

    /**
     * E35.
     * With no @EJB annotation, define a <lookup-name> sub-element of <ejb-ref> in ejb-jar.xml.
     * Define a binding-name attribute on the <ejb-ref> element in ibm-ejb-jar-bnd.xml.
     *
     * Use a remote bean (remote uses <ejb-ref> vs <ejb-ref> in ejb-jar.xml)
     *
     * - Verify that the binding-name value takes precedence over the <lookup-name> value.
     */
    @Test
    public void testE35BindingNameOverLookupName() throws Exception {

        // Try the same with a remote bean:
        String status = svTestDriver2.verifyE35BindingNameOverLookupNameRemote();
        assertEquals("PASS", status);
    }

    /**
     * E4.5.
     * Annotate a bean's instance variable with @EJB(lookup="ejblocal:SLT", beanName="SLT9"),
     * with both the bean referee and referent in the same application.
     * Define a binding-name attribute on the <ejb-ref> element in ibm-ejb-jar-bnd.xml
     *
     * - Verify that the binding name value takes precedence over the
     * conflicting lookup and beanName values.
     */
    @Test
    public void testE45BindingNameOverLookupAndBeanName() {
        String status = svTestDriver2.verifyE45BindingNameOverLookupAndBeanName();
        assertEquals("PASS", status);
    }

    /**
     * E8.
     * Annotate a bean's instance variable referencing another bean, with @EJB(name="abc" lookup="xyz").
     * Define a <lookup-name> element under an <ejb-local-ref> element in ejb-jar.xml
     *
     * - Verify that the <lookup-name> value takes precedence over the annotation's lookup value.
     */
    @Test
    public void testE8LookupNameOverLookup() throws Exception {

        String status = svTestDriver2.verifyE8LookupNameOverLookup();
        assertEquals("PASS", status);
    }

    /**
     * E10.
     * Annotate a bean's instance variable referencing another bean, with @EJB(name="abc" beanName="xyz").
     * Define a <lookup-name> element under an <ejb-local-ref> element in ejb-jar.xml
     *
     * - Verify that the <lookup-name> value takes precedence over the annotation's beanName value.
     */
    @Test
    public void testE10LookupNameOverBeanName() throws Exception {

        String status = svTestDriver2.verifyE10LookupNameOverBeanName();
        assertEquals("PASS", status);
    }

    /**
     * E11.
     * Annotate a bean's instance var with @EJB(beanName="ejblocal:bogus").
     * Define an <ejb-link> subelement of <ejb-ref> in ejb-jar.xml.
     *
     * - Verify that the <ejb-link> value takes precedence over the beanName attribute on the annotation.
     */
    @Test
    public void testE11EjbLinkOverBeanName() throws Exception {

        String status = svTestDriver2.verifyE11EjbLinkOverBeanName();
        assertEquals("PASS", status);
    }

    //
    //
    //  The following are variations for the family of resource references:
    //
    //     env-entry
    //     resource-env-ref
    //     resource-ref
    //     message-destination-ref
    //
    //

    /**
     * R0.
     * Annotate a bean's instance var with:
     *
     * @Resource(name="EnvEntry_Integer") Integer ivEnvEntry_Integer;
     *                                    Define an <env-entry> in ejb-jar.xml,
     *                                    with <env-entry-name>EnvEntry_Integer
     *                                    and with <env-entry-value>
     *
     *                                    - Verify that the <env-entry-value> is injected into ivEnvEntry_Integer.
     *                                    - Verify that the Integer can be explicitly looked up by name "EnvEntry_Integer".
     */
    @Test
    public void testR0EnvEntry() throws Exception {

        String status = svTestDriver2.verifyR0EnvEntry();
        assertEquals("PASS", status);
    }

///////////////////// basic @Resource annotation with lookup= variations /////////////////////

    /**
     * R1.
     * Bind an Integer into the Global namespace by name "envEntryIntegerBound".
     *
     * Annotate a bean's instance var with:
     *
     * @Resource(name="EnvEntry_IntegerBound", lookup="envEntryIntegerBound") Integer ivEnvEntry_IntegerBound;
     *
     *                                         - Verify that the bound Integer value is injected into ivEnvEntry_IntegerBound.
     *                                         - Verify that the bound Integer value can be explicitly looked up by name "EnvEntry_IntegerBound".
     */
    @Test
    public void testR1EnvEntryLookup() throws Exception {

        String status = svTestDriver2.verifyR1EnvEntryLookup();
        assertEquals("PASS", status);

    }

    /**
     * R3.
     * Annotate a bean's instance var with:
     *
     * @Resource(name="ResourceRef_DataSource_Lookup", lookup="jdbc/DefaultEJBTimerDataSource")
     *                                                 javax.sql.DataSource ivResourceRef_DataSource_Lookup;
     *
     *                                                 Define a <resource-ref> in ejb-jar.xml with
     *                                                 <resource-ref-name>ResourceRef_DataSource_Lookup
     *                                                 and <resource-ref-type>javax.sql.DataSource
     *
     *                                                 - Verify that the datasource is injected into ivResourceRef_DataSource_Lookup.
     *                                                 - Verify that the datasource can be explicitly looked up by name "ResourceRef_DataSource_Lookup".
     */
    @Test
    public void testR3ResourceRefLookup() throws Exception {

        String status = svTestDriver2.verifyR3ResourceRefLookup();
        assertEquals("PASS", status);
    }

    /**
     * R4.
     * Bind a message destination into the Global namespace by name "Jetstream/jms/RequestQueue" (via TestBuild.xml)
     *
     * Annotate a bean's instance var with:
     *
     * @Resource(name="MessageDestinationRef_RequestQueueLookup")
     *                                                            MessageDestination ivMessageDestinationRef_RequestQueueLookup;
     *
     *                                                            Define a <message-destination-ref> in ejb-jar.xml with
     *                                                            <message-destination-ref-name>MessageDestinationRef_RequestQueueLookup
     *                                                            and <message-destination-type>javax.jms.Queue
     *
     *                                                            - Verify that the message destination is injected into ivMessageDestinationRef_RequestQueueLookup.
     *                                                            - Verify that the message destination can be explicitly looked up by name
     *                                                            "MessageDestinationRef_RequestQueueLookup".
     */
    @Test
    public void testR4MessageDestinationRefLookup() throws Exception {

        String status = svTestDriver2.verifyR4MessageDestinationRefLookup();
        assertEquals("PASS", status);
    }

///////////////////// binding-name over lookup variations /////////////////////
//
// Verify that the binding-name in ibm-ejb-jar-bnd.xml takes precedence over the
// lookup attribute in the @Resource annotation

    /**
     * R5.
     * Annotate a bean's instance var with:
     *
     * @Resource(name="ResourceRef_DataSource_BindingNameOverLookup", lookup="eis/BogusDataSource")
     *                                                                javax.resource.cci.ConnectionFactory ivResourceRef_DataSource_BindingNameOverLookup;
     *
     *                                                                Define a <resource-ref name="ResourceRef_DataSource_BindingNameOverLookup" in ibm-ejb-jar-bnd.xml with
     *                                                                binding-name="jdbc/DefaultEJBTimerDataSource"
     *
     *                                                                - Verify that the existing valid datasource is injected into ivResourceRef_DataSource_BindingNameOverLookup.
     *                                                                - Verify that the existing valid datasource can be explicitly looked up by name
     *                                                                "ResourceRef_DataSource_BindingNameOverLookup".
     */
    @Test
    public void testR5ResourceRefBindingNameOverLookup() throws Exception {

        String status = svTestDriver2.verifyR5ResourceRefBindingNameOverLookup();
        assertEquals("PASS", status);
    }

    /**
     * R6.
     * Annotate a bean's instance var with:
     *
     * @Resource(name="ResourceEnvRef_Queue_BindingNameOverLookup", lookup="BogusEnvRef")
     *                                                              Queue ivResourceEnvRef_Queue_BindingNameOverLookup;
     *
     *                                                              Define a <resource-env-ref name="ResourceEnvRef_Queue_BindingNameOverLookup" in ibm-ejb-jar-bnd.xml with
     *                                                              binding-name="java:comp/env/jms/ResponseQueue"
     *
     *                                                              - Verify that the bound String is injected into ivResourceEnvRef_Queue_BindingNameOverLookup.
     *                                                              - Verify that the bound String can be explicitly looked up by name "ResourceEnvRef_Queue_BindingNameOverLookup".
     */
    @Test
    public void testR6ResourceEnvRefBindingNameOverLookup() throws Exception {

        String status = svTestDriver2.verifyR6ResourceEnvRefBindingNameOverLookup();
        assertEquals("PASS", status);
    }

    /**
     * R7.
     * Bind a message destination into the Global namespace by name "Jetstream/jms/RequestQueue" (via TestBuild.xml)
     *
     * Annotate a bean's instance var with:
     *
     * @Resource(name="MessageDestinationRef_BindingNameOverLookup", lookup="BogusMessageDestinationRef")
     *                                                               String ivMessageDestinationRef_bogusLookup;
     *
     *                                                               Define a <message-destination-ref name="MessageDestinationRef_BindingNameOverLookup" in ibm-ejb-jar-bnd.xml
     *                                                               with
     *                                                               binding-name="Jetstream/jms/RequestQueue"
     *
     *                                                               - Verify that the bound message destination is injected into ivMessageDestinationRef_BindingNameOverLookup.
     *                                                               - Verify that the bound message destination can be explicitly looked up by name
     *                                                               "MessageDestinationRef_BindingNameOverLookup".
     */
    @Test
    public void testR7MessageDestinationRefBindingNameOverLookup() throws Exception {

        String status = svTestDriver2.verifyR7MessageDestinationRefBindingNameOverLookup();
        assertEquals("PASS", status);
    }

/////////////////////// binding-name over lookup-name variations /////////////////////
//
// Verify that the binding-name in ibm-ejb-jar-bnd.xml takes precedence over the
// corresponding lookup-name attribute in ejb-jar.xml

    /**
     * R8.
     * Annotate a bean's instance var with:
     *
     * @Resource(name="ResourceRef_DataSource_BindingNameOverLookupName")
     *                                                                    javax.sql.DataSource ivResourceRef_DataSource_BindingNameOverLookupName;
     *
     *                                                                    Define a <resource-ref> in ejb-jar.xml
     *                                                                    with <res-ref-name>ResourceRef_DataSource_BindingNameOverLookupName
     *                                                                    and <lookup-name>eis/bogusDataSource
     *
     *                                                                    Define a <resource-ref name="ResourceRef_DataSource_BindingNameOverLookupName" in ibm-ejb-jar-bnd.xml with
     *                                                                    binding-name="jdbc/DefaultEJBTimerDataSource"
     *
     *                                                                    - Verify that the existing valid datasource is injected into
     *                                                                    ivResourceRef_DataSource_BindingNameOverLookupName.
     *                                                                    - Verify that the existing valid datasource can be explicitly looked up by name
     *                                                                    "ResourceRef_DataSource_BindingNameOverLookupName".
     */
    @Test
    public void testR8ResourceRefBindingNameOverLookupName() throws Exception {

        String status = svTestDriver2.verifyR8ResourceRefBindingNameOverLookupName();
        assertEquals("PASS", status);
    }

    /**
     * R9.
     * Annotate a bean's instance var with:
     *
     * @Resource(name="ResourceEnvRef_Queue_BindingNameOverLookupName")
     *                                                                  Queue ResourceEnvRef_Queue_BindingNameOverLookupName;
     *
     *                                                                  Define a <resource-env-ref> in ejb-jar.xml with
     *                                                                  <res-env-ref-name>ResourceEnvRef_Queue_BindingNameOverLookupName
     *                                                                  and <lookup-name>bogusResourceEnvRef
     *
     *                                                                  Define a <resource-env-ref name="ResourceEnvRef_Queue_BindingNameOverLookupName" in ibm-ejb-jar-bnd.xml with
     *                                                                  binding-name="java:comp/env/jms/ResponseQueue"
     *
     *                                                                  - Verify that the existing valid String is injected into ivResourceEnvRef_Queue_BindingNameOverLookupName.
     *                                                                  - Verify that the bound String can be explicitly looked up by name
     *                                                                  "ResourceEnvRef_Queue_BindingNameOverLookupName".
     */
    @Test
    public void testR9ResourceEnvRefBindingNameOverLookupName() throws Exception {

        String status = svTestDriver2.verifyR9ResourceEnvRefBindingNameOverLookupName();
        assertEquals("PASS", status);
    }

    /**
     * R10.
     * Bind a message destination into the Global namespace by name "Jetstream/jms/RequestQueue" (via TestBuild.xml)
     *
     * Annotate a bean's instance var with:
     *
     * @Resource(name="MessageDestinationRef_BindingNameOverLookupName")
     *
     *                                                                   Define a <message-destination-ref> in ejb-jar.xml with
     *                                                                   <msg-destination-ref-name>DestinationRef_BindingNameOverLookupName
     *                                                                   and with <lookup-name>bogusMessageDestinationRef
     *
     *                                                                   Define a <message-destination-ref in ibm-ejb-jar-bnd.xml with
     *                                                                   name="MessageDestinationRef_BindingNameOverLookupName"
     *                                                                   and with binding-name="Jetstream/jms/RequestQueue"
     *
     *                                                                   - Verify that the binding-name value takes precedence over the <lookup-name> value.
     *                                                                   - Verify that the message destination can be explicitly looked up by name
     *                                                                   "MessageDestinationRef_BindingNameOverLookupName".
     */
    @Test
    public void testR10MessageDestinationRefBindingNameOverLookupName() throws Exception {

        String status = svTestDriver2.verifyR10MessageDestinationRefBindingNameOverLookupName();
        assertEquals("PASS", status);
    }

/////////////////////// lookup-name over lookup variations /////////////////////

    /**
     * R11.
     * Annotate a bean's instance var with:
     *
     * @Resource(name="ResourceRef_DataSource_LookupNameOverLookup", lookup="eis/BogusDataSource")
     *                                                               javax.sql.DataSource ivResourceRef_DataSource_LookupNameOverLookup;
     *
     *                                                               Define a <resource-ref> in ejb-jar.xml with
     *                                                               <res-ref-name>ResourceRef_DataSource_LookupNameOverLookup
     *                                                               and <lookup-name>jdbc/DefaultEJBTimerDataSource
     *
     *                                                               - Verify that the existing valid DataSource is injected into ivResourceRef_DataSource_LookupNameOverLookup.
     *                                                               - Verify that the existing valid datasource can be explicitly looked up by name
     *                                                               "ResourceRef_DataSource_LookupNameOverLookup".
     */
    @Test
    public void testR11ResourceRefLookupNameOverLookup() throws Exception {

        String status = svTestDriver2.verifyR11ResourceRefLookupNameOverLookup();
        assertEquals("PASS", status);
    }

    /**
     * R12.
     * Annotate a bean's instance var with:
     *
     * @Resource(name="ResourceEnvRef_Queue_LookupNameOverLookup", lookup="bogusEnvref")
     *                                                             Queue ivResourceEnvRef_Queue_LookupNameOverLookup;
     *
     *                                                             Define a <resource-env-ref> ejb-jar.xml, with
     *                                                             <resource-env-ref-name>ResourceEnvRef_Queue_LookupNameOverLookup
     *                                                             and <lookup-name>java:comp/env/jms/ResponseQueue
     *
     *                                                             - Verify that the Queue is injected into ivResourceEnvRef_Queue_LookupNameOverLookup.
     *                                                             - Verify that the Queue can be explicitly looked up by name "ResourceEnvRef_Queue_LookupNameOverLookup".
     */
    @Test
    public void testR12ResourceEnvRefLookupNameOverLookup() throws Exception {

        String status = svTestDriver2.verifyR12ResourceEnvRefLookupNameOverLookup();
        assertEquals("PASS", status);
    }

    /**
     * R13.
     * Bind a message destination into the Global namespace by name "Jetstream/jms/RequestQueue" (via TestBuild.xml)
     *
     * Annotate a bean's instance var with:
     *
     * @Resource(name="MessageDestinationRef_LookupNameOverLookup", lookup="destref/bogus")
     *                                                              String ivMessageDestinationRef_LookupNameOverLookup;
     *
     *                                                              Define a <message-destination-ref> in ejb-jar.xml, with
     *                                                              <message-destination-ref-name>MessageDestinationRef_LookupNameOverLookup
     *                                                              and <lookup-name>"destref/bogus"
     *
     *                                                              - Verify that the message destination is injected into ivMessageDestinationRef_LookupNameOverLookup.
     *                                                              - Verify that the message destination can be explicitly looked up by name
     *                                                              "MessageDestinationRef_LookupNameOverLookup".
     */
    @Test
    public void testR13MessageDestinationLookupNameOverLookup() throws Exception {

        String status = svTestDriver2.verifyR13MessageDestinationLookupNameOverLookup();
        assertEquals("PASS", status);
    }

    /**
     * R20.
     * Annotate a bean's instance var with:
     * @Resource(name="EnvEntry_Integer_nameWithLookup", lookup="EnvEntry_IntegerAlternate").
     *
     * Define an <env-entry> in ejb-jar.xml,
     * with <env-entry-name>EnvEntry_IntegerAlternate
     * and with <env-entry-value>
     *
     * - Verify that the <env-entry-value> is injected into ivEnvEntry_Integer.
     * - Verify that the Integer can be explicitly looked up by name "EnvEntry_Integer_nameWithLookup".
     */
    @Test
    public void testR14NameAndLookupOnEnvEntry() throws Exception {

        String status = svTestDriver2.verifyR14NameAndLookupOnEnvEntry();
        assertEquals("PASS", status);
    }

    private static void bindObject(Context ctx, String name, Object value) throws NamingException {
        try {
            ctx.bind(name, value);
            svLogger.info("bound " + value + " by name <" + name + ">");
        } catch (NameAlreadyBoundException e) {
            svLogger.log(Level.INFO, "already bound <" + name + ">", e);
        }
    }
}
