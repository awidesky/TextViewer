package io.github.awidesky.textViewer.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PlainTextWriter extends TextWriter {


	protected BufferedWriter bw;
	
	public PlainTextWriter(TextFile writeTo) throws IOException {
		writeFile = writeTo;
		bw = new BufferedWriter(new FileWriter(writeFile.file, writeFile.encoding));
	}
	
	public void writeString(String str) throws IOException {
		bw.write(replaceNewLine(str, writeFile.lineSep.getStr()));
	}

	@Override
	public void close() throws IOException {
		if(bw != null) bw.close();
	}
	
}
