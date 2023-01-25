package main;


/**
 * Read-only immutable object that represents a Page.
 * */
public class Page {
	
	public static final Page EOF = new Page(null, -2, true) {
		@Override
		public boolean equals(Object other) {
			return other != null && other == this; 
		}
	};

	public final String text;
	/** page number starts from 1, not 0! */
	public final long pageNum;
	/** Is it guaranteed that this is the last page of the file? */
	public final boolean isLastPage;
	/** is trailing newline of the page removed? */
	public final boolean lastNewlineRemoved;
	
	public Page(String text, long pageNum, boolean isLastPage) {
		this(text, pageNum, isLastPage, false);
	}

	public Page(String text, long pageNum, boolean isLastPage, boolean lastNewlineRemoved) {
		this.text = text;
		this.pageNum = pageNum;
		this.isLastPage = isLastPage;
		this.lastNewlineRemoved = lastNewlineRemoved;
	}
}
