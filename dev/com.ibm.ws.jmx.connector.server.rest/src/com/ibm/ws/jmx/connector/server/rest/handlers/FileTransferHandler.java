/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.handlers;

import java.io.IOException;
import java.net.URLDecoder;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.FileTransferHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.audit.utils.AuditConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;

/**
 * Legacy code for V2 clients ONLY.
 *
 * DO NOT add new functionality or change behaviour.
 */
@Component(service = { RESTHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                        RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=" + APIConstants.JMX_CONNECTOR_API_ROOT_PATH,
                        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.PATH_FILETRANSFER_FILEPATH })
public class FileTransferHandler implements RESTHandler {
    public static final TraceComponent tc = Tr.register(FileTransferHandler.class);

    //OSGi service
    private final String KEY_FILE_TRANSFER_HELPER = "fileTransferHelper";
    private transient FileTransferHelper fileTransferHelper;
    private final AtomicServiceReference<FileTransferHelper> fileTransferHelperRef = new AtomicServiceReference<FileTransferHelper>(KEY_FILE_TRANSFER_HELPER);

    @Activate
    protected void activate(ComponentContext context) {
        fileTransferHelperRef.activate(context);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        fileTransferHelperRef.deactivate(context);
    }

    // FileTransferHelper
    @Reference(name = KEY_FILE_TRANSFER_HELPER, service = FileTransferHelper.class)
    protected void setFileTransferHelperRef(ServiceReference<FileTransferHelper> ref) {
        fileTransferHelperRef.setReference(ref);
    }

    protected void unsetFileTransferHelperRef(ServiceReference<FileTransferHelper> ref) {
        fileTransferHelperRef.unsetReference(ref);
    }

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        System.out.println("EFT: IN HANDLEREQUEST");
        String method = request.getMethod();
        if (RESTHelper.isGetMethod(method)) {

            System.out.println("  in get method");
            downloadLegacy(request, response);
            //try {
            //    if (URLDecoder.decode(request.getContextPath() + request.getPath(), "UTF-8").equals("/IBMJMXConnectorREST/file/${server.config.dir}/server.xml")) {
            //Audit.audit(Audit.EventID.SERVER_CONFIG_CHANGE_01, request, response, response.getStatus());
            //    }
            //} catch (Exception e) {
            //
            //}

        } else if (RESTHelper.isPostMethod(method)) {

            System.out.println("  in post  method");
            uploadLegacy(request, response);
            try {
                System.out.println("file: " + URLDecoder.decode(request.getContextPath() + request.getPath(), "UTF-8"));
                if (URLDecoder.decode(request.getContextPath() + request.getPath(), "UTF-8").equals("/IBMJMXConnectorREST/file/${server.config.dir}/server.xml")) {
                    Audit.audit(Audit.EventID.FILE_TRANSFER_UPDATE_01, request, response, response.getStatus());
                    System.out.println(" >> transfer update for server.xml");
                } else {
                    Audit.audit(Audit.EventID.FILE_TRANSFER_ADD_01, request, response, response.getStatus());
                    System.out.println(" >> transfer add");
                }
            } catch (IOException e) {

            }

        } else if (RESTHelper.isDeleteMethod(method)) {
            deleteLegacy(request, response);

            System.out.println("  in delete  method");
            Audit.audit(Audit.EventID.FILE_TRANSFER_DELETE_01, request, response, response.getStatus());
            System.out.println(" >> transfer delete");

        } else {
            throw new RESTHandlerMethodNotAllowedError("GET,POST,DELETE");
        }
    }

    /**
     * @Legacy
     */
    private void downloadLegacy(RESTRequest request, RESTResponse response) {
        System.out.println("EFT: in handler, downloadLegacy");
        String filePath = RESTHelper.getRequiredParam(request, APIConstants.PARAM_FILEPATH);

        //Get the helper
        final FileTransferHelper helper = getFileTransferHelper();
        helper.downloadInternal(request, response, filePath, true);

        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }

    /**
     * @Legacy
     */
    private void uploadLegacy(RESTRequest request, RESTResponse response) {
        System.out.println("EFT: in handler, uploadLegacy");
        String filePath = RESTHelper.getRequiredParam(request, APIConstants.PARAM_FILEPATH);
        String expand = RESTHelper.getQueryParam(request, APIConstants.PARAM_EXPAND_ON_COMPLETION);
        final boolean expansion = expand != null && "true".compareToIgnoreCase(expand) == 0;

        //Get the helper
        final FileTransferHelper helper = getFileTransferHelper();
        helper.uploadInternal(request, filePath, expansion, true);

        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }

    /**
     * @Legacy
     */
    private void deleteLegacy(RESTRequest request, RESTResponse response) {

        System.out.println("EFT: in handler, deleteLegacy");
        String filePath = RESTHelper.getRequiredParam(request, APIConstants.PARAM_FILEPATH);

        //Get the helper
        final FileTransferHelper helper = getFileTransferHelper();
        helper.deleteInternal(filePath, false);

        response.setStatus(APIConstants.STATUS_NO_CONTENT);
    }

    private synchronized FileTransferHelper getFileTransferHelper() {
        if (fileTransferHelper == null) {
            fileTransferHelper = fileTransferHelperRef != null ? fileTransferHelperRef.getService() : null;

            if (fileTransferHelper == null) {
                IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                               APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                               "OSGI_SERVICE_ERROR",
                                                                               new Object[] { "FileTransferHelper" },
                                                                               "CWWKX0122E: OSGi service is not available."));
                throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            }
        }

        return fileTransferHelper;
    }
}
