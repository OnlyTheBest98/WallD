from threading import Thread
import settings
import discord
from discord.ext import commands, tasks
import asyncio
import sys
from dataclasses import dataclass
from datetime import datetime
import typing
import http.server
import socketserver
import re

ALLOWED_FORMATS = ["png", "jpg", "jpeg"]
DATETIME_FORMAT = "%Y%m%d%H%M%S"
URL_REGEX = r"(https?://[^\s]+\.(" + "|".join(ALLOWED_FORMATS) + "))"

intents = discord.Intents().default()
client = commands.Bot(command_prefix="??", intents=intents)


@dataclass
class Category:
    id: int
    name: str


@dataclass
class Image:
    url: str
    image_format: str
    time: str

    def to_json(self) -> bytes:
        url = f"\"url\": \"{self.url}\""
        format = f"\"format\": \"{self.image_format}\""
        datetime = f"\"datetime\": \"{self.time}\""
        save_as = f"\"save_as\": \"{self.time}.{self.image_format}\""
        result = "{\n" + url + ",\n" + format + ",\n" + datetime + ",\n" + save_as + "\n" + "}"
        return result.encode("utf-8")


@dataclass
class Channel:
    id: int
    name: str
    category_id: typing.Optional[int]
    last_image: typing.Optional[Image] = None

    def id_name(self) -> str:
        if self.category_id:
            cat: typing.Optional[discord.CategoryChannel] = client.get_channel(self.category_id)
            if cat:
                return f"\"{self.id}\": \"{cat.name}: {self.name}\""
        return f"\"{self.id}\": \"{self.name}\""


categories: typing.List[Category] = []
text_channels: typing.List[Channel] = []


class HTTPImageRequestHandler(http.server.BaseHTTPRequestHandler):
    def __init__(self, request: bytes, client_address: tuple[str, int], server: socketserver.BaseServer) -> None:
        super().__init__(request, client_address, server)
        self.server_version = "0.0.2"

    def do_HEAD(self):
        self.send_error(405, "not supported", "please do not use this!")

    def do_GET(self):
        if self.path == "/image.json":
            self.get_image()
        elif self.path == "/categories.json":
            self.get_categories()
        elif self.path == "/channels.json":
            self.get_channels()
        else:
            self.send_error(405, "request not supported", "can only get /image|categories.json")

    def get_image(self):
        if not text_channels:
            self.send_error(404, "no text channels!", "Bot not fully started or no Text Channels on Server?")
            return
        # print("received headers: ", self.headers)
        requested_categories: typing.Set[int] = set()
        excluded_channels: typing.Set[int] = set()
        for k, v in self.headers.items():
            if k == "categories":
                for cat in v.split(","):
                    try:
                        requested_categories.add(int(cat))
                    except ValueError:
                        self.send_error(400, "Invalid Header", "Maybe Category was not an ID?")
                        return
            elif k == "excluded-channels":
                for channel_id in v.split(","):
                    try:
                        excluded_channels.add(int(channel_id))
                    except ValueError:
                        self.send_error(400, "Invalid Header", "Maybe Channel was not an ID?")
                        return
            elif k == "limits":
                # TODO: DEPRECATED will be removed in the future because of performance!
                for channel_name in v.split(","):
                    for text_channel in text_channels:
                        if channel_name == text_channel.name:
                            excluded_channels.add(text_channel.id)

        # search for fitting Image
        best_image: typing.Optional[Image] = None
        for channel in text_channels:
            if channel.id in excluded_channels:
                continue
            if len(requested_categories) > 0:
                if channel.category_id is None and 0 not in requested_categories:
                    continue
                if channel.category_id is not None and channel.category_id not in requested_categories:
                    continue

            if channel.last_image is not None:
                # image has to be considered
                if best_image is None:
                    best_image = channel.last_image
                else:
                    if best_image.time < channel.last_image.time:
                        best_image = channel.last_image

        if best_image is None:
            self.send_error(404, "no suitable image found!")
        else:
            self.send_response(200, "OK")
            self.send_header("Content-type", "application/json; charset=utf-8")
            self.end_headers()
            self.wfile.write(best_image.to_json())

    def get_categories(self):
        if not categories:
            self.send_error(404, "no categories!", "Bot not fully started?")
            return
        result = "{\n"
        for cat in categories:
            result += f"\"{cat.id}\": \"{cat.name}\",\n"
        result += f"\"0\": \"Channels without Category\"\n"
        result += "}"
        self.send_response(200, "OK")
        self.send_header("Content-type", "application/json; charset=utf-8")
        self.end_headers()
        self.wfile.write(result.encode("utf-8"))

    def get_channels(self):
        if not text_channels:
            self.send_error(404, "no text channels!", "Bot not fully started or no Text Channels on Server?")
            return
        requested_categories: typing.Set[int] = set()
        for k, v in self.headers.items():
            if k == "categories":
                for cat in v.split(","):
                    try:
                        requested_categories.add(int(cat))
                    except ValueError:
                        self.send_error(400, "Invalid Header", "Maybe Category was not a number?")
                        return
        result = "{\n"
        for channel in text_channels:
            if len(requested_categories) > 0:
                if channel.category_id is None and 0 in requested_categories:
                    result += channel.id_name() + ",\n"
                elif channel.category_id is not None and channel.category_id in requested_categories:
                    result += channel.id_name() + ",\n"
            else:
                result += channel.id_name() + ",\n"
        if result.endswith(",\n"):
            result = result[:-2] + "\n"
        result += "}"
        self.send_response(200, "OK")
        self.send_header("Content-type", "application/json; charset=utf-8")
        self.end_headers()
        self.wfile.write(result.encode("utf-8"))

    # def log_message(self, *arg):
    #     pass  # override log function to avoid logging all request in console


@tasks.loop(seconds=5)
async def mainloop():
    # TODO: need this later for sync job: server changed channels or categories
    pass


@client.event
async def on_ready():
    global categories, text_channels
    print(f"{client.user} has connected to Discord!")
    await client.change_presence(activity=discord.Game("Beta Version Image Bot"))
    channels: typing.List[discord.abc.GuildChannel] = []
    for guild in client.guilds:
        if settings.SERVER_ID and settings.SERVER_ID != guild.id:
            continue
        for channel in guild.channels:
            channels.append(channel)
    for channel in channels:
        if isinstance(channel, discord.CategoryChannel):
            categories.append(Category(channel.id, channel.name))
        if isinstance(channel, discord.TextChannel):
            text_channels.append(Channel(channel.id, channel.name, channel.category_id))
    print("Channels processed")


@client.event
async def on_message(message: discord.Message):
    global text_channels
    if not message.guild:
        return
    guild_id = message.guild.id  # type: ignore
    if settings.SERVER_ID and settings.SERVER_ID != guild_id:
        return

    # find image from message content
    image_urls = re.findall(URL_REGEX, message.content)
    if len(image_urls) > 0:
        image_url, image_format = image_urls[0]
        last_image = Image(image_url, image_format, datetime.now().strftime(DATETIME_FORMAT))
        print("new image from url in message:", last_image)
        for channel in text_channels:
            if channel.id == message.channel.id:
                channel.last_image = last_image

    # get image from attachment
    for at in message.attachments:
        image_format = at.filename.split(".")[-1]
        if image_format in ALLOWED_FORMATS:
            last_image = Image(at.url, image_format, datetime.now().strftime(DATETIME_FORMAT))
            print("new image:", last_image)
            for channel in text_channels:
                if channel.id == message.channel.id:
                    channel.last_image = last_image


if __name__ == "__main__":
    if not settings.DISCORD_TOKEN:
        print("error: DISCORD_TOKEN not found! create a bot first!", file=sys.stderr)
        exit(11)
    if not settings.SERVER_ID:
        print("security risk: Server ID not found activate developer mode and set the server ID to the desired server")
    mainloop.start()
    http_server = http.server.ThreadingHTTPServer(("", 8000), HTTPImageRequestHandler)
    http_thread = Thread(target=http_server.serve_forever, name="HTTP Server Thread")
    http_thread.start()
    loop = asyncio.get_event_loop_policy().get_event_loop()
    try:
        loop.run_until_complete(client.start(settings.DISCORD_TOKEN))
    except KeyboardInterrupt:
        http_server.shutdown()
        mainloop.stop()
        loop.run_until_complete(client.close())