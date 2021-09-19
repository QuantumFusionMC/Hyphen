package dev.quantumfusion.hyphen.scan.poly;


import dev.quantumfusion.hyphen.scan.poly.classes.C1;

public class IntC1 extends C1<Integer> {
	public IntC1(Integer integer) {
		super(integer);
	}

	// FIXME: tracking issue #10
	public IntC1(Object integer) {
		super((Integer) integer);
	}

}