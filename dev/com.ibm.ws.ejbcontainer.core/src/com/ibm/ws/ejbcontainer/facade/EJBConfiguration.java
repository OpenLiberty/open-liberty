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
package com.ibm.ws.ejbcontainer.facade;

import java.security.AccessController;
import java.util.Arrays;

import com.ibm.ejs.util.Util;
import com.ibm.ejs.util.dopriv.SystemGetPropertyPrivileged;

/**
 * Provides the EJB configuration required to support an EJB facade, exposing
 * another component model as an EJB. <p>
 *
 * This implementation may be used directly, or subclassed to provide additional
 * metadata that may be useful by the corresponding EJBClassFactory implementation.
 * The EJBClassFactory implementation may be a subclass of this class. <p>
 **/
public class EJBConfiguration
{
    protected String ivEJBName;
    protected String ivEJBImplName;
    protected String[] ivRemoteInterfaceNames;
    protected String[] ivRemoteJndiNames;
    protected String[] ivLocalInterfaceNames;
    protected String[] ivLocalJndiNames;
    protected String ivRemoteHomeInterfaceName;
    protected String ivRemoteHomeJndiName;
    protected String ivLocalHomeInterfaceName;
    protected String ivLocalHomeJndiName;
    protected EJBClassFactory ivEJBFactory;

    /**
     * Default constructor for use with set methods. <p>
     **/
    public EJBConfiguration()
    {
        // Intentionally blank; for use with set methods.
    }

    /**
     * Constructor that allows setting of all of the configuration attributes
     * at once, rather than using the set methods. <p>
     **/
    public EJBConfiguration(String ejbName,
                            String ejbImplName,
                            String[] remoteInterfaceNames,
                            String[] remoteJndiNames,
                            String[] localInterfaceNames,
                            String[] localJndiNames,
                            String remoteHomeInterfaceName,
                            String remoteHomeJndiName,
                            String localHomeInterfaceName,
                            String localHomeJndiName,
                            ClassLoader classLoader,
                            EJBClassFactory ejbFactory)
    {
        ivEJBName = ejbName;
        ivEJBImplName = ejbImplName;
        ivRemoteInterfaceNames = remoteInterfaceNames;
        ivRemoteJndiNames = remoteJndiNames;
        ivLocalInterfaceNames = localInterfaceNames;
        ivLocalJndiNames = localJndiNames;
        ivRemoteHomeInterfaceName = remoteHomeInterfaceName;
        ivRemoteHomeJndiName = remoteHomeJndiName;
        ivLocalHomeInterfaceName = localHomeInterfaceName;
        ivLocalHomeJndiName = localHomeJndiName;
        ivEJBFactory = ejbFactory;
    }

    @Override
    public String toString() // F743-36290.1
    {
        String nl = AccessController.doPrivileged(new SystemGetPropertyPrivileged("line.separator", "\n")) +
                    "                                 ";

        StringBuilder sb = new StringBuilder();
        sb.append(nl).append(super.toString());
        sb.append(nl).append("EJB Name              = ").append(ivEJBName);
        sb.append(nl).append("EJB Class             = ").append(ivEJBImplName);
        sb.append(nl).append("Remote Interfaces     = ").append(Arrays.toString(ivRemoteInterfaceNames));
        sb.append(nl).append("Remote JNDI Names     = ").append(Arrays.toString(ivRemoteJndiNames));
        sb.append(nl).append("Local Interfaces      = ").append(Arrays.toString(ivLocalInterfaceNames));
        sb.append(nl).append("Local JNDI Names      = ").append(Arrays.toString(ivLocalJndiNames));
        sb.append(nl).append("Remote Home Interface = ").append(ivRemoteHomeInterfaceName);
        sb.append(nl).append("Remote Home JNDI      = ").append(ivRemoteHomeJndiName);
        sb.append(nl).append("Local Home Interface  = ").append(ivLocalHomeInterfaceName);
        sb.append(nl).append("Local Home JNDI       = ").append(ivLocalHomeJndiName);
        sb.append(nl).append("EJBClassFactory       = ").append(Util.identity(ivEJBFactory));

        return sb.toString();
    }

    /**
     * Returns the name of the EJB.
     **/
    public String getEJBName()
    {
        return ivEJBName;
    }

    /**
     * Sets the name of the EJB.
     **/
    public void setEJBName(String ejbName)
    {
        ivEJBName = ejbName;
    }

    /**
     * Returns the EJB implementation class name.
     **/
    public String getEJBImplementationName()
    {
        return ivEJBImplName;
    }

    /**
     * Sets the EJB implementation class name.
     **/
    public void setEJBImplementationName(String ejbImplName)
    {
        ivEJBImplName = ejbImplName;
    }

    /**
     * Returns the EJB remote business interface names.
     **/
    public String[] getRemoteInterfaceNames()
    {
        return ivRemoteInterfaceNames;
    }

    /**
     * Sets the EJB remote business interface names.
     **/
    public void setRemoteInterfaceNames(String[] remoteInterfaceNames)
    {
        ivRemoteInterfaceNames = remoteInterfaceNames;
    }

    /**
     * Returns the JNDI names corresponding to the EJB remote business
     * interfaces. Each EJB remote business interface will be bound
     * into the global name space with the corresponding name in the
     * returned list.
     **/
    public String[] getRemoteJndiNames()
    {
        return ivRemoteJndiNames;
    }

    /**
     * Sets the JNDI names corresponding to the EJB remote business
     * interfaces. Each EJB remote business interface will be bound
     * into the global name space with the corresponding name in the
     * specified list.
     **/
    public void setRemoteJndiNames(String[] remoteJndiNames)
    {
        ivRemoteJndiNames = remoteJndiNames;
    }

    /**
     * Returns the EJB local business interface names.
     **/
    public String[] getLocalInterfaceNames()
    {
        return ivLocalInterfaceNames;
    }

    /**
     * Sets the EJB local business interface names.
     **/
    public void setLocalInterfaceNames(String[] localInterfaceNames)
    {
        ivLocalInterfaceNames = localInterfaceNames;
    }

    /**
     * Returns the JNDI names corresponding to the EJB local business
     * interfaces. Each EJB local business interface will be bound
     * into the ejblocal: name space with the corresponding name in the
     * returned list.
     **/
    public String[] getLocalJndiNames()
    {
        return ivLocalJndiNames;
    }

    /**
     * Sets the JNDI names corresponding to the EJB local business
     * interfaces. Each EJB local business interface will be bound
     * into the ejblocal: name space with the corresponding name in the
     * specified list.
     **/
    public void setLocalJndiNames(String[] localJndiNames)
    {
        ivLocalJndiNames = localJndiNames;
    }

    /**
     * Returns the EJB remote component home interface. The remote
     * component interface will be defined by the return type of the
     * home create method.
     **/
    public String getRemoteHomeInterfaceName()
    {
        return ivRemoteHomeInterfaceName;
    }

    /**
     * Sets the EJB remote component home interface. The remote
     * component interface will be defined by the return type of the
     * home create method.
     **/
    public void setRemoteHomeInterfaceName(String remoteHomeInterfaceName)
    {
        ivRemoteHomeInterfaceName = remoteHomeInterfaceName;
    }

    /**
     * Returns the JNDI name of the EJB remote component home interface.
     * The EJB remote component home interface will be bound into the
     * global name space with the returned value.
     **/
    public String getRemoteHomeJndiName()
    {
        return ivRemoteHomeJndiName;
    }

    /**
     * Sets the JNDI name of the EJB remote component home interface.
     * The EJB remote component home interface will be bound into the
     * global name space with the specified value.
     **/
    public void setRemoteHomeJndiName(String remoteHomeJndiName)
    {
        ivRemoteHomeJndiName = remoteHomeJndiName;
    }

    /**
     * Returns the EJB local component home interface. The local
     * component interface will be defined by the return type of the
     * home create method.
     **/
    public String getLocalHomeInterfaceName()
    {
        return ivLocalHomeInterfaceName;
    }

    /**
     * Sets the EJB local component home interface. The local
     * component interface will be defined by the return type of the
     * home create method.
     **/
    public void setLocalHomeInterfaceName(String localHomeInterfaceName)
    {
        ivLocalHomeInterfaceName = localHomeInterfaceName;
    }

    /**
     * Returns the JNDI name of the EJB local component home interface.
     * The EJB local component home interface will be bound into the
     * global name space with the returned value.
     **/
    public String getLocalHomeJndiName()
    {
        return ivLocalHomeJndiName;
    }

    /**
     * Sets the JNDI name of the EJB local component home interface.
     * The EJB local component home interface will be bound into the
     * global name space with the specified value.
     **/
    public void setLocalHomeJndiName(String localHomeJndiName)
    {
        ivLocalHomeJndiName = localHomeJndiName;
    }

    /**
     * Returns the EJB Class factory, which will be used to dynamically load
     * the EJB interfaces and implementation classes. <p>
     **/
    public EJBClassFactory getEJBClassFactory()
    {
        return ivEJBFactory;
    }

    /**
     * Set the EJB Class factory, which will be used to dynamically load
     * the EJB interfaces and implementation classes. <p>
     **/
    public void setEJBClassFactory(EJBClassFactory ejbFactory)
    {
        ivEJBFactory = ejbFactory;
    }
}
