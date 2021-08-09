/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.event;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.ws.security.audit.utils.AuditUtils;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * Class with default values for MemberManagementEvent events
 */
public class MemberManagementEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(MemberManagementEvent.class);

    public static final String REALM_NOT_MATCHED = "realm not matched";

    public static final String REPO_NOT_MATCHED = "repository not matched";

    @SuppressWarnings("unchecked")
    public MemberManagementEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_MEMBER_MGMT);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public MemberManagementEvent(Object request, String action, String repositoryId, String uniqueName, String realmName, Object rootObj, Integer statusCode) {
        this(request, action, repositoryId, uniqueName, realmName, rootObj, statusCode, "vmmservice");
    }

    public MemberManagementEvent(Object request, String action, String repositoryId, String uniqueName, String realmName, Object rootObj, Integer statusCode, String serviceType) {
        this();
        try {
            String httpType = AuditEvent.REASON_TYPE_HTTPS;
            RESTRequest req = (RESTRequest) request;
            Root root = (Root) rootObj;

            set(AuditEvent.TARGET_APPNAME, AuditUtils.getJ2EEComponentName());

            if (req != null) { // process as REST or SCIM request
                if (req.getRemoteAddr() != null)
                    set(AuditEvent.INITIATOR_HOST_ADDRESS, req.getRemoteAddr());

                String agent = req.getHeader("User-Agent");
                if (agent != null)
                    set(AuditEvent.INITIATOR_HOST_AGENT, agent);

                set(AuditEvent.TARGET_NAME, req.getURI());

                if (req.getQueryString() != null) {
                    String str = URLDecoder.decode(req.getQueryString(), "UTF-8");
                    str = AuditUtils.hidePassword(str);
                    set(AuditEvent.TARGET_PARAMS, str);
                }

                set(AuditEvent.TARGET_HOST_ADDRESS, req.getRemoteAddr() + ":" + req.getRemotePort());

                if (req.getUserPrincipal() != null && req.getUserPrincipal().getName() != null) {
                    set(AuditEvent.TARGET_CREDENTIAL_TOKEN, req.getUserPrincipal().getName());
                    set(AuditEvent.TARGET_CREDENTIAL_TYPE, AuditEvent.CRED_TYPE_BASIC);
                }

                if (req.getSessionId() != null)
                    set(AuditEvent.TARGET_SESSION, req.getSessionId());

                set(AuditEvent.TARGET_METHOD, req.getMethod());

            } else {
                /*
                 * For non REST requests, we have to build the data that is included in the
                 * RESTRequest object
                 */

                // create the equivalent HTTP action
                if ("create".equals(action)) {
                    set(AuditEvent.TARGET_METHOD, AuditEvent.TARGET_METHOD_POST);
                } else if ("update".equals(action)) {
                    set(AuditEvent.TARGET_METHOD, AuditEvent.TARGET_METHOD_PUT);
                } else if ("delete".equals(action)) {
                    set(AuditEvent.TARGET_METHOD, AuditEvent.TARGET_METHOD_DELETE);
                } else if ("search".equals(action)) {
                    set(AuditEvent.TARGET_METHOD, AuditEvent.TARGET_METHOD_GET);
                } else if ("get".equals(action)) {
                    set(AuditEvent.TARGET_METHOD, AuditEvent.TARGET_METHOD_GET);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unknown action code: " + action);
                    }
                }

                // some items are populated on the AuditManager thread by AuditPE
                AuditManager auditManager = new AuditManager();

                set(AuditEvent.TARGET_CREDENTIAL_TOKEN, auditManager.getCredentialUser());
                set(AuditEvent.TARGET_CREDENTIAL_TYPE, auditManager.getCredentialType());

                if (auditManager.getRemoteAddr() != null) {
                    set(AuditEvent.INITIATOR_HOST_ADDRESS, auditManager.getRemoteAddr());
                }

                if (auditManager.getAgent() != null) {
                    set(AuditEvent.INITIATOR_HOST_AGENT, auditManager.getAgent());
                }

                if (auditManager.getLocalAddr() != null && auditManager.getLocalPort() != null) {
                    set(AuditEvent.TARGET_HOST_ADDRESS, auditManager.getLocalAddr() + ":" + auditManager.getLocalPort());
                }

                if (auditManager.getSessionId() != null) {
                    set(AuditEvent.TARGET_SESSION, auditManager.getSessionId());
                }

                if (auditManager.getHttpType() != null) {
                    httpType = auditManager.getHttpType();
                }

            }
            set(AuditEvent.TARGET_TYPEURI, "service/vmmservice/" + action);

            if (repositoryId != null) {
                set(AuditEvent.TARGET_REPOSITORY_ID, repositoryId);
            } else {
                set(AuditEvent.TARGET_REPOSITORY_ID, REPO_NOT_MATCHED);
            }

            if (uniqueName != null)
                set(AuditEvent.TARGET_UNIQUENAME, uniqueName);

            if (realmName != null) {
                set(AuditEvent.TARGET_REALM, realmName);
            } else {
                set(AuditEvent.TARGET_REALM, REALM_NOT_MATCHED);
            }

            if (action != null)
                set(AuditEvent.TARGET_ACTION, action);

            String qualifiedEntityType = null;
            if (root != null) {
                List<Entity> entities = root.getEntities();
                if (entities != null && !entities.isEmpty()) {
                    Entity entity = entities.get(0);
                    if (entity != null) {
                        qualifiedEntityType = entity.getTypeName();
                    }
                }
            }

            if (qualifiedEntityType == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No entity type provided, defaulting to Entity");
                }
                qualifiedEntityType = "Entity";
            }

            set(AuditEvent.TARGET_ENTITY_TYPE, qualifiedEntityType);

            if (get(AuditEvent.TARGET_NAME) == null) {
                String actionType = "Entity";
                if (qualifiedEntityType.equals("PersonAccount")) {
                    actionType = "Users";
                } else if (qualifiedEntityType.equals("Group")) {
                    actionType = "Group";
                }
                set(AuditEvent.TARGET_NAME, "/ibm/api/" + serviceType + "/" + action + "/" + actionType);
            }
            if (statusCode != null) {
                set(AuditEvent.REASON_CODE, statusCode);
                if (statusCode.intValue() == 200) {
                    setOutcome(AuditEvent.OUTCOME_SUCCESS);
                    set(AuditEvent.REASON_TYPE, httpType);
                } else {
                    setOutcome(AuditEvent.OUTCOME_FAILURE);
                    switch (statusCode) {
                        case 201: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_MISSING_ENTITY_DATA_OBJECT);
                            break;
                        }
                        case 202: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_ACTION_MULTIPLE_ENTITIES_SPECIFIED);
                            break;
                        }
                        case 203: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_ENTITY_IDENTIFIER_NOT_SPECIFIED);
                            break;
                        }
                        case 204: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_ENTITY_NOT_IN_REALM_SCOPE);
                            break;
                        }
                        case 205: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_INVALID_PARENT_UNIQUE_ID);
                            break;
                        }
                        case 206: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_DEFAULT_PARENT_NOT_FOUND);
                            break;
                        }
                        case 207: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION);
                            break;
                        }
                        case 208: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_ENTITY_TYPE_NOT_SUPPORTED);
                            break;
                        }
                        case 209: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_CANNOT_WRITE_TO_READ_ONLY_REPOSITORY);
                            break;
                        }
                        case 210: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_EXTERNAL_NAME_CONTROL_NOT_FOUND);
                            break;
                        }
                        case 211: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_INVALID_IDENTIFIER);
                            break;
                        }
                        case 212: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_ENTITY_NOT_FOUND);
                            break;
                        }
                        case 213: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_MISSING_SORT_KEY);
                            break;
                        }
                        case 214: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_MISSING_SEARCH_CONTROL);
                            break;
                        }
                        case 215: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_INCORRECT_SEARCH_LIMIT);
                            break;
                        }
                        case 216: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_CANNOT_SPECIFY_COUNT_LIMIT);
                            break;
                        }
                        case 217: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_MISSING_SEARCH_EXPRESSION);
                            break;
                        }
                        case 218: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_SEARCH_EXPRESSION_ERROR);
                            break;
                        }
                        case 219: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_INVALID_SEARCH_EXPRESSION);
                            break;
                        }
                        case 220: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_EXCEED_MAX_TOTAL_SEARCH_LIMIT);
                            break;
                        }
                        case 221: {
                            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_ENTITY_SEARCH_FAILED);
                            break;
                        }

                        default: {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Unknown statusCode: " + statusCode.intValue());
                            }
                            set(AuditEvent.REASON_TYPE, "Unknown:" + statusCode);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error creating MemberManagementEvent", e);
            }
        }
    }
}
