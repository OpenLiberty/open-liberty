/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.bci;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Interface that must be implemented by method adapters that wish to plug into
 * the trace instrumentation framework.
 */
public abstract class RasMethodAdapter extends MethodVisitor {

    public RasMethodAdapter(MethodVisitor visitor) {
        super(Opcodes.ASM8, visitor);
    }

    /**
     * Generate the code required to initialize the trace object field. The
     * generated code will exist in a static method used during class static
     * initialization and may not access access any field other than the
     * trace object field.
     */
    public abstract void initializeTraceObjectField();

    /**
     * Generate the code required to trace method entry. This method
     * is called for all methods including:
     * <ul>
     * <li><strike>static initializers (<code>&lt;clinit&gt;</code>)</strike></li>
     * <li>constructors (<code>&lt;init&gt;</code>)</li>
     * <li>static methods</li>
     * <li>declared instance methods</li>
     * </ul>
     * The generated code must not declare new local variables and should
     * use the stack to handle method arguments.
     *
     * @return true if this adapter modified the method byte code
     */
    public abstract boolean onMethodEntry();

    /**
     * Generate the code required to trace method exit.
     * <ul>
     * <li><strike>static initializers (<code>&lt;clinit&gt;</code>)</strike></li>
     * <li>constructors (<code>&lt;init&gt;</code>)</li>
     * <li>static methods</li>
     * <li>declared instance methods</li>
     * </ul>
     * The generated code must declare new local variables. The result of
     * the method (if any) is on the top of the stack and must preserved by
     * the generated code.
     *
     * @return true if this adapter modified the method byte code
     */
    public abstract boolean onMethodReturn();

    /**
     * Generate the code required to trace entry to an exception handler. The <code>java.lang.Throwable</code> that
     * was caught is either on the top of the stack if <code>var</code> is -1
     * (and the value must be preserved by the generated code), or otherwise, it
     * is stored in the local variable <code>var</code>.
     * </ul>
     * stack and must be preserved by the generated code.
     * <p>
     * This method is not called for <code>finally</code> blocks.
     * </p>
     *
     * @param exception
     *            the <code>Type</code> of exceptions caught by this
     *            exception handler
     * @return true if this adapter modified the method byte code
     */
    public abstract boolean onExceptionHandlerEntry(Type exception, int var);

    /**
     * Generate the code required to trace the explicit throw of an Exception.
     * The <code>java.lang.Throwable</code> that is being thrown is on the top of
     * the stack and must be preserved by the generated code.
     *
     * @return true if this adapter modified the method byte code
     */
    public abstract boolean onThrowInstruction();

}
