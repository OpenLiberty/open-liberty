/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.v20.cdi.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "api.classes=" +
                        "javax.validation.Validator;" +
                        "javax.validation.ValidatorFactory;" +
                        "org.hibernate.validator.cdi.HibernateValidator;" +
                        "org.hibernate.validator.cdi.ValidationExtension;" +
                        "org.hibernate.validator.cdi.internal.interceptor.ValidationInterceptor;" +
                        "org.hibernate.validator.internal.engine.ValidatorImpl",
                        "service.vendor=IBM"
           })
public class LibertyHibernateValidatorExtension implements WebSphereCDIExtension {}
