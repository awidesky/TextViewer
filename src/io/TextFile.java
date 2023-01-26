package io;

import java.io.File;
import java.nio.charset.Charset;

public class TextFile {

	public final File file;
	public final Charset encoding;
	
	public TextFile(File file, Charset encoding) {
		this.file = file;
		this.encoding = encoding;
	}
	
	@Override
	public String toString() {
		return file.getAbsolutePath() + " as " + encoding.displayName();
	}
		
}
