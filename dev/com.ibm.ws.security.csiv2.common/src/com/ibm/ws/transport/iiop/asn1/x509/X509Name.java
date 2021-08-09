/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */

package com.ibm.ws.transport.iiop.asn1.x509;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.asn1.ASN1Encodable;
import com.ibm.ws.transport.iiop.asn1.ASN1EncodableVector;
import com.ibm.ws.transport.iiop.asn1.ASN1Sequence;
import com.ibm.ws.transport.iiop.asn1.ASN1Set;
import com.ibm.ws.transport.iiop.asn1.ASN1TaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERObjectIdentifier;
import com.ibm.ws.transport.iiop.asn1.DERSequence;
import com.ibm.ws.transport.iiop.asn1.DERSet;
import com.ibm.ws.transport.iiop.asn1.DERString;
import com.ibm.ws.transport.iiop.asn1.pkcs.PKCSObjectIdentifiers;

/**
 * <pre>
 * RDNSequence ::= SEQUENCE OF RelativeDistinguishedName
 * 
 * RelativeDistinguishedName ::= SET SIZE (1..MAX) OF AttributeTypeAndValue
 * 
 * AttributeTypeAndValue ::= SEQUENCE {
 * type OBJECT IDENTIFIER,
 * value ANY }
 * </pre>
 */
public class X509Name
                extends ASN1Encodable
{
    /**
     * country code - StringType(SIZE(2))
     */
    public static final DERObjectIdentifier C = new DERObjectIdentifier("2.5.4.6");

    /**
     * organization - StringType(SIZE(1..64))
     */
    public static final DERObjectIdentifier O = new DERObjectIdentifier("2.5.4.10");

    /**
     * organizational unit name - StringType(SIZE(1..64))
     */
    public static final DERObjectIdentifier OU = new DERObjectIdentifier("2.5.4.11");

    /**
     * Title
     */
    public static final DERObjectIdentifier T = new DERObjectIdentifier("2.5.4.12");

    /**
     * common name - StringType(SIZE(1..64))
     */
    public static final DERObjectIdentifier CN = new DERObjectIdentifier("2.5.4.3");

    /**
     * device serial number name - StringType(SIZE(1..64))
     */
    public static final DERObjectIdentifier SN = new DERObjectIdentifier("2.5.4.5");

    /**
     * locality name - StringType(SIZE(1..64))
     */
    public static final DERObjectIdentifier L = new DERObjectIdentifier("2.5.4.7");

    /**
     * state, or province name - StringType(SIZE(1..64))
     */
    public static final DERObjectIdentifier ST = new DERObjectIdentifier("2.5.4.8");

    /**
     * Naming attributes of type X520name
     */
    public static final DERObjectIdentifier SURNAME = new DERObjectIdentifier("2.5.4.4");
    public static final DERObjectIdentifier GIVENNAME = new DERObjectIdentifier("2.5.4.42");
    public static final DERObjectIdentifier INITIALS = new DERObjectIdentifier("2.5.4.43");
    public static final DERObjectIdentifier GENERATION = new DERObjectIdentifier("2.5.4.44");
    public static final DERObjectIdentifier UNIQUE_IDENTIFIER = new DERObjectIdentifier("2.5.4.45");

    /**
     * Email address (RSA PKCS#9 extension) - IA5String.
     * <p>Note: if you're trying to be ultra orthodox, don't use this! It shouldn't be in here.
     */
    public static final DERObjectIdentifier EmailAddress = PKCSObjectIdentifiers.pkcs_9_at_emailAddress;

    /**
     * more from PKCS#9
     */
    public static final DERObjectIdentifier UnstructuredName = PKCSObjectIdentifiers.pkcs_9_at_unstructuredName;
    public static final DERObjectIdentifier UnstructuredAddress = PKCSObjectIdentifiers.pkcs_9_at_unstructuredAddress;

    /**
     * email address in Verisign certificates
     */
    public static final DERObjectIdentifier E = EmailAddress;

    /*
     * others...
     */
    public static final DERObjectIdentifier DC = new DERObjectIdentifier("0.9.2342.19200300.100.1.25");

    /**
     * LDAP User id.
     */
    public static final DERObjectIdentifier UID = new DERObjectIdentifier("0.9.2342.19200300.100.1.1");

    /**
     * look up table translating OID values into their common symbols - this static is scheduled for deletion
     */
    public static Hashtable OIDLookUp = new Hashtable();

    /**
     * determines whether or not strings should be processed and printed
     * from back to front.
     */
    public static boolean DefaultReverse = false;

    /**
     * default look up table translating OID values into their common symbols following
     * the convention in RFC 2253 with a few extras
     */
    public static Hashtable DefaultSymbols = OIDLookUp;

    /**
     * look up table translating OID values into their common symbols following the convention in RFC 2253
     * with a few extras
     */
    public static Hashtable RFC2253Symbols = new Hashtable();

    /**
     * look up table translating string values into their OIDS -
     * this static is scheduled for deletion
     */
    public static Hashtable SymbolLookUp = new Hashtable();

    /**
     * look up table translating common symbols into their OIDS.
     */
    public static Hashtable DefaultLookUp = SymbolLookUp;

    static
    {
        DefaultSymbols.put(C, "C");
        DefaultSymbols.put(O, "O");
        DefaultSymbols.put(T, "T");
        DefaultSymbols.put(OU, "OU");
        DefaultSymbols.put(CN, "CN");
        DefaultSymbols.put(L, "L");
        DefaultSymbols.put(ST, "ST");
        DefaultSymbols.put(SN, "SN");
        DefaultSymbols.put(EmailAddress, "E");
        DefaultSymbols.put(DC, "DC");
        DefaultSymbols.put(UID, "UID");
        DefaultSymbols.put(SURNAME, "SURNAME");
        DefaultSymbols.put(GIVENNAME, "GIVENNAME");
        DefaultSymbols.put(INITIALS, "INITIALS");
        DefaultSymbols.put(GENERATION, "GENERATION");
        DefaultSymbols.put(UnstructuredAddress, "unstructuredAddress");
        DefaultSymbols.put(UnstructuredName, "unstructuredName");

        RFC2253Symbols.put(C, "C");
        RFC2253Symbols.put(O, "O");
        RFC2253Symbols.put(T, "T");
        RFC2253Symbols.put(OU, "OU");
        RFC2253Symbols.put(CN, "CN");
        RFC2253Symbols.put(L, "L");
        RFC2253Symbols.put(ST, "ST");
        RFC2253Symbols.put(SN, "SN");
        RFC2253Symbols.put(EmailAddress, "EMAILADDRESS");
        RFC2253Symbols.put(DC, "DC");
        RFC2253Symbols.put(UID, "UID");
        RFC2253Symbols.put(SURNAME, "SURNAME");
        RFC2253Symbols.put(GIVENNAME, "GIVENNAME");
        RFC2253Symbols.put(INITIALS, "INITIALS");
        RFC2253Symbols.put(GENERATION, "GENERATION");

        DefaultLookUp.put("c", C);
        DefaultLookUp.put("o", O);
        DefaultLookUp.put("t", T);
        DefaultLookUp.put("ou", OU);
        DefaultLookUp.put("cn", CN);
        DefaultLookUp.put("l", L);
        DefaultLookUp.put("st", ST);
        DefaultLookUp.put("sn", SN);
        DefaultLookUp.put("emailaddress", E);
        DefaultLookUp.put("dc", DC);
        DefaultLookUp.put("e", E);
        DefaultLookUp.put("uid", UID);
        DefaultLookUp.put("surname", SURNAME);
        DefaultLookUp.put("givenname", GIVENNAME);
        DefaultLookUp.put("initials", INITIALS);
        DefaultLookUp.put("generation", GENERATION);
        DefaultLookUp.put("unstructuredaddress", UnstructuredAddress);
        DefaultLookUp.put("unstructuredname", UnstructuredName);
    }

    private X509NameEntryConverter converter = null;
    private Vector ordering = new Vector();
    private Vector values = new Vector();
    private Vector added = new Vector();

    private ASN1Sequence seq;

    /**
     * Return a X509Name based on the passed in tagged object.
     * 
     * @param obj tag object holding name.
     * @param explicit true if explicitly tagged false otherwise.
     * @return the X509Name
     */
    public static X509Name getInstance(
                                       ASN1TaggedObject obj,
                                       boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static X509Name getInstance(
                                       Object obj)
    {
        if (obj == null || obj instanceof X509Name)
        {
            return (X509Name) obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new X509Name((ASN1Sequence) obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass());
    }

    /**
     * Constructor from ASN1Sequence
     * 
     * the principal will be a list of constructed sets, each containing an (OID, String) pair.
     */
    public X509Name(
                    ASN1Sequence seq)
    {
        this.seq = seq;

        Enumeration e = seq.getObjects();

        while (e.hasMoreElements())
        {
            ASN1Set set = (ASN1Set) e.nextElement();

            for (int i = 0; i < set.size(); i++)
            {
                ASN1Sequence s = (ASN1Sequence) set.getObjectAt(i);

                ordering.addElement(s.getObjectAt(0));
                values.addElement(((DERString) s.getObjectAt(1)).getString());
                added.addElement((i != 0) ? Boolean.TRUE : Boolean.FALSE);
            }
        }
    }

    /**
     * constructor from a table of attributes.
     * <p>
     * it's is assumed the table contains OID/String pairs, and the contents
     * of the table are copied into an internal table as part of the
     * construction process.
     * <p>
     * <b>Note:</b> if the name you are trying to generate should be
     * following a specific ordering, you should use the constructor
     * with the ordering specified below.
     */
    public X509Name(
                    Hashtable attributes)
    {
        this(null, attributes);
    }

    /**
     * Constructor from a table of attributes with ordering.
     * <p>
     * it's is assumed the table contains OID/String pairs, and the contents
     * of the table are copied into an internal table as part of the
     * construction process. The ordering vector should contain the OIDs
     * in the order they are meant to be encoded or printed in toString.
     */
    public X509Name(
                    Vector ordering,
                    Hashtable attributes)
    {
        this(ordering, attributes, new X509DefaultEntryConverter());
    }

    /**
     * Constructor from a table of attributes with ordering.
     * <p>
     * it's is assumed the table contains OID/String pairs, and the contents
     * of the table are copied into an internal table as part of the
     * construction process. The ordering vector should contain the OIDs
     * in the order they are meant to be encoded or printed in toString.
     * <p>
     * The passed in converter will be used to convert the strings into their
     * ASN.1 counterparts.
     */
    public X509Name(
                    Vector ordering,
                    Hashtable attributes,
                    X509DefaultEntryConverter converter)
    {
        this.converter = converter;

        if (ordering != null)
        {
            for (int i = 0; i != ordering.size(); i++)
            {
                this.ordering.addElement(ordering.elementAt(i));
                this.added.addElement(Boolean.FALSE);
            }
        }
        else
        {
            Enumeration e = attributes.keys();

            while (e.hasMoreElements())
            {
                this.ordering.addElement(e.nextElement());
                this.added.addElement(Boolean.FALSE);
            }
        }

        for (int i = 0; i != this.ordering.size(); i++)
        {
            DERObjectIdentifier oid = (DERObjectIdentifier) this.ordering.elementAt(i);

            if (attributes.get(oid) == null)
            {
                throw new IllegalArgumentException("No attribute for object id - " + oid.getId() + " - passed to distinguished name");
            }

            this.values.addElement(attributes.get(oid)); // copy the hash table
        }
    }

    /**
     * Takes two vectors one of the oids and the other of the values.
     */
    public X509Name(
                    Vector oids,
                    Vector values)
    {
        this(oids, values, new X509DefaultEntryConverter());
    }

    /**
     * Takes two vectors one of the oids and the other of the values.
     * <p>
     * The passed in converter will be used to convert the strings into their
     * ASN.1 counterparts.
     */
    public X509Name(
                    Vector oids,
                    Vector values,
                    X509NameEntryConverter converter)
    {
        this.converter = converter;

        if (oids.size() != values.size())
        {
            throw new IllegalArgumentException("oids vector must be same length as values.");
        }

        for (int i = 0; i < oids.size(); i++)
        {
            this.ordering.addElement(oids.elementAt(i));
            this.values.addElement(values.elementAt(i));
            this.added.addElement(Boolean.FALSE);
        }
    }

    /**
     * Takes an X509 dir name as a string of the format "C=AU, ST=Victoria", or
     * some such, converting it into an ordered set of name attributes.
     */
    public X509Name(
                    String dirName)
    {
        this(DefaultReverse, DefaultLookUp, dirName);
    }

    /**
     * Takes an X509 dir name as a string of the format "C=AU, ST=Victoria", or
     * some such, converting it into an ordered set of name attributes with each
     * string value being converted to its associated ASN.1 type using the passed
     * in converter.
     */
    public X509Name(
                    String dirName,
                    X509NameEntryConverter converter)
    {
        this(DefaultReverse, DefaultLookUp, dirName, converter);
    }

    /**
     * Takes an X509 dir name as a string of the format "C=AU, ST=Victoria", or
     * some such, converting it into an ordered set of name attributes. If reverse
     * is true, create the encoded version of the sequence starting from the
     * last element in the string.
     */
    public X509Name(
                    boolean reverse,
                    String dirName)
    {
        this(reverse, DefaultLookUp, dirName);
    }

    /**
     * Takes an X509 dir name as a string of the format "C=AU, ST=Victoria", or
     * some such, converting it into an ordered set of name attributes with each
     * string value being converted to its associated ASN.1 type using the passed
     * in converter. If reverse is true the ASN.1 sequence representing the DN will
     * be built by starting at the end of the string, rather than the start.
     */
    public X509Name(
                    boolean reverse,
                    String dirName,
                    X509NameEntryConverter converter)
    {
        this(reverse, DefaultLookUp, dirName, converter);
    }

    /**
     * Takes an X509 dir name as a string of the format "C=AU, ST=Victoria", or
     * some such, converting it into an ordered set of name attributes. lookUp
     * should provide a table of lookups, indexed by lowercase only strings and
     * yielding a DERObjectIdentifier, other than that OID. and numeric oids
     * will be processed automatically.
     * <br>
     * If reverse is true, create the encoded version of the sequence
     * starting from the last element in the string.
     * 
     * @param reverse true if we should start scanning from the end (RFC 2553).
     * @param lookUp table of names and their oids.
     * @param dirName the X.500 string to be parsed.
     */
    public X509Name(
                    boolean reverse,
                    Hashtable lookUp,
                    String dirName)
    {
        this(reverse, lookUp, dirName, new X509DefaultEntryConverter());
    }

    private DERObjectIdentifier decodeOID(
                                          String name,
                                          Hashtable lookUp)
    {
        if (name.toUpperCase().startsWith("OID."))
        {
            return new DERObjectIdentifier(name.substring(4));
        }
        else if (name.charAt(0) >= '0' && name.charAt(0) <= '9')
        {
            return new DERObjectIdentifier(name);
        }

        DERObjectIdentifier oid = (DERObjectIdentifier) lookUp.get(name.toLowerCase());
        if (oid == null)
        {
            throw new IllegalArgumentException("Unknown object id - " + name + " - passed to distinguished name");
        }

        return oid;
    }

    /**
     * Takes an X509 dir name as a string of the format "C=AU, ST=Victoria", or
     * some such, converting it into an ordered set of name attributes. lookUp
     * should provide a table of lookups, indexed by lowercase only strings and
     * yielding a DERObjectIdentifier, other than that OID. and numeric oids
     * will be processed automatically. The passed in converter is used to convert the
     * string values to the right of each equals sign to their ASN.1 counterparts.
     * <br>
     * 
     * @param reverse true if we should start scanning from the end, false otherwise.
     * @param lookUp table of names and oids.
     * @param dirName the string dirName
     * @param converter the converter to convert string values into their ASN.1 equivalents
     */
    public X509Name(
                    boolean reverse,
                    Hashtable lookUp,
                    String dirName,
                    X509NameEntryConverter converter)
    {
        this.converter = converter;
        X509NameTokenizer nTok = new X509NameTokenizer(dirName);

        while (nTok.hasMoreTokens())
        {
            String token = nTok.nextToken();
            int index = token.indexOf('=');

            if (index == -1)
            {
                throw new IllegalArgumentException("badly formated directory string");
            }

            String name = token.substring(0, index);
            String value = token.substring(index + 1);
            DERObjectIdentifier oid = decodeOID(name, lookUp);

            if (value.indexOf('+') > 0)
            {
                X509NameTokenizer vTok = new X509NameTokenizer(value, '+');

                this.ordering.addElement(oid);
                this.values.addElement(vTok.nextToken());
                this.added.addElement(Boolean.FALSE);

                while (vTok.hasMoreTokens())
                {
                    String sv = vTok.nextToken();
                    int ndx = sv.indexOf('=');

                    String nm = sv.substring(0, ndx);
                    String vl = sv.substring(ndx + 1);
                    this.ordering.addElement(decodeOID(nm, lookUp));
                    this.values.addElement(vl);
                    this.added.addElement(Boolean.TRUE);
                }
            }
            else
            {
                this.ordering.addElement(oid);
                this.values.addElement(value);
                this.added.addElement(Boolean.FALSE);
            }
        }

        if (reverse)
        {
            Vector o = new Vector();
            Vector v = new Vector();
            Vector a = new Vector();

            for (int i = this.ordering.size() - 1; i >= 0; i--)
            {
                o.addElement(this.ordering.elementAt(i));
                v.addElement(this.values.elementAt(i));
                a.addElement(this.added.elementAt(i));
            }

            this.ordering = o;
            this.values = v;
            this.added = a;
        }
    }

    /**
     * return a vector of the oids in the name, in the order they were found.
     */
    public Vector getOIDs()
    {
        Vector v = new Vector();

        for (int i = 0; i != ordering.size(); i++)
        {
            v.addElement(ordering.elementAt(i));
        }

        return v;
    }

    /**
     * return a vector of the values found in the name, in the order they
     * were found.
     */
    public Vector getValues()
    {
        Vector v = new Vector();

        for (int i = 0; i != values.size(); i++)
        {
            v.addElement(values.elementAt(i));
        }

        return v;
    }

    @Override
    public DERObject toASN1Object()
    {
        if (seq == null)
        {
            ASN1EncodableVector vec = new ASN1EncodableVector();
            ASN1EncodableVector sVec = new ASN1EncodableVector();
            DERObjectIdentifier lstOid = null;

            for (int i = 0; i != ordering.size(); i++)
            {
                ASN1EncodableVector v = new ASN1EncodableVector();
                DERObjectIdentifier oid = (DERObjectIdentifier) ordering.elementAt(i);

                v.add(oid);

                String str = (String) values.elementAt(i);

                v.add(converter.getConvertedValue(oid, str));

                if (lstOid == null
                    || ((Boolean) this.added.elementAt(i)).booleanValue())
                {
                    sVec.add(new DERSequence(v));
                }
                else
                {
                    vec.add(new DERSet(sVec));
                    sVec = new ASN1EncodableVector();

                    sVec.add(new DERSequence(v));
                }

                lstOid = oid;
            }

            vec.add(new DERSet(sVec));

            seq = new DERSequence(vec);
        }

        return seq;
    }

    /**
     * @param inOrder if true the order of both X509 names must be the same,
     *            as well as the values associated with each element.
     */
    public boolean equals(Object _obj, boolean inOrder)
    {
        if (_obj == this)
        {
            return true;
        }

        if (!inOrder)
        {
            return this.equals(_obj);
        }

        if (_obj == null || !(_obj instanceof X509Name))
        {
            return false;
        }

        X509Name _oxn = (X509Name) _obj;
        int _orderingSize = ordering.size();

        if (_orderingSize != _oxn.ordering.size())
        {
            return false;
        }

        for (int i = 0; i < _orderingSize; i++)
        {
            String _oid = ((DERObjectIdentifier) ordering.elementAt(i)).getId();
            String _val = (String) values.elementAt(i);

            String _oOID = ((DERObjectIdentifier) _oxn.ordering.elementAt(i)).getId();
            String _oVal = (String) _oxn.values.elementAt(i);

            if (_oid.equals(_oOID))
            {
                _val = _val.trim().toLowerCase();
                _oVal = _oVal.trim().toLowerCase();
                if (_val.equals(_oVal))
                {
                    continue;
                }
                else
                {
                    StringBuilder v1 = new StringBuilder();
                    StringBuilder v2 = new StringBuilder();

                    if (_val.length() != 0)
                    {
                        char c1 = _val.charAt(0);

                        v1.append(c1);

                        for (int k = 1; k < _val.length(); k++)
                        {
                            char c2 = _val.charAt(k);
                            if (!(c1 == ' ' && c2 == ' '))
                            {
                                v1.append(c2);
                            }
                            c1 = c2;
                        }
                    }

                    if (_oVal.length() != 0)
                    {
                        char c1 = _oVal.charAt(0);

                        v2.append(c1);

                        for (int k = 1; k < _oVal.length(); k++)
                        {
                            char c2 = _oVal.charAt(k);
                            if (!(c1 == ' ' && c2 == ' '))
                            {
                                v2.append(c2);
                            }
                            c1 = c2;
                        }
                    }

                    if (!v1.toString().equals(v2.toString()))
                    {
                        return false;
                    }
                }
            }
            else
            {
                return false;
            }
        }

        return true;
    }

    /**
     * test for equality - note: case is ignored.
     */
    @Override
    public boolean equals(Object _obj)
    {
        if (_obj == this)
        {
            return true;
        }

        if (_obj == null || !(_obj instanceof X509Name))
        {
            return false;
        }

        X509Name _oxn = (X509Name) _obj;

        if (this.getDERObject().equals(_oxn.getDERObject()))
        {
            return true;
        }

        int _orderingSize = ordering.size();

        if (_orderingSize != _oxn.ordering.size())
        {
            return false;
        }

        boolean[] _indexes = new boolean[_orderingSize];

        for (int i = 0; i < _orderingSize; i++)
        {
            boolean _found = false;
            String _oid = ((DERObjectIdentifier) ordering.elementAt(i)).getId();
            String _val = (String) values.elementAt(i);

            for (int j = 0; j < _orderingSize; j++)
            {
                if (_indexes[j] == true)
                {
                    continue;
                }

                String _oOID = ((DERObjectIdentifier) _oxn.ordering.elementAt(j)).getId();
                String _oVal = (String) _oxn.values.elementAt(j);

                if (_oid.equals(_oOID))
                {
                    _val = _val.trim().toLowerCase();
                    _oVal = _oVal.trim().toLowerCase();
                    if (_val.equals(_oVal))
                    {
                        _indexes[j] = true;
                        _found = true;
                        break;
                    }
                    else
                    {
                        StringBuilder v1 = new StringBuilder();
                        StringBuilder v2 = new StringBuilder();

                        if (_val.length() != 0)
                        {
                            char c1 = _val.charAt(0);

                            v1.append(c1);

                            for (int k = 1; k < _val.length(); k++)
                            {
                                char c2 = _val.charAt(k);
                                if (!(c1 == ' ' && c2 == ' '))
                                {
                                    v1.append(c2);
                                }
                                c1 = c2;
                            }
                        }

                        if (_oVal.length() != 0)
                        {
                            char c1 = _oVal.charAt(0);

                            v2.append(c1);

                            for (int k = 1; k < _oVal.length(); k++)
                            {
                                char c2 = _oVal.charAt(k);
                                if (!(c1 == ' ' && c2 == ' '))
                                {
                                    v2.append(c2);
                                }
                                c1 = c2;
                            }
                        }

                        if (v1.toString().equals(v2.toString()))
                        {
                            _indexes[j] = true;
                            _found = true;
                            break;
                        }
                    }
                }
            }

            if (!_found)
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        ASN1Sequence seq = (ASN1Sequence) this.getDERObject();
        Enumeration e = seq.getObjects();
        int hashCode = 0;

        while (e.hasMoreElements())
        {
            hashCode ^= e.nextElement().hashCode();
        }

        return hashCode;
    }

    private void appendValue(
                             StringBuilder buf,
                             Hashtable oidSymbols,
                             DERObjectIdentifier oid,
                             String value)
    {
        String sym = (String) oidSymbols.get(oid);

        if (sym != null)
        {
            buf.append(sym);
        }
        else
        {
            buf.append(oid.getId());
        }

        buf.append("=");

        int index = buf.length();

        buf.append(value);

        int end = buf.length();

        while (index != end)
        {
            if ((buf.charAt(index) == ',')
                || (buf.charAt(index) == '"')
                || (buf.charAt(index) == '\\')
                || (buf.charAt(index) == '+')
                || (buf.charAt(index) == '<')
                || (buf.charAt(index) == '>')
                || (buf.charAt(index) == ';'))
            {
                buf.insert(index, "\\");
                index++;
                end++;
            }

            index++;
        }
    }

    /**
     * convert the structure to a string - if reverse is true the
     * oids and values are listed out starting with the last element
     * in the sequence (ala RFC 2253), otherwise the string will begin
     * with the first element of the structure. If no string definition
     * for the oid is found in oidSymbols the string value of the oid is
     * added. Two standard symbol tables are provided DefaultSymbols, and
     * RFC2253Symbols as part of this class.
     * 
     * @param reverse if true start at the end of the sequence and work back.
     * @param oidSymbols look up table strings for oids.
     */
    @Trivial
    public String toString(
                           boolean reverse,
                           Hashtable oidSymbols)
    {
        StringBuilder buf = new StringBuilder();
        boolean first = true;

        if (reverse)
        {
            for (int i = ordering.size() - 1; i >= 0; i--)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    if (((Boolean) added.elementAt(i + 1)).booleanValue())
                    {
                        buf.append("+");
                    }
                    else
                    {
                        buf.append(",");
                    }
                }

                appendValue(buf, oidSymbols,
                            (DERObjectIdentifier) ordering.elementAt(i),
                            (String) values.elementAt(i));
            }
        }
        else
        {
            for (int i = 0; i < ordering.size(); i++)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    if (((Boolean) added.elementAt(i)).booleanValue())
                    {
                        buf.append("+");
                    }
                    else
                    {
                        buf.append(",");
                    }
                }

                appendValue(buf, oidSymbols,
                            (DERObjectIdentifier) ordering.elementAt(i),
                            (String) values.elementAt(i));
            }
        }

        return buf.toString();
    }

    @Override
    public String toString()
    {
        return toString(DefaultReverse, DefaultSymbols);
    }
}
