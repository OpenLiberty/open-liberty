/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine.factory;

import java.io.Serializable;

import javax.annotation.Resource.AuthenticationType;

import com.ibm.ws.resource.ResourceRefConfig;

/**
 * Resource Information for resource injection resolution. Used to support
 * Resource injection of data types beyond those defined by the Java EE
 * core specification. <p>
 *
 * When a component registers an ObjectFactory in support of a data type
 * beyond those defined by the specification, an instance of this object
 * will be provided to that factory with the information provided by
 * the Resource annotation or corresponding XML. <p>
 *
 * This class is NOT used when a binding override has been provided.
 * When a binding has been provided, the built in naming indirect
 * lookup support is used. <p>
 */
public class ResourceInfo implements Serializable
{
    private static final long serialVersionUID = 2086085582533155160L;

    /**
     * Application name where the resource reference was defined.
     **/
    private final String ivApplication;

    /**
     * Module name where the resource reference was defined.
     **/
    private final String ivModule;

    /**
     * Component name where the resource reference was defined. <p>
     *
     * Will be null for references defined at the module level
     * (i.e. for example in a war).
     **/
    private final String ivComponent;

    /**
     * The JNDI name where this Resource reference is bound into the component
     * naming context (java:comp/env). This name does NOT include the string
     * "java:comp/env". <p>
     *
     * This will be the 'name' property of the corresponding annotation or
     * XML definition. If not present, it will be the default as defined
     * by the Java EE Specification. <p>
     **/
    private final String ivJndiName;

    /**
     * Class name of the data type for this Resource reference. The associated
     * ObjectFactory must return an object that may be cast to this type.
     **/
    private final String ivType;

    /**
     * The authentication type specified by the resource annotation or XML.
     **/
    private final String ivAuthenticationType;

    /**
     * The shareable setting specified by the resource annotation or XML.
     **/
    private final boolean ivShareable;

    /**
     * The message destination link setting, or null if non-applicable or unset.
     * This field is transient because it is not applicable to ObjectFactory.
     */
    private transient String ivLink; // F48603.9

    private transient ResourceRefConfig ivConfig;

    /**
     * Construct an instance for a specified @Resource metadata or equivalent
     * XML metadata.
     *
     * @param application name of the application where the reference was defined.
     * @param module name of the module where the reference was defined.
     * @param component name of the component (EJB) where the reference was defined.
     * @param jndiName name in the java:comp/env name context.
     * @param type data type the reference refers to.
     * @param authType authentication type specified on the reference definition.
     * @param shareable the shareable setting of the reference definition.
     * @param link the message-destination-link, if applicable
     */
    public ResourceInfo(String application,
                        String module,
                        String component,
                        String jndiName,
                        String type,
                        AuthenticationType authType,
                        boolean shareable,
                        String link,
                        ResourceRefConfig config)
    {
        ivApplication = application;
        ivModule = module;
        ivComponent = component;
        ivJndiName = jndiName;
        ivType = type;
        ivAuthenticationType = authType.toString();
        ivShareable = shareable;
        ivLink = link;
        ivConfig = config;
    }

    /**
     * Returns the application name where the resource reference was defined. <p>
     **/
    public String getApplication()
    {
        return ivApplication;
    }

    /**
     * Returns the module name where the resource reference was defined. <p>
     **/
    public String getModule()
    {
        return ivModule;
    }

    /**
     * Returns the component name where the resource reference was defined. <p>
     *
     * Will be null for references defined at the module level
     * (i.e. for example in a war). <p>
     **/
    public String getComponent()
    {
        return ivComponent;
    }

    /**
     * Returns the JNDI name where this Resource reference is bound into the
     * component naming context (java:comp/env). This name does NOT include
     * the string "java:comp/env". <p>
     *
     * This will be the 'name' property of the corresponding annotation or
     * XML definition. If not present, it will be the default as defined
     * by the Java EE Specification. <p>
     **/
    public String getName()
    {
        return ivJndiName;
    }

    /**
     * Returns the Class name of the data type for this Resource reference. <p>
     *
     * The associated ObjectFactory must return an object that may be cast to
     * this type. <p>
     **/
    public String getType()
    {
        return ivType;
    }

    /**
     * Returns the authentication type specified by the resource annotation or XML.
     **/
    public AuthenticationType getAuthenticationType()
    {
        return Enum.valueOf(AuthenticationType.class, ivAuthenticationType);
    }

    /**
     * Returns the shareable setting specified by the resource annotation or XML.
     **/
    public boolean isShareable()
    {
        return ivShareable;
    }

    /**
     * Returns the message destination link specified by XML.
     */
    public String getLink() // F48603.9
    {
        return ivLink;
    }

    public ResourceRefConfig getConfig()
    {
        return ivConfig;
    }
}
