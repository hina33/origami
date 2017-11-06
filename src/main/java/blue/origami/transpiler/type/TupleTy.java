package blue.origami.transpiler.type;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OArrays;
import blue.origami.common.OStrings;

public class TupleTy extends Ty {
	protected final Ty[] paramTypes;

	TupleTy(Ty... paramTypes) {
		this.paramTypes = paramTypes;
		assert (this.paramTypes.length > 1) : "tuple size " + this.paramTypes.length;
	}

	public int getParamSize() {
		return this.paramTypes.length;
	}

	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.joins(sb, this.paramTypes, "*");
	}

	@Override
	public void typeKey(StringBuilder sb) {
		Ty[] ts = this.paramTypes;
		OStrings.forEach(sb, ts.length, "*", (n) -> ts[n].typeKey(sb));
	}

	@Override
	public String keyFrom() {
		return "Tuple" + this.paramTypes.length;
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return OArrays.testSome(this.getParamTypes(), t -> t.hasSome(f));
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		if (this.hasSome(Ty.IsVarParam)) {
			return Ty.tTuple(Ty.map(this.paramTypes, x -> x.dupVar(dom)));
		}
		return this;
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		Ty self = f.apply(this);
		if (self != this) {
			return self;
		}
		Ty[] ts = Ty.map(this.paramTypes, x -> x.map(f));
		if (Arrays.equals(ts, this.paramTypes)) {
			return this;
		}
		return Ty.tTuple(ts);
	}

	@Override
	public boolean match(boolean sub, Ty codeTy, TypeMatcher logs) {
		if (codeTy.isTuple()) {
			TupleTy tupleTy = (TupleTy) codeTy.base();
			if (tupleTy.getParamSize() != this.getParamSize()) {
				return false;
			}
			for (int i = 0; i < this.getParamSize(); i++) {
				if (!this.paramTypes[i].match(false, tupleTy.paramTypes[i], logs)) {
					return false;
				}
			}
			return true;
		}
		return this.matchVar(sub, codeTy, logs);
	}

	@Override
	public Ty memoed() {
		if (!this.isMemoed()) {
			return Ty.tTuple(Ty.map(this.paramTypes, t -> t.memoed()));
		}
		return this;
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.forTupleType(this);
	}

}
