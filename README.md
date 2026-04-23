Don't expect any pushed commits any time soon unless I got something to show.



# Shattered Pixel Dungeon

[Shattered Pixel Dungeon](https://shatteredpixel.com/shatteredpd/) is an open-source traditional roguelike dungeon crawler with randomized levels and enemies, and hundreds of items to collect and use. It's based on the [source code of Pixel Dungeon](https://github.com/00-Evan/pixel-dungeon-gradle), by [Watabou](https://watabou.itch.io/).

Shattered Pixel Dungeon currently compiles for Android, iOS, and Desktop platforms. You can find official releases of the game on:

[![Get it on Google Play](https://shatteredpixel.com/assets/images/badges/gplay.png)](https://play.google.com/store/apps/details?id=com.shatteredpixel.shatteredpixeldungeon)
[![Download on the App Store](https://shatteredpixel.com/assets/images/badges/appstore.png)](https://apps.apple.com/app/shattered-pixel-dungeon/id1563121109)
[![Steam](https://shatteredpixel.com/assets/images/badges/steam.png)](https://store.steampowered.com/app/1769170/Shattered_Pixel_Dungeon/)<br>
[![GOG.com](https://shatteredpixel.com/assets/images/badges/gog.png)](https://www.gog.com/game/shattered_pixel_dungeon)
[![Itch.io](https://shatteredpixel.com/assets/images/badges/itch.png)](https://shattered-pixel.itch.io/shattered-pixel-dungeon)
[![Github Releases](https://shatteredpixel.com/assets/images/badges/github.png)](https://github.com/00-Evan/shattered-pixel-dungeon/releases)

If you like this game, please consider [supporting me on Patreon](https://www.patreon.com/ShatteredPixel)!

## Headless (logic-only)

Run a minimal headless simulation without OpenGL:

```
./gradlew :headless:run -Dheadless.steps=300 -Dheadless.heroClass=WARRIOR -Dheadless.seed=demo
```

Optional properties: `headless.slot` (1-based save slot number), `headless.basePath` (default `/tmp/shpd-headless/`), `headless.version`, and `headless.versionCode`.
