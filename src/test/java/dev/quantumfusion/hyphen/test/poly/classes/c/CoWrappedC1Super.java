package dev.quantumfusion.hyphen.test.poly.classes.c;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static dev.quantumfusion.hyphen.util.TestSupplierUtil.cross;

public class CoWrappedC1Super<A, CA extends C1<? super A>> extends C1<CA> {
    public A selfA;

    public CoWrappedC1Super(CA ca, A selfA) {
        super(ca);
        this.selfA = selfA;
    }

    public CoWrappedC1Super(Object ca, A selfA) {
        super((CA) ca);
        this.selfA = selfA;
    }

    public static <A, CA extends C1<? super A>> Supplier<? extends Stream<? extends CoWrappedC1Super<A, CA>>> generateCoWrappedC1Super(
            Supplier<? extends Stream<? extends A>> aSupplier,
            Supplier<? extends Stream<? extends CA>> caSupplier
    ) {
        return cross(caSupplier, aSupplier, CoWrappedC1Super::new);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        CoWrappedC1Super<?, ?> that = (CoWrappedC1Super<?, ?>) o;
        return Objects.equals(this.selfA, that.selfA);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.selfA);
    }

    @Override
    public String toString() {
        return "CoWrappedC1Super{" +
                "a=" + this.a +
                ", selfA=" + this.selfA +
                '}';
    }
}
