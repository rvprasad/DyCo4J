/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package sindu.jvm.instrumentation.entry

filenames = []
new File(".").eachFileRecurse({
    if (it.getName() ==~ /.*class$/ && it.getName() != ~/.*orig$/) {
        filenames << it.getCanonicalPath()
    }
})

println("Instrumenting ${filenames.size()} files")
sindu.jvm.instrumentation.entry.InstrumenterCLI.main(filenames as String[])
