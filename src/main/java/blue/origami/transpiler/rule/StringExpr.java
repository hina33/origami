package blue.origami.transpiler.rule;

import blue.origami.common.OStringUtils;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.StringCode;

public class StringExpr implements ParseRule {

	@Override
	public Code apply(Env env, AST t) {
		return new StringCode(OStringUtils.unquoteString(t.getString()));
	}

}