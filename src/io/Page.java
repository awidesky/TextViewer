package io;


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
	public static final Page ERR = new Page(null, -2, true) {
		@Override
		public boolean equals(Object other) {
			return other != null && other == this; 
		}
	};
	
	/** text of this page */
	public final String text;
	/** metadata of this page */
	public final Metadata metadata;
	
	public Page(String text, long pageNum, boolean isLastPage) {
		this(text, pageNum, isLastPage, false);
	}
	
	public Page(String text, Metadata nowPageMetadata) {
		this(text, nowPageMetadata.pageNum, nowPageMetadata.isLastPage, nowPageMetadata.lastNewlineRemoved);
	}

	public Page(String text, long pageNum, boolean isLastPage, boolean lastNewlineRemoved) {
		this.text = text;
		this.metadata = new Metadata(pageNum, isLastPage, lastNewlineRemoved);
	}
	

	public long pageNum() {
		return metadata.pageNum;
	}
	public boolean isLastPage() {
		return metadata.isLastPage;
	}
	public boolean lastNewlineRemoved() {
		return metadata.lastNewlineRemoved;
	}
	
	public class Metadata {
		/** page number starts from 1, not 0! */
		public final long pageNum;
		/** Is it guaranteed that this is the last page of the file? */
		public final boolean isLastPage;
		/** is trailing newline of the page removed? */
		public final boolean lastNewlineRemoved;
		
		public Metadata(long pageNum, boolean isLastPage, boolean lastNewlineRemoved) {
			this.pageNum = pageNum;
			this.isLastPage = isLastPage;
			this.lastNewlineRemoved = lastNewlineRemoved;
		}
	}
}
