package gui;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import main.Main;


public class SwingDialogs {



	/**
	 * show error dialog.
	 * String <code>"%e%"</code> in <code>content</code> will replaced by error message of given <code>Exception</code> if it's not <code>null</code>
	 * */
	public static void error(String title, String content, Exception e, boolean waitTillClosed) {

		Main.logger.log("\n");
		String co = content.replace("%e%", (e == null) ? "null" : e.getMessage());
		
		if (waitTillClosed) {
			showErrorDialog(title, co);
		} else {
			SwingUtilities.invokeLater(() -> {
				showErrorDialog(title, co);
			});
		}
		
		Main.logger.log("[GUI.error] " + title + "\n\t" + co);
		if(e != null) Main.logger.log(e);
		
	}
	
	/**
	 * Show error dialog.
	 * this method returns after the dialog closed.
	 * */
	private static void showErrorDialog(String title, String content) {

		final JDialog dialog = new JDialog();
		dialog.setAlwaysOnTop(true);
		
		if (EventQueue.isDispatchThread()) {

			JOptionPane.showMessageDialog(dialog, content.replace("\n", System.lineSeparator()), title.replace("\n", System.lineSeparator()), JOptionPane.ERROR_MESSAGE);
			
		} else {
			
			try {
				SwingUtilities.invokeAndWait(() -> {
					JOptionPane.showMessageDialog(dialog, content.replace("\n", System.lineSeparator()), title.replace("\n", System.lineSeparator()), JOptionPane.ERROR_MESSAGE);
				});
			} catch (Exception e) {
				error("Exception in Thread working(SwingWorker)", "%e%", (e instanceof InvocationTargetException) ? (Exception)e.getCause() : e, true);
			}

		}
		
	}
	
	
	
	
	/**
	 * show warning dialog.
	 * String <code>"%e%"</code> in <code>content</code> will replaced by warning message of given <code>Exception</code> if it's not <code>null</code>
	 * 
	 * */
	public static void warning(String title, String content, Exception e, boolean waitTillClosed) {

		Main.logger.log("\n");
		String co = content.replace("%e%", (e == null) ? "null" : e.getMessage());
		
		if (waitTillClosed) {
			showWarningDialog(title, co);
		} else {
			SwingUtilities.invokeLater(() -> {
				showWarningDialog(title, co);
			});
		}
		
		Main.logger.log("[GUI.error] " + title + "\n\t" + co);
		if(e != null) Main.logger.log(e);
		
	}
	
	/**
	 * Show warning dialog.
	 * this method returns after the dialog closed.
	 * */
	private static void showWarningDialog(String title, String content) {
		
		final JDialog dialog = new JDialog();
		dialog.setAlwaysOnTop(true);
		
		if (EventQueue.isDispatchThread()) {

			JOptionPane.showMessageDialog(dialog, content.replace("\n", System.lineSeparator()), title.replace("\n", System.lineSeparator()), JOptionPane.WARNING_MESSAGE);
			
		} else {
			
			try {
				SwingUtilities.invokeAndWait(() -> {
					JOptionPane.showMessageDialog(dialog, content.replace("\n", System.lineSeparator()), title.replace("\n", System.lineSeparator()), JOptionPane.WARNING_MESSAGE);
				});
			} catch (Exception e) {
				error("Exception in Thread working(SwingWorker)", "%e%", (e instanceof InvocationTargetException) ? (Exception)e.getCause() : e, true);
			}

		}
		
	}
	
	
	
	
	/**
	 * show information dialog.
	 * @param waitTillClosed 
	 * 
	 * */
	public static void information(String title, String content, boolean waitTillClosed) {

		Main.logger.log("\n");

		if (waitTillClosed) {
			showInfoDialog(title, content);
		} else {
			SwingUtilities.invokeLater(() -> {
				showInfoDialog(title, content);
			});
		}
		
		Main.logger.log("[GUI.information] " + title + "\n\t" + content);
		
	}
	
	/**
	 * Show information dialog.
	 * this method returns after the dialog closed.
	 * */
	private static void showInfoDialog(String title, String content) {
		
		final JDialog dialog = new JDialog();
		dialog.setAlwaysOnTop(true);
		
		if (EventQueue.isDispatchThread()) {

			JOptionPane.showMessageDialog(dialog, content.replace("\n", System.lineSeparator()), title.replace("\n", System.lineSeparator()), JOptionPane.INFORMATION_MESSAGE);
			
		} else {
			
			try {
				SwingUtilities.invokeAndWait(() -> {
					JOptionPane.showMessageDialog(dialog, content.replace("\n", System.lineSeparator()), title.replace("\n", System.lineSeparator()), JOptionPane.INFORMATION_MESSAGE);
				});
			} catch (Exception e) {
				error("Exception in Thread working(SwingWorker)", "%e%", (e instanceof InvocationTargetException) ? (Exception)e.getCause() : e, true);
			}

		}
		
	}
	
	

	/**
	 * Ask user to do confirm something with <code>JOptionPane{@link #showConfirmDialog(String, String, JDialog)}</code>. <br>
	 * This method checks if current thread is EDT or not, so you don't have to check it or avoid thread deadlock manually.
	 * */
	public static boolean confirm(String title, String message) {

		final JDialog dialog = new JDialog();
		dialog.setAlwaysOnTop(true);
		
		if (EventQueue.isDispatchThread()) {

			return showConfirmDialog(title, message, dialog);
			
		} else {
			
			final AtomicReference<Boolean> result = new AtomicReference<>();

			try {

				SwingUtilities.invokeAndWait(() -> {
					result.set(showConfirmDialog(title, message, dialog));
				});
				
				return result.get();

			} catch (Exception e) {
				error("Exception in Thread working(SwingWorker)",
						e.getClass().getName() + "-%e%\nI'll consider you chose \"no\"", (e instanceof InvocationTargetException) ? (Exception)e.getCause() : e, true);
			}

			return false;

		}

	}
	
	private static boolean showConfirmDialog(String title, String message, JDialog dialog) {
		return JOptionPane.showConfirmDialog(dialog, message, title,JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	} 
}
