/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe.bci.internal;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.AnalyzerAdapter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class RequestProbeMethodAdapter extends AdviceAdapter {
    private static final TraceComponent tc = Tr.register(RequestProbeMethodAdapter.class);

    private final MethodVisitor visitor;

    private final MethodInfo methodInfo;
    private final String _type;
    private final String _classname;
    private int tdHelper = newLocal(Type.getType("Lcom/ibm/wsspi/request/probe/bci/TransformDescriptorHelper"));
    
    private final String td;

    protected RequestProbeMethodAdapter(MethodVisitor visitor, MethodInfo mInfo, String timedOpsType, String classname, 
    			String td, AnalyzerAdapter aa) {
        super(ASM5, visitor, mInfo.getAccessFlags(), mInfo.getMethodName(), mInfo.getDescriptor());
        this.visitor = visitor;
        this.methodInfo = mInfo;
        this._classname = classname;
        this._type = timedOpsType;
        this.td = td;
    }

    protected String getMethodName() {
        return methodInfo.getMethodName();
    }

    protected String getDescriptor() {
        return methodInfo.getDescriptor();
    }

    protected boolean isConstructor() {
        return "<init>".equals(getMethodName());
    }

    protected boolean isStaticInitializer() {
        return isStatic() && "<clinit>".equals(getMethodName());
    }

    protected int getAccessFlags() {
        return methodInfo.getAccessFlags();
    }

    protected boolean isStatic() {
        return (getAccessFlags() & ACC_STATIC) != 0;
    }

    protected String getSignature() {
        return methodInfo.getSignature();
    }

    public Type[] getArgumentTypes() {
        return Type.getArgumentTypes(getDescriptor());
    }

    public Type getReturnType() {
        return Type.getReturnType(getDescriptor());
    }

    protected Set<Type> getDeclaredExceptions() {
        Set<Type> exceptionSet = new HashSet<Type>();
        for (String exception : methodInfo.getDeclaredExceptions()) {
            exceptionSet.add(Type.getType(exception));
        }
        return exceptionSet;
    }

    protected void createParameterArray() {
        // Static methods don't have an implicit "this" argument so start
        // working with slot 0 instead of 1 for the parameter list.
        int localVarOffset = isStatic() ? 0 : 1;
        Type[] argTypes = getArgumentTypes();

        // Build the object array that will hold the input arguments to the method.
        createObjectArray(argTypes.length);

        for (int i = 0; i < argTypes.length; i++) {
            int j = i + localVarOffset;

            visitInsn(DUP);
            visitLdcInsn(Integer.valueOf(i));
            visitVarInsn(argTypes[i].getOpcode(ILOAD), j);
            box(argTypes[i]);
            visitInsn(AASTORE);

            // Local variables can use more than one slot.  (DJ)
            // Account for those here by adding them to the local
            // var offset.
            localVarOffset += argTypes[i].getSize() - 1;
        }
    }

    /**
     * Create an object array with the specified capacity.
     * 
     * @param capacity the size of the array to allocate
     */
    protected void createObjectArray(int capacity) {
        visitLdcInsn(Integer.valueOf(capacity));
        visitTypeInsn(ANEWARRAY, "java/lang/Object");
    }

    @Override
    protected void onMethodEnter() {
    	if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
    		Tr.debug(tc, "onMethodEnter", "[methodName=" + getMethodName() + ", descriptor=" +  getDescriptor() + "]");
    	}
    	
    	visitor.visitTypeInsn(Opcodes.NEW, "com/ibm/wsspi/request/probe/bci/TransformDescriptorHelper");
        visitor.visitInsn(DUP);
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/ibm/wsspi/request/probe/bci/TransformDescriptorHelper", "<init>","()V", false);
        visitor.visitVarInsn(ASTORE, tdHelper);

        visitor.visitVarInsn(ALOAD, tdHelper);
        visitor.visitLdcInsn(_classname);
        visitor.visitLdcInsn(getMethodName());
        visitor.visitLdcInsn(getDescriptor());
        visitor.visitLdcInsn(_type);
        visitor.visitLdcInsn(td);
        visitor.visitVarInsn(ALOAD, 0); // Instance of this
        createParameterArray(); // this and args
        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"com/ibm/wsspi/request/probe/bci/TransformDescriptorHelper","entryHelper",
        		"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", false );
        

    }
    @Override
    public void onMethodExit(int opcode) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "onMethodExit", "[methodName=" + getMethodName() + ", descriptor=" +  getDescriptor() + ", opcode="+opcode+"]");
        //Need to handle the case when an exception is thrown
        //if (opcode == ATHROW) {
        //return;
        //}
        visitor.visitVarInsn(ALOAD, tdHelper);
        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"com/ibm/wsspi/request/probe/bci/TransformDescriptorHelper","exitHelper","()V" , false );
    }
}
