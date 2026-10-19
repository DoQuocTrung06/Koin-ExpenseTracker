package com.sharedfund.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.model.Room;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SupabaseApiTest {

    @Test
    public void testCreateAndGetRooms() throws InterruptedException {
        RoomRepository repository = new RoomRepository();
        CountDownLatch latch = new CountDownLatch(1);

        String randomCode = "SF" + (int)(Math.random() * 900000 + 100000);
        Room newRoom = new Room("Phong Tro 524h0132", "Quy test kiem thu", randomCode, 0.0, null);

        repository.createRoom(newRoom, new RoomRepository.DataCallback<Room>() {
            @Override
            public void onSuccess(Room createdRoom) {
                assertNotNull(createdRoom);

                repository.getAllRooms(new RoomRepository.DataCallback<List<Room>>() {
                    @Override
                    public void onSuccess(List<Room> rooms) {
                        assertNotNull(rooms);
                        assertTrue(rooms.size() > 0);
                        System.out.println("TEST SUCCESS: Found " + rooms.size() + " rooms on Supabase!");
                        System.out.println("First room name: " + rooms.get(0).getName());
                        latch.countDown();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        System.err.println("GET ERROR: " + errorMessage);
                        latch.countDown();
                    }
                });
                System.out.println("CREATED ROOM ID: " + createdRoom.getId());
                System.out.println("JOIN CODE: " + createdRoom.getJoinCode());
            }

            @Override
            public void onError(String errorMessage) {
                System.err.println("CREATE ERROR: " + errorMessage);
                latch.countDown();
            }
        });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue("Test timed out", completed);
    }
}
