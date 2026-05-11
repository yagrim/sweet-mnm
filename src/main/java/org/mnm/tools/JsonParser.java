package org.mnm.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JsonParser {

    private static final Gson GSON = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

    private static final Type MAP_TYPE =
            new TypeToken<Map<String, Object>>() {
            }.getType();


    public static Map<String, Object> read(byte[] json) {
        return GSON.fromJson(inputStreamReader(json), MAP_TYPE);
    }

    public static <T> T read(byte[] json, Class<T> type) {
        return read(inputStreamReader(json), type);
    }

    public static <T> T read(Reader reader, Class<T> type) {
        return GSON.fromJson(reader, type);
    }

    private static InputStreamReader inputStreamReader(byte[] json) {
        return new InputStreamReader(new ByteArrayInputStream(json), StandardCharsets.UTF_8);
    }

    public static String toJson(Map<String, Object> values) {
        return GSON.toJson(values);
    }

}
