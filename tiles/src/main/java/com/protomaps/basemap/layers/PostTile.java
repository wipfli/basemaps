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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.GlyphMetrics;

import com.onthegomap.planetiler.ForwardingProfile;
import com.onthegomap.planetiler.VectorTile.Feature;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.geo.TileCoord;

public class PostTile implements ForwardingProfile.TilePostProcessor {

  HashMap<String, Integer> codepointMap;
  Font font;

  public PostTile() {
    System.out.println("hello from PostTile constructor.");
    this.codepointMap = getCodepointMap();

    InputStream is;
    // Font font = null;
    try {
      is = new FileInputStream(new File("src/main/java/com/protomaps/basemap/NotoSansDevanagari-Regular.ttf"));
      this.font = Font.createFont(Font.TRUETYPE_FONT, is);
    } catch (IOException | FontFormatException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

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

  private static String serializeGlyph(int index, int x_offset, int y_offset, int x_advance, int y_advance) {
    return Integer.toString(index) + "|" +
      Integer.toString(x_offset) + "|" +
      Integer.toString(y_offset) + "|" +
      Integer.toString(x_advance) + "|" +
      Integer.toString(y_advance);
  }

  private static HashMap<String, Integer> getCodepointMap() {
    HashMap<String, Integer> codepointMap = new HashMap<String, Integer>();

    System.out.println("Start reading encoding.csv...");
    try {
      BufferedReader reader;
      reader = new BufferedReader(new FileReader("src/main/java/com/protomaps/basemap/encoding.csv"));
      reader.readLine(); // skip header
      String line = reader.readLine();
      while (line != null) {
        String[] parts = line.split(",");

        String index = parts[0].trim();
        String x_offset = parts[1].trim();
        String y_offset = parts[2].trim();
        String x_advance = parts[3].trim();
        String y_advance = parts[4].trim();
        String codepoint = parts[5].trim();

        String key = index + "|" + x_offset + "|" + y_offset + "|" + x_advance  + "|" + y_advance;

        codepointMap.put(key, Integer.parseInt(codepoint));
        line = reader.readLine();
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    return codepointMap;
  }

  private int codepointFromGlyph(int index, int x_offset, int y_offset, int x_advance, int y_advance) {

    // HashMap<String, Integer> codepointMap = getCodepointMap();
    Integer[][] deltas = {
      {0, 0},

      {1, 0},
      {0, 1},
      {-1, 0},
      {0, -1},
      
      {2, 0},
      {0, 2},
      {-2, 0},
      {0, -2},

      {3, 0},
      {0, 3},
      {-3, 0},
      {0, -3}
    };

    String glyph = "";
    for (int i = 0; i < deltas.length; ++i) {
      int delta_x_offset = deltas[i][0];
      int delta_x_advance = deltas[i][1];
      // if (i == 1) {
      //   System.out.println("Exact glyph not found for index = " + index +
      //   ", x_offset = " + x_offset +
      //   ", y_offset = " + y_offset +
      //   ", x_advance = " + x_advance +
      //   ", y_advance = " + y_advance + ".");
      // }
      // if (i > 0) {
      //   System.out.println("Trying delta_x_offset = " + delta_x_offset + ", delta_x_advance = " + delta_x_advance + "...");
      // }
      glyph = serializeGlyph(index, x_offset + delta_x_offset, y_offset, x_advance + delta_x_advance, y_advance);
      if (this.codepointMap.get(glyph) != null) {
        return this.codepointMap.get(glyph);
      }
    }
    System.out.println("Could not find a matching glyph for index = " + index +
      ", x_offset = " + x_offset +
      ", y_offset = " + y_offset +
      ", x_advance = " + x_advance +
      ", y_advance = " + y_advance + ". Aborting...");
    System.exit(1);
    return 0; // did not find any matching codepoint
  }

  private String encodeString(String text) {
    String result = "";

    // InputStream is;
    // // Font font = null;
    // try {
    //   is = new FileInputStream(new File("src/main/java/com/protomaps/basemap/NotoSansDevanagari-Regular.ttf"));
    //   font = Font.createFont(Font.TRUETYPE_FONT, is);
    // } catch (IOException | FontFormatException e) {
    //   e.printStackTrace();
    //   System.exit(1);
    // }


    FontRenderContext frc = new FontRenderContext(null, true, true);
    char[] charArray = text.toCharArray();
    GlyphVector glyphVector = this.font.layoutGlyphVector(frc, charArray, 0, charArray.length, 0);

    float sumXAdvances = 0;

    for (int i = 0; i < glyphVector.getNumGlyphs(); i++) {
      GlyphMetrics glyphMetrics = glyphVector.getGlyphMetrics(i);
      int glyphCode = glyphVector.getGlyphCode(i);

      double xAdvance = glyphMetrics.getAdvanceX();
      double xPosition = glyphVector.getGlyphPosition(i).getX();
      double xOffset = xPosition - sumXAdvances;

      int xAdvanceML = (int) Math.floor(1000.0 * xAdvance / 64.0);
      int xOffsetML = (int) Math.floor(1000.0 * xOffset / 64.0);

      int yAdvanceML = 0;
      int yOffsetML = 0;

      int codepoint = codepointFromGlyph(glyphCode, xOffsetML, yOffsetML, xAdvanceML, yAdvanceML);

      sumXAdvances += xAdvance;

      // System.out.println("Glyph " + i +
      //   ", Code = " + glyphCode +
      //   ", xAdvanceML = " + xAdvanceML +
      //   ", xOffsetML = " + xOffsetML +
      //   ", codepoint = " + codepoint);

      result += new StringBuilder().appendCodePoint(codepoint).toString();
    }
    return result;
  }

  private static String getScript(String text) {
    if (text.length() == 0) {
      return "";
    }

    String overallScript = "";

    for (int i = 0; i < text.length(); ++i) {
      String script = "" + Character.UnicodeScript.of(text.charAt(i));
      if (script.equals("COMMON")) {
        continue;
      }
      if (script.equals("UNKNOWN")) {
        continue;
      }
      if (script.equals("INHERITED")) {
        continue;
      }
      if (overallScript.equals("")) {
        overallScript = script;
      }
      else {
        if (script.equals(overallScript)) {
          continue;
        }
        else {
          return "MIXED";
        }
      }
    }

    if (overallScript.equals("")) {
      // all characters are in COMMON or UNKNOWN or INHERITED
      return "GENERIC";
    }

    return overallScript;
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
            newAttrs.put("pmap:encoded:" + key, encodedValue);
            newAttrs.put("pmap:script:" + key, getScript(value));
          }
        }
        item.attrs().putAll(newAttrs);
      }
    }
    return null;
  }

}
