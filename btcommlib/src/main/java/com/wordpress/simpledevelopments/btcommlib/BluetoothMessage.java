package com.wordpress.simpledevelopments.btcommlib;

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
 *  should do is send a message with type=Type.JOIN_REQUEST
 *
 */
public class BluetoothMessage implements Serializable {
    public enum Type {
        INTERNAL_USE,
        CLIENT_USE,
//        ACK,
//        PING_CLIENT,
//        JOIN_REQUEST,
//        JOIN_RESPONSE,
//        GRAVEYARD,
//        BURGER_JOINT
    }

    public Type type;
    public String content;
    public String contentType;
    public UUID id;

    public BluetoothMessage(Type type, String contentType, String content) {
        this.type = type;
        this.contentType = contentType;
        this.content = content;
        this.id = UUID.randomUUID();
    }
}
