/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.install.internal;

public class MavenRepository {
    private String name;
    private String repositoryUrl;
    private String userId;
    private String password;

    public MavenRepository(String name, String repositoryUrl, String userId, String password) {
        this.name = name;
        this.repositoryUrl = repositoryUrl;
        this.userId = userId;
        this.password = password;
    }

    public String getName(){
        return name;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String toString(){
        return this.repositoryUrl;
    }


}
