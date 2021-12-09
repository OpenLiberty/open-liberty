/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.rest.v1;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.ws.Response;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.rest.CommonRESTHandler;
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.rest.exceptions.ResourceNoContentException;
import com.ibm.ws.ui.internal.rest.exceptions.UserNotAuthorizedException;
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.IToolDataService;
import com.ibm.ws.ui.internal.v1.IToolbox;
import com.ibm.ws.ui.internal.v1.IToolboxService;
import com.ibm.ws.ui.internal.v1.pojo.ToolEntry;
import com.ibm.ws.ui.internal.v1.utils.Utils;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>Defines the per user tool data API for the version 1 of the adminCenter REST API.</p>
 * <p>Http header ETag (entity tag) is used in the response header to send back the MD5 checksum of
 * the tool data.</p>
 * <p>When a client performs a GET call, the response header's ETag contains the MD5 checksum of the
 * returned tool data. When the same client tries to perform a PUT (update data) call, the "IF-Match"
 * request header must contain the MD5 checksum of the original tool data. If the MD5 checksum
 * ("IF-Match" request header value) does not match the MD5 checksum of the tool data in the storage
 * layer, HTTP BAD REQUEST (error 400) will be returned.</p>
 * <p>GET, POST and PUT responses contain ETag response header, the value is the MD5 checksum of
 * the returned tool data.</p>
 *
 * <p>Maps to host:port/ibm/api/adminCenter/v1/tooldata</p>
 */
public class ToolDataAPI extends CommonRESTHandler implements V1Constants {
    private final IToolDataService toolDataService;
    private final IToolboxService toolboxService;
    private static final TraceComponent tc = Tr.register(ToolDataAPI.class);

    /**
     * Register the REST URL
     */
    public ToolDataAPI(IToolDataService toolDataService, IToolboxService toolboxService) {
        super(TOOLDATA_PATH, true, false);
        this.toolDataService = toolDataService;
        this.toolboxService = toolboxService;

    }

    /**
     * Checks if the given child is a known resource.
     * A known resource is an existing feature tool, it also must be added into the
     * given user's toolbox.
     *
     * @param child   the tool name
     * @param request The REST request instance.
     * @return <code>true</code> if the tool is a feature tool and exists in the user's toolbox.
     *         Otherwise returns <code>false</code>.
     */
    @Override
    public boolean isKnownChildResource(String child, RESTRequest request) {
        IToolbox tb = this.toolboxService.getToolbox(request.getUserPrincipal().getName());
        List<ToolEntry> ts = tb.getToolEntries();
        boolean found = false;
        if (ts == null || ts.size() == 0)
            return false;
        for (int i = 0; i < ts.size() && found == false; i++) {
            ToolEntry te = ts.get(i);
            if (te.getType().equals(ITool.TYPE_FEATURE_TOOL)) {
                String toolname = getToolNameFromToolID(te.getId());
                if (toolname != null) {
                    found = toolname.equals(child);
                }
            }
        }
        return found;
    }

    /**
     * Gets the tool data of for the giving tool/user.
     * When the tool data is returned, a md5 checksum of the data is also returned using the Response's HTTP header Etag.
     *
     *
     * {@inheritDoc}
     *
     * @param request  The REST request instance.
     * @param response The REST response instance.
     * @param child    the child resource which is the tool name.
     *
     * @return the tool data string.
     */
    @Override
    public Object getChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        if (isKnownChildResource(child, request)) {
            toolDataService.promoteIfPossible(request.getUserPrincipal().getName(), child);
            Object object = toolDataService.getToolData(request.getUserPrincipal().getName(), child);
            if (object != null) {
                String md5 = Utils.getMD5String(object.toString());
                response.setResponseHeader(HTTP_HEADER_ETAG, md5);
                return object;
            }
            // Return 204 (no content) so as to get rid of the 404 console error
            throw new ResourceNoContentException();
        } else {
            throw new NoSuchResourceException();
        }

    }

    /**
     * Adds the tool data for the giving user and tool.
     *
     * {@inheritDoc}
     *
     * @return {@link Response} object which contains the tool data and http response
     *         code {@link Response.Status.CREATED} if the tool data is added successfully;
     *         or {@link Response} object which contains an error message and http response
     *         code {@link Response.Status.CONFLICT} if the tool data already exists;
     *         or {@link Response} object which contains an error message and http response
     *         code {@link Response.Status.INTERNAL_SERVER_ERROR } if the tool data cannot be posted.
     */
    @Override
    public POSTResponse postChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        if (isKnownChildResource(child, request)) {
            try {
                toolDataService.promoteIfPossible(request.getUserPrincipal().getName(), child);
                boolean exists = toolDataService.exists(request.getUserPrincipal().getName(), child);

                if (exists) {
                    throw new RESTException(HTTP_CONFLICT);
                }

                String tooldata = getReaderContents(request.getInputStream(), POST_MAX_PLAIN_TEXT_SIZE);

                String td = toolDataService.addToolData(request.getUserPrincipal().getName(), child, tooldata);
                if (td == null)
                    throw new RESTException(HTTP_INTERNAL_ERROR);
                POSTResponse postResponse = new POSTResponse();
                postResponse.createdURL = request.getURL() + "/" + child;
                postResponse.jsonPayload = td;
                String md5 = Utils.getMD5String(td);
                response.setResponseHeader(HTTP_HEADER_ETAG, md5);
                return postResponse;
            } catch (IOException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpected IOException while reading RESTRequest payload", e);
                }
            }
            throw new RESTException(HTTP_INTERNAL_ERROR);
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * Deletes the tool data for the giving tool / user.
     * If both repository and file persistence providers are available, the tool data will be deleted from
     * both providers.
     *
     * {@inheritDoc}
     *
     */
    @Override
    public Object deleteChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        if (isKnownChildResource(child, request)) {
            String uid = request.getUserPrincipal().getName();
            boolean exists = toolDataService.exists(uid, child);
            if (!exists) {
                throw new NoSuchResourceException();
            }
            if (toolDataService.deleteToolData(uid, child))
                return "";
            else
                throw new RESTException(HTTP_INTERNAL_ERROR, MEDIA_TYPE_TEXT_PLAIN, RequestNLS.formatMessage(tc, "TOOLDATA_DELETE_UNSUCCESSFUL", uid, child));
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * Updates the tool data for the giving tool/user.
     *
     * The pre-condition for the update operation is the http request header "If-Match".
     * The "If-Match" should contain the MD5 checksum of the tool data that is being updated.
     * The GET response of the tool contains response header Etag. The value of Etag is the MD5 checksum
     * of the tool data returned to the caller.
     *
     * {@inheritDoc}
     *
     * @return The tool data. If the update failed, the response will be set as below:
     *         code {@link Response.Status.OK} if the tool data is updated successfully;
     *         or code {@link Response.Status.HTTP_NOT_FOUND} if the tool data does not exist;
     *         or code {@link Response.Status.HTTP_PRECONDITION_FAILED } if the request doesn't contain If-Match header;
     *         or code {@link Response.Status.HTTP_PRECONDITION_FAILED } if the MD5 checksum (ETag value of the request header) passed
     *         in doesn't match the MD5 checksum of the data being replaced;
     *         or code {@link Response.Status.HTTP_INTERNAL_ERROR } if other failure occurred.
     *
     */
    @Override
    public Object putChild(final RESTRequest request, final RESTResponse response, final String child) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        if (isKnownChildResource(child, request)) {

            try {
                String md5In = request.getHeader(HTTP_HEADER_IF_MATCH);
                if (md5In == null)
                    throw new RESTException(HTTP_PRECONDITION_FAILED);
                boolean exists = toolDataService.exists(request.getUserPrincipal().getName(), child);
                if (!exists) {
                    // updating non-existing data
                    throw new NoSuchResourceException();
                }
                String originalData = toolDataService.getToolData(request.getUserPrincipal().getName(), child);
                String md5 = Utils.getMD5String(originalData.toString());

                if (md5In.equals(md5) == false) {
                    throw new RESTException(HTTP_PRECONDITION_FAILED, MEDIA_TYPE_TEXT_PLAIN, RequestNLS.formatMessage(tc, "TOOLDATA_UPDATE_CHECKSUM_NOT_MATCH",
                                                                                                                      request.getUserPrincipal().getName(),
                                                                                                                      child));
                }
                String tooldata = getReaderContents(request.getInputStream(), POST_MAX_PLAIN_TEXT_SIZE);

                String td = toolDataService.addToolData(request.getUserPrincipal().getName(), child, tooldata);

                if (td == null)
                    throw new RESTException(HTTP_INTERNAL_ERROR);
                md5 = Utils.getMD5String(td);
                response.setResponseHeader(HTTP_HEADER_ETAG, md5);

                return td;
            } catch (IOException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpected IOException while reading RESTRequest payload", e);
                }
            }
            throw new RESTException(HTTP_INTERNAL_ERROR);
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * Gets the tool name from the tool id.
     * For example, tool id is: com.ibm.websphere.appserver.adminCenter.tool.collectiveManagement-1.0-1.0.0
     * The tool name is: com.ibm.websphere.appserver.adminCenter.tool.collectiveManagement
     *
     * @param toolID
     * @return the tool name
     */
    private String getToolNameFromToolID(String toolID) {
        Pattern pattern = Pattern.compile("(.*)-\\d+\\.\\d+-\\d+\\.\\d+\\.\\d+");

        Matcher matcher = pattern.matcher(toolID);
        if (matcher.matches() && matcher.groupCount() == 1) {
            return matcher.group(1);
        } else {
            Tr.error(tc, "UNABLE_TO_PARSE_TOOL_ID", toolID);
        }
        return null;
    }

}
