package com.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
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
import com.badlogic.gdx.graphics.g3d.*;


import java.util.ArrayList;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    PerspectiveCamera camera;
    ModelBatch modelBatch;
    Model model;
    ModelInstance modelInstance;
    Environment environment;
    float sensitivity = 0.2f;



    ModelCache modelCache = new ModelCache();

    ArrayList<ModelInstance> blocks = new ArrayList<ModelInstance>();

    @Override
    public void create() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(2f, 2f, 4f); // Move the camera back to see the cube
        camera.near = 0.1f;
        camera.far = 1000f;
        camera.update();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        Material material = new com.badlogic.gdx.graphics.g3d.Material();

        Texture texture = new Texture("stone.jpg");
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        material.set(TextureAttribute.createDiffuse(texture));

        ModelBuilder modelBuilder = new ModelBuilder();
        model= modelBuilder.createBox(1f, 1f, 1f, material,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        FastNoiseLite noise = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        noise.SetFrequency(0.02f);

        modelCache.begin();
        for (int i = 0; i<300; i++){
            for (int e = 0; e<300; e++){
                    modelInstance = new ModelInstance(model);
                    modelInstance.transform.setToTranslation(i, (int)(noise.GetNoise(i, e)*20), e);
                    blocks.add(modelInstance);
                    modelCache.add(modelInstance);
            }
        }
        modelCache.end();
        modelBatch = new ModelBatch();

        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        handleMouseInput();
        handleKeyboardInput(Gdx.graphics.getDeltaTime());

        camera.update();

        modelBatch.begin(camera);
        modelBatch.render(modelCache, environment);
        modelBatch.end();
    }

    void handleMouseInput() {
        float deltaX = -Gdx.input.getDeltaX() * sensitivity;
        float deltaY = -Gdx.input.getDeltaY() * sensitivity;

        camera.direction.rotate(camera.up, deltaX);
        camera.direction.rotate(camera.direction.cpy().crs(camera.up), deltaY);
    }

   void handleKeyboardInput(float deltaTime) {
        Vector3 forward = new Vector3(camera.direction.x, 0, camera.direction.z).nor(); // Ignore vertical movement
        Vector3 right = new Vector3(camera.direction).crs(camera.up).nor(); // Perpendicular to forward
        Vector3 up = new Vector3(0,1,0); // Perpendicular to forward
        Vector3 moveDirection = new Vector3();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            moveDirection.add(forward);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            moveDirection.sub(forward);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            moveDirection.sub(right);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            moveDirection.add(right);
        }

       if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
           moveDirection.add(up);
       }
       if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
           moveDirection.sub(up);
       }

        moveDirection.nor();
        moveDirection.x *= 0.2;
        moveDirection.y *= 0.2;
        moveDirection.z *= 0.2;


       camera.position.add(moveDirection);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        model.dispose();
    }
}
