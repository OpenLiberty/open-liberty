/*******************************************************************************
 * Copyright (c) 1997, 2022 IBM Corporation and others.
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
package com.ibm.ws.jsp.configuration;


public class StaticIncludeJspConfiguration extends JspConfiguration {
    public StaticIncludeJspConfiguration(JspConfiguration parentConfig) {
        super(parentConfig.getConfigManager(), parentConfig.getServletVersion(), parentConfig.getJspVersion(), parentConfig.isXml(), parentConfig.isXmlSpecified(), parentConfig.elIgnored(), parentConfig.errorOnELNotFound(), parentConfig.scriptingInvalid(), parentConfig.isTrimDirectiveWhitespaces(), parentConfig.isDeferredSyntaxAllowedAsLiteral(), parentConfig.getTrimDirectiveWhitespaces(), parentConfig.getDeferredSyntaxAllowedAsLiteral(), parentConfig.elIgnoredSetTrueInPropGrp(), parentConfig.elIgnoredSetTrueInPage(), parentConfig.errorOnELNotFoundSetTrueInPropGrp(), parentConfig.errorOnELNotFoundSetTrueInPage(), parentConfig.getDefaultContentType(), parentConfig.getBuffer(), parentConfig.isErrorOnUndeclaredNamespace()); 
    }
}
