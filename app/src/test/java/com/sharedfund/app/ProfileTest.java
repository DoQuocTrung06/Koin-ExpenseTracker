package com.sharedfund.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.sharedfund.app.model.Profile;

import org.junit.Test;

public class ProfileTest {

    @Test
    public void testProfileCustomFields() {
        Profile profile = new Profile("user-123", "test@sharedfund.vn", "Nguyễn Văn A", "https://avatar.png");
        assertNull(profile.getNickname());
        assertNull(profile.getOccupation());
        assertNull(profile.getEducation());
        assertNull(profile.getBio());

        profile.setNickname("Alex");
        profile.setOccupation("Kỹ sư phần mềm");
        profile.setEducation("Đại học Bách Khoa");
        profile.setBio("Chia sẻ chi tiêu thông minh");

        assertEquals("user-123", profile.getId());
        assertEquals("test@sharedfund.vn", profile.getEmail());
        assertEquals("Nguyễn Văn A", profile.getFullName());
        assertEquals("Alex", profile.getNickname());
        assertEquals("Kỹ sư phần mềm", profile.getOccupation());
        assertEquals("Đại học Bách Khoa", profile.getEducation());
        assertEquals("Chia sẻ chi tiêu thông minh", profile.getBio());
    }

    @Test
    public void testDefaultConstructor() {
        Profile profile = new Profile();
        profile.setId("id-999");
        profile.setFullName("User 999");
        profile.setEmail("user999@test.com");
        profile.setWalletBalance(150000.0);

        assertEquals("id-999", profile.getId());
        assertEquals("User 999", profile.getFullName());
        assertEquals("user999@test.com", profile.getEmail());
        assertEquals(150000.0, profile.getWalletBalance(), 0.001);
    }
}
