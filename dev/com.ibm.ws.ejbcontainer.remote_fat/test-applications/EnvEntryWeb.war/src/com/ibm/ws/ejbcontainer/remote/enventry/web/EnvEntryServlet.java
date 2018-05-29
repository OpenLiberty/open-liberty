/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.remote.enventry.web;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.remote.enventry.ejb.EnvEntryClass;
import com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver;
import com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver.EnvEntryEnum;

import componenttest.app.FATServlet;

/**
 * Verifies that <env-entry> elements with env-entry-type of java.lang.Class, or subclass of Enum have
 * their env-entry-value injected, via the <injection-target>, or via @Resource injection.
 * <p>
 *
 * <b>Test Matrix:</b>
 * <p>
 * <ul>
 * XML-only variations: {@link #testC1xEnvEntryClass} - env-entry-value is injected {@link #testC2xEnvEntryNonExistingClass} - error results from non-existing Class in
 * env-entry-value {@link #testE1xEnvEntryEnum} - env-entry-value is injected {@link #testE2xEnvEntryNonExistingEnumType} - error results from non-existing Enum type in
 * env-entry-type {@link #testE3xEnvEntryNonExistingEnumValue} - error results from non-existing Enum value in env-entry-value {@link #testE4xEnvEntryExistingNonEnumNonClass} -
 * error results from existing but unsupported type
 *
 * Variations using @Resource annotation: {@link #testC1aEnvEntryClass} - env-entry-value is injected {@link #testC2aEnvEntryNonExistingClass} - error results from non-existing
 * Class in env-entry-value {@link #testC3aEnvEntryLookupClass} - env-entry-value is injected, using annotation lookup= {@link #testE1aEnvEntryEnum} - env-entry-value is injected
 * {@link #testE2aEnvEntryNonExistingEnumType} - error results from non-existing Enum type in env-entry-type {@link #testE3aEnvEntryNonExistingEnumValue} - error results from
 * non-existing Enum value in env-entry-value {@link #testE4aEnvEntryExistingNonEnumNonClass} - error results from existing but unsupported type
 * {@link #testE4aEnvEntryExistingNonEnumNonClass} - env-entry-value is injected, using annotation lookup=
 * </ul>
 */
@SuppressWarnings("serial")
@WebServlet("/EnvEntryServlet")
public class EnvEntryServlet extends FATServlet {
    private static final String CLASS_NAME = EnvEntryServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Simplicity Setup variables (only needs to be done once)
    private static EnvEntryDriver svXmlTestDriver = null;
    private static EnvEntryDriver svAnnTestDriver = null;

    static {
        // For variation C3a, bind a Class into the Global namespace by name "envEntryClassBound".
        EnvEntryClass eec = new EnvEntryClass();
        Class<?> classToBind = eec.getClass();
        bindObject("envEntryClassBound", classToBind);

        // For variation E5a, bind an Enum into the Global namespace by name "envEntryEnumBound".
        bindObject("envEntryEnumBound", EnvEntryEnum.EV3);
    }

    @PostConstruct
    private void oneTimeSetUp() {
        // Obtain the naming context for use by the tests
        try {
            svLogger.info("creating InitialContext");
            InitialContext initCtx = new InitialContext();

            if (svXmlTestDriver == null) {
                // Lookup the Xml test driver EJB
                Object obj = initCtx.lookup("java:app/EnvEntryEJB/EnvEntryXmlDriverBean");
                svXmlTestDriver = (EnvEntryDriver) PortableRemoteObject.narrow(obj, EnvEntryDriver.class);
            }

            if (svAnnTestDriver == null) {
                // Lookup the Ann test driver EJB
                Object obj = initCtx.lookup("java:app/EnvEntryEJB/EnvEntryAnnDriverBean");
                svAnnTestDriver = (EnvEntryDriver) PortableRemoteObject.narrow(obj, EnvEntryDriver.class);
            }
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }

    ////////////                           ////////////
    ////////        XML-only variations        ////////
    ////////////                           ////////////

    /**
     * Define an <env-entry> in ejb-jar.xml:
     * <env-entry>
     * <description>C1x - class specified in XML only</description>
     * <env-entry-name>EnvEntry_Class_EntryName</env-entry-name>
     * <env-entry-type>java.lang.Class</env-entry-type>
     * <env-entry-value>com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryClass</env-entry-value>
     * <injection-target>
     * <injection-target-class>com.ibm.ws.ejbcontainer.remote.enventry.driver.ejb.EnvEntryXmlDriverBean</injection-target-class>
     * <injection-target-name>ivEnvEntry_Class</injection-target-name>
     * </injection-target>
     * </env-entry>
     *
     * - Verify that the <env-entry-value> is injected into Class<?> ivEnvEntry_Class;
     * - Verify that the Class<?> can be explicitly looked up by name "EnvEntry_Class_EntryName".
     */
    @Test
    public void testC1xEnvEntryClass() throws Exception {
        svXmlTestDriver.verifyC1EnvEntryClass();
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad1XmlBean:
     * <env-entry>
     * <description>C2x - Non-existent class specified in XML only</description>
     * <env-entry-name>EnvEntry_Non-existentClass_EntryName</env-entry-name>
     * <env-entry-type>java.lang.Class</env-entry-type>
     * <env-entry-value>com.ibm.ws.ejbcontainer.remote.enventry.shared.NoSuchClass</env-entry-value>
     * <injection-target>
     * <injection-target-class>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad1XmlBean</injection-target-class>
     * <injection-target-name>ivEnvEntry_NoSuchClass</injection-target-name>
     * </injection-target>
     * </env-entry>
     *
     * - Verify that an appropriate error CWNEN0011E is issued
     */
    //CALLED FROM TEST CLASS
    public void testC2xEnvEntryNonExistingClass() throws Exception {
        svXmlTestDriver.verifyC2EnvEntryNonExistingClass();
    }

    /**
     * Define an <env-entry> in ejb-jar.xml:
     * <env-entry>
     * <description>E1x - enum specified in XML only</description>
     * <env-entry-name>EnvEntry_Enum_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver$EnvEntryEnum</env-entry-type>
     * <env-entry-value>EV2</env-entry-value>
     * <injection-target>
     * <injection-target-class>com.ibm.ws.ejbcontainer.remote.enventry.driver.ejb.EnvEntryXmlDriverBean</injection-target-class>
     * <injection-target-name>ivEnvEntry_Enum</injection-target-name>
     * </injection-target>
     * </env-entry>
     *
     * - Verify that the <env-entry-value> is injected into Enum<?> ivEnvEntry_Enum;
     * - Verify that the Integer can be explicitly looked up by name "ivEnvEntry_Enum".
     */
    @Test
    public void testE1xEnvEntryEnum() throws Exception {
        svXmlTestDriver.verifyE1EnvEntryEnum();
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad2XmlBean:
     * <env-entry>
     * <description>E2x - Non-existent enum type specified in XML only</description>
     * <env-entry-name>EnvEntry_Non-existentEnumType_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.NoSuchEnumType</env-entry-type>
     * <env-entry-value>EV0</env-entry-value>
     * <injection-target>
     * <injection-target-class>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad2XmlBean</injection-target-class>
     * <injection-target-name>ivEnvEntry_NoSuchEnumType</injection-target-name>
     * </injection-target>
     * </env-entry>
     *
     * - Verify that an appropriate error is issued
     */
    //CALLED FROM TEST CLASS
    public void testE2xEnvEntryNonExistingEnumType() throws Exception {
        svXmlTestDriver.verifyE2EnvEntryNonExistingEnumType();
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad2XmlBean:
     * <env-entry>
     * <description>E3x - Non-existent enum value specified in XML only</description>
     * <env-entry-name>EnvEntry_Non-existentEnumValue_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver$EnvEntryEnum</env-entry-type>
     * <env-entry-value>NO_SUCH_ENUM_VALUE</env-entry-value>
     * <injection-target>
     * <injection-target-class>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad3XmlBean</injection-target-class>
     * <injection-target-name>ivEnvEntry_NoSuchEnumValue</injection-target-name>
     * </injection-target>
     * </env-entry>
     *
     * - Verify that an appropriate error is issued
     */
    //CALLED FROM TEST CLASS
    public void testE3xEnvEntryNonExistingEnumValue() throws Exception {
        svXmlTestDriver.verifyE3EnvEntryNonExistingEnumValue();
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad4XmlBean:
     * <env-entry>
     * <description>E4X - Existing class that is neither an Enum nor a Class; specified in XML only</description>
     * <env-entry-name>EnvEntry_ExistingNonEnumNonClass_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad4XmlBean</env-entry-type>
     * <env-entry-value>NOT_APPLICABLE</env-entry-value>
     * <injection-target>
     * <injection-target-class>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad4XmlBean</injection-target-class>
     * <injection-target-name>ivEnvEntry_ExistingNonEnumNonClass</injection-target-name>
     * </injection-target>
     * </env-entry>
     *
     * - Verify that an appropriate error is issued
     */
    //CALLED FROM TEST CLASS
    public void testE4xEnvEntryExistingNonEnumNonClass() throws Exception {
        svXmlTestDriver.verifyE4EnvEntryExistingNonEnumNonClass();
    }

    // Begin 661640
    @Test
    public void testP1xEnvEntryInteger() throws Exception {
        svXmlTestDriver.verifyP1EnvEntryInteger();
    }

    @Test
    public void testP2xEnvEntryInt() throws Exception {
        svXmlTestDriver.verifyP2EnvEntryInt();
    }

    @Test
    public void testP3xEnvEntryEnumQual() throws Exception {
        svXmlTestDriver.verifyP3EnvEntryEnumQual();
    }

    // End 661640

    ////////////                           ////////////
    ////////       Annotation variations       ////////
    ////////////                           ////////////

    /**
     * Define an <env-entry> in ejb-jar.xml:
     * <env-entry>
     * <description>C1a - Class specified in XML and @Resource annotation</description>
     * <env-entry-name>EnvEntry_Class_EntryName</env-entry-name>
     * <env-entry-type>java.lang.Class</env-entry-type>
     * <env-entry-value>com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryClass</env-entry-value>
     * </env-entry>
     *
     * Annotate a Class<?> instance var:
     *
     * @Resource(name="EnvEntry_Class_EntryName") Class<?> ivEnvEntry_Class;
     *
     *                                            - Verify that the <env-entry-value> is injected into Class<?> ivEnvEntry_Class;
     *                                            - Verify that the Class<?> can be explicitly looked up by name "EnvEntry_Class_EntryName".
     */
    @Test
    public void testC1aEnvEntryClass() throws Exception {
        svAnnTestDriver.verifyC1EnvEntryClass();
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad1Bean:
     * <env-entry>
     * <description>C2a - Non-existent class specified in XML, for @Resource annotation</description>
     * <env-entry-name>EnvEntry_Non-existentClass_EntryName</env-entry-name>
     * <env-entry-type>java.lang.Class</env-entry-type>
     * <env-entry-value>com.ibm.ws.ejbcontainer.remote.enventry.shared.NoSuchClass</env-entry-value>
     * </env-entry>
     * Annotate an Enum<?> instance var:
     *
     * @Resource(name="EnvEntry_Non-existentClass_EntryName") Enum<?> ivEnvEntry_NoSuchClass;
     *
     *                                                        - Verify that an appropriate error is issued
     */
    //CALLED FROM TEST CLASS
    public void testC2aEnvEntryNonExistingClass() throws Exception {
        svAnnTestDriver.verifyC2EnvEntryNonExistingClass();
    }

    /**
     * C3a
     * Bind a java.lang.Class object into the Global namespace by name "envEntryClassBound".
     * Define an <env-entry> in ejb-jar.xml:
     * <env-entry>
     * <description>C3a - Class specified in XML with @Resource lookup annotation</description>
     * <env-entry-name>EnvEntry_Class_Using_Lookup</env-entry-name>
     * <env-entry-type>java.lang.Class</env-entry-type>
     * <env-entry-value>com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryClass</env-entry-value>
     * </env-entry>
     *
     * Annotate a bean's instance var with:
     *
     * @Resource(name="EnvEntry_Class_Using_Lookup", lookup="envEntryClassBound")
     *                                               Class<?> ivEnvEntry_Lookup_Class;
     *
     *                                               - Verify that the bound Class object is injected into ivEnvEntry_Lookup_Class.
     *                                               - Verify that the bound Class object can be explicitly looked up by name "EnvEntry_Class_Using_Lookup".
     */
    @Test
    public void testC3aEnvEntryLookupClass() throws Exception {
        svAnnTestDriver.verifyC3EnvEntryLookupClass();
    }

    /**
     * Define an <env-entry> in ejb-jar.xml:
     * <env-entry>
     * <description>E1a - Enum specified in XML and @Resource annotation</description>
     * <env-entry-name>EnvEntry_Enum_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver$EnvEntryEnum</env-entry-type>
     * <env-entry-value>EV2</env-entry-value>
     * </env-entry>
     * Annotate an Enum<?> instance var:
     *
     * @Resource(name="EnvEntry_Enum_EntryName") Enum<?> ivEnvEntry_Enum;
     *
     *                                           - Verify that the <env-entry-value> is injected into Enum<?> ivEnvEntry_Enum;
     *                                           - Verify that the Enum can be explicitly looked up by name "ivEnvEntry_Enum".
     */
    @Test
    public void testE1aEnvEntryEnum() throws Exception {
        svAnnTestDriver.verifyE1EnvEntryEnum();
    }

    // Begin 661640
    @Test
    public void testP1aEnvEntryInteger() throws Exception {
        svAnnTestDriver.verifyP1EnvEntryInteger();
    }

    @Test
    public void testP2aEnvEntryInt() throws Exception {
        svAnnTestDriver.verifyP2EnvEntryInt();
    }

    @Test
    public void testP3aEnvEntryEnumQual() throws Exception {
        svAnnTestDriver.verifyP3EnvEntryEnumQual();
    }

    // End 661640

    /**
     * E2A.
     * Define an <env-entry> in ejb-jar.xml, under bean Bad2Bean:
     * <env-entry>
     * <description>E2a - Non-existent enum type specified in XML and @Resource annotation</description>
     * <env-entry-name>EnvEntry_Non-existentEnumType_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.NoSuchEnumType</env-entry-type>
     * <env-entry-value>EV0</env-entry-value>
     * </env-entry>
     * Annotate an Enum<?> instance var:
     *
     * @Resource(name="EnvEntry_Non-existentEnumType_EntryName")
     *                                                           Enum<?> ivEnvEntry_NoSuchEnumType;
     *
     *                                                           - Verify that an appropriate error is issued
     */
    //CALLED FROM TEST CLASS
    public void testE2aEnvEntryNonExistingEnumType() throws Exception {
        svAnnTestDriver.verifyE2EnvEntryNonExistingEnumType();
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad3Bean:
     * <env-entry>
     * <description>E3a - Non-existent enum value specified in XML and @Resource annotation</description>
     * <env-entry-name>EnvEntry_Non-existentEnumValue_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver$EnvEntryEnum</env-entry-type>
     * <env-entry-value>NO_SUCH_ENUM_VALUE</env-entry-value>
     * </env-entry>
     * Annotate an Enum<?> instance var:
     *
     * @Resource(name="EnvEntry_Non-existentEnumValue_EntryName")
     *                                                            Enum<?> ivEnvEntry_NoSuchEnumValue;
     *
     *                                                            - Verify that an appropriate error is issued
     */
    //CALLED FROM TEST CLASS
    public void testE3aEnvEntryNonExistingEnumValue() throws Exception {
        svAnnTestDriver.verifyE3EnvEntryNonExistingEnumValue();
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad4Bean:
     * <env-entry>
     * <description>E4a - Existing class that is neither an Enum nor a Class; specified in XML and @Resource annotation</description>
     * <env-entry-name>EnvEntry_ExistingNonEnumNonClass_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad1Bean</env-entry-type>
     * <env-entry-value>NOT_APPLICABLE</env-entry-value>
     * </env-entry>
     * Annotate an Enum<?> instance var:
     *
     * @Resource(name="EnvEntry_ExistingNonEnumNonClass_EntryName")
     *                                                              Enum<?> ivEnvEntry_NotApplicableEnumValue;
     *
     *                                                              - Verify that an appropriate error is issued
     */
    //CALLED FROM TEST CLASS
    public void testE4aEnvEntryExistingNonEnumNonClass() throws Exception {
        svAnnTestDriver.verifyE4EnvEntryExistingNonEnumNonClass();
    }

    /**
     * E5a
     * Bind an Enum into the Global namespace by name "envEntryEnumBound".
     * Define an <env-entry> in ejb-jar.xml:
     * <env-entry>
     * <description>E5a - Enum specified in XML with @Resource lookup annotation</description>
     * <env-entry-name>EnvEntry_Enum_Using_Lookup</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver$EnvEntryEnum</env-entry-type>
     * <env-entry-value>EV3</env-entry-value>
     * </env-entry>
     *
     * Annotate a bean's instance var with:
     *
     * @Resource(name="EnvEntry_Enum_Using_Lookup", lookup="envEntryEnumBound")
     *                                              Class<?> ivEnvEntry_Lookup_Enum;
     *
     *                                              - Verify that the bound Enum object is injected into ivEnvEntry_Lookup_Enum.
     *                                              - Verify that the bound Enum object can be explicitly looked up by name "EnvEntry_Enum_Using_Lookup".
     */
    @Test
    public void testE5aEnvEntryLookupEnum() throws Exception {
        svAnnTestDriver.verifyE5EnvEntryLookupEnum();
    }

    private static String bindObject(String name, Object value) {
        // bind an Object into the global JNDI namespace:

        try {
            InitialContext initCtx = new InitialContext();
            if (initCtx != null) {
                initCtx.rebind(name, value);
                svLogger.info("bound " + value + " by name <" + name + ">");
            }
        } catch (NamingException ne) {
            svLogger.info("Caught NamingException performing rebind");
            ne.printStackTrace();
        }

        return "bound";
    }
}