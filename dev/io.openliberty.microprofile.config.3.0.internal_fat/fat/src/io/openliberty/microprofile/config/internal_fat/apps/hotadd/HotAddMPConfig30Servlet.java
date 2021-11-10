/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.microprofile.config.internal_fat.apps.hotadd;

import org.eclipse.microprofile.config.Config;

import componenttest.app.FATServlet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/")
@ApplicationScoped
public class HotAddMPConfig30Servlet extends FATServlet {

    @Inject
    Config config;

    public String configTest() {
        return config.toString();
    }

}
