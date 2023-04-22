# G-Presets

G-Presets is a G-Earth extension that can export and import creations inside Habbo, with emphasis on Wired, but it works for anything. Download it from the [G-ExtensionStore](https://github.com/sirjonasxx/G-ExtensionStore)!

Watch the youtube playlist for G-Presets: https://www.youtube.com/playlist?list=PL-dmL-QOkwvz03PNFQH7pFFDPu93F6XOE

![image](https://user-images.githubusercontent.com/36828922/133943406-725fb841-ee6c-4342-9f2d-b0ecf261e49e.png)


## Exporting a preset

To export a preset, type `:exportpreset` or `:ep`. You will be asked to select the area of the preset, and to name the preset.

You can also use `:exportpreset all` or `:ep all` to export the entire room

The extension will record all wired updates you make. If it does not know the internals of a wired box at time of exporting, it will fetch these before exporting the room.


## Importing a preset

To import a preset, type `:importpreset` or `:ip`. You need to parse the catalog & inventory before importing a preset. You also need to make sure a stack tile is found (the stack tile is customizable)

It will ask you to select an **unoccupied space**. It will use the selected space to change states of furniture by toggling it. Some furniture require to be on no other furniture to be toggled, and some furniture require the user to be standing next to the item in order to toggle it (like floor switches), so it is recommended to do choose a location next to your avatar, where no other furniture will be, and that is big enough to contain all furniture that need state toggles.

Secondly, it will ask you to select the root location of the preset. You can also define this in the command itself (for example `:importpreset 0,5` will import at `x=0` and `y=5`). If you use the tool to export and import entire rooms, you will probably be using `:importpreset 0,0` a lot

Afterwards, it will start placing the furniture in the corner of the room, setup wired, and move the furniture to the correct location. Placing furniture may take a while if the preset contains a lot of furniture!


## Checking availability of a preset

Before importing a preset, it may be useful to double check if you have all furniture (in inventory and/or BC catalog if you opted-in for that). Click the `Check Availability` button and you will see the following:

![image](https://user-images.githubusercontent.com/36828922/133943499-23cfe5f4-c11a-42d4-b151-158d47bd592f.png)

If you start building without having all furniture, the import will fail right away. If you want to continue building without all furniture, you need to select that option in `Settings`

## Settings

![image](https://user-images.githubusercontent.com/36828922/134006632-4394a8f9-a6be-4328-bb45-5ed80da36d2c.png)

Use the settings tab to:
* Select the **stacktile** you want to use. Make sure all items fit on this stacktile for the initial placements. If you have more stacktiles in the room, they will also be used if they are needed for movements of the furniture
* Select the **Item source**, speaks for itself
* Increase the ratelimit if needed, using the extension will become slower
* Export rooms without exporting wired, for example if you have no rights
* Allow building without having all furniture available if you're sure you want this
* _Open the presets folder to rename/add/delete presets manually, afterwards, click "Reload Presets" to see the changes_
* _Open the selected preset in a text editor to make adjustments or inspect the preset if you feel like it_


## Preset configuration

This is/will be a more advanced tab for wired-specific extra configurations. Currently, it only allows you to replace a furniture in the preset with an already existing furniture.

For example: the `Wired timer` uses a colortile to control if the timer should reset, pause or start.
In that case, you may want to use a colortile that is already in the room instead of a new one, to allow for easier "plug and play". If you want this, you can specify it in this tab. Every furniture has a unique name assigned, you can use the button to open a text editor in `Settings` to find out what name you should use

![image](https://user-images.githubusercontent.com/36828922/133943714-0d2937e4-f151-4db4-91c5-cc50d7102e38.png)


# Default presets

I included a few simple presets you can already use. I may include more of them in the future, you can also PR one if you feel like it's basic enough

## Bopper
Teleports a user when the glowball is moved next to him

![image](https://user-images.githubusercontent.com/36828922/133943775-91dd9f58-34f5-4c09-bc4a-3c45a4dc2af2.png)


## Bopper with timeout
The same thing as `Bopper`, but automatically moves back the glowball if it did not hit any user within 2-3 seconds

![image](https://user-images.githubusercontent.com/36828922/133943780-0bcf4ec7-aed2-4472-91b3-44b9dfbd020e.png)


## Wired timer
A simple manual wired timer. Controlled with a colortile (`lightblue = reset`, `yellow = running`, `any other state = pause`)

![image](https://user-images.githubusercontent.com/36828922/133943817-1a0a6b5a-f5dc-4f69-826f-14fe495c7d70.png)


## Score counter
Increments the counter by one when the candle is turned on

![image](https://user-images.githubusercontent.com/36828922/133943851-aea42148-3cff-46f0-b074-f6694336890c.png)
