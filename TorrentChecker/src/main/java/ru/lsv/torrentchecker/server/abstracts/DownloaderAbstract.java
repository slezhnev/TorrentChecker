package ru.lsv.torrentchecker.server.abstracts;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import ru.lsv.torrentchecker.server.Commons;
import ru.lsv.torrentchecker.shared.User;
import ru.lsv.torrentchecker.shared.WorkingResult.FileResult;

/**
 * Абстрактный класс загрузчика для ТИПА файлов - торренты или rss
 * 
 * @author s.lezhnev
 */
public abstract class DownloaderAbstract {

	public DownloaderAbstract() {
		super();
	}

	/**
	 * Осуществляет проверку обновления указанных файлов <br>
	 * 
	 * @param checkedFiles
	 *            Файлы, обновление которых надо проверить
	 * @param credentials
	 *            Имена пользователей и паролей к ресурсам
	 * @param pathToDownload
	 *            Куда загружать временно .torrent-файл
	 * @param autoloadPath
	 *            Куда выкладывать .torrent-файл, если он изменился
	 * @return Результат обработки файлов
	 */
	public abstract List<FileResult> check(List<File> checkedFiles,
			Map<String, User> credentials, String pathToDownload,
			String autoloadPath);

	/**
	 * Выполняет отсылку письма о том, что найдено чойта новое
	 * 
	 * @param name
	 *            Имя файла торрента
	 * @param newFiles
	 *            Список новых файлов в торренте (null - если торрент просто
	 *            новый)
	 */
	public void fireMailAnnounce(String name, List<String> newFiles) {
		// Первое - нам надо проверить наличие пачки дополнительных
		// пропертей в credentials
		if ((Commons.getMailCredentials() != null)
				&& (Commons.getSendEmailTo() != null)) {
			Properties props = System.getProperties();
			// props.put("mail.smtp.starttls.enable", "false");
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.port", "465");
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.debug", "true");
			props.put("mail.smtp.socketFactory.port", "465");
			props.put("mail.smtp.socketFactory.class",
					"javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.socketFactory.fallback", "false");
			//
			Session session = Session.getDefaultInstance(props,
					new javax.mail.Authenticator() {
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(Commons
									.getMailCredentials().getUserName(),
									Commons.getMailCredentials().getPassword());
						}
					});
			MimeMessage message = new MimeMessage(session);
			try {
				message.setFrom(new InternetAddress(Commons
						.getMailCredentials().getUserName()));
				message.addRecipient(Message.RecipientType.TO,
						new InternetAddress(Commons.getSendEmailTo()));
				message.setSubject("Обновление " + name);
				StringBuffer str = new StringBuffer();
				str.append("<html><head><title>").append(message.getSubject())
						.append("</title></head><body>");
				if (newFiles != null) {
					str.append("Новые файлы в торренте ").append(name)
							.append(": <p/>");
					for (String file : newFiles) {
						str.append(file).append("<br/>");
					}
				} else {
					str.append("Новый торрент - добавлен в закачки");
				}
				str.append("\n</body></html>");
				message.setDataHandler(new DataHandler(new ByteArrayDataSource(
						str.toString(), "text/html")));
				message.setHeader("X-Mailer", "Torrent checker");
				message.setSentDate(new Date());
				Transport.send(message);
			} catch (AddressException e) {
				// Do nothing
			} catch (MessagingException e) {
				// Do nothing
				e.printStackTrace();
			} catch (IOException e) {
				// Do nothing
			}
		}
	}

}