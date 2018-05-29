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
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb;

import static javax.ejb.TransactionAttributeType.NEVER;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.TransactionAttribute;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * Bean implementation class for Enterprise Bean:
 * BaseAnnotationOverByXMLTxAttrBean1 This bean is used to verify that the XML
 * used in the Base Bean takes precedence over the SuperBean's annotation
 * demarcation of TX attributes.
 *
 * This bean uses the *(wild card) in the XML to set the trans-attribute for all
 * methods to RequiresNew.
 **/
@Local(BaseAnnotationOverByXMLTxAttrBeanLocal.class)
@Remote(BaseAnnotationOverByXMLTxAttrBeanRemote.class)
@TransactionAttribute(NEVER)
public class BaseAnnotationOverByXMLTxAttrBean1 extends SuperAnnotationOverByXMLTxAttrBean {
    /**
     * 1)The SuperClass has class level demarcation of TX attr = NEVER and thus
     * its methods should implicitly be set to NEVER. 2)This method will be
     * implemented in the BaseClass in the same manner as the SuperClass. 3)The
     * BaseClass's XML will either use the *(wild card) to explicitly set all
     * methods to have the trans-attribute of RequiresNew OR use XML to
     * explicitly set the superAnnotationMethod2 method to have the
     * trans-attribute of RequiresNew. 4)The XML defined in the BaseClass should
     * take precedence if the wild card is used - otherwise the TX attr of NEVER
     * should be used.
     *
     * The caller must begin a global transaction prior to calling this method.
     *
     * Since this method will be implemented with a TX attribute of NEVER and
     * the method is called while the thread is currently associated with a
     * global transaction the container will throw a javax.ejb.EJBException IF
     * NOT overridden by the BC's XML.
     *
     * To verify it was overridden by the BC's XML, when a method with a
     * RequiresNew transaction attribute is called while the calling thread is
     * currently associated with a global transaction it causes the container to
     * dispatch the method in a new global transaction context (e.g container
     * does begin a new global transaction). Again, the caller must begin a
     * global transaction prior to calling this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return String override = "XML" if method is dispatched in a global
     *         transaction with a global transaction ID that does not match the
     *         tid parameter (i.e. the XML override worked). Otherwise an error
     *         message is returned in String override.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    @Override
    public String superAnnotationMethod2(byte[] tid) {
        String override = "FROM BASECLASS:The xml override failed and if that failed this method "
                          + "should have used the BaseClass implementation and thrown a javax.ejb.EJBException "
                          + "which it didn't. In short bad things happened.";
        byte[] myTid = FATTransactionHelper.getTransactionId();
        if (myTid == null) {
            return override = "Failure: myTid == null.  This should not be the case.";
        }

        if (FATTransactionHelper.isSameTransactionId(tid) == false) {
            override = "XML";
        }

        return override;
    }
}