/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.request.logging.internal;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.zos.request.logging.UserData;
import com.ibm.ws.zos.core.utils.NativeUtils;
import com.ibm.ws.zos.request.logging.SmfDataRecorder;
import com.ibm.ws.zos.request.logging.ZosRequestLoggingSafService;
import com.ibm.ws.zos.request.logging.data.DataProvider;
import com.ibm.wsspi.probeExtension.ContextInfoRequirement;
import com.ibm.wsspi.probeExtension.ProbeExtension;
import com.ibm.wsspi.request.probe.bci.ContextInfoHelper;
import com.ibm.wsspi.requestContext.Event;
import com.ibm.wsspi.requestContext.RequestContext;

/**
 * ProbeExtension implementation that gathers HTTP request information to write SMF 120 subtype 11 records.
 *
 * Classes such as this implementing the ProbeExtension interface are notified of specific events around
 * method invocations along the request execution path. In this particular case, only entry and exit events
 * are of interest. See invokeForEventTypes() for the list of events this class is interested in.
 *
 * Event type implementations are actual OSGI service classes that represent the event/method. For example:
 * Service: com.ibm.ws.request.probe.zoswlm.EnclaveManagerProcessPostEnclaveDelete represents method:
 * com.ibm.ws.zos.wlm.internal.EnclaveManager.processPostEnclaveDelete().
 * These service classes are called pre and post method invocation by the probe framework to gather and
 * pre-process the input data, which is then passed to ProbeExtension implementations such as this class in
 * a form of an event within the processEntryEvent() and processExitEvent() methods.
 *
 * One important limitation of the probe framework is the thread specific id that is used to associate
 * the methods along the request path. The first method/event encountered on a particular thread becomes the root
 * event for that thread. Every subsequent method/event on that same thread are considered child events. Therefore,
 * if events/methods are driven on different threads or the request starts another root event sequence on
 * the same thread, the request id (used to tie a synchronous execution together) will vary.
 *
 * This request id variation makes this framework not too friendly for asynchronous executions.
 * To handle asynchronous work (when using WLM) and still be able to associate all interested methods
 * along the entire asynchronous circuit, this implementation relies on request IDs and the enclave IDs
 * because enclave IDs remain constant through enclave.join/leave operations when processing asynchronous
 * requests.
 *
 * IMPORTANT: In order for asynchronous request data to be tracked properly when WLM is enabled and when
 * using the servlet-3.0 feature, users MUST have the following webcontainer property set in the server
 * configuration file: <webContainer transfercontextinasyncservletrequest="true"/>
 * If the property is not set, data such as the response bytes maybe missing in the logged SMF record.
 * The mentioned property was introduced for proper (web container) context propagation and it also fixes
 * issues with context propagation across nested dispatches.
 *
 * NOTE: Asynchronous requests without WLM are not supported at the moment. If one is executed without WLM,
 * at the very minimum, the recorded request will not contain the servlet response because it is
 * processed on a thread that is different from the thread that started processing the request. For example:
 * T1: Wrapper -> Service -> service.exit -> wrapper.exit (record is written to WLM)
 * T2: logFinalResponse (captures servlet response data)
 */
@Component(service = { ProbeExtension.class }, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class SmfProbeExtensionImpl implements ProbeExtension {

    private static final TraceComponent tc = Tr.register(SmfProbeExtensionImpl.class);

    /** Map of request Id keys associated to RequestMap objects. */
    private ConcurrentHashMap<String, Map<String, Object>> requestMap = new ConcurrentHashMap<String, Map<String, Object>>();;

    /** Map of enclave token keys associated to RequestMap objects. */
    private ConcurrentHashMap<String, Map<String, Object>> enclaveRequestMap = new ConcurrentHashMap<String, Map<String, Object>>();

    /** Map of data providers. */
    private final ConcurrentHashMap<String, DataProvider> dataProviderMap = new ConcurrentHashMap<String, DataProvider>();

    /** SMF data recorder reference. */
    private SmfDataRecorder smfRecorder;

    /** Native Utility object reference. */
    private NativeUtils nativeUtils;

    /** RequestLoggingSafService object reference. */
    private ZosRequestLoggingSafService zosRequestLoggingSafService;

    /** UserData object reference. */
    private UserData userData;

    /** Provider name we are interested in: EnclaveManager. the name is as described in the respective DataProvider implementation */
    private final String ENCLAVE_MGR_PROVIDER_NAME = "com.ibm.ws.zos.wlm.EnclaveManager";

    /** Constant that points to the array list holding user data. */
    private final String USER_DATA_LIST = "apiUserDataList";

    /** com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink.wrapHandlerAndExecute() */
    private final String HTTP_DISPATCHER_WRAP_HANDLER_AND_EXECUTE = "websphere.http.wrapHandlerAndExecute";

    /** com.ibm.ws.zos.wlm.internal.WlmClassification.wlmRunWork() */
    private final String WLM_CLASSIFICATION_WLM_RUN_WORK = "websphere.wlm.WlmClassification.wlmRunWork";

    /** com.ibm.ws.webcontainer.servlet.ServletWrapper.service() */
    private final String WEB_SERVLET_SERICE = "websphere.servlet.service";

    /** com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl.logFinalResponse() */
    private final String HTTP_INBOUND_CTX_LOG_FINAL_RESP = "websphere.http.logFinalResponse";

    /** com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl.processPostEnclaveDelete() */
    private final String WLM_ENCLAVE_MGR_POST_ENCLAVE_DELETE = "websphere.wlm.EnclaveManagerImpl.processPostEnclaveDelete";

    /**
     * DS method to activate this component.
     *
     * @param properties : Map containing service properties
     */
    @Activate
    protected void activate(Map<String, Object> configuration) {
    }

    /**
     * DS method to deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Request map size: " + requestMap.size() + ". Request map: " + requestMap);
            Tr.debug(tc, "Enclave request map size: " + enclaveRequestMap.size() + ". Enclave request map: " + enclaveRequestMap);
        }

        enclaveRequestMap = null;
        requestMap = null;
    }

    /**
     * Sets the SmfDataRecorder object reference.
     *
     * @param smfRecorder The SmfDataRecorder object reference.
     */
    @Reference(service = SmfDataRecorder.class, cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    protected void setSmfDataRecorder(SmfDataRecorder smfRecorder) {
        this.smfRecorder = smfRecorder;
    }

    /**
     * Unsets the SmfDataRecorder object reference.
     *
     * @param smfRecorder The SmfDataRecorder object reference.
     */
    protected void unsetSmfDataRecorder(SmfDataRecorder smfRecorder) {
        if (this.smfRecorder == smfRecorder) {
            this.smfRecorder = null;
        }
    }

    /**
     * Sets the NativeUtils object reference.
     *
     * @param nativeUtils The NativeUtils object reference.
     */
    @Reference(service = NativeUtils.class, cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    protected void setNativeUtils(NativeUtils nativeUtils) {
        this.nativeUtils = nativeUtils;
    }

    /**
     * Unsets the NativeUtils object reference.
     *
     * @param nativeUtils The NativeUtils object reference.
     */
    protected void unsetNativeUtils(NativeUtils nativeUtils) {
        if (this.nativeUtils == nativeUtils) {
            this.nativeUtils = null;
        }
    }

    /**
     * Sets the UserData object reference.
     *
     * @param nativeUtils The NativeUtils object reference.
     */
    @Reference(service = UserData.class, cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    protected void setUserData(UserData userData) {
        this.userData = userData;
    }

    /**
     * Unsets the NativeUtils object reference.
     *
     * @param nativeUtils The NativeUtils object reference.
     */
    protected void unsetUserData(UserData userData) {
        if (this.userData == userData) {
            this.userData = null;
        }
    }

    /**
     * Sets the EnclaveManager object reference. Currently a single provider is expected.
     *
     * @param enclaveManager The EnclaveManager object reference.
     */
    @Reference(service = DataProvider.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setDataProvider(DataProvider dataProvider) {
        dataProviderMap.put(dataProvider.getProviderName(), dataProvider);
    }

    /**
     * Unsets the EnclaveManager object reference.
     *
     * @param enclaveManager The EnclaveManager object reference.
     */
    protected void unsetDataProvider(DataProvider dataProvider) {
        dataProviderMap.remove(dataProvider.getProviderName());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Data provider map size: " + dataProviderMap.size() + ". Data provider map: " + dataProviderMap);
        }
    }

    /**
     * Sets the ZosRequestLoggingSafService object reference.
     *
     * @param safService The ZosRequestLoggingSafService object reference.
     */
    @Reference(service = ZosRequestLoggingSafService.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setZosRequestLoggingSafService(ZosRequestLoggingSafService safService) {
        this.zosRequestLoggingSafService = safService;
    }

    /**
     * Unsets the ZosRequestLoggingSafService object reference.
     *
     * @param safService The ZosRequestLoggingSafService object reference.
     */
    protected void unsetZosRequestLoggingSafService(ZosRequestLoggingSafService safService) {
        if (this.zosRequestLoggingSafService == safService) {
            this.zosRequestLoggingSafService = null;
        }
    }

    /**
     * Returns the mapped LDAP name of the user making the request.
     *
     * @return The mapped LDAP user name.
     */
    public String getMappedUserName() {
        String mvsUserId = null;
        if (zosRequestLoggingSafService != null) {
            mvsUserId = zosRequestLoggingSafService.getMappedUserName();
        }

        return mvsUserId;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> invokeForEventTypes() {
        // Return the list of events/methods we are interested in.
        List<String> eventTypes = new ArrayList<String>();
        eventTypes.add(HTTP_DISPATCHER_WRAP_HANDLER_AND_EXECUTE);
        eventTypes.add(WLM_CLASSIFICATION_WLM_RUN_WORK);
        eventTypes.add(WEB_SERVLET_SERICE);
        eventTypes.add(HTTP_INBOUND_CTX_LOG_FINAL_RESP);
        eventTypes.add(WLM_ENCLAVE_MGR_POST_ENCLAVE_DELETE);
        return eventTypes;
    }

    /** {@inheritDoc} */
    @Override
    public int getRequestSampleRate() {
        // Return 1. The number indicates every how many requests we want to be called.
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public int getContextInfoRequirement() {
        // Get the context information just for the events/methods we are interested in.
        return ContextInfoRequirement.EVENTS_MATCHING_SPECIFIED_EVENT_TYPES;
    }

    /** {@inheritDoc} */
    @Override
    public boolean invokeForRootEventsOnly() {
        // We want to be called for all root and child events.
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean invokeForCounter() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void processCounter(Event event) {
        // This method should not get called. invokeForCounter() returned false.
    }

    /** {@inheritDoc} */
    @Override
    public boolean invokeForEventEntry() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean invokeForEventExit() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void processEntryEvent(Event event, RequestContext requestContext) {
        try {
            String eventType = event.getType();
            String requestIdString = requestContext.getRequestId().getId();

            // Execution order 1 (parent). Gather basic Id information.
            if (HTTP_DISPATCHER_WRAP_HANDLER_AND_EXECUTE.equals(eventType)) {
                ((UserDataImpl) userData).clearUserData();
                Map<String, Object> requestData = new HashMap<String, Object>();
                if (requestData != null) {
                    requestData.put(SmfDataRecorder.TIMEUSED_START, nativeUtils.getTimeusedData());
                    requestData.put(SmfDataRecorder.START_STCK, Long.valueOf(nativeUtils.getSTCK()));
                    requestData.put(SmfDataRecorder.THREAD_ID, Long.valueOf(requestContext.getThreadId()));
                    requestData.put(SmfDataRecorder.REQUEST_ID, requestIdString);
                    requestData.put(USER_DATA_LIST, Collections.synchronizedList(new ArrayList<HashMap<Integer, byte[]>>()));
                    requestMap.put(requestIdString, requestData);
                }
                return;
            }

            // Execution order 2 (child). Gather basic WLM data and save it in the enclave map. Called only when the WLM feature enabled.
            if (WLM_CLASSIFICATION_WLM_RUN_WORK.equals(eventType)) {
                Map<String, Object> requestData = requestMap.get(requestIdString);
                if (requestData != null) {
                    HashMap<String, Object> wlmData = getEventContextInfo(event);
                    if (wlmData != null) {
                        requestData.put(SmfDataRecorderImpl.WLM_DATA_HOST, wlmData.get(SmfDataRecorderImpl.WLM_DATA_HOST));
                        requestData.put(SmfDataRecorderImpl.WLM_DATA_PORT, wlmData.get(SmfDataRecorderImpl.WLM_DATA_PORT));
                        requestData.put(SmfDataRecorderImpl.WLM_DATA_URI, wlmData.get(SmfDataRecorderImpl.WLM_DATA_URI));
                        requestData.put(SmfDataRecorderImpl.WLM_DATA_TRAN_CLASS, wlmData.get(SmfDataRecorderImpl.WLM_DATA_TRAN_CLASS));
                        requestData.put(SmfDataRecorderImpl.WLM_DATA_ARRIVAL_TIME, wlmData.get(SmfDataRecorderImpl.WLM_DATA_ARRIVAL_TIME));
                        enclaveRequestMap.put((String) wlmData.get(SmfDataRecorderImpl.WLM_DATA_ENCLAVE_TOKEN), requestData);
                    }
                }
                return;
            }

            // Execution order 3 (child): The servlet method is about to be called. Gather servlet request information.
            // If the request is part of a nested set of servlet calls (i.e. servlet forward), this event will
            // be driven once per subsequent servlet requests.
            if (WEB_SERVLET_SERICE.equals(eventType)) {
                Map<String, Object> requestData = requestMap.get(requestIdString);
                if (requestData != null) {
                    // If this event is driven multiple times, capture the request information only once.
                    if (!requestData.containsKey(SmfDataRecorderImpl.REMOTE_ADDR)) {
                        getHTTPReqtInfo(requestData, event);
                    }
                }
                return;
            }
        } catch (Exception e) {
            // No action needed. An FFDC will be issued.
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processExitEvent(Event event, RequestContext requestContext) {
        try {
            String eventType = event.getType();
            String requestIdString = requestContext.getRequestId().getId();
            Map<String, Object> requestData = requestMap.get(requestIdString);

            // Execution order 1. The servlet method was called. Gather time and user data.
            // If the request is part of a nested set of servlet calls (i.e. servlet forward), this event will
            // be driven once per subsequent servlet requests.
            if (WEB_SERVLET_SERICE.equals(eventType)) {
                processServletServiceExitEvent(requestData);
                return;
            }
            // Execution order *. Sync part of request ended. Write record if WLM feature not enabled. Cleanup user data.
            if (HTTP_DISPATCHER_WRAP_HANDLER_AND_EXECUTE.equals(eventType)) {
                processWrapHandlerAndExecExitEvent(requestData, requestIdString);
                return;
            }
            // Execution order *. Gathers the request's response data.
            if (HTTP_INBOUND_CTX_LOG_FINAL_RESP.equals(eventType)) {
                processLogFinalResponseExitEvent(requestData, event);
                return;
            }
            // Execution order *. Gather delete data. Write record when WLM feature enabled.
            if (WLM_ENCLAVE_MGR_POST_ENCLAVE_DELETE.equals(eventType)) {
                processPostEnclaveDeleteExitEvent(event);
                return;
            }
        } catch (Exception e) {
            // No action needed. An FFDC will be issued.
        }
    }

    /**
     * Processes a servlet service exit event.
     * NOTE: If the request is part of a nested set of servlet calls (i.e. servlet forward),
     * this event will be driven once per subsequent servlet requests.
     *
     * @param requestData The current request data.
     */
    public void processServletServiceExitEvent(Map<String, Object> requestData) {
        if (requestData != null) {
            // If request data is found, we are processing a synchronous request or the
            // 'synchronous' part of an asynchronous request before it goes async.
            //
            // When processing nested sync requests without WLM, the data below is captured
            // multiple times when the servlet requests unwinds. Although, the end
            // result is accurate, it would make sense to capture the data in
            // processWrapHandlerAndExecExitEvent. However, that would affect the synchronous
            // case with WLM enabled because the captured data is written to WLM by
            // processPostEnclaveDeleteExitEvent, which runs prior to the wrapperHandler
            // exit event where this information would be obtained.

            // Capture thread information. When processing async requests, this data may not
            // be accurate as it is thread specific. Until async requests are fully supported,
            // capture information for the 'original' thread before the request goes truly async.
            requestData.put(SmfDataRecorder.THREAD_INFO, nativeUtils.getSmfData());

            // Capture CPU time. This information is thread specific. It is mainly geared for
            // synchronous requests when WLM not enabled. When processing async requests with
            // WLM enabled, the CPU time is captured 'accurately' from the enclave when it
            // is deleted (WLM_DATA_DELETE_DATA) at the end of the async request.
            requestData.put(SmfDataRecorder.TIMEUSED_END, nativeUtils.getTimeusedData());

            // Capture the request end time if WLM is not enabled. When processing async requests
            // with WLM enabled, the request end time is captured 'accurately' during the
            // enclave delete event.
            if (!requestData.containsKey(SmfDataRecorder.WLM_DATA_ARRIVAL_TIME)) {
                requestData.put(SmfDataRecorder.END_STCK, Long.valueOf(nativeUtils.getSTCK()));
            }
        } else {
            // If we could not find the requestData, we are most likely on a different thread (i.e. nested dispatch request).
            // Use the current enclave on the thread to find the requestData map.
            DataProvider dataProvider = dataProviderMap.get(ENCLAVE_MGR_PROVIDER_NAME);
            if (dataProvider != null) {
                String currentEnclaveToken = dataProvider.getDataString();
                if (currentEnclaveToken != null) {
                    requestData = enclaveRequestMap.get(currentEnclaveToken);
                }
            }
        }

        // Collect the user data for the servlet invocation.
        if (requestData != null) {
            HashMap<Integer, byte[]> data = ((UserDataImpl) userData).getUserDataBytes();
            if (data != null && data.size() != 0) {
                @SuppressWarnings("unchecked")
                List<HashMap<Integer, byte[]>> list = (List<HashMap<Integer, byte[]>>) requestData.get(USER_DATA_LIST);
                synchronized (list) {
                    list.add(data);
                }
            }
        }
    }

    /**
     * Processes the wrap handler and execute exit event.
     *
     * @param requestData     The current request data.
     * @param requestIdString The probe framework provided request id.
     */
    public void processWrapHandlerAndExecExitEvent(Map<String, Object> requestData, String requestIdString) {
        if (requestData != null) {
            requestMap.remove(requestIdString);
            // If the servlet service entry event was called and WLM is NOT enabled ...
            if (requestData.containsKey(SmfDataRecorderImpl.REMOTE_ADDR) && !requestData.containsKey(SmfDataRecorder.WLM_DATA_ARRIVAL_TIME)) {
                organizeUserData(requestData);
                smfRecorder.buildAndWriteRecord(requestData);
            }
        }
    }

    /**
     * Processes the log final response exit event.
     *
     * @param requestData The current request data.
     * @param event       The probe event associated with the wrap and run probe descriptor implementation.
     */
    public void processLogFinalResponseExitEvent(Map<String, Object> requestData, Event event) {
        // If we could not find the requestData, we are most likely on a different thread (i.e. async complete runnable).
        // Use the current enclave on the thread to find the requestData map.
        if (requestData == null) {
            DataProvider dataProvider = dataProviderMap.get(ENCLAVE_MGR_PROVIDER_NAME);
            if (dataProvider != null) {
                String currentEnclaveToken = dataProvider.getDataString();
                if (currentEnclaveToken != null) {
                    requestData = enclaveRequestMap.get(currentEnclaveToken);
                }
            }
        }

        // Make sure that we process this call on the correct path of execution (servlet service event called).
        if (requestData != null && requestData.containsKey(SmfDataRecorderImpl.REMOTE_ADDR)) {
            requestData.put(SmfDataRecorder.RESPONSE_BYTES, event.getContextInfo());
        }
    }

    /**
     * Processes the post enclave delete exit event.
     * Called only when the WLM feature enabled.
     *
     * @param event The probe event associated with the wrap and run probe descriptor implementation.
     */
    public void processPostEnclaveDeleteExitEvent(Event event) {
        HashMap<String, Object> wlmData = getEventContextInfo(event);
        if (wlmData != null) {
            boolean skipRecord = (Boolean) wlmData.get(SmfDataRecorderImpl.WLM_DATA_ENCLAVE_FORCED_DELETION);
            Map<String, Object> requestData = enclaveRequestMap.remove(wlmData.get(SmfDataRecorder.WLM_DATA_ENCLAVE_TOKEN));
            if (!skipRecord && requestData != null) {
                requestData.put(SmfDataRecorderImpl.WLM_DATA_DELETE_DATA, wlmData.get(SmfDataRecorderImpl.WLM_DATA_DELETE_DATA));
                requestData.put(SmfDataRecorder.END_STCK, Long.valueOf(nativeUtils.getSTCK()));
                wlmData.put(SmfDataRecorderImpl.WLM_DATA_END_TIME, Long.valueOf(nativeUtils.getSTCK()));
                organizeUserData(requestData);
                smfRecorder.buildAndWriteRecord(requestData);
            }
        }
    }

    /**
     * Organizes the user data that could have collected along nested servlet dispatches in a first come
     * first serve basis. The maximum allowed number of user data entries per record is 5; therefore, the
     * first 5 entries found are picked.
     *
     * Note that the user data support is thread based. This means that in the context of nested asynchronous
     * requests, each asynchronous request will add an entry to the list processed by this method.
     * The order in which the entries are added might not always be consistent. Therefore, attempts
     * to merge entries that may contain user data with the same IDs might yield random results.
     *
     * @param requestData The request data map containing all data associated with the HTTP request.
     */
    private void organizeUserData(Map<String, Object> requestData) {
        HashMap<Integer, byte[]> userData = new HashMap<Integer, byte[]>();
        @SuppressWarnings("unchecked")
        List<HashMap<Integer, byte[]>> list = (List<HashMap<Integer, byte[]>>) requestData.get(USER_DATA_LIST);
        synchronized (list) {
            for (HashMap<Integer, byte[]> data : list) {
                for (Map.Entry<Integer, byte[]> dataEntry : data.entrySet()) {
                    userData.put(dataEntry.getKey(), dataEntry.getValue());
                    if (userData.size() == 5) {
                        requestData.put(SmfDataRecorder.API_USER_DATA, userData);
                        return;
                    }
                }
            }
        }

        requestData.put(SmfDataRecorder.API_USER_DATA, userData);
    }

    /**
     * Populates the requestData map with HTTP request related data.
     *
     * @param requestData The requestData holding all pertinent data for the request.
     * @param event       The probe framework event.
     */
    private void getHTTPReqtInfo(Map<String, Object> requestData, Event event) {
        ContextInfoHelper contextInfoHelper = (ContextInfoHelper) event.getContextInfo();
        Object methodArgs = contextInfoHelper.getMethodArgs();
        Object[] obj = (Object[]) methodArgs;
        HttpServletRequest servletRequest = (HttpServletRequest) obj[0];
        requestData.put(SmfDataRecorder.REMOTE_ADDR, servletRequest.getRemoteAddr());
        requestData.put(SmfDataRecorder.REMOTE_PORT, Integer.valueOf(servletRequest.getRemotePort()));
        requestData.put(SmfDataRecorder.LOCAL_PORT, Integer.valueOf(servletRequest.getLocalPort()));
        requestData.put(SmfDataRecorder.REQUEST_URI, servletRequest.getRequestURI());

        Principal principal = servletRequest.getUserPrincipal();
        String userName = (principal != null) ? principal.getName() : null;
        if (userName != null) {
            requestData.put(SmfDataRecorder.USER_NAME, userName);
        }
        String mappedUserName = getMappedUserName();
        if (mappedUserName != null) {
            requestData.put(SmfDataRecorder.MAPPED_USER_NAME, mappedUserName);
        }
    }

    /**
     * Returns the event's context information.
     *
     * @param event The probe event.
     * @return The context information.
     */
    @SuppressWarnings("unchecked")
    public HashMap<String, Object> getEventContextInfo(Event event) {
        return (HashMap<String, Object>) event.getContextInfo();
    }
}