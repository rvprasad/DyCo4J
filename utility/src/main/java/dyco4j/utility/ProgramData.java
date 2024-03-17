/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 *
 */

package dyco4j.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ProgramData {
    // INFO: Refer to ClassNameHelper for info about format of names
    final Map<String, String> fieldId2Name = new HashMap<>();
    final Map<String, String> shortFieldName2Id = new HashMap<>();
    final Map<String, String> methodId2Name = new HashMap<>();
    final Map<String, String> shortMethodName2Id = new HashMap<>();
    final Map<String, String> class2SuperClass = new HashMap<>();

    // Returns null if dataFile is empty
    public static ProgramData loadData(final Path dataFile) throws IOException {
        if (Files.exists(dataFile)) {
            try (final Reader _rdr = new FileReader(dataFile.toFile())) {
                return new Gson().fromJson(_rdr, ProgramData.class);
            }
        } else
            return new ProgramData();
    }

    public static void saveData(final ProgramData staticData, final Path dataFile) throws IOException {
        if (Files.exists(dataFile))
            Files.move(dataFile, Paths.get(dataFile + ".bak"), StandardCopyOption.REPLACE_EXISTING);

        try (final Writer _wtr = new FileWriter(dataFile.toFile())) {
            new GsonBuilder().setPrettyPrinting().create().toJson(staticData, _wtr);
        }
    }

    public Map<String, String> getViewOfShortFieldName2Id() {
        return Collections.unmodifiableMap(shortFieldName2Id);
    }

    public Map<String, String> getViewOfFieldId2Name() {
        return Collections.unmodifiableMap(fieldId2Name);
    }

    public Map<String, String> getViewOfShortMethodName2Id() {
        return Collections.unmodifiableMap(shortMethodName2Id);
    }

    public Map<String, String> getViewOfMethodId2Name() {
        return Collections.unmodifiableMap(methodId2Name);
    }

    public Map<String, String> getViewOfClass2SuperClass() {
        return Collections.unmodifiableMap(class2SuperClass);
    }

    public String addClass2SuperClassMapping(final String className, final String superClassName) {
        return class2SuperClass.put(className, superClassName);
    }

    public Optional<String> addNewField(final String shortName, final String name, final String prefix) {
        return addNewName(shortName, name, prefix, shortFieldName2Id, fieldId2Name);
    }

    public Optional<String> addNewMethod(final String shortName, final String name, final String prefix) {
        return addNewName(shortName, name, prefix, shortMethodName2Id, methodId2Name);
    }

    private static Optional<String> addNewName(final String shortName, final String name, final String prefix,
                                      final Map<String, String> shortName2Id, final Map<String, String> id2Name) {
        if (shortName2Id.containsKey(shortName))
            return Optional.empty();

        final String _id = prefix + shortName2Id.size();
        shortName2Id.put(shortName, _id);
        id2Name.put(_id, name);
        return Optional.of(_id);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ProgramData _that))
            return false;

        if (!fieldId2Name.equals(_that.fieldId2Name) ||
                !shortFieldName2Id.equals(_that.shortFieldName2Id) ||
                !methodId2Name.equals(_that.methodId2Name) ||
                !shortMethodName2Id.equals(_that.shortMethodName2Id))
            return false;

        return class2SuperClass.equals(_that.class2SuperClass);
    }

    @Override
    public int hashCode() {
        int result = fieldId2Name.hashCode();
        result = 31 * result + shortFieldName2Id.hashCode();
        result = 31 * result + methodId2Name.hashCode();
        result = 31 * result + shortMethodName2Id.hashCode();
        result = 31 * result + class2SuperClass.hashCode();
        return result;
    }
}
