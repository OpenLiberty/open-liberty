/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Collection;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * This class is used to redefine classes that have already been loaded. The JDK only allows classes
 * whose method bodies have changed to be reloaded (see java.lang.instrument.Instrumentation.redefineClasses(...)
 * for more details.
 */
public class ClassRedefiner {
    private final static TraceComponent tc = Tr.register(ClassRedefiner.class);
    private final static String LS = System.getProperty("line.separator");
    private final Instrumentation inst;

    public ClassRedefiner(Instrumentation inst) {
        this.inst = inst;
    }

    //@Trivial
    public boolean canRedefine() {
        return inst != null && inst.isRedefineClassesSupported();
    }

    @FFDCIgnore(UnsupportedOperationException.class)
    public boolean redefineClasses(Set<ClassDefinition> classDefinitions) {
        boolean b = false;
        if (canRedefine()) {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "About to redefine classes: " + toString(classDefinitions));
                }

                inst.redefineClasses(classDefinitions.toArray(new ClassDefinition[classDefinitions.size()]));
                b = true;
            } catch (UnsupportedOperationException uoe) {
                // This is expected in two cases:
                // 1) The JVM does not support class re-definition -- if this were the case, the canRedine() method would return false, and we would never get here.
                // 2) The class cannot be redefined because it changed too much - the only change supported by the JVM at this point is the implementation of a method body.
                // Since 1 is not possible, and 2 is expected for users in a development environment, this exception should result in a no-op.
            } catch (UnmodifiableClassException uce) {
                // This occurs when the user attempted to modify a unmodifiable class (i.e. java.lang.*, etc.)
                // Log to FFDC? Trace?
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "redefineClasses caught unexpected exception ", uce);
                }
            } catch (LinkageError le) {
                // This covers most of the errors thrown by redefineClasses and would usually occur if the updated class bytes are invalid.
                // Log to FFDC
            } catch (Exception e) {
                // Used to catch NPE (which would occur if any of the classDefinitions contained a null pointer) or ClassNotFoundException - which the JDK javadocs
                // indicate should never be thrown.
                // Log to FFDC
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "redefineClasses ", b);
        }
        return b;
    }

    @Trivial
    private static String toString(Collection<ClassDefinition> classDefinitions) {
        StringBuilder sb = new StringBuilder();
        for (ClassDefinition def : classDefinitions) {
            sb.append(LS + "  ").append(def.getDefinitionClass()).append(" size: " + def.getDefinitionClassFile().length);
        }
        return sb.toString();
    }
}
