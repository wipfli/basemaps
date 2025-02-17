package com.protomaps.basemap.names;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.Map;

public class NeNames {
  public static FeatureCollector.Feature setNeNames(FeatureCollector.Feature feature, SourceFeature source,
    int minzoom) {
    for (Map.Entry<String, Object> tag : source.tags().entrySet()) {
      var key = tag.getKey();
      if (key.equals("name")) {
        feature.setAttrWithMinzoom(key, source.getTag(key), minzoom);
      } else if (key.startsWith("name_")) {
        feature.setAttrWithMinzoom(key.replace("_", ":"), source.getTag(key), minzoom);
      }
    }

    return feature;
  }
}
