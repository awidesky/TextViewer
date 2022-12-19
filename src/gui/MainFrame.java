package gui;

import static main.Main.logger;

import java.awt.Color;
import java.awt.Dimension;
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
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;

import main.Main;
import main.ReferenceDTO;
import main.SelectedFileHandler;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 4096788492544078810L;
	
	private JScrollPane sp;
	private JTextArea ta = new JTextArea();
	private UndoManager undoManager = new UndoManager();;
	private File lastOpened = new File(System.getProperty("user.home"));
	private File lastSaved = new File(System.getProperty("user.home"));
	
	private TextFilechooser fileChooser = new TextFilechooser();
	
	private LinkedBlockingQueue<Consumer<String>> readCallbackQueue = new LinkedBlockingQueue<>();
	
	private SelectedFileHandler fileHandle = null;

	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem openFile;
	private JMenuItem saveFile;
	private JMenu editMenu;
	private JMenuItem undo;
	private JMenuItem redo;
	private JMenu formatMenu;
	private JMenuItem bufSetting;
	private JMenuItem font;
	private JCheckBoxMenuItem editable;
	private JMenu pageMenu;
	private JMenuItem next;
	private JMenuItem reRead;

	/** <code>true</code> if new content is being written in TextArea */
	private boolean newPageReading = false;
	
	
	public MainFrame() {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			SwingDialogs.error("Error while setting window look&feel", "%e%", e, false);
			e.printStackTrace();
		}
		
		TitleGeneartor.titleConsumer(this::setTitle);	
		
		setTitle(Main.VERSION);
		setSize(800, 700);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {

				if(!saveBeforeClose()) return; 

				e.getWindow().dispose();
				Main.kill(0);

			}

		});
		
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		
		
		addMenubar();
		
		ta.setBackground(Color.LIGHT_GRAY);
		ta.setEditable(false);
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
	        public void changedUpdate(DocumentEvent arg0) {
	        	changed(arg0);
	        }
	        
	        private void changed(DocumentEvent e) {
  	        	if(newPageReading) { //new file is just read. user didn't type anything.
	        		return;	
	        	}
  	        	
  	        	undo.setEnabled(undoManager.canUndo());
  				redo.setEnabled(undoManager.canRedo());
	        	if(!getTitle().startsWith("*")) {
	        		logger.log("File Edited! : " + e.getType().toString());
	        		TitleGeneartor.edited(true);
	        	}
	        }
	    });
		new DropTarget(ta, new DropTargetListener(){
            public void dragEnter(DropTargetDragEvent e)
            {
            }
            
            public void dragExit(DropTargetEvent e)
            {
            }
            
            public void dragOver(DropTargetDragEvent e)
            {
            }
            
            public void dropActionChanged(DropTargetDragEvent e)
            {
            
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

						if(list.size() > 1) {
							SwingDialogs.error("Drag & Drop error!", "Do not drag & drop more than one file!", null, false);
							return;
						}
						 
						dropped = list.get(0);
					}
				} catch (Exception ex) {
					SwingDialogs.error("Drag & Drop error!", "%e%", ex, false);
				}
				openFile(dropped);
			}
        });
		
		
		sp = new JScrollPane(ta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		sp.setBackground(Color.WHITE);
		
		add(sp);
		
		setVisible(true);
		
	}
	
	/**
	 * 
	 * Ask save the file before closing file.
	 * 
	 * @return <code>true</code> if current file is OK to close. <code>false</code> if it's not OK.
	 * 
	 * */
	protected boolean saveBeforeClose() {
		if(getTitle().startsWith("*")) {
			switch(JOptionPane.showConfirmDialog(null, "Save changed content?", "Save change?", JOptionPane.YES_NO_CANCEL_OPTION)) {
			case JOptionPane.YES_OPTION:
				return saveFile();
			case JOptionPane.CANCEL_OPTION:
			case JOptionPane.CLOSED_OPTION:
				return false;
			}
		}
		return true;
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
			
			//open chosen file
			openFile(null);
			saveFile.setEnabled(true);
			
		});
		saveFile = new JMenuItem("Save file in another encoding...", KeyEvent.VK_S);
		saveFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		saveFile.getAccessibleContext().setAccessibleDescription("Save file in another encoding");
		saveFile.addActionListener((e) -> {
			
			/** Read file in EDT */
			saveFile();
			
		});
		saveFile.setEnabled(false);
		fileMenu.add(openFile);
		fileMenu.add(saveFile);

		
		
		editMenu = new JMenu("Edit");
		editMenu.getAccessibleContext().setAccessibleDescription("Edit menu");
		
		undo = new JMenuItem("Undo", KeyEvent.VK_Z);
		undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
		undo.getAccessibleContext().setAccessibleDescription("Undo");
		undo.addActionListener((e) -> {
			undoManager.undo();
			undo.setEnabled(undoManager.canUndo());
			redo.setEnabled(undoManager.canRedo());
			if(!undoManager.canUndo()) TitleGeneartor.edited(false);
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
		editMenu.setEnabled(false);
		
		
		
		formatMenu = new JMenu("Setting");
		formatMenu.setMnemonic(KeyEvent.VK_T);
		formatMenu.getAccessibleContext().setAccessibleDescription("Setting menu");
		
		bufSetting = new JMenuItem("Change buffer size", KeyEvent.VK_B);
		bufSetting.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		bufSetting.getAccessibleContext().setAccessibleDescription("Buffer size setting");
		bufSetting.addActionListener((e) -> {
			ReferenceDTO<Integer> ref = new ReferenceDTO<>(Main.bufferSize);
			new BufferSettingDialog(ref);
			Main.bufferSize = ref.get();
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
		editable.addActionListener((e) -> {
			
			saveFile.setEnabled(true);
			boolean swapped = !ta.isEditable();
			ta.setEditable(swapped);
			editMenu.setEnabled(swapped);
			
		});
		formatMenu.add(bufSetting);
		formatMenu.add(font);
		formatMenu.add(editable);
		

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
			readCallbackQueue.clear();
			fileHandle.reRead(readCallbackQueue);
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
	
	
	private void openFile(File file) {
		

		if(!saveBeforeClose()) {
			return;
		}
		
		try {
			
			if (file != null) {
				lastOpened = file;
			} else {
				fileChooser.setDialogTitle("Select file to read...");
				fileChooser.setCurrentDirectory(lastOpened.getParentFile());
				fileChooser.getActionMap().get("viewTypeDetails").actionPerformed(null);
				if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
					return;

				lastOpened = fileChooser.getSelectedFile();
			}
		    
			if(fileHandle != null) fileHandle.close();
		    fileHandle = new SelectedFileHandler(lastOpened, fileChooser.getSelectedCharset());
		    readCallbackQueue = new LinkedBlockingQueue<>();
		    
		    if(fileHandle.isPaged()) {
		    	enableNextPageMenu();
		    } else {
		    	disableNextPageMenu();
		    }
		    TitleGeneartor.reset(lastOpened.getAbsolutePath(), Main.formatFileSize(lastOpened.length()), fileHandle.isPaged(), fileChooser.getSelectedCharset().name(), false, true, 1L);
			
			fileHandle.startNewRead(readCallbackQueue);
			
			newPageReading = true;
			readCallbackQueue.put(s -> {
				if (s != null) {
					ta.setText(s);
					ta.setCaretPosition(0);
					sp.getVerticalScrollBar().setValue(0);
					undoManager.discardAllEdits();
					TitleGeneartor.loading(false);
					newPageReading = false; 
				}
			});
			
		} catch (InterruptedException excep) {
			SwingDialogs.error("Cannot read seleceted file!", "Thread Iterrupted when reading : %e%", excep, true);
		}
		
		
	}
	
	/**
	 *  This method saves content to the file.
	 * 	
	 * 	File is written in EDT.
	 *  
	 *  @return <code>true</code> if successfully saved. if canceled/failed, <code>false</code>
	 *  
	 *  */
	private boolean saveFile() {
		
		fileChooser.setDialogTitle("Save file at...");
		fileChooser.setSelectedFile(lastOpened);
		fileChooser.setCurrentDirectory(lastSaved.getParentFile());
		if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) 
	    	return false;
		
		lastSaved = fileChooser.getSelectedFile();
		if(lastSaved.exists() && JOptionPane.showConfirmDialog(null, "replace file?", lastSaved.getName() + " already exists!", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
			return false;
		}
		
		TitleGeneartor.edited(false);
		return fileHandle.write(lastSaved, fileChooser.getSelectedCharset(), ta.getText());
			
	}
	
	
	private void nextPage() {
		
		if (!pageMenu.isEnabled()) return;
		
		newPageReading = true;
		TitleGeneartor.loading(true);
		
		try {
			readCallbackQueue.put(s -> {
				if (s != null) {
					ta.setText(s);
					ta.setCaretPosition(0);
					sp.getVerticalScrollBar().setValue(0);
					undoManager.discardAllEdits();
					TitleGeneartor.loading(false);
					newPageReading = false;
				} else {
					SwingDialogs.information("No more page to read!", "Reached EOF!", false);
					disableNextPageMenu();
				}
			});
		} catch (InterruptedException e1) {
			SwingDialogs.error("interrupted while loading!", "%e%", e1, false);
		}
		
	}


	private void enableNextPageMenu() {
		pageMenu.setEnabled(true);
	}
	
	private void disableNextPageMenu() {
		pageMenu.setEnabled(false);
	}


}
