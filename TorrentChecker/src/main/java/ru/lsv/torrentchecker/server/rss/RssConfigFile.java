package ru.lsv.torrentchecker.server.rss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Кофигурационный файл загрузки из rss <br/>
 * 
 * @author s.lezhnev
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Slf4j
public class RssConfigFile {

	private final String rssLink;
	private final String beginPart;
	private final String[] mustContain;
	private final String quality;
	private transient String name;

	public static RssConfigFile load(File configFile) {
		Gson gson = new Gson();
		try (InputStreamReader in = new InputStreamReader(new FileInputStream(
				configFile), Charset.forName("UTF-8"))) {
			RssConfigFile res = gson.fromJson(in, RssConfigFile.class);
			res.name = configFile.getName();
			return res;
		} catch (FileNotFoundException e) {
			log.error("Rss configuration file not found - {}", configFile);
			return null;
		} catch (IOException e) {
			log.error("Error while reading rss configuration file - {}",
					configFile);
			return null;
		}
	}

}
