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

import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeadlessScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.List;

/**
 * Window for configuring items that the hero will start with in every AI run.
 *
 * Items are picked from Generator category lists.  For each selected item the
 * user may choose the upgrade level and whether it starts identified.
 */
public class WndStartingItems extends Window {

	private static final int WIDTH  = 140;
	private static final int MARGIN = 2;
	private static final int BTN_H  = 16;

	/** Mutable working copy; applied to HeadlessScene only on "OK". */
	private final List<HeadlessScene.StartingItem> items;

	private float pos;

	public WndStartingItems() {
		super();

		// Work on a copy so Cancel doesn't modify the shared list.
		items = new ArrayList<>();
		if (HeadlessScene.uiStartingItems != null) {
			items.addAll(HeadlessScene.uiStartingItems);
		}

		rebuildLayout();
	}

	// -----------------------------------------------------------------------

	private void rebuildLayout() {
		clear();
		pos = MARGIN;

		// Title
		RenderedTextBlock title = PixelScene.renderTextBlock("Starting Items", 9);
		title.hardlight(Window.TITLE_COLOR);
		title.setPos(MARGIN + (WIDTH - MARGIN * 2 - title.width()) / 2f, pos);
		add(title);
		pos = title.bottom() + MARGIN * 2;

		// Current items list
		if (items.isEmpty()) {
			RenderedTextBlock none = PixelScene.renderTextBlock("(none)", 6);
			none.setPos(MARGIN, pos);
			add(none);
			pos = none.bottom() + MARGIN;
		} else {
			for (int i = 0; i < items.size(); i++) {
				final int idx = i;
				HeadlessScene.StartingItem spec = items.get(i);

				// Build a friendly display name
				String displayName = friendlyName(spec.className)
						+ (spec.level > 0 ? " +" + spec.level : "")
						+ (spec.identified ? " [ID]" : "");

				RenderedTextBlock label = PixelScene.renderTextBlock(displayName, 6);
				label.setPos(MARGIN, pos + (BTN_H - label.height()) / 2f);
				add(label);

				RedButton btnRemove = new RedButton("X") {
					@Override
					protected void onClick() {
						items.remove(idx);
						rebuildLayout();
					}
				};
				btnRemove.setRect(WIDTH - MARGIN - BTN_H, pos, BTN_H, BTN_H);
				add(btnRemove);

				pos += BTN_H + MARGIN;
			}
		}

		// "Add Item" button
		RedButton btnAdd = new RedButton("Add Item") {
			@Override
			protected void onClick() {
				WndStartingItems.this.addToFront(new WndPickCategory());
			}
		};
		btnAdd.setRect(MARGIN, pos, (WIDTH - MARGIN * 3) / 2f, BTN_H);
		add(btnAdd);

		// "OK" button
		RedButton btnOK = new RedButton("OK") {
			@Override
			protected void onClick() {
				HeadlessScene.uiStartingItems = items.isEmpty() ? null : new ArrayList<>(items);
				hide();
			}
		};
		btnOK.setRect(MARGIN + (WIDTH - MARGIN * 3) / 2f + MARGIN, pos,
				(WIDTH - MARGIN * 3) / 2f, BTN_H);
		add(btnOK);
		pos += BTN_H + MARGIN;

		resize(WIDTH, (int) pos);
	}

	// -----------------------------------------------------------------------

	private static String friendlyName(String className) {
		try {
			Class<?> cls = Reflection.forName(className);
			if (cls != null) {
				Item item = (Item) Reflection.newInstance(cls);
				if (item != null) return item.name();
			}
		} catch (Exception ignored) { }
		// Fallback: extract simple class name
		int dot = className.lastIndexOf('.');
		return dot >= 0 ? className.substring(dot + 1) : className;
	}

	// -----------------------------------------------------------------------
	// Category picker

	private class WndPickCategory extends Window {

		WndPickCategory() {
			super();

			float p = MARGIN;

			RenderedTextBlock title = PixelScene.renderTextBlock("Pick Category", 8);
			title.hardlight(Window.TITLE_COLOR);
			title.setPos(MARGIN + (WIDTH - MARGIN * 2 - title.width()) / 2f, p);
			add(title);
			p = title.bottom() + MARGIN * 2;

			// Group Generator categories that have useful items
			Generator.Category[] cats = {
					Generator.Category.WEP_T1, Generator.Category.WEP_T2,
					Generator.Category.WEP_T3, Generator.Category.WEP_T4,
					Generator.Category.WEP_T5,
					Generator.Category.ARMOR,
					Generator.Category.WAND, Generator.Category.RING,
					Generator.Category.ARTIFACT,
					Generator.Category.POTION, Generator.Category.SCROLL,
					Generator.Category.SEED, Generator.Category.STONE,
					Generator.Category.MISSILE, Generator.Category.FOOD
			};
			String[] labels = {
					"Weapons T1", "Weapons T2", "Weapons T3", "Weapons T4", "Weapons T5",
					"Armor",
					"Wands", "Rings", "Artifacts",
					"Potions", "Scrolls", "Seeds", "Runestones",
					"Missiles", "Food"
			};

			float btnW = (WIDTH - MARGIN * 3) / 2f;
			for (int i = 0; i < cats.length; i++) {
				final Generator.Category cat = cats[i];
				final String catLabel = labels[i];
				RedButton btn = new RedButton(catLabel) {
					@Override
					protected void onClick() {
						hide();
						WndPickCategory.this.addToFront(new WndPickItem(cat, catLabel));
					}
				};
				float col = (i % 2) * (btnW + MARGIN);
				btn.setRect(MARGIN + col, p, btnW, BTN_H);
				add(btn);
				if (i % 2 == 1) p += BTN_H + MARGIN;
			}
			// If odd number of cats, close the last row
			if (cats.length % 2 == 1) p += BTN_H + MARGIN;

			RedButton btnCancel = new RedButton("Cancel") {
				@Override
				protected void onClick() {
					hide();
				}
			};
			btnCancel.setRect(MARGIN, p, WIDTH - MARGIN * 2, BTN_H);
			add(btnCancel);
			p += BTN_H + MARGIN;

			resize(WIDTH, (int) p);
		}
	}

	// -----------------------------------------------------------------------
	// Item picker within a category

	private class WndPickItem extends Window {

		WndPickItem(Generator.Category cat, String catLabel) {
			super();

			float p = MARGIN;

			RenderedTextBlock title = PixelScene.renderTextBlock(catLabel, 8);
			title.hardlight(Window.TITLE_COLOR);
			title.setPos(MARGIN + (WIDTH - MARGIN * 2 - title.width()) / 2f, p);
			add(title);
			p = title.bottom() + MARGIN * 2;

			Class<?>[] classes = cat.classes;
			if (classes == null || classes.length == 0) {
				RenderedTextBlock empty = PixelScene.renderTextBlock("(no items)", 6);
				empty.setPos(MARGIN, p);
				add(empty);
				p = empty.bottom() + MARGIN * 2;
			} else {
				float btnW = (WIDTH - MARGIN * 3) / 2f;
				for (int i = 0; i < classes.length; i++) {
					final String clsName = classes[i].getName();
					final String itemLabel = friendlyName(clsName);
					RedButton btn = new RedButton(itemLabel) {
						@Override
						protected void onClick() {
							hide();
							WndPickItem.this.addToFront(new WndConfigItem(clsName, itemLabel));
						}
					};
					float col = (i % 2) * (btnW + MARGIN);
					btn.setRect(MARGIN + col, p, btnW, BTN_H);
					add(btn);
					if (i % 2 == 1) p += BTN_H + MARGIN;
				}
				if (classes.length % 2 == 1) p += BTN_H + MARGIN;
			}

			RedButton btnCancel = new RedButton("Back") {
				@Override
				protected void onClick() {
					hide();
				}
			};
			btnCancel.setRect(MARGIN, p, WIDTH - MARGIN * 2, BTN_H);
			add(btnCancel);
			p += BTN_H + MARGIN;

			resize(WIDTH, (int) p);
		}
	}

	// -----------------------------------------------------------------------
	// Item configuration (level + identified)

	private class WndConfigItem extends Window {

		WndConfigItem(String className, String label) {
			super();

			float p = MARGIN;

			RenderedTextBlock title = PixelScene.renderTextBlock(label, 8);
			title.hardlight(Window.TITLE_COLOR);
			title.setPos(MARGIN + (WIDTH - MARGIN * 2 - title.width()) / 2f, p);
			add(title);
			p = title.bottom() + MARGIN * 2;

			final int[] levelHolder = {0};
			OptionSlider levelSlider = new OptionSlider("Level", "+0", "+10", 0, 10) {
				@Override
				protected void onChange() {
					levelHolder[0] = getSelectedValue();
				}
			};
			levelSlider.setSelectedValue(0);
			levelSlider.setRect(MARGIN, p, WIDTH - MARGIN * 2, 24);
			add(levelSlider);
			p = levelSlider.bottom() + MARGIN;

			final CheckBox chkId = new CheckBox("Start identified");
			chkId.setRect(MARGIN, p, WIDTH - MARGIN * 2, BTN_H);
			chkId.checked(true);
			add(chkId);
			p = chkId.bottom() + MARGIN * 2;

			RedButton btnAdd = new RedButton("Add") {
				@Override
				protected void onClick() {
					items.add(new HeadlessScene.StartingItem(className, levelHolder[0], chkId.checked()));
					hide();
					// Close the whole chain and rebuild the parent list
					WndStartingItems.this.rebuildLayout();
				}
			};
			btnAdd.setRect(MARGIN, p, WIDTH - MARGIN * 2, BTN_H);
			add(btnAdd);
			p += BTN_H + MARGIN;

			resize(WIDTH, (int) p);
		}
	}
}
