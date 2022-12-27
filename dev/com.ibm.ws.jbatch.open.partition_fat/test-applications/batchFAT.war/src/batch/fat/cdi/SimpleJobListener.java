/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.cdi;

import javax.batch.api.listener.JobListener;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@Dependent
public class SimpleJobListener implements JobListener {

    @Inject
    private SimpleJobLogger logger;

    @Override
    public void beforeJob() throws Exception {
        logger.log("before job");
    }

    @Override
    public void afterJob() throws Exception {
        logger.log("after job");
    }

}
