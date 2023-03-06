/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.iiop.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.omg.CosNaming.BindingHolder;
import org.omg.CosNaming.BindingIteratorHolder;
import org.omg.CosNaming.BindingListHolder;
import org.omg.CosNaming.BindingType;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;

public class NamingUtil {
    private static final String CNC_STRING = "CosNamingChecker";
    private static final String NESTED_CTX_STRING = "NestedContext";
    private static final String CNC_RESOLVABLE_STRING = "ResolvableCosNamingChecker";
    public static final NameComponent[] CNC_NAME = NamingUtil.makeName(CNC_STRING);
    public static final NameComponent[] NESTED_CTX_NAME = NamingUtil.makeName(NESTED_CTX_STRING);
    public static final NameComponent[] NESTED_CNC_NAME = NamingUtil.makeName(NESTED_CTX_STRING, CNC_STRING);
    public static final NameComponent[] CNC_RESOLVABLE_NAME = NamingUtil.makeName(CNC_RESOLVABLE_STRING);

    public static NameComponent[] makeName(String... names) {
        List<NameComponent> list = new ArrayList<NameComponent>(names.length);
        for (String name : names)
            list.add(new NameComponent(name, ""));
        return list.toArray(new NameComponent[names.length]);
    }

    public static String getNameServiceListing(NamingContext nc) throws Exception {
        SortedSet<String> entries = new TreeSet<String>();
        NamingUtil.addListingToSet(nc, "/", entries);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            for (String entry : entries)
                pw.println(entry);
            pw.flush();
            return sw.toString();
        } finally {
            pw.close();
        }
    }

    public static void addListingToSet(NamingContext nc, String prefix, SortedSet<String> entries) throws Exception {
        BindingListHolder blh = new BindingListHolder();
        BindingIteratorHolder bih = new BindingIteratorHolder();
        BindingHolder bh = new BindingHolder();
        entries.add(prefix);
        nc.list(0, blh, bih);
        while (bih.value.next_one(bh)) {
            String entry = bh.value.binding_name[0].id;
            if (bh.value.binding_type.value() == BindingType._nobject) {
                entries.add(prefix + entry);
            } else {
                addListingToSet(NamingContextHelper.narrow(nc.resolve(bh.value.binding_name)), prefix + entry + "/", entries);
            }
        }
    }
}
