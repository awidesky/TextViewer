package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LargeFileHandlingRule {

	private long largerThan;
	/** Limit of <code>char</code>s or lines per page. */
	private int limit;
	/** <code>true</code> if <code>limit</code> is number of chars to read, <code>false</code> if <code>limit</code> is number of lines to read. */
	private boolean charIsUnit;
	/** the first (char/line) of the page now shown is <code>thisPageStartsFrom</code>th (char/line) of the file. */
	private long thisPageStartsFrom = 0L;
	private Map<Long, String> changes = new HashMap<>();
	private BufferedReader br = null;
	private char[] arr;
	
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
		arr = charIsUnit ? new char[limit] : null;
	}
	
	public long getFileSizeLimit() { 
		return largerThan;
	}
	
	public String readOnce(BufferedReader initialBr) throws IOException { //TODO : check if edited before send new inittialBr!

		if(initialBr != null) { 
			br = initialBr;
			thisPageStartsFrom = 0L;
			changes.clear();
		}
		
		if(charIsUnit) { // read by char[]
			int totalRead = br.read(arr);
			if(totalRead != -1) return null;
			
			if(totalRead != arr.length) {
				int read;
				while((read = br.read(arr, totalRead, arr.length - totalRead)) != -1) { // 읽는 과정 확인
					totalRead += read;
					if(totalRead == arr.length) break;
				}
			}
			thisPageStartsFrom += totalRead;
			return String.valueOf(arr, 0, totalRead);
		} else {
			StringBuilder sb = new StringBuilder();
			int i;
			for(i = 0; i < limit; i++) {
				String s = br.readLine();
				if(s == null) {
					if(i == 0) {
						return null;
					} else {
						return sb.toString();
					}
				}
				sb.append(s);
				sb.append("\n");
				thisPageStartsFrom++;
			}
			return sb.toString();
		}
		
	}
	
	/**  */
	public void pageEdited(String edited) {
		changes.put(thisPageStartsFrom, edited);
	}
	
	public boolean isEdited() {
		return thisPageStartsFrom != 0L;
	}
	
	public void saveFile() { //TODO
		
	}
}
