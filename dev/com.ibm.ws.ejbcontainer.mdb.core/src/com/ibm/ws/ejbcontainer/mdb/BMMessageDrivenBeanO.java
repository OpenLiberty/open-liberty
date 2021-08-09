/*******************************************************************************
 * Copyright (c) 2001, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb;

import java.io.Serializable;

import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.MessageDrivenBeanO;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * BMMessageDrivenBeanO manages the lifecycle of a single MessageDrivenBean
 * instance for the case of a MessageDriven defined to BeanManaged transactions,
 * it also provides the MessageDrivenConext implementation for the enterprise bean.
 * <p>
 **/
public class BMMessageDrivenBeanO
                extends MessageDrivenBeanO
                implements Serializable
{
    private static final long serialVersionUID = -5207521013489319578L;

    private static final TraceComponent tc = Tr.register(BMMessageDrivenBeanO.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * Create new <code>BMMessageDrivenBeanO</code>. <p>
     */

    public BMMessageDrivenBeanO(EJSContainer c, EJSHome h)
    {
        super(c, h);
    }

    @Override
    public boolean getRollbackOnly() { //d116376

        //do not allow getRollbackOnly for bean-managed MDB's
        Tr.error(tc, "METHOD_NOT_ALLOWED_CNTR0047E",
                 "BMMessageDrivenBeanO.getRollbackOnly()");
        throw new IllegalStateException("Method Not Allowed Exception: See Message-drive Bean Component Contract section of the applicable EJB Specification.");
    }

    @Override
    public void setRollbackOnly() { //d116376

        //do not allow setRollbackOnly for bean-managed MDB's
        Tr.error(tc, "METHOD_NOT_ALLOWED_CNTR0047E",
                 "BMMessageDrivenBeanO.setRollbackOnly()");
        throw new IllegalStateException("Method Not Allowed Exception: See Message-drive Bean Component Contract section of the applicable EJB Specification.");
    }

}
