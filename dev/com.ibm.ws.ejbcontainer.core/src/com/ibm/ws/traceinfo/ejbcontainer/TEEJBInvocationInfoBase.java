/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.traceinfo.ejbcontainer;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.EJSDeployedSupport;
import com.ibm.ejs.container.EJSWrapperBase;

/**
 * This is the base class of all the EJB Invocation Info objects.
 * It saves all the necessary information to reflect the current state of a method
 * call. At runtime, ejbcontainer server code invokes the appropriate subclass's write method
 * to write out a trace entry to the trace log which contains method call relevant information
 * for the tool to "read" it back.
 * This class is responsible to make the write and read operations are parallel and
 * consistent.
 */
public class TEEJBInvocationInfoBase implements TEInfoConstants
{
    /**
     * Helper method to convert a Class object to its textual representation.
     * If class is null, the <i>NullDefaultStr</i> is returned.
     */
    protected static String turnNullClass2EmptyString(Class<?> cls, String NullDefaultStr)
    {
        return (cls == null) ? NullDefaultStr : cls.toString();
    }

    /**
     * Helper method to convert a String object to its textual representation.
     * If class is null, the <i>NullDefaultStr</i> is returned.
     */
    protected static String turnNullString2EmptyString(String str, String NullDefaultStr)
    {
        return (str == null) ? NullDefaultStr : str;
    }

    /**
     * Collects all the method call information from the input EJSDeploySupport, EJSWrapperBase
     * and Throwable objects, converts them to their textual representation and write it out
     * to the input <i>sbuf</i>.
     */
    protected static void writeDeployedSupportInfo(EJSDeployedSupport s, StringBuffer sbuf, EJSWrapperBase wrapper, Throwable t)
    {
        BeanId beanId = wrapper.beanId; // d163197
        EJBMethodInfoImpl mInfo = s.getEJBMethodInfoImpl();
        ContainerTx containerTx = s.getCurrentTx();

        sbuf
                        .append(s.getEJBMethodId()).append(DataDelimiter)
                        .append((mInfo != null) ? mInfo.getMethodName() : UnknownValue).append(DataDelimiter)
                        .append((mInfo != null) ? mInfo.getMethodSignature() : UnknownValue).append(DataDelimiter)
                        .append((mInfo != null) ? (mInfo.isHome() ? "true" : "false") : UnknownValue).append(DataDelimiter)
                        .append(s.beganInThisScope()).append(DataDelimiter)
                        .append(containerTx != null ? containerTx.toString() : NullPointerMarker).append(DataDelimiter)
                        .append(containerTx != null
                                        ? (containerTx.isTransactionGlobal()
                                                        ? GlobalTxType
                                                        : LocalTxType)
                                        : UnSpecifiedTxType).append(DataDelimiter)
                        .append(beanId == null
                                        ? (mInfo != null && mInfo.isHome()
                                                        ? HomeMethodBeanId
                                                        : NonEJBInvocationBeanId)
                                        : beanId.getIdString()).append(DataDelimiter)
                        .append(t == null ? NullPointerMarker : t.getClass().getName()).append(DataDelimiter);
    }
}
