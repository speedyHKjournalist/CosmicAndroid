/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package client;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import net.packet.Packet;
import net.server.PlayerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;
import tools.PacketCreator;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class BuddyList {
    public enum BuddyOperation {
        ADDED, DELETED
    }

    public enum BuddyAddResult {
        BUDDYLIST_FULL, ALREADY_ON_LIST, OK
    }

    private final Map<Integer, BuddylistEntry> buddies = new LinkedHashMap<>();
    private int capacity;
    private final Deque<CharacterNameAndId> pendingRequests = new LinkedList<>();
    private static final Logger log = LoggerFactory.getLogger(BuddyList.class);

    public BuddyList(int capacity) {
        this.capacity = capacity;
    }

    public boolean contains(int characterId) {
        synchronized (buddies) {
            return buddies.containsKey(characterId);
        }
    }

    public boolean containsVisible(int characterId) {
        BuddylistEntry ble;
        synchronized (buddies) {
            ble = buddies.get(characterId);
        }

        if (ble == null) {
            return false;
        }
        return ble.isVisible();

    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BuddylistEntry get(int characterId) {
        synchronized (buddies) {
            return buddies.get(characterId);
        }
    }

    public BuddylistEntry get(String characterName) {
        String lowerCaseName = characterName.toLowerCase();
        for (BuddylistEntry ble : getBuddies()) {
            if (ble.getName().toLowerCase().equals(lowerCaseName)) {
                return ble;
            }
        }

        return null;
    }

    public void put(BuddylistEntry entry) {
        synchronized (buddies) {
            buddies.put(entry.getCharacterId(), entry);
        }
    }

    public void remove(int characterId) {
        synchronized (buddies) {
            buddies.remove(characterId);
        }
    }

    public Collection<BuddylistEntry> getBuddies() {
        synchronized (buddies) {
            return Collections.unmodifiableCollection(buddies.values());
        }
    }

    public boolean isFull() {
        synchronized (buddies) {
            return buddies.size() >= capacity;
        }
    }

    public int[] getBuddyIds() {
        synchronized (buddies) {
            int[] buddyIds = new int[buddies.size()];
            int i = 0;
            for (BuddylistEntry ble : buddies.values()) {
                buddyIds[i++] = ble.getCharacterId();
            }
            return buddyIds;
        }
    }

    public void broadcast(Packet packet, PlayerStorage pstorage) {
        for (int bid : getBuddyIds()) {
            Character chr = pstorage.getCharacterById(bid);

            if (chr != null && chr.isLoggedinWorld()) {
                chr.sendPacket(packet);
            }
        }
    }

    public void loadFromDb(int characterId) {
        SQLiteDatabase con = DatabaseConnection.getConnection();
        try {
            try (Cursor cursor = con.rawQuery("SELECT b.buddyid, b.pending, b.`group`, c.name as buddyname FROM buddies as b, characters as c WHERE c.id = b.buddyid AND b.characterid = ?",
                    new String[]{String.valueOf(characterId)})) {
                while (cursor.moveToNext()) {
                    int pendingIndex = cursor.getColumnIndex("pending");
                    int buddyNameIndex = cursor.getColumnIndex("buddyname");
                    int groupIndex = cursor.getColumnIndex("group");
                    int buddyIdIndex = cursor.getColumnIndex("buddyid");

                    if (pendingIndex != -1 && buddyNameIndex != -1 && groupIndex != -1 && buddyIdIndex != -1) {
                        if (cursor.getInt(pendingIndex) == 1) {
                            pendingRequests.push(new CharacterNameAndId(cursor.getInt(buddyIdIndex), cursor.getString(buddyNameIndex)));
                        } else {
                            put(new BuddylistEntry(cursor.getString(buddyNameIndex), cursor.getString(groupIndex), cursor.getInt(buddyIdIndex), (byte) -1, true));
                        }
                    }
                }
            }
            String whereClause = "pending = ? AND characterid = ?";
            String[] whereArgs = {"1", String.valueOf(characterId)};
            con.delete("buddies", whereClause, whereArgs);
        } catch (SQLiteException ex) {
            log.error("loadFromDb error", ex);
        }
    }

    public CharacterNameAndId pollPendingRequest() {
        return pendingRequests.pollLast();
    }

    public void addBuddyRequest(Client c, int cidFrom, String nameFrom, int channelFrom) {
        put(new BuddylistEntry(nameFrom, "Default Group", cidFrom, channelFrom, false));
        if (pendingRequests.isEmpty()) {
            c.sendPacket(PacketCreator.requestBuddylistAdd(cidFrom, c.getPlayer().getId(), nameFrom));
        } else {
            pendingRequests.push(new CharacterNameAndId(cidFrom, nameFrom));
        }
    }
}
