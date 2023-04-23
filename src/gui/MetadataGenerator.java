package gui;

import java.io.File;
import java.util.function.Consumer;

import main.Main;

public class MetadataGenerator {

	private static Consumer<Metadata> metadataConsumer = null;
	
	private static File file = null;
	private static boolean paged = false;
	private static String charset = null;
	private static boolean edited = false;
	private static boolean loading = false;
	private static long pageNum = -1L;
	
	public static void metadataConsumer(Consumer<Metadata> metadataConsumer) {
		MetadataGenerator.metadataConsumer = metadataConsumer;
	}
	
	public static void loading(boolean b) {
		MetadataGenerator.loading = b;
		generate();
	}

	public static void edited(boolean b) {
		MetadataGenerator.edited = b;
		generate();
	}
	
	public static void fileClosed() {
		file = null;
	}

	public static void reset(File file, boolean paged, String charset, boolean edited, boolean loading, long pageNum) {
		MetadataGenerator.file = file;
		MetadataGenerator.paged = paged;
		MetadataGenerator.charset = charset;
		MetadataGenerator.edited = edited;
		MetadataGenerator.loading = loading;
		MetadataGenerator.pageNum = pageNum;
		
		generate();
	}
	
	public static void generate() {
		String fileExplainPart;
		if(file == null) fileExplainPart = " - \"Untitled\"";
		else fileExplainPart = " - " + file.getName() + (paged ? " (page #" + pageNum + ")" : "");
		metadataConsumer.accept(new Metadata((edited ? "*" : "") + Main.VERSION + fileExplainPart + (loading ? " (loading...)" : ""),
				file.getAbsolutePath(), Main.formatFileSize(file.length()), charset, Main.setting.lineSeparator.getExplain()));
	}

	public static void pageNum(long l) {
		MetadataGenerator.pageNum = l;
		generate();
	}

	public static class Metadata {
		public final String title;
		public final String path;
		public final String fileSize;
		public final String charset;
		public final String newline;
		
		public Metadata(String title, String path, String fileSize, String charset, String newline) {
			this.title = title;
			this.path = path;
			this.fileSize = fileSize;
			this.charset = charset;
			this.newline = newline;
		}
	}
	
}
