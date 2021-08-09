/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webservices.javaee.common.internal;

import com.ibm.ws.javaee.dd.ws.PortComponent;
import com.ibm.ws.javaee.dd.ws.ServiceImplBean;
import com.ibm.ws.javaee.dd.ws.WebserviceDescription;
import com.ibm.ws.javaee.dd.ws.Webservices;
import com.ibm.wsspi.adaptable.module.Adaptable;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Using the class to help to get the element information in Deployment Descriptor
 */
public class JaxWsDDHelper {

    /**
     * Judge if the webservices.xml file exists.
     * 
     * @param containerToAdapt
     * @return
     * @throws UnableToAdaptException
     */
    static boolean isWebServicesXMLExisting(Adaptable containerToAdapt) throws UnableToAdaptException {
        return containerToAdapt.adapt(Webservices.class) != null;
    }

    /**
     * Get the PortComponent by ejb-link.
     * 
     * @param ejbLink
     * @param containerToAdapt
     * @return
     * @throws UnableToAdaptException
     */
    static PortComponent getPortComponentByEJBLink(String ejbLink, Adaptable containerToAdapt) throws UnableToAdaptException {
        return getHighLevelElementByServiceImplBean(ejbLink, containerToAdapt, PortComponent.class, LinkType.EJB);
    }

    /**
     * Get the PortComponent by servlet-link.
     * 
     * @param servletLink
     * @param containerToAdapt
     * @return
     * @throws UnableToAdaptException
     */
    static PortComponent getPortComponentByServletLink(String servletLink, Adaptable containerToAdapt) throws UnableToAdaptException {
        return getHighLevelElementByServiceImplBean(servletLink, containerToAdapt, PortComponent.class, LinkType.SERVLET);
    }

    /**
     * Get the WebserviceDescription by ejb-link.
     * 
     * @param ejbLink
     * @param containerToAdapt
     * @return
     * @throws UnableToAdaptException
     */
    static WebserviceDescription getWebserviceDescriptionByEJBLink(String ejbLink, Adaptable containerToAdapt) throws UnableToAdaptException {
        return getHighLevelElementByServiceImplBean(ejbLink, containerToAdapt, WebserviceDescription.class, LinkType.EJB);
    }

    /**
     * Get the WebserviceDescription by servlet-link.
     * 
     * @param servletLink
     * @param containerToAdapt
     * @return
     * @throws UnableToAdaptException
     */
    static WebserviceDescription getWebserviceDescriptionByServletLink(String servletLink, Adaptable containerToAdapt) throws UnableToAdaptException {
        return getHighLevelElementByServiceImplBean(servletLink, containerToAdapt, WebserviceDescription.class, LinkType.SERVLET);
    }

    /**
     * For internal usage. Can only process the PortComponent.class and WebserviceDescription.class.
     * 
     * @param portLink
     * @param containerToAdapt
     * @param clazz
     * @return
     * @throws UnableToAdaptException
     */
    @SuppressWarnings("unchecked")
    private static <T> T getHighLevelElementByServiceImplBean(String portLink, Adaptable containerToAdapt, Class<T> clazz, LinkType linkType) throws UnableToAdaptException {
        if (null == portLink) {
            return null;
        }
        if (PortComponent.class.isAssignableFrom(clazz)
            || WebserviceDescription.class.isAssignableFrom(clazz)) {
            Webservices wsXml = containerToAdapt.adapt(Webservices.class);
            if (null == wsXml) {
                return null;
            }
            for (WebserviceDescription wsDes : wsXml.getWebServiceDescriptions()) {
                if (wsDes.getPortComponents().size() == 0) {
                    continue;
                }
                for (PortComponent portCmpt : wsDes.getPortComponents()) {
                    ServiceImplBean servImplBean = portCmpt.getServiceImplBean();

                    String serviceLink = LinkType.SERVLET == linkType ? servImplBean.getServletLink() : servImplBean.getEJBLink();

                    if (serviceLink == null) {
                        continue;
                    } else if (serviceLink.equals(portLink)) {
                        if (PortComponent.class.isAssignableFrom(clazz)) {
                            return (T) portCmpt;
                        } else {
                            return (T) wsDes;
                        }
                    }
                }
            }
            return null;
        }
        return null;
    }

    private enum LinkType {
        EJB, SERVLET
    }
}
