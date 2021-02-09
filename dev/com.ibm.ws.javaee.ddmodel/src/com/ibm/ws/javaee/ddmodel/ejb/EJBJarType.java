/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.InterceptorCallback;
import com.ibm.ws.javaee.dd.common.LifecycleCallback;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.RunAs;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.dd.common.SecurityRoleRef;
import com.ibm.ws.javaee.dd.ejb.AccessTimeout;
import com.ibm.ws.javaee.dd.ejb.ActivationConfig;
import com.ibm.ws.javaee.dd.ejb.ActivationConfigProperty;
import com.ibm.ws.javaee.dd.ejb.ApplicationException;
import com.ibm.ws.javaee.dd.ejb.AssemblyDescriptor;
import com.ibm.ws.javaee.dd.ejb.AsyncMethod;
import com.ibm.ws.javaee.dd.ejb.CMPField;
import com.ibm.ws.javaee.dd.ejb.CMRField;
import com.ibm.ws.javaee.dd.ejb.ConcurrentMethod;
import com.ibm.ws.javaee.dd.ejb.ContainerTransaction;
import com.ibm.ws.javaee.dd.ejb.DependsOn;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.EJBRelation;
import com.ibm.ws.javaee.dd.ejb.EJBRelationshipRole;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.Entity;
import com.ibm.ws.javaee.dd.ejb.ExcludeList;
import com.ibm.ws.javaee.dd.ejb.InitMethod;
import com.ibm.ws.javaee.dd.ejb.Interceptor;
import com.ibm.ws.javaee.dd.ejb.InterceptorBinding;
import com.ibm.ws.javaee.dd.ejb.InterceptorOrder;
import com.ibm.ws.javaee.dd.ejb.Interceptors;
import com.ibm.ws.javaee.dd.ejb.MessageDriven;
import com.ibm.ws.javaee.dd.ejb.Method;
import com.ibm.ws.javaee.dd.ejb.MethodPermission;
import com.ibm.ws.javaee.dd.ejb.NamedMethod;
import com.ibm.ws.javaee.dd.ejb.Query;
import com.ibm.ws.javaee.dd.ejb.QueryMethod;
import com.ibm.ws.javaee.dd.ejb.RelationshipRoleSource;
import com.ibm.ws.javaee.dd.ejb.Relationships;
import com.ibm.ws.javaee.dd.ejb.RemoveMethod;
import com.ibm.ws.javaee.dd.ejb.SecurityIdentity;
import com.ibm.ws.javaee.dd.ejb.Session;
import com.ibm.ws.javaee.dd.ejb.StatefulTimeout;
import com.ibm.ws.javaee.dd.ejb.Timeout;
import com.ibm.ws.javaee.dd.ejb.Timer;
import com.ibm.ws.javaee.dd.ejb.TimerSchedule;
import com.ibm.ws.javaee.ddmodel.BooleanType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.TokenType;
import com.ibm.ws.javaee.ddmodel.common.DescribableType;
import com.ibm.ws.javaee.ddmodel.common.DescriptionGroup;
import com.ibm.ws.javaee.ddmodel.common.DescriptionType;
import com.ibm.ws.javaee.ddmodel.common.DisplayNameType;
import com.ibm.ws.javaee.ddmodel.common.IconType;
import com.ibm.ws.javaee.ddmodel.common.JNDIEnvironmentRefsGroup;
import com.ibm.ws.javaee.ddmodel.common.MessageDestinationType;
import com.ibm.ws.javaee.ddmodel.common.RunAsType;
import com.ibm.ws.javaee.ddmodel.common.SecurityRoleRefType;
import com.ibm.ws.javaee.ddmodel.common.SecurityRoleType;
import com.ibm.ws.javaee.ddmodel.common.XSDBooleanType;
import com.ibm.ws.javaee.ddmodel.common.XSDIntegerType;
import com.ibm.ws.javaee.ddmodel.common.XSDStringType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:element name="ejb-jar"
 type="javaee:ejb-jarType">
 <xsd:key name="ejb-name-key">
 <xsd:selector xpath="javaee:enterprise-beans/*"/>
 <xsd:field xpath="javaee:ejb-name"/>
 </xsd:key>
 <xsd:keyref name="ejb-name-references"
 refer="javaee:ejb-name-key">
 <xsd:selector xpath=".//javaee:ejb-relationship-role/javaee:relationship-role-source"/>
 <xsd:field xpath="javaee:ejb-name"/>
 </xsd:keyref>
 <xsd:key name="role-name-key">
 <xsd:selector xpath="javaee:assembly-descriptor/javaee:security-role"/>
 <xsd:field xpath="javaee:role-name"/>
 </xsd:key>
 <xsd:keyref name="role-name-references"
 refer="javaee:role-name-key">
 */
//      <xsd:selector xpath="javaee:enterprise-beans/*/javaee:security-role-ref"/>
/*
 <xsd:field xpath="javaee:role-link"/>
 </xsd:keyref>
 </xsd:element>

 <xsd:complexType name="ejb-jarType">
 <xsd:sequence>
 <xsd:element name="module-name"
 type="javaee:string"
 minOccurs="0"/>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="enterprise-beans"
 type="javaee:enterprise-beansType"
 minOccurs="0"/>
 <xsd:element name="interceptors"
 type="javaee:interceptorsType"
 minOccurs="0"/>
 <xsd:element name="relationships"
 type="javaee:relationshipsType"
 minOccurs="0">
 <xsd:unique name="relationship-name-uniqueness">
 <xsd:selector xpath="javaee:ejb-relation"/>
 <xsd:field xpath="javaee:ejb-relation-name"/>
 </xsd:unique>
 </xsd:element>
 <xsd:element name="assembly-descriptor"
 type="javaee:assembly-descriptorType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="ejb-client-jar"
 type="javaee:pathType"
 minOccurs="0">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="version"
 type="javaee:dewey-versionType"
 fixed="3.1"
 use="required">
 </xsd:attribute>
 <xsd:attribute name="metadata-complete"
 type="xsd:boolean">
 </xsd:attribute>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */
public class EJBJarType extends DescriptionGroup implements DeploymentDescriptor, EJBJar, DDParser.RootParsable {
    public EJBJarType(String path) {
        this.path = path;
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return path;
    }

    @Override
    public Object getComponentForId(String id) {
        return idMap.getComponentForId(id);
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return idMap.getIdForComponent(ddComponent);
    }

    @Override
    public String getModuleName() {
        return module_name != null ? module_name.getValue() : null;
    }

//    @Override
//    public boolean isSetMetadataComplete() {
//        return AnySimpleType.isSet(metadata_complete);
//    }

    @Override
    public boolean isMetadataComplete() {
        return metadata_complete != null && metadata_complete.getBooleanValue();
    }

//    @Override
//    public String getVersion() {
//        return version.getValue();
//    }

    @Override
    public int getVersionID() {
        return versionId;
    }

    @Override
    public List<EnterpriseBean> getEnterpriseBeans() {
        if (enterprise_beans != null) {
            return enterprise_beans.getEnterpriseBeans();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Interceptors getInterceptors() {
        return interceptors;
    }

//    @Override
//    public Relationships getRelationships() {
//        return relationships;
//    }

    @Override
    public Relationships getRelationshipList() {
        return relationships;
    }

    @Override
    public AssemblyDescriptor getAssemblyDescriptor() {
        return assembly_descriptor;
    }

    @Override
    public String getEjbClientJar() {
        return ejb_client_jar != null ? ejb_client_jar.getValue() : null;
    }

    String path;
    int versionId;
    // attributes
    TokenType version;
    BooleanType metadata_complete;
    // elements
    IconType compatIcon;
    XSDTokenType module_name;
    EnterpriseBeansType enterprise_beans;
    InterceptorsType interceptors;
    RelationshipsType relationships;
    AssemblyDescriptorType assembly_descriptor;
    XSDTokenType ejb_client_jar;

    // key ejb-name-key
    // <xsd:selector xpath="javaee:enterprise-beans/*"/>
    // <xsd:field xpath="javaee:ejb-name"/>
    // keyref ejb-name-references refer="javaee:ejb-name-key"
    // <xsd:selector xpath=".//javaee:ejb-relationship-role/javaee:relationship-role-source"/>
    // <xsd:field xpath="javaee:ejb-name"/>
    // key role-name-key
    // <xsd:selector xpath="javaee:assembly-descriptor/javaee:security-role"/>
    // <xsd:field xpath="javaee:role-name"/>
    // keyref role-name-references refer="javaee:role-name-key"
    // <xsd:selector xpath="javaee:enterprise-beans/*/javaee:security-role-ref"/>
    // <xsd:field xpath="javaee:role-link"/>

    // Component ID map
    DDParser.ComponentIDMap idMap;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (nsURI == null) {
            if (parser.version >= 21 && "version".equals(localName)) {
                version = parser.parseTokenAttributeValue(index);
                return true;
            }
            if (parser.version >= 30 && "metadata-complete".equals(localName)) {
                metadata_complete = parser.parseBooleanAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public void finish(DDParser parser) throws ParseException {
        super.finish(parser);
        if (version == null) {
            if (parser.version < 21) {
                version = parser.parseToken(parser.version == 11 ? "1.1" : "2.0");
            } else {
                throw new ParseException(parser.requiredAttributeMissing("version"));
            }
        }
        this.versionId = parser.version;
        this.idMap = parser.idMap;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if (parser.version < EJBJar.VERSION_2_1 && ("small-icon".equals(localName) || "large-icon".equals(localName))) {
            if (compatIcon == null) {
                compatIcon = new IconType();
                addIcon(compatIcon);
            }
            return compatIcon.handleChild(parser, localName);
        }
        if ("module-name".equals(localName)) {
            XSDTokenType module_name = new XSDTokenType();
            parser.parse(module_name);
            this.module_name = module_name;
            return true;
        }
        if ("enterprise-beans".equals(localName)) {
            EnterpriseBeansType enterprise_beans = new EnterpriseBeansType();
            parser.parse(enterprise_beans);
            this.enterprise_beans = enterprise_beans;
            return true;
        }
        if ("interceptors".equals(localName)) {
            InterceptorsType interceptors = new InterceptorsType();
            parser.parse(interceptors);
            this.interceptors = interceptors;
            return true;
        }
        if ("relationships".equals(localName)) {
            RelationshipsType relationships = new RelationshipsType();
            parser.parse(relationships);
            this.relationships = relationships;
            return true;
        }
        if ("assembly-descriptor".equals(localName)) {
            AssemblyDescriptorType assembly_descriptor = new AssemblyDescriptorType();
            parser.parse(assembly_descriptor);
            this.assembly_descriptor = assembly_descriptor;
            return true;
        }
        if ("ejb-client-jar".equals(localName)) {
            XSDTokenType ejb_client_jar = new XSDTokenType();
            parser.parse(ejb_client_jar);
            this.ejb_client_jar = ejb_client_jar;
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("version", version);
        diag.describeIfSet("metadata-complete", metadata_complete);
        diag.describeIfSet("module-name", module_name);
        super.describe(diag);
        diag.describeIfSet("enterprise-beans", enterprise_beans);
        diag.describeIfSet("interceptors", interceptors);
        diag.describeIfSet("relationships", relationships);
        diag.describeIfSet("assembly-descriptor", assembly_descriptor);
        diag.describeIfSet("ejb-client-jar", ejb_client_jar);
    }

    @Override
    protected String toTracingSafeString() {
        return "ejb-jar";
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }

    /*
     * <xsd:complexType name="enterprise-beansType">
     * <xsd:choice maxOccurs="unbounded">
     * <xsd:element name="session"
     * type="javaee:session-beanType">
     * <xsd:unique name="session-ejb-local-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:ejb-local-ref"/>
     * <xsd:field xpath="javaee:ejb-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="session-ejb-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:ejb-ref"/>
     * <xsd:field xpath="javaee:ejb-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="session-resource-env-ref-uniqueness">
     * <xsd:selector xpath="javaee:resource-env-ref"/>
     * <xsd:field xpath="javaee:resource-env-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="session-message-destination-ref-uniqueness">
     * <xsd:selector xpath="javaee:message-destination-ref"/>
     * <xsd:field xpath="javaee:message-destination-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="session-res-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:resource-ref"/>
     * <xsd:field xpath="javaee:res-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="session-env-entry-name-uniqueness">
     * <xsd:selector xpath="javaee:env-entry"/>
     * <xsd:field xpath="javaee:env-entry-name"/>
     * </xsd:unique>
     * </xsd:element>
     * <xsd:element name="entity"
     * type="javaee:entity-beanType">
     * <xsd:unique name="entity-ejb-local-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:ejb-local-ref"/>
     * <xsd:field xpath="javaee:ejb-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="entity-ejb-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:ejb-ref"/>
     * <xsd:field xpath="javaee:ejb-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="entity-resource-env-ref-uniqueness">
     * <xsd:selector xpath="javaee:resource-env-ref"/>
     * <xsd:field xpath="javaee:resource-env-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="entity-message-destination-ref-uniqueness">
     * <xsd:selector xpath="javaee:message-destination-ref"/>
     * <xsd:field xpath="javaee:message-destination-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="entity-res-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:resource-ref"/>
     * <xsd:field xpath="javaee:res-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="entity-env-entry-name-uniqueness">
     * <xsd:selector xpath="javaee:env-entry"/>
     * <xsd:field xpath="javaee:env-entry-name"/>
     * </xsd:unique>
     * </xsd:element>
     * <xsd:element name="message-driven"
     * type="javaee:message-driven-beanType">
     * <xsd:unique name="messaged-ejb-local-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:ejb-local-ref"/>
     * <xsd:field xpath="javaee:ejb-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="messaged-ejb-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:ejb-ref"/>
     * <xsd:field xpath="javaee:ejb-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="messaged-resource-env-ref-uniqueness">
     * <xsd:selector xpath="javaee:resource-env-ref"/>
     * <xsd:field xpath="javaee:resource-env-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="messaged-message-destination-ref-uniqueness">
     * <xsd:selector xpath="javaee:message-destination-ref"/>
     * <xsd:field xpath="javaee:message-destination-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="messaged-res-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:resource-ref"/>
     * <xsd:field xpath="javaee:res-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="messaged-env-entry-name-uniqueness">
     * <xsd:selector xpath="javaee:env-entry"/>
     * <xsd:field xpath="javaee:env-entry-name"/>
     * </xsd:unique>
     * </xsd:element>
     * </xsd:choice>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class EnterpriseBeansType extends DDParser.ElementContentParsable {

        List<EnterpriseBean> getEnterpriseBeans() {
            if (enterprise_beans != null) {
                return enterprise_beans;
            } else {
                return Collections.emptyList();
            }
        }

        List<EnterpriseBean> enterprise_beans;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("session".equals(localName)) {
                SessionBeanType session = new SessionBeanType();
                parser.parse(session);
                addEnterpriseBean(session);
                return true;
            }
            if ("entity".equals(localName)) {
                EntityBeanType entity = new EntityBeanType();
                parser.parse(entity);
                addEnterpriseBean(entity);
                return true;
            }
            if ("message-driven".equals(localName)) {
                MessageDrivenBeanType message_driven = new MessageDrivenBeanType();
                parser.parse(message_driven);
                addEnterpriseBean(message_driven);
                return true;
            }
            return false;
        }

        private void addEnterpriseBean(EnterpriseBeanType enterprise_bean) {
            if (this.enterprise_beans == null) {
                this.enterprise_beans = new ArrayList<EnterpriseBean>();
            }
            this.enterprise_beans.add(enterprise_bean);
        }

        @Override
        public void describe(Diagnostics diag) {
            if (enterprise_beans != null) {
                diag.append("[(");
                for (EnterpriseBean eBean : enterprise_beans) {
                    switch (eBean.getKindValue()) {
                        case EnterpriseBean.KIND_SESSION:
                            diag.describe("session", (EnterpriseBeanType) eBean);
                            break;
                        case EnterpriseBean.KIND_ENTITY:
                            diag.describe("entity", (EnterpriseBeanType) eBean);
                            break;
                        case EnterpriseBean.KIND_MESSAGE_DRIVEN:
                            diag.describe("message-driven", (EnterpriseBeanType) eBean);
                            break;
                        default:
                            break;
                    }
                }
                diag.append(")]");
            }
        }
    }

    /*
     * <xsd:unique name="{session,entity,messaged}-ejb-local-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:ejb-local-ref"/>
     * <xsd:field xpath="javaee:ejb-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="{session,entity,messaged}-ejb-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:ejb-ref"/>
     * <xsd:field xpath="javaee:ejb-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="{session,entity,messaged}-resource-env-ref-uniqueness">
     * <xsd:selector xpath="javaee:resource-env-ref"/>
     * <xsd:field xpath="javaee:resource-env-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="{session,entity,messaged}-message-destination-ref-uniqueness">
     * <xsd:selector xpath="javaee:message-destination-ref"/>
     * <xsd:field xpath="javaee:message-destination-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="{session,entity,messaged}-res-ref-name-uniqueness">
     * <xsd:selector xpath="javaee:resource-ref"/>
     * <xsd:field xpath="javaee:res-ref-name"/>
     * </xsd:unique>
     * <xsd:unique name="{session,entity,messaged}-env-entry-name-uniqueness">
     * <xsd:selector xpath="javaee:env-entry"/>
     * <xsd:field xpath="javaee:env-entry-name"/>
     * </xsd:unique>
     *
     * <xsd:complexType name="enterprise-beanType">
     * <xsd:sequence>
     * <xsd:group ref="javaee:descriptionGroup"/>
     * <xsd:element name="ejb-name"
     * type="javaee:ejb-nameType"/>
     * <xsd:element name="mapped-name"
     * type="javaee:xsdStringType"
     * minOccurs="0"/>
     * <xsd:element name="ejb-class"
     * type="javaee:ejb-classType"
     * minOccurs="0"> // NOTE: not optional for Entity beans
     * </xsd:element>
     * <xsd:group ref="javaee:jndiEnvironmentRefsGroup"/>
     * <xsd:element name="security-role-ref"
     * type="javaee:security-role-refType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="security-identity"
     * type="javaee:security-identityType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static abstract class EnterpriseBeanType extends JNDIEnvironmentRefsGroup implements EnterpriseBean {

        @Override
        public List<Description> getDescriptions() {
            if (description != null) {
                return description.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<DisplayName> getDisplayNames() {
            if (display_name != null) {
                return display_name.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<Icon> getIcons() {
            if (icon != null) {
                return icon.getList();
            } else {
                return Collections.emptyList();
            }
        }

        /**
         * @return &lt;ejb-name>
         */
        @Override
        public String getName() {
            return ejb_name.getValue();
        }

        /**
         * @return &lt;mapped-name>, or null if unspecified
         */
        @Override
        public String getMappedName() {
            return mapped_name != null ? mapped_name.getValue() : null;
        }

        /**
         * @return &lt;ejb-class>, or null if unspecified
         */
        @Override
        public String getEjbClassName() {
            return ejb_class != null ? ejb_class.getValue() : null;
        }

        /**
         * @return &lt;security-role-ref> as a read-only list
         */
        @Override
        public List<SecurityRoleRef> getSecurityRoleRefs() {
            if (security_role_ref != null) {
                return security_role_ref.getList();
            } else {
                return Collections.emptyList();
            }
        }

        /**
         * @return &lt;security-identity>, or null if unspecified
         */
        @Override
        public SecurityIdentity getSecurityIdentity() {
            return security_identity;
        }

        /**
         * @return the kind of enterprise bean represented by this object
         */
        @Override
        public int getKindValue() {
            return beanKind;
        }

        // elements
        DescriptionType.ListType description;
        DisplayNameType.ListType display_name;
        IconType.ListType icon;
        IconType compatIcon;
        XSDTokenType ejb_name = new XSDTokenType();
        XSDStringType mapped_name;
        XSDTokenType ejb_class;
        SecurityRoleRefType.ListType security_role_ref;
        SecurityIdentityType security_identity;

        final int beanKind;

        EnterpriseBeanType(int beanKind) {
            this.beanKind = beanKind;
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("description".equals(localName)) {
                DescriptionType description = new DescriptionType();
                parser.parse(description);
                addDescription(description);
                return true;
            }
            if ("display-name".equals(localName)) {
                DisplayNameType display_name = new DisplayNameType();
                parser.parse(display_name);
                addDisplayName(display_name);
                return true;
            }
            if (parser.version < 21 && ("small-icon".equals(localName) || "large-icon".equals(localName))) {
                if (compatIcon == null) {
                    compatIcon = new IconType();
                    addIcon(compatIcon);
                }
                return compatIcon.handleChild(parser, localName);
            }
            if ("icon".equals(localName)) {
                IconType icon = new IconType();
                parser.parse(icon);
                addIcon(icon);
                return true;
            }
            if ("ejb-name".equals(localName)) {
                parser.parse(ejb_name);
                return true;
            }
            if ("mapped-name".equals(localName)) {
                XSDStringType mapped_name = new XSDStringType();
                parser.parse(mapped_name);
                this.mapped_name = mapped_name;
                return true;
            }
            if ("ejb-class".equals(localName)) {
                XSDTokenType ejb_class = new XSDTokenType();
                parser.parse(ejb_class);
                this.ejb_class = ejb_class;
                return true;
            }
            if ("security-role-ref".equals(localName)) {
                SecurityRoleRefType security_role_ref = new SecurityRoleRefType();
                parser.parse(security_role_ref);
                addSecurityRoleRef(security_role_ref);
                return true;
            }
            if ("security-identity".equals(localName)) {
                SecurityIdentityType security_identity = new SecurityIdentityType();
                parser.parse(security_identity);
                this.security_identity = security_identity;
                return true;
            }
            return false;
        }

        private void addDescription(DescriptionType description) {
            if (this.description == null) {
                this.description = new DescriptionType.ListType();
            }
            this.description.add(description);
        }

        private void addDisplayName(DisplayNameType display_name) {
            if (this.display_name == null) {
                this.display_name = new DisplayNameType.ListType();
            }
            this.display_name.add(display_name);
        }

        private void addIcon(IconType icon) {
            if (this.icon == null) {
                this.icon = new IconType.ListType();
            }
            this.icon.add(icon);
        }

        private void addSecurityRoleRef(SecurityRoleRefType security_role_ref) {
            if (this.security_role_ref == null) {
                this.security_role_ref = new SecurityRoleRefType.ListType();
            }
            this.security_role_ref.add(security_role_ref);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("description", description);
            diag.describeIfSet("display-name", display_name);
            diag.describeIfSet("icon", icon);
            diag.describe("ejb-name", ejb_name);
            diag.describeIfSet("mapped-name", mapped_name);
            diag.describeIfSet("ejb-class", ejb_class);
            super.describe(diag);
            diag.describeIfSet("security-role-ref", security_role_ref);
            diag.describeIfSet("security-identity", security_identity);
        }
    }

    /*
     * <xsd:complexType name="security-identityType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:choice>
     * <xsd:element name="use-caller-identity"
     * type="javaee:emptyType">
     * </xsd:element>
     * <xsd:element name="run-as"
     * type="javaee:run-asType"/>
     * </xsd:choice>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class SecurityIdentityType extends DescribableType implements SecurityIdentity {
        @Override
        public boolean isUseCallerIdentity() {
            return use_caller_identity != null;
        }

        @Override
        public RunAs getRunAs() {
            return run_as;
        }

        // elements
        EmptyType use_caller_identity;
        RunAsType run_as;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("use-caller-identity".equals(localName)) {
                EmptyType use_caller_identity = new EmptyType();
                parser.parse(use_caller_identity);
                this.use_caller_identity = use_caller_identity;
                return true;
            }
            if ("run-as".equals(localName)) {
                RunAsType run_as = new RunAsType();
                parser.parse(run_as);
                this.run_as = run_as;
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("use-caller-identity", use_caller_identity);
            diag.describeIfSet("run-as", run_as);
        }
    }

    /*
     * <xsd:complexType name="emptyType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class EmptyType extends DDParser.ElementContentParsable {

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            // EMPTY
        }
    }

    /*
     * <xsd:complexType name="session-beanType">
     * <xsd:sequence>
     * <xsd:element name="home"
     * type="javaee:homeType"
     * minOccurs="0"/>
     * <xsd:element name="remote"
     * type="javaee:remoteType"
     * minOccurs="0"/>
     * <xsd:element name="local-home"
     * type="javaee:local-homeType"
     * minOccurs="0"/>
     * <xsd:element name="local"
     * type="javaee:localType"
     * minOccurs="0"/>
     * <xsd:element name="business-local"
     * type="javaee:fully-qualified-classType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="business-remote"
     * type="javaee:fully-qualified-classType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="local-bean"
     * type="javaee:emptyType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="service-endpoint"
     * type="javaee:fully-qualified-classType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="session-type"
     * type="javaee:session-typeType"
     * minOccurs="0"/>
     * <xsd:element name="stateful-timeout"
     * type="javaee:stateful-timeoutType"
     * minOccurs="0"/>
     * <xsd:element name="timeout-method"
     * type="javaee:named-methodType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="timer"
     * type="javaee:timerType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="init-on-startup"
     * type="javaee:true-falseType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="concurrency-management-type"
     * type="javaee:concurrency-management-typeType"
     * minOccurs="0"/>
     * <xsd:element name="concurrent-method"
     * type="javaee:concurrent-methodType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="depends-on"
     * type="javaee:depends-onType"
     * minOccurs="0"/>
     * <xsd:element name="init-method"
     * type="javaee:init-methodType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="remove-method"
     * type="javaee:remove-methodType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="async-method"
     * type="javaee:async-methodType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="transaction-type"
     * type="javaee:transaction-typeType"
     * minOccurs="0"/>
     * <xsd:element name="after-begin-method"
     * type="javaee:named-methodType"
     * minOccurs="0"/>
     * <xsd:element name="before-completion-method"
     * type="javaee:named-methodType"
     * minOccurs="0"/>
     * <xsd:element name="after-completion-method"
     * type="javaee:named-methodType"
     * minOccurs="0"/>
     * <xsd:element name="around-invoke"
     * type="javaee:around-invokeType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="around-timeout"
     * type="javaee:around-timeoutType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="post-activate"
     * type="javaee:lifecycle-callbackType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="pre-passivate"
     * type="javaee:lifecycle-callbackType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * </xsd:complexType>
     */
    static class SessionBeanType extends EnterpriseBeanType implements Session {

        @Override
        public String getHomeInterfaceName() {
            return home != null ? home.getValue() : null;
        }

        @Override
        public String getRemoteInterfaceName() {
            return remote != null ? remote.getValue() : null;
        }

        @Override
        public String getLocalHomeInterfaceName() {
            return local_home != null ? local_home.getValue() : null;
        }

        @Override
        public String getLocalInterfaceName() {
            return local != null ? local.getValue() : null;
        }

        @Override
        public List<String> getLocalBusinessInterfaceNames() {
            if (business_local != null) {
                return business_local.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<String> getRemoteBusinessInterfaceNames() {
            if (business_remote != null) {
                return business_remote.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean isLocalBean() {
            return local_bean != null;
        }

        @Override
        public String getServiceEndpointInterfaceName() {
            return service_endpoint != null ? service_endpoint.getValue() : null;
        }

        @Override
        public int getSessionTypeValue() {
            if (session_type != null) {
                switch (session_type.value) {
                    case Singleton:
                        return SESSION_TYPE_SINGLETON;
                    case Stateful:
                        return SESSION_TYPE_STATEFUL;
                    case Stateless:
                        return SESSION_TYPE_STATELESS;
                }
            }
            return SESSION_TYPE_UNSPECIFIED;
        }

        @Override
        public StatefulTimeout getStatefulTimeout() {
            return stateful_timeout;
        }

        @Override
        public NamedMethod getTimeoutMethod() {
            return timeout_method;
        }

        @Override
        public List<Timer> getTimers() {
            if (timer != null) {
                return timer.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean isSetInitOnStartup() {
            return init_on_startup != null;
        }

        @Override
        public boolean isInitOnStartup() {
            return init_on_startup != null ? init_on_startup.getBooleanValue() : false;
        }

        @Override
        public int getConcurrencyManagementTypeValue() {
            if (concurrency_management_type != null) {
                switch (concurrency_management_type.value) {
                    case Bean:
                        return CONCURRENCY_MANAGEMENT_TYPE_BEAN;
                    case Container:
                        return CONCURRENCY_MANAGEMENT_TYPE_CONTAINER;
                }
            }
            return CONCURRENCY_MANAGEMENT_TYPE_UNSPECIFIED;
        }

        @Override
        public List<ConcurrentMethod> getConcurrentMethods() {
            if (concurrent_method != null) {
                return concurrent_method.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public DependsOn getDependsOn() {
            return depends_on;
        }

        @Override
        public List<InitMethod> getInitMethod() {
            if (init_method != null) {
                return init_method.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<RemoveMethod> getRemoveMethod() {
            if (remove_method != null) {
                return remove_method.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<AsyncMethod> getAsyncMethods() {
            if (async_method != null) {
                return async_method.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public int getTransactionTypeValue() {
            if (transaction_type != null) {
                switch (transaction_type.value) {
                    case Bean:
                        return TRANSACTION_TYPE_BEAN;
                    case Container:
                        return TRANSACTION_TYPE_CONTAINER;
                }
            }
            return TRANSACTION_TYPE_UNSPECIFIED;
        }

        @Override
        public NamedMethod getAfterBeginMethod() {
            return after_begin_method;
        }

        @Override
        public NamedMethod getBeforeCompletionMethod() {
            return before_completion_method;
        }

        @Override
        public NamedMethod getAfterCompletionMethod() {
            return after_completion_method;
        }

        @Override
        public boolean isSetPassivationCapable() {
            return passivation_capable != null;
        }

        @Override
        public boolean isPassivationCapable() {
            return passivation_capable != null && passivation_capable.getBooleanValue();
        }

        @Override
        public List<InterceptorCallback> getAroundInvoke() {
            if (around_invoke != null) {
                return around_invoke.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<InterceptorCallback> getAroundTimeoutMethods() {
            if (around_timeout != null) {
                return around_timeout.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<LifecycleCallback> getPostActivate() {
            if (post_activate != null) {
                return post_activate.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<LifecycleCallback> getPrePassivate() {
            if (pre_passivate != null) {
                return pre_passivate.getList();
            } else {
                return Collections.emptyList();
            }
        }

        // elements
        XSDTokenType home;
        XSDTokenType remote;
        XSDTokenType local_home;
        XSDTokenType local;
        XSDTokenType.ListType business_local;
        XSDTokenType.ListType business_remote;
        EmptyType local_bean;
        XSDTokenType service_endpoint;
        SessionTypeType session_type;
        StatefulTimeoutType stateful_timeout;
        NamedMethodType timeout_method;
        TimerType.ListType timer;
        XSDBooleanType init_on_startup;
        ConcurrencyManagementTypeType concurrency_management_type;
        ConcurrentMethodType.ListType concurrent_method;
        DependsOnType depends_on;
        InitMethodType.ListType init_method;
        RemoveMethodType.ListType remove_method;
        AsyncMethodType.ListType async_method;
        TransactionTypeType transaction_type;
        NamedMethodType after_begin_method;
        NamedMethodType before_completion_method;
        NamedMethodType after_completion_method;
        XSDBooleanType passivation_capable;
        InterceptorCallbackType.ListType around_invoke;
        InterceptorCallbackType.ListType around_timeout;
        LifecycleCallbackType.ListType post_activate;
        LifecycleCallbackType.ListType pre_passivate;

        SessionBeanType() {
            super(KIND_SESSION);
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("home".equals(localName)) {
                XSDTokenType home = new XSDTokenType();
                parser.parse(home);
                this.home = home;
                return true;
            }
            if ("remote".equals(localName)) {
                XSDTokenType remote = new XSDTokenType();
                parser.parse(remote);
                this.remote = remote;
                return true;
            }
            if ("local-home".equals(localName)) {
                XSDTokenType local_home = new XSDTokenType();
                parser.parse(local_home);
                this.local_home = local_home;
                return true;
            }
            if ("local".equals(localName)) {
                XSDTokenType local = new XSDTokenType();
                parser.parse(local);
                this.local = local;
                return true;
            }
            if ("business-local".equals(localName)) {
                XSDTokenType business_local = new XSDTokenType();
                parser.parse(business_local);
                addBusinessLocal(business_local);
                return true;
            }
            if ("business-remote".equals(localName)) {
                XSDTokenType business_remote = new XSDTokenType();
                parser.parse(business_remote);
                addBusinessRemote(business_remote);
                return true;
            }
            if ("local-bean".equals(localName)) {
                EmptyType local_bean = new EmptyType();
                parser.parse(local_bean);
                this.local_bean = local_bean;
                return true;
            }
            if ("service-endpoint".equals(localName)) {
                XSDTokenType service_endpoint = new XSDTokenType();
                parser.parse(service_endpoint);
                this.service_endpoint = service_endpoint;
                return true;
            }
            if ("session-type".equals(localName)) {
                SessionTypeType session_type = new SessionTypeType();
                parser.parse(session_type);
                this.session_type = session_type;
                return true;
            }
            if ("stateful-timeout".equals(localName)) {
                StatefulTimeoutType stateful_timeout = new StatefulTimeoutType();
                parser.parse(stateful_timeout);
                if (stateful_timeout.timeout.getIntegerValue() != null)
                    this.stateful_timeout = stateful_timeout;
                return true;
            }
            if ("timeout-method".equals(localName)) {
                NamedMethodType timeout_method = new NamedMethodType();
                parser.parse(timeout_method);
                this.timeout_method = timeout_method;
                return true;
            }
            if ("timer".equals(localName)) {
                TimerType timer = new TimerType();
                parser.parse(timer);
                addTimer(timer);
                return true;
            }
            if ("init-on-startup".equals(localName)) {
                XSDBooleanType init_on_startup = new XSDBooleanType();
                parser.parse(init_on_startup);
                this.init_on_startup = init_on_startup;
                return true;
            }
            if ("concurrency-management-type".equals(localName)) {
                ConcurrencyManagementTypeType concurrency_management_type = new ConcurrencyManagementTypeType();
                parser.parse(concurrency_management_type);
                this.concurrency_management_type = concurrency_management_type;
                return true;
            }
            if ("concurrent-method".equals(localName)) {
                ConcurrentMethodType concurrent_method = new ConcurrentMethodType();
                parser.parse(concurrent_method);
                addConcurrentMethod(concurrent_method);
                return true;
            }
            if ("depends-on".equals(localName)) {
                DependsOnType depends_on = new DependsOnType();
                parser.parse(depends_on);
                this.depends_on = depends_on;
                return true;
            }
            if ("init-method".equals(localName)) {
                InitMethodType init_method = new InitMethodType();
                parser.parse(init_method);
                addInitMethod(init_method);
                return true;
            }
            if ("remove-method".equals(localName)) {
                RemoveMethodType remove_method = new RemoveMethodType();
                parser.parse(remove_method);
                addRemoveMethod(remove_method);
                return true;
            }
            if ("async-method".equals(localName)) {
                AsyncMethodType async_method = new AsyncMethodType();
                parser.parse(async_method);
                addAsyncMethod(async_method);
                return true;
            }
            if ("transaction-type".equals(localName)) {
                TransactionTypeType transaction_type = new TransactionTypeType();
                parser.parse(transaction_type);
                this.transaction_type = transaction_type;
                return true;
            }
            if ("after-begin-method".equals(localName)) {
                NamedMethodType after_begin = new NamedMethodType();
                parser.parse(after_begin);
                this.after_begin_method = after_begin;
                return true;
            }
            if ("before-completion-method".equals(localName)) {
                NamedMethodType before_completion = new NamedMethodType();
                parser.parse(before_completion);
                this.before_completion_method = before_completion;
                return true;
            }
            if ("after-completion-method".equals(localName)) {
                NamedMethodType after_completion = new NamedMethodType();
                parser.parse(after_completion);
                this.after_completion_method = after_completion;
                return true;
            }
            if (parser.version >= EJBJar.VERSION_3_2 && "passivation-capable".equals(localName)) {
                XSDBooleanType passivation_capable = new XSDBooleanType();
                parser.parse(passivation_capable);
                this.passivation_capable = passivation_capable;
                return true;
            }
            if ("around-invoke".equals(localName)) {
                InterceptorCallbackType around_invoke = new InterceptorCallbackType();
                parser.parse(around_invoke);
                addAroundInvoke(around_invoke);
                return true;
            }
            if ("around-timeout".equals(localName)) {
                InterceptorCallbackType around_timeout = new InterceptorCallbackType();
                parser.parse(around_timeout);
                addAroundTimeout(around_timeout);
                return true;
            }
            if ("post-activate".equals(localName)) {
                LifecycleCallbackType post_activate = new LifecycleCallbackType();
                parser.parse(post_activate);
                addPostActivate(post_activate);
                return true;
            }
            if ("pre-passivate".equals(localName)) {
                LifecycleCallbackType pre_passivate = new LifecycleCallbackType();
                parser.parse(pre_passivate);
                addPrePassivate(pre_passivate);
                return true;
            }
            return false;
        }

        private void addBusinessLocal(XSDTokenType business_local) {
            if (this.business_local == null) {
                this.business_local = new XSDTokenType.ListType();
            }
            this.business_local.add(business_local);
        }

        private void addBusinessRemote(XSDTokenType business_remote) {
            if (this.business_remote == null) {
                this.business_remote = new XSDTokenType.ListType();
            }
            this.business_remote.add(business_remote);
        }

        private void addTimer(TimerType timer) {
            if (this.timer == null) {
                this.timer = new TimerType.ListType();
            }
            this.timer.add(timer);
        }

        private void addConcurrentMethod(ConcurrentMethodType concurrent_method) {
            if (this.concurrent_method == null) {
                this.concurrent_method = new ConcurrentMethodType.ListType();
            }
            this.concurrent_method.add(concurrent_method);
        }

        private void addInitMethod(InitMethodType init_method) {
            if (this.init_method == null) {
                this.init_method = new InitMethodType.ListType();
            }
            this.init_method.add(init_method);
        }

        private void addRemoveMethod(RemoveMethodType remove_method) {
            if (this.remove_method == null) {
                this.remove_method = new RemoveMethodType.ListType();
            }
            this.remove_method.add(remove_method);
        }

        private void addAsyncMethod(AsyncMethodType async_method) {
            if (this.async_method == null) {
                this.async_method = new AsyncMethodType.ListType();
            }
            this.async_method.add(async_method);
        }

        private void addAroundInvoke(InterceptorCallbackType around_invoke) {
            if (this.around_invoke == null) {
                this.around_invoke = new InterceptorCallbackType.ListType();
            }
            this.around_invoke.add(around_invoke);
        }

        private void addAroundTimeout(InterceptorCallbackType around_timeout) {
            if (this.around_timeout == null) {
                this.around_timeout = new InterceptorCallbackType.ListType();
            }
            this.around_timeout.add(around_timeout);
        }

        private void addPostActivate(LifecycleCallbackType post_activate) {
            if (this.post_activate == null) {
                this.post_activate = new LifecycleCallbackType.ListType();
            }
            this.post_activate.add(post_activate);
        }

        private void addPrePassivate(LifecycleCallbackType pre_passivate) {
            if (this.pre_passivate == null) {
                this.pre_passivate = new LifecycleCallbackType.ListType();
            }
            this.pre_passivate.add(pre_passivate);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("home", home);
            diag.describeIfSet("remote", remote);
            diag.describeIfSet("local-home", local_home);
            diag.describeIfSet("local", local);
            diag.describeIfSet("business-local", business_local);
            diag.describeIfSet("business-remote", business_remote);
            diag.describeIfSet("local-bean", local_bean);
            diag.describeIfSet("service-endpoint", service_endpoint);
            diag.describeIfSet("session-type", session_type);
            diag.describeIfSet("stateful-timeout", stateful_timeout);
            diag.describeIfSet("timeout-method", timeout_method);
            diag.describeIfSet("timer", timer);
            diag.describeIfSet("init-on-startup", init_on_startup);
            diag.describeIfSet("concurrency-management-type", concurrency_management_type);
            diag.describeIfSet("concurrent-method", concurrent_method);
            diag.describeIfSet("depends-on", depends_on);
            diag.describeIfSet("init-method", init_method);
            diag.describeIfSet("remove-method", remove_method);
            diag.describeIfSet("async-method", async_method);
            diag.describeIfSet("transaction-type", transaction_type);
            diag.describeIfSet("after-begin-method", after_begin_method);
            diag.describeIfSet("before-completion-method", before_completion_method);
            diag.describeIfSet("after-completion-method", after_completion_method);
            diag.describeIfSet("passivation-capable", passivation_capable);
            diag.describeIfSet("around-invoke", around_invoke);
            diag.describeIfSet("around-timeout", around_timeout);
            diag.describeIfSet("post-activate", post_activate);
            diag.describeIfSet("pre-passivate", pre_passivate);
        }
    }

    /*
     * <xsd:complexType name="session-typeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="Singleton"/>
     * <xsd:enumeration value="Stateful"/>
     * <xsd:enumeration value="Stateless"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class SessionTypeType extends XSDTokenType {

        static enum SessionTypeEnum {
            // lexical value must be (Singleton|Stateful|Stateless)
            Singleton,
            Stateful,
            Stateless;
        }

        // content
        SessionTypeEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, SessionTypeEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    /*
     *
     * <xsd:complexType name="stateful-timeoutType">
     * <xsd:sequence>
     * <xsd:element name="timeout"
     * type="javaee:xsdIntegerType"/>
     * <xsd:element name="unit"
     * type="javaee:time-unit-typeType"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     *
     * <xsd:complexType name="access-timeoutType">
     * <xsd:sequence>
     * <xsd:element name="timeout"
     * type="javaee:xsdIntegerType"/>
     * <xsd:element name="unit"
     * type="javaee:time-unit-typeType"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class TimeoutType extends DDParser.ElementContentParsable implements Timeout {

        @Override
        public long getTimeout() {
            return timeout.getLongValue();
        }

        @Override
        public TimeUnit getUnitValue() {
            switch (unit.value) {
                case Days:
                    return TimeUnit.DAYS;
                case Hours:
                    return TimeUnit.HOURS;
                case Minutes:
                    return TimeUnit.MINUTES;
                case Seconds:
                    return TimeUnit.SECONDS;
                case Milliseconds:
                    return TimeUnit.MILLISECONDS;
                case Microseconds:
                    return TimeUnit.MICROSECONDS;
                case Nanoseconds:
                    return TimeUnit.NANOSECONDS;
            }
            return null;
        }

        // elements
        XSDIntegerType timeout = new XSDIntegerType();
        TimeUnitType unit = new TimeUnitType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("timeout".equals(localName)) {
                parser.parse(timeout);
                return true;
            }
            if ("unit".equals(localName)) {
                parser.parse(unit);
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describe("timeout", timeout);
            diag.describe("unit", unit);
        }
    }

    static class StatefulTimeoutType extends TimeoutType implements StatefulTimeout {
        @Override
        public TimeUnit getUnitValue() {
            if (unit.value != null) {
                switch (unit.value) {
                    case Days:
                        return TimeUnit.DAYS;
                    case Hours:
                        return TimeUnit.HOURS;
                    case Minutes:
                        return TimeUnit.MINUTES;
                    case Seconds:
                        return TimeUnit.SECONDS;
                    case Milliseconds:
                        return TimeUnit.MILLISECONDS;
                    case Microseconds:
                        return TimeUnit.MICROSECONDS;
                    case Nanoseconds:
                        return TimeUnit.NANOSECONDS;
                }
                return TimeUnit.MINUTES;
            } else {
                // return MINUTES to match tWAS default behavior
                return TimeUnit.MINUTES;
            }
        }

        // everything else is in the base type
    }

    static class AccessTimeoutType extends TimeoutType implements AccessTimeout {
        // everything is in the base type
    }

    /*
     * <xsd:complexType name="time-unit-typeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="Days"/>
     * <xsd:enumeration value="Hours"/>
     * <xsd:enumeration value="Minutes"/>
     * <xsd:enumeration value="Seconds"/>
     * <xsd:enumeration value="Milliseconds"/>
     * <xsd:enumeration value="Microseconds"/>
     * <xsd:enumeration value="Nanoseconds"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class TimeUnitType extends XSDTokenType {

        static enum TimeUnitEnum {
            // lexical value must be (Days|Hours|Minutes|Seconds|Milliseconds|Microseconds|Nanoseconds)
            Days,
            Hours,
            Minutes,
            Seconds,
            Milliseconds,
            Microseconds,
            Nanoseconds;
        }

        // content
        TimeUnitEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, TimeUnitEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    /*
     * <xsd:complexType name="named-methodType">
     * <xsd:sequence>
     * <xsd:element name="method-name"
     * type="javaee:string"/>
     * <xsd:element name="method-params"
     * type="javaee:method-paramsType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     *
     * <xsd:complexType name="async-methodType">
     * <xsd:sequence>
     * <xsd:element name="method-name"
     * type="javaee:string"/>
     * <xsd:element name="method-params"
     * type="javaee:method-paramsType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     *
     * <xsd:complexType name="query-methodType">
     * <xsd:sequence>
     * <xsd:element name="method-name"
     * type="javaee:method-nameType"/>
     * <xsd:element name="method-params"
     * type="javaee:method-paramsType"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class BasicMethodType extends DDParser.ElementContentParsable {

        public String getMethodName() {
            return method_name.getValue();
        }

        public List<String> getMethodParamList() {
            if (method_params != null) {
                return method_params.getList();
            } else {
                return null;
            }
        }

        // elements
        XSDTokenType method_name = new XSDTokenType();
        MethodParamsType method_params;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("method-name".equals(localName)) {
                parser.parse(method_name);
                return true;
            }
            if ("method-params".equals(localName)) {
                MethodParamsType method_params = new MethodParamsType();
                parser.parse(method_params);
                this.method_params = method_params;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("method-name", method_name);
            diag.describeIfSet("method-params", method_params);
        }
    }

    static class NamedMethodType extends BasicMethodType implements NamedMethod {
        // everything is in the base type
    }

    static class AsyncMethodType extends BasicMethodType implements AsyncMethod {

        public static class ListType extends ParsableListImplements<AsyncMethodType, AsyncMethod> {
            @Override
            public AsyncMethodType newInstance(DDParser parser) {
                return new AsyncMethodType();
            }
        }
        // everything else is in the base type
    }

    static class QueryMethodType extends BasicMethodType implements QueryMethod {

        public static class ListType extends ParsableListImplements<QueryMethodType, QueryMethod> {
            @Override
            public QueryMethodType newInstance(DDParser parser) {
                return new QueryMethodType();
            }
        }
        // everything else is in the base type
    }

    /*
     * <xsd:complexType name="method-paramsType">
     * <xsd:sequence>
     * <xsd:element name="method-param"
     * type="javaee:java-typeType"
     * minOccurs="0"
     * maxOccurs="unbounded">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class MethodParamsType extends DDParser.ElementContentParsable {

        List<String> getList() {
            if (method_param != null) {
                return method_param.getList();
            } else {
                return Collections.emptyList();
            }
        }

        // elements
        XSDTokenType.ListType method_param;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("method-param".equals(localName)) {
                XSDTokenType method_param = new XSDTokenType();
                parser.parse(method_param);
                addMethodParam(method_param);
                return true;
            }
            return false;
        }

        private void addMethodParam(XSDTokenType method_param) {
            if (this.method_param == null) {
                this.method_param = new XSDTokenType.ListType();
            }
            this.method_param.add(method_param);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("method-param", method_param);
        }
    }

    /*
     * <xsd:complexType name="timerType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="schedule"
     * type="javaee:timer-scheduleType"/>
     * <xsd:element name="start"
     * type="xsd:dateTime"
     * minOccurs="0"/>
     * <xsd:element name="end"
     * type="xsd:dateTime"
     * minOccurs="0"/>
     * <xsd:element name="timeout-method"
     * type="javaee:named-methodType"/>
     * <xsd:element name="persistent"
     * type="javaee:true-falseType"
     * minOccurs="0"/>
     * <xsd:element name="timezone"
     * type="javaee:string"
     * minOccurs="0"/>
     * <xsd:element name="info"
     * type="javaee:string"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class TimerType extends DescribableType implements Timer {

        public static class ListType extends ParsableListImplements<TimerType, Timer> {
            @Override
            public TimerType newInstance(DDParser parser) {
                return new TimerType();
            }
        }

        @Override
        public TimerSchedule getSchedule() {
            return schedule;
        }

        @Override
        public String getStart() {
            return start != null ? start.getLexicalValue() : null;
        }

        @Override
        public String getEnd() {
            return end != null ? end.getLexicalValue() : null;
        }

        @Override
        public NamedMethod getTimeoutMethod() {
            return timeout_method;
        }

        @Override
        public boolean isSetPersistent() {
            return persistent != null;
        }

        @Override
        public boolean isPersistent() {
            return persistent != null ? persistent.getBooleanValue() : false;
        }

        @Override
        public String getTimezone() {
            return timezone != null ? timezone.getValue() : null;
        }

        @Override
        public String getInfo() {
            return info != null ? info.getValue() : null;
        }

        // elements
        TimerScheduleType schedule = new TimerScheduleType();
        DateTimeType start;
        DateTimeType end;
        NamedMethodType timeout_method = new NamedMethodType();
        XSDBooleanType persistent;
        XSDTokenType timezone;
        XSDTokenType info;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("schedule".equals(localName)) {
                parser.parse(schedule);
                return true;
            }
            if ("start".equals(localName)) {
                DateTimeType start = new DateTimeType();
                parser.parse(start);
                this.start = start;
                return true;
            }
            if ("end".equals(localName)) {
                DateTimeType end = new DateTimeType();
                parser.parse(end);
                this.end = end;
                return true;
            }
            if ("timeout-method".equals(localName)) {
                parser.parse(timeout_method);
                return true;
            }
            if ("persistent".equals(localName)) {
                XSDBooleanType persistent = new XSDBooleanType();
                parser.parse(persistent);
                this.persistent = persistent;
                return true;
            }
            if ("timezone".equals(localName)) {
                XSDTokenType timezone = new XSDTokenType();
                parser.parse(timezone);
                this.timezone = timezone;
                return true;
            }
            if ("info".equals(localName)) {
                XSDTokenType info = new XSDTokenType();
                parser.parse(info);
                this.info = info;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("schedule", schedule);
            diag.describeIfSet("start", start);
            diag.describeIfSet("end", end);
            diag.describeIfSet("timeout-method", timeout_method);
            diag.describeIfSet("persistent", persistent);
            diag.describeIfSet("timezone", timezone);
            diag.describeIfSet("info", info);
        }
    }

    /*
     * <xsd:complexType name="timer-scheduleType">
     * <xsd:sequence>
     * <xsd:element name="second"
     * type="javaee:string"
     * minOccurs="0"/>
     * <xsd:element name="minute"
     * type="javaee:string"
     * minOccurs="0"/>
     * <xsd:element name="hour"
     * type="javaee:string"
     * minOccurs="0"/>
     * <xsd:element name="day-of-month"
     * type="javaee:string"
     * minOccurs="0"/>
     * <xsd:element name="month"
     * type="javaee:string"
     * minOccurs="0"/>
     * <xsd:element name="day-of-week"
     * type="javaee:string"
     * minOccurs="0"/>
     * <xsd:element name="year"
     * type="javaee:string"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class TimerScheduleType extends DDParser.ElementContentParsable implements TimerSchedule {
        @Override
        public String getSecond() {
            return second != null ? second.getValue() : null;
        }

        @Override
        public String getMinute() {
            return minute != null ? minute.getValue() : null;
        }

        @Override
        public String getHour() {
            return hour != null ? hour.getValue() : null;
        }

        @Override
        public String getDayOfMonth() {
            return day_of_month != null ? day_of_month.getValue() : null;
        }

        @Override
        public String getMonth() {
            return month != null ? month.getValue() : null;
        }

        @Override
        public String getDayOfWeek() {
            return day_of_week != null ? day_of_week.getValue() : null;
        }

        @Override
        public String getYear() {
            return year != null ? year.getValue() : null;
        }

        // elements
        XSDTokenType second;
        XSDTokenType minute;
        XSDTokenType hour;
        XSDTokenType day_of_month;
        XSDTokenType month;
        XSDTokenType day_of_week;
        XSDTokenType year;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("second".equals(localName)) {
                XSDTokenType second = new XSDTokenType();
                parser.parse(second);
                this.second = second;
                return true;
            }
            if ("minute".equals(localName)) {
                XSDTokenType minute = new XSDTokenType();
                parser.parse(minute);
                this.minute = minute;
                return true;
            }
            if ("hour".equals(localName)) {
                XSDTokenType hour = new XSDTokenType();
                parser.parse(hour);
                this.hour = hour;
                return true;
            }
            if ("day-of-month".equals(localName)) {
                XSDTokenType day_of_month = new XSDTokenType();
                parser.parse(day_of_month);
                this.day_of_month = day_of_month;
                return true;
            }
            if ("month".equals(localName)) {
                XSDTokenType month = new XSDTokenType();
                parser.parse(month);
                this.month = month;
                return true;
            }
            if ("day-of-week".equals(localName)) {
                XSDTokenType day_of_week = new XSDTokenType();
                parser.parse(day_of_week);
                this.day_of_week = day_of_week;
                return true;
            }
            if ("year".equals(localName)) {
                XSDTokenType year = new XSDTokenType();
                parser.parse(year);
                this.year = year;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("second", second);
            diag.describeIfSet("minute", minute);
            diag.describeIfSet("hour", hour);
            diag.describeIfSet("day-of-month", day_of_month);
            diag.describeIfSet("month", month);
            diag.describeIfSet("day-of-week", day_of_week);
            diag.describeIfSet("year", year);
        }
    }

    /*
     * <xs:simpleType name="dateTime" id="dateTime">
     * <xs:restriction base="xs:anySimpleType">
     * <xs:whiteSpace fixed="true" value="collapse" id="dateTime.whiteSpace"/>
     * </xs:restriction>
     * </xs:simpleType>
     */
    static class DateTimeType extends TokenType {
        // nothing yet as values of this type are being returned as String for now
    }

    /*
     * <xsd:complexType name="concurrency-management-typeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="Bean"/>
     * <xsd:enumeration value="Container"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     *
     * <xsd:complexType name="transaction-typeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="Bean"/>
     * <xsd:enumeration value="Container"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     *
     * <xsd:complexType name="persistence-typeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="Bean"/>
     * <xsd:enumeration value="Container"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class BeanContainerEnumType extends XSDTokenType {

        static enum BeanContainerEnum {
            // lexical value must be (Bean|Container)
            Bean,
            Container;
        }

        // content
        BeanContainerEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, BeanContainerEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    static class ConcurrencyManagementTypeType extends BeanContainerEnumType {
        // everything is in the base type
    }

    static class TransactionTypeType extends BeanContainerEnumType {
        // everything is in the base type
    }

    static class PersistenceTypeType extends BeanContainerEnumType {
        // everything is in the base type
    }

    /*
     * <xsd:complexType name="concurrent-lock-typeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="Read"/>
     * <xsd:enumeration value="Write"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class ConcurrentLockTypeType extends XSDTokenType {

        static enum ConcurrentLockTypeEnum {
            // lexical value must be (Read|Write)
            Read,
            Write;
        }

        // content
        ConcurrentLockTypeEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, ConcurrentLockTypeEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    /*
     * <xsd:complexType name="concurrent-methodType">
     * <xsd:sequence>
     * <xsd:element name="method"
     * type="javaee:named-methodType"/>
     * <xsd:element name="lock"
     * type="javaee:concurrent-lock-typeType"
     * minOccurs="0"/>
     * <xsd:element name="access-timeout"
     * type="javaee:access-timeoutType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ConcurrentMethodType extends DDParser.ElementContentParsable implements ConcurrentMethod {

        public static class ListType extends ParsableListImplements<ConcurrentMethodType, ConcurrentMethod> {
            @Override
            public ConcurrentMethodType newInstance(DDParser parser) {
                return new ConcurrentMethodType();
            }
        }

        @Override
        public NamedMethod getMethod() {
            return method;
        }

        @Override
        public int getLockTypeValue() {
            if (lock_type != null) {
                switch (lock_type.value) {
                    case Read:
                        return LOCK_TYPE_READ;
                    case Write:
                        return LOCK_TYPE_WRITE;
                }
            }
            return LOCK_TYPE_UNSPECIFIED;
        }

        @Override
        public AccessTimeout getAccessTimeout() {
            return access_timeout;
        }

        // elements
        NamedMethodType method = new NamedMethodType();
        ConcurrentLockTypeType lock_type;
        AccessTimeoutType access_timeout;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("method".equals(localName)) {
                parser.parse(method);
                return true;
            }
            if ("lock".equals(localName)) {
                ConcurrentLockTypeType lock_type = new ConcurrentLockTypeType();
                parser.parse(lock_type);
                this.lock_type = lock_type;
                return true;
            }
            if ("access-timeout".equals(localName)) {
                AccessTimeoutType access_timeout = new AccessTimeoutType();
                parser.parse(access_timeout);
                this.access_timeout = access_timeout;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describe("method", method);
            diag.describeIfSet("lock", lock_type);
            diag.describeIfSet("access-timeout", access_timeout);
        }
    }

    /*
     * <xsd:complexType name="depends-onType">
     * <xsd:sequence>
     * <xsd:element name="ejb-name"
     * type="javaee:ejb-linkType"
     * minOccurs="1"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class DependsOnType extends DDParser.ElementContentParsable implements DependsOn {
        /**
         * @return &lt;ejb-name> as a read-only list
         */
        @Override
        public List<String> getEjbName() {
            return ejb_name.getList();
        }

        // elements
        XSDTokenType.ListType ejb_name = new XSDTokenType.ListType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("ejb-name".equals(localName)) {
                XSDTokenType ejb_name = new XSDTokenType();
                parser.parse(ejb_name);
                addEjbName(ejb_name);
                return true;
            }
            return false;
        }

        private void addEjbName(XSDTokenType ejb_name) {
            this.ejb_name.add(ejb_name);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describe("ejb-name", ejb_name);
        }
    }

    /*
     * <xsd:complexType name="init-methodType">
     * <xsd:sequence>
     * <xsd:element name="create-method"
     * type="javaee:named-methodType"/>
     * <xsd:element name="bean-method"
     * type="javaee:named-methodType"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class InitMethodType extends DDParser.ElementContentParsable implements InitMethod {

        public static class ListType extends ParsableListImplements<InitMethodType, InitMethod> {
            @Override
            public InitMethodType newInstance(DDParser parser) {
                return new InitMethodType();
            }
        }

        @Override
        public NamedMethod getCreateMethod() {
            return create_method;
        }

        @Override
        public NamedMethod getBeanMethod() {
            return bean_method;
        }

        // elements
        NamedMethodType create_method = new NamedMethodType();
        NamedMethodType bean_method = new NamedMethodType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("create-method".equals(localName)) {
                parser.parse(create_method);
                return true;
            }
            if ("bean-method".equals(localName)) {
                parser.parse(bean_method);
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describe("create-method", create_method);
            diag.describe("bean-method", bean_method);
        }
    }

    /*
     * <xsd:complexType name="remove-methodType">
     * <xsd:sequence>
     * <xsd:element name="bean-method"
     * type="javaee:named-methodType"/>
     * <xsd:element name="retain-if-exception"
     * type="javaee:true-falseType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class RemoveMethodType extends DDParser.ElementContentParsable implements RemoveMethod {

        public static class ListType extends ParsableListImplements<RemoveMethodType, RemoveMethod> {
            @Override
            public RemoveMethodType newInstance(DDParser parser) {
                return new RemoveMethodType();
            }
        }

        @Override
        public NamedMethod getBeanMethod() {
            return bean_method;
        }

        @Override
        public boolean isSetRetainIfException() {
            return retain_if_exception != null;
        }

        @Override
        public boolean isRetainIfException() {
            return retain_if_exception != null ? retain_if_exception.getBooleanValue() : false;
        }

        // elements
        NamedMethodType bean_method = new NamedMethodType();
        XSDBooleanType retain_if_exception;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("bean-method".equals(localName)) {
                parser.parse(bean_method);
                return true;
            }
            if ("retain-if-exception".equals(localName)) {
                XSDBooleanType retain_if_exception = new XSDBooleanType();
                parser.parse(retain_if_exception);
                this.retain_if_exception = retain_if_exception;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describe("bean-method", bean_method);
            diag.describeIfSet("retain-if-exception", retain_if_exception);
        }
    }

    /*
     * <xsd:complexType name="entity-beanType">
     * <xsd:sequence>
     * <xsd:element name="home"
     * type="javaee:homeType"
     * minOccurs="0"/>
     * <xsd:element name="remote"
     * type="javaee:remoteType"
     * minOccurs="0"/>
     * <xsd:element name="local-home"
     * type="javaee:local-homeType"
     * minOccurs="0"/>
     * <xsd:element name="local"
     * type="javaee:localType"
     * minOccurs="0"/>
     * <xsd:element name="persistence-type"
     * type="javaee:persistence-typeType"/>
     * <xsd:element name="prim-key-class"
     * type="javaee:fully-qualified-classType">
     * </xsd:element>
     * <xsd:element name="reentrant"
     * type="javaee:true-falseType">
     * </xsd:element>
     * <xsd:element name="cmp-version"
     * type="javaee:cmp-versionType"
     * minOccurs="0"/>
     * <xsd:element name="abstract-schema-name"
     * type="javaee:java-identifierType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="cmp-field"
     * type="javaee:cmp-fieldType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="primkey-field"
     * type="javaee:string"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="query"
     * type="javaee:queryType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * </xsd:complexType>
     */
    static class EntityBeanType extends EnterpriseBeanType implements Entity {

        @Override
        public String getHomeInterfaceName() {
            return home != null ? home.getValue() : null;
        }

        @Override
        public String getRemoteInterfaceName() {
            return remote != null ? remote.getValue() : null;
        }

        @Override
        public String getLocalHomeInterfaceName() {
            return local_home != null ? local_home.getValue() : null;
        }

        @Override
        public String getLocalInterfaceName() {
            return local != null ? local.getValue() : null;
        }

        @Override
        public int getPersistenceTypeValue() {
            if (persistence_type != null) {
                switch (persistence_type.value) {
                    case Bean:
                        return PERSISTENCE_TYPE_BEAN;
                    case Container:
                        return PERSISTENCE_TYPE_CONTAINER;
                }
            }
            throw new IllegalStateException("persistence-type");
        }

        @Override
        public String getPrimaryKeyName() {
            return prim_key_class.getValue();
        }

        @Override
        public boolean isReentrant() {
            return reentrant.getBooleanValue();
        }

        @Override
        public int getCMPVersionValue() {
            if (cmp_version != null) {
                switch (cmp_version.value) {
                    case Version_1_x:
                        return CMP_VERSION_1_X;
                    case Version_2_x:
                        return CMP_VERSION_2_X;
                }
            }
            return CMP_VERSION_UNSPECIFIED;
        }

        @Override
        public String getAbstractSchemaName() {
            return abstract_schema_name != null ? abstract_schema_name.getValue() : null;
        }

        @Override
        public List<CMPField> getCMPFields() {
            if (cmp_field != null) {
                return cmp_field.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public CMPField getPrimKeyField() {
            if (primkey_field != null && cmp_field != null) {
                String field_name = primkey_field.getValue();
                if (field_name != null) {
                    for (CMPField field : cmp_field) {
                        if (field_name.equals(field.getName())) {
                            return field;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public List<Query> getQueries() {
            if (query != null) {
                return query.getList();
            } else {
                return Collections.emptyList();
            }
        }

        // elements
        XSDTokenType home;
        XSDTokenType remote;
        XSDTokenType local_home;
        XSDTokenType local;
        PersistenceTypeType persistence_type = new PersistenceTypeType();
        XSDTokenType prim_key_class = new XSDTokenType();
        XSDBooleanType reentrant = new XSDBooleanType();
        CMPVersionType cmp_version;
        XSDTokenType abstract_schema_name;
        CMPFieldType.ListType cmp_field;
        XSDTokenType primkey_field;
        QueryType.ListType query;

        EntityBeanType() {
            super(KIND_ENTITY);
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("home".equals(localName)) {
                XSDTokenType home = new XSDTokenType();
                parser.parse(home);
                this.home = home;
                return true;
            }
            if ("remote".equals(localName)) {
                XSDTokenType remote = new XSDTokenType();
                parser.parse(remote);
                this.remote = remote;
                return true;
            }
            if ("local-home".equals(localName)) {
                XSDTokenType local_home = new XSDTokenType();
                parser.parse(local_home);
                this.local_home = local_home;
                return true;
            }
            if ("local".equals(localName)) {
                XSDTokenType local = new XSDTokenType();
                parser.parse(local);
                this.local = local;
                return true;
            }
            if ("persistence-type".equals(localName)) {
                parser.parse(persistence_type);
                return true;
            }
            if ("prim-key-class".equals(localName)) {
                parser.parse(prim_key_class);
                return true;
            }
            if ("reentrant".equals(localName)) {
                parser.parse(reentrant);
                return true;
            }
            if ("cmp-version".equals(localName)) {
                CMPVersionType cmp_version = new CMPVersionType();
                parser.parse(cmp_version);
                this.cmp_version = cmp_version;
                return true;
            }
            if ("abstract-schema-name".equals(localName)) {
                XSDTokenType abstract_schema_name = new XSDTokenType();
                parser.parse(abstract_schema_name);
                this.abstract_schema_name = abstract_schema_name;
                return true;
            }
            if ("cmp-field".equals(localName)) {
                CMPFieldType cmp_field = new CMPFieldType();
                parser.parse(cmp_field);
                addCMPField(cmp_field);
                return true;
            }
            if ("primkey-field".equals(localName)) {
                XSDTokenType primkey_field = new XSDTokenType();
                parser.parse(primkey_field);
                this.primkey_field = primkey_field;
                return true;
            }
            if ("query".equals(localName)) {
                QueryType query = new QueryType();
                parser.parse(query);
                addQuery(query);
                return true;
            }
            return false;
        }

        private void addCMPField(CMPFieldType cmp_field) {
            if (this.cmp_field == null) {
                this.cmp_field = new CMPFieldType.ListType();
            }
            this.cmp_field.add(cmp_field);
        }

        private void addQuery(QueryType query) {
            if (this.query == null) {
                this.query = new QueryType.ListType();
            }
            this.query.add(query);
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("home", home);
            diag.describeIfSet("remote", remote);
            diag.describeIfSet("local-home", local_home);
            diag.describeIfSet("local", local);
            diag.describe("persistence-type", persistence_type);
            diag.describe("prim-key-class", prim_key_class);
            diag.describe("reentrant", reentrant);
            diag.describeIfSet("cmp-version", cmp_version);
            diag.describeIfSet("abstract-schema-name", abstract_schema_name);
            diag.describeIfSet("cmp-field", cmp_field);
            diag.describeIfSet("primkey-field", primkey_field);
            diag.describeIfSet("query", query);
        }
    }

    /*
     * <xsd:complexType name="cmp-versionType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="1.x"/>
     * <xsd:enumeration value="2.x"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class CMPVersionType extends XSDTokenType {

        static enum CMPVersionEnum {
            // lexical value must be (1.x|2.x)
            Version_1_x,
            Version_2_x;
        }

        // content
        CMPVersionEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                String sVal = getValue();
                if (sVal.equals("1.x")) {
                    value = CMPVersionEnum.Version_1_x;
                } else if (sVal.equals("2.x")) {
                    value = CMPVersionEnum.Version_2_x;
                } else {
                    throw new IllegalArgumentException(sVal);
                }
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            if (value != null) {
                if (value == CMPVersionEnum.Version_1_x) {
                    diag.append("1.x");
                } else {
                    diag.append("2.x");
                }
            }
        }
    }

    /*
     * <xsd:complexType name="cmp-fieldType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="field-name"
     * type="javaee:java-identifierType">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class CMPFieldType extends DescribableType implements CMPField {

        public static class ListType extends ParsableListImplements<CMPFieldType, CMPField> {
            @Override
            public CMPFieldType newInstance(DDParser parser) {
                return new CMPFieldType();
            }
        }

        @Override
        public String getName() {
            return field_name.getValue();
        }

        // elements
        XSDTokenType field_name = new XSDTokenType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("field-name".equals(localName)) {
                parser.parse(field_name);
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            super.describe(diag);
            diag.describe("field-name", field_name);
        }
    }

    /*
     * <xsd:complexType name="queryType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"/>
     * <xsd:element name="query-method"
     * type="javaee:query-methodType"/>
     * <xsd:element name="result-type-mapping"
     * type="javaee:result-type-mappingType"
     * minOccurs="0"/>
     * <xsd:element name="ejb-ql"
     * type="javaee:xsdStringType"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class QueryType extends DescribableType implements Query {

        public static class ListType extends ParsableListImplements<QueryType, Query> {
            @Override
            public QueryType newInstance(DDParser parser) {
                return new QueryType();
            }
        }

        @Override
        public QueryMethod getQueryMethod() {
            return query_method;
        }

        @Override
        public int getResultTypeMappingValue() {
            if (result_type_mapping != null) {
                switch (result_type_mapping.value) {
                    case Local:
                        return RESULT_TYPE_MAPPING_LOCAL;
                    case Remote:
                        return RESULT_TYPE_MAPPING_REMOTE;
                }
            }
            return RESULT_TYPE_MAPPING_UNSPECIFIED;
        }

        @Override
        public String getEjbQL() {
            return ejb_ql.getValue();
        }

        // elements
        QueryMethodType query_method = new QueryMethodType();
        ResultTypeMappingType result_type_mapping;
        XSDStringType ejb_ql = new XSDStringType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("query-method".equals(localName)) {
                parser.parse(query_method);
                return true;
            }
            if ("result-type-mapping".equals(localName)) {
                ResultTypeMappingType result_type_mapping = new ResultTypeMappingType();
                parser.parse(result_type_mapping);
                this.result_type_mapping = result_type_mapping;
                return true;
            }
            if ("ejb-ql".equals(localName)) {
                parser.parse(ejb_ql);
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            super.describe(diag);
            diag.describe("query-method", query_method);
            diag.describeIfSet("result-type-mapping", result_type_mapping);
            diag.describe("ejb-ql", ejb_ql);
        }
    }

    /*
     * <xsd:complexType name="result-type-mappingType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="Local"/>
     * <xsd:enumeration value="Remote"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class ResultTypeMappingType extends XSDTokenType {

        static enum ResultTypeMappingEnum {
            // lexical value must be (Local|Remote)
            Local,
            Remote;
        }

        // content
        ResultTypeMappingEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, ResultTypeMappingEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    /*
     * <xsd:complexType name="message-driven-beanType">
     * <xsd:sequence>
     * <xsd:element name="messaging-type"
     * type="javaee:fully-qualified-classType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="timeout-method"
     * type="javaee:named-methodType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="timer"
     * type="javaee:timerType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="transaction-type"
     * type="javaee:transaction-typeType"
     * minOccurs="0"/>
     * <xsd:element name="message-destination-type"
     * type="javaee:message-destination-typeType"
     * minOccurs="0"/>
     * <xsd:element name="message-destination-link"
     * type="javaee:message-destination-linkType"
     * minOccurs="0"/>
     * <xsd:element name="activation-config"
     * type="javaee:activation-configType"
     * minOccurs="0"/>
     * <xsd:element name="around-invoke"
     * type="javaee:around-invokeType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="around-timeout"
     * type="javaee:around-timeoutType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * </xsd:complexType>
     */
    static class MessageDrivenBeanType extends EnterpriseBeanType implements MessageDriven {

        @Override
        public String getMessagingTypeName() {
            return messaging_type != null ? messaging_type.getValue() : null;
        }

        @Override
        public NamedMethod getTimeoutMethod() {
            return timeout_method;
        }

        @Override
        public List<Timer> getTimers() {
            if (timer != null) {
                return timer.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public int getTransactionTypeValue() {
            if (transaction_type != null) {
                switch (transaction_type.value) {
                    case Bean:
                        return TRANSACTION_TYPE_BEAN;
                    case Container:
                        return TRANSACTION_TYPE_CONTAINER;
                }
            }
            return TRANSACTION_TYPE_UNSPECIFIED;
        }

        @Override
        public String getMessageDestinationName() {
            return message_destination_type != null ? message_destination_type.getValue() : null;
        }

        @Override
        public String getLink() {
            return message_destination_link != null ? message_destination_link.getValue() : null;
        }

        @Override
        public ActivationConfig getActivationConfigValue() {
            return activation_config;
        }

        @Override
        public List<InterceptorCallback> getAroundInvoke() {
            if (around_invoke != null) {
                return around_invoke.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<InterceptorCallback> getAroundTimeoutMethods() {
            if (around_timeout != null) {
                return around_timeout.getList();
            } else {
                return Collections.emptyList();
            }
        }

        // elements
        XSDTokenType messaging_type;
        NamedMethodType timeout_method;
        TimerType.ListType timer;
        TransactionTypeType transaction_type;
        XSDTokenType message_destination_type;
        XSDTokenType message_destination_link;
        ActivationConfigType activation_config;
        InterceptorCallbackType.ListType around_invoke;
        InterceptorCallbackType.ListType around_timeout;

        MessageDrivenBeanType() {
            super(KIND_MESSAGE_DRIVEN);
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("messaging-type".equals(localName)) {
                XSDTokenType messaging_type = new XSDTokenType();
                parser.parse(messaging_type);
                this.messaging_type = messaging_type;
                return true;
            }
            if ("timeout-method".equals(localName)) {
                NamedMethodType timeout_method = new NamedMethodType();
                parser.parse(timeout_method);
                this.timeout_method = timeout_method;
                return true;
            }
            if ("timer".equals(localName)) {
                TimerType timer = new TimerType();
                parser.parse(timer);
                addTimer(timer);
                return true;
            }
            if ("transaction-type".equals(localName)) {
                TransactionTypeType transaction_type = new TransactionTypeType();
                parser.parse(transaction_type);
                this.transaction_type = transaction_type;
                return true;
            }
            if ("message-destination-type".equals(localName)) {
                XSDTokenType message_destination_type = new XSDTokenType();
                parser.parse(message_destination_type);
                this.message_destination_type = message_destination_type;
                return true;
            }
            if ("message-destination-link".equals(localName)) {
                XSDTokenType message_destination_link = new XSDTokenType();
                parser.parse(message_destination_link);
                this.message_destination_link = message_destination_link;
                return true;
            }
            if (parser.version == EJBJar.VERSION_2_0 && "message-selector".equals(localName)) {
                if (activation_config == null) {
                    activation_config = new ActivationConfigType();
                }

                StringType message_selector = new StringType();
                parser.parse(message_selector);
                activation_config.addActivationConfigProperty(parser, ACTIVATION_CONFIG_PROPERTY_MESSAGE_SELECTOR, message_selector.getValue());
                return true;
            }
            if (parser.version == EJBJar.VERSION_2_0 && "acknowledge-mode".equals(localName)) {
                if (activation_config == null) {
                    activation_config = new ActivationConfigType();
                }

                StringType acknowledge_mode = new StringType();
                parser.parse(acknowledge_mode);

                String acknowledge_mode_value = acknowledge_mode.getValue();
                if ("Auto-acknowledge".equals(acknowledge_mode_value) || "Dups-ok-acknowledge".equals(acknowledge_mode_value)) {
                    activation_config.addActivationConfigProperty(parser, ACTIVATION_CONFIG_PROPERTY_ACKNOWLEDGE_MODE, acknowledge_mode_value);
                } else {
                    throw new ParseException(parser.invalidEnumValue(acknowledge_mode_value, "Auto-acknowledge", "Dups-ok-acknowledge"));
                }
                return true;
            }
            if (parser.version == EJBJar.VERSION_2_0 && "message-driven-destination".equals(localName)) {
                if (activation_config == null) {
                    activation_config = new ActivationConfigType();
                }
                MessageDrivenDestinationType message_driven_destination = new MessageDrivenDestinationType(activation_config);
                parser.parse(message_driven_destination);
                return true;
            }
            if ("activation-config".equals(localName)) {
                ActivationConfigType activation_config = new ActivationConfigType();
                parser.parse(activation_config);
                this.activation_config = activation_config;
                return true;
            }
            if ("around-invoke".equals(localName)) {
                InterceptorCallbackType around_invoke = new InterceptorCallbackType();
                parser.parse(around_invoke);
                addAroundInvoke(around_invoke);
                return true;
            }
            if ("around-timeout".equals(localName)) {
                InterceptorCallbackType around_timeout = new InterceptorCallbackType();
                parser.parse(around_timeout);
                addAroundTimeout(around_timeout);
                return true;
            }
            return false;
        }

        private void addTimer(TimerType timer) {
            if (this.timer == null) {
                this.timer = new TimerType.ListType();
            }
            this.timer.add(timer);
        }

        private void addAroundInvoke(InterceptorCallbackType around_invoke) {
            if (this.around_invoke == null) {
                this.around_invoke = new InterceptorCallbackType.ListType();
            }
            this.around_invoke.add(around_invoke);
        }

        private void addAroundTimeout(InterceptorCallbackType around_timeout) {
            if (this.around_timeout == null) {
                this.around_timeout = new InterceptorCallbackType.ListType();
            }
            this.around_timeout.add(around_timeout);
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("messaging-type", messaging_type);
            diag.describeIfSet("timeout-method", timeout_method);
            diag.describeIfSet("timer", timer);
            diag.describeIfSet("transaction-type", transaction_type);
            diag.describeIfSet("message-destination-type", message_destination_type);
            diag.describeIfSet("message-destination-link", message_destination_link);
            diag.describeIfSet("activation-config", activation_config);
            diag.describeIfSet("around-invoke", around_invoke);
            diag.describeIfSet("around-timeout", around_timeout);
        }
    }

    /*
     * <xsd:complexType name="activation-configType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="activation-config-property"
     * type="javaee:activation-config-propertyType"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ActivationConfigType extends DescribableType implements ActivationConfig {

        @Override
        public List<ActivationConfigProperty> getConfigProperties() {
            return activation_config_property.getList();
        }

        // elements
        ActivationConfigPropertyType.ListType activation_config_property = new ActivationConfigPropertyType.ListType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("activation-config-property".equals(localName)) {
                ActivationConfigPropertyType activation_config_property = new ActivationConfigPropertyType();
                parser.parse(activation_config_property);
                addActivationConfigProperty(activation_config_property);
                return true;
            }
            return false;
        }

        private void addActivationConfigProperty(ActivationConfigPropertyType activation_config_property) {
            this.activation_config_property.add(activation_config_property);
        }

        void addActivationConfigProperty(DDParser parser, String name, String value) throws ParseException {
            for (ActivationConfigPropertyType property : activation_config_property) {
                if (name.equals(property.getName())) {
                    property.activation_config_property_value = XSDStringType.wrap(parser, value);
                    return;
                }
            }

            ActivationConfigPropertyType property = new ActivationConfigPropertyType();
            property.activation_config_property_name = XSDStringType.wrap(parser, name);
            property.activation_config_property_value = XSDStringType.wrap(parser, value);
            addActivationConfigProperty(property);
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            super.describe(diag);
            diag.describe("activation-config-property", activation_config_property);
        }
    }

    /*
     * <xsd:complexType name="activation-config-propertyType">
     * <xsd:sequence>
     * <xsd:element name="activation-config-property-name"
     * type="javaee:xsdStringType">
     * </xsd:element>
     * <xsd:element name="activation-config-property-value"
     * type="javaee:xsdStringType">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ActivationConfigPropertyType extends DDParser.ElementContentParsable implements ActivationConfigProperty {

        public static class ListType extends ParsableListImplements<ActivationConfigPropertyType, ActivationConfigProperty> {
            @Override
            public ActivationConfigPropertyType newInstance(DDParser parser) {
                return new ActivationConfigPropertyType();
            }
        }

        @Override
        public String getName() {
            return activation_config_property_name.getValue();
        }

        @Override
        public String getValue() {
            return activation_config_property_value.getValue();
        }

        // elements
        XSDStringType activation_config_property_name = new XSDStringType();
        XSDStringType activation_config_property_value = new XSDStringType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("activation-config-property-name".equals(localName)) {
                parser.parse(activation_config_property_name);
                return true;
            }
            if ("activation-config-property-value".equals(localName)) {
                parser.parse(activation_config_property_value);
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describe("activation-config-property-name", activation_config_property_name);
            diag.describe("activation-config-property-value", activation_config_property_value);
        }
    }

    /*
     * Instances of this type are only created temporarily for EJB 2.0
     * compatibility. The parsed values are stored in ActivationConfigType.
     *
     * <!ELEMENT message-driven-destination (destination-type, subscription-durability?)>
     */
    static class MessageDrivenDestinationType extends DDParser.ElementContentParsable {
        private final ActivationConfigType activation_config;

        MessageDrivenDestinationType(ActivationConfigType activation_config) {
            this.activation_config = activation_config;
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("destination-type".equals(localName)) {
                StringType destination_type = new StringType();
                parser.parse(destination_type);

                String destination_type_value = destination_type.getValue();
                if ("javax.jms.Queue".equals(destination_type_value) || "javax.jms.Topic".equals(destination_type_value)) {
                    activation_config.addActivationConfigProperty(parser, MessageDriven.ACTIVATION_CONFIG_PROPERTY_DESTINATION_TYPE, destination_type_value);
                } else {
                    throw new ParseException(parser.invalidEnumValue(destination_type_value, "javax.jms.Queue", "javax.jms.Topic"));
                }
                return true;
            }
            if ("subscription-durability".equals(localName)) {
                StringType subscription_durability = new StringType();
                parser.parse(subscription_durability);

                String subscription_durability_value = subscription_durability.getValue();
                if ("Durable".equals(subscription_durability_value) || "NonDurable".equals(subscription_durability_value)) {
                    activation_config.addActivationConfigProperty(parser, MessageDriven.ACTIVATION_CONFIG_PROPERTY_SUBSCRIPTION_DURABILITY, subscription_durability_value);
                } else {
                    throw new ParseException(parser.invalidEnumValue(subscription_durability_value, "Durable", "NonDurable"));
                }
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            throw new UnsupportedOperationException();
        }
    }

    /*
     * <xsd:complexType name="around-invokeType">
     * <xsd:sequence>
     * <xsd:element name="class"
     * type="javaee:fully-qualified-classType"
     * minOccurs="0"/>
     * <xsd:element name="method-name"
     * type="javaee:java-identifierType"/>
     * </xsd:sequence>
     * </xsd:complexType>
     *
     * <xsd:complexType name="around-timeoutType">
     * <xsd:sequence>
     * <xsd:element name="class"
     * type="javaee:fully-qualified-classType"
     * minOccurs="0"/>
     * <xsd:element name="method-name"
     * type="javaee:java-identifierType"/>
     * </xsd:sequence>
     * </xsd:complexType>
     */
    static class InterceptorCallbackType extends DDParser.ElementContentParsable implements InterceptorCallback {

        public static class ListType extends ParsableListImplements<InterceptorCallbackType, InterceptorCallback> {
            @Override
            public InterceptorCallbackType newInstance(DDParser parser) {
                return new InterceptorCallbackType();
            }
        }

        @Override
        public String getClassName() {
            return class_name != null ? class_name.getValue() : null;
        }

        @Override
        public String getMethodName() {
            return method_name.getValue();
        }

        // elements
        XSDTokenType class_name;
        XSDTokenType method_name = new XSDTokenType();

        // internals
        final String class_element_type;
        final String method_name_element_type;

        InterceptorCallbackType(String class_element_type, String method_name_element_type) {
            this.class_element_type = class_element_type;
            this.method_name_element_type = method_name_element_type;
        }

        InterceptorCallbackType() {
            this("class", "method-name");
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (class_element_type.equals(localName)) {
                XSDTokenType class_name = new XSDTokenType();
                parser.parse(class_name);
                this.class_name = class_name;
                return true;
            }
            if (method_name_element_type.equals(localName)) {
                parser.parse(method_name);
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet(class_element_type, class_name);
            diag.describe(method_name_element_type, method_name);
        }
    }

    /*
     * <xsd:complexType name="lifecycle-callbackType">
     * <xsd:sequence>
     * <xsd:element name="lifecycle-callback-class"
     * type="javaee:fully-qualified-classType"
     * minOccurs="0"/>
     * <xsd:element name="lifecycle-callback-method"
     * type="javaee:java-identifierType"/>
     * </xsd:sequence>
     * </xsd:complexType>
     */
    static class LifecycleCallbackType extends InterceptorCallbackType implements LifecycleCallback {

        public static class ListType extends ParsableListImplements<LifecycleCallbackType, LifecycleCallback> {
            @Override
            public LifecycleCallbackType newInstance(DDParser parser) {
                return new LifecycleCallbackType();
            }
        }

        LifecycleCallbackType() {
            super("lifecycle-callback-class", "lifecycle-callback-method");
        }
    }

    /*
     * <xsd:complexType name="interceptorsType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="interceptor"
     * type="javaee:interceptorType"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class InterceptorsType extends DescribableType implements Interceptors {

        @Override
        public List<Interceptor> getInterceptorList() {
            return interceptor.getList();
        }

        // elements
        InterceptorType.ListType interceptor = new InterceptorType.ListType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("interceptor".equals(localName)) {
                InterceptorType interceptor = new InterceptorType();
                parser.parse(interceptor);
                addInterceptor(interceptor);
                return true;
            }
            return false;
        }

        private void addInterceptor(InterceptorType interceptor) {
            this.interceptor.add(interceptor);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("interceptor", interceptor);
        }
    }

    /*
     * <xsd:complexType name="interceptorType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="interceptor-class"
     * type="javaee:fully-qualified-classType"/>
     * <xsd:element name="around-invoke"
     * type="javaee:around-invokeType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="around-timeout"
     * type="javaee:around-timeoutType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:group ref="javaee:jndiEnvironmentRefsGroup"/>
     * <xsd:element name="post-activate"
     * type="javaee:lifecycle-callbackType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="pre-passivate"
     * type="javaee:lifecycle-callbackType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class InterceptorType extends JNDIEnvironmentRefsGroup implements Interceptor {

        public static class ListType extends ParsableListImplements<InterceptorType, Interceptor> {
            @Override
            public InterceptorType newInstance(DDParser parser) {
                return new InterceptorType();
            }
        }

        @Override
        public List<Description> getDescriptions() {
            if (description != null) {
                return description.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public String getInterceptorClassName() {
            return interceptor_class.getValue();
        }

        @Override
        public List<LifecycleCallback> getAroundConstruct() {
            if (around_construct != null) {
                return around_construct.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<InterceptorCallback> getAroundInvoke() {
            if (around_invoke != null) {
                return around_invoke.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<InterceptorCallback> getAroundTimeoutMethods() {
            if (around_timeout != null) {
                return around_timeout.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<LifecycleCallback> getPostActivate() {
            if (post_activate != null) {
                return post_activate.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<LifecycleCallback> getPrePassivate() {
            if (pre_passivate != null) {
                return pre_passivate.getList();
            } else {
                return Collections.emptyList();
            }
        }

        // elements
        DescriptionType.ListType description;
        XSDTokenType interceptor_class = new XSDTokenType();
        LifecycleCallbackType.ListType around_construct;
        InterceptorCallbackType.ListType around_invoke;
        InterceptorCallbackType.ListType around_timeout;
        LifecycleCallbackType.ListType post_activate;
        LifecycleCallbackType.ListType pre_passivate;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("description".equals(localName)) {
                DescriptionType description = new DescriptionType();
                parser.parse(description);
                addDescription(description);
                return true;
            }
            if ("interceptor-class".equals(localName)) {
                parser.parse(interceptor_class);
                return true;
            }
            if (parser.version >= EJBJar.VERSION_3_2 && "around-construct".equals(localName)) {
                LifecycleCallbackType around_construct = new LifecycleCallbackType();
                parser.parse(around_construct);
                addAroundConstruct(around_construct);
                return true;
            }
            if ("around-invoke".equals(localName)) {
                InterceptorCallbackType around_invoke = new InterceptorCallbackType();
                parser.parse(around_invoke);
                addAroundInvoke(around_invoke);
                return true;
            }
            if ("around-timeout".equals(localName)) {
                InterceptorCallbackType around_timeout = new InterceptorCallbackType();
                parser.parse(around_timeout);
                addAroundTimeout(around_timeout);
                return true;
            }
            if ("post-activate".equals(localName)) {
                LifecycleCallbackType post_activate = new LifecycleCallbackType();
                parser.parse(post_activate);
                addPostActivate(post_activate);
                return true;
            }
            if ("pre-passivate".equals(localName)) {
                LifecycleCallbackType pre_passivate = new LifecycleCallbackType();
                parser.parse(pre_passivate);
                addPrePassivate(pre_passivate);
                return true;
            }
            return false;
        }

        private void addDescription(DescriptionType description) {
            if (this.description == null) {
                this.description = new DescriptionType.ListType();
            }
            this.description.add(description);
        }

        private void addAroundConstruct(LifecycleCallbackType around_construct) {
            if (this.around_construct == null) {
                this.around_construct = new LifecycleCallbackType.ListType();
            }
            this.around_construct.add(around_construct);
        }

        private void addAroundInvoke(InterceptorCallbackType around_invoke) {
            if (this.around_invoke == null) {
                this.around_invoke = new InterceptorCallbackType.ListType();
            }
            this.around_invoke.add(around_invoke);
        }

        private void addAroundTimeout(InterceptorCallbackType around_timeout) {
            if (this.around_timeout == null) {
                this.around_timeout = new InterceptorCallbackType.ListType();
            }
            this.around_timeout.add(around_timeout);
        }

        private void addPostActivate(LifecycleCallbackType post_activate) {
            if (this.post_activate == null) {
                this.post_activate = new LifecycleCallbackType.ListType();
            }
            this.post_activate.add(post_activate);
        }

        private void addPrePassivate(LifecycleCallbackType pre_passivate) {
            if (this.pre_passivate == null) {
                this.pre_passivate = new LifecycleCallbackType.ListType();
            }
            this.pre_passivate.add(pre_passivate);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("description", description);
            diag.describe("interceptor-class", interceptor_class);
            diag.describeIfSet("around-construct", around_construct);
            diag.describeIfSet("around-invoke", around_invoke);
            diag.describeIfSet("around-timeout", around_timeout);
            super.describe(diag);
            diag.describeIfSet("post-activate", post_activate);
            diag.describeIfSet("pre-passivate", pre_passivate);
        }
    }

    /*
     * <xsd:unique name="relationship-name-uniqueness">
     * <xsd:selector xpath="javaee:ejb-relation"/>
     * <xsd:field xpath="javaee:ejb-relation-name"/>
     * </xsd:unique>
     *
     * <xsd:complexType name="relationshipsType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="ejb-relation"
     * type="javaee:ejb-relationType"
     * maxOccurs="unbounded">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class RelationshipsType extends DescribableType implements Relationships {

        @Override
        public List<EJBRelation> getEjbRelations() {
            return ejb_relation.getList();
        }

        // elements
        EJBRelationType.ListType ejb_relation = new EJBRelationType.ListType();

        //<xsd:unique name="relationship-name-uniqueness">
        //<xsd:selector xpath="javaee:ejb-relation"/>
        //<xsd:field xpath="javaee:ejb-relation-name"/>
        //</xsd:unique>

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("ejb-relation".equals(localName)) {
                EJBRelationType ejb_relation = new EJBRelationType();
                parser.parse(ejb_relation);
                addEJBRelation(ejb_relation);
                return true;
            }
            return false;
        }

        private void addEJBRelation(EJBRelationType ejb_relation) {
            this.ejb_relation.add(ejb_relation);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("ejb-relation", ejb_relation);
        }
    }

    /*
     * <xsd:unique name="role-name-uniqueness">
     * <xsd:selector xpath=".//javaee:ejb-relationship-role-name"/>
     * <xsd:field xpath="."/>
     * </xsd:unique>
     *
     * <xsd:complexType name="ejb-relationType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="ejb-relation-name"
     * type="javaee:string"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="ejb-relationship-role"
     * type="javaee:ejb-relationship-roleType"/>
     * <xsd:element name="ejb-relationship-role"
     * type="javaee:ejb-relationship-roleType"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class EJBRelationType extends DescribableType implements EJBRelation {

        public static class ListType extends ParsableListImplements<EJBRelationType, EJBRelation> {
            @Override
            public EJBRelationType newInstance(DDParser parser) {
                return new EJBRelationType();
            }
        }

        @Override
        public String getName() {
            return ejb_relation_name != null ? ejb_relation_name.getValue() : null;
        }

        @Override
        public List<EJBRelationshipRole> getRelationshipRoles() {
            return ejb_relationship_role.getList();
        }

        // elements
        XSDTokenType ejb_relation_name;
        EJBRelationshipRoleType.ListType ejb_relationship_role = new EJBRelationshipRoleType.ListType();

        // <xsd:unique name="role-name-uniqueness">
        // <xsd:selector xpath=".//javaee:ejb-relationship-role-name"/>
        // <xsd:field xpath="."/>
        // </xsd:unique>

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("ejb-relation-name".equals(localName)) {
                XSDTokenType ejb_relation_name = new XSDTokenType();
                parser.parse(ejb_relation_name);
                this.ejb_relation_name = ejb_relation_name;
                return true;
            }
            if ("ejb-relationship-role".equals(localName)) {
                EJBRelationshipRoleType ejb_relationship_role = new EJBRelationshipRoleType();
                parser.parse(ejb_relationship_role);
                addEJBRelationshipRole(ejb_relationship_role);
                return true;
            }
            return false;
        }

        private void addEJBRelationshipRole(EJBRelationshipRoleType ejb_relationship_role) {
            this.ejb_relationship_role.add(ejb_relationship_role);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("ejb-relation-name", ejb_relation_name);
            diag.describe("ejb-relationship-role", ejb_relationship_role);
        }
    }

    /*
     * <xsd:complexType name="ejb-relationship-roleType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="ejb-relationship-role-name"
     * type="javaee:string"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="multiplicity"
     * type="javaee:multiplicityType"/>
     * <xsd:element name="cascade-delete"
     * type="javaee:emptyType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="relationship-role-source"
     * type="javaee:relationship-role-sourceType"/>
     * <xsd:element name="cmr-field"
     * type="javaee:cmr-fieldType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class EJBRelationshipRoleType extends DescribableType implements EJBRelationshipRole {

        public static class ListType extends ParsableListImplements<EJBRelationshipRoleType, EJBRelationshipRole> {
            @Override
            public EJBRelationshipRoleType newInstance(DDParser parser) {
                return new EJBRelationshipRoleType();
            }
        }

        @Override
        public String getName() {
            return ejb_relationship_role_name != null ? ejb_relationship_role_name.getValue() : null;
        }

        @Override
        public int getMultiplicityTypeValue() {
            if (multiplicity != null) {
                switch (multiplicity.value) {
                    case One:
                        return MULTIPLICITY_TYPE_ONE;
                    case Many:
                        return MULTIPLICITY_TYPE_MANY;
                }
            }
            throw new IllegalStateException("multiplicity");
        }

        @Override
        public boolean isCascadeDelete() {
            return cascade_delete != null;
        }

        @Override
        public RelationshipRoleSource getSource() {
            return relationship_role_source;
        }

        @Override
        public CMRField getCmrField() {
            return cmr_field;
        }

        // elements
        XSDTokenType ejb_relationship_role_name;
        MultiplicityType multiplicity = new MultiplicityType();
        EmptyType cascade_delete;
        RelationshipRoleSourceType relationship_role_source = new RelationshipRoleSourceType();
        CMRFieldType cmr_field;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("ejb-relationship-role-name".equals(localName)) {
                XSDTokenType ejb_relationship_role_name = new XSDTokenType();
                parser.parse(ejb_relationship_role_name);
                this.ejb_relationship_role_name = ejb_relationship_role_name;
                return true;
            }
            if ("multiplicity".equals(localName)) {
                parser.parse(multiplicity);
                return true;
            }
            if ("cascade-delete".equals(localName)) {
                EmptyType cascade_delete = new EmptyType();
                parser.parse(cascade_delete);
                this.cascade_delete = cascade_delete;
                return true;
            }
            if ("relationship-role-source".equals(localName)) {
                parser.parse(relationship_role_source);
                return true;
            }
            if ("cmr-field".equals(localName)) {
                CMRFieldType cmr_field = new CMRFieldType();
                parser.parse(cmr_field);
                this.cmr_field = cmr_field;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("ejb-relationship-role-name", ejb_relationship_role_name);
            diag.describe("multiplicity", multiplicity);
            diag.describeIfSet("cascade-delete", cascade_delete);
            diag.describe("relationship-role-source", relationship_role_source);
            diag.describeIfSet("cmr-field", cmr_field);
        }
    }

    /*
     * <xsd:complexType name="multiplicityType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="One"/>
     * <xsd:enumeration value="Many"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class MultiplicityType extends XSDTokenType {

        static enum MultiplicityEnum {
            // lexical value must be (One|Many)
            One,
            Many;
        }

        // content
        MultiplicityEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, MultiplicityEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    /*
     * <xsd:complexType name="relationship-role-sourceType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="ejb-name"
     * type="javaee:ejb-nameType"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class RelationshipRoleSourceType extends DescribableType implements RelationshipRoleSource {

        @Override
        public String getEntityBeanName() {
            return ejb_name.getValue();
        }

        // elements
        XSDTokenType ejb_name = new XSDTokenType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("ejb-name".equals(localName)) {
                parser.parse(ejb_name);
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("ejb-name", ejb_name);
        }
    }

    /*
     * <xsd:complexType name="cmr-fieldType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="cmr-field-name"
     * type="javaee:string">
     * </xsd:element>
     * <xsd:element name="cmr-field-type"
     * type="javaee:cmr-field-typeType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class CMRFieldType extends DescribableType implements CMRField {

        @Override
        public String getName() {
            return cmr_field_name.getValue();
        }

        @Override
        public int getTypeValue() {
            if (cmr_field_type != null) {
                switch (cmr_field_type.value) {
                    case JavaUtilCollection:
                        return TYPE_JAVA_UTIL_COLLECTION;
                    case JavaUtilSet:
                        return TYPE_JAVA_UTIL_SET;
                }
            }
            return TYPE_UNSPECIFIED;
        }

        // elements
        XSDTokenType cmr_field_name = new XSDTokenType();
        CMRFieldTypeType cmr_field_type;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("cmr-field-name".equals(localName)) {
                parser.parse(cmr_field_name);
                return true;
            }
            if ("cmr-field-type".equals(localName)) {
                CMRFieldTypeType cmr_field_type = new CMRFieldTypeType();
                parser.parse(cmr_field_type);
                this.cmr_field_type = cmr_field_type;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("cmr-field-name", cmr_field_name);
            diag.describeIfSet("cmr-field-type", cmr_field_type);
        }
    }

    /*
     * <xsd:complexType name="cmr-field-typeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="java.util.Collection"/>
     * <xsd:enumeration value="java.util.Set"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class CMRFieldTypeType extends XSDTokenType {

        static enum CMRFieldTypeEnum {
            // lexical value must be (java.util.Collection|java.util.Set)
            JavaUtilCollection,
            JavaUtilSet;
        }

        // content
        CMRFieldTypeEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                String sVal = getValue();
                if (sVal.equals("java.util.Collection")) {
                    value = CMRFieldTypeEnum.JavaUtilCollection;
                } else if (sVal.equals("java.util.Set")) {
                    value = CMRFieldTypeEnum.JavaUtilSet;
                } else {
                    throw new IllegalArgumentException(sVal);
                }
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            if (value != null) {
                if (value == CMRFieldTypeEnum.JavaUtilCollection) {
                    diag.append("java.util.Collection");
                } else {
                    diag.append("java.util.Set");
                }
            }
        }
    }

    /*
     * <xsd:complexType name="assembly-descriptorType">
     * <xsd:sequence>
     * <xsd:element name="security-role"
     * type="javaee:security-roleType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="method-permission"
     * type="javaee:method-permissionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="container-transaction"
     * type="javaee:container-transactionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="interceptor-binding"
     * type="javaee:interceptor-bindingType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="message-destination"
     * type="javaee:message-destinationType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="exclude-list"
     * type="javaee:exclude-listType"
     * minOccurs="0"/>
     * <xsd:element name="application-exception"
     * type="javaee:application-exceptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class AssemblyDescriptorType extends DDParser.ElementContentParsable implements AssemblyDescriptor {

        @Override
        public List<SecurityRole> getSecurityRoles() {
            if (security_role != null) {
                return security_role.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<MethodPermission> getMethodPermissions() {
            if (method_permission != null) {
                return method_permission.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<ContainerTransaction> getContainerTransactions() {
            if (container_transaction != null) {
                return container_transaction.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<InterceptorBinding> getInterceptorBinding() {
            if (interceptor_binding != null) {
                return interceptor_binding.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<MessageDestination> getMessageDestinations() {
            if (message_destination != null) {
                return message_destination.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public ExcludeList getExcludeList() {
            return exclude_list;
        }

        @Override
        public List<ApplicationException> getApplicationExceptionList() {
            if (application_exception != null) {
                return application_exception.getList();
            } else {
                return Collections.emptyList();
            }
        }

        // elements
        SecurityRoleType.ListType security_role;
        MethodPermissionType.ListType method_permission;
        ContainerTransactionType.ListType container_transaction;
        InterceptorBindingType.ListType interceptor_binding;
        MessageDestinationType.ListType message_destination;
        ExcludeListType exclude_list;
        ApplicationExceptionType.ListType application_exception;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("security-role".equals(localName)) {
                SecurityRoleType security_role = new SecurityRoleType();
                parser.parse(security_role);
                addSecurityRole(security_role);
                return true;
            }
            if ("method-permission".equals(localName)) {
                MethodPermissionType method_permission = new MethodPermissionType();
                parser.parse(method_permission);
                addMethodPermission(method_permission);
                return true;
            }
            if ("container-transaction".equals(localName)) {
                ContainerTransactionType container_transaction = new ContainerTransactionType();
                parser.parse(container_transaction);
                addContainerTransaction(container_transaction);
                return true;
            }
            if ("interceptor-binding".equals(localName)) {
                InterceptorBindingType interceptor_binding = new InterceptorBindingType();
                parser.parse(interceptor_binding);
                addInterceptorBinding(interceptor_binding);
                return true;
            }
            if ("message-destination".equals(localName)) {
                MessageDestinationType message_destination = new MessageDestinationType();
                parser.parse(message_destination);
                addMessageDestination(message_destination);
                return true;
            }
            if ("exclude-list".equals(localName)) {
                ExcludeListType exclude_list = new ExcludeListType();
                parser.parse(exclude_list);
                this.exclude_list = exclude_list;
                return true;
            }
            if ("application-exception".equals(localName)) {
                ApplicationExceptionType application_exception = new ApplicationExceptionType();
                parser.parse(application_exception);
                addApplicationException(application_exception);
                return true;
            }
            return false;
        }

        private void addSecurityRole(SecurityRoleType security_role) {
            if (this.security_role == null) {
                this.security_role = new SecurityRoleType.ListType();
            }
            this.security_role.add(security_role);
        }

        private void addMethodPermission(MethodPermissionType method_permission) {
            if (this.method_permission == null) {
                this.method_permission = new MethodPermissionType.ListType();
            }
            this.method_permission.add(method_permission);
        }

        private void addContainerTransaction(ContainerTransactionType container_transaction) {
            if (this.container_transaction == null) {
                this.container_transaction = new ContainerTransactionType.ListType();
            }
            this.container_transaction.add(container_transaction);
        }

        private void addInterceptorBinding(InterceptorBindingType interceptor_binding) {
            if (this.interceptor_binding == null) {
                this.interceptor_binding = new InterceptorBindingType.ListType();
            }
            this.interceptor_binding.add(interceptor_binding);
        }

        private void addMessageDestination(MessageDestinationType message_destination) {
            if (this.message_destination == null) {
                this.message_destination = new MessageDestinationType.ListType();
            }
            this.message_destination.add(message_destination);
        }

        private void addApplicationException(ApplicationExceptionType application_exception) {
            if (this.application_exception == null) {
                this.application_exception = new ApplicationExceptionType.ListType();
            }
            this.application_exception.add(application_exception);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("security-role", security_role);
            diag.describeIfSet("method-permission", method_permission);
            diag.describeIfSet("container-transaction", container_transaction);
            diag.describeIfSet("interceptor-binding", interceptor_binding);
            diag.describeIfSet("message-destination", message_destination);
            diag.describeIfSet("exclude-list", exclude_list);
            diag.describeIfSet("application-exception", application_exception);
        }
    }

    /*
     * <xsd:complexType name="method-permissionType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:choice>
     * <xsd:element name="role-name"
     * type="javaee:role-nameType"
     * maxOccurs="unbounded"/>
     * <xsd:element name="unchecked"
     * type="javaee:emptyType">
     * </xsd:element>
     * </xsd:choice>
     * <xsd:element name="method"
     * type="javaee:methodType"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class MethodPermissionType extends DescribableType implements MethodPermission {

        public static class ListType extends ParsableListImplements<MethodPermissionType, MethodPermission> {
            @Override
            public MethodPermissionType newInstance(DDParser parser) {
                return new MethodPermissionType();
            }
        }

        @Override
        public List<String> getRoleNames() {
            return role_name.getList();
        }

        @Override
        public boolean isUnchecked() {
            return unchecked != null;
        }

        @Override
        public List<Method> getMethodElements() {
            return method.getList();
        }

        // elements
        XSDTokenType.ListType role_name = new XSDTokenType.ListType();
        EmptyType unchecked;
        MethodType.ListType method = new MethodType.ListType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("role-name".equals(localName)) {
                XSDTokenType role_name = new XSDTokenType();
                parser.parse(role_name);
                addRoleName(role_name);
                return true;
            }
            if ("unchecked".equals(localName)) {
                EmptyType unchecked = new EmptyType();
                parser.parse(unchecked);
                this.unchecked = unchecked;
                return true;
            }
            if ("method".equals(localName)) {
                MethodType method = new MethodType();
                parser.parse(method);
                addMethod(method);
                return true;
            }
            return false;
        }

        private void addRoleName(XSDTokenType role_name) {
            this.role_name.add(role_name);
        }

        private void addMethod(MethodType method) {
            this.method.add(method);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("role-name", role_name);
            diag.describeIfSet("unchecked", unchecked);
            diag.describe("method", method);
        }
    }

    /*
     * <xsd:complexType name="methodType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="ejb-name"
     * type="javaee:ejb-nameType"/>
     * <xsd:element name="method-intf"
     * type="javaee:method-intfType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="method-name"
     * type="javaee:method-nameType"/>
     * <xsd:element name="method-params"
     * type="javaee:method-paramsType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class MethodType extends DescribableType implements Method {

        public static class ListType extends ParsableListImplements<MethodType, Method> {
            @Override
            public MethodType newInstance(DDParser parser) {
                return new MethodType();
            }
        }

        @Override
        public String getEnterpriseBeanName() {
            return ejb_name.getValue();
        }

        @Override
        public int getInterfaceTypeValue() {
            return method_intf != null ? method_intf.value.value : INTERFACE_TYPE_UNSPECIFIED;
        }

        @Override
        public String getMethodName() {
            return method_name.getValue();
        }

        @Override
        public List<String> getMethodParamList() {
            if (method_params != null) {
                return method_params.getList();
            } else {
                return null;
            }
        }

        // elements
        XSDTokenType ejb_name = new XSDTokenType();
        MethodIntfType method_intf;
        XSDTokenType method_name = new XSDTokenType();
        MethodParamsType method_params;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("ejb-name".equals(localName)) {
                parser.parse(ejb_name);
                return true;
            }
            if ("method-intf".equals(localName)) {
                MethodIntfType method_intf = new MethodIntfType();
                parser.parse(method_intf);
                this.method_intf = method_intf;
                return true;
            }
            if ("method-name".equals(localName)) {
                parser.parse(method_name);
                return true;
            }
            if ("method-params".equals(localName)) {
                MethodParamsType method_params = new MethodParamsType();
                parser.parse(method_params);
                this.method_params = method_params;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("ejb-name", ejb_name);
            diag.describeIfSet("method-intf", method_intf);
            diag.describe("method-name", method_name);
            diag.describeIfSet("method-params", method_params);
        }
    }

    /*
     * <xsd:complexType name="method-intfType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="Home"/>
     * <xsd:enumeration value="Remote"/>
     * <xsd:enumeration value="LocalHome"/>
     * <xsd:enumeration value="Local"/>
     * <xsd:enumeration value="ServiceEndpoint"/>
     * <xsd:enumeration value="Timer"/>
     * <xsd:enumeration value="MessageEndpoint"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class MethodIntfType extends XSDTokenType {

        static enum MethodIntfEnum implements DDParser.VersionedEnum {
            Home(Method.INTERFACE_TYPE_HOME),
            Remote(Method.INTERFACE_TYPE_REMOTE),
            LocalHome(Method.INTERFACE_TYPE_LOCAL_HOME),
            Local(Method.INTERFACE_TYPE_LOCAL),
            ServiceEndpoint(Method.INTERFACE_TYPE_SERVICE_ENDPOINT),
            Timer(Method.INTERFACE_TYPE_TIMER),
            MessageEndpoint(Method.INTERFACE_TYPE_MESSAGE_ENDPOINT),
            LifecycleCallback(Method.INTERFACE_TYPE_LIFECYCLE_CALLBACK, EJBJar.VERSION_3_2);

            final int value;
            final int minVersion;

            MethodIntfEnum(int value) {
                this(value, 0);
            }

            MethodIntfEnum(int value, int minVersion) {
                this.value = value;
                this.minVersion = minVersion;
            }

            @Override
            public int getMinVersion() {
                return minVersion;
            }
        }

        // content
        MethodIntfEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, MethodIntfEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    /*
     * <xsd:complexType name="container-transactionType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="method"
     * type="javaee:methodType"
     * maxOccurs="unbounded"/>
     * <xsd:element name="trans-attribute"
     * type="javaee:trans-attributeType"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ContainerTransactionType extends DescribableType implements ContainerTransaction {

        public static class ListType extends ParsableListImplements<ContainerTransactionType, ContainerTransaction> {
            @Override
            public ContainerTransactionType newInstance(DDParser parser) {
                return new ContainerTransactionType();
            }
        }

        @Override
        public List<Method> getMethodElements() {
            return method.getList();
        }

        @Override
        public int getTransAttributeTypeValue() {
            if (trans_attribute != null) {
                switch (trans_attribute.value) {
                    case NotSupported:
                        return TRANS_ATTRIBUTE_NOT_SUPPORTED;
                    case Supports:
                        return TRANS_ATTRIBUTE_SUPPORTS;
                    case Required:
                        return TRANS_ATTRIBUTE_REQUIRED;
                    case RequiresNew:
                        return TRANS_ATTRIBUTE_REQUIRES_NEW;
                    case Mandatory:
                        return TRANS_ATTRIBUTE_MANDATORY;
                    case Never:
                        return TRANS_ATTRIBUTE_NEVER;
                }
            }
            throw new IllegalStateException("trans-attribute");
        }

        // elements
        MethodType.ListType method = new MethodType.ListType();
        TransAttributeType trans_attribute = new TransAttributeType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("method".equals(localName)) {
                MethodType method = new MethodType();
                parser.parse(method);
                addMethod(method);
                return true;
            }
            if ("trans-attribute".equals(localName)) {
                parser.parse(trans_attribute);
                return true;
            }
            return false;
        }

        private void addMethod(MethodType method) {
            this.method.add(method);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("method", method);
            diag.describe("trans-attribute", trans_attribute);
        }
    }

    /*
     * <xsd:complexType name="trans-attributeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:string">
     * <xsd:enumeration value="NotSupported"/>
     * <xsd:enumeration value="Supports"/>
     * <xsd:enumeration value="Required"/>
     * <xsd:enumeration value="RequiresNew"/>
     * <xsd:enumeration value="Mandatory"/>
     * <xsd:enumeration value="Never"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class TransAttributeType extends XSDTokenType {

        static enum TransAttributeEnum {
            // lexical value must be (NotSupported|Supports|Required|RequiresNew|Mandatory|Never)
            NotSupported,
            Supports,
            Required,
            RequiresNew,
            Mandatory,
            Never;
        }

        // content
        TransAttributeEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, TransAttributeEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    /*
     * <xsd:complexType name="interceptor-bindingType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="ejb-name"
     * type="javaee:string"/>
     * <xsd:choice>
     * <xsd:element name="interceptor-class"
     * type="javaee:fully-qualified-classType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="interceptor-order"
     * type="javaee:interceptor-orderType"
     * minOccurs="1"/>
     * </xsd:choice>
     * <xsd:element name="exclude-default-interceptors"
     * type="javaee:true-falseType"
     * minOccurs="0"/>
     * <xsd:element name="exclude-class-interceptors"
     * type="javaee:true-falseType"
     * minOccurs="0"/>
     * <xsd:element name="method"
     * type="javaee:named-methodType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class InterceptorBindingType extends DescribableType implements InterceptorBinding {

        public static class ListType extends ParsableListImplements<InterceptorBindingType, InterceptorBinding> {
            @Override
            public InterceptorBindingType newInstance(DDParser parser) {
                return new InterceptorBindingType();
            }
        }

        @Override
        public String getEjbName() {
            return ejb_name.getValue();
        }

        @Override
        public List<String> getInterceptorClassNames() {
            if (interceptor_class != null) {
                return interceptor_class.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public InterceptorOrder getInterceptorOrder() {
            return interceptor_order;
        }

        @Override
        public boolean isSetExcludeDefaultInterceptors() {
            return exclude_default_interceptors != null;
        }

        @Override
        public boolean isExcludeDefaultInterceptors() {
            return exclude_default_interceptors != null ? exclude_default_interceptors.getBooleanValue() : false;
        }

        @Override
        public boolean isSetExcludeClassInterceptors() {
            return exclude_class_interceptors != null;
        }

        @Override
        public boolean isExcludeClassInterceptors() {
            return exclude_class_interceptors != null ? exclude_class_interceptors.getBooleanValue() : false;
        }

        @Override
        public NamedMethod getMethod() {
            return method;
        }

        // elements
        XSDTokenType ejb_name = new XSDTokenType();
        XSDTokenType.ListType interceptor_class;
        InterceptorOrderType interceptor_order;
        XSDBooleanType exclude_default_interceptors;
        XSDBooleanType exclude_class_interceptors;
        NamedMethodType method;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("ejb-name".equals(localName)) {
                parser.parse(ejb_name);
                return true;
            }
            if ("interceptor-class".equals(localName)) {
                XSDTokenType interceptor_class = new XSDTokenType();
                parser.parse(interceptor_class);
                addInterceptorClass(interceptor_class);
                return true;
            }
            if ("interceptor-order".equals(localName)) {
                InterceptorOrderType interceptor_order = new InterceptorOrderType();
                parser.parse(interceptor_order);
                this.interceptor_order = interceptor_order;
                return true;
            }
            if ("exclude-default-interceptors".equals(localName)) {
                XSDBooleanType exclude_default_interceptors = new XSDBooleanType();
                parser.parse(exclude_default_interceptors);
                this.exclude_default_interceptors = exclude_default_interceptors;
                return true;
            }
            if ("exclude-class-interceptors".equals(localName)) {
                XSDBooleanType exclude_class_interceptors = new XSDBooleanType();
                parser.parse(exclude_class_interceptors);
                this.exclude_class_interceptors = exclude_class_interceptors;
                return true;
            }
            if ("method".equals(localName)) {
                NamedMethodType method = new NamedMethodType();
                parser.parse(method);
                this.method = method;
                return true;
            }
            return false;
        }

        private void addInterceptorClass(XSDTokenType interceptor_class) {
            if (this.interceptor_class == null) {
                this.interceptor_class = new XSDTokenType.ListType();
            }
            this.interceptor_class.add(interceptor_class);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("ejb-name", ejb_name);
            diag.describeIfSet("interceptor-class", interceptor_class);
            diag.describeIfSet("interceptor-order", interceptor_order);
            diag.describeIfSet("exclude-default-interceptors", exclude_default_interceptors);
            diag.describeIfSet("exclude-class-interceptors", exclude_class_interceptors);
            diag.describeIfSet("method", method);
        }
    }

    /*
     * <xsd:complexType name="interceptor-orderType">
     * <xsd:sequence>
     * <xsd:element name="interceptor-class"
     * type="javaee:fully-qualified-classType"
     * minOccurs="1"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class InterceptorOrderType extends DDParser.ElementContentParsable implements InterceptorOrder {

        @Override
        public List<String> getInterceptorClassNames() {
            if (interceptor_class != null) {
                return interceptor_class.getList();
            } else {
                return Collections.emptyList();
            }
        }

        // elements
        XSDTokenType.ListType interceptor_class = new XSDTokenType.ListType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("interceptor-class".equals(localName)) {
                XSDTokenType interceptor_class = new XSDTokenType();
                parser.parse(interceptor_class);
                addInterceptorClass(interceptor_class);
                return true;
            }
            return false;
        }

        private void addInterceptorClass(XSDTokenType interceptor_class) {
            this.interceptor_class.add(interceptor_class);
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describeIfSet("interceptor-class", interceptor_class);
        }
    }

    /*
     * <xsd:complexType name="exclude-listType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="method"
     * type="javaee:methodType"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ExcludeListType extends DescribableType implements ExcludeList {

        @Override
        public List<Method> getMethodElements() {
            return method.getList();
        }

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        // elements
        MethodType.ListType method = new MethodType.ListType();

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("method".equals(localName)) {
                MethodType method = new MethodType();
                parser.parse(method);
                addMethod(method);
                return true;
            }
            return false;
        }

        private void addMethod(MethodType method) {
            this.method.add(method);
        }

        @Override
        public void describe(Diagnostics diag) {
            super.describe(diag);
            diag.describe("method", method);
        }
    }

    /*
     * <xsd:complexType name="application-exceptionType">
     * <xsd:sequence>
     * <xsd:element name="exception-class"
     * type="javaee:fully-qualified-classType"/>
     * <xsd:element name="rollback"
     * type="javaee:true-falseType"
     * minOccurs="0"/>
     * <xsd:element name="inherited"
     * type="javaee:true-falseType"
     * minOccurs="0"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class ApplicationExceptionType extends DDParser.ElementContentParsable implements ApplicationException {

        public static class ListType extends ParsableListImplements<ApplicationExceptionType, ApplicationException> {
            @Override
            public ApplicationExceptionType newInstance(DDParser parser) {
                return new ApplicationExceptionType();
            }
        }

        @Override
        public String getExceptionClassName() {
            return exception_class.getValue();
        }

        @Override
        public boolean isSetRollback() {
            return rollback != null;
        }

        @Override
        public boolean isRollback() {
            return rollback != null ? rollback.getBooleanValue() : false;
        }

        @Override
        public boolean isSetInherited() {
            return inherited != null;
        }

        @Override
        public boolean isInherited() {
            // Per XSD and specification: If not specified, this value defaults to true.
            return inherited != null ? inherited.getBooleanValue() : true;
        }

        // elements
        XSDTokenType exception_class = new XSDTokenType();
        XSDBooleanType rollback;
        XSDBooleanType inherited;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("exception-class".equals(localName)) {
                parser.parse(exception_class);
                return true;
            }
            if ("rollback".equals(localName)) {
                XSDBooleanType rollback = new XSDBooleanType();
                parser.parse(rollback);
                this.rollback = rollback;
                return true;
            }
            if ("inherited".equals(localName)) {
                XSDBooleanType inherited = new XSDBooleanType();
                parser.parse(inherited);
                this.inherited = inherited;
                return true;
            }
            return false;
        }

        @Override
        public void describe(Diagnostics diag) {
            diag.describe("exception-class", exception_class);
            diag.describe("rollback", rollback);
            diag.describe("inherited", inherited);
        }
    }
}
