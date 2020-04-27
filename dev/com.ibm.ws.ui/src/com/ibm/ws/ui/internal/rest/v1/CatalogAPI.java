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
import com.ibm.ws.ui.internal.v1.ICatalog;
import com.ibm.ws.ui.internal.v1.ICatalogService;
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.pojo.Bookmark;
import com.ibm.ws.ui.internal.v1.pojo.DuplicateException;
import com.ibm.ws.ui.internal.v1.pojo.Message;
import com.ibm.ws.ui.internal.v1.utils.Utils;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * <p>Defines the catalog API for the version 1 of the adminCenter REST API.</p>
 *
 * <p>Maps to host:port/ibm/api/adminCenter/v1/catalog</p>
 */
public class CatalogAPI extends CommonJSONRESTHandler implements V1Constants {
    private static final TraceComponent tc = Tr.register(CatalogAPI.class);

    static final String CHILD_RESOURCE_METADATA = "_metadata";
    static final String CHILD_RESOURCE_FEATURE_TOOLS = "featureTools";
    static final String CHILD_RESOURCE_BOOKMARKS = "bookmarks";

    static final String RESET_CATALOG_CONFIRMATION_PARAM = "resetCatalog";

    private final ICatalogService catalogService;

    /**
     * Default constructor.
     *
     * @param catalogService
     */
    public CatalogAPI(ICatalogService catalogService) {
        super(CATALOG_PATH, true, true);
        this.catalogService = catalogService;
    }

    /**
     * Constructor used by unit tests
     *
     * @param catalogService
     * @param jsonService
     */
    protected CatalogAPI(ICatalogService catalogService, Filter filter, JSON jsonService) {
        super(CATALOG_PATH, true, true, filter, jsonService);
        this.catalogService = catalogService;
    }

    /**
     * Grab the catalog singleton. Do not hold a reference as per the getCatalog()
     * JavaDoc.
     *
     * @return The ICatalog instance, {@code null} is not returned.
     */
    @Trivial
    private ICatalog getCatalog() {
        return catalogService.getCatalog();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isKnownChildResource(String child, RESTRequest request) {
        return CHILD_RESOURCE_BOOKMARKS.equals(child) || CHILD_RESOURCE_FEATURE_TOOLS.equals(child) ||
               CHILD_RESOURCE_METADATA.equals(child);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isKnownGrandchildResource(String child, String grandchild, RESTRequest request) {
        final String toolId = Utils.urlEncode(grandchild);
        if (CHILD_RESOURCE_BOOKMARKS.equals(child)) {
            return getCatalog().getBookmark(toolId) != null;
        } else if (CHILD_RESOURCE_FEATURE_TOOLS.equals(child)) {
            return getCatalog().getFeatureTool(toolId) != null;
        } else {
            return false;
        }
    }

    /**
     * Get all tools in the catalog.
     * Fields can be filtered by specifying "?fields=<field1>,<field2>,..." in the URL
     *
     * {@inheritDoc}
     *
     * @return the JSON representation of the catalog
     */
    @Override
    public Object getBase(RESTRequest request, RESTResponse response) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        return applyFilter(request, getCatalog());
    }

    /**
     * Get a collection from the catalog.
     * Fields can be filtered by specifying "?fields=<field1>,<field2>,..." in the URL
     *
     * {@inheritDoc}
     *
     * @return the JSON representation of a collection in the catalog
     */
    @Override
    public Object getChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        if (CHILD_RESOURCE_BOOKMARKS.equals(child)) {
            return applyFilter(request, getCatalog().getBookmarks());
        } else if (CHILD_RESOURCE_FEATURE_TOOLS.equals(child)) {
            return applyFilter(request, getCatalog().getFeatureTools());
        } else if (CHILD_RESOURCE_METADATA.equals(child)) {
            return applyFilter(request, getCatalog().get_metadata());
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
            Message tmsg = new Message(HTTP_NOT_FOUND, RequestNLS.formatMessage(tc, "TOOL_NOT_FOUND", toolId));
            throw new NoSuchResourceException(MEDIA_TYPE_APPLICATION_JSON, tmsg);
        }
    }

    /**
     * Get a tool from the catalog where the tool ID equals {toolId}.
     *
     * {@inheritDoc}
     *
     * @return the JSON representation of a tool in the catalog
     */
    @Override
    public Object getGrandchild(RESTRequest request, RESTResponse response, String child, String grandchild) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        // The inbound grandchild resource name is not URL encoded, need to encode it
        final String toolId = Utils.urlEncode(grandchild);
        if (CHILD_RESOURCE_BOOKMARKS.equals(child)) {
            return handleToolResponse(request, toolId, getCatalog().getBookmark(toolId));
        } else if (CHILD_RESOURCE_FEATURE_TOOLS.equals(child)) {
            return handleToolResponse(request, toolId, getCatalog().getFeatureTool(toolId));
        } else {
            throw new NoSuchResourceException();
        }
    }

    /**
     * Add a new bookmark to the catalog.
     * The Bookmark POST should be {name:"",url:"",icon:"",description:""} OR {id:"",type:"",name:"",url:"",icon:"",description:""}
     *
     * {@inheritDoc}
     *
     * @return {@link Response} object which contains the tool and http response code {@link Response.Status.CREATED} if the bookmark is added successfully;
     *         or {@link Response} object which contains an error message and http response code {@link Response.Status.CONFLICT} if the bookmark already exists;
     *         or {@link Response} object which contains an error message and http response code {@link Response.Status.BAD_REQUEST} if the bookmark does not pass the validation.
     */
    @FFDCIgnore({ DuplicateException.class, InvalidToolException.class })
    @Override
    public POSTResponse postChild(RESTRequest request, RESTResponse response, String child) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        if (CHILD_RESOURCE_BOOKMARKS.equals(child)) {
            Bookmark bookmark = readJSONPayload(request, Bookmark.class);
            try {
                ITool created = getCatalog().addBookmark(bookmark);
                POSTResponse postResponse = new POSTResponse();
                postResponse.createdURL = request.getURL() + "/" + created.getId();
                postResponse.jsonPayload = created;
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
     * Reset the catalog to the default configuration.
     *
     * {@inheritDoc}
     */
    @Override
    public Object deleteBase(RESTRequest request, RESTResponse response) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        String resetCatalog = request.getParameter(RESET_CATALOG_CONFIRMATION_PARAM);
        if (Boolean.valueOf(resetCatalog)) {
            getCatalog().reset();
            return new Message(HTTP_OK, RequestNLS.formatMessage(tc, "CATALOG_RESET_SUCCESSFUL"));
        } else {
            Message msg = new Message(HTTP_BAD_REQUEST, RequestNLS.formatMessage(tc, "CATALOG_RESET_MUST_BE_CONFIRMED"));
            msg.setDeveloperMessage(RequestNLS.formatMessage(tc, "CATALOG_RESET_MUST_BE_CONFIRMED.developeraction"));
            throw new BadRequestException(MEDIA_TYPE_APPLICATION_JSON, msg);
        }
    }

    /**
     * Delete a bookmark from the catalog.
     *
     * {@inheritDoc}
     */
    @Override
    public Object deleteGrandchild(RESTRequest request, RESTResponse response, String child, String grandchild) throws RESTException {
        if (!isAuthorizedDefault(request, response)) {
            throw new UserNotAuthorizedException();
        }
        // The inbound child resource name is not URL encoded, need to encode it
        String toolId = Utils.urlEncode(grandchild);
        if (CHILD_RESOURCE_BOOKMARKS.equals(child)) {
            ITool deletedTool = getCatalog().deleteBookmark(toolId);
            return handleToolResponse(request, toolId, deletedTool);
        } else if (CHILD_RESOURCE_FEATURE_TOOLS.equals(child)) {
            if (getCatalog().getFeatureTool(toolId) != null) {
                throw new MethodNotSupportedException();
            } else {
                throw new NoSuchResourceException();
            }
        } else {
            throw new MethodNotSupportedException();
        }
    }
}
