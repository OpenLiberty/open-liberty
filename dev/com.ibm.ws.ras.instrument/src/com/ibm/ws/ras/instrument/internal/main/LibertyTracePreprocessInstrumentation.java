/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ras.instrument.internal.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.SerialVersionUIDAdder;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import com.ibm.ws.ras.instrument.internal.bci.CheckInstrumentableClassAdapter;
import com.ibm.ws.ras.instrument.internal.bci.FFDCClassAdapter;
import com.ibm.ws.ras.instrument.internal.bci.JSR47TracingClassAdapter;
import com.ibm.ws.ras.instrument.internal.bci.JSR47TracingMethodAdapter;
import com.ibm.ws.ras.instrument.internal.bci.LibertyTracePreprocessClassAdapter;
import com.ibm.ws.ras.instrument.internal.bci.LibertyTracingClassAdapter;
import com.ibm.ws.ras.instrument.internal.bci.LibertyTracingMethodAdapter;
import com.ibm.ws.ras.instrument.internal.bci.WebSphereTrTracingClassAdapter;
import com.ibm.ws.ras.instrument.internal.bci.WebSphereTrTracingMethodAdapter;
import com.ibm.ws.ras.instrument.internal.introspect.InjectedTraceAnnotationVisitor;
import com.ibm.ws.ras.instrument.internal.introspect.TraceObjectFieldAnnotationVisitor;
import com.ibm.ws.ras.instrument.internal.introspect.TraceOptionsAnnotationVisitor;
import com.ibm.ws.ras.instrument.internal.model.PackageInfo;
import com.ibm.ws.ras.instrument.internal.model.TraceOptionsData;
import com.ibm.ws.ras.instrument.internal.model.TraceType;

/**
 * Class file transformer that pre-processes annotations, configuration files,
 * and other metadata that controls dynamic trace injection and injects the
 * aggregated configuration into the classes in a format that allows more
 * efficient bytecode transformations at runtime.
 */
//TODO: Inner class support to look for Options on outer class before package?
public class LibertyTracePreprocessInstrumentation extends AbstractInstrumentation {

    public final static Type TRIVIAL_TYPE = Type.getType(com.ibm.websphere.ras.annotation.Trivial.class);
    public final static Type TRACE_OPTIONS_TYPE = Type.getType(com.ibm.websphere.ras.annotation.TraceOptions.class);

    public final static Type LIBERTY_TR_TYPE = LibertyTracingClassAdapter.TR_TYPE;
    public final static Type LIBERTY_TRACE_COMPONENT_TYPE = LibertyTracingClassAdapter.TRACE_COMPONENT_TYPE;

    public final static Type WEBSPHERE_TR_TYPE = WebSphereTrTracingClassAdapter.TR_TYPE;
    public final static Type WEBSPHERE_TRACE_COMPONENT_TYPE = WebSphereTrTracingClassAdapter.TRACE_COMPONENT_TYPE;

    public final static Type LOGGER_TYPE = Type.getType(java.util.logging.Logger.class);

    public final static Type INJECTED_TRACE_TYPE = Type.getType(com.ibm.websphere.ras.annotation.InjectedTrace.class);
    public final static Type MANUAL_TRACE_TYPE = Type.getType(com.ibm.websphere.ras.annotation.ManualTrace.class);
    public final static Type TRACE_OBJECT_FIELD_TYPE = Type.getType(com.ibm.websphere.ras.annotation.TraceObjectField.class);

    private boolean addFfdc = false;
    private boolean injectStatic = false;
    private String defaultTraceComponentName = "$$$tc$$$";


	private TraceType defaultTraceType = TraceType.LIBERTY;

    /**
     * Transient class that collects class information needed during
     * pre-processing.
     */
    public class ClassTraceInfo {
        ClassNode classNode;
        public PackageInfo packageInfo;

        // Explicitly declared Liberty TraceComponent
        FieldNode libertyTraceComponentFieldNode;
        boolean libertyTraceComponentFieldAlreadyInitialized;

        // Explicitly declared WebSphere TraceComponent
        FieldNode websphereTraceComponentFieldNode;
        boolean websphereTraceComponentFieldAlreadyInitialized;

        // Explicitly declared j.u.l.Logger
        FieldNode loggerFieldNode;
        boolean loggerFieldAlreadyInitialized;

        // Trace state field we'll be using
        FieldNode traceStateField;
        boolean traceStateFieldAlreadyInitialized;

        List<String> warnings = new ArrayList<String>();
        boolean failInstrumentation;
		public TraceOptionsData getTraceOptionsData() {
			if (packageInfo != null)
				return packageInfo.getTraceOptionsData();
			return null;
		}
    }

    /**
     * Default constructor for programmatic use.
     */
    public LibertyTracePreprocessInstrumentation() {}

    /**
     * Find the described annotation in the list of {@code AnnotationNode}s.
     * 
     * @param desc the annotation descriptor
     * @param annotations the list of annotations
     * 
     * @return the annotation that matches the provided descriptor or null
     *         if no matching annotation was found
     */
    private AnnotationNode getAnnotation(String desc, List<AnnotationNode> annotations) {
        if (annotations == null) {
            return null;
        }
        for (AnnotationNode an : annotations) {
            if (desc.equals(an.desc)) {
                return an;
            }
        }
        return null;
    }

    /**
     * Find the described field in the list of {@code FieldNode}s.
     * 
     * @param desc the field type descriptor
     * @param fields the list of fields
     * 
     * @return the fields the match the provided descriptor
     */
    private List<FieldNode> getFieldsByDesc(String desc, List<FieldNode> fields) {
        List<FieldNode> result = new ArrayList<FieldNode>();
        for (FieldNode fn : fields) {
            if (desc.equals(fn.desc)) {
                result.add(fn);
            }
        }
        return result;
    }

    /**
     * Find the methods with the given name in the list of {@code MethodNode}s.
     * 
     * @param name the method name to search for
     * @param methods the list of methods to search
     * 
     * @return the methods that match the provided name
     */
    private List<MethodNode> getMethods(String name, List<MethodNode> methods) {
        List<MethodNode> result = new ArrayList<MethodNode>();
        for (MethodNode mn : methods) {
            if (name.equals(mn.name)) {
                result.add(mn);
            }
        }
        return result;
    }

    /**
     * Find the method with the given name and descriptor in the list of {@code MethodNode}s.
     * 
     * @param name the method name
     * @param desc the method descriptor
     * @param methods the list of methods to search
     * 
     * @return the matching {@code MethodNode} if found or null if not found
     */
    private MethodNode getMethod(String name, String desc, List<MethodNode> methods) {
        for (MethodNode mn : methods) {
            if (name.equals(mn.name) && desc.equals(mn.desc)) {
                return mn;
            }
        }
        return null;
    }

    /**
     * Determine if the class is &quot;trivial&quot;.
     * 
     * @param info the collected class information
     */
    private boolean isClassTrivial(ClassTraceInfo info) {
        AnnotationNode trivialAnnotation = getAnnotation(TRIVIAL_TYPE.getDescriptor(), info.classNode.visibleAnnotations);
        if (trivialAnnotation != null) {
            return true;
        }
        return false;
    }
    
    /**
     * Determine if the class is an Inner class
     * 
     * @param info the collected class information
     */
    private boolean isInnerClass(ClassTraceInfo info) {
    	
	if(info.classNode.innerClasses.isEmpty())
		return false;
	else {
		int innerIdentifierIndex = info.classNode.name.lastIndexOf("$");
		if (innerIdentifierIndex == -1)
			return false;
		return true;
		}
    }

    /**
     * Locate and merge the metadata from the {@code TraceOptions} annotations
     * specified on the class and the package. This is used to determine the
     * resource bundle name, trace group names, and other miscellaneous info.
     * <p>
     * The class annotation is intended to override package information when
     * appropriate.
     * 
     * @param info the collected class information
     */
    private void processClassTraceOptionsAnnotation(ClassTraceInfo info) {
        // Get class annotation
        AnnotationNode traceOptionsAnnotation = getAnnotation(TRACE_OPTIONS_TYPE.getDescriptor(), info.classNode.visibleAnnotations);
        if (traceOptionsAnnotation != null) {
            TraceOptionsAnnotationVisitor optionsVisitor = new TraceOptionsAnnotationVisitor();
            traceOptionsAnnotation.accept(optionsVisitor);
            TraceOptionsData traceOptions = optionsVisitor.getTraceOptionsData();

            // Merge with package annotation's defaults
            TraceOptionsData packageData = info.packageInfo != null ? info.packageInfo.getTraceOptionsData() : null;
            if (packageData != null) {
                // Remove the current annotation if present
                if (traceOptionsAnnotation != null) {
                    info.classNode.visibleAnnotations.remove(traceOptionsAnnotation);
                }

                // If the class trace options differ from the package trace
                // options, merge them and add a class annotation.
                if (!traceOptions.equals(packageData)) {
                    if (traceOptions.getMessageBundle() == null && packageData.getMessageBundle() != null) {
                        traceOptions.setMessageBundle(packageData.getMessageBundle());
                    }
                    if (traceOptions.getTraceGroups().isEmpty() && !packageData.getTraceGroups().isEmpty()) {
                        for (String group : packageData.getTraceGroups()) {
                            traceOptions.addTraceGroup(group);
                        }
                    }

                    traceOptionsAnnotation = (AnnotationNode) info.classNode.visitAnnotation(TRACE_OPTIONS_TYPE.getDescriptor(), true);
                    AnnotationVisitor groupsVisitor = traceOptionsAnnotation.visitArray("traceGroups");
                    for (String group : traceOptions.getTraceGroups()) {
                        groupsVisitor.visit(null, group);
                    }
                    groupsVisitor.visitEnd();

                    traceOptionsAnnotation.visit("traceGroup", "");
                    traceOptionsAnnotation.visit("messageBundle", traceOptions.getMessageBundle() == null ? "" : traceOptions.getMessageBundle());
                    traceOptionsAnnotation.visit("traceExceptionThrow", Boolean.valueOf(traceOptions.isTraceExceptionThrow()));
                    traceOptionsAnnotation.visit("traceExceptionHandling", Boolean.valueOf(traceOptions.isTraceExceptionHandling()));
                    traceOptionsAnnotation.visitEnd();
                }
            }
        }
    }

    /**
     * Introspect the class to obtain the list of fields declared as {@code com.ibm.websphere.ras.TraceComponent}s. Only static
     * declarations are considered.
     * 
     * @param info the collected class information
     */
    private void processLibertyTraceComponentDiscovery(ClassTraceInfo info) {
        List<FieldNode> traceComponentFields = getFieldsByDesc(LIBERTY_TRACE_COMPONENT_TYPE.getDescriptor(), info.classNode.fields);
        if (!traceComponentFields.isEmpty()) {
            // Remove references to non-static TraceComponents
            for (int i = traceComponentFields.size() - 1; i >= 0; i--) {
                FieldNode fn = traceComponentFields.get(i);
                if ((fn.access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
                	// Trace Component fields found, but not static
                    traceComponentFields.remove(i);
                    StringBuilder sb = new StringBuilder();
                    sb.append("WARNING: TraceComponent field declared but must be static in class: ");
                    sb.append(info.classNode.name.replaceAll("/", "\\."));
                    info.warnings.add(sb.toString());
                    info.failInstrumentation = true;
                }
            }
            if (traceComponentFields.size() > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("WARNING: Multiple com.ibm.websphere.ras.TraceComponent fields declared on class ");
                sb.append(info.classNode.name.replaceAll("/", "\\.")).append(": ");
                for (int i = 0; i < traceComponentFields.size(); i++) {
                    sb.append(traceComponentFields.get(i).name);
                    if (i + 1 != traceComponentFields.size()) {
                        sb.append(", ");
                    }
                }
                info.warnings.add(sb.toString());
            }

                     
            // Keep track of the first static TraceComponent
            if (traceComponentFields.size() > 0) {
                info.libertyTraceComponentFieldNode = traceComponentFields.get(0);
            }
        }
    }
    
    public String getDefaultTraceComponentName() {
		return defaultTraceComponentName;
	}

	/**
     * Introspect the class to obtain the list of fields declared as {@code com.ibm.ejs.ras.TraceComponent}s. Only static declarations
     * are considered.
     * 
     * @param info the collected class information
     */
    private void processWebsphereTraceComponentDiscovery(ClassTraceInfo info) {
        List<FieldNode> traceComponentFields = getFieldsByDesc(WEBSPHERE_TRACE_COMPONENT_TYPE.getDescriptor(), info.classNode.fields);
        if (!traceComponentFields.isEmpty()) {
            // Remove references to non-static TraceComponents
            for (int i = traceComponentFields.size() - 1; i >= 0; i--) {
                FieldNode fn = traceComponentFields.get(i);
                if ((fn.access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
                    traceComponentFields.remove(i);
                }
            }
            if (traceComponentFields.size() > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("WARNING: Multiple com.ibm.ejs.ras.TraceComponent fields declared on class ");
                sb.append(info.classNode.name.replaceAll("/", "\\.")).append(": ");
                for (int i = 0; i < traceComponentFields.size(); i++) {
                    sb.append(traceComponentFields.get(i).name);
                    if (i + 1 != traceComponentFields.size()) {
                        sb.append(", ");
                    }
                }
                info.warnings.add(sb.toString());
            }

            // Keep track of the first static TraceComponent
            if (traceComponentFields.size() > 0) {
                info.websphereTraceComponentFieldNode = traceComponentFields.get(0);
            }
        }
    }

    /**
     * Introspect the class to obtain the list of fields declared as {@code Logger}s. Only static fields are considered.
     * 
     * @param info the collected class information
     */
    private void processJavaLoggerDiscovery(ClassTraceInfo info) {
        List<FieldNode> loggerFields = getFieldsByDesc(LOGGER_TYPE.getDescriptor(), info.classNode.fields);
        if (!loggerFields.isEmpty()) {
            // Remove references to non-static Loggers
            for (int i = loggerFields.size() - 1; i >= 0; i--) {
                FieldNode fn = loggerFields.get(i);
                if ((fn.access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
                    loggerFields.remove(i);
                    StringBuilder sb = new StringBuilder();
                    sb.append("WARNING: Non-static java.util.logging.Logger field declared on class ");
                    sb.append(info.classNode.name.replaceAll("/", "\\.")).append(": ");
                    sb.append(fn.name);
                    info.warnings.add(sb.toString());
                }
            }
            if (loggerFields.size() > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("WARNING: Multiple java.util.logging.Logger fields declared on class ");
                sb.append(info.classNode.name.replaceAll("/", "\\.")).append(": ");
                for (int i = 0; i < loggerFields.size(); i++) {
                    sb.append(loggerFields.get(i).name);
                    if (i + 1 != loggerFields.size()) {
                        sb.append(", ");
                    }
                }
                info.warnings.add(sb.toString());
            }

            // Keep track of the first static Logger
            if (loggerFields.size() > 0) {
                info.loggerFieldNode = loggerFields.get(0);
            }
        }
    }

    /**
     * Find or create the field that will hold the {@code TraceComponent} or {@code Logger} and create a class level annotation holding the field
     * name and descriptor.
     * 
     * @param info the collected class information
     */
    private void setupTraceStateObjectField(ClassTraceInfo info) {
        // Skip adding trace object field if it already exists
        AnnotationNode traceObjectAnnotation = getAnnotation(TRACE_OBJECT_FIELD_TYPE.getDescriptor(), info.classNode.visibleAnnotations);
        if (traceObjectAnnotation != null) {        	
            TraceObjectFieldAnnotationVisitor visitor = new TraceObjectFieldAnnotationVisitor();
            traceObjectAnnotation.accept(visitor);
            List<FieldNode> fields = getFieldsByDesc(visitor.getFieldDescriptor(), info.classNode.fields);
            for (FieldNode fn : fields) {
                if (fn.name.equals(visitor.getFieldName())) {
                    info.traceStateField = fn;
                    break;
                }
            }
            if (info.traceStateField != null) // Only return if matching field found
            	return;
            
        }

        // If a logger or trace component has been declared, use it.
        // Preference is given to the Liberty TraceComponent over the
        // WebSphere TraceComponent and either TraceComponent over a
        // Logger reference.  If none are declared, generate as a
        // synthetic.
        if (info.libertyTraceComponentFieldNode != null) {
            info.traceStateField = info.libertyTraceComponentFieldNode;
            info.traceStateFieldAlreadyInitialized = info.libertyTraceComponentFieldAlreadyInitialized;
        } else if (info.websphereTraceComponentFieldNode != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("INFO: Runtime BCI is not supported for com.ibm.ejs.ras.  Build-time BCI will be used for class ");
            sb.append(info.classNode.name.replace('/', '.'));
            sb.append(".  Consider using the com.ibm.websphere.ras package.");
            info.warnings.add(sb.toString());
            info.traceStateField = info.websphereTraceComponentFieldNode;
            info.traceStateFieldAlreadyInitialized = info.websphereTraceComponentFieldAlreadyInitialized;
        } else if (info.loggerFieldNode != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("INFO: Runtime BCI is not supported for JSR47 Logging.  Build-time BCI will be used for class ");
            sb.append(info.classNode.name.replace('/', '.'));
            sb.append(".");
            info.warnings.add(sb.toString());
            info.traceStateField = info.loggerFieldNode;
            info.traceStateFieldAlreadyInitialized = info.loggerFieldAlreadyInitialized;
        } else if (defaultTraceType == TraceType.LIBERTY) {
            // TODO: Check for an outer class and a declared field
            int access = (Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC);
            info.traceStateField = (FieldNode) info.classNode.visitField(access, getDefaultTraceComponentName(), LIBERTY_TRACE_COMPONENT_TYPE.getDescriptor(), null, null);
        }

        // Add the class annotation with the field name and descriptor
        if (info.traceStateField != null) {
            AnnotationVisitor av = info.classNode.visitAnnotation(TRACE_OBJECT_FIELD_TYPE.getDescriptor(), true);
            av.visit("fieldName", info.traceStateField.name);
            av.visit("fieldDesc", info.traceStateField.desc);
            av.visitEnd();
        }
    }

    /**
     * Check if the specified method has already been annotated as processed by the
     * trace injection framework.
     * 
     * @param methodNode the method to examine
     * 
     * @return true if a non-FFDC RAS method adapter processed the specified method
     */
    private boolean isMethodAlreadyInjectedAnnotationPresent(MethodNode methodNode) {
        AnnotationNode injectedTraceAnnotation = getAnnotation(INJECTED_TRACE_TYPE.getDescriptor(), methodNode.visibleAnnotations);
        AnnotationNode manualTraceAnnotation = getAnnotation(MANUAL_TRACE_TYPE.getDescriptor(), methodNode.visibleAnnotations);
        if (manualTraceAnnotation != null)
            return true;

        if (injectedTraceAnnotation != null) {
            InjectedTraceAnnotationVisitor itav = new InjectedTraceAnnotationVisitor();
            injectedTraceAnnotation.accept(itav);
            List<String> methodAdapters = itav.getMethodAdapters();
            if (methodAdapters.contains(LibertyTracingMethodAdapter.class.getName())) {
                return true;
            }
            if (methodAdapters.contains(WebSphereTrTracingMethodAdapter.class.getName())) {
                return true;
            }
            if (methodAdapters.contains(JSR47TracingMethodAdapter.class.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Analyze the class static initializer looking for initializations of the various Logger/TraceComponent fields.
     * <p>
     * When the next phase of instrumentation occurs, the code to get a {@code Logger} or a {@code TraceComponent} will be injected at the
     * beginning of the class static initializer if one is not already present.
     * 
     * @param info the collected class information
     */
    private void processExistingStaticInitializer(ClassTraceInfo info) {
        List<MethodNode> clinitMethods = getMethods("<clinit>", info.classNode.methods);
        MethodNode staticInitializer = clinitMethods.isEmpty() ? null : clinitMethods.get(0);
        if (staticInitializer == null) {
            return;
        }
        if (isMethodAlreadyInjectedAnnotationPresent(staticInitializer)) {
            return;
        }

        Iterator<? extends AbstractInsnNode> instructionIterator = staticInitializer.instructions.iterator();
        
        while (instructionIterator.hasNext()) {
            AbstractInsnNode insnNode = instructionIterator.next();
            // Determine if a Logger/TraceComponent field is being initialized.
            if (insnNode.getType() == AbstractInsnNode.FIELD_INSN) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insnNode;
                if (fieldInsn.getOpcode() == Opcodes.PUTSTATIC) {
                    if (info.libertyTraceComponentFieldNode != null && fieldInsn.name.equals(info.libertyTraceComponentFieldNode.name)) {
                    	if (fieldInsn.getPrevious().getOpcode() == Opcodes.INVOKESTATIC) {
                    		info.libertyTraceComponentFieldAlreadyInitialized = true;
                    	}
                    }
                    if (info.websphereTraceComponentFieldNode != null && fieldInsn.name.equals(info.websphereTraceComponentFieldNode.name)) {
                        info.websphereTraceComponentFieldAlreadyInitialized = true;
                    }
                    if (info.loggerFieldNode != null && fieldInsn.name.equals(info.loggerFieldNode.name)) {
                        info.loggerFieldAlreadyInitialized = true;
                    }
                    		
                }
            }
        }
    }

    /**
     * Examine the class's {@code toString()} implementation (if present) and
     * warn if traced methods are called.
     * 
     * @param info the collected class information
     */
    private void processToString(ClassTraceInfo info) {
        for (MethodNode mn : (List<MethodNode>) info.classNode.methods) {
            if (!mn.name.equals("toString") || !mn.desc.equals("()Ljava/lang/String;")) {
                continue;
            }
            Iterator<? extends AbstractInsnNode> instructionIterator = mn.instructions.iterator();
            INSN_LOOP: while (instructionIterator.hasNext()) {
                AbstractInsnNode insnNode = instructionIterator.next();
                if (insnNode.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insnNode;

                    // Skip static methods
                    if (methodInsn.getOpcode() == Opcodes.INVOKESTATIC) {
                        continue;
                    }

                    // Skip methods that aren't part of this class
                    if (!methodInsn.owner.equals(info.classNode.name)) {
                        continue;
                    }

                    // Skip explicitly trivial methods from this file class
                    MethodNode m = getMethod(methodInsn.name, methodInsn.desc, info.classNode.methods);
                    if (m != null && getAnnotation(TRIVIAL_TYPE.getDescriptor(), m.visibleAnnotations) != null) {
                        continue;
                    }

                    // If the target method is not found on this class and the super class
                    // is in the set of files being instrumented, see if it's trivial
                    String superName = info.classNode.superName;
                    while (m == null && superName != null && !"java/lang/Object".equals(superName)) {
                        if (superName.startsWith("java/") || superName.startsWith("javax/")) {
                            continue INSN_LOOP;
                        }
                        InputStream superClassInputStream = getClassInputStream(superName);
                        if (superClassInputStream == null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("INFO: ").append(info.classNode.name.replaceAll("/", "\\."));
                            sb.append(" extends class ").append(superName.replaceAll("/", "\\."));
                            sb.append(" and is calling ").append(methodInsn.name);
                            sb.append(" from toString()");
                            info.warnings.add(sb.toString());
                            continue INSN_LOOP;
                        }
                        ClassNode cn = getClassNode(superClassInputStream, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                        m = getMethod(methodInsn.name, methodInsn.desc, cn.methods);
                        superName = cn.superName;
                    }

                    // Found a method declaration on the super class that's marked as trivial
                    if (m != null && getAnnotation(TRIVIAL_TYPE.getDescriptor(), m.visibleAnnotations) != null) {
                        continue;
                    }

                    // Add a warning
                    StringBuilder sb = new StringBuilder();
                    sb.append("WARNING: ").append(info.classNode.name.replaceAll("/", "\\."));
                    sb.append(" is calling traceable methods from toString(); this may result in infinite recursion.  ");
                    sb.append("Consider referencing class fields or marking the called methods trivial to avoid trace.");
                    info.warnings.add(sb.toString());
                    break;
                }
            }
        }
    }

    /**
     * Read an input stream to populate a {@code ClassNode}.
     * 
     * @param inputStream the input stream containing the byte code
     * @param flags flags to pass to the class reader
     * 
     * @return the {@code ClassNode} or {@code null} if an error ocurred
     */
    private ClassNode getClassNode(InputStream inputStream, int flags) {
        ClassNode cn = new ClassNode();
        try {
            ClassReader reader = new ClassReader(inputStream);
            reader.accept(cn, flags);
            inputStream.close();
        } catch (IOException e) {
            cn = null;
        }
        return cn;
    }

    /**
     * Process the class and look for hard-coded entry/exit trace points.
     * Methods with hard-coded trace points will not be instrumented and
     * a warning will be issued.
     * 
     * @param info the collected class information
     */
    private void processManuallyTracedMethods(ClassTraceInfo info) {
        for (MethodNode mn : (List<MethodNode>) info.classNode.methods) {
            // Don't re-process methods that have already had trace injected
            if (isMethodAlreadyInjectedAnnotationPresent(mn)) {
                continue;
            }

            // Look through the method's instruction stream for well known entry/exit methods
            Iterator<? extends AbstractInsnNode> instructionIterator = mn.instructions.iterator();
            while (instructionIterator.hasNext()) {
                AbstractInsnNode insnNode = instructionIterator.next();

                // Look for calls to Tr.entry, Tr.exit, Logger.entering, Logger.exiting
                boolean manuallyTraced = false;
                if (insnNode.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insnNode;
                    String methodName = methodInsn.name;
                    if (methodInsn.owner.equals(LOGGER_TYPE.getInternalName())) {
                        manuallyTraced = (methodName.equals("entering") || methodName.equals("exiting"));
                    } else if (methodInsn.owner.equals(LIBERTY_TR_TYPE.getInternalName())) {
                        manuallyTraced = (methodName.equals("entry") || methodName.equals("exit"));
                    } else if (methodInsn.owner.equals(WEBSPHERE_TR_TYPE.getInternalName())) {
                        manuallyTraced = (methodName.equals("entry") || methodName.equals("exit"));
                    }
                }

                // Mark the manually traced method, and create a warning
                if (manuallyTraced) {
                    mn.visitAnnotation(MANUAL_TRACE_TYPE.getDescriptor(), true).visitEnd();

                    StringBuilder sb = new StringBuilder();
                    sb.append("WARNING: Hard coded entry/exit trace point found in ");
                    sb.append(info.classNode.name.replaceAll("/", "\\.")).append(".").append(mn.name).append(mn.desc);
                    sb.append(".  Skipping method.");
                    info.warnings.add(sb.toString());
                    break;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] transform(InputStream classfileStream) throws IOException {

        // Read in the class bytes and chain to the serialization version adpater.
        // If we fail to calculate the serialVersionUID before mucking around with
        // the class we'll very likely introduce issues with serializable classes
        // that have not coded the default serialVersionUID.
        //
        // ClassReader --> SerialVersionUIDAder --> CheckInstrumentableClassAdapter --> ClassNode
        ClassReader reader = new ClassReader(classfileStream);
        ClassNode directory = new ClassNode();
        CheckInstrumentableClassAdapter checkInstrumentableAdapter = new CheckInstrumentableClassAdapter(directory);
        SerialVersionUIDAdder uidAdder = new SerialVersionUIDAdder(checkInstrumentableAdapter);

        // Read the class information into the ClassNode tree
        reader.accept(uidAdder, 0);

        // Create a transient object to hold the parsed information
        ClassTraceInfo info = new ClassTraceInfo();
        info.classNode = directory;
        info.packageInfo = getPackageInfo(directory.name.replaceAll("/[^/]+$", ""));


        // #1: Check for a trace options annotation
        if (!isInnerClass(info))
        	processClassTraceOptionsAnnotation(info);

        // #2: Look for declared TraceComponents
        processLibertyTraceComponentDiscovery(info);
        processWebsphereTraceComponentDiscovery(info);
       

        // #3: Look for declared Logger fields
        processJavaLoggerDiscovery(info);

        // #4: See if TraceComponent and a Logger were defined
        int declaredTraceStateFieldCount = 0;
        if (info.libertyTraceComponentFieldNode != null) {
            declaredTraceStateFieldCount++;
        }
        
        // #5: Check for a trivial annotation on the class - and if a static field exists - continue - otherwise return
        if (!checkInstrumentableAdapter.isInstrumentableClass() || ( isClassTrivial(info) && declaredTraceStateFieldCount == 0)) {
            return null;
        }
		

		if (info.websphereTraceComponentFieldNode != null) {
			declaredTraceStateFieldCount++;
		}
		if (info.loggerFieldNode != null) {
			declaredTraceStateFieldCount++;
		}
		if (declaredTraceStateFieldCount > 1) {
			StringBuilder sb = new StringBuilder();
			sb.append("WARNING: More than one type of tracing has been detected on class ");
			sb.append(info.classNode.name.replaceAll("/", "\\."));
			info.warnings.add(sb.toString());
		}
		
		// #6 Check if Inner class, skip any static field initialization if doesn't
		// already exist
		if (!isInnerClass(info) || ((isInnerClass(info)) && declaredTraceStateFieldCount == 0)) {

			// #7: Determine if Logger/TraceComponent is initialized
			processExistingStaticInitializer(info);

			// #8: Define the TraceComponent if needed
			setupTraceStateObjectField(info);
			
		}

		// #9: Look at the toString method for calls to locally declared methods
		processToString(info);

		// #10: Look for methods that have hard-coded entry/exit trace points
		processManuallyTracedMethods(info);

		// #11: Dump the list of warnings
		for (String warning : info.warnings) {
			System.out.println(warning);
		}

		if (info.failInstrumentation) {
			System.out.println(
					"ERROR: Instrumentation failed for " + info.classNode.name + ".  Please see previous messages");
			return null;
		}

		// Create the ClassWriter
		ClassWriter classWriter = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		ClassVisitor cv = classWriter;

		// Trace the class as it's visited if debug is enabled
		if (isDebug()) {
			cv = new CheckClassAdapter(cv);
			cv = new TraceClassVisitor(cv, new PrintWriter(System.out));
		}

		// If requested, inject tracing at invocation by chaining.
		// Static injection for JSR47 or WebSphere is always done our of
		// the pre-process class adpater.
		if (injectStatic && info.traceStateField != null
				&& LIBERTY_TRACE_COMPONENT_TYPE.getDescriptor().equals(info.traceStateField.desc)) {
			cv = new LibertyTracingClassAdapter(cv, info, true);
		}

		// Pre-process the class and inject FFDC if requested
		if (info.traceStateField != null) {
			cv = new LibertyTracePreprocessClassAdapter(cv, !info.traceStateFieldAlreadyInitialized, info);
		} else if (defaultTraceType == TraceType.TR) {
			cv = new WebSphereTrTracingClassAdapter(cv, null, info);
		} else if (defaultTraceType == TraceType.JAVA_LOGGING) {
			cv = new JSR47TracingClassAdapter(cv, null, info);
		}
		
        if (addFfdc && !isClassTrivial(info)) {
            cv = new FFDCClassAdapter(cv, null,info);
        }
        directory.accept(cv);

        return classWriter.toByteArray();
    }

    /**
     * Process the command line arguments for the tool
     * 
     * @param args the command line arguments
     * @throws IOException the percolated file processing exception
     */
    @Override
    public void processArguments(String[] args) throws IOException {
        List<File> classFiles = new ArrayList<File>();
        List<File> jarFiles = new ArrayList<File>();
        String[] fileArgs = null;
        
       

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--debug") || args[i].equals("-d")) {
                setDebug(true);
            } else if (args[i].equalsIgnoreCase("--config")) {
                // TODO: Handle config file parsing
                File configFile = new File(args[++i]);
                System.out.println("Config file not currently supported" + configFile);
            } else if (args[i].equalsIgnoreCase("--ffdc")) {
                addFfdc = true;
            } else if (args[i].equalsIgnoreCase("--static")) {
                injectStatic = true;
            } else if (args[i].equalsIgnoreCase("--liberty")) {
                defaultTraceType = TraceType.LIBERTY;
            } else if (args[i].equalsIgnoreCase("--tr")) {
                defaultTraceType = TraceType.TR;
            } else if (args[i].equalsIgnoreCase("--java-logging")) {
                defaultTraceType = TraceType.JAVA_LOGGING;
            } else {
                fileArgs = new String[args.length - i];
                System.arraycopy(args, i, fileArgs, 0, fileArgs.length);
                break;
            }
        }

        if (fileArgs == null || fileArgs.length == 0) {
            throw new IllegalArgumentException("No file specified");
        }

        // Add jar files, zip files, and class files to the appropriate collections
        for (int i = 0; i < fileArgs.length; i++) {
            File f = new File(fileArgs[i]);
            if (!f.exists()) {
                throw new IllegalArgumentException("File \"" + f + "\" does not exist");
            } else if (f.isDirectory()) {
                classFiles.addAll(getClassFiles(f, null));
                jarFiles.addAll(getJarFiles(f, null));
            } else if (f.getName().endsWith(".class")) {
                classFiles.add(f);
            } else if (f.getName().endsWith(".jar")) {
                jarFiles.add(f);
            } else if (f.getName().endsWith(".zip")) {
                jarFiles.add(f);
            } else {
                System.err.println(f + " is an unexpected file type; ignoring");
            }
        }

        setClassFiles(classFiles);
        setJarFiles(jarFiles);
    }

    /**
     * Display some very (unhelpful) text to the user when no command line
     * arguments have been provided.
     */
    private static void printUsageMessage() {
        System.err.println("Descrption:");
        System.err.println("");
        System.err.println("Required arguments:");
        System.err.println("  The paths to one or more binary classes, jars, or");
        System.err.println("  directories to scan for classes and jars are required");
        System.err.println("  parameters.");
        System.err.println("");
        System.err.println("  Class files must have a .class extension.");
        System.err.println("  Jar files must have a .jar or a .zip extension.");
        System.err.println("  Directories are recursively scanned for .class files");
        System.err.println("  to process.");
    }

    /**
     * Main entry point for command line execution.
     * 
     * @param args the program arguments
     * @throws IOException unhandled exceptions
     */
    public static void main(String[] args) throws IOException {

        // Make sure we've got something to do
        if (args == null || args.length == 0) {
            printUsageMessage();
            return;
        }

        LibertyTracePreprocessInstrumentation processor = new LibertyTracePreprocessInstrumentation();
        processor.processArguments(args);
        processor.processPackageInfo();
        processor.executeInstrumentation();
    }
}
