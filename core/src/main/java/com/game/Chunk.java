package com.game;

import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.HashMap;

public class Chunk {
    public ModelCache modelCache;
    public HashMap<Vector3, ModelInstance> blocks = new HashMap<>();
    Vector3 positionInChunkCoords;

    public boolean full = false;

    public Chunk(Vector3 positionInChunkCoords) {
        modelCache = new ModelCache();
        this.positionInChunkCoords = positionInChunkCoords;
    }

    public void rebuildCache() {
        modelCache.begin();
        for (ModelInstance block : blocks.values()) {
            modelCache.add(block);
        }
        modelCache.end();
    }
}
