from threading import Thread
import settings
import discord
from discord.ext import commands, tasks
import asyncio
import sys
from dataclasses import dataclass
import http.server
import socketserver

ALLOWED_FORMATS = ["png", "jpg", "jpeg"]

intents = discord.Intents().default()
client = commands.Bot(command_prefix="??", intents=intents)
last_image = None


@dataclass
class Image:
    url: str
    channel: str

    def to_json(self) -> bytes:
        url = f"\"url\": \"{self.url}\""
        channel = f"\"channel\": \"{self.channel}\""
        result = "{\n" + channel + ",\n" + url + "\n" + "}"
        return result.encode("utf-8")


class HTTPImageRequestHandler(http.server.BaseHTTPRequestHandler):
    def __init__(self, request: bytes, client_address: tuple[str, int], server: socketserver.BaseServer) -> None:
        super().__init__(request, client_address, server)
        self.server_version = "0.0.1"

    def do_HEAD(self):
        self.send_error(405, "not supported", "please do not use this!")

    def do_GET(self):
        if self.path != "/image.json":
            self.send_error(405, "not supported", "can only get /image.json")
            return
        if not last_image:
            self.send_error(404, "no last image found!", "Bot needs to see a new image!")
            return
        self.send_response(200, "OK")
        self.send_header("Content-type", "application/json; charset=utf-8")
        self.end_headers()
        self.wfile.write(last_image.to_json())


@tasks.loop(seconds=5)
async def mainloop():
    # maybe need this later
    pass


@client.event
async def on_ready():
    print(f"{client.user} has connected to Discord!")
    await client.change_presence(activity=discord.Game("Alpha Version Image Bot"))


@client.event
async def on_message(message: discord.Message):
    global last_image
    if not message.guild:
        return
    guild_id = message.guild.id  # type: ignore
    if settings.SERVER_ID and settings.SERVER_ID != guild_id:
        return
    for at in message.attachments:
        if at.filename.split(".")[-1] in ALLOWED_FORMATS:
            last_image = Image(at.url, message.channel.name)
    print("last image:", last_image)


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