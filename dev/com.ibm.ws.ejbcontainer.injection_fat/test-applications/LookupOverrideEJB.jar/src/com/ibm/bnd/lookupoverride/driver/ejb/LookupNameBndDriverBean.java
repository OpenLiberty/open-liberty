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

package com.ibm.bnd.lookupoverride.driver.ejb;

import static com.ibm.websphere.ejbcontainer.test.tools.FATHelper.lookupLocalBinding;

import java.sql.SQLException;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.bnd.lookupoverride.shared.Bad;
import com.ibm.bnd.lookupoverride.shared.LookupNameBndDriver;

/**
 * Singleton session bean that acts as a test driver in the server process for
 * the AppInstallChgDefBndTest client container test.
 * <p>
 **/
@Singleton
@Remote(LookupNameBndDriver.class)
public class LookupNameBndDriverBean implements LookupNameBndDriver {

    private static final String CLASS_NAME = LookupNameBndDriverBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String CONFLICTING_BEANNAME_LOOKUP_ATTRIBUTE_VALUES = "CWNEN0054E";
    private static final String CONFLICTING_EJBLINK_LOOKUPNAME_ELEMENT_VALUES = "Conflicting ejb-link/lookup-name element values exist for multiple ejb-local-ref elements with the same ejb-ref-name element value";
    private static final String FIELD_ANNOTATIONS_IGNORED = "CWNEN0047W";

    private static final String BAD1BEAN_JNDI = "java:global/LookupOverrideApp/DoaApp1/Bad1Bean!com.ibm.bnd.lookupoverride.shared.Bad";
    private static final String BAD2BEAN_JNDI = "java:global/LookupOverrideApp/DoaApp2/Bad2Bean!com.ibm.bnd.lookupoverride.shared.Bad";
    private static final String BAD3BEAN_JNDI = "java:global/LookupOverrideApp/DoaApp3/Bad3Bean!com.ibm.bnd.lookupoverride.shared.Bad";
    private static final String BAD4BEAN_JNDI = "java:global/LookupOverrideApp/DoaApp4/Bad4Bean!com.ibm.bnd.lookupoverride.shared.Bad";
    private static final String BAD5BEAN_JNDI = "java:global/LookupOverrideApp/DoaApp5/Bad5Bean!com.ibm.bnd.lookupoverride.shared.Bad";
    private static final String BAD6BEAN_JNDI = "java:global/LookupOverrideApp/DoaApp6/Bad6Bean!com.ibm.bnd.lookupoverride.shared.Bad";

    // Do an instance level injection of the SessionContext
    // This will cause the SessionContext, which contains the environment for this bean,
    // to get loaded into the variable.
    @Resource
    SessionContext ivContext;

    // test E1
    private static final String NAME_VALUE = "SLT"; // relative to java:comp/env ENC
    private static final String LOOKUP_VALUE = "ejblocal:SLT";
    @EJB(name = NAME_VALUE, lookup = LOOKUP_VALUE)
    SimpleLookupTarget ivEjb;

    @Override
    public String verifyE1LookupWithNoOtherBindings() {
        String retVal = "nothing done";

        // use implicit EJB reference annotated with @EJB to call a method on the bean:
        retVal = callPong(ivEjb, 55);

        if (retVal.equals("PASS")) {

            // do an explicit JNDI lookup using the @EJB lookup= value:
            retVal = lookupBeanAndCallMethod(LOOKUP_VALUE, 55);

            if (retVal.equals("PASS")) {

                // do an explicit JNDI lookup using the @EJB name= value:
                try {
                    SimpleLookupTarget ejb = (SimpleLookupTarget) ivContext.lookup(NAME_VALUE);
                    retVal = callPong(ejb, 55);
                } catch (Exception e) {
                    retVal = "Failure in explicit lookup using name value: " + NAME_VALUE;
                }

            }

        }

        return retVal;

    }

    // test E2
    private static final String NAME_VALUE2 = "SLT2"; // relative to java:comp/env ENC
    @EJB(name = NAME_VALUE2, lookup = LOOKUP_VALUE) // lookup will be overridden by binding-name
    SimpleLookupTarget ivEjb2;

    @Override
    public String verifyE2BindingNameOverLookup() {
        String retVal = "nothing done";

        // use implicit EJB reference annotated with @EJB to call a method on the bean:
        retVal = callPong(ivEjb2, 65);

        if (retVal.equals("PASS")) {

            // do an explicit JNDI lookup using the @EJB name= value:
            try {
                SimpleLookupTarget ejb = (SimpleLookupTarget) ivContext.lookup(NAME_VALUE2);

                // verify that the looked-up bean "equals" the injected bean:
                if (!ejb.equals(ivEjb2)) {
                    retVal = "looked-up bean by name " + NAME_VALUE2 + " not equal to injected bean";
                }

                // for good measure, call a method to verify the right implementation gets executed:
                retVal = callPong(ejb, 65);
            } catch (Exception e) {
                retVal = "Failure in explicit lookup using name value: " + NAME_VALUE2;
            }

        }

        return retVal;

    }

    // test E3
    @Override
    public String verifyE3BindingNameOverLookupName() {
        String retVal = "nothing done";

        // do an explicit JNDI lookup using the common <ejb-ref-name> and
        // <ejb-ref>  name= value:
        try {
            SimpleLookupTarget ejb = (SimpleLookupTarget) ivContext.lookup("SLT3");
            retVal = callPong(ejb, 65);
        } catch (Exception e) {
            retVal = "Failure in explicit lookup using name value: " + "SLT3";
        }

        return retVal;

    }

    // test E3.5
    @Override
    public String verifyE35BindingNameOverLookupNameRemote() {
        String retVal = "nothing done";

        // do an explicit JNDI lookup using the common <ejb-ref-name> and
        // <ejb-ref>  name= value:
        try {
            SimpleLookupTarget ejb = (SimpleLookupTarget) ivContext.lookup("SLT3Remote");
            retVal = callPong(ejb, 78);
        } catch (Exception e) {
            retVal = "Failure in explicit lookup using name value: " + "SLT3Remote";
        }

        return retVal;

    }

    // test E4.5
    private static final String NAME_VALUE4 = "SLT4"; // relative to java:comp/env ENC
    @EJB(name = NAME_VALUE4, beanName = "Invalid", lookup = "Invalid")
    SimpleLookupTarget ivEjb45;

    @Override
    public String verifyE45BindingNameOverLookupAndBeanName() {
        String retVal = "nothing done";

        // do an explicit JNDI lookup using the common <ejb-ref-name> and
        // <ejb-ref>  name= value:
        try {
            SimpleLookupTarget ejb = (SimpleLookupTarget) ivContext.lookup(NAME_VALUE4);
            retVal = callPong(ejb, 55);
        } catch (Exception e) {
            retVal = "Failure in explicit lookup using name value: " + NAME_VALUE4;
        }

        return retVal;

    }

    // test E5
    @Override
    public String verifyE5ErrorLookupAndBeanNameMultiFields() {

        final String meth = "verifyE5ErrorLookupAndBeanNameMultiFields";
        String retVal = "nothing done";

        try {
            // lookup the bean:
            try {

                Bad bad2 = (Bad) new InitialContext().lookup(BAD2BEAN_JNDI);

                // Should not get this far, but in case we do, try to invoke a method on the looked-up bean:
                try {
                    int boingResult = bad2.boing();
                    retVal = "Unexpectedly returned from bad2.boing().  Returned value was " + boingResult;
                } catch (Exception e) {
                    retVal = "Caught Exception running method on bad2 bean: " + e;
                }

            } catch (Exception e) {

                retVal = "failure in explicit lookup";
                svLogger.info(meth + " caught Exception doing lookupLocalBinding(" + BAD2BEAN_JNDI + ") : " + e);

                if (e.getMessage().contains("InjectionConfigurationException")) {
                    return "PASS";
                }

                retVal = "Exception without expected " + CONFLICTING_BEANNAME_LOOKUP_ATTRIBUTE_VALUES;

            }

        } catch (EJBException ejbex) {
            ejbex.printStackTrace();
            throw ejbex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EJBException("Unexpected exception occurred : " + ex.getClass().getName(), ex);
        }

        return retVal;

    }

    // test E7
    @Override
    public String verifyE7ErrorLookupNameAndEjbLinkInterceptorAndBean() {

        final String meth = "verifyE7ErrorLookupNameAndEjbLinkInterceptorAndBean";
        String retVal = "nothing done";

        try {

            // lookup the bean:
            try {

                Bad bad = (Bad) new InitialContext().lookup(BAD5BEAN_JNDI);

                // Should not get this far, but in case we do, try to invoke a method on the looked-up bean:
                try {
                    int boingResult = bad.boing();
                    retVal = "Unexpectedly returned from bad.boing().  Returned value was " + boingResult;
                } catch (Exception e) {
                    retVal = "Caught Exception running method on bad5 bean: " + e;
                }

            } catch (Exception e) {

                retVal = "failure in explicit lookup";
                svLogger.info(meth + " caught Exception doing lookupLocalBinding(" + BAD5BEAN_JNDI + ") : " + e);

                // Make sure the correct error message was logged
                if (e.getMessage().contains("InjectionConfigurationException")) {
                    return "PASS";
                }

                retVal = "Exception without expected " + CONFLICTING_BEANNAME_LOOKUP_ATTRIBUTE_VALUES;

            }

        } catch (EJBException ejbex) {
            ejbex.printStackTrace();
            throw ejbex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EJBException("Unexpected exception occurred : " + ex.getClass().getName(), ex);
        }
        return retVal;

    }

    // test E7.5
    @Override
    public String verifyE75ErrorLookupAndBeanNameInterceptorAndBean() {

        final String meth = "verifyE75ErrorLookupAndBeanNameInterceptorAndBean";
        String retVal = "nothing done";

        try {

            // lookup the bean:
            try {

                Bad bad = (Bad) new InitialContext().lookup(BAD4BEAN_JNDI);

                // Should not get this far, but in case we do, try to invoke a method on the looked-up bean:
                try {
                    int boingResult = bad.boing();
                    retVal = "Unexpectedly returned from bad.boing().  Returned value was " + boingResult;
                } catch (Exception e) {
                    retVal = "Caught Exception running method on bad3 bean: " + e;
                }

            } catch (Exception e) {

                retVal = "failure in explicit lookup";
                svLogger.info(meth + " caught Exception doing lookupLocalBinding(" + BAD4BEAN_JNDI + ") : " + e);

                // Make sure the correct error message was logged
                if (e.getMessage().contains("InjectionConfigurationException")) {
                    return "PASS";
                }

                retVal = "Exception without expected " + "CNTR4006E";

            }

        } catch (EJBException ejbex) {
            ejbex.printStackTrace();
            throw ejbex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EJBException("Unexpected exception occurred : " + ex.getClass().getName(), ex);
        }
        return retVal;

    }

    // test E8
    @EJB(name = "SLT8", lookup = "ejblocal:SLT") // lookup will be overridden by <lookup-name>
    SimpleLookupTarget ivEjb8;

    @Override
    public String verifyE8LookupNameOverLookup() {
        String retVal = "nothing done";

        // do an explicit JNDI lookup using the @EJB name= value:
        try {
            SimpleLookupTarget ejb = (SimpleLookupTarget) ivContext.lookup("SLT8");
            retVal = callPong(ejb, 65);
        } catch (Exception e) {
            retVal = "Failure in explicit lookup using name value: " + "SLT8";
        }

        return retVal;

    }

    // test E10
    @EJB(name = "SLT10", beanName = "bogusBeanName") // beanName will be overridden by <lookup-name>
    SimpleLookupTarget ivEjb10;

    @Override
    public String verifyE10LookupNameOverBeanName() {
        String retVal = "nothing done";

        // do an explicit JNDI lookup using the @EJB name= value:
        try {
            SimpleLookupTarget ejb = (SimpleLookupTarget) ivContext.lookup("SLT10");
            retVal = callPong(ejb, 55);
        } catch (Exception e) {
            retVal = "Failure in explicit lookup using name value: " + "SLT10";
        }

        return retVal;

    }

    // test E11
    private static final String NAME_VALUE11 = "SLT11"; // relative to java:comp/env ENC
    private static final String BEAN_NAME_VALUE11 = "ejblocal:bogus"; // non-existing bean
    @EJB(name = NAME_VALUE11, beanName = BEAN_NAME_VALUE11) // beanName will be overridden by <ejb-link>
    SimpleLookupTarget ivEjb11;

    @Override
    public String verifyE11EjbLinkOverBeanName() {
        String retVal = "nothing done";

        // use implicit EJB reference annotated with @EJB to call a method on the bean:
        retVal = callPong(ivEjb11, 65);

        if (retVal.equals("PASS")) {

            // do an explicit JNDI lookup using the @EJB name= value:
            try {

                SimpleLookupTarget ejb11 = (SimpleLookupTarget) ivContext.lookup(NAME_VALUE11);

                // verify that the looked-up bean "equals" the injected bean:
                if (!ejb11.equals(ivEjb11)) {
                    retVal = "looked-up bean by name " + NAME_VALUE11 + " not equal to injected bean";
                } else {
                    // for good measure, call a method to verify the right implementation gets executed:
                    retVal = callPong(ejb11, 65);
                }

            } catch (Exception e) {
                retVal = "Failure in explicit lookup using name value: " + NAME_VALUE11;
            }

        }

        return retVal;

    }

    // test E12
    // 641396
    @Override
    public String verifyE12MissingClassThwartsInjection() {

        final String meth = "verifyE12MissingClassThwartsInjection";

        String retVal = "nothing done";

        try {

            // lookup the bean:
            try {

                Bad bad = (Bad) new InitialContext().lookup(BAD6BEAN_JNDI);

                // Invoke a method on the looked-up bean:
                try {
                    int boingResult = bad.boing();
                    if (boingResult == 66) {
                        retVal = "PASS";
                    } else {
                        retVal = "Expected 66 returned from bad.boing().  Returned value was " + boingResult;
                    }
                } catch (Exception e) {
                    retVal = "Caught Exception running method on bad6 bean: " + e;
                }

            } catch (Exception e) {

                retVal = "failure in explicit lookup";
                svLogger.info(meth + " caught Exception doing lookupLocalBinding(" + BAD6BEAN_JNDI + ") : " + e);

                retVal = "Exception without expected " + FIELD_ANNOTATIONS_IGNORED;

            }

        } catch (EJBException ejbex) {
            ejbex.printStackTrace();
            throw ejbex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EJBException("Unexpected exception occurred : " + ex.getClass().getName(), ex);
        }
        return retVal;

    }

    //////////////////////////////////////////////////////////////////////

    // test R0
    @Resource(name = "EnvEntry_Integer")
    Integer ivEnvEntry_Integer;

    @Override
    public String verifyR0EnvEntry() {
        String retVal = "uninitialized retVal";

        // use implicit env-entry for an Integer field annotated with @Resource to dereference the Integer
        if (ivEnvEntry_Integer.equals(22)) {
            retVal = "PASS";
        } else {
            retVal = "Unexpected Integer injection.  Expected 22 and got " + ivEnvEntry_Integer;
        }

        if (retVal.equals("PASS")) {
            // if ok so far, do an explicit lookup:
            Integer envEntry_Integer = (Integer) ivContext.lookup("java:comp/env/EnvEntry_Integer");
            if (envEntry_Integer.equals(22)) {
                retVal = "PASS";
            } else {
                retVal = "Unexpected value from looked up env-entry.  Expected 22 and got " + envEntry_Integer;
            }
        }

        return retVal;

    }

    // test R1
    @Resource(name = "EnvEntry_IntegerBound", lookup = "envEntryIntegerBound")
    Integer ivEnvEntry_IntegerBound;

    @Override
    public String verifyR1EnvEntryLookup() {
        String retVal = "env-entry failure";

        // Integer assumed to have been bound during static initialization of class AppInstallChgDefBndTest

        // Explicitly look up the Integer:
        String jndiName = "java:comp/env/EnvEntry_IntegerBound";
        Integer envEntry_Integer = (Integer) ivContext.lookup(jndiName);
        if (envEntry_Integer != null) {
            if (envEntry_Integer.equals(ivEnvEntry_IntegerBound)) {
                // Verify that the expected value was retrieved:
                retVal = "annotated and looked-up Integers equal, but not the expected 601.  Value was: " + envEntry_Integer;
                if (envEntry_Integer == 601) {
                    retVal = "PASS";
                }
            } else {
                retVal = "annotated and looked-up Integers are NOT .equal()";
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }

        unbindObject("envEntryIntegerBound");

        return retVal;

    }

    // test R3
    @Resource(name = "ResourceRef_DataSource_Lookup", lookup = "jdbc/TestDataSource")
    javax.sql.DataSource ivResourceRef_DataSource_Lookup;

    @Override
    public String verifyR3ResourceRefLookup() {
        String retVal = "resource-ref failure";

        // Explicitly lookup the datasource:
        String jndiName = "java:comp/env/ResourceRef_DataSource_Lookup";

        javax.sql.DataSource ds = (javax.sql.DataSource) ivContext.lookup(jndiName);

        if (ds != null) {

            // Ensure that the looked-up and injected datasources are the same:
            try {
                if (ds.getConnection().getMetaData().getURL().equals(ivResourceRef_DataSource_Lookup.getConnection().getMetaData().getURL())) {
                    retVal = "PASS";
                } else {
                    retVal = "annotated and looked-up datasources are NOT .equal()";
                }
            } catch (SQLException e) {
                retVal = "exception getting datasource info";
                e.printStackTrace();
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }

        return retVal;

    }

    // test R4

    String Qmessage = "Not set";

    // XML-based field injection
    public QueueConnectionFactory queueConnectionFactory;

    @Resource(name = "MessageDestinationRef_RequestQueueLookup", lookup = "Jetstream/jms/RequestQueue")
    Queue ivMessageDestinationRef_RequestQueueLookup;

    @Override
    public String verifyR4MessageDestinationRefLookup() {
        String retVal = "uninitialized retVal";

        // Explicitly look up the message destination ref:
        javax.jms.Queue dest = (javax.jms.Queue) ivContext.lookup("MessageDestinationRef_RequestQueueLookup");

        if (dest.equals(ivMessageDestinationRef_RequestQueueLookup)) {
            // Ensure that the looked-up and injected message destinations are the same:
            retVal = "PASS";
        } else {
            retVal = "annotated and looked-up message destinations are NOT .equal()";
            svLogger.info("  annotated destination : " + ivMessageDestinationRef_RequestQueueLookup);
            svLogger.info("  looked-up destination : " + dest);
        }

        return retVal;

    }

    // test R5
    @Resource(name = "ResourceRef_DataSource_BindingNameOverLookup", lookup = "eis/BogusDataSource")
    javax.sql.DataSource ivResourceRef_DataSource_BindingNameOverLookup;

    @Override
    public String verifyR5ResourceRefBindingNameOverLookup() {
        String retVal = "resource-ref failure";

        // Explicitly lookup the datasource:
        String jndiName = "java:comp/env/ResourceRef_DataSource_BindingNameOverLookup";
        javax.sql.DataSource ds = (javax.sql.DataSource) ivContext.lookup(jndiName);

        if (ds != null) {
            // Ensure that the looked-up and injected datasources are the same:
            try {
                if (ds.getConnection().getMetaData().getURL().equals(ivResourceRef_DataSource_BindingNameOverLookup.getConnection().getMetaData().getURL())) {
                    retVal = "PASS";
                } else {
                    retVal = "annotated and looked-up datasources are NOT .equal()";
                }
            } catch (SQLException e) {
                retVal = "exception getting datasource info";
                e.printStackTrace();
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }

        return retVal;

    }

    // test R6
    @Resource(name = "ResourceEnvRef_Queue_BindingNameOverLookup", lookup = "BogusEnvRef")
    Queue ivResourceEnvRef_Queue_BindingNameOverLookup;

    @Override
    public String verifyR6ResourceEnvRefBindingNameOverLookup() {
        String retVal = "uninitialized retVal";

        // Explicitly lookup the string:
        String jndiName = "ResourceEnvRef_Queue_BindingNameOverLookup";
        Queue s = (Queue) ivContext.lookup(jndiName);

        if (s != null) {
            // Ensure that the looked-up and injected Queues are the same:
            if (s.equals(ivResourceEnvRef_Queue_BindingNameOverLookup)) {
                retVal = "PASS";
            } else {
                retVal = "annotated and looked-up queues are NOT .equal()";
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }

        return retVal;

    }

    // test R7
    @Resource(name = "MessageDestinationRef_BindingNameOverLookup", lookup = "BogusMessageDestinationRef")
    Queue ivMessageDestinationRef_BindingNameOverLookup;

    @Override
    public String verifyR7MessageDestinationRefBindingNameOverLookup() {
        String retVal = "uninitialized retVal";

        // Explicitly lookup the string:
        String jndiName = "MessageDestinationRef_BindingNameOverLookup";
        Queue reqQ = (Queue) ivContext.lookup(jndiName);

        if (reqQ != null) {
            // Ensure that the looked-up and injected Queues are the same:
            if (reqQ.equals(ivMessageDestinationRef_BindingNameOverLookup)) {
                retVal = "PASS";
            } else {
                retVal = "annotated and looked-up queues are NOT .equal()";
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }

        return retVal;

    }

    // test R8
    @Resource(name = "ResourceRef_DataSource_BindingNameOverLookupName")
    javax.sql.DataSource ivResourceRef_DataSource_BindingNameOverLookupName;

    @Override
    public String verifyR8ResourceRefBindingNameOverLookupName() {
        String retVal = "uninitialized retVal";

        // Explicitly lookup the datasource:
        String jndiName = "java:comp/env/ResourceRef_DataSource_BindingNameOverLookupName";
        javax.sql.DataSource ds = (javax.sql.DataSource) ivContext.lookup(jndiName);

        if (ds != null) {
            // Ensure that the looked-up and injected datasources are the same:
            try {
                if (ds.getConnection().getMetaData().getURL().equals(ivResourceRef_DataSource_BindingNameOverLookupName.getConnection().getMetaData().getURL())) {
                    retVal = "PASS";
                } else {
                    retVal = "annotated and looked-up datssources are NOT .equal()";
                }
            } catch (SQLException e) {
                retVal = "exception getting datasource info";
                e.printStackTrace();
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }

        return retVal;

    }

    // test R9
    @Resource(name = "ResourceEnvRef_Queue_BindingNameOverLookupName")
    Queue ivResourceEnvRef_Queue_BindingNameOverLookupName;

    @Override
    public String verifyR9ResourceEnvRefBindingNameOverLookupName() {
        String retVal = "uninitialized retVal";

        // Explicitly lookup the Queue:
        String jndiName = "java:comp/env/ResourceEnvRef_Queue_BindingNameOverLookupName";
        Queue resEnvRef = (Queue) ivContext.lookup(jndiName);

        if (resEnvRef != null) {
            // Ensure that the looked-up and injected Queues are the same:
            if (resEnvRef.equals(ivResourceEnvRef_Queue_BindingNameOverLookupName)) {
                retVal = "PASS";
            } else {
                retVal = "annotated and looked-up queues are NOT .equal()";
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }
        return retVal;
    }

    // test R10
    @Resource(name = "MessageDestinationRef_BindingNameOverLookupName")
    Queue ivMessageDestinationRef_BindingNameOverLookupName;

    @Override
    public String verifyR10MessageDestinationRefBindingNameOverLookupName() {
        String retVal = "uninitialized retVal";

        // Explicitly lookup the Queue:
        String jndiName = "java:comp/env/MessageDestinationRef_BindingNameOverLookupName";
        Queue mdr = (Queue) ivContext.lookup(jndiName);

        if (mdr != null) {
            // Ensure that the looked-up and injected Queues are the same:
            if (mdr.equals(ivMessageDestinationRef_BindingNameOverLookupName)) {
                retVal = "PASS";
            } else {
                retVal = "annotated and looked-up Strings are NOT .equal()";
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }
        return retVal;
    }

    // test R11
    @Resource(name = "ResourceRef_DataSource_LookupNameOverLookup", lookup = "eis/BogusDataSource")
    javax.sql.DataSource ivResourceRef_DataSource_LookupNameOverLookup;

    @Override
    public String verifyR11ResourceRefLookupNameOverLookup() {
        String retVal = "uninitialized retVal";

        // Explicitly lookup the datasource:
        String jndiName = "java:comp/env/ResourceRef_DataSource_LookupNameOverLookup";
        javax.sql.DataSource ds = (javax.sql.DataSource) ivContext.lookup(jndiName);

        if (ds != null) {
            // Ensure that the looked-up and injected datasources are the same:
            try {
                if (ds.getConnection().getMetaData().getURL().equals(ivResourceRef_DataSource_LookupNameOverLookup.getConnection().getMetaData().getURL())) {
                    retVal = "PASS";
                } else {
                    retVal = "annotated and looked-up datasources are NOT .equal()";
                }
            } catch (SQLException e) {
                retVal = "exception getting datasource info";
                e.printStackTrace();
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }
        return retVal;
    }

    // test R12
    @Resource(name = "ResourceEnvRef_Queue_LookupNameOverLookup", lookup = "bogusEnvref")
    Queue ivResourceEnvRef_Queue_LookupNameOverLookup;

    @Override
    public String verifyR12ResourceEnvRefLookupNameOverLookup() {

        String retVal = "uninitialized retVal";

        // Explicitly lookup the Queue:
        String jndiName = "java:comp/env/ResourceEnvRef_Queue_LookupNameOverLookup";
        Queue resEnvRef = (Queue) ivContext.lookup(jndiName);

        if (resEnvRef != null) {
            // Ensure that the looked-up and injected Queues are the same:
            if (resEnvRef.equals(ivResourceEnvRef_Queue_LookupNameOverLookup)) {
                retVal = "PASS";
            } else {
                retVal = "annotated and looked-up queues are NOT .equal()";
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }
        return retVal;
    }

    // test R13
    @Resource(name = "MessageDestinationRef_LookupNameOverLookup", lookup = "destref/bogus")
    Queue ivMessageDestinationRef_LookupNameOverLookup;

    @Override
    public String verifyR13MessageDestinationLookupNameOverLookup() {

        String retVal = "uninitialized retVal";

        // Explicitly lookup the Queue:
        String jndiName = "java:comp/env/MessageDestinationRef_LookupNameOverLookup";
        Queue reqQ = (Queue) ivContext.lookup(jndiName);

        if (reqQ != null) {
            // Ensure that the looked-up and injected Queues are the same:
            if (reqQ.equals(ivMessageDestinationRef_LookupNameOverLookup)) {
                retVal = "PASS";
            } else {
                retVal = "annotated and looked-up queues are NOT .equal()";
            }
        } else {
            retVal = "looked up value for " + jndiName + " was null";
        }
        return retVal;
    }

    // R14
    @Resource(name = "EnvEntry_Integer_nameWithLookup", lookup = "EnvEntry_IntegerAlternate")
    Integer ivEnvEntry_nameAndLookup_Integer;

    @Override
    public String verifyR14NameAndLookupOnEnvEntry() {

        String retVal = "nothing done";

        try {

            // reference the field annotated with the env-entry value:
            if (ivEnvEntry_nameAndLookup_Integer.equals(333)) {
                retVal = "PASS";
            } else {
                retVal = String.valueOf(ivEnvEntry_nameAndLookup_Integer);
            }
        } catch (EJBException ejbex) {
            ejbex.printStackTrace();
            throw ejbex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EJBException("Unexpected exception occurred : " + ex.getClass().getName(), ex);
        }
        return retVal;
    }

    private String lookupBeanAndCallMethod(String name, int expected) {

        try {
            SimpleLookupTarget ejb = (SimpleLookupTarget) lookupLocalBinding(name);
            return callPong(ejb, expected);
        } catch (Exception e) {
            return "Failure in explicit lookup using lookup value: " + name;
        }

    }

    private String callPong(SimpleLookupTarget ejb, int expected) {

        try {
            int pongResult = ejb.pong();
            if (pongResult == expected)
                return "PASS";
            return "Expected " + expected + " but got " + pongResult;
        } catch (Exception e) {
            return "Caught Exception running method pong() on bean reference: " + e;
        }

    }

    private static void unbindObject(String name) {

        // unbind an Object from the global JNDI namespace:
        InitialContext initialCtx;
        try {
            initialCtx = new InitialContext();
        } catch (NamingException e1) {
            svLogger.info("caught NamingException getting InitialContext()");
            initialCtx = null;
        }

        try {
            initialCtx.unbind(name);
        } catch (NamingException e) {
            svLogger.info("caught NamingException unbinding " + name);
        }

    }

}
