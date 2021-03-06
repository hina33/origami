/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package blue.origami.common;

import java.util.ArrayList;

public interface SourcePosition {
	public OSource getSource();

	public long getSourcePosition();

	// Utils

	public static SourcePosition UnknownPosition = new UnknownSourcePosition();

	public default boolean isUnknownPosition() {
		return this == UnknownPosition;
	}

	static class UnknownSourcePosition implements SourcePosition {

		@Override
		public OSource getSource() {
			return new UnknownSource();
		}

		@Override
		public long getSourcePosition() {
			return 0;
		}
	}

	static class UnknownSource implements OSource {

		@Override
		public String getResourceName() {
			return "(unknown source)";
		}

		@Override
		public long length() {
			return 0;
		}

		@Override
		public int byteAt(long pos) {
			return 0;
		}

		@Override
		public boolean eof(long pos) {
			return true;
		}

		@Override
		public boolean match(long pos, byte[] text) {
			return false;
		}

		@Override
		public String subString(long startIndex, long endIndex) {
			return "";
		}

		@Override
		public byte[] subBytes(long startIndex, long endIndex) {
			return new byte[0];
		}

		@Override
		public OSource subSource(long startIndex, long endIndex) {
			return this;
		}

		@Override
		public long linenum(long pos) {
			return 0;
		}

		@Override
		public int column(long pos) {
			return 0;
		}

	}

	public static String getLineString(OSource s, long pos) {
		long start = pos - 1;
		for (; start >= 0; start--) {
			if (s.byteAt(start) == '\n') {
				break;
			}
		}
		long end = start;
		for (; s.byteAt(end) != 0; end++) {
			if (s.byteAt(end) == '\n') {
				break;
			}
		}
		return s.subString(start, end);
	}

	public static void appendFormatMessage(StringBuilder sb, SourcePosition s, String mtype, OFormat format,
			Object... args) {
		appendFileLine(sb, s.getSource(), s.getSourcePosition(), mtype);
		OStrings.appendFormat(sb, format, args);
		if (!s.isUnknownPosition()) {
			sb.append("\n");
			extractTextAround(sb, s.getSource(), (int) s.getSourcePosition());
		}
	}

	static void appendFileLine(StringBuilder sb, OSource s, long pos, String mtype) {
		String file = extractFileName(s.getResourceName());
		sb.append("(");
		sb.append(file);
		sb.append(":");
		sb.append(s.linenum(pos));
		sb.append("+");
		sb.append(s.column(pos));
		sb.append(") [");
		sb.append(mtype);
		sb.append("] ");
	}

	public static String formatMessage(SourcePosition s, String mtype, OFormat fmt, Object... args) {
		StringBuilder sb = new StringBuilder();
		appendFormatMessage(sb, s, mtype, fmt, args);
		return sb.toString();
	}

	public static String formatErrorMessage(SourcePosition s, OFormat fmt, Object... args) {
		return formatMessage(s, fmt.error(), fmt, args);
	}

	public static String formatWarningMessage(SourcePosition s, OFormat fmt, Object... args) {
		return formatMessage(s, fmt.warning(), fmt, args);
	}

	public static String formatNoticeMessage(SourcePosition s, OFormat fmt, Object... args) {
		return formatMessage(s, fmt.notice(), fmt, args);
	}

	static int getch(OSource s, int pos) {
		if (pos >= 0 && pos < s.length()) {
			return s.byteAt(pos);
		}
		return '\n';
	}

	public static void extractTextAround(StringBuilder sb, OSource s, int pos) {
		String quote = "";
		int startIndex = pos - 1;
		while (true) {
			int ch = getch(s, startIndex);
			if (ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			if (pos - startIndex > 72 && ch < 128) {
				quote = " ...";
				break;
			}
			startIndex = startIndex - 1;
		}
		int endIndex = pos;
		while (true) {
			int ch = getch(s, endIndex);
			if (ch == '\n') {
				break;
			}
			if (endIndex - startIndex > 72 && ch < 128) {
				break;
			}
			endIndex = endIndex + 1;
		}
		sb.append(quote);
		sb.append(s.subString(startIndex, endIndex));
		sb.append("\n");
		sb.append(quote);
		for (int i = startIndex; i <= endIndex; i++) {
			if (i == pos) {
				sb.append(OConsole.bold("^"));
				break;
			}
			sb.append(" ");
		}
	}

	public static String getTextAround2(OSource s, long pos, String delim) {
		int ch = 0;
		if (pos < 0) {
			pos = 0;
		}
		while ((s.eof(pos) || s.byteAt(pos) == 0) && pos > 0) {
			pos -= 1;
		}
		long startIndex = pos;
		while (startIndex > 0) {
			ch = s.byteAt(startIndex);
			if (ch == '\n' && pos - startIndex > 0) {
				startIndex = startIndex + 1;
				break;
			}
			if (pos - startIndex > 60 && ch < 128) {
				break;
			}
			startIndex = startIndex - 1;
		}
		long endIndex = pos + 1;
		if (endIndex < s.length()) {
			while ((ch = s.byteAt(endIndex)) != 0 /* this.EOF() */) {
				if (ch == '\n' || endIndex - startIndex > 78 && ch < 128) {
					break;
				}
				endIndex = endIndex + 1;
			}
		} else {
			endIndex = s.length();
		}

		ArrayList<Byte> source = new ArrayList<>();
		// ByteBuffer source = new ByteBuffer();
		// ByteBuilder source = new StringBuilder();
		StringBuilder marker = new StringBuilder();
		for (long i = startIndex; i < endIndex; i++) {
			ch = s.byteAt(i);
			if (ch == '\n') {
				// source.append("\\n");
				source.add((byte) '\\');
				source.add((byte) '\n');
				if (i == pos) {
					marker.append("^^");
				} else {
					marker.append("\\n");
				}
			} else if (ch == '\t') {
				// source.append(" ");
				source.add((byte) ' ');
				source.add((byte) ' ');
				source.add((byte) ' ');
				source.add((byte) ' ');
				if (i == pos) {
					marker.append("^^^^");
				} else {
					marker.append("    ");
				}
			} else {
				// source.append((char) ch);
				source.add((byte) ch);
				if (i == pos) {
					marker.append("^");
				} else {
					marker.append(" ");
				}
			}
		}
		byte[] b = new byte[source.size()];
		for (int i = 0; i < b.length; i++) {
			b[i] = source.get(i);
		}
		return delim + OStringUtils.newString(b) + delim + marker.toString();
	}

	public static String extractFileName(String path) {
		int loc = path.lastIndexOf('/');
		if (loc > 0) {
			return path.substring(loc + 1);
		}
		loc = path.lastIndexOf('\\');
		if (loc > 0) {
			return path.substring(loc + 1);
		}
		return path;
	}

	public static String extractFileBaseName(String path) {
		String f = extractFileName(path);
		int loc = f.lastIndexOf('.');
		if (loc > 0) {
			return f.substring(0, loc);
		}
		return f;
	}

	public static String extractFileExtension(String path) {
		int loc = path.lastIndexOf('.');
		if (loc > 0) {
			return path.substring(loc + 1);
		}
		return path;
	}

	public static String extractFilePath(String path) {
		int loc = path.lastIndexOf('/');
		if (loc > 0) {
			return path.substring(0, loc);
		}
		loc = path.lastIndexOf('\\');
		if (loc > 0) {
			return path.substring(0, loc);
		}
		return path;
	}

	public static String changeFileExtension(String path, String ext) {
		int loc = path.lastIndexOf('.');
		if (loc > 0) {
			return path.substring(0, loc + 1) + ext;
		}
		return path + "." + ext;
	}

	public static SourcePosition newInstance(OSource s, long pos) {
		class SimpleSourcePosition implements SourcePosition {
			private final OSource s;
			private final long pos;

			SimpleSourcePosition(OSource s, long pos) {
				this.s = s;
				this.pos = pos;
			}

			@Override
			public OSource getSource() {
				return this.s;
			}

			@Override
			public long getSourcePosition() {
				return this.pos;
			}
		}
		return new SimpleSourcePosition(s, pos);
	}
}
