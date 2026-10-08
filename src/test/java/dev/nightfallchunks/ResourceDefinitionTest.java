package dev.nightfallchunks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ResourceDefinitionTest {
    private static JsonObject resource(String path) throws Exception {
        try (var stream = ResourceDefinitionTest.class.getResourceAsStream(path)) {
            assertTrue(stream != null, "Missing resource: " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void presetIsAirOnlyAndOptIn() throws Exception {
        JsonObject preset = resource("/data/nightfallchunks/worldgen/world_preset/one_chunk.json");
        JsonObject generator = preset.getAsJsonObject("dimensions")
            .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
        assertEquals("minecraft:flat", generator.get("type").getAsString());
        JsonObject settings = generator.getAsJsonObject("settings");
        assertEquals("nightfallchunks:island", settings.get("biome").getAsString());
        assertEquals(1, settings.getAsJsonArray("layers").size());
        assertEquals("minecraft:air", settings.getAsJsonArray("layers").get(0)
            .getAsJsonObject().get("block").getAsString());

        JsonObject tag = resource("/data/minecraft/tags/worldgen/world_preset/normal.json");
        assertTrue(tag.getAsJsonArray("values").toString().contains("nightfallchunks:one_chunk"));
        assertTrue(resource("/data/nightfallchunks/worldgen/biome/island.json")
            .getAsJsonObject("spawners").getAsJsonArray("monster").size() > 0);
        JsonObject source = preset.getAsJsonObject("dimensions")
            .getAsJsonObject("nightfallchunks:source").getAsJsonObject("generator");
        assertEquals("minecraft:noise", source.get("type").getAsString());
    }

    @Test
    void seedRecipePointsToRegisteredItem() throws Exception {
        JsonObject recipe = resource("/data/nightfallchunks/recipes/chunk_seed.json");
        assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
        assertEquals("nightfallchunks:chunk_seed", recipe.getAsJsonObject("result").get("item").getAsString());
        assertEquals(6, recipe.getAsJsonArray("ingredients").size());
        String fuelAlternatives = recipe.getAsJsonArray("ingredients").get(5).getAsJsonArray().toString();
        assertTrue(fuelAlternatives.contains("minecraft:coal"));
        assertTrue(fuelAlternatives.contains("minecraft:charcoal"));
        JsonObject model = resource("/assets/nightfallchunks/models/item/chunk_seed.json");
        assertEquals(6, model.getAsJsonArray("elements").size());
        assertEquals("minecraft:block/azalea_leaves", model.getAsJsonObject("textures").get("leaves").getAsString());
        assertTrue(!model.toString().contains("emerald_block"));
        assertTrue(model.getAsJsonObject("display").has("gui"));
    }

    @Test
    void onboardingAndWorldTypeAreLocalized() throws Exception {
        for (String locale : new String[] { "en_us", "de_de" }) {
            JsonObject lang = resource("/assets/nightfallchunks/lang/" + locale + ".json");
            assertEquals("One Chunk", lang.get("generator.nightfallchunks.one_chunk").getAsString());
            for (String key : new String[] {
                "message.nightfallchunks.welcome.title",
                "message.nightfallchunks.welcome.subtitle",
                "message.nightfallchunks.welcome.hint",
                "tooltip.nightfallchunks.chunk_seed.use",
                "tooltip.nightfallchunks.chunk_seed.recipe",
                "key.nightfallchunks.open_journal",
                "screen.nightfallchunks.journal.title",
                "screen.nightfallchunks.journal.total",
                "screen.nightfallchunks.journal.position",
                "screen.nightfallchunks.journal.legend",
                "screen.nightfallchunks.journal.controls",
                "screen.nightfallchunks.journal.wrong_world"
            }) {
                assertTrue(lang.has(key), "Missing " + locale + " translation: " + key);
            }
        }
    }

    @Test
    void pickingUpStarterSeedUnlocksRecipeBookEntry() throws Exception {
        JsonObject advancement = resource("/data/nightfallchunks/advancements/recipes/chunk_seed.json");
        assertEquals("minecraft:recipes/root", advancement.get("parent").getAsString());
        assertEquals("minecraft:inventory_changed", advancement.getAsJsonObject("criteria")
            .getAsJsonObject("has_chunk_seed").get("trigger").getAsString());
        assertTrue(advancement.getAsJsonObject("criteria").getAsJsonObject("has_chunk_seed")
            .getAsJsonObject("conditions").toString().contains("nightfallchunks:chunk_seed"));
        assertEquals("nightfallchunks:chunk_seed", advancement.getAsJsonObject("rewards")
            .getAsJsonArray("recipes").get(0).getAsString());
    }
}
