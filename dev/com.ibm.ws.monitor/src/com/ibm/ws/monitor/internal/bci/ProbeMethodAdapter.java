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

package com.ibm.ws.monitor.internal.bci;

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.ws.monitor.internal.MonitoringProxyActivator;
import com.ibm.ws.monitor.internal.ProbeImpl;
import com.ibm.ws.monitor.internal.ProbeListener;
import com.ibm.ws.monitor.internal.ProbeManagerImpl;

/**
 * Base class for method adapters that inject code to fire probes. The class
 * is expected to serve as the base clase for all probe injection method
 * adapters.
 * <p>
 * In addition to providing some basic utility functions, this class drives
 * method entry, method exit, and before first instruction callbacks. These
 * callbacks may be overridden by subclasses interested in injecting code at
 * those positions. The separation of &quot;first instruction&quot; and
 * &quot;method entry&quot; callbacks is due to the difference in behavior
 * between object constructors and methods.
 * <p>
 * In the case of non-constructor methods, the method entry and first
 * instruction callbacks are both issued when the first instruction of the
 * method is observed. In practical terms, there's no difference between the
 * two. These callbacks always happen <em>after</em> {@linkplain visitCode}.
 * By delaying the callback until the first instruction of the method, the
 * appropriate stack map frames and debug data should have already been
 * visited.
 * <p>
 * In the case of an object constructor, the first instruction(s)
 * in the method are generally used to initialize the super class. Per the
 * Java language specification, an object's super class instance must be
 * initialized prior to local initialization. In real terms that means that
 * the newly constructed object may not reference instance fields or methods
 * until the super class's constructor returns.
 * <p>
 * In the time between object allocation and super class initialization, the
 * object instance is represented by a special type in the stack map called
 * <em>uninitialized this</em>. This type can only be used to drive the
 * super class's constructor.
 * <p>
 * A good deal of the code in this class exists to keep track of the state
 * of the stack when running a constructor so we can detect when the super
 * class has been initialized.
 * <p>
 * Why isn't it enough to simply watch for method instruction of the form
 * <code>INVOKESPECIAL superclassInternalName &lt;init&gt;</code>?
 * <p>Well, it turns out that the constructor can actually instantiate an
 * instance of the super class before calling the superclass's constructor:
 * <pre>
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
 * </pre>
 * That little sequence involves two calls to the super class's initialization
 * method - but the first one is targeted to a different instance.
 * <p>
 * In addition to providing callbacks, this method is responsible for skipping
 * over probe injection adapters further in the chain when the current adapter
 * is injecting code in support of a probe.
 */
public class ProbeMethodAdapter extends MethodVisitor {

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
     * The {@code fireProbe} method name.
     */
    public final static String FIRE_PROBE_METHOD_NAME = "fireProbe";

    /**
     * The method descriptor for the {@code fireProbe} method on the proxy.
     */
    public final static String FIRE_PROBE_METHOD_DESC = Type.getMethodDescriptor(
                                                                                 Type.VOID_TYPE,
                                                                                 new Type[] { Type.LONG_TYPE, Type.getType(Object.class), Type.getType(Object.class),
                                                                                             Type.getType(Object.class) });

    /**
     * Indicate we're in the deferred processing area.
     */
    private boolean waitingForSuper = false;

    /**
     * Indicate that we've seen the first byte code instruction.
     */
    private boolean observedFirstInstruction = false;

    /**
     * List to keep track of the current stack.
     */
    private List<Object> currentStack;

    /**
     * Map of branch targets to the stack that should be current when
     * executing code at that label.
     */
    private Map<Label, List<Object>> branchTargets;

    /**
     * The next visitor in the chain that's not a probe method adapter.
     */
    private final MethodVisitor visitor;

    /**
     * The next {@link ProbeMethodAdapter} in the transformation chain or {@code null} if this is the last.
     */
    private ProbeMethodAdapter probeMethodAdapter;

    /**
     * Indication that a visitor in the chain is injecting a probe instruction
     * sequence.
     */
    private boolean probeInProgress = false;

    /**
     * Method information.
     */
    private final MethodInfo methodInfo;

    /**
     * Set of enabled probes that were processed by this adapter for the
     * specified listener.
     */
    private final Map<ProbeImpl, Set<ProbeListener>> enabledProbes = new HashMap<ProbeImpl, Set<ProbeListener>>();

    /**
     * Create an instance of a {@code ProbeMethodAdapter} that forwards
     * all calls to the specified visitor.
     * 
     * @param visitor
     * @param methodInfo
     */
    ProbeMethodAdapter(MethodVisitor visitor, MethodInfo methodInfo) {
        super(Opcodes.ASM8, visitor);
        this.visitor = visitor;
        this.methodInfo = methodInfo;
    }

    /**
     * Create an instance of a {@code ProbeMethodAdapter}.
     */
    protected ProbeMethodAdapter(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo) {
        super(Opcodes.ASM8, probeMethodAdapter);
        this.probeMethodAdapter = probeMethodAdapter;
        this.visitor = probeMethodAdapter.getVisitor();
        this.methodInfo = methodInfo;
        if (isConstructor()) {
            this.waitingForSuper = true;
            this.currentStack = new ArrayList<Object>();
            this.branchTargets = new HashMap<Label, List<Object>>();
        }
    }

    /**
     * Associate a collection of listeners with the specified probe.
     * 
     * @param probe the injected probe
     * @param listeners the listeners that will receive events from the
     *            injected probe
     */
    protected void setProbeListeners(ProbeImpl probe, Collection<ProbeListener> listeners) {
        Set<ProbeListener> enabled = enabledProbes.get(probe);
        if (enabled == null) {
            enabled = new HashSet<ProbeListener>();
            enabledProbes.put(probe, enabled);
        }
        enabled.addAll(listeners);
    }

    /**
     * Get the set of probes that were enabled by this adapter.
     * 
     * @return the probes activated by this adapter.
     */
    protected Set<ProbeImpl> getEnabledProbes() {
        return enabledProbes.keySet();
    }

    /**
     * Get the set of probe listeners that have been associated with the
     * specified probe by this adapter.
     * 
     * @param probe the associated probe
     * 
     * @return the set of listeners associated with {@code probe}
     */
    protected Set<ProbeListener> getProbeListeners(ProbeImpl probe) {
        Set<ProbeListener> listeners = enabledProbes.get(probe);
        if (listeners == null) {
            listeners = Collections.emptySet();
        }
        return listeners;
    }

    /**
     * Get a reference to the {@link ProbeManagerImpl} that is tracking the
     * probes and listeners.
     * 
     * @return the probe manager
     */
    protected ProbeManagerImpl getProbeManager() {
        return methodInfo.getClassAdapter().getProbeManager();
    }

    /**
     * Get a reference to the class that is being injected with probes.
     * 
     * @return that class that's currently being probed
     */
    protected Class<?> getProbedClass() {
        return methodInfo.getClassAdapter().getProbedClass();
    }

    protected ProbeImpl getProbe(String probeKey) {
        ProbeImpl probeImpl = getProbeManager().getProbe(getProbedClass(), probeKey);
        if (probeImpl == null) {
            Class<?> probedClass = getProbedClass();
            Constructor<?> ctor = methodInfo.getClassAdapter().getConstructor(getMethodName(), getDescriptor());
            Method method = methodInfo.getClassAdapter().getMethod(getMethodName(), getDescriptor());
            probeImpl = getProbeManager().createProbe(probedClass, probeKey, ctor, method);
        }
        return probeImpl;
    }

    /**
     * Get the name of the method being processed.
     * 
     * @return the name of the method being processed
     */
    protected String getMethodName() {
        return methodInfo.getMethodName();
    }

    /**
     * Get the descriptor of the method being processed.
     * 
     * @return the descriptor of the method being processed
     */
    protected String getDescriptor() {
        return methodInfo.getDescriptor();
    }

    /**
     * Determine whether or not the method being processed is a constructor.
     * 
     * @return true iff the current method is a constructor
     */
    protected boolean isConstructor() {
        return "<init>".equals(getMethodName());
    }

    /**
     * Determine whether or not the current method is the class static
     * initializer.
     * 
     * @return true iff the current method is the class static initializer
     */
    protected boolean isStaticInitializer() {
        return isStatic() && "<clinit>".equals(getMethodName());
    }

    /**
     * Get the current method's access flags.
     * 
     * @return the current method's access flags
     */
    protected int getAccessFlags() {
        return methodInfo.getAccessFlags();
    }

    /**
     * Determine whether or not the current method is a static method.
     * 
     * @return true iff the current method is a static method
     */
    protected boolean isStatic() {
        return (getAccessFlags() & ACC_STATIC) != 0;
    }

    /**
     * Get the current method's generic type signature.
     * 
     * @return the current method's generic type signature or {@code null}
     */
    protected String getSignature() {
        return methodInfo.getSignature();
    }

    /**
     * Get the argument {@link Type}s for the current method.
     * 
     * @return the argurment {@link Type}s
     */
    protected Type[] getArgumentTypes() {
        return Type.getArgumentTypes(getDescriptor());
    }

    /**
     * Get the current method's return {@link Type}.
     * 
     * @return the current method's return {@link Type}
     */
    protected Type getReturnType() {
        return Type.getReturnType(getDescriptor());
    }

    /**
     * Get the exception {@link Type}s declared to be thrown by this
     * method.
     * 
     * @return the set of exception {@link Type}s declared to be thrown
     */
    protected Set<Type> getDeclaredExceptions() {
        Set<Type> exceptionSet = new HashSet<Type>();
        for (String exception : methodInfo.getDeclaredExceptions()) {
            exceptionSet.add(Type.getType(exception));
        }
        return exceptionSet;
    }

    /**
     * Generate the instruction sequence needed to call the {@code fireProbe} method on the proxy. The parameter list must have already been setup.
     */
    protected void visitFireProbeInvocation() {
        visitMethodInsn(
                        INVOKESTATIC,
                        MonitoringProxyActivator.PROBE_PROXY_CLASS_INTERNAL_NAME,
                        FIRE_PROBE_METHOD_NAME,
                        FIRE_PROBE_METHOD_DESC, false);
    }

    /**
     * Generate the instruction sequence needed to "box" the data on the top
     * of stack (if boxing is required).
     * 
     * @param type the <code>Type</code> of the object on the stack.
     */
    protected void box(final Type type) {
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
                break;
            default:
                break;
        }
    }

    /**
     * Generate the instruction sequence needed to "unbox" the boxed data at
     * the top of stack.
     * 
     * @param type the <code>Type</code> associated with the unboxed data
     */
    protected void unbox(final Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                break;
            case Type.BYTE:
                visitTypeInsn(CHECKCAST, "java/lang/Byte");
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                break;
            case Type.CHAR:
                visitTypeInsn(CHECKCAST, "java/lang/Character");
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                break;
            case Type.DOUBLE:
                visitTypeInsn(CHECKCAST, "java/lang/Double");
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                break;
            case Type.FLOAT:
                visitTypeInsn(CHECKCAST, "java/lang/Float");
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                break;
            case Type.INT:
                visitTypeInsn(CHECKCAST, "java/lang/Integer");
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                break;
            case Type.LONG:
                visitTypeInsn(CHECKCAST, "java/lang/Long");
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                break;
            case Type.SHORT:
                visitTypeInsn(CHECKCAST, "java/lang/Short");
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                break;
            case Type.ARRAY:
            case Type.OBJECT:
                visitTypeInsn(CHECKCAST, type.getInternalName());
                break;
            default:
                break;
        }
    }

    /**
     * Generate the instruction sequence needed to create an object array
     * that holds the appropriately boxed method parameters.
     */
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
     * Create an object array and store the target method arguments in it.
     * 
     * @param desc the target method descriptor
     */
    void replaceArgsWithArray(String desc) {
        Type[] methodArgs = Type.getArgumentTypes(desc);
        createObjectArray(methodArgs.length); // [target] args... array
        for (int i = methodArgs.length - 1; i >= 0; i--) {
            if (methodArgs[i].getSize() == 2) {
                visitInsn(DUP_X2); // [target] args... array arg_arg array
                visitLdcInsn(Integer.valueOf(i)); // [target] args... array arg_arg array idx
                visitInsn(DUP2_X2); // [target] args... array array idx arg_arg array idx
                visitInsn(POP2); // [target] args... array array idx arg_arg
            } else {
                visitInsn(DUP_X1); // [target] args... array arg array
                visitInsn(SWAP); // [target] args... array array arg
                visitLdcInsn(Integer.valueOf(i)); // [target] args... array array arg idx
                visitInsn(SWAP); // [target] args... array array idx arg
            }
            box(methodArgs[i]); // [target] args... array array idx boxed
            visitInsn(AASTORE); // [target] args... array
        } // [target] array
    }

    /**
     * Recreate the parameter list for the target method from an Object array
     * containing the data.
     * 
     * @param desc the target method descriptor
     */
    void restoreArgsFromArray(String desc) {
        Type[] methodArgs = Type.getArgumentTypes(desc);
        for (int i = 0; i < methodArgs.length; i++) { // [target] array
            visitInsn(DUP); // [target] args... array array
            visitLdcInsn(Integer.valueOf(i)); // [target] args... array array idx
            visitInsn(AALOAD); // [target] args... array boxed
            unbox(methodArgs[i]); // [target] args... array arg
            if (methodArgs[i].getSize() == 2) {
                visitInsn(DUP2_X1); // [target] args... array arg_arg
                visitInsn(POP2); // [target] args... array
            } else {
                visitInsn(SWAP); // [target] args... array
            }
        }
        visitInsn(POP); // [target] args...
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

    /**
     * Called by the project injection adapter to indicate that the probe
     * instruction stream is being injected. This is used to change the
     * target of the chain to skip other probe related adapters.
     * 
     * @param inProgress true if the probe injection is starting, false
     *            if ended
     */
    protected void setProbeInProgress(boolean inProgress) {
        if (inProgress && !this.probeInProgress) {
            this.probeInProgress = true;
            if (this.probeMethodAdapter != null) {
                this.mv = this.visitor;
            }
        } else if (!inProgress && this.probeInProgress) {
            this.probeInProgress = false;
            this.mv = probeMethodAdapter != null ? probeMethodAdapter : this.visitor;
        }
    }

    private MethodVisitor getVisitor() {
        return visitor;
    }

    /**
     * Determine if a calling adapter is currently injecting code related
     * to a probe.
     * 
     * @return true if an adapter in the chain is currently injecting probe
     *         related code
     */
    protected boolean isProbeInProgress() {
        return this.probeInProgress;
    }

    protected Class<?> getOwningClass(String owner) {
        try {
            Class<?> probedClass = getProbedClass();
            ClassLoader loader = probedClass.getClassLoader();
            Type ownerType = Type.getObjectType(owner);
            int arrayDimensions = 0;
            if (ownerType.getSort() == Type.ARRAY) {
                arrayDimensions = ownerType.getDimensions();
                ownerType = ownerType.getElementType();
            }
            Class<?> clazz = loader.loadClass(ownerType.getClassName());
            while (arrayDimensions > 0) {
                Object o = Array.newInstance(clazz, 0);
                clazz = o.getClass();
                arrayDimensions--;
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            // Missing import means it can't be called at all
        } catch (Throwable t) {
        }
        return null;
    }

    protected Method getTargetMethod(Class<?> targetClass, String targetName, String desc) {
        Class<?> clazz = targetClass;
        while (clazz != null) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().equals(targetName) && Type.getMethodDescriptor(m).equals(desc)) {
                    return m;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    protected Constructor<?> getTargetConstructor(Class<?> targetClass, String desc) {
        Class<?> clazz = targetClass;
        while (clazz != null) {
            Constructor<?>[] ctors = clazz.getDeclaredConstructors();
            for (Constructor<?> ctor : ctors) {
                if (Type.getConstructorDescriptor(ctor).equals(desc)) {
                    return ctor;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    protected Field getTargetField(Class<?> targetClass, String name, String desc) {
        for (Field f : targetClass.getDeclaredFields()) {
            if (f.getName().equals(name) && Type.getDescriptor(f.getType()).equals(desc)) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    /**
     * Please note that this callback is only made for constructors. BCI
     * in the window between the first instruction callback and the method
     * entry callback can be very difficult to implement correctly.
     */
    protected void onFirstInstruction() {}

    protected void onMethodEntry() {}

    protected void onMethodExit(int opcode) {}

    private void push(Object stackElement, int count) {
        for (int i = 0; i < count; i++) {
            currentStack.add(stackElement);
        }
    }

    private void fireFirstInstruction() {
        if (!observedFirstInstruction) {
            observedFirstInstruction = true;
            if (isConstructor()) {
                onFirstInstruction();
            } else {
                onMethodEntry();
            }
        }
    }

    private void fireMethodEntry() {
        if (!waitingForSuper) {
            onMethodEntry();
        }
    }

    private void fireMethodExit(int opcode) {
        onMethodExit(opcode);
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

    //
    // Methods that are required to implement MethodVisitor
    //

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        super.visitFieldInsn(opcode, owner, name, desc);

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
                default:
                    break;
            }
        }
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        super.visitIincInsn(var, increment);

        // if (waitingForSuper) {
        //     NOOP: push(OTHER, 1); pop(1);
        // }
    }

    @Override
    public void visitInsn(int opcode) {
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        // List of opcodes taken from visitInsn documentation
        if (waitingForSuper) {
            switch (opcode) {
                case NOP:
                    break;

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
                    push(OTHER, 1); // One slot constants
                    break;
                case DCONST_0:
                case DCONST_1:
                case LCONST_0:
                case LCONST_1:
                    push(OTHER, 2); // Two slot constants
                    break;

                case IALOAD:
                case FALOAD:
                case AALOAD:
                case BALOAD:
                case CALOAD:
                case SALOAD:
                    pop(1); // pop(2); push(OTHER, 1);
                    break;
                case DALOAD:
                case LALOAD:
                    break; // pop(2); push(OTHER, 2);

                case IASTORE:
                case FASTORE:
                case AASTORE:
                case BASTORE:
                case CASTORE:
                case SASTORE:
                    pop(3); // One slot array store
                    break;
                case DASTORE:
                case LASTORE:
                    pop(4); // Two slot array store
                    break;

                case POP:
                    pop(1);
                    break;
                case POP2:
                    pop(2);
                    break;

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
                    pop(1); // One slot arithmetic and logical instructions
                    break;
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
                    pop(2); // Two slot arithmetic and logical instructions
                    break;

                case I2D:
                case I2L:
                case F2D:
                case F2L:
                    push(OTHER, 1); // One slot to two slot conversions
                    break;
                case F2I:
                case I2B:
                case I2C:
                case I2F:
                case I2S:
                    break; // One slot to one slot conversions
                case D2F:
                case D2I:
                case L2I:
                case L2F:
                    pop(1); // Two slot to one slot conversions
                    break;
                case L2D:
                case D2L:
                    break; // Two slot to two slot conversions

                case DCMPL:
                case DCMPG:
                case LCMP:
                    pop(3); // Two slot compare: pop(2); pop(2); push(OTHER, 1);
                    break;
                case FCMPL:
                case FCMPG:
                    pop(1); // One slot compare: pop(1); pop(1); push(OTHER, 1);
                    break;

                case IRETURN:
                case FRETURN:
                case ARETURN:
                    pop(1); // One slot return
                    break;
                case DRETURN:
                case LRETURN:
                    pop(2); // Two slot return
                    break;
                case RETURN:
                    break;
                case ATHROW:
                    pop(1);
                    break;

                case ARRAYLENGTH:
                    break;

                case MONITORENTER:
                case MONITOREXIT:
                    pop(1);
                    break;

                default:
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unexpected opcode ");
                    sb.append(Integer.toHexString(opcode));
                    sb.append(" for class ");
                    sb.append(methodInfo.getClassAdapter().getProbedClass());
                    throw new IllegalArgumentException(sb.toString());
            }
        }

        // Method exit callback
        switch (opcode) {
            case RETURN:
            case ARETURN:
            case DRETURN:
            case FRETURN:
            case IRETURN:
            case LRETURN:
            case ATHROW:
                fireMethodExit(opcode);
                break;
            default:
                break;
        }

        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        super.visitIntInsn(opcode, operand);

        if (waitingForSuper) {
            switch (opcode) {
                case BIPUSH:
                case SIPUSH:
                    push(OTHER, 1);
                    break;
                case NEWARRAY:
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        super.visitJumpInsn(opcode, label);

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
                default:
                    break;
            }

            addBranchTarget(label);
        }
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);

        if (waitingForSuper) {
            // Get what should be the shape of the stack in the handler.
            // Not all labels are branch targets so only deal with targets
            if (branchTargets.containsKey(label)) {
                currentStack = branchTargets.get(label);
            }
        }
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        super.visitLdcInsn(cst);

        if (waitingForSuper) {
            if (cst instanceof Long || cst instanceof Double) {
                push(OTHER, 2);
            } else {
                push(OTHER, 1);
            }
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        super.visitLookupSwitchInsn(dflt, keys, labels);

        if (waitingForSuper) {
            pop(1);

            // Setup for each branch target
            addBranchTarget(dflt);
            for (Label label : labels) {
                addBranchTarget(label);
            }
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

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
                        fireMethodEntry();
                    }
                    break;
                default:
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
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        super.visitMultiANewArrayInsn(desc, dims);

        if (waitingForSuper) {
            pop(dims);
            push(OTHER, 1);
        }
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        super.visitTableSwitchInsn(min, max, dflt, labels);

        if (waitingForSuper) {
            pop(1);

            // Setup for each branch target
            addBranchTarget(dflt);
            for (Label label : labels) {
                addBranchTarget(label);
            }
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        super.visitTypeInsn(opcode, type);

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
        if (!observedFirstInstruction) {
            fireFirstInstruction();
        }

        super.visitVarInsn(opcode, var);

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

}
