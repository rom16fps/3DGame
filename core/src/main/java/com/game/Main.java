package com.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.Material;


import java.util.HashMap;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    PerspectiveCamera camera;
    ModelBatch modelBatch;
    Model model;
    Environment environment;
    float sensitivity = 0.2f;

    // Using a HashMap to store chunks (each identified by its integer coordinates)
    HashMap<Vector3, Chunk> chunks = new HashMap<>();

    @Override
    public void create() {
        // Set up camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(2f, 2f, 4f);
        camera.near = 0.1f;
        camera.far = 1000f;
        camera.update();

        // Set up lighting/environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        // Create a material with a texture
        Material material = new Material();
        Texture texture = new Texture("stone.jpg");
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        material.set(TextureAttribute.createDiffuse(texture));

        model = createTexturedCube(material);
        // Create a noise generator (using FastNoiseLite, assumed to be available)
        FastNoiseLite noise = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        noise.SetFrequency(0.02f);

        // Generate blocks in a 300x300 grid and assign them to chunks
        for (int i = 0; i < 300; i++) {
            for (int e = 0; e < 300; e++) {
                int height = (int) (noise.GetNoise(i, e) * 20);
                Vector3 blockPos = new Vector3(i, height, e);

                // Get or create the chunk for this block position
                Chunk chunk = getOrCreateChunk(i, height, e);

                // Create a new block instance and set its translation
                ModelInstance blockInstance = new ModelInstance(model);
                blockInstance.transform.setToTranslation(blockPos);

                // Store the block in the chunk using its integer coordinates as key
                // (We create a new Vector3 with integer values to avoid precision issues)
                chunk.blocks.put(new Vector3((int) blockPos.x, (int) blockPos.y, (int) blockPos.z), blockInstance);
            }
        }

        // Build the model caches for all chunks after initialization
        for (Chunk chunk : chunks.values()) {
            chunk.rebuildCache();
        }

        modelBatch = new ModelBatch();
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render() {
        // Clear the screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Update camera based on mouse and keyboard input
        handleMouseInput();
        handleKeyboardInput(Gdx.graphics.getDeltaTime());
        camera.update();

        // Handle left-click for block breaking
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            ModelInstance targetBlock = getTargetedBlock(5.0f);
            if (targetBlock != null) {
                // Get block position (rounded to integer grid coordinates)
                Vector3 blockPos = targetBlock.transform.getTranslation(new Vector3());
                int bx = Math.round(blockPos.x);
                int by = Math.round(blockPos.y);
                int bz = Math.round(blockPos.z);

                Chunk chunk = getChunkAt(bx, by, bz);
                if (chunk != null) {
                    // Remove block from chunk and update only that chunk's cache
                    if (chunk.blocks.remove(new Vector3(bx, by, bz)) != null) {
                        chunk.rebuildCache();
                    }
                }
            }
        }

        // Render each chunk's cached model
        modelBatch.begin(camera);
        for (Chunk chunk : chunks.values()) {
            modelBatch.render(chunk.modelCache, environment);
        }
        modelBatch.end();
    }

    void handleMouseInput() {
        float deltaX = -Gdx.input.getDeltaX() * sensitivity;
        float deltaY = -Gdx.input.getDeltaY() * sensitivity;

        camera.direction.rotate(camera.up, deltaX);
        camera.direction.rotate(camera.direction.cpy().crs(camera.up), deltaY);
    }

    void handleKeyboardInput(float deltaTime) {
        Vector3 forward = new Vector3(camera.direction.x, 0, camera.direction.z).nor();
        Vector3 right = new Vector3(camera.direction).crs(camera.up).nor();
        Vector3 up = new Vector3(0, 1, 0);
        Vector3 moveDirection = new Vector3();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveDirection.add(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveDirection.sub(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveDirection.sub(right);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveDirection.add(right);
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) moveDirection.add(up);
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) moveDirection.sub(up);

        moveDirection.nor().scl(0.2f);
        camera.position.add(moveDirection);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        model.dispose();
        for (Chunk chunk : chunks.values()) {
            chunk.modelCache.dispose();
        }
    }


    /**
     * Casts a ray from the camera position and checks each stepped grid coordinate
     * to see if a block exists in the corresponding chunk.
     */

    public ModelInstance getTargetedBlock(float maxDistance) {
        Vector3 rayStart = new Vector3(camera.position);
        Vector3 rayDirection = new Vector3(camera.direction).nor();

        // Step along the ray in small increments
        for (float d = 0; d < maxDistance; d += 0.1f) {
            Vector3 point = new Vector3(rayStart).mulAdd(rayDirection, d);

            int x = Math.round(point.x);
            int y = Math.round(point.y);
            int z = Math.round(point.z);

            Chunk chunk = getChunkAt(x, y, z);
            if (chunk != null) {
                Vector3 key = new Vector3(x, y, z);
                if (chunk.blocks.containsKey(key)) {
                    return chunk.blocks.get(key);
                }
            }
        }
        return null;
    }


    private Model createTexturedCube(Material material) {
        // Create the model using ModelBuilder's createBox method
        // This automatically handles the UV mapping for the cube
        ModelBuilder modelBuilder = new ModelBuilder();
        return modelBuilder.createBox(1f, 1f, 1f, material,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
    }

    /**
     * Returns the chunk for the given block coordinates.
     */


    public Chunk getChunkAt(int x, int y, int z) {
        int cx = x / 16;
        int cy = y / 16;
        int cz = z / 16;
        // Using integer-based coordinates for chunk key
        Vector3 chunkPos = new Vector3(cx, cy, cz);
        return chunks.get(chunkPos);
    }

    /**
     * Gets an existing chunk or creates a new one for the given block coordinates.
     */
    public Chunk getOrCreateChunk(int x, int y, int z) {
        int cx = x / 16;
        int cy = y / 16;
        int cz = z / 16;
        Vector3 chunkPos = new Vector3(cx, cy, cz);
        return chunks.computeIfAbsent(chunkPos, k -> new Chunk());
    }

}
