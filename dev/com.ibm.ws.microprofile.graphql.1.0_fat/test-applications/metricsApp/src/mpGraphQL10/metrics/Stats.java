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

import javax.json.bind.annotation.JsonbProperty;

public class Stats {

    public static class SimpleTimerStat {
        private int count;
        private long elapsedTime;

        public int getCount() {
            return count;
        }
        public void setCount(int count) {
            this.count = count;
        }
        public long getElapsedTime() {
            return elapsedTime;
        }
        public void setElapsedTime(long elapsedTime) {
            this.elapsedTime = elapsedTime;
        }
    }

    @JsonbProperty("mp_graphql_Query_getCountWidget")
    private SimpleTimerStat count;
    @JsonbProperty("mp_graphql_Query_getTimeWidget")
    private SimpleTimerStat time;

    public SimpleTimerStat getCount() {
        return count;
    }
    public void setCount(SimpleTimerStat count) {
        this.count = count;
    }
    public SimpleTimerStat getTime() {
        return time;
    }
    public void setTime(SimpleTimerStat time) {
        this.time = time;
    }

}
