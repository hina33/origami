package blue.origami.transpiler.rule;

import java.math.BigDecimal;
import java.math.BigInteger;

import blue.origami.common.ODebug;
import blue.origami.common.TLog;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import origami.nez2.ParseTree;

public abstract class NumberExpr extends LoggerRule implements ParseRule {
	public final Class<?> baseType;

	public NumberExpr() {
		this(double.class);
	}

	NumberExpr(Class<?> baseType) {
		this.baseType = baseType;
	}

	protected abstract Code newCode(Number value);

	@Override
	public Code apply(Env env, ParseTree t) {
		TLog log = null;
		String text = t.asString().replace("_", "");
		int radix = 10;
		Class<?> base = this.baseType;
		if (text.endsWith("L") || text.endsWith("l")) {
			text = text.substring(0, text.length() - 1);
		}
		if (text.startsWith("0b") || text.startsWith("0B")) {
			text = text.substring(2);
			radix = 2;
			base = int.class;
		} else if (text.startsWith("0x") || text.startsWith("0X")) {
			text = text.substring(2);
			radix = 16;
			base = int.class;
		} else if (text.startsWith("0") && !text.startsWith("0.")) {
			radix = 8;
			base = int.class;
		}
		Number value = null;
		if (base == int.class) {
			try {
				value = Integer.parseInt(text, radix);
			} catch (NumberFormatException e) {
				ODebug.trace("radix=%d", radix);
				log = this.reportWarning(log, env.s(t), TFmt.wrong_number_format_YY1_by_YY2, text, e);
				value = 0;
			}
		} else if (base == double.class) {
			try {
				value = Double.parseDouble(text);
			} catch (NumberFormatException e) {
				log = this.reportWarning(log, env.s(t), TFmt.wrong_number_format_YY1_by_YY2, text, e);
				value = 0.0;
			}
		} else if (base == long.class) {
			try {
				value = Long.parseLong(text, radix);
			} catch (NumberFormatException e) {
				log = this.reportWarning(log, env.s(t), TFmt.wrong_number_format_YY1_by_YY2, text, e);
				value = 0L;
			}
		} else if (base == float.class) {
			try {
				value = Float.parseFloat(text);
			} catch (NumberFormatException e) {
				log = this.reportWarning(log, env.s(t), TFmt.wrong_number_format_YY1_by_YY2, text, e);
				value = 0.0f;
			}
		} else if (base == BigInteger.class) {
			try {
				value = new BigInteger(text, radix);
			} catch (NumberFormatException e2) {
				log = this.reportWarning(log, env.s(t), TFmt.wrong_number_format_YY1_by_YY2, text, e2);
				value = BigInteger.ZERO;
			}
		} else {
			try {
				value = new BigDecimal(text);
			} catch (NumberFormatException e2) {
				log = this.reportWarning(log, env.s(t), TFmt.wrong_number_format_YY1_by_YY2, text, e2);
				value = BigDecimal.ZERO;
			}
		}
		return this.log(log, this.newCode(value));
	}

}
