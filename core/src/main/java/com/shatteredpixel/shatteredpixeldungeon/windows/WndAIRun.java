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

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeadlessScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.ActionIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class WndAIRun extends Window {

	private static final int WIDTH    = 140;
	private static final int MARGIN   = 2;
	private static final int BTN_H    = 16;
	private static final int SLIDER_H = 24;

	// -----------------------------------------------------------------------
	// Current configuration state (persists across open/close so the window
	// "remembers" what the user set last time within the same session).
	// -----------------------------------------------------------------------
	private HeroClass selectedClass   = HeroClass.WARRIOR;
	private boolean   randomizeHero   = false;
	private int       selectedChals   = 0;        // bitmask of chosen challenges
	private boolean   randomizeChals  = false;
	private int       maxRandomChals  = 3;
	private String    selectedSeed    = "";        // empty = random
	private boolean   headlessMode    = true;      // false → headed (1 run, visible)
	private int       selectedSteps   = 500;

	// -----------------------------------------------------------------------

	public WndAIRun() {
		super();

		float pos = MARGIN;

		// --- Title ---
		RenderedTextBlock title = PixelScene.renderTextBlock("AI Run Configuration", 9);
		title.hardlight(Window.TITLE_COLOR);
		title.setPos(MARGIN + (WIDTH - MARGIN * 2 - title.width()) / 2f, pos);
		add(title);
		pos = title.bottom() + MARGIN * 2;

		// ================================================================
		// MODE  (headless / headed)
		// ================================================================
		RenderedTextBlock modeLabel = PixelScene.renderTextBlock("Mode:", 6);
		modeLabel.setPos(MARGIN, pos);
		add(modeLabel);

		final CheckBox chkHeadless = new CheckBox("Headless (simulate)") {
			@Override
			public void checked(boolean value) {
				super.checked(value);
				headlessMode = value;
			}
		};
		chkHeadless.setRect(MARGIN, modeLabel.bottom() + MARGIN, WIDTH - MARGIN * 2, BTN_H);
		chkHeadless.checked(headlessMode);
		add(chkHeadless);
		pos = chkHeadless.bottom() + MARGIN;

		RenderedTextBlock modeHint = PixelScene.renderTextBlock(
				"Uncheck to watch the AI play in real time (1 run only).", 6);
		modeHint.maxWidth(WIDTH - MARGIN * 2);
		modeHint.setPos(MARGIN, pos);
		add(modeHint);
		pos = modeHint.bottom() + MARGIN * 2;

		// ================================================================
		// HERO CLASS
		// ================================================================
		RenderedTextBlock classLabel = PixelScene.renderTextBlock("Hero Class:", 6);
		classLabel.setPos(MARGIN, pos);
		add(classLabel);
		pos = classLabel.bottom() + MARGIN;

		HeroClass[] classes = HeroClass.values();
		final RedButton[] classBtns = new RedButton[classes.length];

		int cols1 = Math.min(3, classes.length);
		float btnW = (WIDTH - MARGIN * 2 - (cols1 - 1) * MARGIN) / (float) cols1;
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

		if (classes.length > 3) {
			int cols2 = classes.length - 3;
			float btnW2 = (WIDTH - MARGIN * 2 - (cols2 - 1) * MARGIN) / (float) cols2;
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

		updateClassButtons(classBtns, 0);

		// "Random each run" checkbox – disables the class buttons when active
		final CheckBox chkRandHero = new CheckBox("Random each run") {
			@Override
			public void checked(boolean value) {
				super.checked(value);
				randomizeHero = value;
				for (RedButton b : classBtns) {
					if (b != null) b.active = !value;
				}
			}
		};
		chkRandHero.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BTN_H);
		chkRandHero.checked(randomizeHero);
		add(chkRandHero);
		pos = chkRandHero.bottom() + MARGIN * 2;

		// ================================================================
		// SEED
		// ================================================================
		RenderedTextBlock seedLabel = PixelScene.renderTextBlock("Seed:", 6);
		seedLabel.setPos(MARGIN, pos);
		add(seedLabel);

		final RenderedTextBlock seedDisplay = PixelScene.renderTextBlock(
				selectedSeed.isEmpty() ? "Random" : selectedSeed, 6);
		seedDisplay.setPos(MARGIN + seedLabel.width() + MARGIN, pos);
		add(seedDisplay);

		RedButton btnSeed = new RedButton("Set Seed") {
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.scene().addToFront(new WndTextInput(
						"Custom Seed",
						"Leave blank for a random seed each run.",
						selectedSeed, 20, false, "Set", "Clear") {
					@Override
					public void onSelect(boolean positive, String text) {
						if (positive) {
							String formatted = DungeonSeed.formatText(text);
							long val = DungeonSeed.convertFromText(formatted);
							if (val != -1) {
								selectedSeed = formatted;
							} else {
								selectedSeed = "";
							}
						} else {
							selectedSeed = "";
						}
						seedDisplay.text(selectedSeed.isEmpty() ? "Random" : selectedSeed);
					}
				});
			}
		};
		btnSeed.setRect(MARGIN, seedLabel.bottom() + MARGIN, WIDTH - MARGIN * 2, BTN_H);
		add(btnSeed);
		pos = btnSeed.bottom() + MARGIN * 2;

		// ================================================================
		// CHALLENGES
		// ================================================================
		RenderedTextBlock chalLabel = PixelScene.renderTextBlock("Challenges:", 6);
		chalLabel.setPos(MARGIN, pos);
		add(chalLabel);

		final RenderedTextBlock chalCount = PixelScene.renderTextBlock(
				Challenges.activeChallenges(selectedChals) + " active", 6);
		chalCount.setPos(MARGIN + chalLabel.width() + MARGIN, pos);
		add(chalCount);
		pos = chalLabel.bottom() + MARGIN;

		RedButton btnChals = new RedButton("Select Challenges") {
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.scene().addToFront(new WndChallenges(selectedChals, true) {
					@Override
					public void onBackPressed() {
						// Read back whatever was checked
						int mask = 0;
						for (int i = 0; i < boxes.size(); i++) {
							if (boxes.get(i).checked()) {
								mask |= Challenges.MASKS[i];
							}
						}
						selectedChals = mask;
						chalCount.text(Challenges.activeChallenges(selectedChals) + " active");
						super.onBackPressed();
					}
				});
			}
		};
		btnChals.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BTN_H);
		add(btnChals);
		pos = btnChals.bottom() + MARGIN;

		final CheckBox chkRandChals = new CheckBox("Random each run") {
			@Override
			public void checked(boolean value) {
				super.checked(value);
				randomizeChals = value;
			}
		};
		chkRandChals.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BTN_H);
		chkRandChals.checked(randomizeChals);
		add(chkRandChals);
		pos = chkRandChals.bottom() + MARGIN;

		final OptionSlider chalCountSlider = new OptionSlider("Max random challenges",
				"0", String.valueOf(Challenges.MAX_CHALS), 0, Challenges.MAX_CHALS) {
			@Override
			protected void onChange() {
				maxRandomChals = getSelectedValue();
			}
		};
		chalCountSlider.setSelectedValue(maxRandomChals);
		chalCountSlider.setRect(MARGIN, pos, WIDTH - MARGIN * 2, SLIDER_H);
		add(chalCountSlider);
		pos = chalCountSlider.bottom() + MARGIN * 2;

		// ================================================================
		// STEPS  (headless only – steps per run)
		// ================================================================
		OptionSlider stepsSlider = new OptionSlider("Steps per run", "100", "2000", 1, 20) {
			@Override
			protected void onChange() {
				selectedSteps = getSelectedValue() * 100;
			}
		};
		stepsSlider.setSelectedValue(selectedSteps / 100);
		stepsSlider.setRect(MARGIN, pos, WIDTH - MARGIN * 2, SLIDER_H);
		add(stepsSlider);
		pos = stepsSlider.bottom() + MARGIN * 2;

		// ================================================================
		// STARTING ITEMS
		// ================================================================
		RedButton btnItems = new RedButton("Starting Items") {
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.scene().addToFront(new WndStartingItems());
			}
		};
		btnItems.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BTN_H);
		add(btnItems);
		pos = btnItems.bottom() + MARGIN * 2;

		// ================================================================
		// START BUTTON
		// ================================================================
		RedButton btnStart = new RedButton("Start Run") {
			@Override
			protected void onClick() {
				hide();

				if (!chkHeadless.checked()) {
					// ---- Headed mode ----
					// Launch via InterlevelScene, exactly as HeroSelectScene does.
					GamesInProgress.selectedClass = selectedClass;
					GamesInProgress.randomizedClass = randomizeHero;
					if (randomizeHero) {
						java.util.Random rng = new java.util.Random();
						HeroClass[] all = HeroClass.values();
						GamesInProgress.selectedClass = all[rng.nextInt(all.length)];
					}
					SPDSettings.customSeed(selectedSeed);
					if (randomizeChals) {
						java.util.Random rng = new java.util.Random();
						int count = rng.nextInt(maxRandomChals + 1);
						ArrayList<Integer> masks = new ArrayList<>();
						for (int i = 0; i < Challenges.MAX_CHALS; i++) {
							masks.add(Challenges.MASKS[i]);
						}
						Collections.shuffle(masks, rng);
						int mask = 0;
						for (int i = 0; i < count; i++) mask |= masks.get(i);
						SPDSettings.challenges(mask);
					} else {
						SPDSettings.challenges(selectedChals);
					}
					Dungeon.hero = null;
					Dungeon.daily = false;
					Dungeon.dailyReplay = false;
					Dungeon.initSeed();
					GameScene.aiMode = true;
					com.watabou.utils.ActionIndicator.clearAction();
					InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
					Game.switchScene(InterlevelScene.class);
				} else {
					// ---- Headless mode ----
					HeadlessScene.uiHeroClass     = randomizeHero ? null : selectedClass;
					HeadlessScene.uiSteps         = selectedSteps;
					HeadlessScene.uiSeed          = selectedSeed;
					HeadlessScene.uiChallenges    = randomizeChals ? -1 : selectedChals;
					HeadlessScene.uiRandomizeHero  = randomizeHero;
					HeadlessScene.uiRandomizeChals = randomizeChals;
					HeadlessScene.uiMaxRandomChals = maxRandomChals;
					// uiStartingItems is managed directly by WndStartingItems
					ShatteredPixelDungeon.switchNoFade(HeadlessScene.class);
				}
			}
		};
		btnStart.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BTN_H);
		add(btnStart);
		pos += BTN_H + MARGIN;

		resize(WIDTH, (int) pos);
	}

	// -----------------------------------------------------------------------

	private void updateClassButtons(RedButton[] btns, int selected) {
		for (int i = 0; i < btns.length; i++) {
			if (btns[i] != null) {
				btns[i].textColor(i == selected ? Window.TITLE_COLOR : Window.WHITE);
			}
		}
	}
}

