/**
 * Представление торрента в таблице отображения
 */
package ru.lsv.torrentchecker.client;

import ru.lsv.torrentchecker.shared.WorkingResult;
import ru.lsv.torrentchecker.shared.WorkingResult.FileResult;

/**
 * @author admin
 * 
 */
public class TorrentOnControl {

	/**
	 * Выделен или нет торрент
	 */
	private boolean isChecked;
	/**
	 * Результат обработки торрента
	 */
	private WorkingResult.FileResult result;

	/**
	 * Default constructor
	 * 
	 * @param result
	 *            Результаты обработки торрента
	 */
	public TorrentOnControl(FileResult result) {
		super();
		this.isChecked = false;
		this.result = result;
	}

	/**
	 * @return the isChecked
	 */
	public boolean isChecked() {
		return isChecked;
	}

	/**
	 * @param isChecked
	 *            the isChecked to set
	 */
	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}

	/**
	 * @return the result
	 */
	public WorkingResult.FileResult getResult() {
		return result;
	}

}
