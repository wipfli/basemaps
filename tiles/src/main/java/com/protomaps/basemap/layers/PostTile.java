package com.protomaps.basemap.layers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.onthegomap.planetiler.ForwardingProfile;
import com.onthegomap.planetiler.VectorTile.Feature;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.geo.TileCoord;

public class PostTile implements ForwardingProfile.TilePostProcessor {

  private static Map<String, ArrayList<?>> segmentString(String input) {
    ArrayList<String> segments = new ArrayList<>();
    ArrayList<Boolean> isInside = new ArrayList<>();
    StringBuilder insideSegment = new StringBuilder();
    StringBuilder outsideSegment = new StringBuilder();

    for (int i = 0; i < input.length(); i++) {
      char currentChar = input.charAt(i);
      int charUnicode = (int) currentChar;

      if ((0x0900 <= charUnicode && charUnicode <= 0x097F) || // Devanagari
          (0xA8E0 <= charUnicode && charUnicode <= 0xA8FF) // Devanagari Extended
      ) {
        insideSegment.append(currentChar);
        if (outsideSegment.length() > 0) {
          segments.add(outsideSegment.toString());
          isInside.add(false);
          outsideSegment.setLength(0);
        }
      } else {
        outsideSegment.append(currentChar);
        if (insideSegment.length() > 0) {
          segments.add(insideSegment.toString());
          isInside.add(true);
          insideSegment.setLength(0);
        }
      }
    }

    if (insideSegment.length() > 0) {
      segments.add(insideSegment.toString());
      isInside.add(true);
    } else {
      segments.add(outsideSegment.toString());
      isInside.add(false);
    }

    Map<String, ArrayList<?>> result = new HashMap<>();
    result.put("segments", segments);
    result.put("isInside", isInside);
    return result;

  }

  private static String encodeString(String input) {
    String result = "DEFAULT";

    try {
      String encodedString = URLEncoder.encode(input, StandardCharsets.UTF_8.toString());
      String urlString = String.format("http://localhost:3002/%s", encodedString);

      URL url = new URL(urlString);

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");

      BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      reader.close();

      String markedStringEncoded = response.toString();
      result = URLDecoder.decode(markedStringEncoded, StandardCharsets.UTF_8.toString());
      // System.out.println(result);

    } catch (MalformedURLException e) {
      System.out.println("MalformedURLException: " + e.getMessage());
    } catch (ProtocolException e) {
      System.out.println("ProtocolException: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
    return result;
  }

  @Override
  public Map<String, List<Feature>> postProcessTile(TileCoord tileCoord, Map<String, List<Feature>> layers)
      throws GeometryException {
    for (var layer : layers.values()) {
      for (var item : layer) {
        Map<String, String> newAttrs = new HashMap<>();
        for (var entry : item.attrs().entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue().toString();

          if (key.equals("name") || key.startsWith("name:")) {
            Map<String, ArrayList<?>> segmentation = segmentString(value);

            ArrayList<String> segments = (ArrayList<String>) segmentation.get("segments");
            ArrayList<Boolean> isInside = (ArrayList<Boolean>) segmentation.get("isInside");

            String encodedValue = "";
            for (int i = 0; i < isInside.size(); ++i) {
              if (isInside.get(i)) {
                encodedValue += encodeString(segments.get(i));
              } else {
                encodedValue += segments.get(i);
              }
            }
            newAttrs.put("@" + key, encodedValue);
          }
        }
        item.attrs().putAll(newAttrs);
      }
    }
    return null;
  }

}
