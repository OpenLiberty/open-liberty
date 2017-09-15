/*******************************************************************************
 * Copyright (c) 1998, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.RemoteException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


/**
 * Create new <code>BMStatelessBeanO</code>. <p>
 **/
public class BMStatelessBeanO
                extends StatelessBeanO
{
    private static final TraceComponent tc =
                    Tr.register(BMStatelessBeanO.class,
                                "EJBContainer",
                                "com.ibm.ejs.container.container");//121558

    /**
     * Create new <code>BMStatelessBeanO</code>. <p>
     */
    public BMStatelessBeanO(EJSContainer c, EJSHome h)
    {
        super(c, h);
    } // BMStatelessBeanO

    /**
     * setRollbackOnly - This method is illegal for bean managed stateless
     * session beans
     */
    @Override
    public void setRollbackOnly()
    {
        throw new IllegalStateException();
    } // setRollbackOnly

    /**
     * getRollbackOnly - This method is illegal for bean managed stateless
     * session beans
     */
    @Override
    public boolean getRollbackOnly()
    {
        throw new IllegalStateException();
    } // getRollbackOnly

    //d170394
    @Override
    public final void postInvoke(int id, EJSDeployedSupport s)
                    throws RemoteException
    {
        if (state == DESTROYED) {
            return;
        }
        ContainerTx tx = null;
        if (null == ivContainerTx) { //d170394
            tx = ivContainerTx;
        } else {
            tx = container.getCurrentTx(false);
        }

        //167937 - discard bean if BMT was started and is still active.

        if (tx != null && tx.isBmtActive(s.methodInfo))
        {
            // BMT is still active.  Discard bean and let the BeanManaged.postInvoke
            // do the rollback and throwing of the exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc,
                         "Stateless SB method is not allowed to leave a BMT active. " +
                                         "Discarding bean.");
            discard();
        }

        ivContainerTx = null;//d170394
    }

} // BMStatelessBeanO
