package dev.notalpha.hyphen.test.simple;

import java.util.ArrayList;
import java.util.Objects;

public class DefTypeFollowTest {
    public final Test test;

    public DefTypeFollowTest(Test test) {
        this.test = test;
    }

    // TODO: implement List/Map subclasses
	/*
	public static Supplier<Stream<? extends DefTypeFollowTest>> generateDefTypeFollowTest() {
		return cross(array(INTEGERS, 98542, 32, Integer.class), arr -> {
					Test t = new Test();
					t.addAll(Arrays.asList(arr));
					return new DefTypeFollowTest(t);
				}
		);
	}*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        DefTypeFollowTest that = (DefTypeFollowTest) o;
        return Objects.equals(this.test, that.test);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.test);
    }

    @Override
    public String toString() {
        return "DefTypeFollowTest{" +
                "test=" + this.test +
                '}';
    }

    public static class Test extends ArrayList<Integer> {

    }
}
