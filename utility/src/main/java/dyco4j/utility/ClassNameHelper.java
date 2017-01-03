/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.utility;

import java.util.Optional;

/*
 * INFO
 *
 * The format of the name descriptors is <FQN>:<type desc>:(S|I|?):(+|-|?) where
 *   - S, I, and ? denote static member, instance member, and don't know in case of invokedynamic, respectively.
 *   - +, -, and ? denote published member, unpublished member, and don't know in case of invokedynamic, respectively.
 *   - in case of invokedynamic, owner is set to "<dynamic>" in FQN.
 *
 * The short format is identical to the above format but without :(S|I|?):(+|-|?) part.
 */
@SuppressWarnings("unused")
public class ClassNameHelper {
    public static final String DYNAMIC_METHOD_OWNER = "<dynamic>";

    public static String createJavaName(final String name, final String owner) {
        return owner.replace("/", ".") + "." + name;
    }

    public static String createNameDesc(final String name, final Optional<String> owner, final String desc,
                                        final Optional<Boolean> isStatic, final Optional<Boolean> isPublished) {
        final String _isStatic = isStatic.map(v -> v ? "S" : "I").orElse("?");
        final String _isPublished = isPublished.map(v -> v ? "+" : "-").orElse("?");
        return owner.orElse(DYNAMIC_METHOD_OWNER) + "/" + name + ":" + desc + ":" + _isStatic + ":" + _isPublished;
    }

    public static String createShortNameDesc(final String name, final Optional<String> owner, final String desc) {
        return owner.orElse(DYNAMIC_METHOD_OWNER) + "/" + name + ":" + desc;
    }

    public static String createShortNameDesc(final String nameDesc) {
        final String[] _tmp = nameDesc.split(":");
        return _tmp[0] + ":" + _tmp[1];
    }
}
