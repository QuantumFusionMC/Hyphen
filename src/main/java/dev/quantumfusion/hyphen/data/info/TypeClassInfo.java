package dev.quantumfusion.hyphen.data.info;

import dev.quantumfusion.hyphen.ScanHandler;
import dev.quantumfusion.hyphen.data.metadata.SerializerMetadata;
import dev.quantumfusion.hyphen.thr.ClassScanException;
import dev.quantumfusion.hyphen.thr.ThrowHandler;

import java.lang.annotation.Annotation;
import java.util.Map;

import static dev.quantumfusion.hyphen.thr.ThrowEntry.of;

public class TypeClassInfo extends TypeInfo {
	public final String typeName;
	public final Class<?> type;
	public final TypeInfo actual;

	public TypeClassInfo(Class<?> clazz, Map<Class<Annotation>, Annotation> annotations, String typeName, Class<?> type, TypeInfo actual) {
		super(clazz, annotations);
		this.typeName = typeName;
		this.type = type;
		this.actual = actual;
	}


	public static TypeClassInfo create(TypeInfo source, Class<?> clazz, Map<Class<Annotation>, Annotation> annotations, String typeName, Class<?> type, TypeInfo actual) {
		if (actual == ScanHandler.UNKNOWN_INFO)
			throw ThrowHandler.fatal(ClassScanException::new, "Type could not be identified",
					of("Source Class", source.clazz.getName()),
					of("Error Class", clazz));
		return new TypeClassInfo(clazz, annotations, typeName, type, actual);
	}

	@Override
	public String toFancyString() {
		return this.actual.toFancyString();
	}

	@Override
	public SerializerMetadata createMetadata(ScanHandler factory) {
		return actual.createMetadata(factory);
	}

	@Override
	public TypeInfo copy() {
		return new TypeClassInfo(this.clazz, this.annotations, this.typeName, this.type, this.actual);
	}

	@Override
	public Class<?> getRawClass() {
		return type;
	}
}