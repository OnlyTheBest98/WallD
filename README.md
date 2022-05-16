# WallD
Discord Image Bot


## How to Install
- Downlaod the App Automate from the Play Store
- Download WallDSettings.flo and WallD.flo from this github
- Import those files in the App Automate
- Run WallD Settings
- Run WallD
- Have fun

## Troubleshooting
Q: It says I need Premium?
A: Please stop all running Flows thewn run the Settings flow and when finished run the main program.

Q: Images won't load?
A: Delete your old Flows and download the 2 new .flo files and import them in Automate. Maybe you were running an old version.
A: Have you run the new Settings Flow before running WallD? The old settings won't work in the new version.
A: Have you seleced the categories you WANT TO SEE and the channels you do NOT WANT TO SEE? Do not select all channels to be excluded!
A: Maybe that is a new problem you can message me on Discord.

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
