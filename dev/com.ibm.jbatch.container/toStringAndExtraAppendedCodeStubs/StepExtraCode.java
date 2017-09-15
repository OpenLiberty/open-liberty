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
	public String toString() {
	    StringBuilder buf = new StringBuilder(100);
	    buf.append("Step: id=" + id);
	    buf.append(", startLimit=" + startLimit);
	    buf.append(", allowStartIfComplete=" + allowStartIfComplete);
	    buf.append("\nnextFromAttribute =" + nextFromAttribute);
		buf.append("\nTransition elements: \n");
		if (transitionElements == null) {
			buf.append("<none>");
		} else {
			int j = 0;
			for ( com.ibm.jbatch.container.jsl.TransitionElement e : transitionElements) {
				buf.append("transition element[" + j + "]:" + e + "\n");
				j++;
			}
		}
	    buf.append("\nProperties = " + com.ibm.jbatch.jsl.util.PropertiesToStringHelper.getString(properties));
	    buf.append("\n");
	    if (batchlet != null) {
	    	buf.append("Contains batchlet=" + batchlet);
	    }
	    if (chunk != null) {
	    	buf.append("Contains chunk=" + chunk);
	    }
	    return buf.toString();
    }
