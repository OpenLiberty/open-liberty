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

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.DUP2_X1;
import static org.objectweb.asm.Opcodes.DUP2_X2;
import static org.objectweb.asm.Opcodes.DUP_X1;
import static org.objectweb.asm.Opcodes.DUP_X2;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.SWAP;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;

import com.ibm.websphere.monitor.annotation.ProbeAfterCall;
import com.ibm.websphere.monitor.annotation.ProbeAtCallError;
import com.ibm.websphere.monitor.annotation.ProbeBeforeCall;
import com.ibm.ws.monitor.internal.ListenerConfiguration;
import com.ibm.ws.monitor.internal.ProbeFilter;
import com.ibm.ws.monitor.internal.ProbeImpl;
import com.ibm.ws.monitor.internal.ProbeListener;

public class ProbeMethodCallMethodAdapter extends ProbeMethodAdapter {

    /**
     * Key to use when caching the before call target filter.
     */
    private final static String BEFORE_CALL_FILTER = ProbeBeforeCall.class.getSimpleName() + ":Filter";

    /**
     * Key to use when caching the after call target filter.
     */
    private final static String AFTER_CALL_FILTER = ProbeAfterCall.class.getSimpleName() + ":Filter";

    /**
     * Key to use when caching the at call error target filter;
     */
    private final static String AT_CALL_ERROR_FILTER = ProbeAtCallError.class.getSimpleName() + ":Filter";

    /**
     * The {@link Type} representing a java.lang.Throwable.
     */
    private final static Type THROWABLE_TYPE = Type.getType(Throwable.class);

    /**
     * Set of listeners that are interested in before call probe events.
     */
    private final Map<ProbeListener, ProbeFilter> beforeListeners = new HashMap<ProbeListener, ProbeFilter>();

    /**
     * Set of listeners that are interested in after call probe events.
     */
    private final Map<ProbeListener, ProbeFilter> afterListeners = new HashMap<ProbeListener, ProbeFilter>();

    /**
     * Set of listeners that are interested in call error probe events.
     */
    private final Map<ProbeListener, ProbeFilter> errorListeners = new HashMap<ProbeListener, ProbeFilter>();

    /**
     * For constructors, keep track of when it's safe to start the injection.
     */
    private boolean methodEntryFired = false;

    String methodName = null;

    String argsDescriptor = null;

    String methodOwner = null;

    Constructor<?> targetConstructor = null;

    Method targetMethod = null;

    /**
     * Create a new method adapter that injects a probe at method entry.
     * 
     * @param probeMethodAdapter the chained adapter
     */
    protected ProbeMethodCallMethodAdapter(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
        super(probeMethodAdapter, methodInfo);

        for (ProbeListener listener : interested) {
            ListenerConfiguration config = listener.getListenerConfiguration();

            ProbeBeforeCall probeBeforeCall = config.getProbeBeforeCall();
            if (probeBeforeCall != null) {
                ProbeFilter filter = config.getTransformerData(BEFORE_CALL_FILTER);
                if (filter == null) {
                    filter = new ProbeFilter(probeBeforeCall.clazz(), probeBeforeCall.method(), probeBeforeCall.args(), null, null);
                    config.setTransformerData(BEFORE_CALL_FILTER, filter);
                }
                beforeListeners.put(listener, filter);
            }

            ProbeAfterCall probeAfterCall = config.getProbeAfterCall();
            if (probeAfterCall != null) {
                ProbeFilter filter = config.getTransformerData(AFTER_CALL_FILTER);
                if (filter == null) {
                    filter = new ProbeFilter(probeAfterCall.clazz(), probeAfterCall.method(), probeAfterCall.args(), null, null);
                    config.setTransformerData(AFTER_CALL_FILTER, filter);
                }
                afterListeners.put(listener, filter);
            }

            ProbeAtCallError probeAtCallError = config.getProbeAtCallError();
            if (probeAtCallError != null) {
                ProbeFilter filter = config.getTransformerData(AT_CALL_ERROR_FILTER);
                if (filter == null) {
                    filter = new ProbeFilter(probeAtCallError.clazz(), probeAtCallError.method(), probeAtCallError.args(), null, null);
                    config.setTransformerData(AT_CALL_ERROR_FILTER, filter);
                }
                errorListeners.put(listener, filter);
            }
        }
    }

    @Override
    protected void onMethodEntry() {
        methodEntryFired = true;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (methodEntryFired == false || (beforeListeners.isEmpty() && afterListeners.isEmpty() && errorListeners.isEmpty())) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }

        methodName = name;
        argsDescriptor = null;
        methodOwner = null;
        targetConstructor = null;
        targetMethod = null;

        // Phase 1 - Iterate over listeners and remove those that don't match method name
        Set<ProbeListener> matchingBeforeListeners = getMatchesByMethodName(beforeListeners);
        Set<ProbeListener> matchingAfterListeners = getMatchesByMethodName(afterListeners);
        Set<ProbeListener> matchingErrorListeners = getMatchesByMethodName(errorListeners);

        // Phase 2 - Iterate over listeners and remove those that don't match method args
        if (!matchingBeforeListeners.isEmpty() || !matchingAfterListeners.isEmpty()) {
            argsDescriptor = ProbeFilter.buildArgsDescriptor(Type.getArgumentTypes(desc));
            filterByArgsDescriptor(beforeListeners, matchingBeforeListeners);
            filterByArgsDescriptor(afterListeners, matchingAfterListeners);
            filterByArgsDescriptor(errorListeners, matchingErrorListeners);
        }

        // Phase 3 - Iterate over listeners and remove those that don't match declaring class
        if (!matchingBeforeListeners.isEmpty() || !matchingAfterListeners.isEmpty()) {
            methodOwner = Type.getObjectType(owner).getClassName();
            filterByMethodOwner(beforeListeners, matchingBeforeListeners);
            filterByMethodOwner(afterListeners, matchingAfterListeners);
            filterByMethodOwner(errorListeners, matchingErrorListeners);
        }

        // Phase 4 - Check for any "advanced" filters associated with the remaining listeners.
        // If an "advanced" filter is found, we need to reflect the appropriate method data,
        // otherwise what remains in the matching listener sets is correct.
        if (!matchingBeforeListeners.isEmpty() || !matchingAfterListeners.isEmpty()) {
            if (hasAdvancedFilter(beforeListeners, matchingBeforeListeners) ||
                    hasAdvancedFilter(afterListeners, matchingAfterListeners) ||
                    hasAdvancedFilter(errorListeners, matchingErrorListeners)) {
                Class<?> targetClass = getOwningClass(owner);
                if ("<init>".equals(name)) {
                    targetConstructor = getTargetConstructor(targetClass, desc);
                    filterByConstructor(beforeListeners, matchingBeforeListeners);
                    filterByConstructor(afterListeners, matchingAfterListeners);
                    filterByConstructor(errorListeners, matchingErrorListeners);
                } else {
                    targetMethod = getTargetMethod(targetClass, name, desc);
                    filterByMethod(beforeListeners, matchingBeforeListeners);
                    filterByMethod(afterListeners, matchingAfterListeners);
                    filterByMethod(errorListeners, matchingErrorListeners);
                }
            }
        }

        if (!matchingBeforeListeners.isEmpty() || !matchingAfterListeners.isEmpty() || !matchingErrorListeners.isEmpty()) {
            processMethodInsn(opcode, owner, name, desc, matchingBeforeListeners, matchingAfterListeners, matchingErrorListeners);
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    Set<ProbeListener> getMatchesByMethodName(Map<ProbeListener, ProbeFilter> listeners) {
        Set<ProbeListener> matches = null;
        for (ProbeListener listener : listeners.keySet()) {
            ProbeFilter filter = listeners.get(listener);
            if (filter.basicMemberNameMatches(methodName)) {
                if (matches == null) {
                    matches = new HashSet<ProbeListener>();
                }
                matches.add(listener);
            }
        }
        if (matches == null) {
            matches = Collections.emptySet();
        }
        return matches;
    }

    void filterByArgsDescriptor(Map<ProbeListener, ProbeFilter> listeners, Set<ProbeListener> matches) {
        if (matches.isEmpty())
            return;
        Iterator<ProbeListener> it = matches.iterator();
        while (it.hasNext()) {
            ProbeListener listener = it.next();
            ProbeFilter filter = listeners.get(listener);
            if (!filter.basicArgsDescriptorMatches(argsDescriptor)) {
                it.remove();
            }
        }
    }

    void filterByMethodOwner(Map<ProbeListener, ProbeFilter> listeners, Set<ProbeListener> matches) {
        if (matches.isEmpty())
            return;
        Iterator<ProbeListener> it = matches.iterator();
        while (it.hasNext()) {
            ProbeListener listener = it.next();
            ProbeFilter filter = listeners.get(listener);
            if (!filter.basicClassNameMatches(methodOwner)) {
                it.remove();
            }
        }
    }

    boolean hasAdvancedFilter(Map<ProbeListener, ProbeFilter> listeners, Set<ProbeListener> matches) {
        if (matches.isEmpty())
            return false;
        for (ProbeListener listener : matches) {
            ProbeFilter filter = listeners.get(listener);
            if (filter.isAdvancedFilter()) {
                return true;
            }
        }
        return false;
    }

    void filterByConstructor(Map<ProbeListener, ProbeFilter> listeners, Set<ProbeListener> matches) {
        if (matches.isEmpty())
            return;
        Iterator<ProbeListener> it = matches.iterator();
        while (it.hasNext()) {
            ProbeListener listener = it.next();
            ProbeFilter filter = listeners.get(listener);
            if (!filter.matches(targetConstructor)) {
                it.remove();
            }
        }
    }

    void filterByMethod(Map<ProbeListener, ProbeFilter> listeners, Set<ProbeListener> matches) {
        if (matches.isEmpty())
            return;
        Iterator<ProbeListener> it = matches.iterator();
        while (it.hasNext()) {
            ProbeListener listener = it.next();
            ProbeFilter filter = listeners.get(listener);
            if (!filter.matches(targetMethod)) {
                it.remove();
            }
        }
    }

    void processMethodInsn(
                           int opcode,
                           String owner,
                           String name,
                           String desc,
                           Set<ProbeListener> matchingBeforeListeners,
                           Set<ProbeListener> matchingAfterListeners,
                           Set<ProbeListener> matchingErrorListeners) {
        boolean enabledBefore = !matchingBeforeListeners.isEmpty();
        boolean enabledAfter = !matchingAfterListeners.isEmpty();
        boolean enabledError = !matchingErrorListeners.isEmpty();

        // Indicate we're injecting probe data
        setProbeInProgress(true);

        //        // Create the handler labels and define the try / catch scope
        //        Label startLabel = new Label();
        //        Label endLabel = new Label();
        //        if (enabledError) {
        //            visitTryCatchBlock(startLabel, endLabel, endLabel, THROWABLE_TYPE.getInternalName());
        //            visitLabel(startLabel);
        //            String key = createKey("ERROR", owner, name, desc);
        //            ProbeImpl probe = getProbe(key);
        //        }

        // Replace args with array so we can work off the stack
        boolean nullTarget = (opcode == INVOKESTATIC);
        if (enabledBefore || (enabledAfter && !nullTarget)) {
            replaceArgsWithArray(desc); // [target] array
        }

        // If we're doing a probe after the method call, we'll need to
        // dup the target reference if non-null so we can pass it along
        // in the probe payload
        if (enabledAfter && !nullTarget) {
            visitInsn(DUP2); // [target] array target array
            visitInsn(POP); // [target] array target
            visitInsn(SWAP); // [target] target array
        }

        // If a probe before the call is enabled, fire the probe.
        // The stack must have the array containing the args on the top
        // and the method target (if needed) immediately below the array.
        // This structure must be preserved on exit.
        if (enabledBefore) {
            beforeVisitMethodInsn(matchingBeforeListeners, opcode, owner, name, desc, nullTarget);
        }

        // We need to restore the parm list from the array
        if (enabledBefore || (enabledAfter && !nullTarget)) {
            restoreArgsFromArray(desc); // [target] args...
        }

        setProbeInProgress(false);
        super.visitMethodInsn(opcode, owner, name, desc, false);

        // If a probe after the call is enabled, fire the probe.
        // The stack must contain the return data (if any) from the
        // method at the top and the instance that was driven just
        // below that.
        if (enabledAfter) {
            setProbeInProgress(true);
            afterVisitMethodInsn(matchingAfterListeners, opcode, owner, name, desc, nullTarget);
            setProbeInProgress(false);
        }

        //        if (enabledError) {
        //            setProbeInProgress(true);
        //            visitLabel(endLabel);
        //            setProbeInProgress(false);
        //        }

    }

    void beforeVisitMethodInsn(
                               Set<ProbeListener> listeners,
                               int opcode,
                               String owner,
                               String name,
                               String desc,
                               boolean nullTarget) {
        String key = createKey("BEFORE", owner, name, desc);
        ProbeImpl probe = getProbe(key); // TODO: Target class and method
        long probeId = probe.getIdentifier();

        //
        // Setup the correct method target reference
        //
        if (nullTarget || (opcode == INVOKESPECIAL && "<init>".equals(name))) {
            visitInsn(DUP); // [target] array array
            visitInsn(ACONST_NULL); // [target] array array target
            visitInsn(SWAP); // [target] array target array
        } else {
            visitInsn(DUP2); // [target] array target array
        }

        visitLdcInsn(Long.valueOf(probeId)); // [target] array target array long1 long2
        visitInsn(DUP2_X2); // [target] array long1 long2 target array long1 long2
        visitInsn(POP2); // [target] array long1 long2 target array
        if (isStatic()) {
            visitInsn(ACONST_NULL); // [target] array long1 long2 target array this
        } else {
            visitVarInsn(ALOAD, 0); // [target] array long1 long2 target array this
        }
        visitInsn(DUP_X2); // [target] array long1 long2 this target array this
        visitInsn(POP); // [target] array long1 long2 this target array
        visitFireProbeInvocation(); // [target] array

        setProbeListeners(probe, listeners);
    }

    void afterVisitMethodInsn(
                              Set<ProbeListener> listeners,
                              int opcode,
                              String owner,
                              String name,
                              String desc,
                              boolean nullTarget) {
        String key = createKey("AFTER", owner, name, desc);
        ProbeImpl probe = getProbe(key); // TODO: Target class and method
        long probeId = probe.getIdentifier();

        Type returnType = Type.getReturnType(desc);

        if (returnType.getSort() == Type.VOID) {
            if (nullTarget) {
                visitInsn(ACONST_NULL); // target
                visitInsn(ACONST_NULL); // target boxed
            } else {
                visitInsn(ACONST_NULL); // target boxed
            }
        } else if (returnType.getSize() == 2) {
            if (nullTarget) {
                visitInsn(DUP2); // retval retval
                box(returnType); // retval boxed
                visitInsn(ACONST_NULL); // retval boxed target
                visitInsn(SWAP); // retval target boxed
            } else {
                visitInsn(DUP2_X1); // retval target retval
                box(returnType); // retval target boxed
            }
        } else {
            if (nullTarget) {
                visitInsn(DUP); // retval retval
                box(returnType); // retval boxed
                visitInsn(ACONST_NULL); // retval boxed target
                visitInsn(SWAP); // retval target boxed
            } else {
                visitInsn(DUP_X1); // retval target retval
                box(returnType); // retval target boxed
            }
        }

        visitLdcInsn(Long.valueOf(probeId)); // retval target boxed long1 long2
        visitInsn(DUP2_X2); // retval long1 long2 target boxed long1 long2
        visitInsn(POP2); // retval long1 long2 target boxed
        if (isStatic()) {
            visitInsn(ACONST_NULL); // retval long1 long2 target boxed this
        } else {
            visitVarInsn(ALOAD, 0); // retval long1 long2 target boxed this
        }
        visitInsn(DUP_X2); // retval long1 long2 this target boxed this
        visitInsn(POP); // retval long1 long2 this target boxed
        visitFireProbeInvocation();

        setProbeListeners(probe, listeners);
    }

    String createKey(String stem, String owner, String methodName, String desc) {
        StringBuilder sb = new StringBuilder("METHOD_CALL: ").append(stem).append(" ");
        sb.append(getMethodName());
        sb.append(getDescriptor());
        sb.append(" calls ");
        sb.append(owner).append(".").append(methodName).append(desc);
        return sb.toString();
    }

}
