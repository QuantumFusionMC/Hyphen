package dev.quantumfusion.hyphen.test.poly.enums;

import dev.quantumfusion.hyphen.scan.annotations.DataSubclasses;
import dev.quantumfusion.hyphen.test.poly.classes.c.CM1;
import dev.quantumfusion.hyphen.test.poly.classes.c.enums.EnumC;
import dev.quantumfusion.hyphen.test.poly.classes.c.enums.EnumCBoolean;
import dev.quantumfusion.hyphen.test.poly.classes.c.enums.EnumCSingleton;
import dev.quantumfusion.hyphen.util.TestThis;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static dev.quantumfusion.hyphen.util.TestSupplierUtil.cross;
import static dev.quantumfusion.hyphen.util.TestSupplierUtil.subClasses;

@TestThis
public class EnumEnumSubclassTest {
    @DataSubclasses({EnumC.class, EnumCBoolean.class, EnumCSingleton.class})
    public CM1 data;

    public EnumEnumSubclassTest(CM1 data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "EnumTest{" +
                "data=" + this.data +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        EnumEnumSubclassTest c0IntC1 = (EnumEnumSubclassTest) o;
        return Objects.equals(this.data, c0IntC1.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data);
    }

    public static Supplier<Stream<? extends EnumEnumSubclassTest>> generateEnumEnumSubclassTest() {
        return cross(subClasses(
                EnumC.generateEnumC(),
                EnumCBoolean.generateEnumCBoolean(),
                EnumCSingleton.generateEnumCSingleton()), EnumEnumSubclassTest::new);
    }
}
