package com.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
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
import com.badlogic.gdx.math.collision.BoundingBox;


import java.util.HashMap;
import java.util.Map;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    ModelBatch modelBatch;
    Model model;
    Environment environment;
    float sensitivity = 0.2f;

    float gravity = 18f;

    Player player;

    boolean fly = false;

    FastNoiseLite noise = new FastNoiseLite();

    // Using a HashMap to store chunks (each identified by its integer coordinates)
    HashMap<Vector3, Chunk> chunks = new HashMap<>();

    @Override
    public void create() {
        player = new Player(new Vector3(0,50,0));

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

        noise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        noise.SetFrequency(0.02f);

        // Generate blocks in a 300x300 grid and assign them to chunks
        for (int i = 0; i < 100; i++) {
            for (int e = 0; e < 100; e++) {
                    Vector3 blockPos = new Vector3(i, (int) (noise.GetNoise(i, e) * 20), e);

                    // Get or create the chunk for this block position

                    Chunk chunk = getOrCreateChunk(i, (int) (noise.GetNoise(i, e) * 20), e);
                    // Create a new block instance and set its translation
                    ModelInstance blockInstance = new ModelInstance(model);
                    blockInstance.transform.setToTranslation(blockPos);

                    // Store the block in the chunk using its integer coordinates as key
                    // (We create a new Vector3 with integer values to avoid precision issues)
                    chunk.blocks.put(blockPos, blockInstance);


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
        Gdx.gl.glClearColor(0.7f, 0.9f, 1.0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Update camera based on mouse and keyboard input
        handleMouseInput();
        handleKeyboardInput(Gdx.graphics.getDeltaTime());
        if(!fly){
            handleGravity(Gdx.graphics.getDeltaTime());
        }


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
        modelBatch.begin(player.camera);
        for (Chunk chunk : chunks.values()) {
            modelBatch.render(chunk.modelCache, environment);
        }
        modelBatch.end();
    }

    void handleMouseInput() {
        float deltaX = -Gdx.input.getDeltaX() * sensitivity;
        float deltaY = -Gdx.input.getDeltaY() * sensitivity;

        player.camera.direction.rotate(player.camera.up, deltaX);
        player.camera.direction.rotate(player.camera.direction.cpy().crs(player.camera.up), deltaY);
    }

    void handleKeyboardInput(float deltaTime) {
        Vector3 forward = new Vector3(player.camera.direction.x, 0, player.camera.direction.z).nor();
        Vector3 right = new Vector3(player.camera.direction).crs(player.camera.up).nor();
        Vector3 up = new Vector3(0, 1, 0);
        Vector3 moveDirection = new Vector3();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveDirection.add(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveDirection.sub(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveDirection.sub(right);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveDirection.add(right);
        if(fly){
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) moveDirection.add(up);
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) moveDirection.sub(up);
        }else {
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)&& player.onGround){
                player.verticalVelocity = 7f;
                player.onGround = false;
            }
        }

        moveDirection.nor().scl(10f);
        moveDirection.scl(deltaTime);

        Vector3 newPosition = new Vector3(player.camera.position).add(moveDirection);

        if(canWalkThere(newPosition, player.camera.position)){
            player.setPosition(newPosition);
        }
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        model.dispose();
        for (Chunk chunk : chunks.values()) {
            chunk.modelCache.dispose();
        }
    }

    void handleGravity(float deltaTime){
        player.verticalVelocity -= gravity*deltaTime;

        Vector3 newPosition = new Vector3(player.camera.position).add(new Vector3(0,player.verticalVelocity*deltaTime,0));

        if(canWalkThere(newPosition, player.camera.position)){
            player.setPosition(newPosition);
            player.onGround = false;
        }else {
            player.verticalVelocity = 0;
            player.onGround = true;
        }
    }

    boolean canWalkThere(Vector3 newPosition, Vector3 oldPosition){

        player.setOnlyBox(newPosition);

        BoundingBox boundingBox = new BoundingBox();

        for(Map.Entry<Vector3, Chunk> entry : chunks.entrySet()){
            for (Vector3 blockPos : entry.getValue().blocks.keySet()){
                Vector3 halfSize = new Vector3(0.5f, 0.5f, 0.5f);

                // compute min and max without touching blockPos:
                Vector3 min = blockPos.cpy().sub(halfSize);
                Vector3 max = blockPos.cpy().add(halfSize);

                boundingBox.set(min, max);
                if (boundingBox.intersects(player.playerBox)) {
                    return false;
                }
            }
        }

        player.setOnlyBox(oldPosition);

        return true;
    }

    ModelInstance getTargetedBlock(float maxDistance) {
        Vector3 rayStart = new Vector3(player.camera.position);
        Vector3 rayDirection = new Vector3(player.camera.direction).nor();

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


    Model createTexturedCube(Material material) {

        ModelBuilder modelBuilder = new ModelBuilder();
        return modelBuilder.createBox(1f, 1f, 1f, material,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
    }


    public Chunk getChunkAt(int x, int y, int z) {
        int cx = x / 16;
        int cy = y / 16;
        int cz = z / 16;
        // Using integer-based coordinates for chunk key
        Vector3 chunkPos = new Vector3(cx, cy, cz);
        return chunks.get(chunkPos);
    }

    public Chunk getOrCreateChunk(int x, int y, int z) {
        int cx = x / 16;
        int cy = y / 16;
        int cz = z / 16;
        Vector3 chunkPos = new Vector3(cx, cy, cz);
        return chunks.computeIfAbsent(chunkPos, k -> new Chunk(new Vector3(cx, cy, cz)));
    }

    public void populateChunk(Chunk chunk){
        for (int i = 0; i < 16; i++) {
            for (int e = 0; e < 16; e++) {
                Vector3 blockPos = new Vector3(chunk.positionInChunkCoords.x*16+i, (int) (noise.GetNoise(i, e) * 20), chunk.positionInChunkCoords.z*16+e);

                // Create a new block instance and set its translation
                ModelInstance blockInstance = new ModelInstance(model);
                blockInstance.transform.setToTranslation(blockPos);

                // Store the block in the chunk using its integer coordinates as key
                // (We create a new Vector3 with integer values to avoid precision issues)
                chunk.blocks.put(blockPos, blockInstance);
            }
        }

        chunk.full = true;
    }
}
