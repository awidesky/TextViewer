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
import java.awt.Container;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
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
	
	
	private String checkComponents(Component c) {
		StringBuilder sb = new StringBuilder();
		sb.append(c.getClass().getSimpleName()).append(" ").append(c.hashCode()).append("\n");
		if (c instanceof Container con) {
			for (Component cc : con.getComponents()) {
				for(String s : checkComponents(cc).split("\n")) {
					sb.append("  ").append(s).append("\n");
				}
			}
		}
		//sb.append("\n");
		return sb.toString();
	}
	
	public TextFilechooser() {

		comboBox.setSelectedIndex(charsetNameList.indexOf(defaultCharset));

		setMultiSelectionEnabled(false);
		setFileSelectionMode(JFileChooser.FILES_ONLY);
		addChoosableFileFilter(TEXTFILEFILTER);

		for (Component c : this.getComponents()) {
			System.out.println(checkComponents(c));
		}
		
		JPanel p = ((JPanel) this.getComponent(3));
		//System.out.println(p.getComponentCount());
		//System.out.println(p.getComponents());
		JPanel panel2 = (JPanel) p
				.getComponent(3);

		JButton b1 = (JButton) panel2.getComponent(0); // choose button
		JButton b2 = (JButton) panel2.getComponent(1); // cancel button

		panel2.removeAll();

		encryptedCheckBox.setSize(encryptedCheckBox.getPreferredSize().width, encryptedCheckBox.getPreferredSize().height);

		panel2.setLayout(new BorderLayout());
		panel2.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		JPanel innerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		innerPanel.add(Box.createHorizontalStrut(15));
		innerPanel.add(comboBox);
		innerPanel.add(Box.createHorizontalStrut(15));
		innerPanel.add(b1);
		innerPanel.add(Box.createHorizontalStrut(15));
		innerPanel.add(b2);

		panel2.add(encryptedCheckBox, BorderLayout.WEST);
		panel2.add(innerPanel, BorderLayout.EAST);

		System.out.println("=====================================");
		for (Component c : this.getComponents()) {
			System.out.println(checkComponents(c));
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
	
	public static JComboBox<String> getCharsetChooseComboBox() {
		return new JComboBox<>(new DefaultComboBoxModel<String>(charsetNameList.toArray(new String[] {})));
	}
}