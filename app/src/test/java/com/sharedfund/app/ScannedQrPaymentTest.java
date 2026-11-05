package com.sharedfund.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.sharedfund.app.model.ScannedQrPayment;

import org.junit.Test;

public class ScannedQrPaymentTest {

    @Test
    public void testParseStandardVietcombankVietQr() {
        String payload = "00020101021238540010A00000072701240006970436011010470616150208QRIBFTTA5303704540450005802VN5913DO QUOC TRUNG6006HA NOI62190815QUY AN TET 20266304ABCD";
        ScannedQrPayment parsed = ScannedQrPayment.parse(payload);

        assertNotNull(parsed);
        assertEquals("970436", parsed.bankBin);
        assertEquals("1047061615", parsed.accountNumber);
        assertEquals("DO QUOC TRUNG", parsed.recipientName);
        assertEquals(Long.valueOf(5000), parsed.amount);
        assertEquals("QUY AN TET 2026", parsed.transferContent);
    }

    @Test
    public void testParseMbBankVietQr() {
        String payload = "00020101021238540010A00000072701240006970422011003926584720208QRIBFTTA53037045802VN5912DO QUOC VIET63041234";
        ScannedQrPayment parsed = ScannedQrPayment.parse(payload);

        assertNotNull(parsed);
        assertEquals("970422", parsed.bankBin);
        assertEquals("0392658472", parsed.accountNumber);
        assertEquals("DO QUOC VIET", parsed.recipientName);
    }

    @Test
    public void testParseTechcombankVietQr() {
        String payload = "00020101021238580010A000000727012800069704070114190367891234560208QRIBFTTA53037045802VN5916NGUYEN VAN THIEN63045678";
        ScannedQrPayment parsed = ScannedQrPayment.parse(payload);

        assertNotNull(parsed);
        assertEquals("970407", parsed.bankBin);
        assertEquals("19036789123456", parsed.accountNumber);
        assertEquals("NGUYEN VAN THIEN", parsed.recipientName);
    }

    @Test
    public void testParseVietinbankVietQr() {
        String payload = "00020101021238560010A0000007270126000697041501121018675432100208QRIBFTTA53037045802VN5915LE THI KIM NGAN63049999";
        ScannedQrPayment parsed = ScannedQrPayment.parse(payload);

        assertNotNull(parsed);
        assertEquals("970415", parsed.bankBin);
        assertEquals("101867543210", parsed.accountNumber);
        assertEquals("LE THI KIM NGAN", parsed.recipientName);
    }

    @Test
    public void testParseVietQrUrl() {
        String url = "https://api.vietqr.io/image/970436-1047061615?amount=20000&note=Chuyen%20tien%20quy";
        ScannedQrPayment parsed = ScannedQrPayment.parse(url);

        assertNotNull(parsed);
        assertEquals("970436", parsed.bankBin);
        assertEquals("1047061615", parsed.accountNumber);
        assertEquals(Long.valueOf(20000), parsed.amount);
        assertEquals("Chuyen tien quy", parsed.transferContent);
    }

    @Test
    public void testParseGenericBankString() {
        String text = "970436-1047061615-DO QUOC TRUNG";
        ScannedQrPayment parsed = ScannedQrPayment.parse(text);

        assertNotNull(parsed);
        assertEquals("970436", parsed.bankBin);
        assertEquals("1047061615", parsed.accountNumber);
        assertEquals("DO QUOC TRUNG", parsed.recipientName);
    }
}
