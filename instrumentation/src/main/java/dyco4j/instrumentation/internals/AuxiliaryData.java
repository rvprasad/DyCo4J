/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

final class AuxiliaryData {
    final Map<String, String> fieldId2Name = new HashMap<>();
    final Map<String, String> shortFieldName2Id = new HashMap<>();
    final Map<String, String> methodId2Name = new HashMap<>();
    final Map<String, String> shortMethodName2Id = new HashMap<>();
    final Map<String, String> class2superClass = new HashMap<>();

    static AuxiliaryData loadData(final Path dataFile) throws IOException {
        if (Files.exists(dataFile)) {
            try (final Reader _rdr = new FileReader(dataFile.toFile())) {
                return new Gson().fromJson(_rdr, AuxiliaryData.class);
            }
        } else
            return new AuxiliaryData();
    }

    static void saveData(final AuxiliaryData staticData, final Path dataFile) throws IOException {
        if (Files.exists(dataFile))
            Files.move(dataFile, Paths.get(dataFile.toString() + ".bak"), StandardCopyOption.REPLACE_EXISTING);

        try (final Writer _wtr = new FileWriter(dataFile.toFile())) {
            new GsonBuilder().setPrettyPrinting().create().toJson(staticData, _wtr);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AuxiliaryData))
            return false;

        final AuxiliaryData _that = (AuxiliaryData) o;

        if (!fieldId2Name.equals(_that.fieldId2Name))
            return false;
        if (!shortFieldName2Id.equals(_that.shortFieldName2Id))
            return false;
        if (!methodId2Name.equals(_that.methodId2Name))
            return false;
        if (!shortMethodName2Id.equals(_that.shortMethodName2Id))
            return false;
        return class2superClass.equals(_that.class2superClass);

    }

    @Override
    public int hashCode() {
        int result = fieldId2Name.hashCode();
        result = 31 * result + shortFieldName2Id.hashCode();
        result = 31 * result + methodId2Name.hashCode();
        result = 31 * result + shortMethodName2Id.hashCode();
        result = 31 * result + class2superClass.hashCode();
        return result;
    }
}
