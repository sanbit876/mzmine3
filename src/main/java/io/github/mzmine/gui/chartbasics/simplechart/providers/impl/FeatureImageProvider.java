/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import java.awt.Color;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

public class FeatureImageProvider implements PlotXYZDataProvider {

  private static final Logger logger = Logger.getLogger(FeatureImageProvider.class.getName());

  private final ModularFeature feature;
  private IonTimeSeries<ImagingScan> series;
  private double width;
  private double height;
  private final boolean normalize;

  public FeatureImageProvider(ModularFeature feature) {
    this(feature, false);
  }

  public FeatureImageProvider(ModularFeature feature, boolean normalize) {
    this.feature = feature;
    this.normalize = normalize;
    if (normalize == false) {
      series = (IonTimeSeries<ImagingScan>) feature.getFeatureData();
    }
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return feature.getRawDataFile().getColorAWT();
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return feature.getRawDataFile().getColor();
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return null;
  }

  @Nullable
  @Override
  public PaintScale getPaintScale() {
    return null;
  }

  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return FeatureUtils.featureToString(feature);
  }

  @Nullable
  @Override
  public String getToolTipText(int itemIndex) {
    return null;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    ImagingParameters imagingParam = ((ImagingRawDataFile) feature.getRawDataFile()).getImagingParam();
    height = imagingParam.getLateralHeight() / imagingParam.getMaxNumberOfPixelY();
    width = imagingParam.getLateralWidth() / imagingParam.getMaxNumberOfPixelX();

    final IonTimeSeries<? extends Scan> featureData = feature.getFeatureData();
    try {
      if (normalize) {
        series = (IonTimeSeries<ImagingScan>) IonTimeSeriesUtils.normalizeToAvgTic(
            (IonTimeSeries<? extends ImagingScan>) featureData, null);
      } else {
        series = (IonTimeSeries<ImagingScan>) featureData;
      }
    } catch (ClassCastException e) {
      logger.info("Cannot cast feature data to IonTimeSeries<? extends ImagingScan> for feature "
          + FeatureUtils.featureToString(feature));
    }

    if (series == null) {
      throw new IllegalStateException(
          "Could not create image provider for feature " + FeatureUtils.featureToString(feature));
    }
  }

  @Override
  public double getDomainValue(int index) {
    return series.getSpectra().get(index).getCoordinates().getX() * width;
  }

  @Override
  public double getRangeValue(int index) {
    return series.getSpectra().get(index).getCoordinates().getY() * height;
  }

  @Override
  public int getValueCount() {
    return series.getSpectra().size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return 1d;
  }

  @Override
  public double getZValue(int index) {
    return series.getIntensity(index);
  }

  @Nullable
  @Override
  public Double getBoxHeight() {
    return height;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return width;
  }
}
