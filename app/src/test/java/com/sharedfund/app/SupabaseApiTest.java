package com.sharedfund.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.SupabaseRealtimeClient;
import com.sharedfund.app.data.TransactionRepository;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

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

    @Test
    public void testRealtimeTransactionSync() throws InterruptedException {
        RoomRepository roomRepository = new RoomRepository();
        TransactionRepository transactionRepository = new TransactionRepository();
        SupabaseRealtimeClient realtimeClient = new SupabaseRealtimeClient();
        CountDownLatch latch = new CountDownLatch(1);

        roomRepository.getAllRooms(new RoomRepository.DataCallback<List<Room>>() {
            @Override
            public void onSuccess(List<Room> rooms) {
                assertTrue(rooms.size() > 0);
                String testRoomId = rooms.get(0).getId();

                realtimeClient.setRealtimeListener(new SupabaseRealtimeClient.RealtimeListener() {
                    @Override
                    public void onTransactionInserted(Transaction transaction) {
                        System.out.println("REALTIME EVENT RECEIVED: " + transaction.getNote() + " - " + transaction.getFormattedAmount());
                        realtimeClient.disconnect();
                        latch.countDown();
                    }
                });

                realtimeClient.connect();

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }

                Transaction newTx = new Transaction(testRoomId, "Trung 524h0132", 35000.0, "EXPENSE", "An uong", "Mua tra sua Test Realtime");
                transactionRepository.createTransaction(newTx, new TransactionRepository.DataCallback<Transaction>() {
                    @Override
                    public void onSuccess(Transaction createdTx) {
                        System.out.println("INSERTED TX VIA REST: " + createdTx.getNote());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        System.err.println("INSERT TX ERROR: " + errorMessage);
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                System.err.println("GET ROOM ERROR: " + errorMessage);
                latch.countDown();
            }
        });

        boolean received = latch.await(15, TimeUnit.SECONDS);
        assertTrue("Realtime event not received in time", received);
    }
}
