/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.bci;

import static com.ibm.ws.ras.instrument.internal.bci.AbstractRasClassAdapter.SENSITIVE_TYPE;
import static com.ibm.ws.ras.instrument.internal.bci.AbstractRasClassAdapter.TRIVIAL_TYPE;
import static com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation.INJECTED_TRACE_TYPE;
import static com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation.MANUAL_TRACE_TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.ws.ras.instrument.internal.introspect.InjectedTraceAnnotationVisitor;
import com.ibm.ws.ras.instrument.internal.model.ClassInfo;
import com.ibm.ws.ras.instrument.internal.model.MethodInfo;

/**
 * Method adapter that contains most of the infrastructure required to
 * add &quot;standard&quot; tracing logic to methods declared on a class.
 * <p>
 * &quot;Standard&quot; tracing is considered:
 * <ul>
 * <li>On method entry, the method name and all parameters are traced.
 * <li>On method exit, the method name and the return value are traced.
 * <li>On exception throw, the method name and the exception are traced.
 * <li>On exception caught, the method name and the exception are traced.
 * </ul>
 * </p>
 * In addition to utility functions, this class will keep track of the
 * operand stack in object constructors so we can detect when the super
 * class has been properly initialized.
 * <p>
 * Unfortunately it isn't enough to simply watch for method instruction of the form
 *
 * <pre>
 * INVOKESPECIAL superclassInternalName &lt;init&gt;
 * </pre>
 * <p>
 * because the constructor can actually instantiate an instance of the super class before calling the superclass's constructor:
 *
 * <pre>
 * public class Base
 * {
 * public Base(Base base)
 * {}
 * }
 *
 * public class Derived extends Base
 * {
 * public Derived()
 * {
 * super(new Base(null));
 * }
 * }
 * </pre>
 *
 * That little sequence involves two calls to the super class's initialization method - but the first one is targeted to a different instance.
 */
public abstract class AbstractRasMethodAdapter<C extends AbstractRasClassAdapter> extends RasMethodAdapter implements Opcodes {

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
     * The <code>AbstractRasClassAdapter</code> that created this method adapter.
     */
    private final C classAdapter;

    /**
     * The trace configuration and introspection information at the method level.
     */
    private final MethodInfo methodInfo;

    /**
     * The name of the method that's being processed.
     */
    private final String methodName;

    /**
     * Flag that indicates this method is a static method.
     */
    private boolean isStatic = false;

    /**
     * The types of all of the arguments to this method.
     */
    private Type[] argTypes = null;

    /**
     * The return type from this method. This will be <code>Type.VOID</code> for constructors, static initializers, and methods that return void.
     */
    private Type returnType = null;

    /**
     * The types of annotations that were observed on this class.
     */
    private final Set<Type> observedAnnotations = new HashSet<Type>();

    /**
     * The types of annotations observed on the method parameters.
     */
    private final Map<Integer, Set<Type>> observedParameterAnnotations = new HashMap<Integer, Set<Type>>();

    /**
     * Visitor of the annotation that lists which {@code RasMethodAdapter}s
     * that have visited this method.
     */
    private InjectedTraceAnnotationVisitor injectedTraceAnnotationVisitor;

    /**
     * The set of exception handler <code>Label</code>s associated with
     * this method.
     */
    private final Map<Label, String> handlers = new HashMap<Label, String>();

    /**
     * The last line number that was seen while processing this method.
     */
    private int lineNumber;

    /**
     * The <code>Label</code> associated with the start of a method.
     */
    private Label methodEntryLabel;

    /**
     * Flag indicating whether methodEntryLabel has been given a line number.
     */
    private boolean methodEntryHasLineNumber;

    /**
     * Flag indicating that we've performed method entry processing.
     */
    private boolean processedMethodEntry;

    /**
     * The first local variable slot that is not occupied by a method parameter.
     */
    private int firstNonParameterSlot;

    /**
     * Indicate we're in the deferred processing area.
     */
    private boolean waitingForSuper = false;

    /**
     * List to keep track of the current stack.
     */
    private List<Object> currentStack = null;

    /**
     * Map of branch targets to the stack that should be current when
     * executing code at that label.
     */
    private Map<Label, List<Object>> branchTargets = null;

    /**
     * The type name for an exception handler that is active, but the first
     * instruction has not yet been visited.
     */
    private String pendingExceptionHandlerTypeName;

    /**
     * Indication that subclasses are requesting this class to visit the JDK6
     * stack map frames after each of the onXxx callbacks. This will only
     * work if the following conditions are met:
     * <ul>
     * <li>{@link #onMethodEntry} and {@link #onMethodExit} must return after visiting the trace guard label.
     * </ul>
     */
    private final boolean visitFramesAfterCallbacks;

    /**
     * Constructor that must be called by subclasses.
     *
     * @param classAdapter the <code>RasClassAdapter</code> that created this
     *            <code>RasMethodAdapter</code>
     * @param visitor the chained <code>MethodVisitor</code> that calls must be
     *            forwarded to
     * @param access the method access flags
     * @param methodName the name of the method we're processing
     * @param descriptor the method descriptor containing the parameter and return types
     * @param signature the method's signature (may be null if generic types are not used)
     * @param exceptions the internal names of the exception types declared to be thrown
     */
    public AbstractRasMethodAdapter(C classAdapter,
                                    boolean visitFrames,
                                    MethodVisitor visitor,
                                    int access,
                                    String methodName,
                                    String descriptor,
                                    String signature,
                                    String[] exceptions) {
        super(visitor);

        this.classAdapter = classAdapter;
        this.visitFramesAfterCallbacks = visitFrames;
        this.methodName = methodName;
        this.isStatic = (access & ACC_STATIC) != 0;
        this.argTypes = Type.getArgumentTypes(descriptor);
        this.returnType = Type.getReturnType(descriptor);
        this.firstNonParameterSlot = isStatic ? 0 : 1;
        for (int i = 0; i < this.argTypes.length; i++) {
            this.firstNonParameterSlot += this.argTypes[i].getSize();
        }

        // Create structures needed for stack simulation
        if (isConstructor()) {
            this.currentStack = new ArrayList<Object>();
            this.branchTargets = new HashMap<Label, List<Object>>();
            this.waitingForSuper = true;
        }

        // Class trace configuration information
        ClassInfo classInfo = classAdapter.getClassInfo();
        if (classInfo != null) {
            MethodInfo methodInfo = classInfo.getDeclaredMethod(methodName, descriptor);
            if (methodInfo == null) {
                methodInfo = new MethodInfo(methodName, descriptor);
                methodInfo.updateDefaultValuesFromClassInfo(classInfo);
            }
            this.methodInfo = methodInfo;
        } else {
            methodInfo = null;
        }
    }

    /**
     * Push an operand onto the virtual stack. This is used to help detect
     * when the correct {@code INVOKESPECIAL} operation is called on the super
     * class.
     *
     * @param stackElement a representation of the type of stack item
     *
     * @param count the number of slots the type occupies
     */
    private void push(Object stackElement, int count) {
        for (int i = 0; i < count; i++) {
            currentStack.add(stackElement);
        }
    }

    /**
     * Pop operands from the virtual stack.
     *
     * @param count
     *            the number of slots to pop
     *
     * @return the last reference popped
     */
    private Object pop(int count) {
        Object popped = null;
        for (int i = count; i > 0; i--) {
            popped = currentStack.remove(currentStack.size() - 1);
        }
        return popped;
    }

    /**
     * Keep track of the flow target of a branch so we can restore the view of
     * the operand stack once the branch target is hit.
     *
     * @param target
     *            the {@code Label} representing the target location
     */
    private void addBranchTarget(Label target) {
        if (!branchTargets.containsKey(target)) {
            branchTargets.put(target, new ArrayList<Object>(currentStack));
        }
    }

    /**
     * Process an exception handler entry point if necessary. If <code>astoreVar</code>
     * is -1, then the exception is on the top of the stack. Otherwise, the
     * exception is stored in that local variable.
     */
    private void processPendingExceptionHandlerEntry(int astoreVar) {
        if (pendingExceptionHandlerTypeName != null) {
            Type exceptionType = Type.getObjectType(pendingExceptionHandlerTypeName);
            pendingExceptionHandlerTypeName = null;
            if (onExceptionHandlerEntry(exceptionType, astoreVar)) {
                visitFrameAfterOnExceptionHandlerEntry();
            }
        }
    }

    /**
     * Visit the method annotations looking at the supported RAS annotations.
     * The visitors are only used when a {@code MethodInfo} model object was
     * not provided during construction.
     *
     * @param desc
     *            the annotation descriptor
     * @param visible
     *            true if the annotation is a runtime visible annotation
     */
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        observedAnnotations.add(Type.getType(desc));
        if (desc.equals(INJECTED_TRACE_TYPE.getDescriptor())) {
            injectedTraceAnnotationVisitor = new InjectedTraceAnnotationVisitor(av, getClass());
            av = injectedTraceAnnotationVisitor;
        }
        return av;
    }

    /**
     * Visit the method parameter annotations looking for the supported RAS
     * annotations. The visitors are only used when a {@code MethodInfo} model
     * object was not provided during construction.
     *
     * @param desc
     *            the annotation descriptor
     * @param visible
     *            true if the annotation is a runtime visible annotation
     */
    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
        Set<Type> parameterAnnotations = observedParameterAnnotations.get(parameter);
        if (parameterAnnotations == null) {
            parameterAnnotations = new HashSet<Type>();
            observedParameterAnnotations.put(parameter, parameterAnnotations);
        }
        parameterAnnotations.add(Type.getType(desc));
        return av;
    }

    /**
     * Begin processing a method. This is called when the instruction stream
     * for a method is first encountered. Since this is the first callback, we
     * use this to add the byte codes required for entry tracing.
     */
    @Override
    public void visitCode() {
        if (injectedTraceAnnotationVisitor == null && classAdapter.isInjectedTraceAnnotationRequired()) {
            AnnotationVisitor av = visitAnnotation(INJECTED_TRACE_TYPE.getDescriptor(), true);
            av.visitEnd();
        }

        super.visitCode();

        // Label the entry to the method so we can update the local var
        // debug data and indicate that we're referencing them during
        // method entry
        methodEntryLabel = new Label();
        visitLabel(methodEntryLabel);

        if (!waitingForSuper) {
            // This must be done before all instruction callbacks, but it must
            // also be done before the visitLabel() callback, which might have
            // an associated UNINITIALIZED entry in the stack map frame that is
            // supposed to be pointing at a NEW opcode.
            processMethodEntry();
        }
    }

    /**
     * Inject a simple stack map frame and a PUSH NULL, POP. This method
     * assumes that the onMethodEntry modified code and returned immediately
     * after visiting the target label of a trace guard.
     */
    private void visitFrameAfterOnMethodEntry() {
        if (!visitFramesAfterCallbacks)
            return;

        // The frame that is required after the trace guard must be
        // fully specified as 'this' is no longer an 'uninitialized this'
        if (isConstructor()) {
            List<Object> stackLocals = new ArrayList<Object>(argTypes.length + 1);
            stackLocals.add(classAdapter.getClassType().getInternalName());
            for (Type type : argTypes) {
                switch (type.getSort()) {
                    case Type.ARRAY:
                    case Type.OBJECT:
                        stackLocals.add(type.getInternalName());
                        break;
                    case Type.BOOLEAN:
                    case Type.CHAR:
                    case Type.BYTE:
                    case Type.SHORT:
                    case Type.INT:
                        stackLocals.add(INTEGER);
                        break;
                    case Type.LONG:
                        stackLocals.add(LONG);
                        break;
                    case Type.FLOAT:
                        stackLocals.add(FLOAT);
                        break;
                    case Type.DOUBLE:
                        stackLocals.add(DOUBLE);
                        break;
                }
            }
            visitFrame(F_FULL, stackLocals.size(), stackLocals.toArray(), 0, new Object[] {});
            visitInsn(NOP);
        } else {
            visitFrame(F_SAME, 0, null, 0, null);
            visitInsn(NOP);
        }
    }

    /**
     * Make appropriate callbacks to generate code at method entry.
     */
    private void processMethodEntry() {
        if (processedMethodEntry)
            return;

        processedMethodEntry = true;
        if (isStaticInitializer()) {
            initializeTraceObjectField();
        }

        if (onMethodEntry()) {
            visitFrameAfterOnMethodEntry();
        }
    }

    /**
     * Process instructions without operands that are part of the method.
     * We'll use this callback to handle the return and throw instructions.
     *
     * @param inst
     *            the instruction to be processed
     */
    @Override
    public void visitInsn(int inst) {
        processPendingExceptionHandlerEntry(-1);

        // List of opcodes taken from visitInsn documentation
        if (waitingForSuper) {
            // Visit the instruction
            super.visitInsn(inst);

            switch (inst) {
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
        } else {
            // Handle return and throw instructions
            switch (inst) {
                case RETURN:
                case ARETURN:
                case DRETURN:
                case FRETURN:
                case IRETURN:
                case LRETURN:
                    if (onMethodReturn()) {
                        visitFrameAfterMethodReturnCallback();
                    }
                    break;
                case ATHROW:
                    if (onThrowInstruction()) {
                        visitFrameAfterOnThrowCallback();
                    }
                    break;
            }
            super.visitInsn(inst);
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        processPendingExceptionHandlerEntry(-1);
        super.visitFieldInsn(opcode, owner, name, desc);

        // Update stack in the constructor
        if (waitingForSuper) {
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
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        processPendingExceptionHandlerEntry(-1);
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        processPendingExceptionHandlerEntry(-1);
        super.visitIntInsn(opcode, operand);

        // Constructor processing
        if (waitingForSuper) {
            switch (opcode) {
                case BIPUSH:
                case SIPUSH:
                    push(OTHER, 1);
                case NEWARRAY:
                    break;
            }
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        processPendingExceptionHandlerEntry(-1);
        super.visitJumpInsn(opcode, label);

        // Constructor processing
        if (waitingForSuper) {
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
    }

    @Override
    public void visitLdcInsn(Object constant) {
        processPendingExceptionHandlerEntry(-1);
        super.visitLdcInsn(constant);

        if (waitingForSuper) {
            if (constant instanceof Long || constant instanceof Double) {
                push(OTHER, 2);
            } else {
                push(OTHER, 1);
            }
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label defaultTarget, int[] keys, Label[] labels) {
        processPendingExceptionHandlerEntry(-1);
        super.visitLookupSwitchInsn(defaultTarget, keys, labels);

        if (waitingForSuper) {
            pop(1);

            // Setup for each branch target
            addBranchTarget(defaultTarget);
            for (Label label : labels) {
                addBranchTarget(label);
            }
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        processPendingExceptionHandlerEntry(-1);
        super.visitMethodInsn(opcode, owner, name, desc, itf);

        if (waitingForSuper) {
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
                        processMethodEntry();
                    }
                    break;
            }

            Type returnType = Type.getReturnType(desc);
            if (returnType != Type.VOID_TYPE) {
                push(OTHER, returnType.getSize());
            }
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        processPendingExceptionHandlerEntry(-1);
        super.visitMultiANewArrayInsn(desc, dims);

        // Constructor processing
        if (waitingForSuper) {
            // What an odd instruction
            pop(dims);
            push(OTHER, 1);
        }
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label defaultLabel, Label... labels) {
        processPendingExceptionHandlerEntry(-1);
        super.visitTableSwitchInsn(min, max, defaultLabel, labels);

        if (waitingForSuper) {
            pop(1);

            // Setup for each branch target
            addBranchTarget(defaultLabel);
            for (Label label : labels) {
                addBranchTarget(label);
            }
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        processPendingExceptionHandlerEntry(-1);
        super.visitTypeInsn(opcode, type);

        // Constructor processing
        if (waitingForSuper) {
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
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if (opcode == ASTORE) {
            super.visitVarInsn(opcode, var);
            processPendingExceptionHandlerEntry(var);
        } else {
            processPendingExceptionHandlerEntry(-1);
            super.visitVarInsn(opcode, var);
        }

        // Constructor processing
        if (waitingForSuper) {
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

    /**
     * Generate the stack frame that's needed after visiting the
     * method exit trace guard target injected by tracing adapters.
     */
    private void visitFrameAfterMethodReturnCallback() {
        if (!visitFramesAfterCallbacks)
            return;

        Type returnType = getReturnTypeForTrace();
        if (!Type.VOID_TYPE.equals(getReturnTypeForTrace()) && !isConstructor()) {
            Object typeDescriptor = null;
            switch (returnType.getSort()) {
                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.INT:
                case Type.SHORT:
                    typeDescriptor = INTEGER;
                    break;
                case Type.DOUBLE:
                    typeDescriptor = DOUBLE;
                    break;
                case Type.FLOAT:
                    typeDescriptor = FLOAT;
                    break;
                case Type.LONG:
                    typeDescriptor = LONG;
                    break;
                default:
                    typeDescriptor = returnType.getInternalName();
                    break;
            }
            visitFrame(F_SAME1, 0, null, 1, new Object[] { typeDescriptor });
        } else {
            visitFrame(F_SAME, 0, null, 0, null);
        }
    }

    /**
     * Generate the stack frame that's needed after visiting the
     * exception throw trace guard target injected by tracing adapters.
     */
    private void visitFrameAfterOnThrowCallback() {
        if (!visitFramesAfterCallbacks)
            return;

        visitFrame(F_SAME, 0, null, 0, null);
        visitInsn(NOP);
    }

    /**
     * Visit a try catch block. We will use this to determine the exception
     * handler labels for the try block.
     *
     * @param start
     *            the beginning of the try block
     * @param end
     *            the end of the try block
     * @param handler
     *            the exception handler
     * @param type
     *            the internal name of the throwable being handled
     */
    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        if (type != null) {
            handlers.put(handler, type);
        }
        super.visitTryCatchBlock(start, end, handler, type);
    }

    /**
     * Visit a label. We will use this to determine when we've reached an
     * exception handler block.
     *
     * @param label
     *            the label to visit
     */
    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);

        if (waitingForSuper) {
            // Get what should be the shape of the stack in the handler.
            // Not all labels are branch targets so only deal with targets
            if (branchTargets.containsKey(label)) {
                currentStack = branchTargets.get(label);
            }
        } else {
            pendingExceptionHandlerTypeName = handlers.get(label);
        }
    }

    /**
     * Visit a stack map frame.
     */
    @Override
    public void visitFrame(int type, int numLocals, Object[] locals, int stackSize, Object[] stack) {
        if (!isVisitFrameRequired())
            return;

        super.visitFrame(type, numLocals, locals, stackSize, stack);
    }

    /**
     * Generate the stack frame that's needed after visiting the
     * exception handler entry trace guard target injected by tracing adapters.
     */
    private void visitFrameAfterOnExceptionHandlerEntry() {
        if (!visitFramesAfterCallbacks)
            return;

        visitFrame(F_SAME, 0, null, 0, null);
    }

    /**
     * Visit a local variable. We will use this to change the start label
     * for method parameters references.
     */
    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        if (index < firstNonParameterSlot) {
            start = methodEntryLabel;
        }
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    /**
     * Vist a line number block and its associated label. We will use this
     * to keep track of (roughly) what line we're on.
     */
    @Override
    public void visitLineNumber(int line, Label start) {
        // If we haven't given a line number to the method entry, do so now.
        if (!methodEntryHasLineNumber) {
            methodEntryHasLineNumber = true;
            start = methodEntryLabel;
        }

        lineNumber = line;
        super.visitLineNumber(line, start);
    }

    /**
     * Create an object array that contains references to the method parameters.
     * Primitives will be boxed and parameters that have been annotated as
     * sensitive will only have their class type traced. On exit, the Object
     * array will be on the top of the stack.
     */
    protected void createTraceArrayForParameters() {
        // Static methods don't have an implicit "this" argment so start
        // working with local var 0 instead of 1 for the parm list.
        int localVarOffset = isStatic ? 0 : 1;
        int syntheticArgs = 0;

        // Use an heuristic to guess when we're in a nested class constructor.
        // Nested classes that are not static get a synthetic reference to their
        // parent as the first argument.
        if (isConstructor() && getClassAdapter().isInnerClass() && argTypes.length > 1) {
            String className = getClassAdapter().getClassInternalName();
            String ownerName = className.substring(0, className.lastIndexOf("$"));
            if (Type.getObjectType(ownerName).equals(argTypes[0])) {
                syntheticArgs = 1;
            }
        }

        // Build the object array that will hold the input args to the method.
        visitLdcInsn(new Integer(argTypes.length - syntheticArgs));
        visitTypeInsn(ANEWARRAY, "java/lang/Object");

        for (int i = syntheticArgs; i < argTypes.length; i++) {
            int j = i + localVarOffset;

            visitInsn(DUP);
            visitLdcInsn(new Integer(i - syntheticArgs));
            boxLocalVar(argTypes[i], j, isArgumentSensitive(i));
            visitInsn(AASTORE);

            // Local variables can use more than one slot. (DJ)
            // Account for those here by adding them to the local
            // var offset.
            localVarOffset += argTypes[i].getSize() - 1;
        }
    }

    /**
     * Generate the instruction sequence needed to "box" the local variable
     * if required.
     *
     * @param type
     *            the <code>Type</code> of the object in the specified local
     *            variable slot on the stack.
     * @param slot
     *            the local variable slot to box.
     * @param isSensitive
     *            indication that the variable is sensitive and should
     *            not be traced as is.
     */
    protected void boxLocalVar(final Type type, final int slot, final boolean isSensitive) {
        visitVarInsn(type.getOpcode(ILOAD), slot);
        box(type, isSensitive);
    }

    /**
     * Generate the instruction sequence needed to "box" the data on the top
     * of stack (if boxing is required).
     *
     * @param type
     *            the <code>Type</code> of the object in the specified local
     *            variable slot on the stack.
     * @param isSensitive
     *            indication that the variable is sensitive and should
     *            not be traced as is.
     */
    protected void box(final Type type, final boolean isSensitive) {
        if (isSensitive) {
            boxSensitive(type);
            return;
        }
        switch (type.getSort()) {
            case Type.BOOLEAN:
                visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case Type.BYTE:
                visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case Type.CHAR:
                visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case Type.DOUBLE:
                visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case Type.FLOAT:
                visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case Type.INT:
                visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case Type.LONG:
                visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case Type.SHORT:
                visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case Type.ARRAY:
            case Type.OBJECT:
            default:
        }
    }

    /**
     * Generate the instruction sequence needed to "box" a sensitive local variable
     * as a <code>null</code> reference or the string
     * &quot;&lt;sensitive <i>type</i>&gt;&quot;
     *
     * @param type
     *            the <code>Type</code> of the object in the specified local
     *            variable slot on the stack.
     */
    protected void boxSensitive(final Type type) {
        if (type.getSize() == 2) {
            visitInsn(POP2);
        } else {
            visitInsn(POP);
        }
        visitLdcInsn("<sensitive " + type.getClassName() + ">");
    }

    /**
     * Load the trace object field onto the top of the stack.
     */
    protected void visitGetTraceObjectField() {
        visitFieldInsn(
                       GETSTATIC,
                       classAdapter.getClassInternalName(),
                       classAdapter.getTraceObjectFieldName(),
                       classAdapter.getTraceObjectFieldType().getDescriptor());
    }

    /**
     * Set the trace object field from the reference on the top of the stack.
     */
    protected void visitSetTraceObjectField() {
        visitFieldInsn(
                       PUTSTATIC,
                       classAdapter.getClassInternalName(),
                       classAdapter.getTraceObjectFieldName(),
                       classAdapter.getTraceObjectFieldType().getDescriptor());
    }

    /**
     * Put a string containing the class name on the top of the stack.
     */
    protected void visitLoadClassName() {
        visitLdcInsn(classAdapter.getClassName());
    }

    /**
     * Put a string containing the method name on the type of the stack.
     */
    protected void visitLoadMethodName() {
        visitLdcInsn(methodName);
    }

    /**
     * Put a reference to the <code>java.lang.Class</code> corresponding
     * to the specified <code>Type</code> on the top of the stack.
     *
     * @param classType
     *            the type describing the class to be loaded
     */
    protected void visitGetClassForType(Type classType) {
        if (isLdcFromClassTypeAllowed()) {
            visitLdcInsn(classType);
        } else {
            mv.visitLdcInsn(classType.getClassName());
            mv.visitMethodInsn(
                               INVOKESTATIC,
                               Type.getInternalName(Class.class),
                               "forName",
                               Type.getMethodDescriptor(Type.getType(Class.class), new Type[] { Type.getType(String.class) }), false);
        }
    }

    /**
     * Get the <code>Type</code> to use when tracing the value returned from a
     * method. The type will not be the type declared in the method descriptor
     * for contstructors.
     *
     * @return the declared return type of the method being process or the
     *         type of the corresponding class if the method is a constructor.
     */
    protected Type getReturnTypeForTrace() {
        return isConstructor() ? Type.getObjectType(classAdapter.getClassInternalName()) : returnType;
    }

    /**
     * If the return type of the method being processed is something other than <code>void</code>, duplicate or box the value to put a copy of the return
     * value on the top of the stack that can be consumed by an exit trace. If
     * the return type of the method is void, the stack is left unhcanged as there
     * is nothing to copy or trace.
     *
     * @return true if an object was placed on the stack for the exit trace
     */
    protected boolean setupReturnObjectValueForExitTrace() {
        Type type = getReturnTypeForTrace();
        if (type.equals(Type.VOID_TYPE)) {
            return false;
        }
        if (isConstructor()) {
            visitVarInsn(ALOAD, 0);
        } else if (type != Type.VOID_TYPE) {
            if (type.getSize() == 2) {
                visitInsn(DUP2);
            } else {
                visitInsn(DUP);
            }
        }
        box(type, isResultSensitive());
        return true;
    }

    /**
     * Indicate whether or not the class version supports using the LDC
     * instruction with a class descriptor as an argument.
     */
    protected boolean isLdcFromClassTypeAllowed() {
        return (classAdapter.getClassVersion() & 0xFFFF) >= (V1_5 & 0xFFFF);
    }

    /**
     * Indicate whether or not the class version requires stack map frames.
     */
    protected boolean isVisitFrameRequired() {
        return (classAdapter.getClassVersion() & 0xFFFF) >= (V1_6 & 0xFFFF);
    }

    /**
     * Get the <code>RasClassAdapter</code> that created this method adapter.
     *
     * @return the <code>RasClassAdapter</code> that created this method adapter
     */
    protected C getClassAdapter() {
        return classAdapter;
    }

    /**
     * Get the pre-processed method information that may have been introspected
     * in a previous phase.
     *
     * @return the pre-processed {@code MethodInfo} or null if it doesn't exist
     */
    MethodInfo getMethodInfo() {
        return methodInfo;
    }

    /**
     * Get the name of the method being processed.
     *
     * @return the method name
     */
    protected String getMethodName() {
        return methodName;
    }

    /**
     * Get the line number corresponding to the method information being
     * processed. This line number is only available when the class was
     * compiled with line number attributes in the byte code.
     *
     * @return the last line number that was visited
     */
    protected int getLineNumber() {
        return lineNumber;
    }

    /**
     * Determine if the method being processed is the class static initializer <code>&lt;clinit&gt;</code>
     *
     * @return true if this method is the class static initializer
     */
    protected boolean isStaticInitializer() {
        return "<clinit>".equals(methodName);
    }

    /**
     * Determine if the method being processed is a constructor.
     *
     * @return true if this method is a constructor
     */
    protected boolean isConstructor() {
        return "<init>".equals(methodName);
    }

    /**
     * Determine if the introspection or trace configuration for this method
     * indicates that the method result is sensitive.
     *
     * @return true if the method config implies this class is sensitive
     */
    protected boolean isResultSensitive() {
        if (methodInfo != null) {
            return methodInfo.isResultSensitive();
        }
        return observedAnnotations.contains(SENSITIVE_TYPE);
    }

    /**
     * Determine if the introspection or trace configuration for this method
     * indicates that the specified method parameter is sensitive.
     *
     * @param index
     *            the 0 origin parameter index
     *
     * @return true if the method config implies this class is sensitive
     */
    protected boolean isArgumentSensitive(int index) {
        if (methodInfo != null) {
            return methodInfo.isArgSensitive(index);
        }
        Set<Type> parameterAnnotations = observedParameterAnnotations.get(index);
        if (parameterAnnotations != null) {
            return parameterAnnotations.contains(SENSITIVE_TYPE);
        }
        return false;
    }

    /**
     * Determine if the method has been declared trivial.
     *
     * @return true if the method is trivial
     */
    protected boolean isTrivial() {
        if (methodInfo != null) {
            return methodInfo.isTrivial();
        }
        return observedAnnotations.contains(TRIVIAL_TYPE);
    }

    /**
     * Determine if the method being processed was declared as a static method.
     *
     * @return true if this method is a static method
     */
    protected boolean isStatic() {
        return isStatic;
    }

    /**
     * Determine if the method being processed has had trace points manually
     * injected. In general, manually traced methods should not be injected
     * with additional trace.
     *
     * @return true if the method being processed has hard coded trace points
     */
    protected boolean isTracedManually() {
        return observedAnnotations.contains(MANUAL_TRACE_TYPE);
    }

    /**
     * Determine if the method being processed has already had trace points
     * automatically injected by this method adapter.
     *
     * @return true if the method being processed has already been instrumented
     *         with trace
     */
    protected boolean isMethodInstrumentedByThisAdapter() {
        if (injectedTraceAnnotationVisitor == null) {
            return false;
        }

        List<String> visitedMethodAdapters = injectedTraceAnnotationVisitor.getMethodAdapters();
        return visitedMethodAdapters.contains(getClass().getName());
    }

    /**
     * Determine if the method being processed already contains entry/exit
     * tracing. The tracing may have been injected by instrumentation or
     * manually coded.
     *
     * @return true if the method being process contains entry/exit trace
     */
    protected boolean isAlreadyTraced() {
        return isTracedManually() || isMethodInstrumentedByThisAdapter();
    }

    /**
     * Simple diagnostic aid for debug.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";methodInfo=").append(methodInfo);
        sb.append(",isStatic=").append(isStatic);
        sb.append(",lineNumber=").append(lineNumber);
        return sb.toString();
    }
}
