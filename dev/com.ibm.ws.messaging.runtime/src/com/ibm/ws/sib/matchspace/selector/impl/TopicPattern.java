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

import com.ibm.ws.sib.matchspace.MatchSpace;

/**  A specialization of Pattern for use as an MQSI-style topic pattern
 */
public class TopicPattern extends Pattern {
  
  /** Construct a new TopicPattern from a set of tokens
   * @param tokens an iterator over the tokens
   */
  TopicPattern(Iterator tokens) {
    super(tokens);
  }
  
  // Override checkPrefix to implement topic semantics
  boolean checkPrefix(char[] chars, int[] cursor) {
    if (chars.length > cursor[0] && chars[cursor[0]] == MatchSpace.NONWILD_MARKER)
      return false;
    if (prefix == null)
      return true;
    if (cursor[1] - cursor[0] < prefix.minlen)
      // a conservative test, but if we can't pass it, we'll certainly fail later
      return false;
    for (int i = 0; i < prefix.items.length; i++) {
      Object item = prefix.items[i];
      if (item == Pattern.matchOne)
        if (!topicSkipForward(chars, cursor))
          return false;
        else
          ; // topic skip succeeded so continue
      else if (!matchForward(chars, (char[]) item, cursor))
        return false;
      // else this phase succeeded so continue
    }
    // The prefix matches exactly.  But, unless we are at the end of the string,
    // the next character MUST be a separator.
    return cursor[0] == cursor[1] || 
      chars[cursor[0]] == MatchSpace.SUBTOPIC_SEPARATOR_CHAR;
  }

  /** Skip forward to the next separator character
   * @param chars the characters to be examined
   * @param cursor the int[2] { start, end } "cursor" describing the area to be examined
   * @return true if something was skipped, false if nothing could be skipped
   */
  static boolean topicSkipForward(char[] chars, int[] cursor) {
    if (cursor[0] == cursor[1])
      return false;
    while (cursor[0] < cursor[1] && chars[cursor[0]] != MatchSpace.SUBTOPIC_SEPARATOR_CHAR)
      cursor[0]++;
    return true;      
  }

  // Override checkSuffix to implement topic semantics
  boolean checkSuffix(char[] chars, int[] cursor) {
    if (suffix == null)
      return true;
    if (cursor[1] - cursor[0] < suffix.minlen)
      // a conservative test, but if we can't pass it, we'll certainly fail later
      return false;
    int last = suffix.items.length - 1;
    for (int i = last; i >= 0; i--) {
      Object item = suffix.items[i];
      if (item == Pattern.matchOne)
        if (!topicSkipBackward(chars, cursor))
          return false;
        else
          ; // topic skip succeeded so continue
      else if (!matchBackward(chars, (char[]) item, cursor))
        return false;
      // else this phase succeeded so continue
    }
    // The suffix matches exactly.  But, the preceding character MUST be a separator.  We
    // don't allow start-of-string because we would not have checked the suffix
    // at all if it were allowed to extend to the beginning (it would then be identical
    // to the prefix).
    return cursor[0] < cursor[1] && 
      chars[cursor[1]-1] == MatchSpace.SUBTOPIC_SEPARATOR_CHAR;
  }

  /** Skip backward to the next separator character
   * @param chars the characters to be examined
   * @param cursor the int[2] { start, end } "cursor" describing the area to be examined
   * @return true if something was skipped, false if nothing could be skipped
   */
  static boolean topicSkipBackward(char[] chars, int[] cursor) {
    if (cursor[0] == cursor[1])
      return false;
    while (cursor[0] < cursor[1] && 
        chars[cursor[1]-1] != MatchSpace.SUBTOPIC_SEPARATOR_CHAR)
      cursor[1]--;
    return true;      
  }

  /** Match characters in a backward direction */
  static boolean matchBackward(char[] chars, char[] pattern, int[] cursor) {
    int start = cursor[1] - pattern.length;
    if (start < cursor[0])
      // candidate is too short to possibly match
      return false;
    if (!matchForward(chars, pattern, new int[] {start, cursor[1]}))
      return false;
    // The candidate matches, so reflect that in the cursor
    cursor[1] = start;
    return true;
  }

  /** Parse a string topic pattern into a TopicPattern object
   * @param pattern the string pattern to be parsed
   * @return an object representing the result, as follows.  
   * <ul>
   * <li>the pattern as a string if the pattern contains no wildcards
   *   and hence is better treated as an equality test.
   * <li>the pattern as a TopicPattern object otherwise.
   * </ul>
   * <p>Unlike Pattern.parsePattern, this parser does not detect syntax errors but
   * does something "logical" with every pattern it is given.  Topic strings and topic
   * patterns should be validated by the Matching.validateTopic method first.
   * <p>Unlike Pattern.parsePattern, this parser will not turn a single matchMany
   * wildcard into a NOT NULL test.  Such a transformation is invalid with topic patterns
   * because a special test for the matchNothing character is always required.
   */
  public static Object parsePattern(String pattern) {
    // Unfortunately, this method shares a fair amount of logic with Topic.parsePattern
    // but it is hard to figure out how to factor them.
    char[] accum = new char[pattern.length()];
    int finger = 0;
    List tokens = new ArrayList();
    boolean trivial = true;
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c == '*') 
      {
        finger = flush(accum, finger, tokens);
        tokens.add(matchOne);
        trivial = false;
      }      
      else if (c == MatchSpace.SUBTOPIC_SEPARATOR_CHAR) 
      {
        // It's a normal character unless followed by another separator
        // or a "."
        if(i == pattern.length() - 1)
        {
          accum[finger++] = c;
        }
        else
        { 
          // Check to see if we have a double slash
          if(pattern.charAt(i+1) == MatchSpace.SUBTOPIC_SEPARATOR_CHAR)
          {
            // Double slash
            finger = flush(accum, finger, tokens);
            tokens.add(matchMany);
            trivial = false;           
            // Skip the second slash
            i++;
            
            // If the next char is a '.', then we skip that too
            if(pattern.charAt(i+1) == '.')
            {
              // Skip the dot
              i++;
              
              // Defect 307231, handle the allowable "//./" case
              if( (i+1) < pattern.length() &&
                 pattern.charAt(i+1) == MatchSpace.SUBTOPIC_SEPARATOR_CHAR)
              {
                // Skip a subsequent slash
                i++;
              }
            }
          }
          // Check for special "/." case which we jump over. Note that topic syntax
          // checking will already have disallowed invalid expressions such as "a/.b"
          // so if a "." follows a slash then it must be the final character, or it
          // must be followed by a separator.
          else if(pattern.charAt(i+1) == '.')
          {
            // skip the slash and the dot
            i++;
          }
          else
          {
            accum[finger++] = c;
          }
        }
      }
      else
        accum[finger++] = c;
    }
    if (trivial)
      return new String(accum, 0, finger);
    flush(accum, finger, tokens);
    return new TopicPattern(tokens.iterator());
  }
  
  public static Object parseMQSIPattern(String pattern) {
    // Unfortunately, this method shares a fair amount of logic with Topic.parsePattern
    // but it is hard to figure out how to factor them.
    char[] accum = new char[pattern.length()];
    int finger = 0;
    List tokens = new ArrayList();
    boolean trivial = true;
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c == MatchSpace.SUBTOPIC_MQSI_MATCHONE_CHAR) {
        finger = flush(accum, finger, tokens);
        tokens.add(matchOne);
        trivial = false;
      }
      else if (c == MatchSpace.SUBTOPIC_MQSI_MATCHMANY_CHAR) {
        finger = flush(accum, finger, tokens);
        tokens.add(matchMany);
        trivial = false;
        // skip the next character, if any: it is supposed to a separator
        if (i < pattern.length()-1)
          i++;
      }
      else if (c == MatchSpace.SUBTOPIC_SEPARATOR_CHAR) {
        // It's a normal character unless followed by a matchMany
        if (i == pattern.length() - 1 || 
            pattern.charAt(i+1) != MatchSpace.SUBTOPIC_MQSI_MATCHMANY_CHAR)
          accum[finger++] = c;
      }
      else
        accum[finger++] = c;
    }
    if (trivial)
      return new String(accum, 0, finger);
    flush(accum, finger, tokens);
    return new TopicPattern(tokens.iterator());
  }  
  
}
