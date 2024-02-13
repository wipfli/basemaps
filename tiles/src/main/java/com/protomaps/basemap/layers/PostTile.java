package com.protomaps.basemap.layers;
import java.util.List;
import java.util.Map;

import com.onthegomap.planetiler.ForwardingProfile;
import com.onthegomap.planetiler.VectorTile.Feature;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.geo.TileCoord;

public class PostTile implements ForwardingProfile.TilePostProcessor {

  @Override
  public Map<String, List<Feature>> postProcessTile(TileCoord tileCoord, Map<String, List<Feature>> layers)
      throws GeometryException {
    System.out.println("Unimplemented method 'postProcessTile'");
    return null;
  }

}
