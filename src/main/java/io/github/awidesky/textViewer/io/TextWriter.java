package io.github.awidesky.textViewer.io;

import java.io.IOException;

public abstract class TextWriter implements AutoCloseable {
	
	/** A file to write at */
	protected TextFile writeFile;

	public abstract void writeString(String str) throws IOException;

	public void writePage(Page p) throws IOException {
		writeString(p.text);
	}

	protected String replaceNewLine(String text, String lineSep) { return text.replaceAll("\\R", lineSep); }
	
	
	public static TextWriter getTextWriter(TextFile tf) throws IOException {
		return tf.isEncrypted ? new EncryptedTextWriter(tf) : new PlainTextWriter(tf);
	}
	
	public abstract void close() throws IOException;
	
}
