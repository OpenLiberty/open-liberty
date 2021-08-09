/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import com.ibm.websphere.csi.MethodInterface;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.MethodMetaData;

/**
 * This class provides a stack of pre-allocated EJBMethodInfoImpl objects.
 * 
 * Callers obtain an EJBMethodInfo instance by calling the get() method prior to
 * invoking the target method on the EJB implementation class. Callers must call the
 * done() method after invocation of the target EJB implementation method completes.
 * This allows the stack to wind and unwind in parallel to any nested calls from
 * EJB to EJB on the same thread within the server.
 * 
 * The get() method will always return a usable EJSBMethodInfoImpl instance. If no
 * pre-allocated instances are available, a new EJBMethodInfoImpl
 * instance will be created. The caller must still call the done() method as before.
 * 
 */

public class EJBMethodInfoStack
{
    private static final TraceComponent tc =
                    Tr.register(EJSContainer.class, "EJBContainer",
                                "com.ibm.ejs.container.EJBMethodInfoStack");//d151861

    /**
     * The objects managed by this stack.
     */
    private EJBMethodInfoImpl[] elements;
    private int topOfStack = 0;
    private boolean ivSetup = false;
    private int capacity = 0;
    private boolean orig = false;

    /**
     * Construct capacity sized stack
     */
    private void setup(BeanMetaData bmd)
    {
        if (!ivSetup)
        {
            int slotSize = bmd.container.getEJBRuntime().getMetaDataSlotSize(MethodMetaData.class);
            for (int i = 0; i < capacity; ++i)
            {
                EJBMethodInfoImpl methodInfo = bmd.createEJBMethodInfoImpl(slotSize);
                methodInfo.initializeInstanceData(null, null, null, null, null, false);
                elements[i] = methodInfo;
            }
            ivSetup = true;
        }
    }

    public EJBMethodInfoStack(int cap)
    {
        capacity = cap;
        elements = new EJBMethodInfoImpl[capacity];
    } // EJBMethodInfoStack

    /**
     * Indicate that the caller is finished with the instance that was last obtained.
     */
    public final void done(EJBMethodInfoImpl mi)
    {
        //d151861
        if (orig || (mi == null) || (topOfStack == 0))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "In orig mode returning:" + "  orig: " + orig +
                             " top: " + topOfStack + " mi:  " + mi);
            orig = true;
            elements = null;//d166651
        }
        //d151861
        else
        {
            --topOfStack;
            if (topOfStack < capacity)
            {
                //d156621
                if (mi != (elements[topOfStack]))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "EJBMethodInfoStack::done called with wrong " +
                                     "TopOfStack value: " + mi + "!=" + (elements[topOfStack]));
                    orig = true;
                    elements = null;//d166651
                }
                else
                    elements[topOfStack].initializeInstanceData(null, null, null, // 199625
                                                                null, null, false);//d162441
                //d156621
            }
        }
    } // done

    /**
     * Get an instance of EJBMethodInfoImpl.
     * Either return EJBMethod off the stack or new up a new instance after
     * stack capacity is exhausted returns EJBMethodInfoImpl
     */
    final public EJBMethodInfoImpl get(String methodSignature,
                                       String methodNameOnly,
                                       EJSWrapperBase wrapper,
                                       MethodInterface methodInterface, // d164221
                                       TransactionAttribute txAttr) // 199625
    {
        EJBMethodInfoImpl retVal = null;
        BeanMetaData bmd = wrapper.bmd;
        setup(bmd);// delay initting array so we can get slot count
        //d151861
        if ((topOfStack < 0) || orig)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "EJBMethodInfoStack::get called with neg TopOfStack " +
                             "or in orig mode:" + topOfStack + " orig: " + orig);
            orig = true;
            elements = null;//d166651
            retVal = bmd.createEJBMethodInfoImpl(bmd.container.getEJBRuntime().getMetaDataSlotSize(MethodMetaData.class));

        }//d151861
        else
        {
            if (topOfStack < elements.length)
            {
                retVal = elements[topOfStack++];
            }
            else
            {
                ++topOfStack;
                retVal = bmd.createEJBMethodInfoImpl(bmd.container.getEJBRuntime().getMetaDataSlotSize(MethodMetaData.class));
            }
        }

        retVal.initializeInstanceData(null, methodNameOnly, bmd, methodInterface, txAttr, false);
        return retVal;
    }

} // EJBMethodInfoImplStack
