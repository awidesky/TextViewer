/*
 * Copyright (c) 2023 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package io.github.awidesky.textViewer.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import io.github.awidesky.textViewer.io.TextFile;

public class TextFilechooser extends JFileChooser {
	
	private static final long serialVersionUID = 6120225700667641868L;
	
	//private final ArrayList<Entry<String, Charset>> charsetList = Charset.availableCharsets().entrySet().stream().collect(Collectors.toCollection(ArrayList::new));
	public static final ArrayList<String> charsetNameList = Charset.availableCharsets().keySet().stream().collect(Collectors.toCollection(ArrayList::new));
	public static final String defaultCharset = "UTF-8";
	private static final JComboBox<String> comboBox = getCharsetChooseComboBox();
	private static final JCheckBox encryptedCheckBox = new JCheckBox("Encrypted", false);
	
	public static final FileFilter TEXTFILEFILTER = new FileFilter() {
		public boolean accept(File f) {
			if (f.isDirectory()	|| f.getName().endsWith(".txt"))
				return true;
			else
				return false;
		}
		public String getDescription() {
			return "Text files (*.txt)";
		}
	};

	
	public TextFilechooser() {

		comboBox.setSelectedIndex(charsetNameList.indexOf(defaultCharset));

		setMultiSelectionEnabled(false);
		setFileSelectionMode(JFileChooser.FILES_ONLY);
		addChoosableFileFilter(TEXTFILEFILTER);

		JPanel panel2;
		if(isMac()) {
			Dimension dim = getPreferredSize();
			dim.width += 90; // add some more space for "new Folder" button
			setPreferredSize(dim);
			panel2 = (JPanel)((JPanel)((JPanel)this.getComponent(4)).getComponent(2)).getComponent(1);
		} else { //windows
			panel2 = (JPanel)((JPanel) this.getComponent(3)).getComponent(3);
		}

		Component[] comps = panel2.getComponents();
		
		panel2.removeAll();

		encryptedCheckBox.setSize(encryptedCheckBox.getPreferredSize().width, encryptedCheckBox.getPreferredSize().height);

		if(isMac()) {
			panel2.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
			panel2.add(Box.createHorizontalStrut(15));
			panel2.add(comps[0]); //add new Folder button first
			
			panel2.add(Box.createHorizontalStrut(15));
			panel2.add(encryptedCheckBox);
			panel2.add(Box.createHorizontalStrut(15));
			panel2.add(comboBox);

			for (int i = 1; i < comps.length; i++) { // add rest of the original components(cancel, save/open) 
				panel2.add(Box.createHorizontalStrut(15));
				panel2.add(comps[i]);
			}
		} else {
			panel2.setLayout(new BorderLayout());
			panel2.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
			
			JPanel innerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
			innerPanel.add(Box.createHorizontalStrut(15));
			innerPanel.add(comboBox);
			for (Component c : comps) {
				innerPanel.add(Box.createHorizontalStrut(15));
				innerPanel.add(c);
			}

			panel2.add(encryptedCheckBox, BorderLayout.WEST); // encryption checkbox must be on the leftest(in windows)
			panel2.add(innerPanel, BorderLayout.EAST);
		}

	}
	

	public void setLastOpendDir(TextFile lastOpened) {
		setCurrentDirectory(lastOpened.file.getParentFile());
		comboBox.setSelectedIndex(charsetNameList.indexOf(lastOpened.encoding.name()));
	}
	

	public Charset getSelectedCharset() {
    	comboBox.setSelectedIndex(comboBox.getSelectedIndex());
    	return Charset.forName(charsetNameList.get(comboBox.getSelectedIndex()));
    }
	public boolean isEncrypted() {
		return encryptedCheckBox.isSelected();
	}
	
	
	private static boolean isMac() {
		return System.getProperty("os.name").toLowerCase().contains("mac");
	}
	
	public static JComboBox<String> getCharsetChooseComboBox() {
		return new JComboBox<>(new DefaultComboBoxModel<String>(charsetNameList.toArray(new String[] {})));
	}
}