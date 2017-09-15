/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.ibm.ejs.container.BeanId;

/**
 * A <code>SessionBeanStore</code> provides an EJB container with a
 * mechanism for swapping out/in stateful session bean's conversational
 * state. <p>
 */
public interface SessionBeanStore
{
    /**
     * Get an <code>GZIPInputStream</code> that can be used to unzip the
     * compressed/zipped state for the given <code>StatefulSessionKey</code>. <p>
     * 
     * @param beanId the <code>BeanId</code> that identifies
     *            which stateful session bean's conversational state to read <p>
     * 
     * @exception CSIException thrown if the desired input stream cannot
     *                be obtained for any reason <p>
     */
    public GZIPInputStream getGZIPInputStream(BeanId beanId) //d204278.2
    throws CSIException;

    /**
     * Get an <code>GZIPOutputStream</code> that can be used to write the
     * compressed/zipped state for the stateful session identified by the
     * given <code>StatefulSessionKey</code>. <p>
     * 
     * @param beanId the <code>BeanId</code> that identifies
     *            which stateful session bean's conversational state will be
     *            written <p>
     * 
     * @exception CSIException thrown if the desired output stream cannot
     *                be obtained for any reason <p>
     */
    public GZIPOutputStream getGZIPOutputStream(BeanId beanId) //d204278.2
    throws CSIException;

    /**
     * Discard any conversational state this <code>SessionBeanStore</code>
     * might have stored for the given stateful session bean. <p>
     */
    public void remove(BeanId beanId);

    /**
     * Get an <code>OutputStream</code> that can be used to write the
     * conversational state for the stateful session identified by the
     * given <code>StatefulSessionKey</code>. Note, if data compression
     * is desired, the caller must compress the data prior to using
     * this OutputStream since the OutputStream returned will not do
     * any compression of the data written to this stream. <p>
     * 
     * @param beanId the <code>BeanId</code> that identifies
     *            which stateful session bean's conversational state will be
     *            writtten <p>
     * 
     * @exception CSIException thrown if the desired output stream cannot
     *                be obtained for any reason <p>
     */
    public OutputStream getOutputStream(BeanId beanId) //d204278.2
    throws CSIException; //LIDB2018-1

    /**
     * Get an <code>InputStream</code> that can be used to read the
     * conversational state for the given <code>StatefulSessionKey</code>.
     * Note, if read from this input stream is compressed, the caller
     * is responsible for decompressing the data if so desired since
     * the input stream returned by this method will not decompress
     * the data as it is read from the input stream. <p>
     * 
     * @param beanId the <code>BeanId</code> that identifies
     *            which stateful session bean's conversational state to read <p>
     * 
     * @exception CSIException thrown if the desired input stream cannot
     *                be obtained for any reason <p>
     */
    public InputStream getInputStream(BeanId beanId)
                    throws CSIException; //LIDB2018-1
}
