/*
 * Copyright 2012 International Business Machines Corp.
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
package com.ibm.jbatch.container.jsl.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.jbatch.container.jsl.impl.GlobPatternMatcherImpl;

public class PatternMatchingTest {

	private boolean matchSpecifiedExitStatus(String toMatch, String pattern) {
		GlobPatternMatcherImpl matcher = new GlobPatternMatcherImpl();
		return matcher.matchWithoutBackslashEscape(toMatch, pattern);
	}

	public PatternMatchingTest() {
		super();
	}

	@Test
	public void testBasic() {            
		assertTrue(matchSpecifiedExitStatus("AA", "AA"));
	}

	@Test
	public void testQuestionMark() {            
		assertTrue(matchSpecifiedExitStatus("AA", "A?"));
	}

	@Test
	public void testQuestionMarkRegEx() {            
		assertFalse(matchSpecifiedExitStatus("AA", "A.?"));
	}

	@Test
	public void testAsterisk() {            
		assertTrue(matchSpecifiedExitStatus("ABCDEFG", "A*"));
	}

	@Test
	public void testAsteriskRegEx() {            
		assertFalse(matchSpecifiedExitStatus("ABCDEFG", "A.*"));
	}
	
	@Test
	public void testMultiple1() {            
		assertFalse(matchSpecifiedExitStatus("AAABCABC", "A*C*D*"));
		assertTrue(matchSpecifiedExitStatus("AAABCABDC", "A*C*D*"));
		assertTrue(matchSpecifiedExitStatus("AAABCABDC", "A*C??*D*"));
		assertTrue(matchSpecifiedExitStatus("AAABCABDC", "A*C??*D?"));
		assertTrue(matchSpecifiedExitStatus("AAABCABDC", "*BDC"));
		assertTrue(matchSpecifiedExitStatus("BDC", "*BDC"));
		assertTrue(matchSpecifiedExitStatus("BDC", "BDC*"));
		assertTrue(matchSpecifiedExitStatus("BDC", "*B*D*C*"));
		assertTrue(matchSpecifiedExitStatus("BBC", "*B*B*C*"));
		assertTrue(matchSpecifiedExitStatus("Bxzvc7689asafBC", "*B*B*C*"));
		assertTrue(matchSpecifiedExitStatus("AAABCABC", "AA*A*"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "A*A*"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "A**A*"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "A***A*"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "A?*A*"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "A??A*"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "A??A*"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "*A??A*"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "*AB*"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "*AB**"));
		assertTrue(matchSpecifiedExitStatus("ABCCBAABCCBA", "*A*A*A"));
		assertFalse(matchSpecifiedExitStatus("ABCCBAABCCBA", "*A*D*A"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "*AB*C"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "*AB*C*"));
		// Not sure if this will work
		assertTrue(matchSpecifiedExitStatus("A*B", "A*B"));
		assertFalse(matchSpecifiedExitStatus("ZABCCBAABCCBA", "*A*D*"));
		assertTrue(matchSpecifiedExitStatus("ZABCCBAABCCBA", "Z*C*A*BA"));
		assertFalse(matchSpecifiedExitStatus("ABCABC", "*AB?C*"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "A??*C"));
		assertFalse(matchSpecifiedExitStatus("ABCABC", "A?CB?*C"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "A?CA?*C"));
		assertTrue(matchSpecifiedExitStatus("ABCABC", "A?CA*C"));
		assertFalse(matchSpecifiedExitStatus("ABCCBAABCCBA", "*A*D*A"));
		assertFalse(matchSpecifiedExitStatus("A", "AD*A"));
		assertFalse(matchSpecifiedExitStatus("AD", "AD*A"));
		assertFalse(matchSpecifiedExitStatus("AD", "AD*AD"));
		assertFalse(matchSpecifiedExitStatus("ADADA", "AD*AD"));
		assertFalse(matchSpecifiedExitStatus("AAA", "A*D*A*D"));
		assertFalse(matchSpecifiedExitStatus("AAAD", "A*D*A*D"));
	}
	
	// Basically I just have this here since it's easy to copy-paste a failing
	// assertion to run only the single JUnit test.
	@Test
	public void testMultiple2() {            	

	}


}
