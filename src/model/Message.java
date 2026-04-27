package model;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private String sender;
    private String type;
    private String room;
    private String receiver;
    private String content;
    private LocalDateTime createdAt;

    public Message() {
    }

    public Message(int id, String sender, String type, String room, String receiver, String content, LocalDateTime createdAt) {
        this.id = id;
        this.sender = sender;
        this.type = type;
        this.room = room;
        this.receiver = receiver;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Message(String sender, String type, String room, String receiver, String content, LocalDateTime createdAt) {
        this.sender = sender;
        this.type = type;
        this.room = room;
        this.receiver = receiver;
        this.content = content;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }    

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
