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

package com.ibm.ws.sib.api.jms.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.text.ParseException;

import javax.jms.Destination;
import javax.jms.JMSException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsDestination;
import com.ibm.websphere.sib.api.jms.JmsTopic;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.utils.TopicWildcardTranslation;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The utility methods in this class are called from within JmsSessionImpl when dealing
 * with calls to create Topic and Queue objects.
 *
 * This class uses a lot of String.matches and String.replaceAll calls. These should
 * probably be replaced by explicit calls to the Pattern class, permitting the cacheing
 * of precompiled regexs.
 */
public class URIDestinationCreator implements JmsInternalConstants
{
  private static TraceComponent tc = SibTr.register(URIDestinationCreator.class,ApiJmsConstants.MSG_GROUP_INT,ApiJmsConstants.MSG_BUNDLE_INT);

  // Values indicating how to behave if the URI contains an MA88 style queue manager
  public static final int QM_ERROR     = 0; // Generate an exception. Used for Session.createQueue().
  public static final int QM_DISCARD   = 1; // Throw away the QM info. Used for PSB with JMSDestination.
  public static final int QM_TRANSFORM = 2; // Convert to queue@QM. Used for PSB with JMSReplyTo.
  public static final int QM_PEV       = 3; // Conver to seperate queue and QM as bus name.  Used by PEV with JMSReplyTo

  // Enum to indicate the type of a Destination
  public enum DestType { QUEUE, TOPIC }

  // constant strings identifying Jetstream destination properties
  private static final String TOPIC_NAME  = "topicName";
  private static final String REVERSE_RP  = "reverseRP";
  private static final String FORWARD_RP  = "forwardRP";

  // key characters to be checked for in URIs as Strings
  private static final String STRING_AMPERSAND = "&";
  private static final String STRING_BACKSLASH = "\\";
  private static final String STRING_EQUALS_SIGN = "=";

  // key characters to be checked for in URIs as chars
  private static final char CHAR_BACKSLASH = '\\';


  // MA88 specific properties
  private static final String MA88_EXPIRY = "expiry";
  private static final String MA88_PERSISTENCE = "persistence";
  private static final String MA88_BROKER_VERSION = "brokerVersion"; // 0 = fuji, 1 = mqsi

  // used to convert the topic wildcard syntax
  private TopicWildcardTranslation twt = null;

  /**
   * Public constructor.
   * Used to initialise a MsgDestEncodingUtilsImpl object.
   */
  public URIDestinationCreator() {
    try {
      twt = TopicWildcardTranslation.getInstance();
    }
    catch(Exception e) {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Exception caught calling TopicWildcardTranslation.getInstance()");
    }
  }

  /**
   * Extract the value of the named property from URI.
   * Find the named property in the name value pairs of the uri and return the value,
   * or null if the property is not present.
   *
   * @param propName the name of the property whose value is required
   * @param uri the URI to search in
   * @return
   */
  public String extractPropertyFromURI(String propName, String uri) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "extractPropertyFromURI", new Object[]{propName, uri});
    String result = null;

    // only something to do if uri is non-null & non-empty u
    if (uri != null) {
      uri = uri.trim();
      if (!uri.equals("")) {
        String[] parts = splitOnNonEscapedChar(uri, '?', 2);

        // parts[1] (if present) is the NVPs, so only something to do if present & non-empty
        if (parts.length >= 2) {
          String nvps = parts[1].trim();
          if (!nvps.equals("")) {

            // break the nvps string into an array of name=value strings
            // Use a regular expression to split on an '&' only if it isn't preceeded by a '\'.
            String[] nvpArray = splitOnNonEscapedChar(nvps, '&', -1);
            // Search the array for the named property
            String propNameE = propName+"=";
            for (int i = 0; i < nvpArray.length; i++) {
              String nvp = nvpArray[i];
              if (nvp.startsWith(propNameE)) {
                // everything after the = is the value
                result = nvp.substring(propNameE.length());
                break; // exit the loop
              }
            }
          }
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "extractPropertyFromURI",  result);
    return result;
  }


  /**
   * This is the method called from JmsSessionImpl.createQueue()|Topic().
   * The URI passed is first examined, and then the processing is delegated
   * to one of the more specific methods.
   *
   * The only real processing done in this method is to remove any arbitrary
   * escape characters (see code comment below for more detail).
   *
   * @param uri The URI from which to create the Destination
   * @param destType String indicating Topic or Queue
   * @throws JMSException if problems occure creating Destination from URI
   * @return Object The fully configured Destination
   */
  public Destination createDestinationFromString(String uri, DestType destType) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createDestinationFromString", new Object[]{uri, destType});
    JmsDestination result = null;

    // throw exception if URI begins with illegal string
    if (uri.startsWith(JmsDestinationImpl.DEST_PREFIX)) {
      throw (JMSException) JmsErrorUtils.newThrowable(
        JMSException.class,
        "RESERVED_DEST_PREFIX_CWSIA0383",
        new Object[] { JmsDestinationImpl.DEST_PREFIX, uri },
        tc);
    }

    // Decide which method should process the URI, determined on its prefix.
    if (uri.startsWith(JmsQueueImpl.QUEUE_PREFIX) || uri.startsWith(JmsTopicImpl.TOPIC_PREFIX)) {
      result = processURI(uri, QM_ERROR, null);
    }
    else {
      if (destType == DestType.QUEUE) {
        JmsQueueImpl q = new JmsQueueImpl();
        q.setQueueName(uri);
        result = q;
      }
      else {
        // processShortopicForm processes the optional @topicspace element.
        result = processShortTopicForm(uri);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createDestinationFromString",  result);
    return result;
  }

  /**
   * Create a Topic from a short form string.
   * Short form strings are either "topicname" or "topicspace:topicname".
   *
   * @param topic The short form string
   * @return A Topic object representing the topic described by short form string.
   * @throws JMSException
   */
  private JmsTopicImpl processShortTopicForm(String topic) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processShortTopicForm", topic);
    String topicName, topicSpace;

    // Split the string into the topic and the optional topic space
    // by searching for a non-escaped ':'
    // the , 2 indicates that we only want the string split into 2 parts, i.e. at the first
    // non-escaped :.
    // If a non-escaped : is not found, the entire string is copied to parts[0].
    String[] parts = splitOnNonEscapedChar(topic, ':', 2);

    // the parts[0] may contain "\:" sequences that should now
    // be transformed to ':'
    parts[0] = unescape(parts[0], ':');

    // Only try to unescape this if it actually exists!
    // 237662 - this handles the case where the user escaped their : characters after
    // the unescaped separator - eg topic\\:space:to\\:pic.
    // It's not strictly necessary, but is the logical thing to do since the user
    // wouldn't know we used not to worry if there was more than one unescaped :
    if (parts.length > 1) {
      parts[1] = unescape(parts[1], ':');
    }

    // The only valid escape sequences that should be left are "\\" which
    // transforms to a single '\'
    parts[0] = unescapeBackslash(parts[0]);

    // if there was no :, then parts[0] is the topicName.
    // if there was a :, then parts[0] is the topicSpace and parts[1] is the topicName
    if (parts.length==1) {
      topicName = parts[0];
      topicSpace = null;
    }
    else {
      topicSpace = parts[0];
      topicName = parts[1];
    }

    JmsTopicImpl result = new JmsTopicImpl();
    result.setTopicName(topicName);

    // Was a topicSpace specified?
    if (topicSpace != null) result.setTopicSpace(topicSpace);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processShortTopicForm",  result);
    return result;
  }

  /**
   * This method is called to process a URI starting "queue://" or "topic://".
   *
   * @param uri The URI from which to create the destination.
   * @param origStr The original string provided by the application or message, for
   *                use in error messages.
   * @param qmProcessing flag to indicate how to deal with the QM in a MA88 style
   *                     queue://QM/queue string. QM_ERROR = generate exception,
   *                     QM_IGNORE = ignore, QM_TRANSFORM = convert to a queue@QM destname.
   * @param nvpReturn an optional Map for returning the processed NVPs. If supplied,
   *        must support the clear() operation.
   * @throws JMSException if a problem occurs creating a destination from the URI
   * @return JmsDestination A fully configured JmsQueueImpl or JmsTopicImpl
   */
  JmsDestination processURI(String uri, int qmProcessing, Map<String,String> nvpReturn) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processURI", new Object[]{uri, qmProcessing});

    boolean isQueue;
    JmsDestination result;

    // Setup for queue or topic depending on uri prefix
    if (uri.startsWith(JmsTopicImpl.TOPIC_PREFIX)) {
      result = new JmsTopicImpl();
      isQueue = false;
    }
    else if (uri.startsWith(JmsQueueImpl.QUEUE_PREFIX)) {
      result = new JmsQueueImpl();
      isQueue = true;
    }
    else {
      // throw an exception
      throw (JMSException) JmsErrorUtils.newThrowable(
        JMSException.class,
        "INTERNAL_ERROR_CWSIA0386",
        null,
        tc);
    }

    // Retrieve substring starting from AFTER the prefix to the end of the URI.
    // To do this we take off the first 8 characters which represent the queue prefix.
    String destURI = uri.substring(8, uri.length());

    // Split the string into the dest name and the optional name-value pairs
    // by searching for a non-escaped '?'
    // The 2 indicates that we only want the string split into 2 parts, i.e. at the first
    // non-escaped ?.
    // If a non-escaped ? is not found, the entire string is copied to parts[0].
    String[] parts = splitOnNonEscapedChar(destURI, '?', 2);

    // parts[0] is the dest name.
    String destName = parts[0];

    // parts[1] (if present) is the NVPs.
    String nvps = null;
    if (parts.length > 1) nvps = parts[1].trim();

    // If it is a queue, do some Queue specific modifications to destName
    if (isQueue) {
      // split at first unescaped / to break into qm/queue
      parts = splitOnNonEscapedChar(destName, '/', 2);
      String qmName=null, queueName=null;
      if (parts.length > 1) {
        qmName = parts[0];
        queueName = parts[1];
        // tidy up empty qmName
        if (qmName.trim().equals("")) qmName = null;
      }
      else {
        // no /, so everything is queueName and it's all in parts[0]
        queueName = parts[0];
      }

      // All of the cases below use queueName as the first part of the destName,
      // so factor out to simplify the code:
      destName = queueName;

      switch (qmProcessing) {
        case QM_TRANSFORM:
          // Support MA88-style Queue URI containing a queue manager.
          // recode as queue@QM if QM supplied
          if (qmName != null) {
            destName += "@" + qmName;
          }
          break;
        case QM_DISCARD:
          // DISCARD = discard the QM. - no-op, destName already set above.
          break;
        case QM_PEV:
          // For PEV, the QM name is used as the bus name
          result.setBusName(qmName);
          break;
        case QM_ERROR:
          // Perform a check to find an MA88-style Queue URI which identifies a
          // queue manager.
          // This URI would look like "queue://QM1/myQueue" - and should result
          // in an exception
          // being thrown because Jetstream does not support Queue Managers.
          if (qmName != null ) {
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                "INVALID_URI_ELEMENT_CWSIA0385", new Object[] { uri }, tc);
          }
          // else everything ok, so destName becomes the queueName, which has already been set
          break;
        default:
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unknown value for qmProcessing: " + qmProcessing);
          // throw an internal error
          throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
              "INTERNAL_ERROR_CWSIA0386", null, tc);
      }

      // Now we know there are no unescaped /s, we can remove the escapes.
      destName = unescape(destName, '/');
    }

    // de-escape any '\?'s
    // There can't be any non-escaped ?s, as we split destName before the first one.
    destName = unescape(destName, '?');

    // The only valid escape sequences that should be left are "\\" which
    // transforms to a single '\'
    destName = unescapeBackslash(destName);

    // Finally, we can put the processed destName into the Destination
    if (isQueue) {
      ((JmsQueueImpl) result).setQueueName(destName);
    }
    else {
      ((JmsTopicImpl) result).setTopicName(destName);
    }

    processNVPs(nvps, result, nvpReturn);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processURI",  result);
    return result;
  }


  /**
   * Split the specified string into the requested number of pieces using the splitter
   * character provided. This method is careful to only split on non-escaped instances
   * of the splitter character.
   *
   * Historical regex expressions that this replaces were as follows (from Javadoc)
   *   // Patterns used for splitting strings
   *   // NB These regexs are not completely rigorous, as they assume that a
   *   // preceding backslash always escapes the following character. This may not
   *   // be true, as the backslash may itself be escaped. The correct test is that
   *   // the character is escaped if it is preceded by an odd number of contiguous
   *   // backslashes, but I don't think this can be represented as a regex. To be
   *   // completely correct we would need to replace the regexs with a new routine
   *   // for splitting strings which combined indexof searching with the charIsEscaped
   *   // method. (Low priority, probably not worth the bother, but might be useful
   *   // for both correctness and performance).
   *
   *   // /'?' = non-capturing
   *   // '<!' = negative look-behind --> only  match if there isn't this char before it (behind!)
   *   // '\\\\' = 1 back slash --> 2 escaped to JVM, 1 escaped to Regex compiler
   *   // '\\?' = the char to split on, which has to be escaped (one \ eaten by JVM)
   *   private Pattern pSplitNonEscapedQMark = Pattern.compile("(?<!\\\\)\\?");
   *   private Pattern pSplitNonEscapedSlash = Pattern.compile("(?<!\\\\)/");
   *   // '?' = non-capturing
   *   // '<!' = negative look-behind --> only  match if there isn't this char before it (behind!)
   *   // '\\\\' = 1 back slash --> 2 escaped to JVM, 1 escaped to Regex compiler
   *   // '&' = the char to split on
   *   private Pattern pSplitNonEscapedAmpersand = Pattern.compile("(?<!\\\\)&");
   *   // '?' = non-capturing
   *   // '<!' = negative look-behind --> only  match if there isn't this char before it (behind!)
   *   // '\\\\' = 1 back slash --> 2 escaped to JVM, 1 escaped to Regex compiler
   *   // ':' = the char to split on
   *   private Pattern pSplitNonEscapedColon = Pattern.compile("(?<!\\\\):");
   *
   * @param inputStr The string to be split
   * @param splitChar The character on which the string should be split (if it is not escaped)
   * @param expectedPartCount The number of pieces the string should be split into. For example specifying
   *                  two here indicates splitting at the first occurence giving two parts - before and after.
   *                  Value of 1 indicates return just the original string. Value of less than 1 indicates
   *                  split into as many parts as necessary.
   * @return String[] containing the split parts.
   *                   - If the splitter character is not present then the array will be of size 1 containing
   *                     the full string.                                              size 's' will be 1 <= s <= n
   *                     depending upon whether there were enough separator characters to make up the requested
   *                     piece count. Note that in some extreme cases (like passing in empty string) there might only
   *                     be one element, which will be an empty string.
   *                   - If numPieces is less than 1 (as many pieces as necessary) then the array will be exactly
   *                     the required size (no nulls).
   */
  private String[] splitOnNonEscapedChar(String inputStr, char splitChar, int expectedPartCount) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "splitOnNonEscapedChar", new Object[]{inputStr, splitChar, expectedPartCount});

    String[] parts = null;
    List<String> partsList = null;

    // Index variable along the length of the string
    int startPoint = 0;
    int tokenStart = 0;
    int splitCharIndex = 0;

    // Used to keep track of how many parts we have found
    int partCount = 0;

    // Implement manual splitting here.

    // Loop through searching for unescaped splitter characters until we reach the end of the string
    // or the requested number of pieces.
    while ((splitCharIndex != -1) && (partCount != (expectedPartCount-1))) {
      splitCharIndex = inputStr.indexOf(splitChar, startPoint);

      // We only have work to do here if a splitter char was found and it was not escaped.
      if ((splitCharIndex != -1) && !charIsEscaped(inputStr, splitCharIndex)) {
        // Found a non-escaped splitter character

        // Set the first part of the string.
        String nextPart = inputStr.substring(tokenStart, splitCharIndex);

        // If we have not yet initialized the storage then do so now.
        if (expectedPartCount >= 2) {
          if (parts == null) parts = new String[expectedPartCount];
          parts[partCount] = nextPart;
        }
        else {
          // Variable storage mechanism.
          if (partsList == null) partsList = new ArrayList<String>(5);
          partsList.add(nextPart);
        }

        // Indicate that we have found a new part.
        partCount++;

        // Move up the start point appropriately
        startPoint = splitCharIndex+1;
        tokenStart = startPoint;
      }

      else {
        // If the splitter is escaped then we need to look further
        // down the string for the next one.
        startPoint = splitCharIndex+1;
        // implicit 'continue' on the while loop.
      }

    }

    // Get the last part of the token string here. Either we haven't found another splitter,
    // or we just need to pick up the last chunk (regardless of whether it contains further
    // splitters).

    String nextPart = inputStr.substring(tokenStart, inputStr.length());

    if (expectedPartCount >= 2) {
      if (parts == null) {
        // If we haven't found any splitters then just pass back the string itself.
        parts = new String[1];
      }
      // Either there weren't any splitter characters, or we found one at some point but
      // have subsequently run out of further ones to process - in any case we need to put
      // in the last part of the string.
      parts[partCount] = nextPart;
    }
    else {
      // Variable storage final step here.
      if (partsList == null) {
        // No parts found - special optimized path here.
        parts = new String[1];
        parts[0] = nextPart;
      }
      else {
        partsList.add(nextPart);
        // Conver the list back into a string[]
        parts = partsList.toArray(new String[] {});
      }
    }

    // Do a quick check to see whether we were unable to make up the requested number of elements in
    // the array (in which case it is too big and needs resizing). This should hopefully be a minority
    // case.
    if ((parts[parts.length-1] == null)) {
      // There are one or more empty spaces at the end of the array.
      // Find the last element that doesn't have real data in it and trim the array appropriately.
      // In observations the last element can be empty string, so the partCount isn't any good to us here.
      int lastValidEntry = 0;
      while ((parts[lastValidEntry] != null) && (!"".equals(parts[lastValidEntry]))) {
        lastValidEntry++;
      }
      String[] tempParts = new String[lastValidEntry];
      System.arraycopy(parts, 0, tempParts, 0, lastValidEntry);
      parts = tempParts;
    }

    // Slightly unconventional way of ensuring that the content of the array is properly traced rather than just
    // the array object code (tr.exit will call toString on each of the parts of the array)
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "splitOnNonEscapedChar",  parts);
    return parts;
  }


  /**
   * Configure a JmsDestination object with the data held as name-value pairs
   * in the nvps String.
   * @param nvps The name value pairs from a queue:// or topic:// uri.
   * @param dest The JmsTopicImpl or JmsQueueImpl to be configured.
   * @param nvpReturn If not null, use the provided map to return the processed NVPs. If provided,
   * the map must support the clear() operation.
   * @throws JMSException if the name value pairs are incorrectly formed.
   */
  private void processNVPs(String nvps, JmsDestination dest, Map<String,String> nvpReturn) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processNVPs", new Object[]{nvps, dest});

    Map<String,String> propertyMap;
    if (nvpReturn==null) {
      // no map provided by caller, so make one of our own
      propertyMap = new HashMap<String,String>();
    }
    else {
      // use the callers map, after clearing it
      nvpReturn.clear();
      propertyMap = nvpReturn;
    }

    // Process any name value pairs
    if (nvps != null && !nvps.equals("")) {

      // Use a regular expression to split on an '&' only if it isn't preceeded by a '\'.
      // Statement goal = matching an '&' only if not preceeded by a '\'
      String[] nvpArray = splitOnNonEscapedChar(nvps, '&', -1);

      // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
      // At this point we have an array of strings in the format 'name=value'*
      // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

      // Begin processing on each NVP.
      for (int i = 0; i < nvpArray.length; i++) {
        String nvp = nvpArray[i];
        // First check that the URI element is correctly formed
        // ie. it contains an '=' char and has a value after the '='.
        int equalsIndex = nvp.indexOf(STRING_EQUALS_SIGN);
        if (equalsIndex == -1) {
          // doesn't have an equals
          JmsErrorUtils.newThrowable(   // d267332 just log an FFDC. Don't throw an exception and lose the destination
            JMSException.class,
            "MALFORMED_URI_ELEMENT_CWSIA0382",
            new Object[] { nvp, nvps },
            null,
            "URIDestinationCreator.processNVPs#1",
            this,
            tc);
            continue;
        }
        if (equalsIndex == nvp.length() - 1) {
          // '=' is the last char, so there is no value part
          JmsErrorUtils.newThrowable(   // d267332 just log an FFDC. Don't throw an exception and lose the destination
            JMSException.class,
            "MALFORMED_URI_ELEMENT_CWSIA0382",
            new Object[] { nvp, nvps },
            null,
            "URIDestinationCreator.processNVPs#2",
            this,
            tc);
            continue;
        }

        // For each element in the array separate the name and value, then process.
        String namePart = nvp.substring(0, equalsIndex);
        String valuePart = nvp.substring(equalsIndex + 1);

        // validate the name and value strings - check for unescaped illegal character etc..
        String[] validatedNVP = validateNVP(namePart, valuePart, nvps, dest);
        namePart = validatedNVP[0];
        valuePart = validatedNVP[1];

        // special case forward and reverse routing path NVPs
        if (namePart.equals(FORWARD_RP)) {
          String[] frp = valuePart.split("<#>");
          ((JmsDestinationImpl) dest).setForwardRoutingPath(frp);
          continue;
          // don't put this property in the map, and start the for loop again
        }
        else if (namePart.equals(REVERSE_RP)) {
          String[] rrp = valuePart.split("<#>");
          ((JmsDestinationImpl) dest).setReverseRoutingPath(rrp);
          continue;
          // don't put this property in the map, and start the for loop again
        }

        // add the NVP to the property map
        propertyMap.put(namePart, valuePart);
      }
    }

    // Do we need to perform topic wild card translation?
    if (dest instanceof JmsTopic && propertyMap.containsKey(MA88_BROKER_VERSION)) {

      String topicName = ((JmsTopicImpl) dest).getTopicName();
      // no point converting something thats empty, and avoids NPEs
      if (topicName != null) {
        // get the value of the brokerVersion property, and remove it from the map,
        // since it isn't needed by configureDestinationFromMap().
        String bVer = propertyMap.remove(MA88_BROKER_VERSION);
        if ("1".equals(bVer)) {
          // EB/MQSI topic syntax
          try {
            // perform the conversion
            topicName = twt.convertEventBrokerToSIB(topicName);
          }
          catch (ParseException pe) {
            // No FFDC code needed
            // d238447 FFDC review. Is this an internal or external error? Play it safe and
            //   generate an FFDC.
            JmsErrorUtils.newThrowable(   // d267332 just log an FFDC. Don't throw an exception and lose the destination
              JMSException.class,
              "MALFORMED_URI_ELEMENT_CWSIA0382",
              new Object[] { topicName, nvps },
              pe,
              "URIDestinationCreator.processNVPs#3",
              this,
              tc);
          }
        }
        else { // default to MA0C (Fuji) topic syntax
          try {
            // perform the conversion
            topicName = twt.convertMA0CToSIB(topicName);
          }
          catch (ParseException pe) {
            // No FFDC code needed
            // d238447 FFDC review.
            // Updated during d272111. ParseException can be generated in response to badly formed
            // input from user code, so don't FFDC.
            JmsErrorUtils.newThrowable(   // d267332 just log an FFDC. Don't throw an exception and lose the destination
              JMSException.class,
              "MALFORMED_URI_ELEMENT_CWSIA0382",
              new Object[] { topicName, nvps },
              pe,
              "URIDestinationCreator.processNVPs#4",
              this,
              tc);
          }
        }

        // put the converted topicName back into the Topic
        ((JmsTopicImpl)dest).setTopicName(topicName);

      } // if topicName != null

    } // end of if (need to convert topic wildcards)

    // configure the destination object using the map
    configureDestinationFromMap(dest, propertyMap, nvps);
  }


  /**
   * Remove '\' characters from input String when they precede
   * specified character.
   * @param input The string to be processed
   * @param c The character that should be unescaped
   * @return The modified String
   */
  private String unescape(String input, char c) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unescape", new Object[]{input, c});

    String result = input;
    // if there are no instances of c in the String we can just return the original
    if (input.indexOf(c) != -1) {
      int startValue = 0;
      StringBuffer temp = new StringBuffer(input);
      String cStr = new String(new char[] { c });
      while ((startValue = temp.indexOf(cStr, startValue)) != -1) {
        if (startValue > 0 && temp.charAt(startValue - 1) == '\\') {
          // remove the preceding slash to leave the escaped character
          temp.deleteCharAt(startValue - 1);
        }
        else {
          startValue++;
        }
      }
      result = temp.toString();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unescape",  result);
    return result;
  }

  /**
   * Convert escaped backslash to single backslash.
   * This method de-escapes double backslashes whilst at the same time
   * checking that there are no single backslahes in the input string.
   *
   * @param input The string to be processed
   * @return The modified String
   * @throws JMSException if an unescaped backslash is found
   */
  private String unescapeBackslash(String input) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unescapeBackslash", input);
    String result = input;

    // If there are no backslashes then don't bother creating the buffer etc.
    if (input.indexOf("\\") != -1) {

      int startValue = 0;
      StringBuffer tmp = new StringBuffer(input);
      while ((startValue = tmp.indexOf("\\", startValue)) != -1) {
        // check that the next character is also a \
        if (startValue + 1 < tmp.length() && tmp.charAt(startValue + 1) == '\\') {
          // remove the first slash
          tmp.deleteCharAt(startValue);
          // increment startValue so that the next indexOf begins after the second slash
          startValue++;
        }
        else {
          // we've found a single \, so throw an exception
          throw (JMSException) JmsErrorUtils.newThrowable(
            JMSException.class,
            "BAD_ESCAPE_CHAR_CWSIA0387",
            new Object[] { input },
            tc);
        }
      }
      result = tmp.toString();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unescapeBackslash",  result);
    return result;
  }

  /**
   * This utility method performs the validation on the name and value parts of the NVP.
   * It performs several checks to make sure that neither part contain any illegal characters
   * unless they are escaped by a backslash.
   *
   * The method also performs a conversion between any Jetstream-mappable MA88 properties.
   * Finally, it returns the new values in a String array to the calling code.
   *
   * @param String namePart The name part of an NVP
   * @param String valuePart The value part of an NVP
   * @param String uri The full URI (needed for debug and exception statements)
   * @param JmsDestination dest updated if dest name requires wild card conversion
   *
   * @throws JMSException If an NVP element is found to be illegal
   * @return String[] The newly validated NVP element
   */
  private String[] validateNVP(String namePart, String valuePart, String uri, JmsDestination dest) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "validateNVP", new Object[]{namePart, valuePart, uri, dest});

    // de-escape any escaped &s in the value (we define names, so no & there)
    if (valuePart.indexOf('&') != -1) {
      valuePart = valuePart.replaceAll("\\\\&", "&");
    }

    // The only valid escape sequence that should be left is "\\" which
    // transforms to a single '\'
    valuePart = unescapeBackslash(valuePart);

    // Performing mapping from MA88 names to Jetstream names.
    // If the name part is any of the following MA88 properties, we leave it because
    // there is no direct Jetstream equivalent for the property:
    // 'CCSID', 'encoding', 'brokerDurSubQueue', 'brokerCCDurSubQueue', 'multicast'.
    // In this event, the unknown property will be silently ignored, and NOT throw an exception.
    if (namePart.equalsIgnoreCase(MA88_EXPIRY)) {
      namePart = JmsInternalConstants.TIME_TO_LIVE;
    }
    if (namePart.equalsIgnoreCase(MA88_PERSISTENCE)) {
      namePart = JmsInternalConstants.DELIVERY_MODE;

      // map between the MA88 Integer values and the Jetstream String values
      if (valuePart.equals("1")) {
        valuePart = ApiJmsConstants.DELIVERY_MODE_NONPERSISTENT;
      }
      else if (valuePart.equals("2")) {
        valuePart = ApiJmsConstants.DELIVERY_MODE_PERSISTENT;
      }
      else {
        valuePart = ApiJmsConstants.DELIVERY_MODE_APP;
      }
    }

    // create a new String[] and populate it with the newly validated element, then return.
    String[] result = new String[] { namePart, valuePart };

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "validateNVP",  result);
    return result;
  }

  /**
   * This utililty method uses the supplied Map to configure a destination property.
   * The map contains the name and value pairs from the URI.
   *
   * @param Destination dest The Destination object to configure
   * @param Map props The Map collection containing the NVPs from the URI
   * @param String uri The original URI used for debugging purposes
   * @throws JMSException In the event of an error configuring the destination
   */
  private void configureDestinationFromMap(JmsDestination dest, Map<String,String> props, String uri) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "configureDestinationFromMap", new Object[]{dest, props, uri});

    // Iterate over the map, retrieving the name and value parts of the NVP.
    Iterator<Map.Entry<String,String>> iter = props.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String,String> nextProp = iter.next();
      String namePart = nextProp.getKey();
      String valuePart = nextProp.getValue();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "name " + namePart + ", value " + valuePart);

      // Here we want to try and get the class for the value part or a property name.
      // If this returns null, we can silently ignore the property because we know
      // that it in not a valid property to set on a Destination object.
      // We also use a boolean flag so that we don't then try to process this property again below.
      boolean propertyIsSettable = true;
      Class cl = MsgDestEncodingUtilsImpl.getPropertyType(namePart);
      if (cl == null) {
        propertyIsSettable = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Ignoring invalid property " + namePart);
      }

      // Now we know they're legal, we can process the name and value pairs
      // Determine the type of the value using a dynamic lookup on the name.
      // This uses the utility methods in MsgDestEncodingUtilsImpl.
      // We only want to perform this step if an exception was not throw in the code block above.
      if (propertyIsSettable) {
        try {
          // Convert the property to the right type of object
          Object valueObject = MsgDestEncodingUtilsImpl.convertPropertyToType(namePart, valuePart);
          if (namePart.equals(TOPIC_NAME)) {
            // special case topicName, as it may be null, and the reflection stuff can't cope
            if (dest instanceof JmsTopicImpl) {
              ((JmsTopicImpl) dest).setTopicName((String) valueObject);
            }
            else {
              // generate an internal error
              throw (JMSException) JmsErrorUtils.newThrowable(
                JMSException.class,
                "INTERNAL_ERROR_CWSIA0386",
                null,
                tc);
            }
          }
          else {
            // call setDestinationProperty(JmsDestination dest, String propName, String propVal)
            MsgDestEncodingUtilsImpl.setDestinationProperty(dest, namePart, valueObject);
          }
        }
        catch (NumberFormatException nfe) {
          // No FFDC code needed
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.exception(tc, nfe);
          // d238447 FFDC review.
          // Updated during d272111. NFEs can be generated by supplying badly formed URIs from
          // user code, so don't FFDC.
          throw (JMSException) JmsErrorUtils.newThrowable(
            JMSException.class,
            "INVALID_URI_ELEMENT_CWSIA0384",
            new Object[] { namePart, valuePart, uri },
            nfe, null, null, // nulls = no ffdc
            tc);
        }
        catch (JMSException jmse) {
          // No FFDC code needed
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exception(tc, jmse);
          throw (JMSException) JmsErrorUtils.newThrowable(
            JMSException.class,
            "INVALID_URI_ELEMENT_CWSIA0384",
            new Object[] { namePart, valuePart, uri },
            tc);
        }
      }

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "configureDestinationFromMap");
  }

  /**
   * This method is used by encodeMap() to escape &s in the value
   */
  public static String escapeValueString(String propValue) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "escapeValueString", propValue);

    if(propValue != null) {
      // check for unescaped '\' chars
      int nextIndex = 0;
      while((nextIndex = propValue.indexOf('\\', nextIndex)) != -1) {
        char nextChar = (char)-1;
        if ((nextIndex+1)<propValue.length()) nextChar = propValue.charAt(nextIndex + 1);
        if(nextChar != CHAR_BACKSLASH) {
          // add in an escape char
          propValue = propValue.substring(0, nextIndex) + STRING_BACKSLASH + propValue.substring(nextIndex);
        }
        nextIndex += 2; // need to move on by two
      }

      // check for unescaped '&' chars
      int startValueAmp = 0;
      int indexOfAmpInValue;
      while((indexOfAmpInValue = propValue.indexOf(STRING_AMPERSAND, startValueAmp)) != -1) {
        int newStartFrom = indexOfAmpInValue;
        char previousAtChar = (char)-1;
        if (indexOfAmpInValue>0) previousAtChar = propValue.charAt(indexOfAmpInValue - 1);
        if(previousAtChar != CHAR_BACKSLASH) {
          // add in an escape char
          propValue = propValue.substring(0, newStartFrom) + STRING_BACKSLASH + propValue.substring(newStartFrom);
        }
        startValueAmp = newStartFrom + 1;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "escapeValueString",  propValue);
    return propValue;
  }

  /**
   * This method is used by encodeMap() to escape ?s and \s in the dest name.
   *
   * NB This method will fail if called multiple times on the same string,
   * e.g. foo?bar -> foo\?bar -> foo\\\?bar etc.
   */
  public static String escapeDestName(String destName) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "escapeDestName", destName);

    if(destName != null) {
      // check for unescaped '\' chars
      int nextIndex = 0;
      while((nextIndex = destName.indexOf('\\', nextIndex)) != -1) {
        char nextChar = (char)-1;
        if ((nextIndex+1)<destName.length()) nextChar = destName.charAt(nextIndex + 1);
        if(nextChar != CHAR_BACKSLASH) {
          // add in an escape char
          destName = destName.substring(0, nextIndex) + STRING_BACKSLASH + destName.substring(nextIndex);
        }
        nextIndex += 2; // need to move on by two
      }

      // check for unescaped '?' chars
      nextIndex = 0;
      while((nextIndex = destName.indexOf('?', nextIndex)) != -1) {
        if(!charIsEscaped(destName, nextIndex)) {
          // add in an escape char
          destName = destName.substring(0, nextIndex) + STRING_BACKSLASH + destName.substring(nextIndex);
        }
        nextIndex++;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "escapeDestName",  destName);
    return destName;
  }

  /**
   * Create a Destination object from a full URI format String.
   * @param uri The URI format string describing the destination. If null, method returns
   * null. If not null, must begin with either queue:// or topic://, otherwise an ??
   * exception is thrown.
   * @param qmProcessing flag to indicate how to deal with QMs in MA88 queue URIs.
   * @return a fully configured Destination object (either JmsQueueImpl or JmsTopicImpl)
   * @throws JMSException if createDestinationFromString throws it.
   */
  public Destination createDestinationFromURI(String uri, int qmProcessing) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createDestinationFromURI", new Object[]{uri, qmProcessing});
    Destination result = null;
    if (uri != null) {
      result = processURI(uri, qmProcessing, null);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createDestinationFromURI",  result);
    return result;
  }

  /**
   * Test if the specified character is escaped.
   * Checks whether the character at the specified index is preceded by an
   * escape character. The test is non-trivial because it has to check that
   * the escape character is itself non-escaped.
   * @param str The string in which to perform the check
   * @param index The index in the string of the character that we are interested in.
   * @return true if the specified character is escaped.
   */
  private static boolean charIsEscaped(String str, int index) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "charIsEscaped", new Object[]{str, index});

    // precondition, null str or out of range index returns false.
    if (str == null || index < 0 || index >= str.length()) return false;

    // A character is escaped if it is preceded by an odd number of '\'s.
    int nEscape = 0;
    int i = index-1;
    while(i>=0 && str.charAt(i) == '\\') {
      nEscape++;
      i--;
    }
    boolean result = nEscape % 2 == 1 ;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "charIsEscaped",  result);
    return result;
  }
}
