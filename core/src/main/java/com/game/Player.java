package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class Player
{

    BoundingBox playerBox = new BoundingBox();

    Vector3 position;

    Vector3 boxSize = new Vector3(0.5f,1.8f,0.5f);

    PerspectiveCamera camera = new PerspectiveCamera(67,Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    boolean onGround;

    float verticalVelocity = 0f;

    public Player(Vector3 pos){

        setPosition(pos);

        camera.position.set(position);
        camera.near = 0.1f;
        camera.far = 1000f;
        camera.update();

    }

    void setPosition(Vector3 newpos){
        playerBox.set(new Vector3(newpos.x-boxSize.x/2, newpos.y-boxSize.y/2-0.5f, newpos.z-boxSize.z/2), new Vector3(newpos.x+boxSize.x/2, newpos.y+boxSize.y/2-0.5f, newpos.z+boxSize.z/2));
        position = newpos;
        camera.position.set(newpos);
        camera.update();
    }

    void setOnlyBox(Vector3 newpos){
        playerBox.set(new Vector3(newpos.x-boxSize.x/2, newpos.y-boxSize.y/2-0.5f, newpos.z-boxSize.z/2), new Vector3(newpos.x+boxSize.x/2, newpos.y+boxSize.y/2-0.5f, newpos.z+boxSize.z/2));
    }
}
