package com.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.*;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    PerspectiveCamera camera;
    ModelBatch modelBatch;
    Model model;
    ModelInstance modelInstance;
    Environment environment;
    float sensitivity = 0.2f;

    @Override
    public void create() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(2f, 2f, 4f); // Move the camera back to see the cube
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        ModelBuilder modelBuilder = new ModelBuilder();
         model= modelBuilder.createBox(2f, 2f, 2f,
            new com.badlogic.gdx.graphics.g3d.Material(),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        modelInstance = new ModelInstance(model);

        // Set up ModelBatch for rendering
        modelBatch = new ModelBatch();

        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        handleMouseInput();
        handleKeyboardInput(Gdx.graphics.getDeltaTime());

        modelInstance.transform.rotate(0, 1, 0, 1f); // Rotate around Y-axis

        // Update camera
        camera.update();

        // Render the cube
        modelBatch.begin(camera);
        modelBatch.render(modelInstance, environment);
        modelBatch.end();
    }

    private void handleMouseInput() {
        float deltaX = -Gdx.input.getDeltaX() * sensitivity;
        float deltaY = -Gdx.input.getDeltaY() * sensitivity;

        // Rotate the camera directly
        camera.direction.rotate(camera.up, deltaX); // Horizontal rotation (yaw)
        camera.direction.rotate(camera.direction.cpy().crs(camera.up), deltaY); // Vertical rotation (pitch)
    }

    private void handleKeyboardInput(float deltaTime) {
        Vector3 forward = new Vector3(camera.direction.x, 0, camera.direction.z).nor(); // Ignore vertical movement
        Vector3 right = new Vector3(camera.direction).crs(camera.up).nor(); // Perpendicular to forward

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

        moveDirection.nor();

        camera.position.add(moveDirection);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        model.dispose();
    }
}
