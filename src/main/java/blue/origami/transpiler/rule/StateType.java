// package blue.origami.transpiler.rule;
//
// import origami.nez2.ParseTree;
// import blue.origami.transpiler.Env;
// import blue.origami.transpiler.code.Code;
// import blue.origami.transpiler.code.TypeCode;
// import blue.origami.transpiler.type.Ty;
//
// public class StateType implements ParseRule, Symbols {
// @Override
// public Code apply(Env env, AST t) {
// String name = Ty.parseStateName(t.get(_base));
// Ty ty = env.parseType(env, t.get(_param), null);
// return new TypeCode(Ty.tState(name, ty));
// }
// }