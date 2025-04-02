package com.game;

import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.HashMap;

public class Chunk {
    public ModelCache modelCache;
    public HashMap<Vector3, ModelInstance> blocks = new HashMap<>();

    public Chunk() {
        modelCache = new ModelCache();
    }

    public void rebuildCache() {
        modelCache.begin();
        for (ModelInstance block : blocks.values()) {
            modelCache.add(block);
        }
        modelCache.end();
    }
}
