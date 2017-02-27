package no.rutebanken.anshar.routes.siri.transformer.impl;


import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class BaneNorIdReplacer extends ValueAdapter {

    private static Map<String, String> stopPlaceMappings = new HashMap<>();

    private void init() {
        if (stopPlaceMappings.isEmpty()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("banenor_mapping.csv")))) {
                reader.lines().forEach(line -> {

                    StringTokenizer tokenizer = new StringTokenizer(line, ",");
                    String shortName = tokenizer.nextToken();
                    String nsrId = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;

                    stopPlaceMappings.put(shortName, nsrId);
                });
            } catch (IOException io) {
            }
        }
    }


    public String apply(String text) {
        if (text != null) {
            String nsrId = stopPlaceMappings.get(text);
            if (nsrId != null) {
                return nsrId;
            }
        }
        return text;
    }

    public BaneNorIdReplacer(Class clazz) {
        super(clazz);
        init();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaneNorIdReplacer)) return false;

        BaneNorIdReplacer that = (BaneNorIdReplacer) o;

        return (super.getClassToApply().equals(that.getClassToApply()));
    }
}