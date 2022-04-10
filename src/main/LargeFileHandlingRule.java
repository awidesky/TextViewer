package main;

import java.io.BufferedReader;
import java.io.IOException;

public class LargeFileHandlingRule {

	private long largerThan;
	private int limit;
	private BufferedReader br = null;
	
	public LargeFileHandlingRule(long sizeLimit , int charsPerPage) {
		largerThan = sizeLimit;
		limit = charsPerPage;
	}
	
	public long getFileSizeLimit() { 
		return largerThan;
	}
	
	public String readOnce(BufferedReader br) throws IOException {

		if(br != null) this.br = br;
		
		char[] arr = new char[limit];
		int read = this.br.read(arr);
		if(read != -1) return null;
		return new String(arr, 0, read);
		
	}
	
}
