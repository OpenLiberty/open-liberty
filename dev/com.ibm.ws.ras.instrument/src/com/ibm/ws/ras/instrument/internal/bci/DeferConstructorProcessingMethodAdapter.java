/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ras.instrument.internal.bci;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * This whole pile of code is needed to keep track of the state of the stack
 * when running a constructor so we can detect when the super class has been
 * initialized.
 * <p>
 * Why isn't it enough to simply watch for method instruction of the form <br>
 * INVOKESPECIAL superclassInternalName <init> ???
 * <p>
 * Well, it turns out that the constructor can actually instantiate an instance of the super class before calling the superclass's constructor:
 * 
 * public class Base {
 * public Base(Base base) {
 * }
 * }
 * 
 * public class Derived extends Base {
 * public Derived() {
 * super(new Base(null));
 * }
 * }
 * 
 * That little sequence involves two calls to the super class's initialization
 * method - but the first one is targeted to a different instance.
 */
public class DeferConstructorProcessingMethodAdapter extends MethodVisitor {

    /**
     * Value that indicates a stack reference to "this".
     */
    private final static Object THIS = new Object() {
        @Override
        public String toString() {
            return "this";
        }
    };

    /**
     * Value that indicates a stack reference to something other than "this".
     */
    private final static Object OTHER = new Object() {
        @Override
        public String toString() {
            return "other";
        }
    };

    /**
     * Indicate we're in the deferred processing area.
     */
    private boolean waitingForSuper = false;

    /**
     * List to keep track of the current stack.
     */
    private List<Object> currentStack = new ArrayList<Object>();

    /**
     * Map of branch targets to the stack that should be current when
     * executing code at that label.
     */
    private final Map<Label, List<Object>> branchTargets = new HashMap<Label, List<Object>>();

    /**
     * Create an instance of a {@code ProbeMethodAdapter}.
     */
    DeferConstructorProcessingMethodAdapter(MethodVisitor visitor) {
        super(Opcodes.ASM7, visitor);
    }

    private void push(Object stackElement, int count) {
        for (int i = 0; i < count; i++) {
            currentStack.add(stackElement);
        }
    }

    private Object pop(int count) {
        Object popped = null;
        for (int i = count; i > 0; i--) {
            popped = currentStack.remove(currentStack.size() - 1);
        }
        return popped;
    }

    private void addBranchTarget(Label target) {
        if (!branchTargets.containsKey(target)) {
            branchTargets.put(target, new ArrayList<Object>(currentStack));
        }
    }

    @Override
    public void visitCode() {
        super.visitCode();
        waitingForSuper = true;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
        if (!waitingForSuper)
            return;

        Type type = Type.getType(desc);
        switch (opcode) {
            case GETSTATIC:
                push(OTHER, type.getSize());
                break;
            case GETFIELD:
                push(OTHER, type.getSize() - 1);
                break;
            case PUTSTATIC:
                pop(type.getSize());
                break;
            case PUTFIELD:
                pop(type.getSize() + 1);
                break;
        }
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        if (!waitingForSuper)
            return;

        // List of opcodes taken from visitInsn documentation
        switch (opcode) {
            case NOP:
                break;

            // One slot constants
            case ACONST_NULL:
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                push(OTHER, 1);
                break;
            // Two slot constants
            case DCONST_0:
            case DCONST_1:
            case LCONST_0:
            case LCONST_1:
                push(OTHER, 2);
                break;

            // One slot array load
            case IALOAD:
            case FALOAD:
            case AALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
                pop(1); // pop(2); push(OTHER, 1);
                break;
            // Two slot array load
            case DALOAD:
            case LALOAD:
                // pop(2); push(OTHER, 2);
                break;

            // One slot array store
            case IASTORE:
            case FASTORE:
            case AASTORE:
            case BASTORE:
            case CASTORE:
            case SASTORE:
                pop(3);
                break;
            // Two slot array store
            case DASTORE:
            case LASTORE:
                pop(4);
                break;

            // Basic pop instructions
            case POP:
                pop(1);
                break;
            case POP2:
                pop(2);
                break;

            // Stack manipulation instructions
            case DUP: {
                Object top = currentStack.get(currentStack.size() - 1);
                push(top, 1);
                break;
            }
            case DUP_X1: {
                Object top = currentStack.get(currentStack.size() - 1);
                int addPosition = currentStack.size() - 2;
                currentStack.add(addPosition, top);
                break;
            }
            case DUP_X2: {
                Object top = currentStack.get(currentStack.size() - 1);
                int addPosition = currentStack.size() - 3;
                currentStack.add(addPosition, top);
                break;
            }
            case DUP2: {
                int position = currentStack.size() - 2;
                currentStack.add(currentStack.get(position));
                currentStack.add(currentStack.get(position + 1));
                break;
            }
            case DUP2_X1: {
                int addPosition = currentStack.size() - 3;
                currentStack.add(addPosition, currentStack.get(currentStack.size() - 1));
                currentStack.add(addPosition, currentStack.get(currentStack.size() - 2));
                break;
            }
            case DUP2_X2: {
                int addPosition = currentStack.size() - 4;
                currentStack.add(addPosition, currentStack.get(currentStack.size() - 1));
                currentStack.add(addPosition, currentStack.get(currentStack.size() - 2));
                break;
            }
            case SWAP: {
                Object o = currentStack.remove(currentStack.size() - 2);
                currentStack.add(o);
                break;
            }

                // One slot arithmetic and logical instructions
            case IADD:
            case FADD:
            case ISUB:
            case FSUB:
            case IMUL:
            case FMUL:
            case IDIV:
            case FDIV:
            case IREM:
            case FREM:
            case INEG:
            case FNEG:
            case ISHL:
            case ISHR:
            case IUSHR:
            case IAND:
            case IOR:
            case IXOR:
                pop(1);
                break;
            // Two slot arithmetic and logical instructions
            case DADD:
            case LADD:
            case DSUB:
            case LSUB:
            case DMUL:
            case LMUL:
            case DDIV:
            case LDIV:
            case DREM:
            case LREM:
            case DNEG:
            case LNEG:
            case LSHL:
            case LSHR:
            case LUSHR:
            case LAND:
            case LOR:
            case LXOR:
                pop(2);
                break;

            // One slot to two slot conversions
            case I2D:
            case I2L:
            case F2D:
            case F2L:
                push(OTHER, 1);
                break;
            // One slot to one slot conversions
            case F2I:
            case I2B:
            case I2C:
            case I2F:
            case I2S:
                break;
            // Two slot to one slot conversions
            case D2F:
            case D2I:
            case L2I:
            case L2F:
                pop(1);
                break;
            // Two slot to two slot conversions
            case L2D:
            case D2L:
                break;

            // Two slot compare
            case DCMPL:
            case DCMPG:
            case LCMP:
                pop(3);
                break;
            // One slot compare
            case FCMPL:
            case FCMPG:
                pop(1);
                break;

            // One slot return
            case IRETURN:
            case FRETURN:
            case ARETURN:
                pop(1);
                break;
            // Two slot return
            case DRETURN:
            case LRETURN:
                pop(2);
                break;
            // Zero slot return
            case RETURN:
                break;

            // Get array length
            case ARRAYLENGTH:
                break;

            // Throw an exception
            case ATHROW:
                pop(1);
                break;

            // Enter or leave a monitor
            case MONITORENTER:
            case MONITOREXIT:
                pop(1);
                break;
            default:
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        if (!waitingForSuper)
            return;

        switch (opcode) {
            case BIPUSH:
            case SIPUSH:
                push(OTHER, 1);
            case NEWARRAY:
                break;
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        if (!waitingForSuper)
            return;

        switch (opcode) {
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
                pop(1);
                break;
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
                pop(2);
                break;
            case GOTO:
                break;
            case JSR:
                push(OTHER, 1);
                break;
            case IFNULL:
            case IFNONNULL:
                pop(1);
                break;
        }

        addBranchTarget(label);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        if (!waitingForSuper)
            return;

        // Get what should be the shape of the stack in the handler.
        // Not all labels are branch targets so only deal with targets
        if (branchTargets.containsKey(label)) {
            currentStack = branchTargets.get(label);
        }
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
        if (!waitingForSuper)
            return;

        if (cst instanceof Long || cst instanceof Double) {
            push(OTHER, 2);
        } else {
            push(OTHER, 1);
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        if (!waitingForSuper)
            return;

        pop(1);

        // Setup for each branch target
        addBranchTarget(dflt);
        for (Label label : labels) {
            addBranchTarget(label);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        if (!waitingForSuper)
            return;

        // Process the args
        for (Type arg : Type.getArgumentTypes(desc)) {
            pop(arg.getSize());
        }

        switch (opcode) {
            case INVOKESTATIC:
                break;
            case INVOKEINTERFACE:
            case INVOKEVIRTUAL:
                pop(1);
                break;
            case INVOKESPECIAL:
                Object top = pop(1);
                if (top == THIS) {
                    // Amen - We've called the super class's initializer
                    waitingForSuper = false;
                }
                break;
        }

        Type returnType = Type.getReturnType(desc);
        if (returnType != Type.VOID_TYPE) {
            push(OTHER, returnType.getSize());
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
        if (!waitingForSuper)
            return;

        // What an odd instruction
        pop(dims);
        push(OTHER, 1);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        if (!waitingForSuper)
            return;

        pop(1);

        // Setup for each branch target
        addBranchTarget(dflt);
        for (Label label : labels) {
            addBranchTarget(label);
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        if (!waitingForSuper)
            return;

        switch (opcode) {
            case NEW:
                push(OTHER, 1);
                break;
            case ANEWARRAY:
            case CHECKCAST:
            case INSTANCEOF:
                break;
        }
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        if (!waitingForSuper)
            return;

        switch (opcode) {
            case ALOAD:
                push(var == 0 ? THIS : OTHER, 1);
                break;
            case ILOAD:
            case FLOAD:
                push(OTHER, 1);
                break;
            case DLOAD:
            case LLOAD:
                push(OTHER, 2);
                break;
            case ASTORE:
            case ISTORE:
            case FSTORE:
                pop(1);
                break;
            case DSTORE:
            case LSTORE:
                pop(2);
                break;
            case RET:
                break;
        }
    }
}
