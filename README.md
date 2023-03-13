## TextViewer
- `TextViewer` can open&edit file regardless of the size, and save it to different character sets.
- File that is larger than `Single-paged file limit` is _paged_, which means only part(s) of the file(page(s)) will read & allocated in memory.
- If you want to make big file read as single-paged mode, consider increasing the value of `Single-paged file limit`(which will, of course, consume more memory).
- If you want to reduce memory usage, consider decreasing `Characters per page`, or `Loaded page(s) in memory`.
- `TextViewer` can read a few pages in advance for performance. decreasing `Loaded page(s) in memory` may save much memory, but displaying next page may be slower.


## Usage
```
Usage : java -jar TextViewer.jar [options]
Options : 
	--help	Show this help message

	--showAllFont	Show all fonts(even if it can't display texts in the editor) in font list in "Change font" Dialog

	--verbose	Log verbose/debugging information

	--logConsole	Log at system console, not in a file

	--pageHash=<HashAlgorithm>	Use designated hash algorithm when checking if a page is edited.
					In default, CRC32 checksum will be used. you can use various hash algorithm like SHA,
					or you may use whole text value as a "hash" by --pageHash=RAW
					Since hash values of all read pages are stored in memory, this will cause every page that have read in memory,
					eventually put whole file in memory.
					Available <HashAlgorithm> options are below :
						RAW
						Adler32
						CRC32
						SHA-1
						MD2
						MD5
						SHA-512/256
                                                ......
```
