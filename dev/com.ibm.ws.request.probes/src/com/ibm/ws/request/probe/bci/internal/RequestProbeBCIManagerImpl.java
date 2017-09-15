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

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.request.probe.bci.RequestProbeTransformDescriptor;


public class RequestProbeBCIManagerImpl {

    Instrumentation instrumentation = null;
    RequestProbeTransformer requestProbeTransformer = null;
    
    //Used for internal house keeping.
    Set<String> requestProbeClasses = new HashSet<String>();
    
    private static volatile Map<String, RequestProbeTransformDescriptor> requestProbeTransformDescriptors = 
    		Collections.unmodifiableMap(new HashMap<String, RequestProbeTransformDescriptor>());
    
    public static Map<String, RequestProbeTransformDescriptor> getRequestProbeTransformDescriptors() {
		//return new HashMap<String, RequestProbeTransformDescriptor>(requestProbeTransformDescriptors);
    	return requestProbeTransformDescriptors;
	}

	private static final TraceComponent tc = Tr.register(RequestProbeBCIManagerImpl.class);

    synchronized void activate(BundleContext bundleContext, Map<String, Object> properties) throws Exception {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "activate");
        }
        
        //Handle the case where instrumentation reference is null.
        if(instrumentation == null){
        	Tr.error(tc, "INSTR_INST_NOT_AVAILABLE_ERROR");
        	throw new RuntimeException();
        }


        //STEP 1 Create RequestProbeTransformer (uses ASM internally) and attach it to java instrumentation.
        // This will do transformation at class loading time (Useful when timedoperations-1.0, requestTiming-1.0 is enabled at server start time)
        requestProbeTransformer = new RequestProbeTransformer(instrumentation);
        instrumentation.addTransformer(requestProbeTransformer, true);

        //STEP 2 If this feature is enabled at runtime, we need to take all loaded classes and retransform them
        // This will do transformation for loaded classes (Useful when timedoperations-1.0,requestTiming-1.0 is enabled at runtime)
        requestProbeTransformer.retransformRequestProbeRelatedClasses();
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "activate");
        }

    }

    synchronized void deactivate() throws Exception {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "deactivate");
        }

        //Add Cleanup Code here        
        //STEP 1 : Remove Transformer and cleanup added code.
        instrumentation.removeTransformer(requestProbeTransformer);
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (requestProbeClasses.contains(clazz.getName().replace(".", "/"))) {
                if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                	Tr.debug(tc, "Re-transforming class", clazz);
                }
            	instrumentation.retransformClasses(clazz);
            }
        }
        requestProbeClasses.clear();
        instrumentation = null;
        requestProbeTransformer = null;
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "deactivate");
        }
    }


    
    synchronized protected void setRequestProbeMetaDataProvider(RequestProbeTransformDescriptor transformDescriptor) {
        if (transformDescriptor == null) {
            return;
        }

        if (tc.isEntryEnabled()) {
        	String tdDetails = transformDescriptor.getClassName() + "|" + transformDescriptor.getMethodName() + "|" + transformDescriptor.getMethodDesc() + "|" + transformDescriptor.getEventType();
            Tr.entry(tc, "setRequestProbeMetaDataProvider", tdDetails);
        }

        HashMap<String, RequestProbeTransformDescriptor> transformDescriptors = new HashMap<String, RequestProbeTransformDescriptor>(requestProbeTransformDescriptors);
        String td = (transformDescriptor.getClassName() + transformDescriptor.getMethodName() + transformDescriptor.getMethodDesc()).intern();
        transformDescriptors.put(td, transformDescriptor);
        requestProbeTransformDescriptors = Collections.unmodifiableMap(transformDescriptors);
        //requestProbeTransformDescriptors.put(transformDescriptor.getClassName() + transformDescriptor.getMethodName() + transformDescriptor.getMethodDesc(), transformDescriptor);
        requestProbeClasses.add(transformDescriptor.getClassName());
        
        //Now handle a case for late clients. Dynamic Support
        if (requestProbeTransformer != null) {
            requestProbeTransformer.retransformClass(transformDescriptor.getClassName());
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "setRequestProbeMetaDataProvider");
        }

    }

    synchronized protected void unsetRequestProbeMetaDataProvider(RequestProbeTransformDescriptor transformDescriptor) {
        if (transformDescriptor == null) {
            return;
        }
        if (tc.isEntryEnabled()) {
        	String tdDetails = transformDescriptor.getClassName() + "|" + transformDescriptor.getMethodName() + "|" + transformDescriptor.getMethodDesc() + "|" + transformDescriptor.getEventType();
            Tr.entry(tc, "unsetRequestProbeMetaDataProvider", tdDetails);
        }

        HashMap<String, RequestProbeTransformDescriptor> transformDescriptors = new HashMap<String, RequestProbeTransformDescriptor>(requestProbeTransformDescriptors);
        String td = (transformDescriptor.getClassName()+transformDescriptor.getMethodName()+ transformDescriptor.getMethodDesc()).intern();
        transformDescriptors.remove(td);
        requestProbeTransformDescriptors = Collections.unmodifiableMap(transformDescriptors);;


        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "unsetRequestProbeMetaDataProvider");
        }

    }

    synchronized protected void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    synchronized protected void unsetInstrumentation(Instrumentation instrumentation) {
        if (instrumentation == this.instrumentation) {
            this.instrumentation = null;
        }
    }

}
