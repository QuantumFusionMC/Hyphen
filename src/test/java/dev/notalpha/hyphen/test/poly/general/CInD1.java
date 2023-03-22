package dev.notalpha.hyphen.test.poly.general;

import dev.notalpha.hyphen.scan.annotations.DataSubclasses;
import dev.notalpha.hyphen.test.poly.classes.c.C1;
import dev.notalpha.hyphen.test.poly.classes.c.C2;
import dev.notalpha.hyphen.test.poly.classes.d.D1;
import dev.notalpha.hyphen.util.TestSupplierUtil;
import dev.notalpha.hyphen.util.TestThis;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

@TestThis
public class CInD1 {
    public D1<@DataSubclasses({C1.class, C2.class}) C1<Integer>> data;

    public CInD1(D1<C1<Integer>> data) {
        this.data = data;
    }

    public static Supplier<Stream<? extends CInD1>> generateCInD1() {
        var sub = TestSupplierUtil.subClasses(
                C1.generateC1(TestSupplierUtil.INTEGERS),
                C2.generateC2(TestSupplierUtil.INTEGERS)
        );

        return TestSupplierUtil.cross(D1.generateD1(sub), CInD1::new);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        CInD1 cInD1 = (CInD1) o;
        return Objects.equals(this.data, cInD1.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data);
    }

    @Override
    public String toString() {
        return "CInD1{" +
                "data=" + this.data +
                '}';
    }
}
