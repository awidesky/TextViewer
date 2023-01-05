package main;

public class Page {

	public static final Page EOF = new Page(null, -2) {
		@Override
		public boolean equals(Object other) {
			return other != null && other == this; 
		}
	};
	
	public String text;
	/** page number starts from 1, not 0! */
	public final long pageNum;
	
	public Page(String text, long pageNum) {
		this.text = text;
		this.pageNum = pageNum;
	}
}
