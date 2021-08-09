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
/* Generated By:JavaCC: Do not edit this line. WIMXPathInterpreter.java */
package com.ibm.ws.security.wim.xpath;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.ws.security.wim.xpath.mapping.datatype.LogicalNode;
import com.ibm.ws.security.wim.xpath.mapping.datatype.PropertyNode;
import com.ibm.ws.security.wim.xpath.mapping.datatype.XPathNode;
import com.ibm.ws.security.wim.xpath.util.MetadataMapper;
import com.ibm.wsspi.security.wim.exception.AttributeNotSupportedException;

@Trivial
public class WIMXPathInterpreter implements WIMXPathInterpreterConstants {
    static String CLASSNAME = WIMXPathInterpreter.class.getName();
    private XPathNode node = null;
    StringBuffer reposXPathExpression = new StringBuffer();
    StringBuffer laXPathExpression = new StringBuffer();
    List<String> entityTypes = null;
    private final String xsiType = "xsi:type";
    private final String la = "LA";
    private final String repos = "REPOS";
    private boolean checkLocation = false;

    private MetadataMapper _metaDataMapper = null;

    public List<String> getEntityTypes() {
        return entityTypes;
    }

    /*
     * private boolean checkNoun(String noun) throws ParseException { return
     * _metaDataMapper.isValidXPathNoun(noun); }
     */
    private String checkProperty(String propName) throws AttributeNotSupportedException {
        final String METHODNAME = "checkProperty";
        boolean inRepos = false;
        boolean inLA = false;
        if (propName.equals(xsiType)) {
/*
 * if (trcLogger.isLoggable(Level.FINER)) {
 * trcLogger.exiting(CLASSNAME, METHODNAME, " " + propName
 * + " is defined in Repository " + repos);
 * }
 */ return repos;
        }
        inRepos = false;
        for (int i = 0; i < entityTypes.size() && !inRepos; i++) {
            inRepos = _metaDataMapper.isPropertyInRepository(propName,
                                                             entityTypes.get(i));
        }
        if (!inRepos) {
            for (int i = 0; i < entityTypes.size(); i++) {
                inLA = _metaDataMapper.isPropertyInLookAside(propName,
                                                             entityTypes.get(i));
            }
        } else {
/*
 * if (trcLogger.isLoggable(Level.FINER)) {
 * trcLogger.exiting(CLASSNAME, METHODNAME, "Property " + propName
 * + " is defined in Repository " + repos);
 * }
 */ return repos;
        }
        if (!inRepos && !inLA) {
            throw new AttributeNotSupportedException(WIMMessageKey.ATTRIBUTE_NOT_SUPPORTED, WIMMessageKey.ATTRIBUTE_NOT_SUPPORTED);
        }
/*
 * if (trcLogger.isLoggable(Level.FINER)) {
 * trcLogger.exiting(CLASSNAME, METHODNAME, "Property " + propName
 * + " is defined in Repository " + la);
 * }
 */ return la;
    }

    @SuppressWarnings("unchecked")
    private String getPropertyLocation(XPathNode xnode) throws AttributeNotSupportedException {
        Iterator<PropertyNode> iter = xnode.getPropertyNodes(new HashMap<Integer, PropertyNode>());
        if (iter.hasNext()) {
            PropertyNode propNode = iter.next();
            String retVal = checkProperty(propNode.getName());
            if (retVal.equals(repos)) {
                propNode.setPropertyLocation(true);
            } else {
                propNode.setPropertyLocation(false);
            }
            return retVal;
        } else {
            return null;
        }
    }

    private void setPredicate(XPathNode astrPredicate) {
        String METHODNAME = "setPredicate";
        node = astrPredicate;
/*
 * if (trcLogger.isLoggable(Level.FINER)) {
 * trcLogger.logp(Level.FINER, CLASSNAME, METHODNAME,
 * "Predicate condition : " + astrPredicate);
 * }
 */ }

    /*
     * Simple grammar
     *
     * <LocationPath> := "//"<Noun>(<Predicate>) <Predicate> := "["
     * <PredicateExpr> "]" <RelationaExpr> := <FAName> <comp_operator>
     * (<Literal> | <Number>) // add negative numbers support <comp_operator> :=
     * '<' | '>' | '<=' | '>=' | '=' | '!=' <Name> := <Prefix> <':'> <NCName>
     * <Prefix> := <NCName> <NCName> := (<Letter | '_' ) <NCNameChar>
     * <NCNameChar> := (<Letter> | <Digit> | '.' | '_' | '-') <Letter> :=
     * [a-zA-Z] <Digit> := [0-9] <Noun> := <Name>
     */
    final public XPathNode parse(MetadataMapper aMetaDataMapper) throws ParseException, AttributeNotSupportedException {
        _metaDataMapper = aMetaDataMapper;
        if (_metaDataMapper != null) {
            checkLocation = true;
        }
        XPath();
        {
            if (true)
                return node;
        }
        throw new Error("Missing return statement in function");
    }

    // void XPath() : {} {<SLASH_SLASH> <ENTITIES> (predefinedPredicate())<EOF>
    // }
    final public void XPath() throws ParseException, AttributeNotSupportedException {
        wimexpr();
        jj_consume_token(0);
    }

    /*
     * void noun() : { Token t; } { t=<NAME> { checkNoun(t.image); } }
     */
    final public void wimexpr() throws ParseException, AttributeNotSupportedException {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case SLASH_SLASH:
                jj_consume_token(SLASH_SLASH);
                jj_consume_token(ENTITIES);
                if (jj_2_1(2147483647)) {
                    predefinedLocPredicate();
                } else {
                    switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                        case 4:
                            locOrEntityTypeExpr();
                            break;
                        default:
                            jj_la1[0] = jj_gen;
                            jj_consume_token(-1);
                            throw new ParseException();
                    }
                }
                break;
            case 2:
            case 7:
                if (jj_2_2(2147483647)) {
                    predefinedPredicate();
                } else {
                    switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                        case 7:
                            orEntityTypeExpr();
                            break;
                        default:
                            jj_la1[1] = jj_gen;
                            jj_consume_token(-1);
                            throw new ParseException();
                    }
                }
                break;
            default:
                jj_la1[2] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
    }

    final public void predefinedPredicate() throws ParseException, AttributeNotSupportedException {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case 7:
                entityTypeExpr();
                jj_consume_token(1);
                predicateExpr();
                break;
            case 2:
                jj_consume_token(2);
                orEntityTypeExpr();
                jj_consume_token(3);
                jj_consume_token(1);
                predicateExpr();
                break;
            default:
                jj_la1[3] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
    }

    final public void predefinedLocPredicate() throws ParseException, AttributeNotSupportedException {
        jj_consume_token(4);
        predefinedPredicate();
        jj_consume_token(5);
    }

    final public void locOrEntityTypeExpr() throws ParseException {
        jj_consume_token(4);
        orEntityTypeExpr();
        jj_consume_token(5);
    }

    final public void orEntityTypeExpr() throws ParseException {
        entityTypeExpr();
        label_1: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case 6:;
                    break;
                default:
                    jj_la1[4] = jj_gen;
                    break label_1;
            }
            jj_consume_token(6);
            entityTypeExpr();
        }
    }

    final public void internalPredicate() throws ParseException, AttributeNotSupportedException {
        jj_consume_token(4);
        predicateExpr();
        jj_consume_token(5);
    }

    final public void entityTypeExpr() throws ParseException {
        Token tOp;
        Token tL;
        jj_consume_token(7);
        tOp = jj_consume_token(COMPOP);
        tL = jj_consume_token(LITERAL);
        if (entityTypes == null) {
            entityTypes = new ArrayList<String>();
        }
        entityTypes.add(tL.image.substring(1, tL.image.length() - 1));
    }

    final public void predicate() throws ParseException, AttributeNotSupportedException {
        Token t;
        t = getToken(0);
/*
 * if (trcLogger.isLoggable(Level.FINER)) {
 * trcLogger.logp(Level.FINER, CLASSNAME, "predicate",
 * "Context token for predicate '" + t.image + "'");
 * }
 */
        predicateExpr();
    }

    final public void predicateExpr() throws ParseException, AttributeNotSupportedException {
        XPathNode strPredicate;
        strPredicate = simpleSelectionExpr();
        setPredicate(strPredicate);
    }

    final public XPathNode simpleSelectionExpr() throws ParseException, AttributeNotSupportedException {
        XPathNode s1 = null;
        XPathNode s2 = null;
        s1 = andExpr();
        label_2: while (true) {
            if (jj_2_3(2)) {
                ;
            } else {
                break label_2;
            }
            jj_consume_token(6);
            s2 = simpleSelectionExpr();
        }
        if (s2 != null) {
            LogicalNode lNode = new LogicalNode();
            if (checkLocation) {
                String locS1 = getPropertyLocation(s1);
                String locS2 = getPropertyLocation(s2);
                if (!locS1.equals(locS2)) {
                    lNode = new FederationLogicalNode();
                } else {
                    lNode.setPropertyLocation(locS1.equals(repos));
                }
            }
            lNode.setLeftChild(s1);
            lNode.setOperator("or");
            lNode.setRightChild(s2);
            {
                if (true)
                    return lNode;
            }
        } else {
            {
                if (true)
                    return s1;
            }
        }
        throw new Error("Missing return statement in function");
    }

    final public XPathNode orExpr() throws ParseException, AttributeNotSupportedException {
        XPathNode relExpr = null;
        XPathNode s1 = null;
        XPathNode s2 = null;

        s1 = andExpr();
        label_3: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case 6:;
                    break;
                default:
                    jj_la1[5] = jj_gen;
                    break label_3;
            }
            jj_consume_token(6);
            s2 = andExpr();
        }
        if (s2 != null) {
            if (s1 != null) {
                LogicalNode lNode = new LogicalNode();

                if (checkLocation) {
                    String locS1 = getPropertyLocation(s1);
                    String locS2 = getPropertyLocation(s2);
                    if (!locS1.equals(locS2)) {
                        lNode = new FederationLogicalNode();
                    } else {
                        lNode.setPropertyLocation(locS1.equals(repos));
                    }
                }

                lNode.setLeftChild(s1);
                lNode.setOperator("or");
                lNode.setRightChild(s2);
                {
                    if (true)
                        return lNode;
                }
            } else {
                {
                    if (true)
                        return relExpr;
                }
            }
        } else {
            {
                if (true)
                    return s1;
            }
        }
        throw new Error("Missing return statement in function");
    }

    final public XPathNode andExpr() throws ParseException, AttributeNotSupportedException {
        XPathNode bracketedSimpleSel = null;
        XPathNode relExpr = null;
        XPathNode simpleSel = null;
        XPathNode pNode = null;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case 2:
                jj_consume_token(2);
                bracketedSimpleSel = simpleSelectionExpr();
                jj_consume_token(3);
                label_4: while (true) {
                    if (jj_2_4(2)) {
                        ;
                    } else {
                        break label_4;
                    }
                    jj_consume_token(1);
                    simpleSel = simpleSelectionExpr();
                }
                if (simpleSel != null) {
                    LogicalNode lNode = new LogicalNode();
                    ParenthesisNode parenNode = new ParenthesisNode();
                    boolean inRepos = false;
                    if (checkLocation) {
                        String locS1 = getPropertyLocation(bracketedSimpleSel);
                        String locS2 = getPropertyLocation(simpleSel);
                        if (!locS1.equals(locS2)) {
                            lNode = new FederationLogicalNode();
                        } else {
                            inRepos = locS1.equals(repos);
                        }
                        short nodeType = bracketedSimpleSel.getNodeType();
                        switch (nodeType) {
                            case XPathNode.NODE_FED_LOGICAL:
                            case XPathNode.NODE_FED_PARENTHESIS:
                                parenNode = new FederationParenthesisNode();
                                lNode.setPropertyLocation(inRepos); // PM45289
                                break;
                            default:
                                parenNode.setPropertyLocation(inRepos);
                                lNode.setPropertyLocation(inRepos); // PM45289
                        }
                    }

                    parenNode.setChild(bracketedSimpleSel);
                    pNode = parenNode;
                    lNode.setLeftChild(pNode);
                    lNode.setOperator("and");
                    lNode.setRightChild(simpleSel);
                    {
                        if (true)
                            return lNode;
                    }
                } else {
                    ParenthesisNode parenNode = new ParenthesisNode();
                    if (checkLocation) {
                        short nodeType = bracketedSimpleSel.getNodeType();
                        switch (nodeType) {
                            case XPathNode.NODE_FED_LOGICAL:
                            case XPathNode.NODE_FED_PARENTHESIS:
                                parenNode = new FederationParenthesisNode();
                                break;
                            case XPathNode.NODE_LOGICAL:
                                parenNode.setPropertyLocation(((LogicalNode) bracketedSimpleSel).isPropertyInRepository());
                                break;
                            case XPathNode.NODE_PROPERTY:
                                parenNode.setPropertyLocation(((PropertyNode) bracketedSimpleSel).isPropertyInRepository());
                                break;
                            case XPathNode.NODE_PARENTHESIS:
                                parenNode.setPropertyLocation(((ParenthesisNode) bracketedSimpleSel).isPropertyInRepository());
                                break;
                            default:
                                break;
                        }
                    }
                    parenNode.setChild(bracketedSimpleSel);
                    {
                        if (true)
                            return parenNode;
                    }
                }
                break;
            case COMPOSITENAME:
            case NAME:
            case AT:
                relExpr = relationalExpr();
                label_5: while (true) {
                    if (jj_2_5(2)) {
                        ;
                    } else {
                        break label_5;
                    }
                    jj_consume_token(1);
                    simpleSel = simpleSelectionExpr();
                }
                if (simpleSel != null) {
                    LogicalNode lNode = new LogicalNode();

                    if (checkLocation) {
                        String locS1 = getPropertyLocation(relExpr);
                        String locS2 = getPropertyLocation(simpleSel);
                        if (!locS1.equals(locS2)) {
                            lNode = new FederationLogicalNode();
                        } else {
                            lNode.setPropertyLocation(locS1.equals(repos));
                        }
                    }

                    lNode.setLeftChild(relExpr);
                    lNode.setOperator("and");
                    lNode.setRightChild(simpleSel);
                    {
                        if (true)
                            return lNode;
                    }
                } else {
                    {
                        if (true)
                            return relExpr;
                    }
                }
                break;
            default:
                jj_la1[6] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        throw new Error("Missing return statement in function");
    }

    final public XPathNode relationalExpr() throws ParseException, AttributeNotSupportedException {
        String s;
        String value;
        Token tOp;
        s = faname();
        tOp = jj_consume_token(COMPOP);
        value = processToken();
        PropertyNode pNode = new PropertyNode();
        if (checkLocation) {
            String loc = checkProperty(s);
            pNode.setPropertyLocation(loc.equals(repos));
        }
        pNode.setName(s);
        pNode.setOperator(tOp.image);
        pNode.setValue(value);

        {
            if (true)
                return pNode;
        }
        throw new Error("Missing return statement in function");
    }

    final public String name() throws ParseException {
        Token t;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case NAME:
                t = jj_consume_token(NAME);
/*
 * if (trcLogger.isLoggable(Level.FINER)) {
 * trcLogger.logp(Level.FINER, CLASSNAME, "name", "name = "
 * + t.image);
 * }
 */ {
                if (true)
                    return t.image;
            }
                break;
            case COMPOSITENAME:
                t = jj_consume_token(COMPOSITENAME);
/*
 * if (trcLogger.isLoggable(Level.FINER)) {
 * trcLogger.logp(Level.FINER, CLASSNAME, "name", "name = "
 * + t.image);
 * }
 */ {
                if (true)
                    return t.image;
            }
                break;
            default:
                jj_la1[7] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        throw new Error("Missing return statement in function");
    }

    final public String faname() throws ParseException {
        String strFieldOrAttrName;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case AT:
                jj_consume_token(AT);
                break;
            default:
                jj_la1[8] = jj_gen;;
        }
        strFieldOrAttrName = name();
        // map attribute or field name to column name
        {
            if (true)
                return strFieldOrAttrName;
        }
        throw new Error("Missing return statement in function");
    }

    final public String processToken() throws ParseException {
        Token token;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case LITERAL:
                token = jj_consume_token(LITERAL);
                if (checkLocation) {
                    {
                        if (true)
                            return token.image;
                    }
                } else {
                    {
                        if (true)
                            return token.image.substring(1,
                                                         token.image.length() - 1);
                    }
                }
                break;
            case INTEGERLITERAL:
                token = jj_consume_token(INTEGERLITERAL); {
                if (true)
                    return token.image;
            }
                break;
            case DECIMALLITERAL:
                token = jj_consume_token(DECIMALLITERAL); {
                if (true)
                    return token.image;
            }
                break;
            default:
                jj_la1[9] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        throw new Error("Missing return statement in function");
    }

    final private boolean jj_2_1(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_1();
        } catch (Error ls) {
            return true;
        } finally {
            jj_save(0, xla);
        }
    }

    final private boolean jj_2_2(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_2();
        } catch (Error ls) {
            return true;
        } finally {
            jj_save(1, xla);
        }
    }

    final private boolean jj_2_3(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_3();
        } catch (Error ls) {
            return true;
        } finally {
            jj_save(2, xla);
        }
    }

    final private boolean jj_2_4(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_4();
        } catch (Error ls) {
            return true;
        } finally {
            jj_save(3, xla);
        }
    }

    final private boolean jj_2_5(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_5();
        } catch (Error ls) {
            return true;
        } finally {
            jj_save(4, xla);
        }
    }

    final private boolean jj_3R_18() {
        if (jj_3R_19())
            return true;
        if (jj_scan_token(COMPOP))
            return true;
        if (jj_3R_23())
            return true;
        return false;
    }

    final private boolean jj_3R_12() {
        if (jj_scan_token(7))
            return true;
        if (jj_scan_token(COMPOP))
            return true;
        if (jj_scan_token(LITERAL))
            return true;
        return false;
    }

    final private boolean jj_3R_17() {
        if (jj_scan_token(6))
            return true;
        if (jj_3R_12())
            return true;
        return false;
    }

    final private boolean jj_3R_22() {
        if (jj_scan_token(COMPOSITENAME))
            return true;
        return false;
    }

    final private boolean jj_3_5() {
        if (jj_scan_token(1))
            return true;
        if (jj_3R_8())
            return true;
        return false;
    }

    final private boolean jj_3R_8() {
        if (jj_3R_11())
            return true;
        Token xsp;
        while (true) {
            xsp = jj_scanpos;
            if (jj_3_3()) {
                jj_scanpos = xsp;
                break;
            }
        }
        return false;
    }

    final private boolean jj_3_1() {
        if (jj_3R_6())
            return true;
        return false;
    }

    final private boolean jj_3R_26() {
        if (jj_scan_token(DECIMALLITERAL))
            return true;
        return false;
    }

    final private boolean jj_3R_21() {
        if (jj_scan_token(NAME))
            return true;
        return false;
    }

    final private boolean jj_3R_20() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_21()) {
            jj_scanpos = xsp;
            if (jj_3R_22())
                return true;
        }
        return false;
    }

    final private boolean jj_3R_25() {
        if (jj_scan_token(INTEGERLITERAL))
            return true;
        return false;
    }

    final private boolean jj_3R_19() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_scan_token(21))
            jj_scanpos = xsp;
        if (jj_3R_20())
            return true;
        return false;
    }

    final private boolean jj_3R_14() {
        if (jj_3R_12())
            return true;
        Token xsp;
        while (true) {
            xsp = jj_scanpos;
            if (jj_3R_17()) {
                jj_scanpos = xsp;
                break;
            }
        }
        return false;
    }

    final private boolean jj_3R_13() {
        if (jj_3R_8())
            return true;
        return false;
    }

    final private boolean jj_3R_24() {
        if (jj_scan_token(LITERAL))
            return true;
        return false;
    }

    final private boolean jj_3R_23() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_24()) {
            jj_scanpos = xsp;
            if (jj_3R_25()) {
                jj_scanpos = xsp;
                if (jj_3R_26())
                    return true;
            }
        }
        return false;
    }

    final private boolean jj_3R_6() {
        if (jj_scan_token(4))
            return true;
        if (jj_3R_7())
            return true;
        if (jj_scan_token(5))
            return true;
        return false;
    }

    final private boolean jj_3R_16() {
        if (jj_3R_18())
            return true;
        Token xsp;
        while (true) {
            xsp = jj_scanpos;
            if (jj_3_5()) {
                jj_scanpos = xsp;
                break;
            }
        }
        return false;
    }

    final private boolean jj_3_2() {
        if (jj_3R_7())
            return true;
        return false;
    }

    final private boolean jj_3_4() {
        if (jj_scan_token(1))
            return true;
        if (jj_3R_8())
            return true;
        return false;
    }

    final private boolean jj_3R_10() {
        if (jj_scan_token(2))
            return true;
        if (jj_3R_14())
            return true;
        if (jj_scan_token(3))
            return true;
        if (jj_scan_token(1))
            return true;
        if (jj_3R_13())
            return true;
        return false;
    }

    final private boolean jj_3R_7() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_9()) {
            jj_scanpos = xsp;
            if (jj_3R_10())
                return true;
        }
        return false;
    }

    final private boolean jj_3R_9() {
        if (jj_3R_12())
            return true;
        if (jj_scan_token(1))
            return true;
        if (jj_3R_13())
            return true;
        return false;
    }

    final private boolean jj_3R_11() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_15()) {
            jj_scanpos = xsp;
            if (jj_3R_16())
                return true;
        }
        return false;
    }

    final private boolean jj_3R_15() {
        if (jj_scan_token(2))
            return true;
        if (jj_3R_8())
            return true;
        if (jj_scan_token(3))
            return true;
        Token xsp;
        while (true) {
            xsp = jj_scanpos;
            if (jj_3_4()) {
                jj_scanpos = xsp;
                break;
            }
        }
        return false;
    }

    final private boolean jj_3_3() {
        if (jj_scan_token(6))
            return true;
        if (jj_3R_8())
            return true;
        return false;
    }

    public WIMXPathInterpreterTokenManager token_source;
    SimpleCharStream jj_input_stream;
    public Token token, jj_nt;
    private int jj_ntk;
    private Token jj_scanpos, jj_lastpos;
    private int jj_la;
    public boolean lookingAhead = false;
    private int jj_gen;
    final private int[] jj_la1 = new int[10];
    static private int[] jj_la1_0;
    static private int[] jj_la1_1;
    static {
        jj_la1_0();
        jj_la1_1();
    }

    private static void jj_la1_0() {
        jj_la1_0 = new int[] { 0x10, 0x80, 0x80000084, 0x84, 0x40, 0x40,
                               0x206004, 0x6000, 0x200000, 0x3800000, };
    }

    private static void jj_la1_1() {
        jj_la1_1 = new int[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
                               0x0, };
    }

    final private JJCalls[] jj_2_rtns = new JJCalls[5];
    private boolean jj_rescan = false;
    private int jj_gc = 0;

    public WIMXPathInterpreter(java.io.InputStream stream) {
        jj_input_stream = new SimpleCharStream(stream, 1, 1, 1024);
        token_source = new WIMXPathInterpreterTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 10; i++)
            jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++)
            jj_2_rtns[i] = new JJCalls();
    }

    public void reInit(java.io.InputStream stream) {
        jj_input_stream.ReInit(stream, 1, 1, 1024);
        token_source.reInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 10; i++)
            jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++)
            jj_2_rtns[i] = new JJCalls();
    }

    public WIMXPathInterpreter(java.io.Reader stream) {
        jj_input_stream = new SimpleCharStream(stream, 1, 1, 1024);
        token_source = new WIMXPathInterpreterTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 10; i++)
            jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++)
            jj_2_rtns[i] = new JJCalls();
    }

    public void reInit(java.io.Reader stream) {
        jj_input_stream.ReInit(stream, 1, 1, 1024);
        token_source.reInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 10; i++)
            jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++)
            jj_2_rtns[i] = new JJCalls();
    }

    public WIMXPathInterpreter(WIMXPathInterpreterTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 10; i++)
            jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++)
            jj_2_rtns[i] = new JJCalls();
    }

    public void reInit(WIMXPathInterpreterTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 10; i++)
            jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++)
            jj_2_rtns[i] = new JJCalls();
    }

    final private Token jj_consume_token(int kind) throws ParseException {
        Token oldToken;
        if ((oldToken = token).next != null)
            token = token.next;
        else
            token = token.next = token_source.getNextToken();
        jj_ntk = -1;
        if (token.kind == kind) {
            jj_gen++;
            if (++jj_gc > 100) {
                jj_gc = 0;
                for (int i = 0; i < jj_2_rtns.length; i++) {
                    JJCalls c = jj_2_rtns[i];
                    while (c != null) {
                        if (c.gen < jj_gen)
                            c.first = null;
                        c = c.next;
                    }
                }
            }
            return token;
        }
        token = oldToken;
        jj_kind = kind;
        throw generateParseException();
    }

    final private boolean jj_scan_token(int kind) {
        if (jj_scanpos == jj_lastpos) {
            jj_la--;
            if (jj_scanpos.next == null) {
                jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
            } else {
                jj_lastpos = jj_scanpos = jj_scanpos.next;
            }
        } else {
            jj_scanpos = jj_scanpos.next;
        }
        if (jj_rescan) {
            int i = 0;
            Token tok = token;
            while (tok != null && tok != jj_scanpos) {
                i++;
                tok = tok.next;
            }
            if (tok != null)
                jj_add_error_token(kind, i);
        }
        if (jj_scanpos.kind != kind)
            return true;
        if (jj_la == 0 && jj_scanpos == jj_lastpos)
            throw new Error();
        return false;
    }

    final public Token getNextToken() {
        if (token.next != null)
            token = token.next;
        else
            token = token.next = token_source.getNextToken();
        jj_ntk = -1;
        jj_gen++;
        return token;
    }

    final public Token getToken(int index) {
        Token t = lookingAhead ? jj_scanpos : token;
        for (int i = 0; i < index; i++) {
            if (t.next != null)
                t = t.next;
            else
                t = t.next = token_source.getNextToken();
        }
        return t;
    }

    final private int jj_ntk() {
        if ((jj_nt = token.next) == null)
            return (jj_ntk = (token.next = token_source.getNextToken()).kind);
        else
            return (jj_ntk = jj_nt.kind);
    }

    private final java.util.Vector jj_expentries = new java.util.Vector();
    private int[] jj_expentry;
    private int jj_kind = -1;
    private final int[] jj_lasttokens = new int[100];
    private int jj_endpos;

    private void jj_add_error_token(int kind, int pos) {
        if (pos >= 100)
            return;
        if (pos == jj_endpos + 1) {
            jj_lasttokens[jj_endpos++] = kind;
        } else if (jj_endpos != 0) {
            jj_expentry = new int[jj_endpos];
            for (int i = 0; i < jj_endpos; i++) {
                jj_expentry[i] = jj_lasttokens[i];
            }
            boolean exists = false;
            for (java.util.Enumeration e = jj_expentries.elements(); e.hasMoreElements();) {
                int[] oldentry = (int[]) (e.nextElement());
                if (oldentry.length == jj_expentry.length) {
                    exists = true;
                    for (int i = 0; i < jj_expentry.length; i++) {
                        if (oldentry[i] != jj_expentry[i]) {
                            exists = false;
                            break;
                        }
                    }
                    if (exists)
                        break;
                }
            }
            if (!exists)
                jj_expentries.addElement(jj_expentry);
            if (pos != 0)
                jj_lasttokens[(jj_endpos = pos) - 1] = kind;
        }
    }

    public ParseException generateParseException() {
        jj_expentries.removeAllElements();
        boolean[] la1tokens = new boolean[40];
        for (int i = 0; i < 40; i++) {
            la1tokens[i] = false;
        }
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 10; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & (1 << j)) != 0) {
                        la1tokens[j] = true;
                    }
                    if ((jj_la1_1[i] & (1 << j)) != 0) {
                        la1tokens[32 + j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 40; i++) {
            if (la1tokens[i]) {
                jj_expentry = new int[1];
                jj_expentry[0] = i;
                jj_expentries.addElement(jj_expentry);
            }
        }
        jj_endpos = 0;
        jj_rescan_token();
        jj_add_error_token(0, 0);
        int[][] exptokseq = new int[jj_expentries.size()][];
        for (int i = 0; i < jj_expentries.size(); i++) {
            exptokseq[i] = (int[]) jj_expentries.elementAt(i);
        }
        return new ParseException(token, exptokseq, tokenImage);
    }

    final public void enable_tracing() {}

    final public void disable_tracing() {}

    final private void jj_rescan_token() {
        jj_rescan = true;
        for (int i = 0; i < 5; i++) {
            JJCalls p = jj_2_rtns[i];
            do {
                if (p.gen > jj_gen) {
                    jj_la = p.arg;
                    jj_lastpos = jj_scanpos = p.first;
                    switch (i) {
                        case 0:
                            jj_3_1();
                            break;
                        case 1:
                            jj_3_2();
                            break;
                        case 2:
                            jj_3_3();
                            break;
                        case 3:
                            jj_3_4();
                            break;
                        case 4:
                            jj_3_5();
                            break;
                        default:
                            break;
                    }
                }
                p = p.next;
            } while (p != null);
        }
        jj_rescan = false;
    }

    final private void jj_save(int index, int xla) {
        JJCalls p = jj_2_rtns[index];
        while (p.gen > jj_gen) {
            if (p.next == null) {
                p = p.next = new JJCalls();
                break;
            }
            p = p.next;
        }
        p.gen = jj_gen + xla - jj_la;
        p.first = token;
        p.arg = xla;
    }

    static final class JJCalls {
        int gen;
        Token first;
        int arg;
        JJCalls next;
    }

    public static void main(String args[]) throws AttributeNotSupportedException, ParseException {
        WIMXPathInterpreter parser = new WIMXPathInterpreter(new StringReader("//entities[@xsi:type='LoginAccount' and principalName='admin']"));
        parser.parse(null);
    }
}
