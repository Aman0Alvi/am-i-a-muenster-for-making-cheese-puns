package org.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class App {
    private static boolean isMissing(String s) {
        if (s == null) return true;
        String t = s.trim();
        return t.isEmpty() || t.equalsIgnoreCase("na") || t.equalsIgnoreCase("null");
    }

    public static void main(String[] args) {
        Path input = Path.of("../cheese_data.csv"); 
        Path output = Path.of("output.txt");

        long pasteurized = 0;
        long raw = 0;
        long organicHighMoisture = 0;

        Map<String, Long> milkTypeCounts = new HashMap<>();
        milkTypeCounts.put("cow", 0L);
        milkTypeCounts.put("goat", 0L);
        milkTypeCounts.put("ewe", 0L);     
        milkTypeCounts.put("buffalo", 0L);

        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreSurroundingSpaces(true).setIgnoreEmptyLines(true).setQuote('"').build();

        try (CSVParser parser = CSVParser.parse(input, StandardCharsets.UTF_8, format)) {
            for (CSVRecord r : parser) {
                String treatment = r.get("MilkTreatmentTypeEn");
                if (!isMissing(treatment)) {
                    String lt = treatment.toLowerCase(Locale.ROOT);
                    if (lt.contains("pasteur")) {
                        pasteurized++;
                    } else if (lt.contains("raw")) {
                        raw++;
                    }
                }
              
                String organicStr = r.get("Organic");
                String moistureStr = r.get("MoisturePercent");
                if (!isMissing(organicStr) && !isMissing(moistureStr)) {
                    try {
                        int organic = Integer.parseInt(organicStr.trim());
                        double moisture = Double.parseDouble(moistureStr.trim());
                        if (organic == 1 && moisture > 41.0) {
                            organicHighMoisture++;
                        }
                    } catch (NumberFormatException ignored) {
                        
                    }
                }

                String milkType = r.get("MilkTypeEn");
                if (!isMissing(milkType)) {
                    String lt = milkType.toLowerCase(Locale.ROOT);
                    String bucket = null;
                    if (lt.contains("cow")) 
                        bucket = "cow";
                    else if (lt.contains("goat")) 
                        bucket = "goat";
                    else if (lt.contains("ewe") || lt.contains("sheep"))
                         bucket = "ewe";
                    else if (lt.contains("buffalo")) 
                        bucket = "buffalo";

                    if (bucket != null) {
                        milkTypeCounts.put(bucket, milkTypeCounts.get(bucket) + 1);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read CSV: " + e.getMessage());
            return;
        }

        String mostCommonMilkType = "unknown";
        long max = -1;
        for (Map.Entry<String, Long> entry : milkTypeCounts.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                mostCommonMilkType = entry.getKey();
            }
        }

        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
        try (BufferedWriter w = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            w.write("Pasteurized milk cheeses: " + nf.format(pasteurized));
            w.newLine();
            w.write("Raw milk cheeses: " + nf.format(raw));
            w.newLine();
            w.write("Organic cheeses with moisture > 41.0%: " + nf.format(organicHighMoisture));
            w.newLine();
            w.write("Most common milk type: " + mostCommonMilkType + " (" + nf.format(max) + ")");
            w.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write output.txt: " + e.getMessage());
        }

        System.out.println("Done. Wrote results to " + output.toAbsolutePath());
    }
}
