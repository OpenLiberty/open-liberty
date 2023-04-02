/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.web.factories;

import java.util.EventListener;

import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.module.web.el.WeldELContextListener;
import org.jboss.weld.module.web.el.WeldExpressionFactory;
import org.jboss.weld.module.web.servlet.ConversationFilter;
import org.jboss.weld.module.web.servlet.WeldTerminalListener;

import jakarta.el.ELContextListener;
import jakarta.el.ExpressionFactory;
import jakarta.servlet.Filter;

/**
 * Provide version specific instances of Weld classes
 */
public class WeldListenerFactory {

    public static EventListener newWeldTerminalListener(BeanManagerImpl beanManagerImpl) {
        return new WeldTerminalListener(beanManagerImpl);
    }

    public static EventListener newWeldInitialListener(BeanManagerImpl beanManagerImpl) {
        return new Weld50InitialListener(beanManagerImpl);
    }

    public static ExpressionFactory newWeldExpressionFactory(ExpressionFactory expressionFactory) {
        return new WeldExpressionFactory(expressionFactory);
    }

    public static ELContextListener newWeldELContextListener() {
        return new WeldELContextListener();
    }

    public static Class<? extends Filter> getConversationFilter() {
        return ConversationFilter.class;
    }

}
