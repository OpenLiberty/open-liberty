/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.ejblink.client;

import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.naming.InitialContext;

import com.ibm.ws.ejbcontainer.ejblink.ejb.EjbLinkDriverRemote;

/**
 * <b>Test Matrix:</b>
 * <br>
 * <ul>
 * <li>testStyle1OtherJarXML - ejb-link test: To bean in a separate ejb-jar module using style 1
 * <li>testStyle2OtherJarXML - ejb-link test: To bean in a separate ejb-jar module using style 2
 * <li>testStyle3OtherJarXML - ejb-link test: To bean in a separate ejb-jar module using style 3
 * <li>testStyle1SameJarXML - ejb-link test: To bean in the same ejb-jar module using style 1
 * <li>testStyle2SameJarXML - ejb-link test: To bean in the same ejb-jar module using style 2
 * <li>testStyle3SameJarXML - ejb-link test: To bean in the same ejb-jar module using style 3
 * <li>testStyle1OtherJarAnn - beanName test: To bean in a separate ejb-jar module using style 1
 * <li>testStyle2OtherJarAnn - beanName test: To bean in a separate ejb-jar module using style 2
 * <li>testStyle3OtherJarAnn - beanName test: To bean in a separate ejb-jar module using style 3
 * <li>testStyle1SameJarAnn - beanName test: To bean in the same ejb-jar module using style 1
 * <li>testStyle2SameJarAnn - beanName test: To bean in the same ejb-jar module using style 2
 * <li>testStyle3SameJarAnn - beanName test: To bean in the same ejb-jar module using style 3
 * <li>testStyle1OtherWarXML - ejb-link test: To bean in a separate .war module using style 1
 * <li>testStyle2OtherWarXML - ejb-link test: To bean in a separate .war module using style 2
 * <li>testStyle3OtherWarXML - ejb-link test: To bean in a separate .war module using style 3
 * <li>testStyle1SameWarXML - ejb-link test: To bean in the same .war module using style 1
 * <li>testStyle2SameWarXML - ejb-link test: To bean in the same .war module using style 2
 * <li>testStyle3SameWarXML - ejb-link test: To bean in the same .war module using style 3
 * <li>testStyle1OtherWarAnn - beanName test: To bean in a separate .war module using style 1
 * <li>testStyle2OtherWarAnn - beanName test: To bean in a separate .war module using style 2
 * <li>testStyle3OtherWarAnn - beanName test: To bean in a separate .war module using style 3
 * <li>testStyle1SameWarAnn - beanName test: To bean in the same .war module using style 1
 * <li>testStyle2SameWarAnn - beanName test: To bean in the same .war module using style 2
 * <li>testStyle3SameWarAnn - beanName test: To bean in the same .war module using style 3
 * <li>testJarStyle1toWarXML - ejb-link test: From jar to bean in a .war module using style 1
 * <li>testJarStyle1toWarAnn - beanName test: From jar to bean in a .war module using style 1
 * <li>testWarStyle1toJarXML - ejb-link test: From .war to bean in a ejb-jar module using style 1
 * <li>testWarStyle1toJarAnn - beanName test: From .war to bean in a ejb-jar module using style 1
 * <li>testJarStyle2toWarXML - ejb-link test: From jar to bean in a .war module using style 2
 * <li>testJarStyle2toWarAnn - beanName test: From jar to bean in a .war module using style 2
 * <li>testWarStyle2toJarXML - ejb-link test: From .war to bean in a ejb-jar module using style 2
 * <li>testWarStyle2toJarAnn - beanName test: From .war to bean in a ejb-jar module using style 2
 * <li>testJarStyle3toWarXML - ejb-link test: From jar to bean in a .war module using style 3
 * <li>testJarStyle3toWarAnn - beanName test: From jar to bean in a .war module using style 3
 * <li>testWarStyle3toJarXML - ejb-link test: From .war to bean in a ejb-jar module using style 3
 * <li>testWarStyle3toJarAnn - beanName test: From .war to bean in a ejb-jar module using style 3
 * <li>testStyle1BeanInJarAndWar - ejb-link test: Bean in an ejb-jar module and .war module using style 1
 * <li>findBeanInSameJar - AutoLink test one Bean Implementation in the same jar module
 * <li>findBeanInSameWar - AutoLink test one Bean Implementation in the same war module
 * <li>findBeanFromJarInOtherJar - AutoLink test one Bean Implementation in a separate jar module
 * <li>findBeanFromWarInJar - AutoLink test one Bean Implementation in a jar module
 * <li>findBeanFromJarInWar - AutoLink test one Bean Implementation in a war module
 * <li>findBeanFromWarInOtherWar - AutoLink test two Bean Implementations, one per module
 * <li>findBeanInSameJarAndJar - AutoLink test two Bean Implementations, one per module
 * <li>findBeanInSameJarAndWar - AutoLink test two Bean Implementations, one per module
 * <li>findBeanFromJarInOtherJarAndWar - AutoLink test two Bean Implementations, one per module
 * <li>findBeanFromJarInTwoWars - AutoLink test two Bean Implementations, one per module
 * <li>findBeanInSameWarAndJar - AutoLink test two Bean Implementations, one per module
 * <li>findBeanInSameWarAndWar - AutoLink test two Bean Implementations, one per module
 * <li>findBeanFromWarInOtherJarAndWar - AutoLink test two Bean Implementations, one per module
 * <li>findBeanFromWarInTwoJars - AutoLink test two Bean Implementations, one per module
 * <li>findBeanFromJar2SameJar - AutoLink test two Bean Implementations, both in same jar module
 * <li>findBeanFromJar2OtherJar - AutoLink test two Bean Implementations, both in a separate jar module
 * <li>findBeanFromJar2War - AutoLink test two Bean Implementations, both in war module
 * <li>findBeanFromWar2SameWar - AutoLink test two Bean Implementations, both in same war module
 * <li>findBeanFromWar2OtherWar - AutoLink test two Bean Implementations, both in a separate war module
 * <li>findBeanFromWar2Jar - AutoLink test two Bean Implementations, both in same jar module
 * </ul>
 */
public class EjbLinkClient {
    private static final String CLASS_NAME = EjbLinkClient.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

    private static final String PASSED = "Passed";
    private static final String EXCEPTION_CLASS_NAME = "AmbiguousEJBReferenceException";

    // Name of application and modules... for lookup.
    private static final String Application = "EjbLinkTest";
    private static final String JarModule = "EjbLinkBean";
    private static final String WarModule = "EjbLinkInWar";

    // Name of the ejb-jar bean used for the test... for lookup.
    private static final String TestDriverFromJar = "TestDriverFromJar";

    // Name of the .war bean used for the test... for lookup.
    private static final String TestDriverFromWar = "TestDriverFromWar";

    // Name of the bean with no beans used to test speed.
    private static final String TestDriverNoBeans = "TestDriverNoBeans";

    // Names of the interface used for the test
    private static final String EJBInjectionRemoteInterface = EjbLinkDriverRemote.class.getName();

    public static void main(String[] args) throws Exception {
        EjbLinkClient test = new EjbLinkClient();
        test.run();
    }

    public void run() throws Exception {
        testStyle1OtherJarXML();
        testStyle2OtherJarXML();
        testStyle3OtherJarXML();
        testStyle1SameJarXML();
        testStyle2SameJarXML();
        testStyle3SameJarXML();
        testStyle1OtherJarAnn();
        testStyle2OtherJarAnn();
        testStyle3OtherJarAnn();
        testStyle1SameJarAnn();
        testStyle2SameJarAnn();
        testStyle3SameJarAnn();
        testStyle1OtherWarXML();
        testStyle2OtherWarXML();
        testStyle3OtherWarXML();
        testStyle1SameWarXML();
        testStyle2SameWarXML();
        testStyle3SameWarXML();
        testStyle1OtherWarAnn();
        testStyle2OtherWarAnn();
        testStyle3OtherWarAnn();
        testStyle1SameWarAnn();
        testStyle2SameWarAnn();
        testStyle3SameWarAnn();
        testJarStyle1toWarXML();
        testJarStyle1toWarAnn();
        testWarStyle1toJarXML();
        testWarStyle1toJarAnn();
        testJarStyle2toWarXML();
        testJarStyle2toWarAnn();
        testWarStyle2toJarXML();
        testWarStyle2toJarAnn();
        testJarStyle3toWarXML();
        testJarStyle3toWarAnn();
        testWarStyle3toJarXML();
        testWarStyle3toJarAnn();
        testStyle1BeanInJarAndWar();
        findBeanInSameJar();
        findBeanInSameWar();
        findBeanFromJarInOtherJar();
        findBeanFromWarInJar();
        findBeanFromJarInWar();
        findBeanFromWarInOtherWar();
        findBeanInSameJarAndJar();
        findBeanInSameJarAndWar();
        findBeanFromJarInOtherJarAndWar();
        findBeanFromJarInTwoWars();
        findBeanInSameWarAndJar();
        findBeanInSameWarAndWar();
        findBeanFromWarInOtherJarAndWar();
        findBeanFromWarInTwoJars();
        findBeanFromJar2SameJar();
        findBeanFromJar2OtherJar();
        findBeanFromJar2War();
        findBeanFromWar2SameWar();
        findBeanFromWar2OtherWar();
        findBeanFromWar2Jar();
    }

    public Object lookupDefaultBindingEJRemoteInterface(String beanInterface, String application, String module, String bean) throws Exception {
        return new InitialContext().lookup("java:global/" + application + "/"
                                           + module + "/" + bean + "!" + beanInterface);
    }

    /**
     * Test ejb-link in a ejb-local-ref that specifies only the name of the
     * target enterprise bean that is in an ejb-jar file that is not the current
     * ejb-jar file.
     *
     * For example,
     *
     * <ejb-local-ref> <ejb-ref-name>ejb/OtherJarStyle1</ejb-ref-name>
     * <ejb-link>OtherJarBean</ejb-link> </ejb-local-ref>
     */
    public void testStyle1OtherJarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle1OtherJarXML().equals(PASSED)) {
            System.out.println("testStyle1OtherJarXML--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-ref that specifies the path name of the ejb-jar
     * file containing the referenced enterprise bean from a different ejb-jar
     * file and appends the ejb-name of the target bean separated from the path
     * name by # .
     *
     * For example,
     *
     * <ejb-ref> <ejb-ref-name>ejb/OtherJarStyle2</ejb-ref-name>
     * <ejb-link>../OtherEJB.jar#OtherJarBean</ejb-link> </ejb-ref>
     */
    public void testStyle2OtherJarXML() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle2OtherJarXML().equals(PASSED)) {
            System.out.println("testStyle2OtherJarXML--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-local-ref that specifies the module name, as set
     * in the module-name element, of the ejb-jar file containing the referenced
     * enterprise bean from a different ejb-jar file and appends the ejb-name of
     * the target bean separated by /.
     *
     * For example,
     *
     * <ejb-local-ref> <ejb-ref-name>ejb/OtherJarStyle3</ejb-ref-name>
     * <ejb-link>logicalOther/OtherJarBean</ejb-link> </ejb-local-ref>
     */
    public void testStyle3OtherJarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle3OtherJarXML().equals(PASSED)) {
            System.out.println("testStyle3OtherJarXML--PASSED");
        }
    }

    /**
     * Test ejb-link in a ejb-local-ref that specifies only the name of the
     * target enterprise bean that is in an ejb-jar file that is the current
     * ejb-jar file.
     *
     * For example,
     *
     * <ejb-local-ref> <ejb-ref-name>ejb/SameJarStyle1</ejb-ref-name>
     * <ejb-link>SameJarBean</ejb-link> </ejb-local-ref>
     */
    public void testStyle1SameJarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle1SameJarXML().equals(PASSED)) {
            System.out.println("testStyle1SameJarXML--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-local-ref that specifies the path name of the
     * ejb-jar file containing the referenced enterprise bean from the current
     * ejb-jar file and appends the ejb-name of the target bean separated from
     * the path name by # .
     *
     * For example,
     *
     * <ejb-local-ref> <ejb-ref-name>ejb/SameJarStyle2</ejb-ref-name>
     * <ejb-link>../EJBLINKXBean.jar#SameJarBean</ejb-link> </ejb-local-ref>
     */
    public void testStyle2SameJarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle2SameJarXML().equals(PASSED)) {
            System.out.println("testStyle2SameJarXML--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-local-ref that specifies the a module name which
     * was not set in the module-name element of the ejb-jar file containing the
     * referenced enterprise bean. The referenced bean is in the current ejb-jar
     * file and appends the ejb-name of the target bean separated by /.
     *
     * For example,
     *
     * <ejb-local-ref> <ejb-ref-name>ejb/SameJarStyle3</ejb-ref-name>
     * <ejb-link>EJBLINKXBean/SameJarBean</ejb-link> </ejb-local-ref>
     */
    public void testStyle3SameJarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle3SameJarXML().equals(PASSED)) {
            System.out.println("testStyle3SameJarXML--PASSED");
        }
    }

    /**
     * Test beanName annotation that specifies only the name of the target
     * enterprise bean that is in an ejb-jar file that is not the current
     * ejb-jar file.
     *
     * For example,
     *
     * @EJB(beanName="OtherJarBean") public EjbLinkLocal otherJarStyle1Ann;
     */
    public void testStyle1OtherJarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle1OtherJarAnn().equals(PASSED)) {
            System.out.println("testStyle1OtherJarAnn--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the path name of the ejb-jar file
     * containing the referenced enterprise bean from a different ejb-jar file
     * and appends the ejb-name of the target bean separated from the path name
     * by # .
     *
     * For example,
     *
     * @EJB(beanName="../OtherEJB.jar#OtherJarBean") public EjbLinkLocal
     *                                               otherJarStyle2Ann;
     */
    public void testStyle2OtherJarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle2OtherJarAnn().equals(PASSED)) {
            System.out.println("testStyle2OtherJarAnn--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the module name, as set in the
     * module-name element, of the ejb-jar file containing the referenced
     * enterprise bean from a different ejb-jar file and appends the ejb-name of
     * the target bean separated by /.
     *
     * For example,
     *
     * @EJB(beanName="logicalOther/OtherJarBean") public EjbLinkLocal
     *                                            otherJarStyle3Ann;
     */
    public void testStyle3OtherJarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle3OtherJarAnn().equals(PASSED)) {
            System.out.println("testStyle3OtherJarAnn--PASSED");
        }
    }

    /**
     * Test beanName annotation that specifies only the name of the target
     * enterprise bean that is in an ejb-jar file that is the current ejb-jar
     * file.
     *
     * For example,
     *
     * @EJB(beanName="SameJarBean") public EjbLinkLocal sameJarStyle1Ann;
     */
    public void testStyle1SameJarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null) {
            return;
        }
        if (bean.verifyStyle1SameJarAnn().equals(PASSED)) {
            System.out.println("testStyle1SameJarAnn--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the path name of the ejb-jar file
     * containing the referenced enterprise bean from the current ejb-jar file
     * and appends the ejb-name of the target bean separated from the path name
     * by # .
     *
     * For example,
     *
     * @EJB(beanName="../EJBLINKXBean.jar#SameJarBean") public EjbLinkLocal
     *                                                  sameJarStyle2Ann;
     */
    public void testStyle2SameJarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle2SameJarAnn().equals(PASSED)) {
            System.out.println("testStyle2SameJarAnn--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the module name which was not set
     * in the module-name element of the ejb-jar file containing the referenced
     * enterprise bean. The referenced bean is in the current ejb-jar file and
     * appends the ejb-name of the target bean separated by /.
     *
     * For example,
     *
     * @EJB(beanName="EJBLINKXBean/SameJarBean") public EjbLinkLocal
     *                                           sameJarStyle3Ann;
     */
    public void testStyle3SameJarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle3SameJarAnn().equals(PASSED)) {
            System.out.println("testStyle3SameJarAnn--PASSED");
        }
    }

    /**
     * Test ejb-link in a ejb-local-ref that specifies only the name of the
     * target enterprise bean that is in a .war file that is not the current
     * .war file.
     *
     * For example,
     *
     * <ejb-local-ref> <ejb-ref-name>ejb/OtherWarStyle1</ejb-ref-name>
     * <ejb-link>OtherWarBean</ejb-link> </ejb-local-ref>
     */
    public void testStyle1OtherWarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle1OtherWarXML().equals(PASSED)) {
            System.out.println("testStyle1OtherWarXML--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-ref that specifies the path name of the .war file
     * containing the referenced enterprise bean from a different .war file and
     * appends the ejb-name of the target bean separated from the path name by #
     * .
     *
     * For example,
     *
     * <ejb-ref> <ejb-ref-name>ejb/OtherWarStyle2</ejb-ref-name>
     * <ejb-link>../OtherEJB.war#OtherWarBean</ejb-link> </ejb-ref>
     */
    public void testStyle2OtherWarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle2OtherWarXML().equals(PASSED)) {
            System.out.println("testStyle2OtherWarXML--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-local-ref that specifies the module name, as set
     * in the module-name element, of the .war file containing the referenced
     * enterprise bean from a different .war file and appends the ejb-name of
     * the target bean separated by /.
     *
     * For example,
     *
     * <ejb-local-ref> <ejb-ref-name>ejb/OtherWarStyle3</ejb-ref-name>
     * <ejb-link>logicalOther/OtherWarBean</ejb-link> </ejb-local-ref>
     */
    public void testStyle3OtherWarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle3OtherWarXML().equals(PASSED)) {
            System.out.println("testStyle3OtherWarXML--PASSED");
        }
    }

    /**
     * Test ejb-link in a ejb-local-ref that specifies only the name of the
     * target enterprise bean that is in a .war file that is the current .war
     * file.
     *
     * For example,
     *
     * <ejb-local-ref> <ejb-ref-name>ejb/SameWarStyle1</ejb-ref-name>
     * <ejb-link>SameWarBean</ejb-link> </ejb-local-ref>
     */
    public void testStyle1SameWarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle1SameWarXML().equals(PASSED)) {
            System.out.println("testStyle1SameWarXML--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-local-ref that specifies the path name of the
     * .war file containing the referenced enterprise bean from the current .war
     * file and appends the ejb-name of the target bean separated from the path
     * name by # .
     *
     * For example,
     *
     * <ejb-local-ref> <ejb-ref-name>ejb/SameWarStyle2</ejb-ref-name>
     * <ejb-link>../EJBLINKXBean.war#SameWarBean</ejb-link> </ejb-local-ref>
     */
    public void testStyle2SameWarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle2SameWarXML().equals(PASSED)) {
            System.out.println("testStyle2SameWarXML--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-local-ref that specifies the a module name which
     * was not set in the module-name element of the .war file containing the
     * referenced enterprise bean. The referenced bean is in the current .war
     * file and appends the ejb-name of the target bean separated by /.
     *
     * For example,
     *
     * <ejb-local-ref> <ejb-ref-name>ejb/SameWarStyle3</ejb-ref-name>
     * <ejb-link>EJBLINKXBean/SameWarBean</ejb-link> </ejb-local-ref>
     */
    public void testStyle3SameWarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle3SameWarXML().equals(PASSED)) {
            System.out.println("testStyle3SameWarXML--PASSED");
        }
    }

    /**
     * Test beanName annotation that specifies only the name of the target
     * enterprise bean that is in a .war file that is not the current .war file.
     *
     * For example,
     *
     * @EJB(beanName="OtherWarBean") public EjbLinkLocal otherWarStyle1Ann;
     */
    public void testStyle1OtherWarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle1OtherWarAnn().equals(PASSED)) {
            System.out.println("testStyle1OtherWarAnn--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the path name of the .war file
     * containing the referenced enterprise bean from a different .war file and
     * appends the ejb-name of the target bean separated from the path name by #
     * .
     *
     * For example,
     *
     * @EJB(beanName="../OtherEJB.war#OtherWarBean") public EjbLinkLocal
     *                                               otherWarStyle2Ann;
     */
    public void testStyle2OtherWarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle2OtherWarAnn().equals(PASSED)) {
            System.out.println("testStyle2OtherWarAnn--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the module name, as set in the
     * module-name element, of the .war file containing the referenced
     * enterprise bean from a different .war file and appends the ejb-name of
     * the target bean separated by /.
     *
     * For example,
     *
     * @EJB(beanName="logicalOther/OtherWarBean") public EjbLinkLocal
     *                                            otherWarStyle3Ann;
     */
    public void testStyle3OtherWarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle3OtherWarAnn().equals(PASSED)) {
            System.out.println("testStyle3OtherWarAnn--PASSED");
        }
    }

    /**
     * Test beanName annotation that specifies only the name of the target
     * enterprise bean that is in a .war file that is the current .war file.
     *
     * For example,
     *
     * @EJB(beanName="SameWarBean") public EjbLinkLocal sameWarStyle1Ann;
     */
    public void testStyle1SameWarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle1SameWarAnn().equals(PASSED)) {
            System.out.println("testStyle1SameWarAnn--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the path name of the .war file
     * containing the referenced enterprise bean from the current .war file and
     * appends the ejb-name of the target bean separated from the path name by #
     * .
     *
     * For example,
     *
     * @EJB(beanName="../EJBLINKXBean.war#SameWarBean") public EjbLinkLocal
     *                                                  sameWarStyle2Ann;
     */
    public void testStyle2SameWarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle2SameWarAnn().equals(PASSED)) {
            System.out.println("testStyle2SameWarAnn--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the module name which was not set
     * in the module-name element of the .war file containing the referenced
     * enterprise bean. The referenced bean is in the current .war file and
     * appends the ejb-name of the target bean separated by /.
     *
     * For example,
     *
     * @EJB(beanName="EJBLINKXBean/SameWarBean") public EjbLinkLocal
     *                                           sameWarStyle3Ann;
     */
    public void testStyle3SameWarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle3SameWarAnn().equals(PASSED)) {
            System.out.println("testStyle3SameWarAnn--PASSED");
        }
    }

    /**
     * Test ejb-link in a ejb-local-ref that specifies only the name of the
     * target enterprise bean that is in an .war file that is not the current
     * ejb-jar file.
     */
    public void testJarStyle1toWarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle1OtherWarXML().equals(PASSED)) {
            System.out.println("testJarStyle1toWarXML--PASSED");
        }
    }

    /**
     * Test beanName annotation that specifies only the name of the target
     * enterprise bean that is in an .war file that is not the current ejb-jar
     * file.
     */
    public void testJarStyle1toWarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle1OtherWarAnn().equals(PASSED)) {
            System.out.println("testJarStyle1toWarAnn--PASSED");
        }
    }

    /**
     * Test ejb-link in a ejb-local-ref that specifies only the name of the
     * target enterprise bean that is in an ejb-jar file that is not the current
     * .war file.
     */
    public void testWarStyle1toJarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle1OtherJarXML().equals(PASSED)) {
            System.out.println("testWarStyle1toJarXML--PASSED");
        }
    }

    /**
     * Test beanName annotation that specifies only the name of the target
     * enterprise bean that is in an ejb-jar file that is not the current .war
     * file.
     */
    public void testWarStyle1toJarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle1OtherJarAnn().equals(PASSED)) {
            System.out.println("testWarStyle1toJarAnn--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-local-ref that specifies the path name of the
     * .war file containing the referenced enterprise bean from an ejb-jar file
     * and appends the ejb-name of the target bean separated from the path name
     * by # .
     */
    public void testJarStyle2toWarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle2OtherWarXML().equals(PASSED)) {
            System.out.println("testJarStyle2toWarXML--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the path name of the .war file
     * containing the referenced enterprise bean from an ejb-jar file and
     * appends the ejb-name of the target bean separated from the path name by #
     * .
     */
    public void testJarStyle2toWarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle2OtherWarAnn().equals(PASSED)) {
            System.out.println("testJarStyle2toWarAnn--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-local-ref that specifies the path name of the
     * ejb-jar file containing the referenced enterprise bean from a .war file
     * and appends the ejb-name of the target bean separated from the path name
     * by # .
     */
    public void testWarStyle2toJarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle2OtherJarXML().equals(PASSED)) {
            System.out.println("testWarStyle2toJarXML--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the path name of the ejb-jar file
     * containing the referenced enterprise bean from a .war file and appends
     * the ejb-name of the target bean separated from the path name by # .
     */
    public void testWarStyle2toJarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle2OtherJarAnn().equals(PASSED)) {
            System.out.println("testWarStyle2toJarAnn--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-local-ref that specifies the module name, as set
     * in the module-name element, of the .war file containing the referenced
     * enterprise bean from an ejb-jar file and appends the ejb-name of the
     * target bean separated by /.
     */
    public void testJarStyle3toWarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle3OtherWarXML().equals(PASSED)) {
            System.out.println("testJarStyle3toWarXML--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the module name, as set in the
     * module-name element, of the .war file containing the referenced
     * enterprise bean from an ejb-jar file and appends the ejb-name of the
     * target bean separated by /.
     */
    public void testJarStyle3toWarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyStyle3OtherWarAnn().equals(PASSED)) {
            System.out.println("testJarStyle3toWarAnn--PASSED");
        }
    }

    /**
     * Test ejb-link in an ejb-local-ref that specifies the module name, as set
     * in the module-name element, of the ejb-jar file containing the referenced
     * enterprise bean from a .war file and appends the ejb-name of the target
     * bean separated by /.
     */
    public void testWarStyle3toJarXML() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle3OtherJarXML().equals(PASSED)) {
            System.out.println("testWarStyle3toJarXML--PASSED");
        }
    }

    /**
     * Test beanName annotation specified to the module name, as set in the
     * module-name element, of the ejb-jar file containing the referenced
     * enterprise bean from a .war file and appends the ejb-name of the target
     * bean separated by /.
     */
    public void testWarStyle3toJarAnn() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyStyle3OtherJarAnn().equals(PASSED)) {
            System.out.println("testWarStyle3toJarAnn--PASSED");
        }
    }

    /**
     * Verify that when a bean name exists in both a .war and ejb-jar the
     * appropriate error (AmbiguousException) occurs.
     */
    public void testStyle1BeanInJarAndWar() throws Exception {
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        JarModule, "TestDupBean");

            bean.verifyStyle1BeanInJarAndWar();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            exc.printStackTrace();
            result = PASSED;
        }

        if (result.equals(PASSED)) {
            System.out.println("testStyle1BeanInJarAndWar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists only in an ejb-jar module
     * that is the current ejb-jar module AutoLink finds the bean.
     */
    public void findBeanInSameJar() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyAutoLinkToCurrentModule().equals(PASSED)) {
            System.out.println("findBeanInSameJar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists only in a .war module that
     * is the current .war module AutoLink finds the bean.
     */
    public void findBeanInSameWar() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyAutoLinkToCurrentModule().equals(PASSED)) {
            System.out.println("findBeanInSameWar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists only in an ejb-jar module
     * that is not the current ejb-jar module AutoLink finds the bean.
     */
    public void findBeanFromJarInOtherJar() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyAutoLinkToOtherJar().equals(PASSED)) {
            System.out.println("findBeanFromJarInOtherJar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists only in an ejb-jar module
     * AutoLink finds the bean from the current .war module.
     */
    public void findBeanFromWarInJar() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyAutoLinkToOtherJar().equals(PASSED)) {
            System.out.println("findBeanFromWarInJar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists only in a .war module
     * AutoLink finds the bean from an ejb-jar module.
     */
    public void findBeanFromJarInWar() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyAutoLinkToOtherWar().equals(PASSED)) {
            System.out.println("findBeanFromJarInWar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists only in a .war module that
     * is not the current .war module AutoLink finds the bean.
     */
    public void findBeanFromWarInOtherWar() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyAutoLinkToOtherWar().equals(PASSED)) {
            System.out.println("findBeanFromWarInOtherWar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists in an ejb-jar module that
     * is the current ejb-jar module and also in a separate ejb-jar module
     * AutoLink finds the bean.
     */
    public void findBeanInSameJarAndJar() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyAutoLinkToJarAndOtherJar().equals(PASSED)) {
            System.out.println("findBeanInSameJarAndJar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists in an ejb-jar module that
     * is the current ejb-jar module and also in a .war module AutoLink finds
     * the bean.
     */
    public void findBeanInSameJarAndWar() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, JarModule,
                                                                                                    TestDriverFromJar);
        if (bean == null)
            return;

        if (bean.verifyAutoLinkToJarAndWar().equals(PASSED)) {
            System.out.println("findBeanInSameJarAndWar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists in both a .war and ejb-jar
     * that but not the current ejb-jar module AutoLink throws the appropriate
     * error (AmbiguousEjbReferenceException).
     */
    public void findBeanFromJarInOtherJarAndWar() throws Exception {
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        JarModule, "TestAutoLinkOtherJarWar");
            if (bean == null)
                return;

            bean.verifyAutoLinkToOtherJarAndWar();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            result = PASSED;
        }

        if (result.equals(PASSED)) {
            System.out.println("findBeanFromJarInOtherJarAndWar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists in two separate .war
     * modules AutoLink from an ejb-jar module throws the appropriate error
     * (AmbiguousEjbReferenceException).
     */
    public void findBeanFromJarInTwoWars() throws Exception {
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        JarModule, "TestAutoLinkWarOtherWar");
            if (bean == null)
                return;

            bean.verifyAutoLinkToWarAndOtherWar();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            result = PASSED;
        }

        if (result.equals(PASSED)) {
            System.out.println("findBeanFromJarInTwoWars--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists in a .war module that is
     * the current module and also in an ejb-jar module AutoLink finds the bean.
     */
    public void findBeanInSameWarAndJar() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyAutoLinkToJarAndWar().equals(PASSED)) {
            System.out.println("findBeanInSameWarAndJar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists in a .war module that is
     * the current module and also in an ejb-jar module AutoLink finds the bean.
     */
    public void findBeanInSameWarAndWar() throws Exception {
        EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application, WarModule,
                                                                                                    TestDriverFromWar);
        if (bean == null)
            return;

        if (bean.verifyAutoLinkToWarAndOtherWar().equals(PASSED)) {
            System.out.println("findBeanInSameWarAndWar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists in both a .war and ejb-jar
     * but not the current .war module AutoLink throws the appropriate error
     * (AmbiguousEJBReferenceException).
     */
    public void findBeanFromWarInOtherJarAndWar() throws Exception {
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        WarModule, "TestAutoLinkOtherWarOtherJar");
            if (bean == null)
                return;

            bean.verifyAutoLinkToOtherJarAndWar();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            result = PASSED;
        }

        if (result.equals(PASSED)) {
            System.out.println("findBeanFromWarInOtherJarAndWar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists in two separate ejb-jar
     * modules but not the current .war module AutoLink throws the appropriate
     * error (AmbiguousEJBReferenceException).
     */
    public void findBeanFromWarInTwoJars() throws Exception {
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        WarModule, "TestAutoLinkJarOtherJar");
            if (bean == null)
                return;

            bean.verifyAutoLinkToJarAndOtherJar();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            result = PASSED;
        }

        if (result.equals(PASSED)) {
            System.out.println("findBeanFromWarInTwoJars--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists twice in the current
     * ejb-jar module AutoLink throws the appropriate error
     * (AmbiguousEJBReferenceException).
     */
    public void findBeanFromJar2SameJar() throws Exception {
        System.out.println("Entering findBeanFromJar2SameJar");
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        JarModule, "TestAutoLinkJar2SameJar");
            if (bean == null)
                return;

            bean.verifyAmbiguousEJBReferenceException();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            result = PASSED;
        } finally {
            if (result.equals(PASSED)) {
                System.out.println("findBeanFromJar2SameJar--PASSED");
            }
        }

    }

    /**
     * Verify that when a bean implementation exists twice in an ejb-jar that is
     * not the current module and that bean does not in the current module
     * AutoLink throws the appropriate error (AmbiguousEJBReferenceException).
     */
    public void findBeanFromJar2OtherJar() throws Exception {
        System.out.println("Entering findBeanFromJar2OtherJar");
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        JarModule, "TestAutoLinkJar2OtherJar");
            if (bean == null)
                return;

            bean.verifyAmbiguousEJBReferenceException();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            result = PASSED;
        }

        if (result.equals(PASSED)) {
            System.out.println("findBeanFromJar2OtherJar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists twice in a .war module and
     * not in the current ejb-jar module AutoLink throws the appropriate error
     * (AmbiguousEJBReferenceException).
     */
    public void findBeanFromJar2War() throws Exception {
        System.out.println("Entering findBeanFromJar2War");
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        JarModule, "TestAutoLinkJar2War");
            if (bean == null)
                return;

            bean.verifyAmbiguousEJBReferenceException();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            result = PASSED;
        }

        if (result.equals(PASSED)) {
            System.out.println("findBeanFromJar2War--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists twice in an ejb-jar module
     * and not in the current .war module AutoLink throws the appropriate error
     * (AmbiguousEJBReferenceException).
     */
    public void findBeanFromWar2SameWar() throws Exception {
        System.out.println("Entering findBeanFromWar2SameWar");
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        WarModule, "TestAutoLinkWar2SameWar");
            if (bean == null)
                return;

            bean.verifyAmbiguousEJBReferenceException();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            result = PASSED;
        }

        if (result.equals(PASSED)) {
            System.out.println("findBeanFromWar2SameWar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists twice in a .war module that
     * is not the current module and the bean does not exist in the current
     * module AutoLink throws the appropriate error
     * (AmbiguousEJBReferenceException).
     */
    public void findBeanFromWar2OtherWar() throws Exception {
        System.out.println("Entering findBeanFromWar2OtherWar");
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        WarModule, "TestAutoLinkWar2OtherWar");
            if (bean == null)
                return;

            bean.verifyAmbiguousEJBReferenceException();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            result = PASSED;
        }

        if (result.equals(PASSED)) {
            System.out.println("findBeanFromWar2OtherWar--PASSED");
        }
    }

    /**
     * Verify that when a bean implementation exists twice in an ejb-jar module
     * and not in the current .war module AutoLink throws the appropriate error
     * (AmbiguousEJBReferenceException).
     */
    public void findBeanFromWar2Jar() throws Exception {
        System.out.println("Entering findBeanFromWar2Jar");
        String result = "Failed";
        try {
            EjbLinkDriverRemote bean = (EjbLinkDriverRemote) this.lookupDefaultBindingEJRemoteInterface(EJBInjectionRemoteInterface, Application,
                                                                                                        WarModule, "TestAutoLinkWar2Jar");
            if (bean == null)
                return;

            bean.verifyAmbiguousEJBReferenceException();
            // Change to AmbiguousEJBReferenceException once defect 174107 is resolved
        } catch (EJBException exc) {
            result = PASSED;
        }

        if (result.equals(PASSED)) {
            System.out.println("findBeanFromWar2Jar--PASSED");
        }
    }
}
