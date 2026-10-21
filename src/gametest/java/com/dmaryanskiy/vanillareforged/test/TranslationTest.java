package com.dmaryanskiy.vanillareforged.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class TranslationTest {
	private static final List<String> LANGUAGES = List.of("ru_ru", "de_de", "fr_fr", "es_es", "it_it");

	@GameTest
	public void everyLanguageHasEveryEnglishString(GameTestHelper helper) {
		Set<String> english = read(helper, "en_us").keySet();
		for (String language : LANGUAGES) {
			Map<String, String> strings = read(helper, language);
			Set<String> missing = new TreeSet<>(english);
			missing.removeAll(strings.keySet());
			Set<String> extra = new TreeSet<>(strings.keySet());
			extra.removeAll(english);
			helper.assertTrue(missing.isEmpty(), language + " is missing " + missing);
			helper.assertTrue(extra.isEmpty(), language + " has strings English doesn't: " + extra);
			strings.forEach((key, value) -> helper.assertFalse(value.isBlank(), language + " has an empty " + key));
		}
		helper.succeed();
	}

	private static Map<String, String> read(GameTestHelper helper, String language) {
		try (InputStream in = TranslationTest.class.getResourceAsStream("/assets/vanillareforged/lang/" + language + ".json")) {
			helper.assertTrue(in != null, "No " + language + " translation");
			return new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), new TypeToken<Map<String, String>>() {}.getType());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
