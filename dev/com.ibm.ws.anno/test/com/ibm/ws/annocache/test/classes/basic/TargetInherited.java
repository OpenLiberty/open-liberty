/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.test.classes.basic;

import java.util.List;
import java.util.Map;

import javax.persistence.Id;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlList;

import org.junit.Test;

@AnnoInherited(value = { "a", "b" })
public class TargetInherited {
    @Id
    public String public1 = "Super1";
    public String public2 = "Super2";

    protected String protected1 = "Super1";
    protected String protected2 = "Super2";

    String package1 = "Super1";
    String package2 = "Super2";

    @SuppressWarnings("unused")
	private final String private1 = "Super1";

    //

    @Test(timeout = 10000)
    public void publicMethod() {
        // EMPTY
    }

    public Number publicMethod(int n) {
        return null;
    }

    protected Map<String, String> protectedMethod() {
        return null;
    }

    int packageMethod() {
        return 0;
    }

    @SuppressWarnings("unused")
	private void privateMethod() {
        // EMPTY
    }

    //

    protected void annoMethod(int a, String b, @XmlList List<?> c, @XmlAttachmentRef long d) {
        // EMPTY
    }
}
