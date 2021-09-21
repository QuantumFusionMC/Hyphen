package dev.quantumfusion.hyphen;

import dev.quantumfusion.hyphen.thr.NotYetImplementedException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.fail;

public class TestUtil {

	public static Iterator<DynamicNode> testPackage(String path) {

		try {
			String packageName = "dev.quantumfusion.hyphen." + path;

			var packagePath = Path.of(ClassLoader.getSystemClassLoader().getResource(packageName.replaceAll("[.]", "/")).toURI());


			List<List<DynamicNode>> nodes = new ArrayList<>(List.of(new ArrayList<>()));

			Files.walkFileTree(packagePath, new FileVisitor<>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					if (dir.endsWith("classes")) {
						// skip classes subfolders
						return FileVisitResult.SKIP_SUBTREE;
					}

					nodes.add(new ArrayList<>());
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (file.getFileName().toString().endsWith(".class")) {
						nodes.get(nodes.size() - 1).add(test(TestUtil.getClass(packagePath.relativize(file), packageName)));
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
					int size = nodes.size();
					nodes.get(size - 2).add(DynamicContainer.dynamicContainer(dir.getFileName().toString(), nodes.get(size - 1)));
					nodes.remove(size - 1);
					return FileVisitResult.CONTINUE;
				}
			});
			return nodes.get(0).iterator();
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		throw new RuntimeException();
	}

	public static DynamicNode test(Class<?> clazz) {
		assert clazz != null;
		FailTest failTest = clazz.getDeclaredAnnotation(FailTest.class);
		Executable executable;
		if (failTest != null) {
			executable = () -> {
				Class<? extends Throwable> value = failTest.value();
				try {
					SerializerFactory.createDebug(clazz).build();
				} catch (Throwable throwable) {
					if (throwable.getClass().equals(value)) {
						System.err.println("Got expected error: ");
						throwable.printStackTrace();
						Assumptions.assumeTrue(value != NotYetImplementedException.class, "Ignoring NYI feature");
						return;
					}
					fail("Expected a different exception: " + value.getSimpleName(), throwable);
				}

				if (value == Throwable.class) {
					fail("Forcefully failed test");
				} else {
					fail("Expected test to fail");
				}
			};
		} else {
			executable = () -> SerializerFactory.createDebug(clazz).build();
		}

		return DynamicTest.dynamicTest(clazz.getSimpleName(), URI.create("class:" + clazz.getName()), executable);
	}


	private static Class<?> getClass(Path className, String packageName) {
		try {
			String fileName = className.getFileName().toString();
			StringJoiner sj = new StringJoiner(".");

			sj.add(packageName);

			Path parent = className.getParent();
			if (parent != null) {
				for (Path path : parent) {
					sj.add(path.toString());
				}
			}

			sj.add(fileName.substring(0, fileName.lastIndexOf('.')));

			return Class.forName(sj.toString());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private static Class<?> getClass(String className, String packageName) {
		try {
			return Class.forName(packageName + "."
					+ className.substring(0, className.lastIndexOf('.')));
		} catch (ClassNotFoundException e) {
			// handle the exception
		}
		return null;
	}
}
