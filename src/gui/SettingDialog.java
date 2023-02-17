package gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import main.Main;
import main.SettingData;

public class SettingDialog extends JDialog {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2127297466348994156L;
	
	
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
	private JTextField tf__Queue = new JTextField();
	
	private JButton btn_done = new JButton("done");
	
	public SettingDialog(SettingData setting) {
		
		super((Window)null);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setModal(true);
		setTitle("Setting");
		setSize(250, 220);
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		setResizable(false);
		setLayout(null);
		
		lb_bufSize.setBounds(5, 8, lb_bufSize.getPreferredSize().width, lb_bufSize.getPreferredSize().height);
		tf_bufSize.setBounds(8 + lb_bufSize.getPreferredSize().width, 5, 60, tf_bufSize.getPreferredSize().height);
		tf_bufSize.setText("" + setting.charBufSize);
		
		lb_limit.setBounds(5, 38, lb_limit.getPreferredSize().width, lb_limit.getPreferredSize().height);
		tf_limit.setBounds(8 + lb_limit.getPreferredSize().width, 35, 40, tf_limit.getPreferredSize().height);
		Scanner sc = new Scanner(Main.formatExactFileSize(setting.singlePageFileSizeLimit));
		sc.useDelimiter(Pattern.compile("\\D+"));
		tf_limit.setText(String.valueOf(sc.nextLong()));
		sc.useDelimiter(Pattern.compile("\\d+"));
		cmb_limit.setSelectedItem(sc.next());
		sc.close();
		cmb_limit.setBounds(51 + lb_limit.getPreferredSize().width, 35, cmb_limit.getPreferredSize().width, 22);
		
		lb_charPerPage.setBounds(5, 68, lb_charPerPage.getPreferredSize().width, lb_charPerPage.getPreferredSize().height);
		tf_charPerPage.setBounds(8 + lb_charPerPage.getPreferredSize().width, 65, 50, tf_charPerPage.getPreferredSize().height);
		tf_charPerPage.setText("" + setting.charPerPage);
		lb_chars.setBounds(70 + lb_bufSize.getPreferredSize().width, 65, lb_chars.getPreferredSize().width, 22);
		
		lb_newLine.setBounds(5, 98, lb_newLine.getPreferredSize().width, lb_newLine.getPreferredSize().height);
		chb_newLine.setBounds(8 + lb_newLine.getPreferredSize().width, 95, chb_newLine.getPreferredSize().width, chb_newLine.getPreferredSize().height);
		chb_newLine.setSelected(setting.pageEndsWithNewline);
		
		lb_Queue.setBounds(5, 128, lb_Queue.getPreferredSize().width, lb_Queue.getPreferredSize().height);
		tf__Queue.setBounds(8 + lb_Queue.getPreferredSize().width, 125, 20, tf__Queue.getPreferredSize().height);
		tf__Queue.setText("" + setting.loadedPagesNumber);
		
		btn_done.setBounds(getSize().width/2 - btn_done.getPreferredSize().width/2 - 10, getSize().height - btn_done.getPreferredSize().height - 45, btn_done.getPreferredSize().width, btn_done.getPreferredSize().height);
		btn_done.setMnemonic('d');
		btn_done.addActionListener((e) -> {
			try {
				if(!setting.set(Integer.valueOf(tf_bufSize.getText()), Integer.valueOf(tf_charPerPage.getText()), chb_newLine.isSelected(), Main.getExactByteSize(tf_limit.getText() + cmb_limit.getSelectedItem()), Integer.valueOf(tf__Queue.getText())))
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
		add(tf__Queue);
		add(btn_done);
		
		setVisible(true);
		
	}
	
}
