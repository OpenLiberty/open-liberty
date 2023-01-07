/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.

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
package concurrent.mp.fat.cdi.web;

import java.io.Serializable;

import javax.enterprise.context.ConversationScoped;

@ConversationScoped
public class ConversationScopeBean extends AbstractBean implements Serializable {

    private static final long serialVersionUID = 8175924512735191234L;

}
