/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.utility;

@SuppressWarnings("unused")
public class ClassNameHelper {
    public static String createJavaName(final String name, final String owner) {
        return owner.replace("/", ".") + "." + name;
    }

    public static String createNameDesc(final String name, final String owner, final String desc, final boolean isStatic,
                                 final boolean isPublished) {
        return owner + "/" + name + ":" + desc + ":" + (isStatic ? "S" : "I") + ":" + (isPublished ? "+" : "-");
    }

    public static String createShortNameDesc(final String name, final String owner, final String desc) {
        return owner + "/" + name + ":" + desc;
    }

    public static String createShortNameDesc(final String nameDesc) {
        final String[] tmp = nameDesc.split(":");
        return tmp[0] + ":" + tmp[1];
    }
}
