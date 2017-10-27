// package blue.origami.transpiler.type;
//
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
//
// import blue.origami.common.OArrays;
// import blue.origami.common.ODebug;
// import blue.origami.common.OStrings;
// import blue.origami.transpiler.AST;
// import blue.origami.transpiler.CodeMap;
// import blue.origami.transpiler.Env;
// import blue.origami.transpiler.Transpiler;
// import blue.origami.transpiler.code.ApplyCode;
// import blue.origami.transpiler.code.CastCode;
// import blue.origami.transpiler.code.Code;
// import blue.origami.transpiler.code.FuncCode;
// import blue.origami.transpiler.code.VarNameCode;
//
// public class GenericTy extends Ty {
// protected final String id;
// protected final Ty baseType;
// protected final Ty paramType;
//
// GenericTy(String id, Ty baseType, Ty paramType) {
// this.id = id;
// this.baseType = baseType;
// this.paramType = paramType;
// }
//
// @Override
// public boolean isNonMemo() {
// return OArrays.testSomeTrue(t -> t.isNonMemo(), this.getParamTypes()) ||
// this.getBaseType().isNonMemo();
// }
//
// public Ty getBaseType() {
// return this.baseType;
// }
//
// public int getParamSize() {
// return this.paramTypes.length;
// }
//
// public Ty[] getParamTypes() {
// return this.paramTypes;
// }
//
// public static String stringfy(Ty returnType, Ty... paramTypes) {
// StringBuilder sb = new StringBuilder();
// stringfy(sb, returnType, paramTypes);
// return sb.toString();
// }
//
// public static void stringfy(StringBuilder sb, Ty returnType, Ty...
// paramTypes) {
// sb.append(this.baseType);
//
// if (paramTypes.length != 1) {
// sb.append("(");
// OStrings.joins(sb, paramTypes, ",");
// sb.append(")");
// } else {
// OStrings.joins(sb, paramTypes, ",", (ty) -> group(ty));
// }
// sb.append("->");
// }
//
// private static String group(Ty ty) {
// return (ty.isFunc() || ty.isUnion()) ? "(" + ty + ")" : ty.toString();
// }
//
// @Override
// public void strOut(StringBuilder sb) {
// if (this.id != null) {
// sb.append(this.id);
// } else {
// stringfy(sb, this.getReturnType(), this.getParamTypes());
// }
// }
//
// @Override
// public boolean hasVar() {
// return this.baseType.hasVar() || OArrays.testSomeTrue(t -> t.hasVar(),
// this.getParamTypes());
// }
//
// @Override
// public Ty dupVar(VarDomain dom) {
// if (this.hasVar()) {
// Ty[] p = Arrays.stream(this.paramTypes).map(x ->
// x.dupVar(dom)).toArray(Ty[]::new);
// Ty ret = this.baseType.dupVar(dom);
// return Ty.tFunc(ret, p);
// }
// return this;
// }
//
// @Override
// public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
// if (codeTy.isFunc()) {
// FuncTy funcTy = (FuncTy) codeTy.real();
// if (funcTy.getParamSize() != this.getParamSize()) {
// return false;
// }
// for (int i = 0; i < this.getParamSize(); i++) {
// if (!this.paramTypes[i].acceptTy(false, funcTy.paramTypes[i], logs)) {
// return false;
// }
// }
// return this.baseType.acceptTy(false, funcTy.returnType, logs);
// }
// return this.acceptVarTy(sub, codeTy, logs);
// }
//
// @Override
// public Ty finalTy() {
// if (this.id == null) {
// Ty[] p = Arrays.stream(this.paramTypes).map(x ->
// x.finalTy()).toArray(Ty[]::new);
// Ty ret = this.baseType.finalTy();
// return Ty.tFunc(ret, p);
// }
// return this;
// }
//
// @Override
// public <C> C mapType(TypeMapper<C> codeType) {
// return codeType.forFuncType(this);
// }
//
// @Override
// public int costMapTo(Env env, Ty ty) {
// if (ty.isFunc()) {
// FuncTy toTy = (FuncTy) ty.real();
// if (this.getParamSize() == toTy.getParamSize()) {
// VarLogger logger = new VarLogger();
// Ty[] fromTys = this.getParamTypes();
// Ty[] toTys = toTy.getParamTypes();
// int cost = 0;
// for (int i = 0; i < fromTys.length; i++) {
// cost = Math.max(cost, env.mapCost(env, toTys[i], fromTys[i], logger));
// if (cost >= CastCode.STUPID) {
// logger.abort();
// return CastCode.STUPID;
// }
// }
// cost = Math.max(cost, env.mapCost(env, this.getReturnType(),
// toTy.getReturnType(), logger));
// logger.abort();
// return cost;
// }
// }
// return CastCode.STUPID;
// }
//
// @Override
// public CodeMap findMapTo(Env env, Ty ty) {
// if (ty.isFunc()) {
// FuncTy toTy = (FuncTy) ty.real();
// int cost = this.costMapTo(env, ty);
// if (cost < CastCode.STUPID) {
// return this.genFuncConv(env, this, toTy).setMapCost(cost);
// }
// }
// return null;
// }
//
// public static String mapKey(Ty fromTy, Ty toTy) {
// StringBuilder sb = new StringBuilder();
// if (fromTy.isFunc()) {
// sb.append("(");
// fromTy.strOut(sb);
// sb.append(")");
// } else {
// fromTy.strOut(sb);
// }
// sb.append("->");
// if (toTy.isFunc()) {
// sb.append("(");
// toTy.strOut(sb);
// sb.append(")");
// } else {
// toTy.strOut(sb);
// }
// return sb.toString();
// }
//
// public CodeMap genFuncConv(Env env, FuncTy fromTy, FuncTy toTy) {
// ODebug.stackTrace("generating funcmap %s => %s", fromTy, toTy);
// Transpiler tr = env.getTranspiler();
// AST[] names = AST.getNames("f");
// Ty[] params = { fromTy };
//
// Ty[] fromTypes = fromTy.getParamTypes();
// Ty[] toTypes = toTy.getParamTypes();
// AST[] fnames = AST.getNames(OArrays.names(toTypes.length));
// List<Code> l = new ArrayList<>();
// l.add(new VarNameCode("f"));
// for (int c = 0; c < toTy.getParamSize(); c++) {
// Code p = new VarNameCode(String.valueOf((char) ('a' + c)));
// l.add(new CastCode(fromTypes[c], p)); // Any->Int & (Int->Int? &
// // Int?->Any?) ==> c->d
// // ODebug.trace("[%d] %s->%s %s", c, toTypes[c], fromTypes[c],
// // env.findTypeMap(env, toTypes[c], fromTypes[c]));
// }
// // ODebug.trace("[ret] %s->%s", fromTy.getReturnType(),
// // toTy.getReturnType());
// Code body = new CastCode(toTy.getReturnType(), new ApplyCode(l));
// FuncCode func = new FuncCode(fnames, toTypes, toTy.getReturnType(), body);
// return tr.defineFunction(mapKey(fromTy, toTy), names, params, toTy, func);
// }
//
// }