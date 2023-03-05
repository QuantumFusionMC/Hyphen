package dev.quantumfusion.hyphen.test.poly.enums;

import dev.quantumfusion.hyphen.scan.annotations.DataNullable;
import dev.quantumfusion.hyphen.scan.annotations.DataSubclasses;
import dev.quantumfusion.hyphen.test.poly.classes.c.C0;
import dev.quantumfusion.hyphen.test.poly.classes.c.CM1;
import dev.quantumfusion.hyphen.test.poly.classes.c.IntC1;
import dev.quantumfusion.hyphen.test.poly.classes.c.enums.EnumC;
import dev.quantumfusion.hyphen.test.poly.classes.c.enums.EnumCBoolean;
import dev.quantumfusion.hyphen.test.poly.classes.c.enums.EnumCSingleton;
import dev.quantumfusion.hyphen.util.TestThis;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static dev.quantumfusion.hyphen.util.TestSupplierUtil.cross;
import static dev.quantumfusion.hyphen.util.TestSupplierUtil.nullableSubClasses;

@TestThis
public class EnumAnyNullableSubclassTest {
    @DataSubclasses({EnumC.class, EnumCBoolean.class, EnumCSingleton.class, C0.class, IntC1.class})
    @DataNullable
    public CM1 data;

    public EnumAnyNullableSubclassTest(CM1 data) {
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
        EnumAnyNullableSubclassTest c0IntC1 = (EnumAnyNullableSubclassTest) o;
        return Objects.equals(this.data, c0IntC1.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data);
    }

    public static Supplier<Stream<? extends EnumAnyNullableSubclassTest>> generateEnumAnyNullableSubclassTest() {
        return cross(nullableSubClasses(
                EnumC.generateEnumC(),
                EnumCBoolean.generateEnumCBoolean(),
                EnumCSingleton.generateEnumCSingleton(),
                C0.generateC0(),
                IntC1.generateIntC1()
        ), EnumAnyNullableSubclassTest::new);
    }
}
