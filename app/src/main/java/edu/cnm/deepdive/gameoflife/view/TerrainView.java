/*
 *  Copyright 2021 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.gameoflife.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import edu.cnm.deepdive.gameoflife.model.Terrain;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TerrainView extends View {

  private static final float MAX_HUE = 360;
  private static final float MAX_SATURATION = 1;
  private static final float MAX_BRIGHTNESS = 1;
  private static final float DEFAULT_HUE = 300;
  private static final float DEFAULT_SATURATION = 1;
  private static final float DEFAULT_NEW_BRIGHTNESS = 1;
  private static final float DEFAULT_OLD_BRIGHTNESS = 0.6f;
  private static final long UPDATE_INTERVAL = 20;

  private final Rect source;
  private final Rect dest;
  private final int[] cellColors;
  private final int backgroundColor;
  private final ScheduledExecutorService scheduler;
  private final DisplayUpdater updater;

  private Bitmap bitmap;
  private Terrain terrain;
  private byte[][] cells;
  private ScheduledFuture<?> future;
  private float hue;
  private float saturation;
  private float newBrightness;
  private float oldBrightness;
  private boolean colorsUpdated;

  {
    setWillNotDraw(false);
    source = new Rect();
    dest = new Rect();
    cellColors = new int[Byte.MAX_VALUE];
    backgroundColor = Color.BLACK;
    scheduler = Executors.newScheduledThreadPool(1);
    updater = new DisplayUpdater();
    hue = DEFAULT_HUE;
    saturation = DEFAULT_SATURATION;
    newBrightness = DEFAULT_NEW_BRIGHTNESS;
    oldBrightness = DEFAULT_OLD_BRIGHTNESS;
  }

  public TerrainView(Context context) {
    super(context);
  }

  public TerrainView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public TerrainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public TerrainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
      int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = getSuggestedMinimumWidth();
    int height = getSuggestedMinimumHeight();
    width = resolveSizeAndState(getPaddingLeft() + getPaddingRight() + width, widthMeasureSpec, 0);
    height = resolveSizeAndState(getPaddingTop() + getPaddingBottom() + height, heightMeasureSpec, 0);
    int size = Math.max(width, height);
    setMeasuredDimension(size, size);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    updateBitmap();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (bitmap != null) {
      dest.set(0, 0, getWidth(), getHeight());
      canvas.drawBitmap(bitmap, source, dest, null);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    stopDisplayUpdates();
    future = scheduler.scheduleWithFixedDelay(
        updater, UPDATE_INTERVAL, UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    stopDisplayUpdates();
  }

  public void setTerrain(Terrain terrain) {
    this.terrain = terrain;
    if (terrain != null) {
      int size = terrain.getSize();
      cells = new byte[size][size];
      bitmap = Bitmap.createBitmap(size, size, Config.RGB_565);
      source.set(0, 0, size, size);
    } else {
      bitmap = null;
    }
    colorsUpdated = false;
  }

  public void setGeneration(long generation) {
    updater.setGeneration(generation);
  }

  public void setHue(float hue) {
    this.hue = hue;
    colorsUpdated = false;
  }

  public void setSaturation(float saturation) {
    this.saturation = saturation;
    colorsUpdated = false;
  }

  public void setNewBrightness(float newBrightness) {
    this.newBrightness = newBrightness;
    colorsUpdated = false;
  }

  public void setOldBrightness(float oldBrightness) {
    this.oldBrightness = oldBrightness;
    colorsUpdated = false;
  }

  private void updateBitmap() {
    if (bitmap != null) {
      if (!colorsUpdated) {
        updateColors();
      }
      terrain.copyCells(cells);
      for (int rowIndex = 0; rowIndex < cells.length; rowIndex++) {
        for (int colIndex = 0; colIndex < cells[rowIndex].length; colIndex++) {
          byte age = cells[rowIndex][colIndex];
          bitmap.setPixel(colIndex, rowIndex, (age > 0) ? cellColors[age - 1] : backgroundColor);
        }
      }
    }
  }

  private void updateColors() {
    for (int i = 0; i < Byte.MAX_VALUE; i++) {
      float brightness = oldBrightness +
          (newBrightness - oldBrightness) * (Byte.MAX_VALUE - i) / Byte.MAX_VALUE;
      cellColors[i] = Color.HSVToColor(new float[]{hue, saturation, brightness});
    }
    colorsUpdated = true;
  }

  private void stopDisplayUpdates() {
    if (future != null) {
      future.cancel(true);
    }
  }

  private class DisplayUpdater implements Runnable {

    private volatile long generation = 0;
    private long lastGeneration;

    public void setGeneration(long generation) {
      this.generation = generation;
    }

    @Override
    public void run() {
      if (generation == 0 || generation > lastGeneration) {
        lastGeneration = generation;
        updateBitmap();
        if (generation == 0) {
          postInvalidate();
        }
      }
    }

  }

}
