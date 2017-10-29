package blue.origami.transpiler.type;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OStrings;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoubleCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.StringCode;

public abstract class Ty implements TypeApi, OStrings {
	public final static String Mut = "$";

	// Core types
	public static final Ty tVoid = m(new VoidTy());
	public static final Ty tBool = m(new BoolTy());
	public static final Ty tInt = m(new IntTy());
	public static final Ty tFloat = m(new FloatTy());
	public static final Ty tString = m(new StringTy());
	//
	public static final Ty tOption = m(new OptionTy());
	public static final Ty tList = m(new SimpleTy("List", 1));
	public static final Ty tMList = m(new SimpleTy(Mut + "List", 1));
	public static final Ty tDict = m(new SimpleTy("Dict", 1));
	public static final Ty tMDict = m(new SimpleTy(Mut + "Dict", 1));
	public static final Ty tStream = m(new SimpleTy("Stream", 1));
	public static final Ty tMStream = m(new SimpleTy(Mut + "Stream", 1));
	// VarParam
	public static final Ty[] tVarParam = new Ty[10];
	static {
		for (int i = 0; i < tVarParam.length; i++) {
			tVarParam[i] = m(new VarParamTy(String.valueOf((char) ('a' + i))));
		}
	}

	public static final Ty tUntyped(String varname) {
		return new VarParamTy(Memo.NonStr + NameHint.shortName(varname));
	}

	// Hidden Type
	// public static final Ty tAny = m(new AnyTy());
	public static final Ty tByte = m(new SimpleTy("Byte"));
	public static final Ty tInt64 = m(new SimpleTy("Int64"));
	public static final Ty tFloat32 = m(new SimpleTy("Float32"));
	public static final Ty tChar = m(new SimpleTy("Char"));

	// public static final Ty tNULL = m(new SimpleTy("?"));
	public static final Ty tThis = m(new SimpleTy("_"));
	public static final Ty tAuto = m(new SimpleTy("auto"));

	static Ty m(Ty ty) {
		return (ty.isMemoed()) ? ty : Memo.memo(ty);
	}

	static Ty[] m(Ty[] ty) {
		return Arrays.stream(ty).map(t -> m(t)).toArray(Ty[]::new);
	}

	/* DynamicType */

	public static final DataTy tData() {
		return new DataTy();
	}

	public static final VarTy tUntyped() {
		return new VarTy("");
	}

	public static final VarTy tUntyped(AST s) {
		return new VarTy("");
	}

	/* Data */

	public static final Ty t(String name) {
		return Memo.t(name);
	}

	public static final Ty tGeneric(String base, Ty param) {
		return m(Memo.t(base).newGeneric(m(param)));
	}

	public static final Ty tGeneric(Ty base, Ty param) {
		return m(m(base).newGeneric(m(param)));
	}

	public Ty newGeneric(Ty m) {
		return this;
	}

	public static Ty tOption(Ty ty) {
		return tGeneric(tOption, ty);
	}

	public static final Ty tList(Ty ty) {
		return tGeneric(tList, ty);
	}

	public static final Ty tArray(Ty ty) {
		return tGeneric(tMList, ty);
	}

	public static final DataTy tRecord(String... names) {
		Arrays.sort(names);
		return (DataTy) m(new DataTy(false, names));
	}

	public static final DataTy tData(String... names) {
		Arrays.sort(names);
		return (DataTy) m(new DataTy(true, names));
	}

	/* FuncType */

	public static final FuncTy tFunc(Ty returnType, Ty... paramTypes) {
		return (FuncTy) m(new FuncTy(m(returnType), m(paramTypes)));
	}

	public static final Ty tTuple(Ty... ts) {
		Ty ty = m(new TupleTy(m(ts)));
		// System.out.println("********* " + ty + " memoed=" + ty.isMemoed());
		return ty;
	}

	public static Ty tTag(Ty inner, String... names) {
		if (inner instanceof TagTy) {
			TagTy tag = (TagTy) inner;
			inner = tag.getParamType();
			names = TagTy.joins(names, tag.names);
		}
		Arrays.sort(names);
		return m(new TagTy(inner, names));
	}

	//

	public final static boolean isUntyped(Ty t) {
		return t == null;
	}

	public Ty getParamType() {
		return null;
	}

	@Override
	public final boolean equals(Object t) {
		if (t instanceof Ty) {
			return this.toString().equals(t.toString());
			// return this.acceptTy(false, (Ty) t, VarLogger.Nop);
		}
		return false;
	}

	public boolean eq(Ty ty) {
		return this.acceptTy(false, ty, VarLogger.Nop);
	}

	public final boolean accept(Code code) {
		Ty codeTy = code.getType();
		return this == codeTy || this.acceptTy(true, codeTy, VarLogger.Update);
	}

	public abstract boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs);

	protected boolean acceptVarTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy.isVar()) {
			return (codeTy.acceptTy(false, this, logs));
		}
		return false;
	}

	// public abstract Ty staticTy();

	public static Predicate<Ty> IsVar = (t) -> t instanceof VarTy;
	public static Predicate<Ty> IsGeneric = (t) -> t instanceof VarParamTy;
	public static Predicate<Ty> IsVarParam = (t) -> (t instanceof VarParamTy || t instanceof VarTy);

	public abstract boolean hasSome(Predicate<Ty> f);

	public Ty dupVar(VarDomain dom) {
		return this;
	}

	public boolean isAmbigous() {
		return this.hasSome(Ty.IsVar) /* || this.isUnion() */;
	}

	public boolean isMutable() {
		return false;
	}

	public Ty toImmutable() {
		return this;
	}

	@Override
	public Ty base() {
		return this;
	}

	@Override
	public final String toString() {
		return OStrings.stringfy(this);
	}

	public abstract <C> C mapType(TypeMapper<C> codeType);

	/* Common */

	private int typeId = 0;

	public int typeId() {
		return this.typeId;
	}

	void typeId(int seq) {
		this.typeId = seq;
	}

	protected boolean isMemoed() {
		return this.typeId > 0;
	}

	public String keyMemo() {
		return this.toString();
	}

	/* Mutable */

	public static Ty[] map(Ty[] ts, Function<Ty, Ty> f) {
		Ty[] p = new Ty[ts.length];
		for (int i = 0; i < p.length; i++) {
			p[i] = f.apply(ts[i]);
		}
		return p;
	}

	public Ty memoed() {
		return this;
	}

}

interface TypeApi {

	public Ty base();

	public default boolean is(Ty ty) {
		return base() == ty;
	}

	public default boolean isVoid() {
		return base() == Ty.tVoid;
	}

	public default boolean isVar() {
		return base() instanceof VarTy;
	}

	public default boolean isAny() {
		return base() instanceof VarParamTy;
	}
	//
	// public default boolean isOption() {
	// return real() instanceof OptionTy;
	// }

	public default boolean isFunc() {
		return base() instanceof FuncTy;
	}

	public default boolean isTuple() {
		return base() instanceof TupleTy;
	}

	// public default boolean isUnion() {
	// return real() instanceof UnionTy;
	// }

	public default boolean isData() {
		return base() instanceof DataTy;
	}

	public default boolean isGeneric() {
		return base() instanceof GenericTy;
	}

	public default boolean isGeneric(Ty baseTy) {
		Ty ty = base();
		if (ty instanceof GenericTy) {
			return ((GenericTy) ty).getBaseType().base() == baseTy;
		}
		return false;
	}

	public default boolean isSpecific() {
		return !this.isVoid(); /* && !this.isUnion(); */
	}

	public default Code getDefaultValue() {
		return null;
	}

	public default boolean hasMutation() {
		return false;
	}

	public default void foundMutation() {
	}

	public default int costMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return CastCode.STUPID;
	}

	public default CodeMap findMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return null;
	}

	public default int costMapThisTo(Env env, Ty fromTy, Ty toTy) {
		return CastCode.STUPID;
	}

	public default CodeMap findMapThisTo(Env env, Ty fromTy, Ty toTy) {
		return null;
	}

}

class VoidTy extends SimpleTy {
	VoidTy() {
		super("()");
	}

	@Override
	public Code getDefaultValue() {
		return new MultiCode();
	}

	@Override
	public int costMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return CastCode.SAME;
	}

	@Override
	public CodeMap findMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return new CodeMap(CastCode.SAME | CodeMap.LazyFormat, "(void)", "voidcast", fromTy, Ty.tVoid);
	}

}

class BoolTy extends SimpleTy {
	BoolTy() {
		super("Bool");
	}

	@Override
	public Code getDefaultValue() {
		return new BoolCode(false);
	}

}

class IntTy extends SimpleTy {
	IntTy() {
		super("Int");
	}

	@Override
	public Code getDefaultValue() {
		return new IntCode(0);
	}
}

class FloatTy extends SimpleTy {
	FloatTy() {
		super("Float");
	}

	@Override
	public Code getDefaultValue() {
		return new DoubleCode(0);
	}
}

class StringTy extends SimpleTy {
	StringTy() {
		super("String");
	}

	@Override
	public Code getDefaultValue() {
		return new StringCode("");
	}
}

class UntypedTy extends SimpleTy {

	UntypedTy(String name) {
		super(name);
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		return true;
	}

}
