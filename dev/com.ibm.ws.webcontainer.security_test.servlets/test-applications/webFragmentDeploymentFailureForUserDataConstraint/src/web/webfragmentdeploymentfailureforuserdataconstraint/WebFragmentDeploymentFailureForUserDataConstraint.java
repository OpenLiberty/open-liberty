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

package web.webfragmentdeploymentfailureforuserdataconstraint;

import web.common.BaseServlet;

//2 web-fragment.xml that points to same web resource name, but has user-data-constraint conflicts so app deploy fails
public class WebFragmentDeploymentFailureForUserDataConstraint extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public WebFragmentDeploymentFailureForUserDataConstraint() {
        super("WebFragmentDeploymentFailureForUserDataConstraint");
    }

}
