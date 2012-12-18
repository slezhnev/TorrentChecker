/**
 * The async counterpart of <code>TorrentCheckerService</code>.
 */
package ru.lsv.torrentchecker.client;

import ru.lsv.torrentchecker.shared.WorkingResult;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>TorrentCheckerService</code>.
 */
public interface TorrentCheckerServiceAsync {
	/**
	 * см. описание сервиса
	 * 
	 * @param callback
	 *            см. описание сервиса
	 * @see TorrentCheckerService#getResult()
	 */
	void getResult(AsyncCallback<WorkingResult> callback);

	/**
	 * см. описание сервиса
	 * 
	 * @param callback
	 *            см. описание сервиса
	 * @see TorrentCheckerService#refreshAll()
	 */
	void refreshAll(@SuppressWarnings("rawtypes") AsyncCallback callback);

	/**
	 * см. описание сервиса
	 * 
	 * @param callback
	 *            см. описание сервиса
	 * @see TorrentCheckerService#removeFromControl(String)
	 */
	void removeFromControl(String[] torrents, AsyncCallback<Integer> callback);
}
