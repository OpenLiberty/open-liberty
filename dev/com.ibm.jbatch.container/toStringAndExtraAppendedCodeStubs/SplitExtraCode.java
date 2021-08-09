/**
 * Copyright 2013 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Appended by build tooling.
 */

public List<com.ibm.jbatch.container.jsl.TransitionElement> getTransitionElements() {
    return new ArrayList<com.ibm.jbatch.container.jsl.TransitionElement>();
}
    
/*
 * Appended by build tooling.
 */
public String toString() {
	StringBuilder buf = new StringBuilder(100);
	buf.append("Split: id=" + id);
	buf.append("\nContained Flows: \n");
	if (flows == null) {
		buf.append("<none>");
	} else {
		int i = 0;
		for ( Flow f : flows) {
			buf.append("flow[" + i + "]:" + f + "\n");
			i++;
		}
	}
	buf.append("\nnextFromAttribute =" + nextFromAttribute);
	return buf.toString();
}
