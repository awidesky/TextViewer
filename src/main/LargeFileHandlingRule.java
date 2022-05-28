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
	private Map<Long, String> changes = new HashMap<>();
	private char[] arr;
	
	private Reading reading = new Reading();
	
	
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
	
	/**
	 * Read one page from the file.
	 * If <code>initialBr</code> is <code>null</code>, use previously used <code>BufferedReader</code> to read continuously.
	 * */
	public String readOnce(BufferedReader initialBr) throws IOException { //TODO : check if edited before send new inittialBr!

		if(initialBr != null) { 
			reading.br = initialBr;
			reading.thisPageStartsFrom = 0L;
			changes.clear();
		}
		
		if(charIsUnit) { // read by char[]
			int totalRead = reading.br.read(arr);
			if(totalRead != -1) return null;
			
			if(totalRead != arr.length) {
				int read;
				while((read = reading.br.read(arr, totalRead, arr.length - totalRead)) != -1) {
					totalRead += read;
					if(totalRead == arr.length) break;
				}
			}
			reading.thisPageStartsFrom += totalRead;
			return String.valueOf(arr, 0, totalRead);
		} else {
			StringBuilder sb = new StringBuilder();
			int i;
			for(i = 0; i < limit; i++) {
				String s = reading.br.readLine();
				if(s == null) {
					if(i == 0) {
						return null;
					} else {
						return sb.toString();
					}
				}
				sb.append(s);
				sb.append("\n");
				reading.thisPageStartsFrom++;
			}
			return sb.toString();
		}
		
	}
	
	/**  */
	public void pageEdited(String edited) {
		changes.put(reading.thisPageStartsFrom, edited);
	}
	
	public boolean isEdited() {
		return reading.thisPageStartsFrom != 0L;
	}
	
	
	/**
	 * Get one page of now viewing(or editing) file.
	 * If the file is edited, return the edited page, else read file once.
	 * 
	 * */
	public String getPageOnce() {
		
		
		return "";
	}
	
	
	
	private class Reading {
		
		public BufferedReader br = null;
		/** the first (char/line) of the page now shown is <code>thisPageStartsFrom</code>th (char/line) of the file. */
		public long thisPageStartsFrom = 0L;
		
	}
	
}
