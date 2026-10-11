package com.dmaryanskiy.vanillareforged.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Builds a model's boxes from {@code assets/vanillareforged/geometry/<name>.json}, for models with
 * too many boxes to read as code. A file lists its parts, parents before children; each part has a
 * pivot, a rotation and its boxes (texture offset, corner and size), as in vanilla's model code.
 * It's read from the mod's own files, not through resource packs.
 */
public final class JsonGeometry {
	private JsonGeometry() {
	}

	public static LayerDefinition load(String name) {
		String path = "/assets/" + VanillaReforged.MOD_ID + "/geometry/" + name + ".json";
		try (InputStream stream = JsonGeometry.class.getResourceAsStream(path)) {
			if (stream == null) {
				throw new IllegalStateException("Missing model geometry " + path);
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				return build(JsonParser.parseReader(reader).getAsJsonObject());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static LayerDefinition build(JsonObject geometry) {
		MeshDefinition mesh = new MeshDefinition();
		Map<String, PartDefinition> parts = new HashMap<>();
		for (JsonElement element : geometry.getAsJsonArray("parts")) {
			JsonObject part = element.getAsJsonObject();
			CubeListBuilder cubes = CubeListBuilder.create();
			for (JsonElement boxElement : part.getAsJsonArray("boxes")) {
				JsonObject box = boxElement.getAsJsonObject();
				JsonArray uv = box.getAsJsonArray("uv");
				float[] from = floats(box, "from");
				float[] size = floats(box, "size");
				cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt()).addBox(from[0], from[1], from[2], size[0], size[1], size[2]);
			}
			String name = part.get("name").getAsString();
			PartDefinition parent = part.get("parent").isJsonNull() ? mesh.getRoot() : parts.get(part.get("parent").getAsString());
			float[] pivot = floats(part, "pivot");
			float[] rotation = floats(part, "rotation");
			parts.put(name, parent.addOrReplaceChild(name, cubes,
					PartPose.offsetAndRotation(pivot[0], pivot[1], pivot[2], rotation[0], rotation[1], rotation[2])));
		}
		JsonArray texture = geometry.getAsJsonArray("texture");
		return LayerDefinition.create(mesh, texture.get(0).getAsInt(), texture.get(1).getAsInt());
	}

	private static float[] floats(JsonObject object, String key) {
		JsonArray array = object.getAsJsonArray(key);
		return new float[] {array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
	}
}
