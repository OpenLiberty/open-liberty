/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.javaeesec.cdi.extensions;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.security.javaeesec.cdi.extensions.CommonJavaEESecCDIExtension;

/**
 * TODO: Add all JSR-375 API classes that can be bean types to api.classes.
 *
 * @param <T>
 */
@Component(service = { WebSphereCDIExtension.class },
           property = { "api.classes=javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;javax.security.enterprise.identitystore.IdentityStore;javax.security.enterprise.identitystore.IdentityStoreHandler;javax.security.enterprise.identitystore.RememberMeIdentityStore;javax.security.enterprise.SecurityContext;com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider",
                        "bean.defining.annotations=javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;javax.security.enterprise.authentication.mechanism.http.LoginToContinue;javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;javax.security.enterprise.identitystore.LdapIdentityStoreDefinition" },
           immediate = true)
public class JavaEESecCDIExtension<T> extends CommonJavaEESecCDIExtension<T> implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(JavaEESecCDIExtension.class);

}
