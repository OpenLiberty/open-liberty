/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.identitystores.ldap;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@Named
@ApplicationScoped
public class LdapIDStorePropsBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public String getHostnameUrl() {
        return getUrl("localhost");
    }

    public String getIpUrl() {
        return getUrl("127.0.0.1");
    }

    public String getUrl(String hostname) {
        String url = "ldap://" + hostname + ":" + System.getProperty("ldap.1.port");
        System.out.println("ldap Url : " + url);
        return url;
    }

    @PreDestroy
    public void destroy() {}

    @PostConstruct
    public void create() {}
}
