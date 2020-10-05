/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ras.instrument.internal.main;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.internal.NLSConstants;
import com.ibm.ws.ras.instrument.internal.bci.LibertyTracingClassAdapter;

/**
 * This class is responsible for instrumenting java8 classes at classload time only.
 * It exists due to a JDK bug between 8000->8005 (fixed in 8005) that caused problems
 * doing a retransformation of 1.8 classes containing specific Java8 features like lambdas
 * (likely due to INVOKEDYNAMIC.) In lieu of doing  an up-front (at classload) transform of all
 * classes, instead a flip-flop is made where LibertyRuntimeTransformer does *everything* for
 * compatible JDKs, otherwise it doesn't touch 1.8 classes and instead this class
 * does the work up front at transform time, for 1.8 only.
 * 
 * Consequently, I don't think we need to respond to trace component changes here,
 * since we're all 1.8 classes regardless here.
 */
public class LibertyJava8WorkaroundRuntimeTransformer implements ClassFileTransformer {

    /**
     * TraceComponent for this class. This is required for debug.
     */
    private final static TraceComponent tc = Tr.register(LibertyJava8WorkaroundRuntimeTransformer.class, NLSConstants.GROUP, NLSConstants.LOGGING_NLS);

    /**
     * Indication that the host is an IBM VM.
     */
    private final static boolean isIBMVirtualMachine = System.getProperty("java.vm.name", "unknown").contains("IBM J9");

	/**
	 * Trace instrumentation force. Due to performance concerns with up-front instrumentation of all 1.8 bytecode classes,
	 * the decision was made to for now only enable diagnostic instrumentation when a bootstrap.properties variable is set
	 * to signal that they should be transformed up front.
	*/
	private static final boolean isJava8TraceEnabled = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run()
        {
			Boolean prop = Boolean.getBoolean("com.ibm.ws.ras.instrument.instrumentJava8Trace");
            return (prop == null ? false : prop.booleanValue());
        }
    });
	
    /** Issue detailed entry/exit trace for class transforms if this is true. */
    private static final boolean detailedTransformTrace = Boolean.getBoolean("com.ibm.ws.logging.instrumentation.detail.enabled");

    /**
     * The {@link java.lang.instrument.Instrumentation} reference obtained from
     * the OSGi service registry by the RAS component.
     */
    private static Instrumentation instrumentation;

    /**
     * The singleton instance of this class that has been registered with
     * the transformer.
     */
    private static LibertyJava8WorkaroundRuntimeTransformer registeredTransformer = null;
    
    /**
     * Flag that indicates we should attempt to work around an emma
     * instrumentation issue by removing the bad local variable table
     * that was left behind.
     */
    private static boolean skipDebugData = false;
    
    private static final Boolean isJDK8WithHotReplaceBug = checkJDK8WithHotReplaceBug() ? Boolean.TRUE :  Boolean.FALSE;
    
    protected static Boolean checkJDK8WithHotReplaceBug() {
    	if (isIBMVirtualMachine) {
		
            String runtimeVersion = System.getProperty("java.runtime.version", "unknown-00000000_0000");

            
            //This is largely replicated from the JavaInfo class. The problem is this class gets packed into the 
            //wlp-rasInstrumentation.jar, so it's easier to replicate this than further change the packaging for now.
            //Please keep the definitive version of this in JavaInfo.
            
            // Parse MAJOR and MINOR versions
            String specVersion = System.getProperty("java.specification.version");
            String[] versions = specVersion.split("[^0-9]"); // split on non-numeric chars
            // Offset for 1.MAJOR.MINOR vs. MAJOR.MINOR version syntax
            
            int offset = "1".equals(versions[0]) ? 1 : 0;
            if (versions.length <= offset)
                return false; //If something goes badly wrong, don't use the workaround. 
            
            int MAJOR = Integer.parseInt(versions[offset]);
            int MINOR = versions.length < (2 + offset) ? 0 : Integer.parseInt(versions[(1 + offset)]);
            //SR and FP need to be parsed manually for a string like 2017111111_01(SR5 FP5) 
			
			int SR = 0;
			int FP = 0;
            int srloc = runtimeVersion.toLowerCase().indexOf("sr");
            if (srloc > (-1)) {
                srloc += 2;
                if (srloc < runtimeVersion.length()) {
                    int len = 0;
                    while ((srloc + len < runtimeVersion.length()) && Character.isDigit(runtimeVersion.charAt(srloc + len))) {
                        len++;
                   }
                    SR = Integer.parseInt(runtimeVersion.substring(srloc, srloc + len));
                }
            }
            
			int fploc = runtimeVersion.toLowerCase().indexOf("fp");
            if (fploc > (-1)) {
                fploc += 2;
                if (fploc < runtimeVersion.length()) {
                    int len = 0;
                    while ((fploc + len < runtimeVersion.length()) && Character.isDigit(runtimeVersion.charAt(fploc + len))) {
                        len++;
                   }
                    FP = Integer.parseInt(runtimeVersion.substring(fploc, fploc + len));
                }
            }
			
            //For IBM JDK 80 SR5 FP5 and lower, we need to use a workaround and property activated injection mechanism.
            //Workaround if we're at 8.0 AND we're either SR0,1,2,3,4 OR we're at SR5 FP1,2,3,4. ONLY 8.0 SR5 FP5 or higher FP OR 8.0 SR6 or higher regardless of FP.
			if ((MAJOR==8) && (MINOR==0) && ((SR<5) || ((SR==5) && (FP<5)) )) {
            	return true;
            }

        }
        
        //Otherwise don't use a workaround.
        return false;
    }

    /**
     * Set the {@link java.lang.instrument.Instrumenation} instance to use for
     * for trace injection.
     *
     * @param inst the {@code Instrumentation} reference obtained by RAS
     */
    public static synchronized void setInstrumentation(Instrumentation inst) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setInstrumentation", inst);

        instrumentation = inst;

        if (instrumentation == null) {
        } else if (isJava8TraceEnabled) {
        	setInjectAtTransform(true); //We always inject at transform in this workaround transformer. Either all or nothing here.
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setInstrumentation");
    }

    /**
     * Indicate whether or not to aggressively inject trace at class definition.
     *
     * @param injectAtTransform true if classes should be transformed at definition
     */
    protected static void setInjectAtTransform(boolean injectAtTransform) {
        if (injectAtTransform) {
            addTransformer();
        }
    }

    /**
     * Indicate whether or not class debug data should be preserved in the
     * transform.
     */
    protected static void setSkipDebugData(boolean skipDebugData) {
        LibertyJava8WorkaroundRuntimeTransformer.skipDebugData = skipDebugData;
    }

    /**
     * Add the RAS class transformer to the
     * instrumentation {@link java.lang.instrument.ClassFileTransformer} list.
     */
    private static synchronized void addTransformer() {
        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.entry(tc, "addTransformer");

        if (registeredTransformer == null && instrumentation != null) {
            registeredTransformer = new LibertyJava8WorkaroundRuntimeTransformer();
            instrumentation.addTransformer(registeredTransformer, false);
        }

        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.exit(tc, "addTransformer");
    }

    /**
     * Determines whether or not class bytes can be transformed.
     *
     * @param bytes the class bytes
     * @return true if the class can be transformed
     */
    private static boolean isTransformPossible(byte[] bytes) {
        if (bytes.length < 8) {
            return false;
        }
		
        // The transform method will be called for all classes, but ASM is only
        // capable of processing some class file format versions.  That's ok
        // because the transformer only modifies classes that have been
        // preprocessed by our build anyway.
        //
        // ASM doesn't provide a way to determine its max supported version, so
        // we hard code the supported version number.
        int classFileVersion = ((bytes[6] & 0xff) << 8) | (bytes[7] & 0xff);
        
        //Limit bytecode that we transform based on JDK retransform compatibility
        //If we have issues here, 1.8 classes will be instead handled by a separate
        //transformer that only does those classes.
        if (isJDK8WithHotReplaceBug)
        	return classFileVersion == Opcodes.V1_8;
        else
        	return false; //Don't do anything except 1.8 classes on specific JDKs
    }


    public static byte[] transform(byte[] bytes) throws IOException {
    	return transform(bytes, true);
    }
    
    /**
     * Instrument the class at the current position in the specified input stream.
     *
     * @return instrumented class file or null if the class has already
     *         been instrumented.
     *
     * @throws IOException if an error is encountered while reading from
     *             the <code>InputStream</code>
     */
    public static byte[] transform(byte[] bytes, boolean skipIfNotPreprocessed) throws IOException {
		if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.entry(tc, "transform");
        
        
        
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);

        StringWriter sw = null;
        ClassVisitor visitor = writer;
        if (tc.isDumpEnabled()) {
            sw = new StringWriter();
            visitor = new CheckClassAdapter(visitor, false);
            visitor = new TraceClassVisitor(visitor, new PrintWriter(sw));
        }

        LibertyTracingClassAdapter tracingClassAdapter = new LibertyTracingClassAdapter(visitor, skipIfNotPreprocessed);
        try {
            // Class reader must maintain all metadata information that's present in
            // the class
            reader.accept(tracingClassAdapter, skipDebugData ? ClassReader.SKIP_DEBUG : 0);
        } catch (Throwable t) {
            IOException ioe = new IOException("Unable to instrument class stream with trace: " + t.getMessage(), t);
            throw ioe;
        }

        // Provide a whole lot of detailed information on the resulting class
        if (detailedTransformTrace && tc.isDumpEnabled() && tracingClassAdapter.isClassModified()) {
            Tr.dump(tc, "Transformed class", sw);
        }

        // Try to short circuit when the class didn't change
        byte[] result = tracingClassAdapter.isClassModified() ? writer.toByteArray() : null;
        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.exit(tc, "transform", result);
        return result;
    }

    /**
     * {@inheritDoc} <p>
     * This method will only executes a transformation always, provided
	 * the particular conditions we're looking for are true. Namely the JDK level and
	 * custom property to turn on Java8 instrumentation.
     */
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
     	if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.entry(this, tc, "transform", loader, className, classBeingRedefined, protectionDomain);

		    	
		if (!isJava8TraceEnabled)
			return null; //Look for special trace instrumentation force until JDK bug fully fixed.
		
		if (classBeingRedefined != null)
        	return null;

        byte[] newClassBytes = null;
        if (isTransformPossible(classfileBuffer)) {
            try {
                newClassBytes = transform(classfileBuffer);
            } catch (Throwable t) {
                Tr.error(tc, "INSTRUMENTATION_TRANSFORM_FAILED_FOR_CLASS_2", className, t);
            }
        }

        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.exit(this, tc, "transform", newClassBytes);
        return newClassBytes;
    }
}
