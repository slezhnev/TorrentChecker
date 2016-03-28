TorrentChecker
==============

Проверка обновления торрента с его дальнейшей передачей для загрузки

Поддерживаются следующие торренты:
nmnclib.to
ipv6.nnmclub.to (для доступа необходим IPv6)
torrent.rus.ec
rutracker.org
lostfilm.tv

Загрузка с lostfilm.tv осуществляется через rss.
Для работы с загрузкой через rss вместо файла торрента в inqueue необходимо
положить конфигурационный файл [имя загрузки].rss в формате json со следующими параметрами:
{
"rssLink": "http://lostfilm.tv/rssdd.xml",
"beginPart": "Викинги",
"mustContain": ["[1080p]"],
"quality": "1080p WEB-DLRip"
}
Где:
beginPart - начало названия сериала,
mustContain - список дополнительных строк, которые должны содержаться в названии 
(в примере выше - будут обрабатываться только файлы в 1080)
quality - качество, которое будет скачано (тип качества можно посмотреть при загрузке
торрента с retre.org)

Пути настраиваются в web.xml (параметр "storagePath")
В storagePath должно присутствовать два файла:
paths.properties и credentials.properties.

paths.properties:
temp=[path] - временное хранилище файлов
autoload=[path] - каталог автозагрузки вашего torrent-клиента
torrents=[path] - на текущий момент не используется (но требует наличия)
torrents_inqueue=[path] - каталог хранения контролируемых торрентов
mail_to=mail@example.com - адрес почты, куда будут отправляться анонсы

credentials.properties:
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
<comment>credentials</comment>
<entry key="count">3</entry>				- общее количество
<entry key="url0">torrent.rus.ec</entry>	        - url сайта
<entry key="name0">user0</entry>			- имя пользователя для этого сайта
<entry key="password0">password0</entry> 		- пароль пользователя для этого сайта
<entry key="url1">nnm-club.me</entry>
<entry key="name1">user1</entry>
<entry key="password1">password1</entry>
<entry key="url2">ipv6.nnm-club.me</entry>
<entry key="name2">user2</entry>
<entry key="password2">password2</entry>
<entry key="mail.name">example@gmail.com</entry>	- имя пользователя GMail, от которого будут отправляться анонсы
<entry key="mail.password">passwordGmail</entry>	- пароль пользователя GMail, от которого будут отправляться анонсы
</properties>


Для создания проекта под Eclipse используйте "gradle eclipse"

Для сборки проекта - "gradle build"

Примечание: для предпочтительного использования IPv6 необходимо добавить "-Djava.net.preferIPv6Addresses=true" при запуске контейнера