/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;

final class MemberNameIdData {
    private final File profileFile;
    private final PrintWriter writer;
    private final Map<String, String> fieldId2Name = new HashMap<>();
    private final Map<String, String> shortFieldName2Id = new HashMap<>();
    private final Map<String, String> methodId2Name = new HashMap<>();
    private final Map<String, String> shortMethodName2Id = new HashMap<>();

    MemberNameIdData() {
        try {
            final String fileName = "." + File.separator + "member_data.txt";
            this.profileFile = new File(fileName);
            if (this.profileFile.exists()) {
                load();
                final File f = new File(fileName + ".bak");
                FileUtils.moveFile(this.profileFile, f);
            }
            this.writer = new PrintWriter(new FileOutputStream(profileFile, false));
            this.writer.println((new Date()).toString());
        } catch (final IOException _ex) {
            throw new RuntimeException(_ex);
        }
    }

    void recordData(final TreeMap<String, String> id2Name) {
        id2Name.entrySet().forEach(_e -> this.writer.println(_e.getKey() + ":" + _e.getValue()));
    }

    void finishedLogging() {
        this.writer.flush();
        this.writer.close();
    }

    Map<String, String> getFieldId2Name() {
        return Collections.unmodifiableMap(this.fieldId2Name);
    }

    Map<String, String> getShortFieldName2Id() {
        return Collections.unmodifiableMap(this.shortFieldName2Id);
    }

    Map<String, String> getMethodId2Name() {
        return Collections.unmodifiableMap(this.methodId2Name);
    }

    Map<String, String> getShortMethodName2Id() {
        return Collections.unmodifiableMap(this.shortMethodName2Id);
    }

    private void load() throws IOException {
        final BufferedReader _tr = new BufferedReader(new FileReader(this.profileFile));

        if (_tr.ready())
            _tr.readLine(); // read the date string

        while (_tr.ready()) {
            final String _line = _tr.readLine();
            final String[] _tmp1 = _line.split(":");
            final String _id = _tmp1[0];
            final String _name = _tmp1[1];
            final String _shortName = Helper.createShortNameDesc(_name);
            if (_id.startsWith("f")) {
                assert !this.fieldId2Name.containsKey(_id);
                this.fieldId2Name.put(_id, _name);
                assert !this.shortFieldName2Id.containsKey(_shortName);
                this.shortFieldName2Id.put(_shortName, _id);
            } else if (_id.startsWith("m")) {
                assert !this.methodId2Name.containsKey(_id);
                this.methodId2Name.put(_id, _name);
                assert !this.shortMethodName2Id.containsKey(_id);
                this.shortMethodName2Id.put(_shortName, _id);
            } else {
                throw new RuntimeException("Could not process " + _line);
            }
        }

        _tr.close();
    }
}
