/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.featuredata;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FeatureDataUtils {

  private static final Logger logger = Logger.getLogger(FeatureDataUtils.class.getName());

  public static Range<Float> getRtRange(IonTimeSeries<? extends Scan> series) {
    final List<? extends Scan> scans = series.getSpectra();
    return Range
        .closed(scans.get(0).getRetentionTime(), scans.get(scans.size() - 1).getRetentionTime());
  }

  public static Range<Double> getMzRange(MzSeries series) {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    if (series instanceof IonMobilogramTimeSeries ionTrace) {
      for (IonMobilitySeries mobilogram : ionTrace.getMobilograms()) {
        for (int i = 0; i < mobilogram.getNumberOfValues(); i++) {
          final double mz = mobilogram.getMZ(i);
          // we add flanking 0 intensities with 0d mz during building, don't count those
          if (mz < min && Double.compare(mz, 0d) == 1) {
            min = mz;
          }
          if (mz > max) {
            max = mz;
          }
        }
      }
    } else {
      for (int i = 0; i < series.getNumberOfValues(); i++) {
        final double mz = series.getMZ(i);
        // we add flanking 0 intesities with 0d mz during building, don't count those
        if (mz < min && Double.compare(mz, 0d) == 1) {
          min = mz;
        }
        if (mz > max) {
          max = mz;
        }
      }
    }
    return Range.closed(min, max);
  }

  public static Range<Float> getIntensityRange(IntensitySeries series) {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    for (int i = 0; i < series.getNumberOfValues(); i++) {
      final double intensity = series.getIntensity(i);
      // we add flanking 0s during building, don't count those
      if (intensity < min && intensity > 0d) {
        min = intensity;
      }
      if (intensity > max) {
        max = intensity;
      }
    }
    return Range.closed((float) min, (float) max);
  }

  /**
   * @param series The series. Ascending or descending mobility.
   * @return The mobility range.
   */
  public static Range<Float> getMobilityRange(MobilitySeries series) {
    if (series.getNumberOfValues() == 0) {
      return Range.singleton(Float.NaN);
    }
    return Range.singleton((float) series.getMobility(0))
        .span(Range.singleton((float) series.getMobility(series.getNumberOfValues() - 1)));
  }

  /**
   * @param series The series.
   * @return The index of the highest intensity. May be -1 if no intensity could be foudn (-> series
   * empty).
   */
  public static int getMostIntenseIndex(IntensitySeries series) {
    int maxIndex = -1;
    double maxIntensity = Double.MIN_VALUE;

    for (int i = 0; i < series.getNumberOfValues(); i++) {
      final double intensity = series.getIntensity(i);
      if (intensity > maxIntensity) {
        maxIndex = i;
        maxIntensity = intensity;
      }
    }
    return maxIndex;
  }

  /**
   * @return The most intense spectrum in the series or null.
   */
  @Nullable
  public static MassSpectrum getMostIntenseSpectrum(
      IonSpectrumSeries<? extends MassSpectrum> series) {
    final int maxIndex = getMostIntenseIndex(series);
    return maxIndex != -1 ? series.getSpectrum(maxIndex) : null;
  }

  /**
   * @return The most intense scan in the series or null.
   */
  @Nullable
  public static Scan getMostIntenseScan(IonTimeSeries<? extends Scan> series) {
    return (Scan) getMostIntenseSpectrum(series);
  }

  public static float calculateArea(IonTimeSeries<? extends Scan> series) {
    if (series.getNumberOfValues() <= 1) {
      return 0f;
    }
    float area = 0f;
    DoubleBuffer intensities = series.getIntensityValues();
    List<? extends Scan> scans = series.getSpectra();
    double lastIntensity = intensities.get(0);
    float lastRT = scans.get(0).getRetentionTime();
    for (int i = 1; i < series.getNumberOfValues(); i++) {
      final double thisIntensity = intensities.get(i);
      final float thisRT = scans.get(i).getRetentionTime();
      area += (thisRT - lastRT) * (thisIntensity + lastIntensity) / 2.0 /* 60d*/;
      lastIntensity = thisIntensity;
      lastRT = thisRT;
    }
    return area;
  }

  public static double calculateMz(@Nonnull final IonSeries series,
      @Nonnull final CenterMeasure cm) {
    CenterFunction cf = new CenterFunction(cm, Weighting.LINEAR);
    double[][] data = DataPointUtils
        .getDataPointsAsDoubleArray(series.getMZValues(), series.getIntensityValues());
    return cf.calcCenter(data[0], data[1]);
  }

  public static void recalculateIonSeriesDependingTypes(@Nonnull final ModularFeature feature) {
    recalculateIonSeriesDependingTypes(feature, CenterMeasure.AVG);
  }

  public static void recalculateIonSeriesDependingTypes(@Nonnull final ModularFeature feature,
      @Nonnull final CenterMeasure cm) {
    IonTimeSeries<? extends Scan> featureData = feature.getFeatureData();
    Range<Float> intensityRange = FeatureDataUtils.getIntensityRange(featureData);
    Range<Double> mzRange = FeatureDataUtils.getMzRange(featureData);
    Range<Float> rtRange = FeatureDataUtils.getRtRange(featureData);
    Scan mostIntenseSpectrum = FeatureDataUtils.getMostIntenseScan(featureData);
    float area = FeatureDataUtils.calculateArea(featureData);

    feature.set(AreaType.class, area);
    feature.set(MZRangeType.class, mzRange);
    feature.set(RTRangeType.class, rtRange);
    feature.set(IntensityRangeType.class, intensityRange);
    feature.setRepresentativeScan(mostIntenseSpectrum);
    feature.setHeight(intensityRange.upperEndpoint());
    feature.setRT(mostIntenseSpectrum.getRetentionTime());
    feature.setMZ(calculateMz(featureData, cm));

    if (featureData instanceof IonMobilogramTimeSeries) {
      SummedIntensityMobilitySeries summedMobilogram = ((IonMobilogramTimeSeries) featureData)
          .getSummedMobilogram();
      feature.setMobilityRange(getMobilityRange(summedMobilogram));
      feature.setMobility(calculateMobility(summedMobilogram));
    }
    // todo recalc quality parameters
  }

  public static <T extends IntensitySeries & MobilitySeries> float calculateMobility(T series) {
    int mostIntenseMobilityScanIndex = -1;
    double intensity = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < series.getNumberOfValues(); i++) {
      double currentIntensity = series.getIntensity(i);
      if (currentIntensity > intensity) {
        intensity = currentIntensity;
        mostIntenseMobilityScanIndex = i;
      }
    }

    if (Double.compare(intensity, Double.MIN_VALUE) == 0) {
      logger.info(() -> "Mobility cannot be specified for: " + series);
      return Float.NaN;
    } else {
      return (float) series.getMobility(mostIntenseMobilityScanIndex);
    }
  }

  public static double getSmallestMzDelta(@Nonnull final MzSeries series) {
    double smallestDelta = Double.POSITIVE_INFINITY;

    if (series instanceof IonMobilogramTimeSeries ims) {
      int maxValues = ims.getMobilograms().stream().mapToInt(IonMobilitySeries::getNumberOfValues)
          .max().orElse(0);

      double[] mzBuffer = new double[maxValues];
      for (IonMobilitySeries mobilogram : ims.getMobilograms()) {
        mobilogram.getMzValues(mzBuffer);
        final double delta = ArrayUtils.smallestDelta(mzBuffer, mobilogram.getNumberOfValues());
        if (delta < smallestDelta) {
          smallestDelta = delta;
        }
      }
    } else {
      double[] mzBuffer = new double[series.getNumberOfValues()];
      series.getMzValues(mzBuffer);
      smallestDelta = ArrayUtils.smallestDelta(mzBuffer);
    }

    return smallestDelta;
  }


}
