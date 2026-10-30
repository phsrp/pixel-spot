# Third-party notices

## PixelPlayer

Parts of this app's interface are adapted from **PixelPlayer** (https://github.com/PixelPlayerHQ/PixelPlayer),
taken from commit `d51105d2ccc24220ce1e5c74e27ff5d20749b2ee` (11 May 2026). At that commit the project was released
under the MIT licence. PixelPlayer switched to a proprietary licence on 12 May 2026; nothing from after that
change is used here.

Adapted files live in `app/src/main/java/dev/vibe/pixelspot/pixelplay/` (package renamed, small changes to fit
Spotify). Layout ideas from PixelPlayer's full player and song list are reused in `ui/FullPlayer.kt` and
`ui/Common.kt`. Icons in `app/src/main/res/drawable/` (`rounded_*`, `round_*`, `ic_music_placeholder`) and the
font `app/src/main/res/font/gflex_variable.ttf` come from the same commit.

The full MIT licence text from that commit is in `licenses/PixelPlayer-LICENSE.txt`:

> Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
> documentation files (the "Software"), to deal in the Software without restriction […]. The above copyright
> notice and this permission notice shall be included in all copies or substantial portions of the Software.

## Spotify App Remote SDK

`app/libs/spotify-app-remote-release-0.8.0.aar` is Spotify's official SDK, from
https://github.com/spotify/android-sdk/releases, under the Apache License 2.0
(`licenses/Spotify-Android-SDK-LICENSE.txt`). Use is subject to the Spotify Developer Terms.

## Google Sans Flex

`app/src/main/res/font/gflex_variable.ttf` is Google Sans Flex, under the SIL Open Font License 1.1
(`licenses/GoogleSansFlex-OFL.txt`).

## Theme colours

The app's themes use the published colour palettes of Rosé Pine, Gruvbox, Catppuccin, Nord, Atom One and
Tailwind CSS (Zinc).

## Trademarks

Spotify is a trademark of Spotify AB. Pixel Spot isn't made by, endorsed by or affiliated with Spotify.
