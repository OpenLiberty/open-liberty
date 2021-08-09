/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.annotation;

import java.lang.annotation.Annotation;
import javax.annotation.Resource;

/**
 * Implementation of the Java EE Spec defined @Resource annotation. <p>
 *
 * This implementation of the @Resource annotation is for use by
 * Resource Injection Processors, allowing XML env-entry, resource-ref,
 * resource-env-ref, etc... stanzas to be represented as annotations.
 * Note that this class is NOT only used to hold XML data. It may holddata
 * scanned from annotations. The IsSet flags indicate whether a given stanza was
 * present in a scanned xml file. This is important because this object
 * is also used to hold the result of the XML and annotation merge.
 *
 * @see javax.annotation.Resource
 **/
public class ResourceImpl implements Resource
{
    public String ivName; // no default - required field in xml, otherwise default provided by annotation.
    public boolean ivIsSetName = false;
    public Class<?> ivType = java.lang.Object.class; // default if not set by xml or annotations
    public boolean ivIsSetType = false;
    public AuthenticationType ivAuthenticationType = AuthenticationType.CONTAINER; // default if not set by xml or annotations
    public boolean ivIsSetAuthenticationType = false;
    public boolean ivShareable = true; // default if not set by xml or annotations
    public boolean ivIsSetShareable = false;
    public String ivDescription = ""; // default if not set by xml or annotations
    public boolean ivIsSetDescription = false;

    // We don't currently use mapped name for anything, but is is required by the Resource interface
    public String ivMappedName = "";
    public boolean ivIsSetMappedName = false;

    public String ivLookup = ""; // default defined by annotation class

    /**
     * Constructor for non resource-ref resources, which do not use
     * the authentication type and shareable fields.
     **/
    public ResourceImpl(String jndiName,
                        Class<?> type,
                        String mappedName,
                        String description,
                        String lookup)
    {
        if ((jndiName != null) && (!(jndiName.equals("")))) {
            ivName = jndiName;
            ivIsSetName = true;
        }
        if (type != null) {
            ivType = type;
            ivIsSetType = true;
        }
        if ((mappedName != null) && (!(mappedName.equals("")))) {
            ivMappedName = mappedName;
            ivIsSetMappedName = true;
        }
        if ((description != null) && (!(description.equals("")))) {
            ivDescription = description;
            ivIsSetDescription = true;
        }

        // Note this is inconsistent with the above code, as it considers
        // an empty string in XML to be significant.  This allows the customer
        // to "turn off" a lookup that was specified in annotations, and use
        // ejb-link or auto-link instead.                             F743-21028.4
        if (lookup != null) {
            ivLookup = lookup;
        }
    }

    /**
     * Constructor for resource-ref resources, which do use the
     * authentication type and shareable fields.
     **/
    public ResourceImpl(String jndiName,
                        Class<?> type,
                        AuthenticationType authenticationType,
                        Boolean shareable,
                        String mappedName,
                        String description,
                        String lookup)
    {
        if ((jndiName != null) && (!(jndiName.equals("")))) {
            ivName = jndiName;
            ivIsSetName = true;
        }
        if (type != null) {
            ivType = type;
            ivIsSetType = true;
        }
        if (authenticationType != null) {
            ivAuthenticationType = authenticationType;
            ivIsSetAuthenticationType = true;
        }
        if (shareable != null) {
            ivShareable = shareable.booleanValue();
            ivIsSetShareable = true;
        }
        if ((mappedName != null) && (!(mappedName.equals("")))) {
            ivMappedName = mappedName;
            ivIsSetMappedName = true;
        }
        if ((description != null) && (!(description.equals("")))) {
            ivDescription = description;
            ivIsSetDescription = true;
        }

        // Note this is inconsistent with the above code, as it considers
        // an empty string in XML to be significant.  This allows the customer
        // to "turn off" a lookup that was specified in annotations, and use
        // ejb-link or auto-link instead.                             F743-21028.4
        if (lookup != null) {
            ivLookup = lookup;
        }
    }

    /**
     * @see java.lang.annotation.Annotation#annotationType
     **/
    public Class<? extends Annotation> annotationType()
    {
        return Resource.class;
    }

    /**
     * @see javax.annotation.Resource#name
     **/
    public String name()
    {
        return ivName;
    }

    /**
     * @see javax.annotation.Resource#type
     **/
    public Class<?> type()
    {
        return ivType;
    }

    /**
     * @see javax.annotation.Resource#authenticationType
     **/
    public AuthenticationType authenticationType()
    {
        return ivAuthenticationType;
    }

    /**
     * @see javax.annotation.Resource#shareable
     **/
    public boolean shareable()
    {
        return ivShareable;
    }

    /**
     * @see javax.annotation.Resource#mappedName
     **/
    public String mappedName()
    {
        return ivMappedName;
    }

    /**
     * @see javax.annotation.Resource#description
     **/
    public String description()
    {
        return ivDescription;
    }

    /**
     * @see javax.annotation.Resource#lookup
     **/
    // F743-21028.4
    public String lookup()
    {
        return ivLookup;
    }

    /**
     * @see java.lang.annotation.Annotation#equals
     **/
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Resource)
        {
            Resource resource = (Resource) obj;
            if (this.annotationType().equals(resource.annotationType()) &&
                this.ivName.equals(resource.name()) &&
                this.ivType.equals(resource.type()) &&
                this.ivAuthenticationType.equals(resource.authenticationType()) &&
                (this.ivShareable == resource.shareable()) &&
                ivMappedName.equals(resource.mappedName()) &&
                ivDescription.equals(resource.description()) &&
                ivLookup.equals(resource.lookup()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @see java.lang.annotation.Annotation#hashCode
     **/
    @Override
    public int hashCode()
    {
        int hash = 0;

        hash += "name".hashCode() * 127 ^ ivName.hashCode();
        hash += "type".hashCode() * 127 ^ ivType.hashCode();
        hash += "authenticationType".hashCode() * 127 ^ ivAuthenticationType.hashCode();
        hash += "shareable".hashCode() * 127 ^ Boolean.valueOf(ivShareable).hashCode();
        hash += "mappedName".hashCode() * 127 ^ ivMappedName.hashCode();
        hash += "description".hashCode() * 127 ^ ivDescription.hashCode();
        hash += "lookup".hashCode() * 127 ^ ivLookup.hashCode();

        return hash;
    }

    /**
     * @see java.lang.annotation.Annotation#toString
     **/
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder(256);

        str.append("@");
        str.append(annotationType().getName());
        str.append(", name=").append(ivName);
        str.append(", isSetName=").append(ivIsSetName);
        str.append(", type=class ").append(ivType.getName());
        str.append(", isSetType=").append(ivIsSetType);
        str.append(", authenticationType=").append(ivAuthenticationType);
        str.append(", isSetAuthenticationType=").append(ivIsSetAuthenticationType);
        str.append(", shareable=").append(ivShareable);
        str.append(", isSetShareable=").append(ivIsSetShareable);
        str.append(", mappedName=").append(ivMappedName);
        str.append(", isSetMappedName=").append(ivIsSetMappedName);
        str.append(", description=").append(ivDescription);
        str.append(", isSetDescription=").append(ivIsSetDescription);
        str.append(", lookup=").append(ivLookup);

        return str.toString();
    }

} //ResourceImpl

