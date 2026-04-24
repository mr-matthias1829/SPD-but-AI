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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.noosa.Game;
import com.watabou.noosa.Scene;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class HeadlessScene extends Scene {

	private static final int DEFAULT_STEPS = 300;
	private static final String PROP_STEPS = "headless.steps";
	private static final String PROP_HERO_CLASS = "headless.heroClass";
	private static final String PROP_SEED = "headless.seed";
	private static final String PROP_SLOT = "headless.slot";
	private static final String PROP_BASE_PATH = "headless.basePath";

	// Static config set by the headed UI before switching to this scene.
	// Always written and read from the render/game thread, so no synchronization needed.
	// Cleared in finishRun() to prevent state leaking into subsequent runs.
	public static int uiSteps = -1;
	public static HeroClass uiHeroClass = null;
	public static String uiSeed = null;

	/** Challenge bitmask to use each run. -1 means keep SPDSettings as-is. */
	public static int uiChallenges = -1;
	/** When true, pick a random hero class independently for each run. */
	public static boolean uiRandomizeHero = false;
	/** When true, randomize the challenge set independently for each run. */
	public static boolean uiRandomizeChals = false;
	/** Maximum number of challenges to apply when randomizing challenges. */
	public static int uiMaxRandomChals = 3;
	/** Items to grant the hero at the start of every run, or null for none. */
	public static List<StartingItem> uiStartingItems = null;

	/** Describes a single item that should be placed in the hero's backpack on start. */
	public static class StartingItem {
		public final String className;
		public final int level;
		public final boolean identified;

		public StartingItem(String className, int level, boolean identified) {
			this.className = className;
			this.level = level;
			this.identified = identified;
		}
	}

	private Thread actorThread;
	private int stepsRemaining;
	private float actorThreadNotifyInterval = 1f / 60f;

	@Override
	public void create() {
		super.create();
		configureStorage();
		initializeGame();
		startActorThread();
		stepsRemaining = readSteps();
		Gdx.app.log("Headless", "Started headless run: steps=" + stepsRemaining
				+ " hero=" + Dungeon.hero.heroClass + " seed=" + Dungeon.seed);
	}

	@Override
	public synchronized void update() {
		super.update();

		if (stepsRemaining <= 0) {
			finishRun();
			return;
		}

		if (Dungeon.hero != null && Dungeon.hero.ready) {
			Dungeon.hero.rest(false);
		}

		tickActorThread();
		stepsRemaining--;

		if (stepsRemaining == 0) {
			finishRun();
		}
	}

	@Override
	public void destroy() {
		stopActorThread();
		super.destroy();
	}

	private void configureStorage() {
		// Only override the save path when running under a true headless application;
		// in headed mode the desktop launcher has already configured the correct path.
		if (Game.platform == null || !Game.platform.isHeadless()) return;
		String basePath = System.getProperty(PROP_BASE_PATH, "/tmp/shpd-headless/");
		if (!basePath.endsWith("/")) {
			basePath += "/";
		}
		Gdx.files.absolute(basePath).mkdirs();
		FileUtils.setDefaultFileProperties(Files.FileType.Absolute, basePath);
	}

	private void initializeGame() {
		GamesInProgress.curSlot = readSlot();

		// Hero class: random (independently per run) or fixed
		if (uiRandomizeHero) {
			Random rng = new Random();
			HeroClass[] classes = HeroClass.values();
			GamesInProgress.selectedClass = classes[rng.nextInt(classes.length)];
			GamesInProgress.randomizedClass = true;
		} else {
			GamesInProgress.selectedClass = readHeroClass();
			GamesInProgress.randomizedClass = false;
		}

		Dungeon.daily = false;
		Dungeon.dailyReplay = false;

		String seed = (uiSeed != null) ? uiSeed : System.getProperty(PROP_SEED, "");
		SPDSettings.customSeed(seed.trim());

		// Challenges: random (independently per run), fixed, or leave SPDSettings alone
		if (uiRandomizeChals) {
			Random rng = new Random();
			int count = rng.nextInt(uiMaxRandomChals + 1);
			ArrayList<Integer> masks = new ArrayList<>();
			for (int i = 0; i < Challenges.MAX_CHALS; i++) {
				masks.add(Challenges.MASKS[i]);
			}
			Collections.shuffle(masks, rng);
			int mask = 0;
			for (int i = 0; i < count; i++) {
				mask |= masks.get(i);
			}
			SPDSettings.challenges(mask);
		} else if (uiChallenges >= 0) {
			SPDSettings.challenges(uiChallenges);
		}
		// if uiChallenges == -1, SPDSettings.challenges() is left as-is

		Dungeon.initSeed();
		Dungeon.init();

		// Grant pre-defined starting items
		if (uiStartingItems != null) {
			for (StartingItem spec : uiStartingItems) {
				try {
					Class<?> cls = Reflection.forName(spec.className);
					if (cls != null) {
						Item item = (Item) Reflection.newInstance(cls);
						if (item != null) {
							if (spec.level > 0) item.upgrade(spec.level);
							if (spec.identified) {
								item.levelKnown = true;
								item.cursedKnown = true;
							}
							item.collect(Dungeon.hero.belongings.backpack);
						}
					}
				} catch (Exception e) {
					Gdx.app.log("Headless", "Failed to add starting item " + spec.className + ": " + e);
				}
			}
		}

		Level level = Dungeon.newLevel();
		Dungeon.switchLevel(level, -1);
	}

	private int readSteps() {
		if (uiSteps > 0) return uiSteps;
		String stepsValue = System.getProperty(PROP_STEPS);
		if (stepsValue == null) {
			return DEFAULT_STEPS;
		}
		try {
			return Math.max(1, Integer.parseInt(stepsValue));
		} catch (NumberFormatException e) {
			return DEFAULT_STEPS;
		}
	}

	private int readSlot() {
		String slotValue = System.getProperty(PROP_SLOT);
		if (slotValue != null) {
			try {
				return Math.max(1, Integer.parseInt(slotValue));
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		int emptySlot = GamesInProgress.firstEmpty();
		return emptySlot == -1 ? 1 : emptySlot;
	}

	private HeroClass readHeroClass() {
		if (uiHeroClass != null) return uiHeroClass;
		String heroClassValue = System.getProperty(PROP_HERO_CLASS);
		if (heroClassValue != null && !heroClassValue.trim().isEmpty()) {
			try {
				return HeroClass.valueOf(heroClassValue.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignored) {
				// fall through
			}
		}
		return HeroClass.WARRIOR;
	}

	private void startActorThread() {
		actorThread = new Thread(() -> Actor.process());
		actorThread.setName("SHPD Headless Actor Thread");
		Actor.keepActorThreadAlive = true;
		actorThread.start();
	}

	private void stopActorThread() {
		if (actorThread != null && actorThread.isAlive()) {
			Actor.keepActorThreadAlive = false;
			actorThread.interrupt();
			synchronized (actorThread) {
				actorThread.notify();
			}
		}
	}

	private void tickActorThread() {
		if (Dungeon.hero == null || actorThread == null || !actorThread.isAlive()) {
			return;
		}

		if (actorThreadNotifyInterval > 0f) {
			actorThreadNotifyInterval -= Game.elapsed;
			return;
		}

		actorThreadNotifyInterval = 1f / 60f;
		synchronized (actorThread) {
			actorThread.notify();
		}
	}

	private void finishRun() {
		Gdx.app.log("Headless", "Finished run: depth=" + Dungeon.depth
				+ " heroPos=" + (Dungeon.hero == null ? "none" : Dungeon.hero.pos)
				+ " time=" + Actor.now());
		stopActorThread();
		// Clear the static UI config so they don't bleed into a subsequent run.
		uiSteps = -1;
		uiHeroClass = null;
		uiSeed = null;
		uiChallenges = -1;
		uiRandomizeHero = false;
		uiRandomizeChals = false;
		uiMaxRandomChals = 3;
		uiStartingItems = null;
		if (Game.platform != null && Game.platform.isHeadless()) {
			Game.instance.finish();
		} else {
			ShatteredPixelDungeon.switchNoFade(TitleScene.class);
		}
	}
}
