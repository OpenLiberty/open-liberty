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
package com.ibm.ws.ui.internal.rest.v1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Response;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.ui.internal.Filter;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.rest.CommonJSONRESTHandler;
import com.ibm.ws.ui.internal.rest.exceptions.BadRequestException;
import com.ibm.ws.ui.internal.rest.exceptions.MethodNotSupportedException;
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.rest.exceptions.UserNotAuthorizedException;
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.IToolbox;
import com.ibm.ws.ui.internal.v1.IToolboxService;
import com.ibm.ws.ui.internal.v1.pojo.Bookmark;
import com.ibm.ws.ui.internal.v1.pojo.DuplicateException;
import com.ibm.ws.ui.internal.v1.pojo.Message;
import com.ibm.ws.ui.internal.v1.pojo.NoSuchToolException;
import com.ibm.ws.ui.internal.v1.pojo.ToolEntry;
import com.ibm.ws.ui.internal.v1.utils.Utils;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>Defines the toolbox API for the version 1 of the adminCenter REST API.</p>
 *
 * <p>Maps to host:port/ibm/api/adminCenter/v1/toolbox</p>
 */
public class ToolboxAPI extends CommonJSONRESTHandler implements V1Constants {
    private static final TraceComponent tc = Tr.register(ToolboxAPI.class);

    static final String CHILD_RESOURCE_METADATA = "_metadata";
    static final String CHILD_RESOURCE_PREFERENCES = "preferences";
    static final String CHILD_RESOURCE_TOOL_ENTRIES = "toolEntries";
    static final String CHILD_RESOURCE_BOOKMARKS = "bookmarks";

    static final String RESET_TOOLBOX_CONFIRMATION_PARAM = "resetToolbox";
    private final IToolboxService toolboxService;

    /**
     * Default constructor.
     *
     * @param toolboxService
     */
    public ToolboxAPI(IToolboxService toolboxService) {
        super(TOOLBOX_PATH, true, true);
        this.toolboxService = toolboxService;
    }

    /**
     * Constructor used by unit tests
     *
     * @param toolboxService
     * @param jsonService
     */
    protected ToolboxAPI(IToolboxService toolboxService, Filter filter, JSON jsonService) {
        super(TOOLBOX_PATH, true, true, filter, jsonService);
        this.toolboxService = toolboxService;
    }

    /**
     * Gets the user's authenticated ID.
     *
     * @param request The RESTRequest from handleRequest
     * @return The authenticated user's ID.
     */
    @Trivial
    private String getUserId(RESTRequest request) {
        return request.getUserPrincipal().getName();
    }

    /**
     * Gets the user's toolbox (based on the user associated with the request)
     *
     * @param request The RESTRequest from handleRequest
     * @return The Toolbox associated with the user, {@code null} is not returned.
     */
    @Trivial
    private IToolbox getToolbox(RESTRequest request) {
        return toolboxService.getToolbox(getUserId(request));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isKnownChildResource(String child, RESTRequest request) {
        return CHILD_RESOURCE_BOOKMARKS.equals(child) || CHILD_RESOURCE_TOOL_ENTRIES.equals(child) ||
               CHILD_RESOURCE_METADATA.equals(child) || CHILD_RESOURCE_PREFERENCES.equals(child);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isKnownGrandchildResource(String child, String grandchild, RESTRequest request) {
        final String toolId = Utils.urlEncode(grandchild);
        if (CHILD_RESOURCE_BOOKMARKS.equals(child)) {
            return getToolbox(request).getBookmark(toolId) != null;
        } else if (CHILD_RESOURCE_TOOL_ENTRIES.equals(child)) {
            return getToolbox(request).getToolEntry(toolId) != null;
        } else {
            return false;
        }
    }

    /**
     * Get the toolbox.
     * Fields can be filtered by specifying "?fields=<field1>,<field2>,..." in the URL
     *
     * {@inheritDoc}
     *
     * @return the JSON representation of the toolbox
     */
    @Override
    public Object getBase(RESTRequest request, RESTResponse response) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        return applyFilter(request, getToolbox(request));
    }

    /**
     * Get a collection from the toolbox.
     * Fields can be filtered by specifying "?fields=<field1>,<field2>,..." in the URL
     *
     * {@inheritDoc}
     *
     * @return the JSON representation of the collection
     */
    @Override
    public Object getChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        if (CHILD_RESOURCE_PREFERENCES.equals(child)) {
            return applyFilter(request, getToolbox(request).getPreferences());
        } else if (CHILD_RESOURCE_BOOKMARKS.equals(child)) {
            return applyFilter(request, getToolbox(request).getBookmarks());
        } else if (CHILD_RESOURCE_TOOL_ENTRIES.equals(child)) {
            return applyFilter(request, getToolbox(request).getToolEntries());
        } else if (CHILD_RESOURCE_METADATA.equals(child)) {
            return applyFilter(request, getToolbox(request).get_metadata());
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * Processes the response based on the obtained Tool.
     *
     * @param request
     * @param toolId
     * @return
     * @throws RESTException
     * @throws NoSuchResourceException
     */
    private Object handleToolResponse(RESTRequest request, String toolId, ITool tool) throws RESTException, NoSuchResourceException {
        if (tool != null) {
            return applyFilter(request, tool);
        } else {
            Message tmsg = new Message(HTTP_NOT_FOUND, RequestNLS.formatMessage(tc, "TOOL_NOT_FOUND_TOOLBOX", toolId, getUserId(request)));
            throw new NoSuchResourceException(MEDIA_TYPE_APPLICATION_JSON, tmsg);
        }
    }

    /**
     * Get a collection from the toolbox where the tool ID equals {toolId}.
     * Fields can be filtered by specifying "?fields=<field1>,<field2>,..." in the URL
     *
     * {@inheritDoc}
     *
     * @return the JSON representation of the tool
     */
    @Override
    public Object getGrandchild(final RESTRequest request, final RESTResponse response, final String child, final String grandchild) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        // The inbound child resource name is not URL encoded, need to encode it
        final String toolId = Utils.urlEncode(grandchild);
        if (CHILD_RESOURCE_TOOL_ENTRIES.equals(child)) {
            return handleToolResponse(request, toolId, getToolbox(request).getToolEntry(toolId));
        } else if (CHILD_RESOURCE_BOOKMARKS.equals(child)) {
            return handleToolResponse(request, toolId, getToolbox(request).getBookmark(toolId));
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * Add a new tool to the toolbox. The tool can be either a Bookmark or a ToolEntry.
     * The ToolEntry POST should be {id:"",type:""}
     * The Bookmark POST should be {name:"",url:"",icon:""} OR {id:"",type:"",name:"",url:"",icon:""}. Description is optional.
     *
     * {@inheritDoc}
     *
     * @return {@link Response} object which contains the tool and http response code {@link Response.Status.CREATED} if the tool is added successfully;
     *         or {@link Response} object which contains an error message and http response code {@link Response.Status.CONFLICT} if the tool already exists;
     *         or {@link Response} object which contains an error message and http response code {@link Response.Status.BAD_REQUEST} if the tool does not pass the validation.
     */
    @FFDCIgnore({ DuplicateException.class, InvalidToolException.class })
    @Override
    public POSTResponse postChild(RESTRequest request, RESTResponse response, final String child) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        if (CHILD_RESOURCE_TOOL_ENTRIES.equals(child)) {
            ToolEntry toAdd = readJSONPayload(request, ToolEntry.class);
            try {
                ToolEntry added = getToolbox(request).addToolEntry(toAdd);
                POSTResponse postResponse = new POSTResponse();
                postResponse.createdURL = request.getURL() + "/" + added.getId();
                postResponse.jsonPayload = added;
                return postResponse;
            } catch (NoSuchToolException e) {
                Object payload = new Message(HTTP_BAD_REQUEST, e.getMessage());
                throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, payload);
            } catch (DuplicateException e) {
                Object payload = new Message(HTTP_CONFLICT, e.getMessage());
                throw new RESTException(HTTP_CONFLICT, MEDIA_TYPE_APPLICATION_JSON, payload);
            } catch (InvalidToolException e) {
                Object payload = new Message(HTTP_BAD_REQUEST, e.getMessage());
                throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, payload);
            }
        } else if (CHILD_RESOURCE_BOOKMARKS.equals(child)) {
            Bookmark toAdd = readJSONPayload(request, Bookmark.class);
            try {
                ITool added = getToolbox(request).addBookmark(toAdd);
                POSTResponse postResponse = new POSTResponse();
                postResponse.createdURL = request.getURL() + "/" + added.getId();
                postResponse.jsonPayload = added;
                return postResponse;
            } catch (DuplicateException e) {
                Object payload = new Message(HTTP_CONFLICT, e.getMessage());
                throw new RESTException(HTTP_CONFLICT, MEDIA_TYPE_APPLICATION_JSON, payload);
            } catch (InvalidToolException e) {
                Object payload = new Message(HTTP_BAD_REQUEST, e.getMessage());
                throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, payload);
            }
        } else {
            throw new MethodNotSupportedException();
        }
    }

    /**
     * Supports updates to individual aspects of the toolbox. Currently, only
     * preferences is supported.
     *
     * {@inheritDoc}
     */
    @FFDCIgnore({ NoSuchToolException.class, IllegalArgumentException.class })
    @Override
    public Object putChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        if (CHILD_RESOURCE_PREFERENCES.equals(child)) {
            IToolbox toolbox = getToolbox(request);
            @SuppressWarnings("unchecked")
            Map<String, Object> preferences = readJSONPayload(request, Map.class);
            return toolbox.updatePreferences(preferences);
        } else if (CHILD_RESOURCE_TOOL_ENTRIES.equals(child)) {
            ToolEntry[] listEntries = readJSONPayload(request, ToolEntry[].class);
            List<ToolEntry> toolEntries = new ArrayList<ToolEntry>();
            toolEntries.addAll(Arrays.asList(listEntries));
            try {
                getToolbox(request).updateToolEntries(toolEntries);
                return new Message(HTTP_OK, RequestNLS.formatMessage(tc, "TOOLBOX_UPDATE_SUCCESSFUL"));
            } catch (NoSuchToolException e) {
                Object payload = new Message(HTTP_BAD_REQUEST, e.getMessage());
                throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, payload);
            } catch (IllegalArgumentException e) {
                Object payload = new Message(HTTP_BAD_REQUEST, e.getMessage());
                throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, payload);
            }
        } else {
            throw new MethodNotSupportedException();
        }
    }

    /**
     * Reset the toolbox to the default configuration.
     *
     * {@inheritDoc}
     */
    @Override
    public Object deleteBase(RESTRequest request, RESTResponse response) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        String resetToolbox = request.getParameter(RESET_TOOLBOX_CONFIRMATION_PARAM);
        if (Boolean.valueOf(resetToolbox)) {
            getToolbox(request).reset();
            return new Message(HTTP_OK, RequestNLS.formatMessage(tc, "TOOLBOX_RESET_SUCCESSFUL"));
        } else {
            Message msg = new Message(HTTP_BAD_REQUEST, RequestNLS.formatMessage(tc, "TOOLBOX_RESET_MUST_BE_CONFIRMED"));
            msg.setDeveloperMessage(RequestNLS.formatMessage(tc, "TOOLBOX_RESET_MUST_BE_CONFIRMED.developeraction"));
            throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, msg);
        }
    }

    /**
     * Delete a tool from the toolbox.
     *
     * {@inheritDoc}
     */
    @Override
    public Object deleteGrandchild(RESTRequest request, RESTResponse response, String child, final String grandchild) throws RESTException {
        if (!isAuthorizedAdminOrReader(request, response)) {
            throw new UserNotAuthorizedException();
        }
        // The inbound child resource name is not URL encoded, need to encode it
        String toolId = Utils.urlEncode(grandchild);
        if (CHILD_RESOURCE_BOOKMARKS.equals(child)) {
            return handleToolResponse(request, toolId, getToolbox(request).deleteBookmark(toolId));
        } else if (CHILD_RESOURCE_TOOL_ENTRIES.equals(child)) {
            return handleToolResponse(request, toolId, getToolbox(request).deleteToolEntry(toolId));
        } else {
            throw new MethodNotSupportedException();
        }
    }
}
