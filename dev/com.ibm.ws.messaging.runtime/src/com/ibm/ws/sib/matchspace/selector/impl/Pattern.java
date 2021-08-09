/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.matchspace.selector.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** A representation of a pattern to be applied to Strings.  This encompasses both
 * SQL style patterns and MQSI-style topic patterns.
 */
public class Pattern {
  /** An object that represents the matchOne directive */
  public static Object matchOne = new Object();
  
  /** The character representing matchOne in standard SQL syntax */
  public static char sqlMatchOne = '_';
  
  /** An object that represents the matchMany directive */
  public static Object matchMany = new Object();
  
  /** The character representing matchMany in standard SQL syntax */
  public static char sqlMatchMany = '%';
  
  /** A class representing a single clause in the pattern.  A clause is a sequence of 
   * matchOne and char[] segments with no two contiguous char[] segments.  The Clause
   * object also records the minimum number of characters it takes to match the clause.
   */
  public static class Clause {
    public Object[] items;
    public int minlen;
    Clause(Object[] items) {
      this.items = items;
      for (int i = 0; i < items.length; i++) {
        Object item = items[i];
        if (item == matchOne)
          minlen++;
        else
          minlen += ((char[]) item).length;
      }
    }
  }
  
  // The pattern's prefix clause, if any. A pattern that starts with a matchMany
  // wildcard has no prefix.
  Clause prefix;
  
  // The pattern's suffix clause, if any.  A pattern that ends with a matchMany wildcard
  // has no suffix.  A pattern with no matchMany wildcards at all has prefix == suffix.
  Clause suffix;
  
  // The middle clauses of the pattern, if any.  Note that only a SQL pattern can have
  // middle clauses.
  Clause[] midClauses;
  
  /** Construct a pattern from a set of parsed tokens.  The tokens consist of char[],
   * matchOne, and matchMany objects with no two consecutive char[] objects.
   * @param tokens an Iterator over the tokens
   */
  Pattern(Iterator tokens) {
    List clause = new ArrayList();
    boolean finalWild = getClause(tokens, clause);
    if (clause.size() > 0)
      prefix = new Clause(clause.toArray());
    if (!finalWild) {
      suffix = prefix;
      return;
    }
    List mid = new ArrayList();
    while (tokens.hasNext()) {
      clause.clear();
      finalWild = getClause(tokens, clause);
      if (finalWild && clause.size() > 0)
        mid.add(new Clause(clause.toArray()));
      // only the last clause can be !finalWild
    }
    // If finalWild, then all clauses found have been added to mid, otherwise, the last
    // one found is still stored only in clause.
    if (!finalWild)
      suffix = new Clause(clause.toArray());
    if (mid.size() > 0)
      midClauses = (Clause[]) mid.toArray(new Clause[0]);
  }
  
  // Get one clause from the tokens iterator.  Returns true if the clause was ended by
  // matchMany, false if it was ended by running out of tokens. 
  private boolean getClause(Iterator tokens, List clause) {
    while(tokens.hasNext()) {
      Object token = tokens.next();
      if (token == matchMany)
        return true;
      clause.add(token);
    }
    return false;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
	public boolean equals(Object o)
	{
		boolean retVal = false;
		if (o instanceof Pattern)
		{
			Pattern other = (Pattern) o;

			if (prefix == other.getPrefix() && suffix == other.getSuffix())
			{
				if (hasMidClauses() && other.hasMidClauses())
				{
					if (midClauses.length == other.midClauses.length)
					{
						// assume for now retVal should be true
						retVal = true;
						for (int i = 0; i < midClauses.length; i++)
						{
							if (midClauses[i] != other.midClauses[i])
							{
								retVal = false;
								// return early
								break;
							}
						}
					}
				} else if (!hasMidClauses() && !other.hasMidClauses())
				{
					retVal = true;
				}
			}
		}
		return retVal;
	}
  
  /** Obtain the Pattern's prefix, or null if it has no prefix
   * @return the prefix clause of the pattern or null
   */
  public Clause getPrefix() {
    return prefix;
  }

  /** Obtain the Pattern's suffix, or null if it has no suffix.  
   * @return the suffix clause of the pattern or null.
   */
  public Clause getSuffix() {
    return suffix;
  }
  
  /** Returns true iff this Pattern has midClauses, false otherwise */
  public boolean hasMidClauses() {
    return midClauses != null;
  }

  /** Match a String against this pattern (used by the Evaluator)
   * @param candidate the String to be matched
   * @return true if the candidate matches, false otherwise
   */
  boolean match(String candidate) {
    char[] chars = candidate.toCharArray();
    int[] cursor = new int[] {0, chars.length};
    if (!checkPrefix(chars, cursor))
      return false;
    if (prefix == suffix && midClauses == null)
      // The pattern consists only of the prefix, which must exhaust the string
      return cursor[0] == cursor[1];
    if (!checkSuffix(chars, cursor))
      return false;
    return matchMiddle(chars, cursor[0], cursor[1]);
  }

  /** Internal method to evaluate a candidate against the prefix */
  boolean checkPrefix(char[] chars, int[] cursor) {
    if (prefix == null)
      return true;
    else
      return matchClause(prefix, chars, cursor);
  }
  
  // Match one clause against a character sequence (used by checkPrefix and matchMiddle)
  private boolean matchClause(Clause clause, char[] chars, int[] cursor) {
    if (clause.minlen > cursor[1] - cursor[0])
      return false;
    for (int i = 0; i < clause.items.length; i++) {
      Object item = clause.items[i];
      if (item == matchOne)
        cursor[0]++;
      else if (!matchForward(chars, (char[]) item, cursor))
        return false;
      // else this phase succeeded so continue
    }
    return true;
  }

  /** Match characters in a forward direction */
  static boolean matchForward(char[] chars, char[] pattern, int[] cursor) {
    if (pattern.length > cursor[1] - cursor[0])
      return false;
    int index = 0;
    while (index < pattern.length)
      if (chars[cursor[0]++] != pattern[index++])
        return false;
    return true;
  }

  /** Internal method to evaluate a candidate against the suffix */
  boolean checkSuffix(char[] chars, int[] cursor) {
    if (suffix == null)
      return true;
    // Compute the only possible starting point. This only works because a clause's minlen
    // is an exact length for SQL-style patterns.  This method is overridden for topic
    // patterns.
    int start = cursor[1] - suffix.minlen;
    if (start < cursor[0])
      // candidate is too short to possibly match
      return false;
    if (!matchClause(suffix, chars, new int[] {start, cursor[1]}))
      return false;
    // The suffix matches, so reflect that in the cursor
    cursor[1] = start;
    return true;
  }

  // Match a sequence of characters against the midClauses of this pattern
  public boolean matchMiddle(char[] chars, int start, int end) {
    if (midClauses == null)
      return true;
    int[] cursor = new int[] {start, end};
    for (int i = 0; i < midClauses.length; i++) {
      Clause clause = midClauses[i];
      if (!find(clause, chars, cursor))
        return false;
    }
    return true;
  }
  
  // Find a particular clause in a candidate character array and advance the cursor to 
  // the end of the found portion.
  private boolean find(Clause clause, char[] chars, int[] cursor) {
    int start = cursor[0];
    int end = cursor[1];
    while (end - start >= clause.minlen)
      if (matchClause(clause, chars, cursor))
        return true;
      else
        cursor[0] = ++start;
    return false;
  }

  /** Parse a string containing a SQL-style pattern
   * @param pattern the string containing the pattern
   * @param escaped true if the string employs an escape character
   * @param escape the escape character (ignored if !escaped)
   * @return an Object representing the result of the parse, as follows.  
   * <ul>
   * <li>null if the pattern is syntactically invalid
   * <li>the pattern as a string if the pattern contains no unescaped special characters
   *   and hence is better treated as an equality test.  Any escape chararacters will
   *   have been removed.
   * <li>the matchMany object if the pattern will match any string at all.  This is 
   *   better handled as a "NOT NULL" test.
   * <li>the pattern as a Pattern object otherwise.
   * </ul>
   */
  public static Object parsePattern(String pattern, boolean escaped, char escape) {
    char[] accum = new char[pattern.length()];
    int finger = 0;
    List tokens = new ArrayList();
    boolean trivial = true;
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c == sqlMatchOne) {
        finger = flush(accum, finger, tokens);
        tokens.add(matchOne);
        trivial = false;
      }
      else if (c == sqlMatchMany) {
        finger = flush(accum, finger, tokens);
        tokens.add(matchMany);
        trivial = false;
      }
      else if (escaped && c == escape)
        if (i == pattern.length()-1)
          return null;
        else {
          i++;
          accum[finger++] = pattern.charAt(i);
        }
      else
        accum[finger++] = c;
    }
    if (trivial)
      return new String(accum, 0, finger);
    flush(accum, finger, tokens);
    if (tokens.size() == 1 && tokens.get(0) == matchMany)
      return matchMany;
    return new Pattern(tokens.iterator());
  }

  /** Parsing subroutine to flush the accumulated characters to the tokens list as a char[]
   * 
   * @param accum the char[] being used to accumulate characters
   * @param finger the next position in the buffer (used as a length here)
   * @param tokens the List of tokens being accumulated (mutated by this method)
   * @return the new value of finger, which is always 0
   */
  static int flush(char[] accum, int finger, List tokens) {
    if (finger > 0) {
      char[] token = new char[finger];
      System.arraycopy(accum,0, token,0, finger);
      tokens.add(token);
    }
    return 0;
  }
}
