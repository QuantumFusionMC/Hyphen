package dev.quantumfusion.hyphen.test.simple;

import dev.quantumfusion.hyphen.FailTest;
import dev.quantumfusion.hyphen.util.TestThis;

@FailTest(/*AccessException.class*/)
@TestThis
public class ConstructorIsNotPublic {
    public int prim;

    ConstructorIsNotPublic(int prim) {
        this.prim = prim;
    }
}
