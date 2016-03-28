/**
 * Exception при обработке торрента
 */
package ru.lsv.torrentchecker.server;

/**
 * Exception при обработке торрента
 * 
 * @author admin
 */
public class DownloaderException extends Exception {

	/**
	 * For serializatiod
	 */
	private static final long serialVersionUID = -7175114806920914403L;

	/**
	 * Default constructor
	 * 
	 * @param arg0
	 */
	public DownloaderException(String arg0) {
		super(arg0);
	}

}
