/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.request.probe.bci.internal.RequestProbeBCIManagerImpl;
import com.ibm.wsspi.logging.IntrospectableService;
import com.ibm.wsspi.probeExtension.ProbeExtension;
import com.ibm.wsspi.request.probe.bci.RequestProbeTransformDescriptor;
import com.ibm.wsspi.requestContext.RequestContext;

/**
 * Generate a full report of all registered transform descriptors and probe extensions and active requests.
 */
 @SuppressWarnings("deprecation")
public class RequestProbeIntrospector implements IntrospectableService {

    private static final TraceComponent tc = Tr.register(RequestProbeIntrospector.class);
    private static final int EXTRA_SPACE_REQUIRED = 2;
    
   
    /**
     * Declarative services activation callback.
     */
    protected void activate() {}

    /**
     * Declarative services deactivation callback.
     */
    protected void deactivate() {}

    
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "RequestProbeIntrospector";
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Information about the active requests, registered probe extensions and transform descriptors";
    }

    /** {@inheritDoc} */
    @Override
    public void introspect(OutputStream out) throws IOException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "introspect");

        PrintWriter writer = new PrintWriter(out, true);

        activeRequestIntropectors(writer); // Log details of active requests
        probeExtensionIntrospectors(writer); // Log details of registered probe extensions
        transformDescriptorIntrospectors(writer); // Log details of registered transform descriptors
        writer.flush();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "introspect");
    }
    
    /**
     * This method will dump all the active request, thread id and their duration in tabular format. In addition will dump the RequestContext
     * @param writer
     */
    private void activeRequestIntropectors(PrintWriter writer) {
		
         writer.println("\n------------------------------------------------------------------------------");
         writer.println("              Active Requests");
         writer.println("------------------------------------------------------------------------------\n");
         List<RequestContext> activeRequests = RequestProbeService.getActiveRequests();
         if(activeRequests == null || activeRequests.size() ==  0) { // If no active request available.. 
          	 writer.println("----- No active requests ----- ");
          } else {
        	   	
        		int maxDurationLength = 0;
        	   	List<String> activeRequestDetails = new ArrayList<String>();
	           	for(RequestContext requestContext: activeRequests) {
	           		double totalRequestDuration =  (System.nanoTime() - requestContext.getRootEvent().getStartTime()) / 1000000.0;
	           		
	           		String totalRequestDurationStr = String.format("%.3f", totalRequestDuration); 
	           		totalRequestDurationStr = totalRequestDurationStr + "ms";
	           		
	           		if(requestContext.getRequestId().toString().length() > maxDurationLength) {
	           			maxDurationLength = requestContext.getRequestId().toString().length();  
	           		}
					String threadId = DataFormatHelper.padHexString((int) requestContext.getThreadId(), 8);
	           		activeRequestDetails.add(requestContext.getRequestId().toString()+ "," +threadId+ "," +totalRequestDurationStr);
	           	}
	           	
	           	writer.println(String.format("%-" + (maxDurationLength + EXTRA_SPACE_REQUIRED) + "s" + "%-10s%s","Request", "Thread", "Duration"));
	           	
	           	for(String request : activeRequestDetails) {
	           		String requestId = request.split(",")[0];
					String threadId = request.split(",")[1];
	           		String totalDuration = request.split(",")[2];
	           		writer.println(String.format("%-"+ (maxDurationLength + EXTRA_SPACE_REQUIRED) + "s"	+ "%-10s%s", requestId, threadId, totalDuration));
	           	}
	           	
	           	writer.println("------------------------------------------------------------------------------\n");
	           	for(RequestContext requestContext: activeRequests) { // Print the RequestContext for all the active requests.
	               	
	                   writer.println("Request " + requestContext.getRequestId().toString());
	                   writer.println(RequestContext.dumpTree(requestContext.getRootEvent(), true));
	                   writer.println();
	           	}
           }
         
	}

    /**
     * This method will dump all registered transform descriptors. It includes className, methodName and description
     * @param writer
     */
	private void transformDescriptorIntrospectors(PrintWriter writer) {
		
    	
    	Map<String, RequestProbeTransformDescriptor> registeredTransformDescriptors =  RequestProbeBCIManagerImpl.getRequestProbeTransformDescriptors();
        List<String> transformDescriptorRefs = new ArrayList<String>() {{add("Transform Descriptor");  add("");}};
        List<String> tdDetails = new ArrayList<String>() {{add("ClassName.MethodName(Description)");  add("");}};
        List<Integer> indentationLength = new ArrayList<Integer>();
        int maxSpaceRequired = 0;
       
        for(Entry<String, RequestProbeTransformDescriptor> transformDescriptor : registeredTransformDescriptors.entrySet()) {
        	
        	transformDescriptorRefs.add(transformDescriptor.getValue().toString());
        	if(transformDescriptor.getValue().getMethodDesc().equals("all"))
        		 tdDetails.add(transformDescriptor.getValue().getClassName() + "." + transformDescriptor.getValue().getMethodName() + "("+ transformDescriptor.getValue().getMethodDesc() + ")" );
        	else
        		 tdDetails.add(transformDescriptor.getValue().getClassName() + "." + transformDescriptor.getValue().getMethodName() +  transformDescriptor.getValue().getMethodDesc() );
        }
        
        for(String transformDescriptorRef : transformDescriptorRefs) {
        	if(transformDescriptorRef.length() > maxSpaceRequired) {
        		maxSpaceRequired = transformDescriptorRef.length();
        	}
        }
        indentationLength.add(maxSpaceRequired+ EXTRA_SPACE_REQUIRED);

        writer.println();
        writer.println("------------------------------------------------------------------------------");
        writer.println("              Registered Transform Descriptors ");
        writer.println("------------------------------------------------------------------------------");
        
        for(int i = 0 ; i < transformDescriptorRefs.size(); i ++) {
        	writer.println(String.format("%-" + indentationLength.get(0) + "s%s",transformDescriptorRefs.get(i) ,tdDetails.get(i)));
        }
        
        if(registeredTransformDescriptors.size() ==  0) {
          	 writer.println("----- No transform descriptors are registered ----- ");
           }

	}

	/**
     * This method will dump all the registered probe extensions with their configuration details (SampleRate, Context information required, Invoke only for root, Entry enabled, Exit enabled, Include types)
     * @param writer
     */
	private void probeExtensionIntrospectors(PrintWriter writer) {
    	 
    	 List<ProbeExtension> registeredProbeExtensions = RequestProbeService.getProbeExtensions();
         List<String> probeExtensionRefList = new ArrayList<String>() {{add("Probe extension"); add("");  add("");}};
         List<String> sampleRateList = new ArrayList<String>() {{add("Sample"); add("Rate"); add("");}};
         List<String> contextInfoRequiredList = new ArrayList<String>() { { add("ContextInfo"); add("required"); add(""); }};
         List<String> invokeOnyForRootList = new ArrayList<String>() {{add("Invoke only"); add("for root"); add(""); }};
         List<String> entryEnabledList = new ArrayList<String>( ) {{ add("Entry"); add("enabled"); add("");  }};
         List<String> exitEnabledList = new ArrayList<String>() {{ add("Exit"); add("enabled"); add(""); }};
         List<String> includeTypeList = new ArrayList<String>() {{ add("Include Types"); add(""); add("");}};
         List<Integer> indentationLength = new ArrayList<Integer>();
         int maxSpaceRequired = 0;
        
         
         for(ProbeExtension probeExtension : registeredProbeExtensions) {
         	
         	probeExtensionRefList.add(probeExtension.toString());
         	sampleRateList.add(""+probeExtension.getRequestSampleRate());
         	invokeOnyForRootList.add(probeExtension.invokeForRootEventsOnly() ? "True" : "False");
         	entryEnabledList.add(probeExtension.invokeForEventEntry() ? "True" : "False");
         	exitEnabledList.add(probeExtension.invokeForEventExit() ? "True" : "False");
         	
         	if(probeExtension.getContextInfoRequirement() == 0) {
         		contextInfoRequiredList.add("ALL_EVENTS");
         	} else if(probeExtension.getContextInfoRequirement() == 1) {
         		contextInfoRequiredList.add("EVENTS_MATCHING_SPECIFIED_EVENT_TYPES");
         	} else {
         		contextInfoRequiredList.add("NONE");
         	}
         	
         	StringBuilder eventTypes= new StringBuilder();
         	String eventTypeStr = "";
         	
         	if(probeExtension.invokeForEventTypes() == null) {
         		eventTypeStr = "ALL";
         	} else {
         		
         		for(String eventType: probeExtension.invokeForEventTypes()) {
         			eventTypes.append(eventType + ",");
         		}
         		eventTypeStr = eventTypes.toString().substring(0, eventTypes.length() - 1);
         	}
         	includeTypeList.add(eventTypeStr);
         	
         }
         
         // Find the indentation required for Probe Extension Reference.. 
         for(String probeExtensionRef : probeExtensionRefList) {
         	
         	if(probeExtensionRef.length() > maxSpaceRequired) {
         		maxSpaceRequired = probeExtensionRef.length();
         	}
         }
         indentationLength.add(maxSpaceRequired + EXTRA_SPACE_REQUIRED);
         
         // Find the indentation required for Sample Rate .. 
         maxSpaceRequired = 0;
         for(String sampleRate : sampleRateList) {
         	if(sampleRate.length() > maxSpaceRequired) {
         		maxSpaceRequired = sampleRate.length();
         	}
         }
         indentationLength.add(maxSpaceRequired + EXTRA_SPACE_REQUIRED);
         
         // Find the indentation required for context info required.. 
         maxSpaceRequired = 0;
         for(String contextInfoRequired : contextInfoRequiredList) {
         	if(contextInfoRequired.length() > maxSpaceRequired) {
         		maxSpaceRequired = contextInfoRequired.length();
         	}
         }
         indentationLength.add(maxSpaceRequired + EXTRA_SPACE_REQUIRED);
         
         // Find the indentation required for  invokeOnyForRoot.. 
         maxSpaceRequired = 0;
         for(String invokeOnyForRoot : invokeOnyForRootList) {
         	if(invokeOnyForRoot.length() > maxSpaceRequired) {
         		maxSpaceRequired = invokeOnyForRoot.length();
         	}
         }
         indentationLength.add(maxSpaceRequired + EXTRA_SPACE_REQUIRED);
         
         // Find the indentation required for  entryEnabledList..
         maxSpaceRequired = 0;
         for(String entryEnabled : entryEnabledList) {
         	if(entryEnabled.length() > maxSpaceRequired) {
         		maxSpaceRequired = entryEnabled.length();
         	}
         }
         indentationLength.add(maxSpaceRequired + EXTRA_SPACE_REQUIRED);
         
       // Find the indentation required for  exitEnabledList..
         maxSpaceRequired = 0;
         for(String exitEnabled : exitEnabledList) {
         	if(exitEnabled.length() > maxSpaceRequired) {
         		maxSpaceRequired = exitEnabled.length();
         	}
         }
         indentationLength.add(maxSpaceRequired + EXTRA_SPACE_REQUIRED);
         
         writer.println();
         writer.println("------------------------------------------------------------------------------");
         writer.println("              Registered Probe Extensions ");   
         writer.println("------------------------------------------------------------------------------");

        
        for(int i = 0 ; i < probeExtensionRefList.size() ; i ++) {

         	writer.println(
 							String.format(
	 							"%-" + indentationLength.get(0)+ "s%-"+ indentationLength.get(1)+ "s%-" + indentationLength.get(2)+ "s" +
	 							"%-" + indentationLength.get(3)+ "s%-"+ indentationLength.get(4)+ "s%-" + indentationLength.get(5)+ "s%s", 
	 							probeExtensionRefList.get(i) ,
	 							sampleRateList.get(i) ,
	 							contextInfoRequiredList.get(i),
	 							invokeOnyForRootList.get(i),
	 							entryEnabledList.get(i),
	 							exitEnabledList.get(i),
	 							includeTypeList.get(i)
 						    )					
            );
         }
         
        if(registeredProbeExtensions.size() ==  0) { // No active probe extensions available
       	 writer.println("----- No probe extensions are registered ----- ");
        }
		
	}

}
