package com.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class Player
{


    Vector3 position;

    public Player(Vector3 pos){

        setPosition(pos);

    }

    void setPosition(Vector3 newpos){
        position = newpos;

    }
}
