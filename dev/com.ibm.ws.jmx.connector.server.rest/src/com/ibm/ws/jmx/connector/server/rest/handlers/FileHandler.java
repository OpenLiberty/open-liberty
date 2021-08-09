/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.handlers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.jmx.connector.rest.ConnectorSettings;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.FileTransferHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.MultipleRoutingHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.OutputHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.ws.rest.handler.helper.ServletRESTRequestWithParams;
import com.ibm.ws.rest.handler.helper.ServletRESTResponseWithWriter;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerOSGiError;

@Component(service = { RESTHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                        RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_ROUTING + "=true",
                        RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=" + APIConstants.JMX_CONNECTOR_API_ROOT_PATH,
                        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_FILE_FILEPATH })
public class FileHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(FileHandler.class);

    private final String KEY_FILE_TRANSFER_HELPER = "fileTransferHelper";
    private transient FileTransferHelper fileTransferHelper;
    private final AtomicServiceReference<FileTransferHelper> fileTransferHelperRef = new AtomicServiceReference<FileTransferHelper>(KEY_FILE_TRANSFER_HELPER);

    private transient MultipleRoutingHelper multipleRoutingHelper;
    private transient ComponentContext componentContext;

    @Activate
    protected void activate(ComponentContext context) {
        fileTransferHelperRef.activate(context);
        componentContext = context;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        fileTransferHelperRef.deactivate(context);
        componentContext = null;
    }

    @Reference(name = KEY_FILE_TRANSFER_HELPER, service = FileTransferHelper.class)
    protected void setFileTransferHelperRef(ServiceReference<FileTransferHelper> ref) {
        fileTransferHelperRef.setReference(ref);
    }

    protected void unsetFileTransferHelperRef(ServiceReference<FileTransferHelper> ref) {
        fileTransferHelperRef.unsetReference(ref);
    }

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        String method = request.getMethod();
        if (RESTHelper.isGetMethod(method)) {
            download(request, response);
        } else if (RESTHelper.isPostMethod(method)) {
            upload(request, response);
        } else if (RESTHelper.isDeleteMethod(method)) {
            delete(request, response);
        } else {
            throw new RESTHandlerMethodNotAllowedError("GET,POST,DELETE");
        }
    }

    private void download(RESTRequest request, RESTResponse response) {
        String filePath = RESTHelper.getRequiredParam(request, APIConstants.PARAM_FILEPATH);
        String startOffsetValue = RESTHelper.getQueryParam(request, APIConstants.PARAM_START_OFFSET);
        String endOffsetValue = RESTHelper.getQueryParam(request, APIConstants.PARAM_END_OFFSET);

        //Fetch optional query params
        final long startOffset, endOffset;
        try {
            startOffset = (startOffsetValue != null && !startOffsetValue.isEmpty()) ? Long.valueOf(startOffsetValue) : 0;
            endOffset = (endOffsetValue != null && !endOffsetValue.isEmpty()) ? Long.valueOf(endOffsetValue) : -1;
        } catch (NumberFormatException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }

        //Get the helper
        final FileTransferHelper helper = getFileTransferHelper();

        if (RESTHelper.containsSingleRoutingContext(request)) {
            helper.routedDownloadInternal(request, response, filePath, false);
        } else {
            helper.downloadInternal(request, response, filePath, startOffset, endOffset, false);
        }
    }

    private void upload(RESTRequest request, RESTResponse response) {
        String expand = RESTHelper.getQueryParam(request, APIConstants.PARAM_EXPAND_ON_COMPLETION);
        String local = RESTHelper.getQueryParam(request, APIConstants.PARAM_LOCAL);
        String actionHeader = request.getHeader(ConnectorSettings.POST_TRANSFER_ACTION);
        String filePath = null;
        String originPackagePath = null;
        ServletRESTRequestWithParams req = null;
        boolean deploymentAPICall = false;
        boolean nodeDeployment = false;
        boolean nodeDeploymentRemote = false;
        boolean nodeDeploymentLocal = false;

        String deployService = null;
        boolean isMultipartRequest = request.isMultiPartRequest();

        if (request instanceof ServletRESTRequestWithParams) {
            // This type of request only comes from DeploymentAPI
            // when the request is of this type it means that the request is NOT a multi-part request
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "File transfer call from DeploymentAPI for Liberty or Node.js deployment.");
            }
            req = (ServletRESTRequestWithParams) request;
            if (req.getAdditionalParamaMap().containsKey("deployService")) {
                deploymentAPICall = true;
                deployService = req.getParam("deployService"); // the value is either liberty OR node.js
                local = req.getParam("local");

                filePath = req.getParam("targetPath");
                originPackagePath = req.getParam("originPackagePath");
                if ("liberty".equalsIgnoreCase(deployService)) {
                    expand = "true";
                    actionHeader = req.getParam("action");
                    try {
                        filePath = URLDecoder.decode(filePath, "UTF8");
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Server package is located on the controller, filePath is:" + filePath);
                        }
                    } catch (UnsupportedEncodingException e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exception while decoding the filePath string.");
                        }
                    }
                } else if ("node.js".equalsIgnoreCase(deployService)) {
                    nodeDeployment = true;
                    deploymentAPICall = true;
                    expand = "false";
                    if (isMultipartRequest)
                        nodeDeploymentRemote = true;
                    else
                        nodeDeploymentLocal = true;
                } else {
                    throw new IllegalArgumentException("Invalid deployment type " + deployService);
                }
            }
        }

        if (isMultipartRequest && (ConnectorSettings.POST_TRANSFER_ACTION_FIND_SERVER_NAME.equals(actionHeader) || nodeDeploymentRemote)) {
            deploymentAPICall = true;
            filePath = RESTHelper.getQueryParam(request, "filePath");
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "AdminCenter deployment. Server package NOT located on controller, filePath is:" + filePath);
            }
        }
        if (filePath == null && !nodeDeployment) {
            filePath = RESTHelper.getRequiredParam(request, APIConstants.PARAM_FILEPATH);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "file path: " + filePath);
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Expand: " + expand);
            Tr.debug(tc, "Local: " + local);
            Tr.debug(tc, "ActionHeader: " + actionHeader);
            Tr.debug(tc, "Origin package Path: " + originPackagePath);
            Tr.debug(tc, "TargetPath: " + filePath + ". Target path is not used for node.js Deployment");

        }

        //Fetch optional query params
        final boolean expansion = expand != null && "true".compareToIgnoreCase(expand) == 0;
        final boolean localFile = local != null && "true".compareToIgnoreCase(local) == 0;

        if (filePath.endsWith("/")) {
            filePath = filePath.substring(0, filePath.length() - 1);
        }

        String uploadResults;
        //UPLOAD also supports multiple routing
        if (RESTHelper.containsMultipleRoutingContext(request)) {
            try {
                uploadResults = getMultipleRoutingHelper().multipleUploadInternal(request, filePath, expansion, localFile);
            } catch (IOException e) {
                throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            }
        } else {
            //Get the helper
            final FileTransferHelper helper = getFileTransferHelper();

            if (RESTHelper.containsSingleRoutingContext(request)) {
                helper.routedUploadInternal(request, filePath, expansion, false);
            } else {
                helper.uploadInternal(request, filePath, expansion, false);
            }

            uploadResults = null;
        }
        if (deploymentAPICall) {
            ServletRESTResponseWithWriter customResponse = (ServletRESTResponseWithWriter) response;
            try {
                Writer wr = customResponse.geStringtWriter();
                if (uploadResults != null && uploadResults.contains("failed")) {
                    OutputHelper.writeJsonOutput(response,
                                                 "File Transfer failed, please check your deployment input variables or run update host command to ensure that the target host information are accurate.");
                }

                wr.write(uploadResults);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "String written to response object is: " + uploadResults);
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "IOException while writing to the custom Response object");
                    Tr.debug(tc, e.getMessage());
                }
            }

        } else {
            OutputHelper.writeJsonOutput(response, uploadResults);
        }
    }

    private void delete(RESTRequest request, RESTResponse response) {
        String filePath = RESTHelper.getRequiredParam(request, APIConstants.PARAM_FILEPATH);
        String recursiveDelete = RESTHelper.getQueryParam(request, APIConstants.PARAM_RECURSIVE_DELETE);

        final boolean recursive = recursiveDelete != null && "true".compareToIgnoreCase(recursiveDelete) == 0;

        if (filePath.endsWith("/")) {
            filePath = filePath.substring(0, filePath.length() - 1);
        }

        String deleteOutput;

        //DELETE also supports multiple routing
        if (RESTHelper.containsMultipleRoutingContext(request)) {
            try {
                deleteOutput = getMultipleRoutingHelper().multipleDeleteInternal(request, filePath, recursive);
            } catch (IOException e) {
                throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            }
        } else {
            //Get the helper
            final FileTransferHelper helper = getFileTransferHelper();

            if (RESTHelper.containsSingleRoutingContext(request)) {
                helper.routedDeleteInternal(request, filePath, recursive);
            } else {
                helper.deleteInternal(filePath, recursive);
            }
            deleteOutput = null;
        }

        OutputHelper.writeJsonOutput(response, deleteOutput);
    }

    private synchronized FileTransferHelper getFileTransferHelper() {
        if (fileTransferHelper == null) {
            fileTransferHelper = fileTransferHelperRef != null ? fileTransferHelperRef.getService() : null;

            if (fileTransferHelper == null) {
                throw new RESTHandlerOSGiError("FileTransferHelper");
            }
        }

        return fileTransferHelper;
    }

    private synchronized MultipleRoutingHelper getMultipleRoutingHelper() {
        if (multipleRoutingHelper == null) {
            BundleContext bc = componentContext.getBundleContext();
            ServiceReference<MultipleRoutingHelper> multipleRoutingHelperRef = bc.getServiceReference(MultipleRoutingHelper.class);

            multipleRoutingHelper = multipleRoutingHelperRef != null ? bc.getService(multipleRoutingHelperRef) : null;

            if (multipleRoutingHelper == null) {
                IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                               APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                               "OSGI_SERVICE_ERROR",
                                                                               new Object[] { "MultipleRoutingHelper" },
                                                                               "CWWKX0122E: OSGi service is not available."));
                throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            }
        }

        return multipleRoutingHelper;
    }

    //@FFDCIgnore(IllegalArgumentException.class)
    public Map<String, String> checkRequestForDeploymentParameters(RESTRequest request) {

        Map<String, String> map = null;
        if (request instanceof ServletRESTRequestWithParams) {
            ServletRESTRequestWithParams req = (ServletRESTRequestWithParams) request;
            map = req.getAdditionalParamaMap();

            if (request.getParameterMap().containsKey("deployService")) {
                String deployService = request.getParameter("deployService");
                if (deployService != null && "true".equalsIgnoreCase(deployService)) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "FileHandler service received a call from Deployment service.");
                    }
                    map = new HashMap<String, String>();
                    String local = request.getParameter("local").toLowerCase();
                    String filePath = request.getParameter("targetPath");
                    String actionHeader = request.getParameter(ConnectorSettings.POST_TRANSFER_ACTION);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "**** local:" + local);
                        Tr.debug(tc, "**** targetPath:" + filePath);
                        Tr.debug(tc, "**** actionHeader:" + actionHeader);
                    }

                    map.put("local", local);
                    map.put("targetPath", filePath);
                    map.put("actionHeader", actionHeader);
                }
            }

        }

        return map;
    }

}
