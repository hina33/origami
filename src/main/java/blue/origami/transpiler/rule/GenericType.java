package blue.origami.transpiler.rule;

import blue.origami.common.ODebug;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.TypeCode;
import blue.origami.transpiler.type.Ty;

public class GenericType implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, AST t) {
		Ty ty = env.parseType(env, t.get(_base), null);
		String name = ty.toString();
		AST params = t.get(_param);
		Ty[] p = new Ty[params.size()];
		for (int i = 0; i < p.length; i++) {
			p[i] = env.parseType(env, params.get(i), null);
		}
		switch (name) {
		case "Option":
		case "Maybe":
			return new TypeCode(Ty.tOption(p[0]));
		case "List":
			return new TypeCode(Ty.tList(p[0]));
		default:
			ODebug.TODO();
			// if (Ty.isDefinedMonad(name)) {
			// return new TypeCode(Ty.tMonad(name, p[0]));
			// }
		}
		return new ErrorCode(t.get(_base), TFmt.undefined_type__YY1, name);
	}
}