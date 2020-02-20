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
package mpGraphQL10.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.GraphQLApi;

@GraphQLApi
public class MyGraphQLEndpoint {
    private static Logger LOG = Logger.getLogger(MyGraphQLEndpoint.class.getName());

    private final Widget countWidget = new Widget("Count", 0, 0);
    private final Widget timeWidget = new Widget("Time", -1, 0);


    @Query("getCountWidget")
    public List<Widget> getCountWidget() {
        countWidget.setCount(countWidget.getCount() + 1);
        LOG.info("getCountWidget " + countWidget.getCount());
        return Collections.singletonList(countWidget);
    }

    @Query("getTimeWidget")
    public List<Widget> getTimeWidget() {
        long startTime = System.nanoTime();
        try {
            Thread.sleep(600); // really should only sleep 500 ms, but some JVMs end up sleeping under
                               // that (or nanoTime() reports it is under)
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
        long elapsed = System.nanoTime() - startTime;
        timeWidget.setTime(timeWidget.getTime() + elapsed);
        LOG.info("getTimeWidget " + timeWidget.getTime());
        return Collections.singletonList(timeWidget);
    }
}
