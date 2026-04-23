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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeadlessScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

public class WndAIRun extends Window {

	private static final int WIDTH   = 120;
	private static final int MARGIN  = 2;
	private static final int BTN_H   = 16;
	private static final int SLIDER_H = 24;

	private HeroClass selectedClass = HeroClass.WARRIOR;
	private int selectedSteps       = 500;

	public WndAIRun() {
		super();

		float pos = MARGIN;

		// --- title ---
		RenderedTextBlock title = PixelScene.renderTextBlock("AI Run", 9);
		title.hardlight(Window.TITLE_COLOR);
		title.setPos(MARGIN + (WIDTH - MARGIN * 2 - title.width()) / 2f, pos);
		add(title);
		pos = title.bottom() + MARGIN * 2;

		// --- hero-class label ---
		RenderedTextBlock classLabel = PixelScene.renderTextBlock("Hero Class:", 6);
		classLabel.setPos(MARGIN, pos);
		add(classLabel);
		pos = classLabel.bottom() + MARGIN;

		// --- hero-class buttons (3 + 2 layout) ---
		HeroClass[] classes = HeroClass.values();

		// First row: up to 3 classes
		int cols1 = Math.min(3, classes.length);
		float btnW = (WIDTH - MARGIN * 2 - (cols1 - 1) * MARGIN) / (float) cols1;
		final RedButton[] classBtns = new RedButton[classes.length];
		for (int i = 0; i < cols1; i++) {
			final int idx = i;
			classBtns[i] = new RedButton(classes[i].name()) {
				@Override
				protected void onClick() {
					selectedClass = classes[idx];
					updateClassButtons(classBtns, idx);
				}
			};
			classBtns[i].setRect(MARGIN + i * (btnW + MARGIN), pos, btnW, BTN_H);
			add(classBtns[i]);
		}
		pos += BTN_H + MARGIN;

		// Second row: remaining classes
		if (classes.length > 3) {
			int cols2 = classes.length - 3;
			float btnW2 = (WIDTH - MARGIN * 2 - (cols2 - 1) * MARGIN) / (float) cols2;
			// Centre the second row
			float rowLeft = MARGIN + (WIDTH - MARGIN * 2 - (btnW2 * cols2 + (cols2 - 1) * MARGIN)) / 2f;
			for (int i = 3; i < classes.length; i++) {
				final int idx = i;
				classBtns[i] = new RedButton(classes[i].name()) {
					@Override
					protected void onClick() {
						selectedClass = classes[idx];
						updateClassButtons(classBtns, idx);
					}
				};
				classBtns[i].setRect(rowLeft + (i - 3) * (btnW2 + MARGIN), pos, btnW2, BTN_H);
				add(classBtns[i]);
			}
			pos += BTN_H + MARGIN;
		}

		// Default highlight: WARRIOR is index 0
		updateClassButtons(classBtns, 0);

		// --- steps slider ---
		OptionSlider stepsSlider = new OptionSlider("Steps", "100", "2000", 1, 20) {
			@Override
			protected void onChange() {
				// slider value 1-20 maps to 100-2000 in steps of 100
				selectedSteps = getSelectedValue() * 100;
			}
		};
		stepsSlider.setSelectedValue(selectedSteps / 100);
		stepsSlider.setRect(MARGIN, pos, WIDTH - MARGIN * 2, SLIDER_H);
		add(stepsSlider);
		pos += SLIDER_H + MARGIN;

		// --- start button ---
		RedButton btnStart = new RedButton("Start Run") {
			@Override
			protected void onClick() {
				hide();
				HeadlessScene.uiHeroClass = selectedClass;
				HeadlessScene.uiSteps     = selectedSteps;
				HeadlessScene.uiSeed      = null; // random seed
				ShatteredPixelDungeon.switchNoFade(HeadlessScene.class);
			}
		};
		btnStart.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BTN_H);
		add(btnStart);
		pos += BTN_H + MARGIN;

		resize(WIDTH, (int) pos);
	}

	private void updateClassButtons(RedButton[] btns, int selected) {
		for (int i = 0; i < btns.length; i++) {
			if (btns[i] != null) {
				btns[i].textColor(i == selected ? Window.TITLE_COLOR : Window.WHITE);
			}
		}
	}
}
