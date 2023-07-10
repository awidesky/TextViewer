/*
 * Copyright (c) 2023 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package io.github.awidesky.textViewer.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.textViewer.LineSeparator;
import io.github.awidesky.textViewer.Main;
import io.github.awidesky.textViewer.SettingData;

public class SettingDialog extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7514995941974896916L;
	
	private JLabel lb_bufSize = new JLabel("Prefered Buffer size :");
	private JTextField tf_bufSize = new JTextField();

	private JLabel lb_limit = new JLabel("Single-paged file limit :");
	private JTextField tf_limit = new JTextField();
	private JComboBox<String> cmb_limit = new JComboBox<String>(new String[] { "B", "KB", "MB", "GB" });
	
	private JLabel lb_charPerPage = new JLabel("Characters per page :");
	private JTextField tf_charPerPage = new JTextField();
	private JLabel lb_chars = new JLabel("char(s)");
	
	private JLabel lb_newLine = new JLabel("Page always ends with newline");
	private JCheckBox chb_newLine = new JCheckBox();
	
	private JLabel lb_Queue = new JLabel("Loaded page(s) in memory :");
	private JTextField tf_Queue = new JTextField();
	
	private JLabel lb_lineSep = new JLabel("Line Separator (*system default) :");
	private JComboBox<String> cmb_lineSep = new JComboBox<String>(Arrays.stream(LineSeparator.values()).map(LineSeparator::getExplain).collect(Collectors.toList()).toArray(new String[] {}));
	
	private JButton btn_done = new JButton("done");
	
	public SettingDialog(SettingData setting) {
		
		super((Window)null);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setModal(true);
		setTitle("Setting");
		setSize(250, 300);
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		setResizable(false);
		setLayout(null);
		
		lb_bufSize.setBounds(5, 8, lb_bufSize.getPreferredSize().width, lb_bufSize.getPreferredSize().height);
		tf_bufSize.setBounds(8 + lb_bufSize.getPreferredSize().width, 5, 60, tf_bufSize.getPreferredSize().height);
		tf_bufSize.setText("" + setting.getCharBufSize());
		
		lb_limit.setBounds(5, 38, lb_limit.getPreferredSize().width, lb_limit.getPreferredSize().height);
		tf_limit.setBounds(8 + lb_limit.getPreferredSize().width, 35, 40, tf_limit.getPreferredSize().height);
		Scanner sc = new Scanner(Main.formatExactFileSize(setting.getSinglePageFileSizeLimit()));
		sc.useDelimiter(Pattern.compile("\\D+"));
		tf_limit.setText(String.valueOf(sc.nextLong()));
		sc.useDelimiter(Pattern.compile("\\d+"));
		cmb_limit.setSelectedItem(sc.next());
		sc.close();
		cmb_limit.setBounds(51 + lb_limit.getPreferredSize().width, 35, cmb_limit.getPreferredSize().width, 22);
		
		lb_charPerPage.setBounds(5, 68, lb_charPerPage.getPreferredSize().width, lb_charPerPage.getPreferredSize().height);
		tf_charPerPage.setBounds(8 + lb_charPerPage.getPreferredSize().width, 65, 50, tf_charPerPage.getPreferredSize().height);
		tf_charPerPage.setText("" + setting.getCharPerPage());
		lb_chars.setBounds(70 + lb_bufSize.getPreferredSize().width, 65, lb_chars.getPreferredSize().width, 22);
		
		lb_newLine.setBounds(5, 98, lb_newLine.getPreferredSize().width, lb_newLine.getPreferredSize().height);
		chb_newLine.setBounds(8 + lb_newLine.getPreferredSize().width, 95, chb_newLine.getPreferredSize().width, chb_newLine.getPreferredSize().height);
		chb_newLine.setSelected(setting.getPageEndsWithNewline());
		
		lb_Queue.setBounds(5, 128, lb_Queue.getPreferredSize().width, lb_Queue.getPreferredSize().height);
		tf_Queue.setBounds(8 + lb_Queue.getPreferredSize().width, 125, 20, tf_Queue.getPreferredSize().height);
		tf_Queue.setText("" + setting.getLoadedPagesNumber());
		
		lb_lineSep.setBounds(5, 158, lb_lineSep.getPreferredSize().width, lb_lineSep.getPreferredSize().height);
		cmb_lineSep.setSelectedItem(setting.getLineSeparator().getExplain());
		cmb_lineSep.setBounds(10, 163 + lb_lineSep.getPreferredSize().height, cmb_lineSep.getPreferredSize().width, cmb_lineSep.getPreferredSize().height);
		
		btn_done.setBounds(getSize().width/2 - btn_done.getPreferredSize().width/2 - 10, getSize().height - btn_done.getPreferredSize().height - 45, btn_done.getPreferredSize().width, btn_done.getPreferredSize().height);
		btn_done.setMnemonic('d');
		btn_done.addActionListener((e) -> {
			try {
				if(!setting.set(Integer.valueOf(tf_bufSize.getText()), Integer.valueOf(tf_charPerPage.getText()), chb_newLine.isSelected(), Main.getExactByteSize(tf_limit.getText() + cmb_limit.getSelectedItem()), Integer.valueOf(tf_Queue.getText()), LineSeparator.getFromExplain((String)cmb_lineSep.getSelectedItem())))
					return;
			} catch(NumberFormatException ex) {
				SwingDialogs.error("Invalid input!", "%e%", ex, true);
				return;
			}
			setVisible(false);
			dispose();
		});
		
		
		add(lb_bufSize);
		add(tf_bufSize);
		add(lb_charPerPage);
		add(tf_charPerPage);
		add(lb_chars);
		add(lb_newLine);
		add(chb_newLine);
		add(lb_limit);
		add(tf_limit);
		add(cmb_limit);
		add(lb_Queue);
		add(tf_Queue);
		add(lb_lineSep);
		add(cmb_lineSep);
		add(btn_done);
		
		setVisible(true);
		
	}
	
}