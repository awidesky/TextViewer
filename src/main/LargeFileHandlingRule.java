package main;

import java.io.BufferedReader;
import java.io.IOException;

public class LargeFileHandlingRule {

	private long largerThan;
	/** Limit of <code>char</code>s or lines per page. */
	private int limit;
	/** <code>true</code> if <code>limit</code> is number of chars to read, <code>false</code> if <code>limit</code> is number of lines to read. */
	private boolean charIsUnit;
	private BufferedReader br = null;
	
	/**
	 * 
	 * @param sizeLimit				The file will be paged when it's size is larger than this(in byte).
	 * @param charIsUnit			Put <code>true</code> if <code>limit</code> is number of chars to read, <code>false</code> if <code>limit</code> is number of lines to read.
	 * @param charsOrLinesPerPage	Limit of <code>char</code>s or lines per page.
	 * 
	 * */
	public LargeFileHandlingRule(long sizeLimit, boolean charIsUnit, int charsOrLinesPerPage) {
		largerThan = sizeLimit;
		this.charIsUnit = charIsUnit;
		limit = charsOrLinesPerPage;
	}
	
	public long getFileSizeLimit() { 
		return largerThan;
	}
	
	public String readOnce(BufferedReader br) throws IOException {

		if(br != null) this.br = br;
		
		if(charIsUnit) {
			char[] arr = new char[limit];
			int read = this.br.read(arr);
			if(read != -1) return null;
			return String.valueOf(arr, 0, read);
		} else {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < limit; i++) {
				String s = br.readLine();
				if(s == null) {
					if(i == 0) {
						return null;
					} else {
						return sb.toString();
					}
				}
				sb.append(s);
			}
			return sb.toString();
		}
		
	}
	
}
