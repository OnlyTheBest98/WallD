# WallD
Discord Image Bot


## How to Install
- Downlaod the App Automate from the Play Store
- Download WallDSettings.flo and WallD.flo from this github
- Import those files in the App Automate
- Run WallD Settings
- Run WallD
- Have fun

## Changelog

### Version 0.0.2
- The bot server backend now keeps track of channels and categories on the server.
- On the client side the program is split between two flows: the setting flow and the main flow.
  Do not run the main flow and the settings at the same time.
  When you want to change the settings stop the main flow first.
- The client can select specific categories which they want to receive images from.
- Channels which do not belong to a category are grouped in a special group.
- After selecting the categories you can exclude channels from showing images.
- Images are saved with the correct file ending (every image before got .png even when it was .jpg).
- Images get a timestamp from when it was posted in the discord.

### Version 0.0.1
First working version with hardcoded limits availiable.