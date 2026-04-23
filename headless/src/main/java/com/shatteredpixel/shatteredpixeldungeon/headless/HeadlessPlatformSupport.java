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

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.watabou.utils.PlatformSupport;

public class HeadlessPlatformSupport extends PlatformSupport {

	@Override
	public void updateDisplaySize() {
	}

	@Override
	public boolean isHeadless() {
		return true;
	}

	@Override
	public void updateSystemUI() {
	}

	@Override
	public boolean connectedToUnmeteredNetwork() {
		return true;
	}

	@Override
	public boolean supportsVibration() {
		return false;
	}

	@Override
	public void setupFontGenerators(int pageSize, boolean systemFont) {
		this.pageSize = pageSize;
		this.systemfont = systemFont;
	}

	@Override
	protected FreeTypeFontGenerator getGeneratorForString(String input) {
		return null;
	}

	@Override
	public String[] splitforTextBlock(String text, boolean multiline) {
		return new String[]{text};
	}
}
