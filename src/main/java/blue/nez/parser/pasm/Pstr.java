package blue.nez.parser.pasm;

public final class Pstr extends PAsmInst {
	public final byte[] utf8;

	public Pstr(byte[] byteSeq, PAsmInst next) {
		super(next);
		this.utf8 = byteSeq;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return (matchBytes(px, this.utf8)) ? this.next : raiseFail(px);
	}

}