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

package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.wim.VMMService;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Context;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.ExternalNameControl;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.LoginAccount;
import com.ibm.wsspi.security.wim.model.LoginControl;
import com.ibm.wsspi.security.wim.model.PageControl;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.PropertyControl;
import com.ibm.wsspi.security.wim.model.Root;
import com.ibm.wsspi.security.wim.model.SearchControl;
import com.ibm.wsspi.security.wim.model.SortControl;

/**
 * Servlet to access VMM APIs. VMM APIs are called using VMM Service
 */
public class VmmServiceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Tries to resolve the VMMService
     *
     * @param writer
     * @param loader
     * @return VMMService, which may possibly be null (which is bad but
     *         if it happens, it happens).
     */
    private VMMService getVMMService(PrintWriter writer) {
        VMMService vmmServ;

        Bundle bundle = FrameworkUtil.getBundle(Servlet.class);
        if (bundle == null) {
            writer.println("Unable to determine bundle");
            return null;
        }

        BundleContext bundleContext = bundle.getBundleContext();
        if (bundleContext == null) {
            writer.println("Unable to determine bundle context");
            return null;
        }

        String serviceName = VMMService.class.getName();

        writer.println("Looking up " + serviceName);
        ServiceReference<?> ref = bundleContext.getServiceReference(serviceName);
        writer.println(serviceName + " reference is " + ref);
        vmmServ = (VMMService) bundleContext.getService(ref);

        if (vmmServ == null)
            throw new IllegalStateException("Unable to find VMMService");
        return vmmServ;
    }

    /**
     * Handles method calls which is the core purpose of this servlet.
     * There are certain test flows where exceptions may be expected.
     * Allow those to occur and capture them such that they can be
     * consumed by tests.
     *
     * @param req
     * @param pw
     * @param vmmServ VMMService instance
     * @throws NullPointerException
     * @throws RemoteException
     */
    private void handleMethodRequest(HttpServletRequest req, PrintWriter pw, VMMService vmmServ) {
        String response = null;

        String method = req.getParameter("method");
        System.out.println("Received method request: " + method);
        if (method == null) {
            response = "Method request was null";
            pw.println(response);
            pw.flush();
            return;
        }
        try {
            if ("getUser".equals(method)) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Entity entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();
                properties.getProperties().add("uid");
                properties.getProperties().add("cn");
                properties.getProperties().add("sn");
                properties.getProperties().add("mail");
                properties.getProperties().add("telephoneNumber");
                properties.getProperties().add("photoURL");
                properties.getProperties().add("photoURLThumbnail");

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.get(root);

                PersonAccount person = (PersonAccount) root.getEntities().get(0);

                Map<String, Object> props = new HashMap<String, Object>();

                props.put("uid", person.getUid());
                props.put("cn", person.getCn());
                props.put("sn", person.getSn());
                props.put("mail", person.getMail());
                props.put("telephoneNumber", person.getTelephoneNumber());
                props.put("photoURL", person.getPhotoUrl());
                props.put("photoURLThumbnail", person.getPhotoUrlThumbnail());

                response = props.toString();
            } else if (method.endsWith("InvalidUser")) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Entity entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                if (method.equals("getInvalidUser")) {
                    root = vmmServ.get(root);
                } else if (method.equals("updateInvalidUser")) {
                    root = vmmServ.update(root);
                } else if (method.equals("deleteInvalidUser")) {
                    root = vmmServ.delete(root);
                }
            } else if (method.endsWith("InvalidGroup")) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Entity entity = new Group();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                if (method.equals("getInvalidGroup")) {
                    root = vmmServ.get(root);
                } else if (method.equals("updateInvalidGroup")) {
                    root = vmmServ.update(root);
                } else if (method.equals("deleteInvalidGroup")) {
                    root = vmmServ.delete(root);
                }

            } else if ("login".equals(method)) {

                LoginAccount person = new LoginAccount();
                person.set(SchemaConstants.PROP_PRINCIPAL_NAME, req.getParameter("userName"));
                try {
                    person.set(SchemaConstants.PROP_PASSWORD, req.getParameter("password").getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    pw.println(e.getMessage());
                }

                LoginControl loginCtrl = new LoginControl();

                Root root = new Root();
                root.getEntities().add(person);
                root.getControls().add(loginCtrl);

                root = vmmServ.login(root);

                response = root.getEntities().get(0).getIdentifier().getUniqueName();
            } else if ("deleteUser".equals(method)) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Entity entity = new Entity();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();
                properties.getProperties().add("uid");
                properties.getProperties().add("cn");
                properties.getProperties().add("sn");
                properties.getProperties().add("mail");
                properties.getProperties().add("telephoneNumber");
                properties.getProperties().add("photoURL");
                properties.getProperties().add("photoURLThumbnail");

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.delete(root);

                PersonAccount person = (PersonAccount) root.getEntities().get(0);

                Map<String, Object> props = new HashMap<String, Object>();

                props.put("uid", person.getUid());
                props.put("cn", person.getCn());
                props.put("sn", person.getSn());
                props.put("mail", person.getMail());
                props.put("telephoneNumber", person.getTelephoneNumber());
                props.put("photoURL", person.getPhotoUrl());
                props.put("photoURLThumbnail", person.getPhotoUrlThumbnail());

                response = props.toString();
            } else if ("searchUserNoControl".equals(method)) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.search(root);
            } else if ("searchGroupNoControl".equals(method)) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Group entity = new Group();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.search(root);
            } else if ("searchUserBadLimit".equals(method)) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();
                SearchControl search = new SearchControl();
                search.setCountLimit(5);
                search.setSearchLimit(-5);

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                root.getControls().add(search);

                root = vmmServ.search(root);
            } else if ("searchUserNoExpression".equals(method)) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                
                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();
                SearchControl search = new SearchControl();
                search.setCountLimit(5);
                search.setSearchLimit(5);
                search.setExpression("");

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                root.getControls().add(search);

                root = vmmServ.search(root);
            } else if ("searchUserBadExpression".equals(method)) {

            	PersonAccount entity = new PersonAccount();

                PropertyControl properties = new PropertyControl();
                SearchControl search = new SearchControl();
                search.setSearchLimit(1);
                search.setExpression("@xsi:type='PersonAccount' and (uid='*')");

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                root.getControls().add(search);

                root = vmmServ.search(root);

            } else if ("searchGroupBadLimit".equals(method)) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Group entity = new Group();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();
                SearchControl search = new SearchControl();
                search.setCountLimit(5);
                search.setSearchLimit(-5);

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                root.getControls().add(search);

                root = vmmServ.search(root);

            } else if ("searchUserLimitAndPage".equals(method)) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();
                SearchControl search = new SearchControl();
                search.setCountLimit(5);
                PageControl page = new PageControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                root.getControls().add(search);
                root.getControls().add(page);

                root = vmmServ.search(root);
            } else if ("searchGroupLimitAndPage".equals(method)) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Group entity = new Group();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();
                SearchControl search = new SearchControl();
                search.setCountLimit(5);
                PageControl page = new PageControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                root.getControls().add(search);
                root.getControls().add(page);

                root = vmmServ.search(root);

            } else if ("searchUserMaxLimit".equals(method)) {

                PersonAccount entity = new PersonAccount();

                PropertyControl properties = new PropertyControl();
                SearchControl search = new SearchControl();
                search.setSearchLimit(1);
                search.setExpression("@xsi:type='PersonAccount' and (uid='*')");

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                root.getControls().add(search);

                root = vmmServ.search(root);
            } else if ("searchBasicUser".equals(method)) {

                PersonAccount entity = new PersonAccount();

                PropertyControl properties = new PropertyControl();
                SearchControl search = new SearchControl();
                search.setExpression("@xsi:type='PersonAccount' and (uid='*')");

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                root.getControls().add(search);
                root = vmmServ.search(root);

            } else if ("searchGroupMaxLimit".equals(method)) {

                Group entity = new Group();

                PropertyControl properties = new PropertyControl();
                SearchControl search = new SearchControl();
                search.setSearchLimit(1);
                search.setExpression("@xsi:type='PersonAccount' and (cn='*')");

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                root.getControls().add(search);

                root = vmmServ.search(root);

            } else if (method.endsWith("UserNoId")) {
                Entity entity = new PersonAccount();

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                if ("getUserNoId".equals(method)) {
                    root = vmmServ.get(root);
                } else if ("createUserNoId".equals(method)) {
                    root = vmmServ.create(root);
                } else if ("deleteUserNoId".equals(method)) {
                    root = vmmServ.delete(root);
                } else if ("updateUserNoId".equals(method)) {
                    root = vmmServ.update(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }

            } else if (method.endsWith("GroupNoId")) {
                Entity entity = new Group();

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                if ("getGroupNoId".equals(method)) {
                    root = vmmServ.get(root);
                } else if ("createGroupNoId".equals(method)) {
                    root = vmmServ.create(root);
                } else if ("deleteGroupNoId".equals(method)) {
                    root = vmmServ.delete(root);
                } else if ("updateGroupNoId".equals(method)) {
                    root = vmmServ.update(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }

            } else if ("createUserBadEntityType".equals(method)) {
                Entity entity = new Entity();

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.create(root);

            } else if ("createGroupBadEntityType".equals(method)) {
                Entity entity = new Entity();

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.create(root);
            } else if (method.endsWith("NoEntity")) {
                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getControls().add(properties);

                if ("createUserNoEntity".equals(method)) {
                    root = vmmServ.create(root);
                } else if ("updateUserNoEntity".equals(method)) {
                    root = vmmServ.update(root);
                } else if ("deleteUserNoEntity".equals(method)) {
                    root = vmmServ.delete(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }

            } else if (method.endsWith("UserMultiEntity")) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                IdentifierType id2 = new IdentifierType();
                id2.setUniqueName(req.getParameter("uniqueName"));

                Entity entity = new PersonAccount();
                entity.setIdentifier(id);

                Entity entity2 = new PersonAccount();
                entity2.setIdentifier(id2);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getEntities().add(entity2);
                root.getControls().add(properties);

                if ("createUserMultiEntity".equals(method)) {
                    root = vmmServ.create(root);
                } else if ("updateUserMultiEntity".equals(method)) {
                    root = vmmServ.update(root);
                } else if ("deleteUserMultiEntity".equals(method)) {
                    root = vmmServ.delete(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }
            } else if (method.endsWith("GroupMultiEntity")) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                IdentifierType id2 = new IdentifierType();
                id2.setUniqueName(req.getParameter("uniqueName"));

                Entity entity = new Group();
                entity.setIdentifier(id);

                Entity entity2 = new Group();
                entity2.setIdentifier(id2);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getEntities().add(entity2);
                root.getControls().add(properties);

                if ("createGroupMultiEntity".equals(method)) {
                    root = vmmServ.create(root);
                } else if ("updateGroupMultiEntity".equals(method)) {
                    root = vmmServ.update(root);
                } else if ("deleteGroupMultiEntity".equals(method)) {
                    root = vmmServ.delete(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }
            } else if (method.endsWith("createGroupParentNoID")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Group entity = new Group();
                entity.setIdentifier(id);
                Entity parent = new Entity();
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.create(root);
            } else if (method.endsWith("createUserParentNoID")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);
                Entity parent = new Entity();
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.create(root);
            } else if (method.endsWith("createUserParentBadDN")) {
                Entity parent = new Entity();
                IdentifierType idp = new IdentifierType();
                idp.setUniqueName(req.getParameter("uniqueName") + ",blah");
                parent.setIdentifier(idp);

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.create(root);
            } else if (method.equals("getUserNoIDSettings")) {

                IdentifierType id = new IdentifierType();

                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.get(root);
            } else if (method.equals("getGroupNoIDSettings")) {

                IdentifierType id = new IdentifierType();

                Group entity = new Group();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.get(root);
            } else if (method.equals("getUserExtID")) {

                IdentifierType id = new IdentifierType();
                id.setExternalName(req.getParameter("uniqueName"));

                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.get(root);
            } else if (method.equals("getGroupExtID")) {

                IdentifierType id = new IdentifierType();
                id.setExternalName(req.getParameter("uniqueName"));

                Group entity = new Group();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.get(root);

            } else if (method.endsWith("createGroupParentBadDN")) {
                Entity parent = new Entity();
                IdentifierType idp = new IdentifierType();
                idp.setUniqueName(req.getParameter("uniqueName") + ",blah");
                parent.setIdentifier(idp);

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                Group entity = new Group();
                entity.setIdentifier(id);
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.create(root);
            } else if (method.endsWith("GroupCrossRepo")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                Group entity = new Group();
                entity.setIdentifier(id);
                entity.getSuperTypes().add("Group");

                IdentifierType id2 = new IdentifierType();
                id2.setUniqueName(req.getParameter("memberName"));
                id2.setUniqueId(req.getParameter("memberName"));
                id2.setRepositoryId("ldap2");
                PersonAccount member = new PersonAccount();
                member.setIdentifier(id2);
                entity.set("members", member);

                Group parent = new Group();
                IdentifierType idp = new IdentifierType();
                idp.setUniqueName(req.getParameter("uniqueName"));
                parent.setIdentifier(idp);
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                if (method.equals("createGroupCrossRepo")) {
                    root = vmmServ.create(root);
                } else if (method.equals("updateGroupCrossRepo")) {
                    root = vmmServ.update(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }
            } else if (method.endsWith("GroupMemberBadDN")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                Group entity = new Group();
                entity.setIdentifier(id);
                entity.getSuperTypes().add("Group");

                IdentifierType id2 = new IdentifierType();
                id2.setUniqueName(req.getParameter("memberName"));
                if (method.equals("createGroupMemberBadDN")) {
                    id2.setUniqueId(req.getParameter("memberName"));
                }
                PersonAccount member = new PersonAccount();
                member.setIdentifier(id2);
                entity.set("members", member);

                Group parent = new Group();
                IdentifierType idp = new IdentifierType();
                idp.setUniqueName(req.getParameter("uniqueName"));
                parent.setIdentifier(idp);
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                if (method.equals("createGroupMemberBadDN")) {
                    root = vmmServ.create(root);
                } else if (method.equals("updateGroupMemberBadDN")) {
                    root = vmmServ.update(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }

            } else if (method.endsWith("UserCrossRepo")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);
                entity.setUid(req.getParameter("shortName"));
                entity.setCn(req.getParameter("shortName"));
                entity.setSn(req.getParameter("shortName"));

                IdentifierType id2 = new IdentifierType();
                id2.setUniqueName(req.getParameter("groupName"));
                id2.setUniqueId(req.getParameter("groupName"));
                id2.setRepositoryId("ldap2");
                Group member = new Group();
                member.setIdentifier(id2);
                entity.set("groups", member);

                Entity parent = new Entity();
                IdentifierType idp = new IdentifierType();
                idp.setUniqueName(req.getParameter("uniqueName"));
                parent.setIdentifier(idp);
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                if (method.endsWith("createUserCrossRepo")) {
                    root = vmmServ.create(root);
                } else if (method.endsWith("updateUserCrossRepo")) {
                    root = vmmServ.update(root);
                }
            } else if (method.endsWith("UserGroupBadDN")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);
                entity.setUid(req.getParameter("shortName"));
                entity.setCn(req.getParameter("shortName"));
                entity.setSn(req.getParameter("shortName"));

                IdentifierType id2 = new IdentifierType();
                id2.setUniqueName(req.getParameter("groupName"));
                if (method.endsWith("createUserGroupBadDN")) {
                    id2.setUniqueId(req.getParameter("groupName"));
                }
                Group member = new Group();
                member.setIdentifier(id2);
                entity.set("groups", member);

                Entity parent = new Entity();
                IdentifierType idp = new IdentifierType();
                idp.setUniqueName(req.getParameter("uniqueName"));
                parent.setIdentifier(idp);
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                if (method.endsWith("createUserGroupBadDN")) {
                    root = vmmServ.create(root);
                } else if (method.endsWith("updateUserGroupBadDN")) {
                    root = vmmServ.update(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }
            } else if (method.endsWith("createUserParentBadUniqueId")) {
                Entity parent = new Entity();
                IdentifierType idp = new IdentifierType();
                idp.setUniqueName(req.getParameter("parentName"));
                idp.setUniqueId("blah");
                parent.setIdentifier(idp);

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.create(root);
            } else if (method.endsWith("createGroupParentBadUniqueId")) {
                Entity parent = new Entity();
                IdentifierType idp = new IdentifierType();
                idp.setUniqueName(req.getParameter("parentName"));
                idp.setUniqueId("blah");
                parent.setIdentifier(idp);

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                Group entity = new Group();
                entity.setIdentifier(id);
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.create(root);
            } else if (method.endsWith("createUserParentWrongDN")) {
                Entity parent = new Entity();
                IdentifierType idp = new IdentifierType();
                idp.setUniqueName(req.getParameter("parentName"));
                parent.setIdentifier(idp);

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.create(root);
            } else if (method.endsWith("createGroupParentWrongDN")) {
                Entity parent = new Entity();
                IdentifierType idp = new IdentifierType();
                idp.setUniqueName(req.getParameter("parentName"));
                parent.setIdentifier(idp);

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                Group entity = new Group();
                entity.setIdentifier(id);
                entity.setParent(parent);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.create(root);
            } else if (method.equals("getUserNotFound")) {
                IdentifierType id = new IdentifierType();
                id.setExternalName(req.getParameter("uniqueName"));

                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                ExternalNameControl eCtrl = new ExternalNameControl();
                root.getControls().add(eCtrl);

                Context c = new Context();
                c.setKey("trustEntityType");
                c.setValue("true");

                root.getContexts().add(c);

                root = vmmServ.get(root);
            } else if (method.equals("getBasicUserNotFound")) {
                IdentifierType id = new IdentifierType();
                 id.setUniqueId(req.getParameter("uniqueName"));
                id.setRepositoryId("basic");
                

                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                ExternalNameControl eCtrl = new ExternalNameControl();
                root.getControls().add(eCtrl);

                Context c = new Context();
                root.getContexts().add(c);

                root = vmmServ.get(root);
            } else if (method.endsWith("BasicUser")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                id.setExternalId("vmmuser1");
                
                if (method.equals("getBasicUser") || (method.equals("updateBasicUser"))) {
                	id.setRepositoryId("SampleBasicRealm");
                }
                
                if (method.equals("updateBasicUser")) {
                	id.setUniqueName("uid=vmmuser1,o=SampleBasicRealm");
                	id.setUniqueId(req.getParameter("uniqueName"));
                    id.setExternalName("vmmuser1");
                    
                }

                PersonAccount entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                ExternalNameControl eCtrl = new ExternalNameControl();
                root.getControls().add(eCtrl);
                
                Context c = new Context();
                
                if (method.equals("getBasicUser")) {
                	root = vmmServ.get(root);
                } else if (method.equals("createBasicUser")) {
                	root = vmmServ.create(root);
                } else if (method.equals("deleteBasicUser")) {
                	root = vmmServ.delete(root);
                } else if (method.equals("updateBasicUser")) {
                	
                    c.setKey("trustEntityType");
                    c.setValue("true");

                    root.getContexts().add(c);
                	root = vmmServ.update(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }

            } else if (method.equals("getGroupNotFound")) {
                IdentifierType id = new IdentifierType();
                id.setExternalName(req.getParameter("uniqueName"));

                Group entity = new Group();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                ExternalNameControl eCtrl = new ExternalNameControl();
                root.getControls().add(eCtrl);

                Context c = new Context();
                c.setKey("trustEntityType");
                c.setValue("true");

                root.getContexts().add(c);

                root = vmmServ.get(root);

            } else if (method.equals("getUserNoSortControl")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Entity entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);
                SortControl sortCtrl = new SortControl();
                root.getControls().add(sortCtrl);

                Context c = new Context();
                c.setKey("trustEntityType");
                c.setValue("true");
                root.getContexts().add(c);

                root = vmmServ.get(root);
            } else if (method.equals("getGroupNoSortControl")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Entity entity = new Group();
                entity.setIdentifier(id);

                Root root = new Root();
                root.getEntities().add(entity);
                SortControl sortCtrl = new SortControl();
                root.getControls().add(sortCtrl);

                Context c = new Context();
                c.setKey("trustEntityType");
                c.setValue("true");
                root.getContexts().add(c);

                root = vmmServ.get(root);

            } else if (method.endsWith("UserBadDN")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                id.setExternalId("extIDtemp");
                id.setExternalName("extNametemp");
                id.setRepositoryId("repIDtemp");

                Entity entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                if ("updateUserBadDN".equals(method)) {
                    root = vmmServ.update(root);
                } else if ("deleteUserBadDN".equals(method)) {
                    root = vmmServ.delete(root);
                } else if ("getUserBadDN".equals(method)) {
                    Context c = new Context();
                    c.setKey("trustEntityType");
                    c.setValue("true");
                    root.getContexts().add(c);

                    root = vmmServ.get(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }
            } else if (method.endsWith("UserBadDN_NoRepo")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                id.setExternalId("extIDtemp");
                id.setExternalName("extNametemp");

                Entity entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                if ("updateUserBadDN_NoRepo".equals(method)) {
                    root = vmmServ.update(root);
                } else if ("deleteUserBadDN_NoRepo".equals(method)) {
                    root = vmmServ.delete(root);
                } else if ("getUserBadDN_NoRepo".equals(method)) {
                    Context c = new Context();
                    c.setKey("trustEntityType");
                    c.setValue("true");
                    root.getContexts().add(c);

                    root = vmmServ.get(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }
            } else if (method.endsWith("GroupBadDN_NoRepo")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                id.setExternalId("extIDtemp");
                id.setExternalName("extNametemp");

                Entity entity = new PersonAccount();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                if ("updateGroupBadDN_NoRepo".equals(method)) {
                    root = vmmServ.update(root);
                } else if ("deleteGroupBadDN_NoRepo".equals(method)) {
                    root = vmmServ.delete(root);
                } else if ("getUserGroupDN_NoRepo".equals(method)) {
                    Context c = new Context();
                    c.setKey("trustEntityType");
                    c.setValue("true");
                    root.getContexts().add(c);

                    root = vmmServ.get(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }

            } else if (method.endsWith("GroupBadDN")) {
                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));
                id.setExternalId("extIDtemp");
                id.setExternalName("extNametemp");
                id.setRepositoryId("repIDtemp");

                Entity entity = new Group();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                if ("updateGroupBadDN".equals(method)) {
                    root = vmmServ.update(root);
                } else if ("deleteGroupBadDN".equals(method)) {
                    root = vmmServ.delete(root);
                } else if ("getGroupBadDN".equals(method)) {
                    root = vmmServ.get(root);
                } else {
                    pw.println("Unknown method name: " + method);
                }
            } else if ("ping".equals(method)) {
                response = "PING";
            } else {
                pw.println("Bad method name: " + method + " .Usage: url?method=name&paramName=paramValue&...");
            }
        } catch (

        NullPointerException npe) {
            pw.println("Hit NPE");
            npe.printStackTrace();
            response = npe.toString();
        } catch (WIMException we) {
            pw.println("Hit WIMException");
            we.printStackTrace();
            response = we.toString();
        } catch (Exception e) {
            pw.println("Hit Exception");
            e.printStackTrace();
            response = e.toString();
        }
        pw.println("Result from method: " + method);
        pw.println(response);
        pw.flush();
    }

    /**
     * {@inheritDoc} GET handles method requests and calls them against the
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        try {
            handleMethodRequest(req, pw, getVMMService(pw));
        } catch (IllegalArgumentException e) {
            e.printStackTrace(pw);
            pw.println("getVMMService exception message:");
            pw.println(e);
            pw.flush();
        } catch (Exception e) {
            pw.println("Unexpected Exception during processing:");
            e.printStackTrace(pw);
        }
        pw.flush();
        pw.close();
    }

    /**
     * {@inheritDoc} POST does nothing for this servlet.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.print("use GET method");
    }

}
