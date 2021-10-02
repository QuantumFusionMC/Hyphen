package dev.quantumfusion.hyphen.codegen;

import dev.quantumfusion.hyphen.Options;
import dev.quantumfusion.hyphen.codegen.method.MethodMetadata;
import dev.quantumfusion.hyphen.info.TypeInfo;
import dev.quantumfusion.hyphen.thr.ThrowEntry;
import dev.quantumfusion.hyphen.thr.ThrowHandler;
import dev.quantumfusion.hyphen.thr.exception.HyphenException;
import dev.quantumfusion.hyphen.util.GenUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.quantumfusion.hyphen.util.GenUtil.getMethodDesc;
import static dev.quantumfusion.hyphen.util.GenUtil.getVoidMethodDesc;
import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("WeakerAccess")
public class MethodHandler extends MethodVisitor implements AutoCloseable {
	// just for visual decompilation
	private static final boolean NAME_DEDUP = true;
	public final Class<?> returnClazz;
	private final CodegenHandler codegenHandler;
	private final Map<String, Integer> nameDedup = NAME_DEDUP ? new HashMap<>() : null;
	private final List<@Nullable Var> vars = new ArrayList<>();
	private final List<Label> scopeStarts = new ArrayList<>();
	private final boolean compactVariables;
	private int currentIndex = 0;

	// ================================== CREATE ==================================
	public MethodHandler(MethodVisitor mv, Class<?> returnClazz, CodegenHandler codegenHandler) {
		super(Opcodes.ASM9, mv);
		this.returnClazz = returnClazz == null ? Void.TYPE : returnClazz;
		this.codegenHandler = codegenHandler;
		this.pushScope();
		this.compactVariables = codegenHandler.options.get(Options.COMPACT_VARIABLES);
	}

	public static MethodHandler createVoid(CodegenHandler codegenHandler, int tag, String name, Class<?>... param) {
		final MethodVisitor mv = codegenHandler.cw.visitMethod(tag, name, getVoidMethodDesc(param), null, null);
		return new MethodHandler(mv, Void.TYPE, codegenHandler);
	}

	public static MethodHandler create(CodegenHandler codegenHandler, int tag, String name, @Nullable Class<?> returnClazz, Class<?>... param) {
		final MethodVisitor mv = codegenHandler.cw.visitMethod(tag, name, getMethodDesc(returnClazz, param), null, null);
		return new MethodHandler(mv, returnClazz, codegenHandler);
	}

	// ================================== CLAZZY ====================================
	public void typeInsn(int opcode, Class<?> type) {
		super.visitTypeInsn(opcode, Type.getInternalName(type));
	}

	public void getField(int opcode, Class<?> owner, String name, Class<?> clazz) {
		super.visitFieldInsn(opcode, Type.getInternalName(owner), name, Type.getDescriptor(clazz));
	}

	public void callMethod(int opcode, Class<?> owner, String name, boolean isInterface, @Nullable Class<?> returnClass, Class<?>... parameters) {
		super.visitMethodInsn(opcode, Type.getInternalName(owner), name, GenUtil.getMethodDesc(returnClass, parameters), isInterface);
	}

	public void callInstanceMethod(Class<?> owner, String name, @Nullable Class<?> returnClass, Class<?>... parameters) {
		this.callMethod(INVOKEVIRTUAL, owner, name, false, returnClass, parameters);
	}

	public void callSpecialMethod(Class<?> owner, String name, @Nullable Class<?> returnClass, Class<?>... parameters) {
		this.callMethod(INVOKESPECIAL, owner, name, false, returnClass, parameters);
	}

	public void callStaticMethod(Class<?> owner, String name, @Nullable Class<?> returnClass, Class<?>... parameters) {
		this.callMethod(INVOKESTATIC, owner, name, false, returnClass, parameters);
	}

	public void callInterfaceMethod(Class<?> owner, String name, @Nullable Class<?> returnClass, Class<?>... parameters) {
		this.callMethod(INVOKEINTERFACE, owner, name, true, returnClass, parameters);
	}

	public void callInternalStaticMethod(String name, @Nullable Class<?> returnClass, Class<?>... parameters) {
		this.visitMethodInsn(INVOKESTATIC, this.codegenHandler.name, name, GenUtil.getMethodDesc(returnClass, parameters), false);
	}

	public void callHyphenMethod(MethodMode mode, TypeInfo typeInfo) {
		final CodegenHandler.MethodInfo methodData = codegenHandler.getMethodData(mode, typeInfo);
		this.callInternalStaticMethod(methodData.name(), methodData.returnClass(), methodData.param());
	}

	public void callHyphenMethod(MethodMode mode, MethodMetadata methodMetadata) {
		this.callHyphenMethod(mode, methodMetadata.getInfo());
	}

	public void createMultiArray(Class<?> descriptor, int numDimensions) {
		super.visitMultiANewArrayInsn(Type.getDescriptor(descriptor), numDimensions);
	}

	// ================================== THROW ===================================

	public void fatalStart(int entries) {
		this.visitLdcInsn(entries);
		this.typeInsn(ANEWARRAY, ThrowEntry.class);
		this.visitInsn(DUP);
	}

	public void fatalEntry(String name, int pos) {
		this.visitLdcInsn(name);
		this.visitInsn(SWAP);
		this.callStaticMethod(ThrowEntry.class, "of", ThrowEntry.class, String.class, Object.class);
		this.visitLdcInsn(pos);
		this.visitInsn(SWAP);
		this.visitInsn(AASTORE);
		this.visitInsn(DUP);
	}

	public void fatalInvoke(Class<? extends Throwable> exception, String reason) {
		this.visitInsn(POP);
		this.cast(ThrowHandler.Throwable[].class);
		this.typeInsn(NEW, exception);
		this.visitInsn(DUP);
		this.visitLdcInsn(reason);
		callSpecialMethod(exception, "<init>", null, String.class);
		this.visitInsn(SWAP);
		this.callStaticMethod(ThrowHandler.class, "fatal", HyphenException.class, Throwable.class, ThrowHandler.Throwable[].class);
		this.visitInsn(ATHROW);
	}

	// =================================== UTIL ===================================
	public void returnOp() {
		this.visitInsn(Type.getType(returnClazz).getOpcode(IRETURN));
	}

	public void cast(Class<?> clazz) {
		this.visitTypeInsn(CHECKCAST, Type.getInternalName(clazz));
	}

	// ==================================== IO ====================================
	public void callIOGet(Class<?> clazz) {
		String desc;
		if (clazz.isArray()) desc = getMethodDesc(clazz, int.class);
		else desc = getMethodDesc(clazz);

		invokeIO(desc, "get" + getSuffix(clazz));
	}

	public void callIOPut(Class<?> clazz) {
		invokeIO(getVoidMethodDesc(clazz), "put" + getSuffix(clazz));
	}

	public Class<?> getIOClazz() {
		return this.codegenHandler.getIOMode().ioClass;
	}

	private void invokeIO(String desc, String name) {
		this.visitMethodInsn(INVOKEVIRTUAL, this.codegenHandler.getIOMode().internalName, name, desc, false);
	}

	private String getSuffix(Class<?> clazz) {
		String simpleName;
		if (clazz.isArray())
			simpleName = clazz.getComponentType().getSimpleName() + "Array";
		else
			simpleName = clazz.getSimpleName();

		return simpleName.substring(0, 1).toUpperCase() + simpleName.substring(1);
	}

	// ================================= CLOSABLE =================================
	@Override
	public void close() {
		this.popScope();
		this.mv.visitMaxs(0, 0);
		this.mv.visitEnd();
	}

	public Var getVarOrNull(String name) {
		for (Var var : this.vars) {
			if (var != null && var.name.equals(name)) {
				return var;
			}
		}
		return null;
	}

	public Label createScope() {
		this.vars.add(null);
		Label start = new Label();
		this.scopeStarts.add(start);
		return start;
	}

	public Label pushScope() {
		Label start = this.createScope();
		this.visitLabel(start);
		return start;
	}

	public void popScope() {
		this.popScope(new Label());
	}

	public void popScope(Label stop) {
		Label start = this.scopeStarts.remove(this.scopeStarts.size() - 1);
		this.visitLabel(stop);

		for (int i = this.vars.size() - 1; i >= 0; i--) {
			Var var = this.vars.remove(i);
			if (var == null) return;
			this.currentIndex--;
			this.visitLocalVariable(var.internalName, var.type.getDescriptor(), null, start, stop, var.index);
		}
	}

	public Var getVar(String name) {
		Var var = this.getVarOrNull(name);
		if (var != null)
			return var;
		throw new RuntimeException("Variable " + name + " does not exist\n " + this.vars);
	}

	public Var createOrGetVar(String name, Class<?> clazz) {
		Var var = this.getVarOrNull(name);
		if (var != null) return var;

		return this.createVarInternal(name, Type.getType(clazz));
	}

	private Var createVarInternal(String name, Type type) {
		String internalName;
		if (NAME_DEDUP) {
			if (compactVariables) {
				internalName = "_";
			} else {
				if (this.nameDedup.containsKey(name)) {
					internalName = name + this.nameDedup.merge(name, 1, Integer::sum);
				} else {
					this.nameDedup.put(name, 0);
					internalName = name;
				}
			}

		} else internalName = name;

		Var var = new Var(name, this.currentIndex++, type, internalName);

		this.vars.add(var);
		return var;
	}

	public Var createVar(String name, Class<?> clazz) {
		return this.createVar(name, Type.getType(clazz));
	}

	public boolean existsInScope(String name) {
		for (int i = this.vars.size() - 1; i >= 0; i--) {
			Var var = this.vars.get(i);
			if (var == null) return false;
			if (var.name.equals(name)) return true;
		}
		return false;
	}

	public Var createVar(String name, Type type) {
		if (this.existsInScope(name))
			throw new RuntimeException("Variable " + name + " already exists in scope\n " + this.vars);
		return this.createVarInternal(name, type);
	}

	// =================================== FOR ===================================
	public final class ForI implements AutoCloseable {
		private final Label start = new Label();
		private final Label end = new Label();
		public final MethodHandler.Var length = MethodHandler.this.createVar("length", int.class);
		public final MethodHandler.Var i = MethodHandler.this.createVar("i", int.class);

		public ForI() {
			length.store();
		}

		public ForI start() {
			MethodHandler.this.visitInsn(ICONST_0);
			i.store();
			MethodHandler.this.visitLabel(start);
			i.load();
			length.load();
			MethodHandler.this.visitJumpInsn(IF_ICMPGE, end);
			return this;
		}

		@Override
		public void close() {
			i.iinc(1); // i++
			MethodHandler.this.visitJumpInsn(GOTO, start);
			MethodHandler.this.visitLabel(end);
		}
	}

	public ForI createForWithLength() {
		return new ForI();
	}

	// ================================ VARHANDLER ================================
	public final class Var {
		private final String name;
		private final int index;
		private final Type type;
		private final String internalName;

		private Var(String name, int index, Type type, String internalName) {
			this.name = name;
			this.index = index;
			this.type = type;
			this.internalName = internalName;
		}

		public void load() {
			this.inst(ILOAD);
		}

		public void store() {
			this.inst(ISTORE);
		}

		public void loadFromArray() {
			this.inst(IALOAD);
		}

		public void storeToArray() {
			this.inst(IASTORE);
		}

		public void inst(int op) {
			MethodHandler.this.visitIntInsn(this.type.getOpcode(op), this.index);
		}

		public void iinc(int value) {
			MethodHandler.this.visitIincInsn(this.index, value);
		}
	}

}
