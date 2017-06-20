package com.broomhandleus.maximus.cowpitalism.types;

import java.io.Serializable;
import java.util.UUID;

/**
 * Class defining a single message sent over a bluetooth connection.
 *
 * type - The type of the message.
 * value - A number being sent in the message.
 * body - A string beng sent in the message.
 *
 * Upon connecting to the game-hosting device, the first thing a player
 *  should do is send a message with type=Type.JOIN_REQUEST and value=JOIN_REQUEST_VALUE.
 *  This will allow the game-host to verify that the player is in fact running Cowpitalism
 *
 */
public class BluetoothMessage implements Serializable {
    public static final int JOIN_REQUEST_VALUE = 12345;
    public enum Type {
        ACK,
        PING_CLIENT,
        JOIN_REQUEST,
        JOIN_RESPONSE
    }

    public Type type;
    public int value;
    public String body;
    public UUID id;

    public BluetoothMessage(Type type, int value, String body) {
        this.type = type;
        this.value = value;
        this.body = body;
        this.id = UUID.randomUUID();
    }
}
