package main;

public class Page {

	public String text;
	/** page number starts from 1, not 0! */
	public final long pageNum;
	
	public Page(String text, long pageNum) {
		this.text = text;
		this.pageNum = pageNum;
	}
}
