package blue.origami.transpiler.code;

import java.util.List;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;

public final class FuncRefCode extends CommonCode {
	private String name;
	private CodeMap mapped;

	public FuncRefCode(String name, CodeMap tp) {
		super(tp.getFuncType());
		this.name = name;
		this.mapped = tp;
	}

	public String getName() {
		return this.name;
	}

	public CodeMap getMapped() {
		return this.mapped;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (ret.isFunc()) {
			FuncTy funcTy = (FuncTy) ret.devar();
			List<CodeMap> l = env.findCodeMaps(this.name, funcTy.getParamSize());
			if (l.size() == 0) {
				return this.asUnfound(env, l, funcTy);
			}
			if (l.size() == 1) {
				return this.asMatched(env, l.get(0).generate(env, funcTy.getParamTypes()), ret);
			}
			CodeMap selected = CodeMap.select(env, l, funcTy.getReturnType(), funcTy.getParamTypes(), 0);
			if (selected == null) {
				return this.asMismatched(env, l, funcTy);
			}
			return this.asMatched(env, selected.generate(env, funcTy.getParamTypes()), ret);
		}
		if (this.isUntyped()) {
			this.mapped.used(env);
			this.setType(this.mapped.getFuncType());
		}
		return super.castType(env, ret);
	}

	@Override
	public boolean showError(Env env) {
		if (this.mapped.isAbstract() || this.mapped.isGeneric()) {
			env.reportError(this.getSource(), TFmt.abstract_function_YY1__YY2, this.name, this.mapped.getFuncType());
			return true;
		}
		return false;
	}

	private Code asMatched(Env env, CodeMap selected, Ty ret) {
		this.mapped = selected;
		this.setType(selected.getFuncType());
		return this.castType(env, ret);
	}

	private Code asUnfound(Env env, List<CodeMap> l, FuncTy funcTy) {
		env.findList(this.name, CodeMap.class, l, (tt) -> !tt.isExpired());
		throw new ErrorCode(this, TFmt.undefined_SSS, this.name, "", ExprCode.msgHint(env, l));
	}

	private Code asMismatched(Env env, List<CodeMap> l, FuncTy funcTy) {
		throw new ErrorCode(this, TFmt.mismatched_SSS, this.name, "", ExprCode.msgHint(env, l));
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushFuncRef(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, "funcref " + this.name);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.TypeAnnotation(this.getType(), () -> {
			sh.Name(this.name);
		});
	}

}