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

import javax.ejb.EJBException;

public interface BaseAnnotationOverByXMLTxAttrBeanRemote {
    /**
     * 1)The SuperClass has class level demarcation of TX attr = NEVER and thus
     * its methods should implicitly be set to NEVER. 2)This method is not
     * implemented in the BaseClass. 3)The BaseClass's XML will either use the
     * *(wild card) to explicitly set all methods to have the trans-attribute of
     * RequiresNew OR use XML to explicitly set this specific method to have the
     * trans-attribute of RequiresNew. 4)The XML defined in the BaseClass should
     * take precedence
     *
     * The caller must begin a global transaction prior to calling this method.
     *
     * Since this is the SuperClass implementation with a TX attribute of NEVER
     * and the method is called while the thread is currently associated with a
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
    public String superAnnotationMethod(byte[] tid) throws EJBException;

    /**
     * 1)The SuperClass has class level demarcation of TX attr = NEVER and thus
     * its methods should implicitly be set to NEVER. 2)This method will be
     * implemented in the BaseClass in the same manner as the SuperClass. 3)The
     * BaseClass's XML will either use the *(wild card) to explicitly set all
     * methods to have the trans-attribute of RequiresNew OR use XML to
     * explicitly set the above method to have the trans-attribute of
     * RequiresNew. 4)The XML defined in the BaseClass should take precedence if
     * the wild card is used - otherwise the TX attr of NEVER should be used.
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
    public String superAnnotationMethod2(byte[] tid) throws EJBException;
}