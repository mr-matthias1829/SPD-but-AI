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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.noosa.Game;
import com.watabou.noosa.Scene;
import com.watabou.utils.FileUtils;

import java.util.Locale;

public class HeadlessScene extends Scene {

	private static final int DEFAULT_STEPS = 300;
	private static final String PROP_STEPS = "headless.steps";
	private static final String PROP_HERO_CLASS = "headless.heroClass";
	private static final String PROP_SEED = "headless.seed";
	private static final String PROP_SLOT = "headless.slot";
	private static final String PROP_BASE_PATH = "headless.basePath";

	private Thread actorThread;
	private int stepsRemaining;
	private float notifyDelay = 1f / 60f;

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
		String basePath = System.getProperty(PROP_BASE_PATH, "/tmp/shpd-headless/");
		if (!basePath.endsWith("/")) {
			basePath += "/";
		}
		Gdx.files.absolute(basePath).mkdirs();
		FileUtils.setDefaultFileProperties(Files.FileType.Absolute, basePath);
	}

	private void initializeGame() {
		GamesInProgress.curSlot = readSlot();
		GamesInProgress.selectedClass = readHeroClass();
		GamesInProgress.randomizedClass = false;

		Dungeon.daily = false;
		Dungeon.dailyReplay = false;

		String seed = System.getProperty(PROP_SEED, "");
		SPDSettings.customSeed(seed.trim());

		Dungeon.initSeed();
		Dungeon.init();
		Level level = Dungeon.newLevel();
		Dungeon.switchLevel(level, -1);
	}

	private int readSteps() {
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

		if (notifyDelay > 0f) {
			notifyDelay -= Game.elapsed;
			return;
		}

		notifyDelay = 1f / 60f;
		synchronized (actorThread) {
			actorThread.notify();
		}
	}

	private void finishRun() {
		Gdx.app.log("Headless", "Finished run: depth=" + Dungeon.depth
				+ " heroPos=" + (Dungeon.hero == null ? -1 : Dungeon.hero.pos)
				+ " time=" + Actor.now());
		stopActorThread();
		Game.instance.finish();
	}
}
