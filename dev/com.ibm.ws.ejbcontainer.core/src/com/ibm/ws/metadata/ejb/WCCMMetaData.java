/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.ContainerProperties;
import com.ibm.tx.jta.embeddable.GlobalTransactionSettings;
import com.ibm.tx.jta.embeddable.LocalTransactionSettings;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.resource.ResourceRefConfigList;

/**
 * Class WCCMMetaData
 */
public abstract class WCCMMetaData
{
    public EJBJar ejbjar;
    public EnterpriseBean enterpriseBean;

    public void initialize(BeanInitData bid)
    {
        // d481127.11 start
        EnterpriseBean enterpriseBean = bid.ivEnterpriseBean;
        this.enterpriseBean = enterpriseBean;
        EJBJar ejbJar = bid.ivModuleInitData.ivEJBJar;
        this.ejbjar = ejbJar;
    }

    public static EnterpriseBean getEnterpriseBeanNamed(EJBJar ejbJar, String name)
    {
        for (EnterpriseBean eb : ejbJar.getEnterpriseBeans())
        {
            if (name.equals(eb.getName()))
            {
                return eb;
            }
        }

        return null;
    }

    /**
     * Clear references to WCCM objects. This method is only necessary for
     * deferred EJBS. This method should be called during module start and
     * after failed deferred init.
     */
    public void unload() // d659020
    {
        // Reloading is not supported in the embeddable container, so we keep
        // references to the other data.  See WASWCCMMetaData.
    }

    /**
     * Update references to WCCM objects. This should should be called before
     * deferred init is attempted.
     */
    public void reload()
                    throws ContainerException
    {
        // Reloading is not supported in the embeddable container.
    }

    /**
     * True if the bean has a module in an EAR with a version.
     */
    // F68113.4
    public boolean hasApplicationVersion()
    {
        return false;
    }

    /**
     * The EAR application version. The result is only valid if {@link #hasApplicationVersion} returns true.
     */
    // F68113.4
    public int getApplicationVersion()
    {
        return -1;
    }

    /**
     * Return the common archive ModuleFile LoadStrategy, or null if unsupported
     * by the runtime environment.
     */
    // F68113.4
    public Object getModuleLoadStrategy()
    {
        return null;
    }

    public Object getExtension()
    {
        return null;
    }

    public boolean isStartEJBAtApplicationStart()
    {
        return false;
    }

    /**
     * Creates the ResRefList for this bean component.
     */
    public abstract ResourceRefConfigList createResRefList();

    /**
     * Creates the LocalTransactionSettings for this bean component.
     */
    public abstract LocalTransactionSettings createLocalTransactionSettings();

    /**
     * Creates the GlobalTransactionSettings for this bean component.
     */
    public abstract GlobalTransactionSettings createGlobalTransactionSettings();

    protected void dump(StringBuilder sb) {}

    /**
     * Dump contents of this object into a String object
     * that is typically used for tracing.
     * 
     * @return dump of this object as a String.
     */
    public String dump() //d481127.11
    {
        StringBuilder sb = new StringBuilder();
        String newLine = ContainerProperties.LineSeparator;
        sb.append(newLine).append("-- WCCMMetaData dump --");
        sb.append(newLine).append("enterpriseBean = ").append(enterpriseBean);
        dump(sb);
        sb.append(newLine).append(", ejbjar = ").append(ejbjar);
        sb.append(newLine).append("-- WCCMMetaData end dump --");
        return sb.toString();
    }
}
