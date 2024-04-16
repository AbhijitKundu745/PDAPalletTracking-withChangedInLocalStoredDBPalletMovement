package com.psl.pallettracking.database;

public class RoomMaster {
    String roomId,roomName,roomRfid;

    public String getRoomRfid() {
        return roomRfid;
    }

    public void setRoomRfid(String roomRfid) {
        this.roomRfid = roomRfid;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}
