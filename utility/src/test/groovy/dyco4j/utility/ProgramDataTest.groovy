/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.utility

import org.junit.jupiter.api.Test

import java.nio.file.Files
import java.nio.file.Paths

class ProgramDataTest {
    private static ProgramData createProgramData() {
        final _tmp1 = new ProgramData()
        _tmp1.class2SuperClass['a'] = 'b'
        _tmp1.fieldId2Name['98'] = 'x1'
        _tmp1.shortFieldName2Id['f2'] = '23'
        _tmp1.methodId2Name['23'] = 'm1'
        _tmp1.shortMethodName2Id['m2'] = '908'
        return _tmp1
    }

    @Test
    void testClassAddition() {
        final _programData = createProgramData()
        _programData.addClass2SuperClassMapping('c', 'd')
        assert _programData.getImmutableCopyOfClass2SuperClass() == ['a': 'b', 'c': 'd']
    }

    @Test
    void testFieldAddition() {
        final _shortField = 'shortField'
        final _longField = 'longField'

        final _programData = createProgramData()
        final Optional<String> _fieldId = _programData.addNewField(_shortField, _longField, 'i')
        assert _fieldId.present

        final _shortFieldName2Id = _programData.getImmutableCopyOfShortFieldName2Id()
        assert _shortFieldName2Id[_shortField] == _fieldId.get()
        assert _shortFieldName2Id.size() == 2

        final _fieldId2Name = _programData.getImmutableCopyOfFieldId2Name()
        assert _fieldId2Name[_fieldId.get()] == _longField
    }

    @Test
    void tesMethodAddition() {
        final _shortMethod = 'shortMethod'
        final _longMethod = 'longMethod'

        final _programData = createProgramData()
        final Optional<String> _methodId = _programData.addNewMethod(_shortMethod, _longMethod, 'i')
        assert _methodId.present

        final _shortMethodName2Id = _programData.getImmutableCopyOfShortMethodName2Id()
        assert _shortMethodName2Id[_shortMethod] == _methodId.get()
        assert _shortMethodName2Id.size() == 2

        final _methodId2Name= _programData.getImmutableCopyOfMethodId2Name()
        assert _methodId2Name[_methodId.get()] == _longMethod
    }

    @Test
    void writeAndReadNonEmptyDataObject() {
        final _programData = createProgramData()
        final _dataFile = File.createTempFile("pre", ".json").toPath()
        ProgramData.saveData(_programData, _dataFile)
        final _tmp3 = ProgramData.loadData(_dataFile)
        Files.delete(_dataFile)
        assert _tmp3 == _programData
    }

    @Test
    void writeAndReadEmptyDataObject() {
        final _dataFile = File.createTempFile("pre", ".json").toPath()
        final _programData = new ProgramData()
        ProgramData.saveData(_programData, _dataFile)
        final _tmp3 = ProgramData.loadData(_dataFile)
        Files.delete(_dataFile)
        assert _tmp3 == _programData
    }

    @Test
    void loadFromEmptyFile() {
        final _dataFile = File.createTempFile("pre", ".json").toPath()
        final _programData = ProgramData.loadData(_dataFile)
        Files.delete(_dataFile)
        assert _programData == null
    }

    @Test
    void loadFromNonExistentFile() {
        final _programData = ProgramData.loadData(Paths.get("This cannot exists!"))
        assert _programData == (new ProgramData())
    }

    @Test
    void writeWhenDataFileIsPresent() {
        final _dataFile = File.createTempFile("pre", ".json").toPath()
        final _bakFile = Paths.get(_dataFile.toString() + ".bak")

        final _programData = new ProgramData()
        _programData.class2SuperClass['a'] = 'b'
        ProgramData.saveData(_programData, _dataFile)
        assert Files.exists(_bakFile) && _bakFile.size() == 0

        final _programDataCopy = ProgramData.loadData(_dataFile)
        _programDataCopy.fieldId2Name['98'] = 'x1'
        ProgramData.saveData(_programDataCopy, _dataFile)
        assert Files.exists(_bakFile)
        assert ProgramData.loadData(_bakFile) == _programData
    }
}
