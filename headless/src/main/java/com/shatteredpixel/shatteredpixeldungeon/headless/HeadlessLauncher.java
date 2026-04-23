/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.headless;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeadlessScene;
import com.watabou.noosa.Game;

public class HeadlessLauncher {

	public static void main(String[] args) {
		Game.version = System.getProperty("Specification-Version", "0.0.0-headless");
		try {
			Game.versionCode = Integer.parseInt(System.getProperty("Implementation-Version", "0"));
		} catch (NumberFormatException e) {
			Game.versionCode = 0;
		}

		HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
		config.renderInterval = 1f / 60f;

		new HeadlessApplication(
				new ShatteredPixelDungeon(new HeadlessPlatformSupport(), HeadlessScene.class),
				config);
	}
}
