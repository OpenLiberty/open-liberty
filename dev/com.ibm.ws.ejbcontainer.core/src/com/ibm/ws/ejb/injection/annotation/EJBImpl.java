/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejb.injection.annotation;

import java.lang.annotation.Annotation;
import javax.ejb.EJB;

/**
 * Implementation of the EJB Spec defined @EJB annotation. <p>
 * 
 * This implementation of the @EJB annotation is for use by
 * EJB Injection Processors, allowing XML ejb-ref and ejb-local-ref
 * stanzas to be represented as annotations.
 * 
 * @see javax.ejb.EJB
 **/
public class EJBImpl implements EJB
{
    public String ivName = "";
    public boolean ivIsSetName = false;
    public Class<?> ivBeanInterface = Object.class; // default if not set by xml or annotations
    public boolean ivIsSetBeanInterface = false;
    public String ivBeanName = "";
    public boolean ivIsSetBeanName = false;

    // We don't currently use mapped name for anything, but is is required by the Resource interface
    public String ivMappedName = "";
    public boolean ivIsSetMappedName = false;
    // We don't currently use mapped name for anything, but is is required by the EJB interface
    public String ivDescription = ""; // default if not set by xml or annotations
    public boolean ivIsSetDescription = false;

    public String ivLookup = ""; // default defined by EJB annotation class

    public EJBImpl(String jndiName,
                   Class<?> beanInterface,
                   String beanName,
                   String mappedName,
                   String description,
                   String lookup)
    {
        if ((jndiName != null) && (!(jndiName.equals("")))) {
            ivName = jndiName;
            ivIsSetName = true;
        }
        if (beanInterface != null) {
            ivBeanInterface = beanInterface;
            ivIsSetBeanInterface = true;
        }
        if ((beanName != null) && (!(beanName.equals("")))) {
            ivBeanName = beanName;
            ivIsSetBeanName = true;
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
        return EJB.class;
    }

    /**
     * @see javax.ejb.EJB#name
     **/
    public String name()
    {
        return ivName;
    }

    /**
     * @see javax.ejb.EJB#beanInterface
     **/
    public Class<?> beanInterface()
    {
        return ivBeanInterface;
    }

    /**
     * @see javax.ejb.EJB#beanName
     **/
    public String beanName()
    {
        return ivBeanName;
    }

    /**
     * @see javax.ejb.EJB#mappedName
     **/
    public String mappedName()
    {
        return ivMappedName;
    }

    /**
     * @see javax.ejb.EJB#description
     **/
    public String description()
    {
        return ivDescription;
    }

    /**
     * @see javax.ejb.EJB#lookup
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
        if (obj instanceof EJB)
        {
            EJB ejb = (EJB) obj;
            if (this.annotationType().equals(ejb.annotationType()) &&
                this.ivName.equals(ejb.name()) &&
                this.ivBeanInterface.equals(ejb.beanInterface()) &&
                this.ivBeanName.equals(ejb.beanName()) &&
                this.ivMappedName.equals(ejb.mappedName()) &&
                this.ivDescription.equals(ejb.description()) &&
                this.ivLookup.equals(ejb.lookup()))
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
        hash += "beanInterface".hashCode() * 127 ^ ivBeanInterface.hashCode();
        hash += "beanName".hashCode() * 127 ^ ivBeanName.hashCode();
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
        str.append("(name=").append(ivName);
        str.append(", beanInterface=class ").append(ivBeanInterface.getName());
        str.append(", beanName=").append(ivBeanName);
        str.append(", mappedName=").append(ivMappedName);
        str.append(", description=").append(ivDescription);
        str.append(", lookup=").append(ivLookup);

        return str.toString();
    }

}
