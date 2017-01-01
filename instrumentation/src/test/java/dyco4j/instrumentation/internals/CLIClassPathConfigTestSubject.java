/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

public class CLIClassPathConfigTestSubject {
    public static void main(final String[] s) {
        Object j;

        // The following conditional triggers an exception in ASM that requires all referred types to be
        // included in the class path
        if (s.length < 3)
            j = new String[5][3];
        else
            j = new SomeClass[3][2];
        System.out.println(j);
    }

    private class SomeClass {
    }
}

