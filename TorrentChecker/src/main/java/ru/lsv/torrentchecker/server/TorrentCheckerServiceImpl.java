package ru.lsv.torrentchecker.server;

import java.io.File;

import ru.lsv.torrentchecker.client.TorrentCheckerService;
import ru.lsv.torrentchecker.shared.WorkingResult;
import ru.lsv.torrentchecker.shared.WorkingResult.FileResult;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
public class TorrentCheckerServiceImpl extends RemoteServiceServlet implements
		TorrentCheckerService {

	/**
	 * For serialization
	 */
	private static final long serialVersionUID = 514604118973830888L;

	@Override
	public WorkingResult getResult() {
		return Commons.getWorkingResult();
	}

	@Override
	public void refreshAll() {
		// Запускаем обновление
		TorrentCheckerSheduler.service();
	}

	@Override
	public Integer removeFromControl(String[] torrents) {
		int res = 0;
		for (String torrent : torrents) {
			if (torrent != null) {
				// Ищем файл в torrents
				File torr = new File(Commons.getTorrentsInQueue() + torrent);
				if (!torr.exists()) {
					res = -1;
				} else {
					// Удаляем
					// Надо бы еще удалить из списка WorkingResults
					if (Commons.getWorkingResult() != null) {
						for (FileResult file : Commons.getWorkingResult()
								.getFilesOnControl()) {
							if (torrent.equals(file.getName())) {
								Commons.getWorkingResult().getFilesOnControl()
										.remove(file);
								break;
							}
						}
					}
					torr.delete();
					// Дополнительно - было бы неплохо удалить его из
					// torrentsInQueue
					File torrInQueue = new File(Commons.getTorrentsInQueue()
							+ torrent);
					if (torrInQueue.exists()) {
						torrInQueue.delete();
					}
				}
			}
		}
		return res;
	}
}
