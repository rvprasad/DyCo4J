/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

public class CLIInitializerInstrumentationTestSubject {
    public static void main(final String[] s) {
        new X("Hi") {
            @SuppressWarnings("unused")
            void dummy() {
                boolean i = s.length != 0;
            }
        };
    }

    static class X extends Y {
        X(String s) {
            super(new Y(3));
        }

        @SuppressWarnings("unused")
        X(X s) {
            super(new Y(3));
        }
    }

    static class Y {
        Y(Object o) {
        }

        Y(int i) {
        }
    }
}

