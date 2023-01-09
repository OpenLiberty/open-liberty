/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
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
package ejbtestnobnd.ejb;

public interface EJBBndLocal {
    Class<?> getEJBClass();

    void verifyEnvEntryBinding();
    void verifyResourceBinding() throws Exception;
    void verifyDataSourceBinding() throws Exception;
    void verifyResourceIsolationBindingMerge() throws Exception;
    void verifyEJBRef();
    void verifyInterceptor();
}
