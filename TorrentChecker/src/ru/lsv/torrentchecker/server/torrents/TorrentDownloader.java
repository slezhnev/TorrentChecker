/**
 * �����, �������������� �������� ��������
 */
package ru.lsv.torrentchecker.server.torrents;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.lsv.torrentchecker.server.Commons;
import ru.lsv.torrentchecker.server.torrents.impl.NNMClubDownloader;

import com.turn.ttorrent.bcodec.BDecoder;
import com.turn.ttorrent.bcodec.BEValue;
import com.turn.ttorrent.bcodec.InvalidBEncodingException;

/**
 * �����, �������������� �������� ��������
 * 
 * @author admin
 */
public class TorrentDownloader {

	/**
	 * ����� ������ �����������
	 */
	List<TorrentDownloaderInterface> downloaders = new ArrayList<>();

	/**
	 * Default constructor
	 */
	public TorrentDownloader() {
		// ������� ���������� ������
		downloaders.add(new NNMClubDownloader());
	}

	/**
	 * ������������ �������� ���������� ���������� .torrent-����� <br>
	 * ���� ���� ��������� - ��: <br>
	 * 1. �� ����� ������� �� ����� torrentFile <br>
	 * 2. �� ����� ���������� � autoloadPath <br>
	 * �����, �� ���������� ���� "comment" � �������� URL - ������ ������������
	 * *
	 * 
	 * @param torrentFile
	 *            .torrent-����, ���������� �������� ���� ���������
	 * @param credentials
	 *            ����� ������������� � ������� � ��������
	 * @param pathToDownload
	 *            ���� ��������� �������� .torrent-����
	 * @param autoloadPath
	 *            ���� ����������� .torrent-����, ���� �� ���������
	 * @throws IOException
	 *             � ������ ������������� ������� � �������
	 */
	public void check(File torrentFile, Map<String, User> credentials,
			String pathToDownload, String autoloadPath) throws IOException,
			TorrentDownloaderException {
		// ������ - ������ .torrent ����
		Map<String, BEValue> decoded = BDecoder.bdecode(
				new FileInputStream(torrentFile)).getMap();
		if (decoded.containsKey("comment")) {
			URL torrentUrl = null;
			try {
				torrentUrl = new URL(decoded.get("comment").getString());
			} catch (MalformedURLException e) {
				throw new TorrentDownloaderException("Malformed URL in comment");
			}
			if (torrentUrl != null) {
				// ������� �� ���� host
				String torrentHost = torrentUrl.getHost();
				// ������ - ���� credentials
				if (!credentials.containsKey(torrentHost)) {
					throw new TorrentDownloaderException(
							"Cannot find credentials for " + torrentHost);
				}
				// ������ ������ ������ ��������������� ���������� downloader'�
				TorrentDownloaderInterface implDownloader = null;
				for (TorrentDownloaderInterface impl : downloaders) {
					if ((impl.getResource() != null)
							&& (impl.getResource().equals(torrentHost))) {
						implDownloader = impl;
						break;
					}
				}
				if (implDownloader == null) {
					throw new TorrentDownloaderException(
							"Cannot find downloader for " + torrentHost);
				}
				// ����� �������������. ��������� ����� �� ��������� �����
				// ��������
				String downloadedTorrent = implDownloader.downloadTorrent(
						torrentUrl, credentials.get(torrentHost).getUserName(),
						credentials.get(torrentHost).getPassword(),
						pathToDownload);
				if (downloadedTorrent != null) {
					// ������� �������� - � �� ���� �� ����� ����?
					File downloadedTorrentFile = new File(downloadedTorrent);
					if (downloadedTorrentFile.exists()) {
						// ������ - ���� �� ���� ������� �������� ����� ������
						// �� ����. ���� ��� ��������� - ������ ������ ��
						// ������. ������ ����� ���������
						if (org.apache.commons.io.FileUtils
								.checksumCRC32(torrentFile) != org.apache.commons.io.FileUtils
								.checksumCRC32(downloadedTorrentFile)) {
							// �������. �������� � ���.
							// � ���������� - ��� ���� ����� ��� � ��� ������
							Set<String> torrentFiles = getFilesFromDecoded(
									torrentFile, decoded);
							// ������ ����� �������� ����
							Map<String, BEValue> decodedDownloaded = BDecoder
									.bdecode(
											new FileInputStream(
													downloadedTorrentFile))
									.getMap();
							// �������� ������ ������ �� ����
							Set<String> downloadedTorrentFiles = getFilesFromDecoded(
									downloadedTorrentFile, decodedDownloaded);
							List<String> newFiles = new ArrayList<>();
							for (String file : torrentFiles) {
								if (!downloadedTorrentFiles.contains(file)) {
									newFiles.add(file);
								}
							}
							// ��������� �� ��������� ����-�� ������
							if (newFiles.size() > 0) {
								// �������! ������ � ��������� �������� ����
								// ���-�� �����!
								// ������ - ��������� �������� � ���, ���
								// ������� �����
								fireMailAnnounce(
										downloadedTorrentFile.getName(),
										newFiles);
								// ������� ������ ����
								torrentFile.delete();
								// �������� �� ��� ����� ����� ����
								org.apache.commons.io.FileUtils.copyFile(
										downloadedTorrentFile, torrentFile);
								// ������ �������� ���� �� ���� � autoload
								org.apache.commons.io.FileUtils.copyFile(
										torrentFile,
										new File(Commons.getAutoloadPath()
												+ File.pathSeparator
												+ torrentFile.getName()));
							}
						}
						// ������� ��������� �������
						downloadedTorrentFile.delete();
					}
				}
			}
		}
	}

	/**
	 * ��������� ������� ������ � ���, ��� ������� ����� �����
	 * 
	 * @param name
	 *            ��� ����� ��������
	 * @param newFiles
	 *            ������ ����� ������ � ��������
	 */
	private void fireMailAnnounce(String name, List<String> newFiles) {
		// TODO ��������

	}

	/**
	 * ��������� ������ ������ �� ��������������� ��������
	 * 
	 * @param torrentFile
	 *            ���� � ��������� (����� ��� ��������� ������������ ���������)
	 * @param decoded
	 *            �������������� ������
	 * @return ������ ������ � ��������
	 * @throws InvalidBEncodingException
	 */
	private Set<String> getFilesFromDecoded(File torrentFile,
			Map<String, BEValue> decoded) throws InvalidBEncodingException {
		Set<String> torrentFiles = new HashSet<>();
		// ��������� ������ ������ �� �������� ��������
		if (decoded.containsKey("files")) {
			for (BEValue file : decoded.get("files").getList()) {
				Map<String, BEValue> fileInfo = file.getMap();
				StringBuilder path = new StringBuilder();
				for (BEValue pathElement : fileInfo.get("path").getList()) {
					path.append(File.separator).append(pathElement.getString());
				}
				torrentFiles.add(path.toString());
			}
		} else {
			torrentFiles.add(torrentFile.getName());
		}
		return torrentFiles;
	}

	/**
	 * ��������������� �����, �������������� �������� ���� username - password
	 * 
	 * @author admin
	 * 
	 */
	public static class User {

		/**
		 * ��� ������������
		 */
		private String userName;
		/**
		 * ������
		 */
		private String password;

		/**
		 * Default constructor
		 * 
		 * @param userName
		 * @param password
		 */
		public User(String userName, String password) {
			this.userName = userName;
			this.password = password;
		}

		/**
		 * @return the userName
		 */
		public String getUserName() {
			return userName;
		}

		/**
		 * @return the password
		 */
		public String getPassword() {
			return password;
		}

	}

	/**
	 * ��������������� ����� ��� exception'��, ������������ ��� ���������
	 * ��������
	 * 
	 * @author admin
	 */
	public static class TorrentDownloaderException extends Exception {

		/**
		 * for serialization
		 */
		private static final long serialVersionUID = -2771650402084251961L;

		/**
		 * @param message
		 */
		public TorrentDownloaderException(String message) {
			super(message);
		}

	}

}
