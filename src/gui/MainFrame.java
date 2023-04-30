package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;

import io.Page;
import io.SelectedFileHandler;
import io.TextFile;
import main.LineSeparator;
import main.Main;
import main.ReferenceDTO;
import util.SwingDialogs;
import util.TaskLogger;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = -2213063916837230382L;

	private TaskLogger logger = Main.getLogger("[MainFrame]");
	
	private TextFile lastOpened = new TextFile(new File(System.getProperty("user.home")), Charset.defaultCharset(), LineSeparator.getDefault());
	private TextFile lastSaved = new TextFile(new File(System.getProperty("user.home")), Charset.defaultCharset(), LineSeparator.getDefault());

	private JScrollPane sp;
	private JTextArea ta = new JTextArea();
	private JPanel statusPanel;

	private JLabel pathName = new JLabel();
	private JLabel size = new JLabel();
	private JLabel encoding = new JLabel();
	private JLabel newline = new JLabel(LineSeparator.getDefault().getAbbreviation(), SwingConstants.RIGHT);

	private UndoManager undoManager = new UndoManager();

	private TextFilechooser fileChooser = new TextFilechooser();

	private BlockingQueue<Page> fileContentQueue = null;

	private Set<Long> editedPage = new HashSet<>();

	private SelectedFileHandler fileHandle = null;
	private Page.Metadata nowPageMetadata = null;
	private boolean isEdited = false;

	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem openFile;
	private JMenuItem quickSaveFile;
	private JMenuItem saveFile;
	private JMenuItem closeFile;
	private JMenu editMenu;
	private JMenuItem undo;
	private JMenuItem redo;
	private JMenu formatMenu;
	private JMenuItem setting;
	private JMenuItem font;
	private JCheckBoxMenuItem editable;
	private JCheckBoxMenuItem wrap;
	private JMenu pageMenu;
	private JMenuItem next;
	private JMenuItem reRead;

	/** <code>true</code> if new content is being written in TextArea */
	private AtomicBoolean newPageReading = new AtomicBoolean(false);

	private boolean noNextPage = false;

	public MainFrame() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			SwingDialogs.error("Error while setting window look&feel", "%e%", e, false);
			e.printStackTrace();
		}

		MetadataGenerator.metadataConsumer(this::viewMetadata);

		setTitle(Main.VERSION);
		setSize(800, 700);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {

				if (!saveBeforeClose())
					return;

				if (fileHandle != null)
					fileHandle.close();
				e.getWindow().dispose();
				Main.kill(0);

			}

		});

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width / 2 - getSize().width / 2, dim.height / 2 - getSize().height / 2);

		addMenubar();

		ta.setBackground(Color.LIGHT_GRAY);
		ta.setEditable(true);
		ta.getDocument().addUndoableEditListener(undoManager);
		ta.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				changed(e);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				changed(e);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				changed(e);
			}

			private void changed(DocumentEvent e) {
				if (newPageReading.get()) { // new file is just read. user didn't type anything.
					return;
				}

				undo.setEnabled(undoManager.canUndo());
				redo.setEnabled(undoManager.canRedo());
				logger.log("File Edited! : " + e.getType().toString());
				isEdited = true;

				if (!getTitle().startsWith("*")) {
					closeFile.setEnabled(true);
					saveFile.setEnabled(true);
					quickSaveFile.setEnabled(true);
					MetadataGenerator.edited(true);
				}
			}
		});
		new DropTarget(ta, new DropTargetListener() {
			public void dragEnter(DropTargetDragEvent e) {
			}

			public void dragExit(DropTargetEvent e) {
			}

			public void dragOver(DropTargetDragEvent e) {
			}

			public void dropActionChanged(DropTargetDragEvent e) {
			}

			public void drop(DropTargetDropEvent e) {
				File dropped = null;
				try {
					Transferable tr = e.getTransferable();
					DataFlavor[] flavors = tr.getTransferDataFlavors();

					if (flavors[0].isFlavorJavaFileListType()) {

						e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

						@SuppressWarnings("unchecked")
						List<File> list = (List<File>) tr.getTransferData(flavors[0]);

						if (list.size() > 1) {
							SwingDialogs.error("Drag & Drop error!", "Do not drag & drop more than one file!", null,
									true);
							return;
						}

						dropped = list.get(0);
					}
				} catch (Exception ex) {
					SwingDialogs.error("Drag & Drop error!", "%e%", ex, false);
				}
				openFile(new TextFile(dropped, new CharsetChooser(lastOpened.encoding).getSelectedCharset(), Main.setting.getLineSeparator()));
			}
		});
		ta.addMouseWheelListener(mouseWheelEvent -> {
			if (mouseWheelEvent.isControlDown()) {
				Font font = ta.getFont();
				int fontSize = font.getSize();
				int delta = -(mouseWheelEvent.getUnitsToScroll() / 3);
				if ((delta < 0 && fontSize > 1) || (delta > 0 && fontSize < Integer.MAX_VALUE))
					fontSize += delta;
				Font newFont = new Font(font.getFontName(), font.getStyle(), fontSize);
				ta.setFont(newFont);
			} else {
				ta.getParent().dispatchEvent(mouseWheelEvent);
			}
		});
		ta.setWrapStyleWord(true);
		ta.setLineWrap(false);
		
		setLayout(new BorderLayout(5, 5));

		sp = new JScrollPane(ta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		sp.setBackground(Color.WHITE);

		addstatusPanel();
		
		add(sp, BorderLayout.CENTER);
		add(statusPanel, BorderLayout.SOUTH);

		setVisible(true);

	}

	private void addstatusPanel() {
		statusPanel = new JPanel(new BorderLayout());
		statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 1, 1, 1));
		/*
		size.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		encoding.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		newline.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		pathName.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		*/
		JPanel innerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		innerPanel.add(Box.createHorizontalStrut(15));
		innerPanel.add(size);
		innerPanel.add(Box.createHorizontalStrut(15));
		innerPanel.add(encoding);
		innerPanel.add(Box.createHorizontalStrut(15));
		innerPanel.add(newline);
		statusPanel.add(pathName, BorderLayout.CENTER);
		statusPanel.add(innerPanel, BorderLayout.EAST);
		MetadataGenerator.generate();
	}
	
	private void addMenubar() {

		menuBar = new JMenuBar();

		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.getAccessibleContext().setAccessibleDescription("File menu");

		openFile = new JMenuItem("Open file...", KeyEvent.VK_O);
		openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
		openFile.getAccessibleContext().setAccessibleDescription("Open a file");
		openFile.addActionListener((e) -> {
			// open chosen file
			openFile(null);
		});
		quickSaveFile = new JMenuItem("Save", KeyEvent.VK_S);
		quickSaveFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		quickSaveFile.getAccessibleContext().setAccessibleDescription("Quick save the current file");
		quickSaveFile.addActionListener((e) -> {

			logger.log("Quicksave!");
			TextFile saveTo;

			if (fileHandle == null) {
				if ((saveTo = selectSaveFile()) == null)
					return;
			} else {
				if (!isEdited && editedPage.isEmpty()) {
					logger.log("Nothing to save! Page unedited!");
					return;
				}

				if (fileHandle.isPaged() && isEdited
						&& fileHandle.isPageEdited(nowPageMetadata.pageNum, Main.getHash(ta.getText()))) {
					fileHandle.pageEdited(new Page(ta.getText(), nowPageMetadata.pageNum, false));
					if (!editedPage.contains(nowPageMetadata.pageNum))
						editedPage.add(nowPageMetadata.pageNum);
				}
				saveTo = lastOpened;
			}

			if (fileHandle != null) {
				fileHandle.close();
			}

			if (saveFile(saveTo)) {
				lastSaved = saveTo;
				openFile(lastSaved);
			} else if (fileHandle.getReadCharset() == null)
				fileHandle = null; // if fileHandle was null before and write failed, re-set fileHandle to null

		});
		quickSaveFile.setEnabled(false);
		saveFile = new JMenuItem("Save file in another encoding...", KeyEvent.VK_S);
		saveFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		saveFile.getAccessibleContext().setAccessibleDescription("Save file in another encoding");
		saveFile.addActionListener((e) -> {

			logger.log("Save in another encoding!");
			/** Write file in EDT */
			if (fileHandle != null && fileHandle.isPaged() && isEdited
					&& fileHandle.isPageEdited(nowPageMetadata.pageNum, Main.getHash(ta.getText()))) {
				fileHandle.pageEdited(new Page(ta.getText(), nowPageMetadata.pageNum, false));
				if (!editedPage.contains(nowPageMetadata.pageNum))
					editedPage.add(nowPageMetadata.pageNum);
			}

			TextFile save = selectSaveFile();
			if (save == null)
				return;

			if (saveFile(save)) {
				openFile(lastSaved);
			}

		});
		saveFile.setEnabled(false);
		closeFile = new JMenuItem("Close current file");
		closeFile.getAccessibleContext()
				.setAccessibleDescription("Close current reading file and make TextViewer empty");
		closeFile.addActionListener((e) -> {
			closeFile();
		});
		closeFile.setEnabled(false);
		fileMenu.add(openFile);
		fileMenu.add(quickSaveFile);
		fileMenu.add(saveFile);
		fileMenu.add(closeFile);

		editMenu = new JMenu("Edit");
		editMenu.getAccessibleContext().setAccessibleDescription("Edit menu");

		undo = new JMenuItem("Undo", KeyEvent.VK_Z);
		undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
		undo.getAccessibleContext().setAccessibleDescription("Undo");
		undo.addActionListener((e) -> {
			undoManager.undo();
			undo.setEnabled(undoManager.canUndo());
			redo.setEnabled(undoManager.canRedo());

			if (!undoManager.canUndo()) {
				if (fileHandle.isPaged()
						&& !fileHandle.isPageEdited(nowPageMetadata.pageNum, Main.getHash(ta.getText()))) {
					editedPage.remove(nowPageMetadata.pageNum);
				}
				if (editedPage.isEmpty()) {
					isEdited = false;
					MetadataGenerator.edited(false);
				}
			}
		});
		undo.setEnabled(false);
		redo = new JMenuItem("Redo", KeyEvent.VK_Y);
		redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
		redo.getAccessibleContext().setAccessibleDescription("Undo");
		redo.addActionListener((e) -> {
			undoManager.redo();
			undo.setEnabled(undoManager.canUndo());
			redo.setEnabled(undoManager.canRedo());
		});
		redo.setEnabled(false);
		editMenu.add(undo);
		editMenu.add(redo);
		editMenu.setEnabled(true);
		editable(true);
		
		formatMenu = new JMenu("Setting");
		formatMenu.setMnemonic(KeyEvent.VK_T);
		formatMenu.getAccessibleContext().setAccessibleDescription("Setting menu");

		setting = new JMenuItem("Setting dialog", KeyEvent.VK_D);
		setting.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
		setting.getAccessibleContext().setAccessibleDescription("Buffer size setting");
		setting.addActionListener((e) -> {
			new SettingDialog(Main.setting);
			newline.setText(Main.setting.getLineSeparator().getAbbreviation());
			newline.setMaximumSize(new Dimension(newline.getPreferredSize().width, newline.getPreferredSize().height));
		});
		font = new JMenuItem("Change font", KeyEvent.VK_C);
		font.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
		font.getAccessibleContext().setAccessibleDescription("Change font type or size");
		font.addActionListener((e) -> {
			ReferenceDTO<Font> ref = new ReferenceDTO<>(ta.getFont());
			new FontDialog(ref, ta.getFont(), ta.getText());
			ta.setFont(ref.get());
		});
		editable = new JCheckBoxMenuItem("Editable");
		editable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK));
		editable.getAccessibleContext().setAccessibleDescription("Set this file editable in viewer");
		editable.setSelected(true);
		editable.addActionListener((e) -> {
			saveFile.setEnabled(true);
			quickSaveFile.setEnabled(true);
			editable(!ta.isEditable());
		});
		wrap = new JCheckBoxMenuItem("Word wrap");
		wrap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.ALT_MASK));
		wrap.getAccessibleContext().setAccessibleDescription("Automatic line changing(doesn't change file content)");
		wrap.setSelected(false);
		wrap.addActionListener((e) -> {
			if(wrap.isSelected()) {
				sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				ta.setLineWrap(true);
			} else {
				sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				ta.setLineWrap(false);
			}
		});
		formatMenu.add(setting);
		formatMenu.add(font);
		formatMenu.add(editable);
		formatMenu.add(wrap);

		pageMenu = new JMenu("Pages");
		pageMenu.setMnemonic(KeyEvent.VK_P);
		pageMenu.getAccessibleContext().setAccessibleDescription("Pages menu");

		next = new JMenuItem("Next page", KeyEvent.VK_N);
		next.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		next.getAccessibleContext().setAccessibleDescription("Show next page");
		next.addActionListener((e) -> {
			nextPage();
		});
		reRead = new JMenuItem("Restart from begining", KeyEvent.VK_R);
		reRead.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		reRead.getAccessibleContext().setAccessibleDescription("Re-read from first page");
		reRead.addActionListener((e) -> {
			next.setEnabled(true);
			makeNewQueue();
			fileHandle.reRead(fileContentQueue);
			nextPage();
		});
		pageMenu.add(next);
		pageMenu.add(reRead);
		pageMenu.setEnabled(false);

		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(formatMenu);
		menuBar.add(pageMenu);

		setJMenuBar(menuBar);

	}

	/**
	 * 
	 * Ask save the file before closing file. If user says yes, save the file;
	 * 
	 * @return <code>true</code> if current file is OK to close. <code>false</code>
	 *         if it's not OK.
	 * 
	 */
	protected boolean saveBeforeClose() {
		if (getTitle().startsWith("*")) {
			switch (JOptionPane.showConfirmDialog(null, "Save changed content?", "Save change?",
					JOptionPane.YES_NO_CANCEL_OPTION)) {
			case JOptionPane.YES_OPTION:
				saveFile(selectSaveFile());
			case JOptionPane.CANCEL_OPTION:
			case JOptionPane.CLOSED_OPTION:
				return false;
			}
		}
		return true;
	}

	public void viewMetadata(MetadataGenerator.Metadata mt) {
		Runnable task = () -> {
			setTitle(mt.title);
			pathName.setText(mt.path);
			size.setText(mt.fileSize);
			encoding.setText(mt.charset);
			newline.setText(mt.newline);
		};
		if(SwingUtilities.isEventDispatchThread()) task.run();
		else SwingUtilities.invokeLater(task);
	}

	public void closeFile() {
		if (saveBeforeClose()) {
			ta.setText(null);
			if (fileHandle != null)
				fileHandle.close();
			fileHandle = null;
			fileContentQueue = null;
			disableNextPageMenu();
			setting.setEnabled(true);
			saveFile.setEnabled(false);
			quickSaveFile.setEnabled(false);
			closeFile.setEnabled(false);
			undoManager.discardAllEdits();
			setTitle(Main.VERSION);
			MetadataGenerator.fileClosed();
		}
	}

	private void makeNewQueue() {
		if (Main.setting.getLoadedPagesNumber() < 3) {
			fileContentQueue = new SynchronousQueue<>();
		} else {
			fileContentQueue = new LinkedBlockingQueue<>(Main.setting.getLoadedPagesNumber() - 1);
		}
	}

	private void editable(boolean flag) {
		ta.setEditable(flag);
		editMenu.setEnabled(flag);
	}

	private void openFile(TextFile openfile) {

		if (!saveBeforeClose()) {
			return;
		}

		saveFile.setEnabled(true);
		quickSaveFile.setEnabled(true);
		closeFile.setEnabled(true);

		if (openfile != null) {
			lastOpened = openfile;
		} else {
			fileChooser.setDialogTitle("Select file to read...");
			fileChooser.setLastOpendDir(lastOpened);
			fileChooser.getActionMap().get("viewTypeDetails").actionPerformed(null);
			if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
				return;

			lastOpened = new TextFile(fileChooser.getSelectedFile(), fileChooser.getSelectedCharset(), Main.setting.getLineSeparator());
		}

		if (fileHandle != null)
			fileHandle.close();
		fileHandle = new SelectedFileHandler(lastOpened);
		makeNewQueue();

		if (fileHandle.isPaged()) {
			enableNextPageMenu();
			setting.setEnabled(false);
		} else {
			disableNextPageMenu();
			setting.setEnabled(true);
		}
		MetadataGenerator.reset(lastOpened.file, fileHandle.isPaged(), lastOpened.encoding.name(), false, true, 1L);

		noNextPage = false;
		fileHandle.startNewRead(fileContentQueue);
		displyNewPage();

	}

	/**
	 * Select location to save the file
	 * 
	 * @return Selected <code>File</code>. if canceled/failed, <code>null</code>
	 * 
	 */
	private TextFile selectSaveFile() {

		fileChooser.setDialogTitle("Save file at...");
		fileChooser.setSelectedFile(lastOpened.file);
		switch (fileChooser.showSaveDialog(null)) {

		case JFileChooser.CANCEL_OPTION:
			logger.log("JFileChooser canceled!");
			return null;
		case JFileChooser.ERROR_OPTION:
			SwingDialogs.error("JFileChooser error occured!", "Cancelling save operation...", null, false);
			return null;
		case JFileChooser.APPROVE_OPTION:
			break;
		}

		File f = fileChooser.getSelectedFile();
		if (fileChooser.getFileFilter().equals(TextFilechooser.TEXTFILEFILTER) && !f.getName().endsWith(".txt")) {
			f = new File(f.getParentFile(), f.getName() + ".txt");
		}

		if (f.exists() && JOptionPane.showConfirmDialog(null, "replace file?", f.getName() + " already exists!",
				JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
			logger.log("User refuesed to overwrite existing file!");
			return null;
		}
		return new TextFile(f, fileChooser.getSelectedCharset(), Main.setting.getLineSeparator());

	}

	/**
	 * This method saves content to the file.
	 * 
	 * File is written in EDT.
	 * 
	 * @param saveTo
	 * 
	 * @return <code>true</code> if successfully saved. if canceled/failed,
	 *         <code>false</code>
	 * 
	 */
	private boolean saveFile(TextFile saveTo) {

		MetadataGenerator.edited(false);
		if (fileHandle == null)
			fileHandle = new SelectedFileHandler();

		if (fileHandle.write(saveTo, ta.getText())) {
		} else {
			setTitle(Main.VERSION);
			MetadataGenerator.fileClosed();
			MetadataGenerator.edited(true);
			logger.log("File write failed!");
			return false;
		}
		lastSaved = saveTo;
		return true;

	}

	private void nextPage() {

		if (!pageMenu.isEnabled() || !next.isEnabled())
			return;

		if (fileHandle.isPageEdited(nowPageMetadata.pageNum, Main.getHash(ta.getText()))) {
			if (!editedPage.contains(nowPageMetadata.pageNum))
				editedPage.add(nowPageMetadata.pageNum);
		} else {
			editedPage.remove(nowPageMetadata.pageNum);
			fileHandle.pageNotChanged(nowPageMetadata.pageNum);
		}

		if (editedPage.contains(nowPageMetadata.pageNum) || isEdited) {
			fileHandle.pageEdited(new Page(ta.getText(), nowPageMetadata));
		}

		undoManager.discardAllEdits();
		undo.setEnabled(undoManager.canUndo());
		redo.setEnabled(undoManager.canRedo());

		MetadataGenerator.loading(true);
		isEdited = false;
		SwingUtilities.invokeLater(this::displyNewPage);

	}

	private void displyNewPage() {

		newPageReading.set(true);
		Page nowDisplayed = null;
		boolean originVal = ta.isEditable();
		editable(false);
		try {

			/**
			 * only one page can be loaded in memory, so <code>fileHandle</code> is waiting,
			 * now we wake it up.
			 */
			if (fileHandle.isPaged() && fileHandle.getLoadedPagesNumber() == 1) {

				if (!noNextPage && !fileHandle.isReachedEOF())
					ta.setText(null);
				/** If next page is EOF, TextArea shouldn't be cleared */

				synchronized (fileHandle) {
					fileHandle.notify();
				}
			}
			nowDisplayed = fileContentQueue.poll(500, TimeUnit.MILLISECONDS);

			if (nowDisplayed == Page.ERR || fileHandle.isFailed()) {
				MetadataGenerator.loading(false);
				editable(originVal);
				newPageReading.set(false);
				logger.log("[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId()
						+ ")]Task is failed. abort waiting new page.");
				return;
			} else if (nowDisplayed == null) {
				editable(originVal);
				SwingUtilities.invokeLater(this::displyNewPage);
				return;
			}

			nowPageMetadata = nowDisplayed.metadata;
		} catch (InterruptedException e1) {
			SwingDialogs.error("interrupted while waiting worker thread to read thes page!!", "%e%", e1, true);
			nowDisplayed = new Page("", -1, true);
		}

		if (nowDisplayed != Page.EOF) {
			if (nowDisplayed.isLastPage())
				noNextPage = true;
			if (fileHandle.isPaged()) {
				logger.log("[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId()
						+ ")] page #" + nowDisplayed.pageNum() + " is consumed and displayed");
				MetadataGenerator.pageNum(nowDisplayed.pageNum());
			}
			ta.setText(nowDisplayed.text);
			ta.setCaretPosition(0);
			sp.getVerticalScrollBar().setValue(0);
			undoManager.discardAllEdits();
		} else {
			SwingDialogs.information("No more page to read!", "Reached EOF!", false);
			next.setEnabled(false);
		}

		MetadataGenerator.loading(false);
		editable(originVal);
		newPageReading.set(false);

	}

	private void enableNextPageMenu() {
		pageMenu.setEnabled(true);
		next.setEnabled(true);
	}

	private void disableNextPageMenu() {
		pageMenu.setEnabled(false);
		next.setEnabled(false);
	}

}
