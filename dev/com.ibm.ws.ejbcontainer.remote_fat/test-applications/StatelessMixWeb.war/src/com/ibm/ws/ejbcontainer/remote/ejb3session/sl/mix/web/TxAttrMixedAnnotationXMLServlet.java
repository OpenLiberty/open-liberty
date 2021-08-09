/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ExpClassExpAllxmlLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ExpClassExpAllxmlRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ExpClassExpXMLMethodLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ExpClassExpXMLMethodRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ExpMethodExpAllxmlLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ExpMethodExpAllxmlRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ExpMethodExpXMLMethodLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ExpMethodExpXMLMethodRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ImpClassExpAllxmlLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ImpClassExpAllxmlRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ImpClassExpXMLMethodLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.ImpClassExpXMLMethodRemote;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>TxAttrMixedAnnotationXMLTest
 *
 * <dt>Test Descriptions:
 * <dd>Tests whether the ejb container handles a mixture of annotations and XML
 * for setting TX attributes. The XML should take precedence over the
 * annotations. Note, currently tests attributes on a Stateless Session Bean.
 * Container code is same for all bean types, but it would be safer
 * if this test was extended to test all bean types.
 *
 * <dt>Author:
 * <dd>Urrvano Gamez, Jr.
 *
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testWildCardXmlPrecedenceOverClassLevelAnn verifies that the wild card XML demarcation actually takes precedence over class level demarcation using an annotation.
 * <li>testWildCardXmlPrecedenceOverClassLevelAnnRemote repeats testWildCardXmlPrecedenceOverClassLevelAnn using remote interface.
 * <li>testWildCardXmlDemarcationPrecedence verifies that the wild card XML demarcation actually takes precedence over both class and method level demarcations using annotations.
 * <li>testWildCardXmlDemarcationPrecedenceRemote repeats testWildCardXmlDemarcationPrecedence using remote interface.
 * <li>testWildCardXmlDemarcationPrecedenceSpecificMthd verifies that XML demarcation of a specific method actually takes precedence over class level demarcation using an
 * annotation.
 * <li>testWildCardXmlDemarcationPrecedenceSpecificMthdRemote repeats testWildCardXmlDemarcationPrecedenceSpecificMthd using remote interface.
 * <li>testAnnMethodAXmlMethodB verifies that the class level demarcation using an annotation is used for methodA when there is only XML demarcation for methodB.
 * <li>testAnnMethodAXmlMethodBRemote repeats testAnnMethodAXmlMethodB using remote interface.
 * <li>testXmlDemarcationPrecedenceOverAnn verifies that the wild card XML demarcation actually takes precedence over method level demarcation using an annotation.
 * <li>testXmlDemarcationPrecedenceOverAnnRemote repeats testXmlDemarcationPrecedenceOverAnn using remote interface.
 * <li>testXmlDemarcationPrecedenceOverAnnMthdLevel verifies that XML demarcation of a specific method actually takes precedence over method level demarcation using an annotation.
 * <li>testXmlDemarcationPrecedenceOverAnnMthdLevelRemote repeats testXmlDemarcationPrecedenceOverAnnMthdLevel using remote interface.
 * <li>testImplicitTxREQUIREDUse verifies that implicit Tx Attr of REQUIRED is used for methodA when there is only XML demarcation for methodB.
 * <li>testImplicitTxREQUIREDUseRemote repeats testImplicitTxREQUIREDUse using remote interface.
 * <li>testWildCardXmlPrecedenceOverImpTxREQUIRED verifies that the wild card XML demarcation actually takes precedence over an implicit Tx Attr of REQUIRED.
 * <li>testWildCardXmlPrecedenceOverImpTxREQUIREDRemote repeats testWildCardXmlPrecedenceOverImpTxREQUIRED using remote interface.
 * <li>testWildCardXmlPrecedenceOverImpTxREQUIREDSepcificMthd verifies that XML demarcation of a specific method actually takes precedence over an implicit Tx Attr of REQUIRED.
 * <li>testWildCardXmlPrecedenceOverImpTxREQUIREDSepcificMthdRemote repeats testWildCardXmlPrecedenceOverImpTxREQUIREDSepcificMthd using remote interface.
 * <li>testImplicitTxREQUIREDUseBothAnnXml verifies that implicit Tx Attr of REQUIRED is used for methodA when there is both annotation and XML demarcation for methodB.
 * <li>testImplicitTxREQUIREDUseBothAnnXmlRemote repeats testImplicitTxREQUIREDUseBothAnnXml using remote interface.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/TxAttrMixedAnnotationXMLServlet")
public class TxAttrMixedAnnotationXMLServlet extends FATServlet {
    private static final String CLASS_NAME = TxAttrMixedAnnotationXMLServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB
    private ExpClassExpAllxmlLocal expClassExpAllxmlBean1;

    @EJB
    private ExpClassExpAllxmlRemote expClassExpAllxmlBean2;

    @EJB
    private ExpClassExpXMLMethodLocal expClassExpXMLMethodBean1;

    @EJB
    private ExpClassExpXMLMethodRemote expClassExpXMLMethodBean2;

    @EJB
    private ExpMethodExpAllxmlLocal expMethodExpAllxmlBean1;

    @EJB
    private ExpMethodExpAllxmlRemote expMethodExpAllxmlBean2;

    @EJB
    private ExpMethodExpXMLMethodLocal expMethodExpXMLMethodBean1;

    @EJB
    private ExpMethodExpXMLMethodRemote expMethodExpXMLMethodBean2;

    @EJB
    private ImpClassExpAllxmlLocal impClassExpAllxmlBean1;

    @EJB
    private ImpClassExpAllxmlRemote impClassExpAllxmlBean2;

    @EJB
    private ImpClassExpXMLMethodLocal impClassExpXMLMethodBean1;

    @EJB
    private ImpClassExpXMLMethodRemote impClassExpXMLMethodBean2;

    /**
     * Verify that the XML takes precedence when: 1)Not taking the XML into
     * account, the bean (and implicitly its methods) explicitly has Tx Attr of
     * NEVER set at the class level via annotation. 2)XML uses the * to set all
     * methods to have the trans-attribute of RequiresNew
     *
     * While thread is currently associated with a transaction context, call a
     * method that is setup as described above and verify that the container
     * begins a new global transaction. Verify container completes global
     * transaction prior to returning to caller of method. Verify caller's
     * global transaction is still active when container returns to caller.
     *
     */
    @Test
    public void testWildCardXmlPrecedenceOverClassLevelAnn() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, expClassExpAllxmlBean1, not null", expClassExpAllxmlBean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expClassExpAllXML method
            try {
                if (expClassExpAllxmlBean1.expClassExpAllXML(tid)) {
                    svLogger.info("Container began new global transaction thus the XML took precedence.");
                } else {
                    fail("The method returned false. Meaning both the XML override and the class level demarcation of NEVER failed.");
                }
            } catch (EJBException ex) {
                fail("Container threw an EJBException which likely means that the TX attribute for the method "
                     + "was NEVER meaning the XML demarcation of RequiresNew did not take precedence.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * Verify that the XML takes precedence when: 1)Not taking the XML into
     * account, the bean (and implicitly its methods) explicitly has Tx Attr of
     * NEVER set at the class level via annotation. 2)XML uses the * to set all
     * methods to have the trans-attribute of RequiresNew
     *
     * While thread is currently associated with a transaction context, call a
     * method that is setup as described above and verify that the container
     * begins a new global transaction. Verify container completes global
     * transaction prior to returning to caller of method. Verify caller's
     * global transaction is still active when container returns to caller.
     *
     */
    @Test
    public void testWildCardXmlPrecedenceOverClassLevelAnnRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, expClassExpAllxmlBean2, not null", expClassExpAllxmlBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expClassExpAllXML method
            if (expClassExpAllxmlBean2.expClassExpAllXML(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence.");
            } else {
                fail("The method returned false. Meaning both the XML override and the class level demarcation of NEVER failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * 1)Not taking the XML into account, the bean is explicitly set (via
     * annotation) at the class level to have a Tx Attr of NEVER, however, the
     * method is explicitly set (via annotation) to have a Tx Attr of REQUIRED.
     * 2)XML uses the * to set all methods to have the trans-attribute of
     * RequiresNew 3)The XML should take precedence
     *
     * While thread is currently associated with a transaction context, call a
     * method that is setup as described above and verify that the container
     * begins a new global transaction. Verify container completes global
     * transaction prior to returning to caller of method. Verify caller's
     * global transaction is still active when container returns to caller.
     *
     */
    @Test
    public void testWildCardXmlDemarcationPrecedence() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, expClassExpAllxmlBean1, not null", expClassExpAllxmlBean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expClassExpMethodExpAllXML method
            if (expClassExpAllxmlBean1.expClassExpMethodExpAllXML(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence.");
            } else {
                fail("The method returned false which means the XML override failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * 1)Not taking the XML into account, the bean is explicitly set (via
     * annotation) at the class level to have a Tx Attr of NEVER, however, the
     * method is explicitly set (via annotation) to have a Tx Attr of REQUIRED.
     * 2)XML uses the * to set all methods to have the trans-attribute of
     * RequiresNew 3)The XML should take precedence
     *
     * While thread is currently associated with a transaction context, call a
     * method that is setup as described above and verify that the container
     * begins a new global transaction. Verify container completes global
     * transaction prior to returning to caller of method. Verify caller's
     * global transaction is still active when container returns to caller.
     *
     */
    @Test
    public void testWildCardXmlDemarcationPrecedenceRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, expClassExpAllxmlBean2, not null", expClassExpAllxmlBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expClassExpMethodExpAllXML method
            if (expClassExpAllxmlBean2.expClassExpMethodExpAllXML(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence.");
            } else {
                fail("The method returned false which means the XML override failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * 1)Not taking the XML into account, the bean (and by default its methods)
     * explicitly has Tx Attr of NEVER as set via annotation at the class level
     * 2)XML sets this method to have the trans-attribute of RequiresNew 3)The
     * XML should take precedence
     *
     * While thread is currently associated with a transaction context, call a
     * method that is setup as described above and verify that the container
     * begins a new global transaction. Verify container completes global
     * transaction prior to returning to caller of method. Verify caller's
     * global transaction is still active when container returns to caller.
     *
     * To verify this, the caller must begin a global transaction prior to
     * calling this method.
     *
     */
    @Test
    public void testWildCardXmlDemarcationPrecedenceSpecificMthd() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, expClassExpXMLMethodBean1, not null", expClassExpXMLMethodBean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expClassExpXMLMethod method
            if (expClassExpXMLMethodBean1.expClassExpXMLMethod(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence.");
            } else {
                fail("The method returned false. Meaning both the XML override and the class level demarcation of NEVER failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * 1)Not taking the XML into account, the bean (and by default its methods)
     * explicitly has Tx Attr of NEVER as set via annotation at the class level
     * 2)XML sets this method to have the trans-attribute of RequiresNew 3)The
     * XML should take precedence
     *
     * While thread is currently associated with a transaction context, call a
     * method that is setup as described above and verify that the container
     * begins a new global transaction. Verify container completes global
     * transaction prior to returning to caller of method. Verify caller's
     * global transaction is still active when container returns to caller.
     *
     * To verify this, the caller must begin a global transaction prior to
     * calling this method.
     *
     */
    @Test
    public void testWildCardXmlDemarcationPrecedenceSpecificMthdRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, expClassExpXMLMethodBean2, not null", expClassExpXMLMethodBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expClassExpXMLMethod method
            if (expClassExpXMLMethodBean2.expClassExpXMLMethod(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence.");
            } else {
                fail("The method returned false. Meaning both the XML override and the class level demarcation of NEVER failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * 1)Not taking the XML into account the bean (and thus implicitly its
     * methods) explicitly has Tx Attr of NEVER as set via annotation at the
     * class level 2)There is no XML used to override this method 3)The class
     * annotation should take precedence
     *
     * To verify, when a method with a NEVER transaction attribute is called
     * while the thread is currently associated with a global transaction the
     * container throws a javax.ejb.EJBException. The caller must begin a global
     * transaction prior to calling this method.
     *
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSIException" })
    public void testAnnMethodAXmlMethodB() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, expClassExpXMLMethodBean1, not null", expClassExpXMLMethodBean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expClassNoXMLOverride method
            try {
                expClassExpXMLMethodBean1.expClassNoXMLOverride();
                fail("Container did not throw the expected javax.ejb.EJBException.");
            } catch (EJBException ex) {
                svLogger.info("Container threw expected EJBException.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * 1)Not taking the XML into account the bean (and thus implicitly its
     * methods) explicitly has Tx Attr of NEVER as set via annotation at the
     * class level 2)There is no XML used to override this method 3)The class
     * annotation should take precedence
     *
     * To verify, when a method with a NEVER transaction attribute is called
     * while the thread is currently associated with a global transaction the
     * container throws a javax.ejb.EJBException. The caller must begin a global
     * transaction prior to calling this method.
     *
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSIException" })
    public void testAnnMethodAXmlMethodBRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, expClassExpXMLMethodBean2, not null", expClassExpXMLMethodBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expClassNoXMLOverride method
            try {
                expClassExpXMLMethodBean2.expClassNoXMLOverride();
                fail("Container did not throw the expected javax.ejb.EJBException.");
            } catch (EJBException ex) {
                svLogger.info("Container threw expected EJBException.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * 1)Not taking the XML into account, this method will explicitly have Tx
     * Attr of NEVER as set via annotation.. 2)XML uses the * to set all methods
     * to have the trans-attribute of RequiresNew 3)The XML should take
     * precedence
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container begins a new global transaction. Verify container completes
     * global transaction prior to returning to caller of method. Verify
     * caller's global transaction is still active when container returns to
     * caller.
     *
     */
    @Test
    public void testXmlDemarcationPrecedenceOverAnn() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, expMethodExpAllxmlBean1, not null", expMethodExpAllxmlBean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expMethodExpAllXML method
            try {
                if (expMethodExpAllxmlBean1.expMethodExpAllXML(tid)) {
                    svLogger.info("Container began new global transaction thus the XML took precedence.");
                } else {
                    fail("The method returned false which means the XML override failed.");
                }
            } catch (EJBException ex) {
                fail("Container threw an EJBException which likely means that the XML override "
                     + "failed and that the method level annotation demarcation was used.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * 1)Not taking the XML into account, this method will explicitly have Tx
     * Attr of NEVER as set via annotation.. 2)XML uses the * to set all methods
     * to have the trans-attribute of RequiresNew 3)The XML should take
     * precedence
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container begins a new global transaction. Verify container completes
     * global transaction prior to returning to caller of method. Verify
     * caller's global transaction is still active when container returns to
     * caller.
     *
     */
    @Test
    public void testXmlDemarcationPrecedenceOverAnnRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, expMethodExpAllxmlBean2, not null", expMethodExpAllxmlBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expMethodExpAllXML method
            try {
                if (expMethodExpAllxmlBean2.expMethodExpAllXML(tid)) {
                    svLogger.info("Container began new global transaction thus the XML took precedence.");
                } else {
                    fail("The method returned false which means the XML override failed.");
                }
            } catch (EJBException ex) {
                fail("Container threw an EJBException which likely means that the XML override "
                     + "failed and that the method level annotation demarcation was used.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * 1)Not taking the XML into account this method will explicitly have Tx
     * Attr of NEVER set via method level annotation. 2)XML sets this method to
     * have the trans-attribute of RequiresNew 3)The XML should take precedence
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container begins a new global transaction. Verify container completes
     * global transaction prior to returning to caller of method. Verify
     * caller's global transaction is still active when container returns to
     * caller.
     *
     */
    @Test
    public void testXmlDemarcationPrecedenceOverAnnMthdLevel() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, expMethodExpXMLMethodBean1, not null", expMethodExpXMLMethodBean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expMethodExpXMLMethod method
            try {
                if (expMethodExpXMLMethodBean1.expMethodExpXMLMethod(tid)) {
                    svLogger.info("Container began new global transaction thus the XML took precedence.");
                } else {
                    fail("The method returned false which means the XML override failed.");
                }
            } catch (EJBException ex) {
                fail("Container threw an EJBException which likely means that the XML override "
                     + "failed and that the method level annotation demarcation was used.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * 1)Not taking the XML into account this method will explicitly have Tx
     * Attr of NEVER set via method level annotation. 2)XML sets this method to
     * have the trans-attribute of RequiresNew 3)The XML should take precedence
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container begins a new global transaction. Verify container completes
     * global transaction prior to returning to caller of method. Verify
     * caller's global transaction is still active when container returns to
     * caller.
     *
     */
    @Test
    public void testXmlDemarcationPrecedenceOverAnnMthdLevelRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, expMethodExpXMLMethodBean2, not null", expMethodExpXMLMethodBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call expMethodExpXMLMethod method
            try {
                if (expMethodExpXMLMethodBean2.expMethodExpXMLMethod(tid)) {
                    svLogger.info("Container began new global transaction thus the XML took precedence.");
                } else {
                    fail("The method returned false which means the XML override failed.");
                }
            } catch (EJBException ex) {
                fail("Container threw an EJBException which likely means that the XML override "
                     + "failed and that the method level annotation demarcation was used.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * 1)Not taking the XML into account, the bean (and thus its methods)
     * implicitly has Tx Attr of REQUIRED. 2)There is no XML used to override
     * this method - however there is XML to override another method in this
     * same bean 3)The implicit Tx Attr of REQUIRED should take precedence
     *
     * Note:There is another method in this bean which explicitly has a Tx Attr
     * value of NEVER set via annotation - this is tested in
     * testXmlDemarcationPrecedenceOverAnnMthdLevel().
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container uses the caller's transaction. Verify caller's global
     * transaction is still active when container returns to caller.
     *
     */
    @Test
    public void testImplicitTxREQUIREDUse() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, expMethodExpXMLMethodBean1, not null", expMethodExpXMLMethodBean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call impClassNoXMLOverride method
            try {
                if (expMethodExpXMLMethodBean1.impClassNoXMLOverride(tid)) {
                    svLogger.info("Container used the caller's transaction thus the implicit Tx attr of REQUIRED was properly used.");
                } else {
                    fail("The method returned false which is unexpected since there is no XML override for this method.");
                }
            } catch (EJBException ex) {
                fail("Container threw an EJBException which is unexpected since there is no XML override for this method."
                     + "The annotation demarcation for another method in this bean is NEVER so perhaps it was wrongfully used.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * 1)Not taking the XML into account, the bean (and thus its methods)
     * implicitly has Tx Attr of REQUIRED. 2)There is no XML used to override
     * this method - however there is XML to override another method in this
     * same bean 3)The implicit Tx Attr of REQUIRED should take precedence
     *
     * Note:There is another method in this bean which explicitly has a Tx Attr
     * value of NEVER set via annotation - this is tested in
     * testXmlDemarcationPrecedenceOverAnnMthdLevel().
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container uses the caller's transaction. Verify caller's global
     * transaction is still active when container returns to caller.
     *
     */
    @Test
    public void testImplicitTxREQUIREDUseRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, expMethodExpXMLMethodBean2, not null", expMethodExpXMLMethodBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call impClassNoXMLOverride method
            if (expMethodExpXMLMethodBean2.impClassNoXMLOverride(tid)) {
                svLogger.info("Container used the caller's transaction thus the implicit Tx attr of REQUIRED was properly used.");
            } else {
                fail("The method returned false which is unexpected since there is no XML override for this method.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * 1)Not taking the XML into account, the bean (and by default its methods)
     * implicitly has Tx Attr of REQUIRED. 2)XML uses the * to set all methods
     * to have the trans-attribute of RequiresNew 3)The XML should take
     * precedence
     *
     * Just for fun there are two methods in this bean to ensure that the wild
     * card XML demarcation actually applies to all methods in the bean.
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container begins a new global transaction. Verify container completes
     * global transaction prior to returning to caller of method. Verify
     * caller's global transaction is still active when container returns to
     * caller.
     *
     */
    @Test
    public void testWildCardXmlPrecedenceOverImpTxREQUIRED() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, impClassExpAllxmlBean1, not null", impClassExpAllxmlBean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call impClassExpAllXML1 method
            if (impClassExpAllxmlBean1.impClassExpAllXML1(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence for impClassExpAllXML1().");
            } else {
                fail("The method returned false which means the XML override for impClassExpAllXML1() failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the impClassExpAllXML1() method.", FATTransactionHelper.isSameTransactionId(tid));

            // call impClassExpAllXML2 method
            if (impClassExpAllxmlBean1.impClassExpAllXML2(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence for impClassExpAllXML2().");
            } else {
                fail("The method returned false which means the XML override for impClassExpAllXML2() failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the impClassExpAllXML2() method.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * 1)Not taking the XML into account, the bean (and by default its methods)
     * implicitly has Tx Attr of REQUIRED. 2)XML uses the * to set all methods
     * to have the trans-attribute of RequiresNew 3)The XML should take
     * precedence
     *
     * Just for fun there are two methods in this bean to ensure that the wild
     * card XML demarcation actually applies to all methods in the bean.
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container begins a new global transaction. Verify container completes
     * global transaction prior to returning to caller of method. Verify
     * caller's global transaction is still active when container returns to
     * caller.
     *
     */
    @Test
    public void testWildCardXmlPrecedenceOverImpTxREQUIREDRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, impClassExpAllxmlBean2, not null", impClassExpAllxmlBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call impClassExpAllXML1 method
            if (impClassExpAllxmlBean2.impClassExpAllXML1(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence for impClassExpAllXML1().");
            } else {
                fail("The method returned false which means the XML override for impClassExpAllXML1() failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the impClassExpAllXML1() method.", FATTransactionHelper.isSameTransactionId(tid));

            // call impClassExpAllXML2 method
            if (impClassExpAllxmlBean2.impClassExpAllXML2(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence for impClassExpAllXML2().");
            } else {
                fail("The method returned false which means the XML override for impClassExpAllXML2() failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the impClassExpAllXML2() method.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * 1)Not taking the XML into account, the bean (and by default its methods)
     * implicitly has Tx Attr of REQUIRED. 2)XML sets one of the methods to have
     * the trans-attribute of RequiresNew 3)The XML should take precedence
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container begins a new global transaction. Verify container completes
     * global transaction prior to returning to caller of method. Verify
     * caller's global transaction is still active when container returns to
     * caller.
     *
     */
    @Test
    public void testWildCardXmlPrecedenceOverImpTxREQUIREDSepcificMthd() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, impClassExpXMLMethodBean1, not null", impClassExpXMLMethodBean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call impClassExpXMLMethod method
            if (impClassExpXMLMethodBean1.impClassExpXMLMethod(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence.");
            } else {
                fail("The method returned false which means the XML override failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * 1)Not taking the XML into account, the bean (and by default its methods)
     * implicitly has Tx Attr of REQUIRED. 2)XML sets one of the methods to have
     * the trans-attribute of RequiresNew 3)The XML should take precedence
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container begins a new global transaction. Verify container completes
     * global transaction prior to returning to caller of method. Verify
     * caller's global transaction is still active when container returns to
     * caller.
     *
     */
    @Test
    public void testWildCardXmlPrecedenceOverImpTxREQUIREDSepcificMthdRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, impClassExpXMLMethodBean2, not null", impClassExpXMLMethodBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call impClassExpXMLMethod method
            if (impClassExpXMLMethodBean2.impClassExpXMLMethod(tid)) {
                svLogger.info("Container began new global transaction thus the XML took precedence.");
            } else {
                fail("The method returned false which means the XML override failed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * 1)Not taking the XML into account, the bean (and thus its methods)
     * implicitly has Tx Attr of REQUIRED. 2)There is no XML used to override
     * this method - however there is XML to override another method in this
     * same bean (see test 17)to be RequiresNew 3)The implicit Tx Attr of
     * REQUIRED should be used
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container uses the caller's transaction. Verify caller's global
     * transaction is still active when container returns to caller.
     *
     * public boolean impClassNoXMLOverride( byte[] tid )
     *
     */
    @Test
    public void testImplicitTxREQUIREDUseBothAnnXml() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, impClassExpXMLMethodBean1, not null", impClassExpXMLMethodBean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call impClassNoXMLOverride method
            if (impClassExpXMLMethodBean1.impClassNoXMLOverride(tid)) {
                svLogger.info("Container used the caller's transaction thus the implicit Tx attr of REQUIRED was properly used.");
            } else {
                fail("The method returned false which is unexpected since there is no XML override for this method.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * 1)Not taking the XML into account, the bean (and thus its methods)
     * implicitly has Tx Attr of REQUIRED. 2)There is no XML used to override
     * this method - however there is XML to override another method in this
     * same bean (see test 17)to be RequiresNew 3)The implicit Tx Attr of
     * REQUIRED should be used
     *
     * To verify, while thread is currently associated with a transaction
     * context, call a method that is setup as described above and verify that
     * the container uses the caller's transaction. Verify caller's global
     * transaction is still active when container returns to caller.
     *
     * public boolean impClassNoXMLOverride( byte[] tid )
     *
     */
    @Test
    public void testImplicitTxREQUIREDUseBothAnnXmlRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, impClassExpXMLMethodBean2, not null", impClassExpXMLMethodBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call impClassNoXMLOverride method
            if (impClassExpXMLMethodBean2.impClassNoXMLOverride(tid)) {
                svLogger.info("Container used the caller's transaction thus the implicit Tx attr of REQUIRED was properly used.");
            } else {
                fail("The method returned false which is unexpected since there is no XML override for this method.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }
}